<template>
  <div class="profile-container">
    <div class="profile-card">
      <div class="profile-header">
        <div class="header-background"></div>
        <div class="header-content">
          
          <!-- 头像区域 -->
          <div class="avatar-section">
            <el-upload
              ref="uploadRef"
              class="avatar-uploader"
              :show-file-list="false"
              :auto-upload="false"
              :on-change="handleAvatarChange"
              accept="image/gif,image/jpeg,image/jpg,image/png"
            >
              <div class="profile-avatar-wrapper">
                <div v-if="isAvatarUploading" class="avatar-loading">
                  <el-icon class="is-loading" :size="30"><Loading /></el-icon>
                </div>
                <div class="profile-avatar">
                  <img v-if="displayAvatar" :src="displayAvatar" alt="Avatar" class="avatar-img" />
                  <div v-else class="avatar-placeholder">{{ getUserInitial() }}</div>
                  <div v-if="!isAvatarUploading" class="avatar-hover-mask">
                    <el-icon :size="24" color="#fff"><Camera /></el-icon>
                    <span>更换头像</span>
                  </div>
                </div>
              </div>
            </el-upload>
          </div>

          <!-- 用户基本信息 -->
          <div class="user-basic-info">
            <h1 class="username">{{ userStore.user?.nickname || userStore.user?.username || '访客' }}</h1>
            <div class="user-meta">
              <span class="meta-tag">ID: {{ userStore.user?.id || 'N/A' }}</span>
              <span class="meta-tag role-tag">{{ userStore.user?.role || '普通用户' }}</span>
            </div>
          </div>
        </div>
      </div>

      <div class="profile-details">
        <el-tabs v-model="activeTab" class="profile-tabs" @tab-change="handleTabChange">
          
          <!-- 1. 个人资料 Tab -->
          <el-tab-pane label="个人资料" name="info">
            <div class="tab-content">
              <div class="section-header">
                <h3>基本信息</h3>
                <el-button v-if="!isEditing" type="primary" plain :icon="Edit" @click="startEditing">编辑资料</el-button>
                <div v-else class="action-buttons">
                  <el-button @click="cancelEdit">取消</el-button>
                  <el-button type="primary" :loading="isSaving" @click="saveChanges">保存更改</el-button>
                </div>
              </div>

              <div class="info-grid">
                <div class="info-group">
                  <label class="info-label">用户名</label>
                  <div class="info-value disabled">{{ userStore.user?.username || '-' }}</div>
                </div>
                <div class="info-group">
                  <label class="info-label">昵称</label>
                  <div v-if="!isEditing" class="info-value">{{ userStore.user?.nickname || '-' }}</div>
                  <el-input v-else v-model="form.nickname" placeholder="请输入昵称" />
                </div>
                <div class="info-group">
                  <label class="info-label">性别</label>
                  <div v-if="!isEditing" class="info-value">{{ getGenderText(userStore.user?.gender) }}</div>
                  <el-select v-else v-model="form.gender" placeholder="请选择性别" style="width: 100%">
                    <el-option label="男" value="male"></el-option>
                    <el-option label="女" value="female"></el-option>
                    <el-option label="保密" value="secret"></el-option>
                  </el-select>
                </div>
                <div class="info-group">
                  <label class="info-label">电子邮箱</label>
                  <div v-if="!isEditing" class="info-value">{{ userStore.user?.email || '未绑定' }}</div>
                  <el-input v-else v-model="form.email" placeholder="请输入邮箱" />
                </div>
                <div class="info-group">
                  <label class="info-label">出生日期</label>
                  <div v-if="!isEditing" class="info-value">{{ formatDate(userStore.user?.birthday) || '未设置' }}</div>
                  <el-date-picker v-else v-model="form.birthday" type="date" placeholder="选择日期" style="width: 100%" format="YYYY-MM-DD" value-format="YYYY-MM-DD" />
                </div>
              </div>
            </div>
          </el-tab-pane>

          <!-- 2. 我的考试 Tab -->
          <el-tab-pane label="考试记录" name="exams">
            <div class="tab-content">
              <div class="section-header">
                <h3>考试记录</h3>
                <el-button :icon="Refresh" circle @click="fetchExamList" :loading="examState.loading" />
              </div>

              <el-table :data="examState.list" v-loading="examState.loading" style="width: 100%" class="exam-table">
                <el-table-column prop="name" label="试卷名称" min-width="150">
                  <template #default="{ row }">
                    <div class="exam-name-cell">
                      <el-icon class="exam-icon"><Document /></el-icon>
                      <span>{{ row.name }}</span>
                    </div>
                  </template>
                </el-table-column>
                
                <el-table-column label="得分 / 总分" width="150" align="center">
                  <template #default="{ row }">
                    <span class="score-text">
                      <span class="my-score">{{ row.finScore }}</span> 
                      <span class="separator">/</span> 
                      <span class="total-score">{{ row.totalScore }}</span>
                    </span>
                  </template>
                </el-table-column>

                <el-table-column label="状态" width="100" align="center">
                  <template #default="{ row }">
                    <el-tag :type="getExamStatusType(row)" effect="light" round>
                      {{ getExamStatusText(row) }}
                    </el-tag>
                  </template>
                </el-table-column>

                <el-table-column label="交卷人" prop="createBy" min-width="180" show-overflow-tooltip />

                <el-table-column label="操作" width="120" align="center">
                  <template #default="{ row }">
                    <el-button type="primary" link @click="viewExamDetail(row)">查看详情</el-button>
                  </template>
                </el-table-column>
              </el-table>

              <div class="pagination-wrapper" v-if="examState.total > 0">
                <el-pagination
                  background
                  layout="prev, pager, next"
                  :total="examState.total"
                  :page-size="examState.pageSize"
                  :current-page="examState.currentPage"
                  @current-change="handleExamPageChange"
                />
              </div>
              
              <el-empty v-if="!examState.loading && examState.total === 0" description="暂无考试记录" />
            </div>
          </el-tab-pane>

          <!-- 3. 我的勋章 Tab (新增) -->
          <el-tab-pane label="我的勋章" name="badges">
            <div class="tab-content">
              <div class="section-header">
                <h3>荣誉殿堂</h3>
                <span class="badge-count">已解锁 {{ unlockedBadgesCount }} / {{ badgesList.length }}</span>
              </div>

              <div class="badges-grid">
                <div 
                  v-for="badge in badgesList" 
                  :key="badge.id" 
                  class="badge-card"
                  :class="{ 'is-locked': !badge.unlocked }"
                >
                  <div class="badge-icon-wrapper">
                    <component :is="badge.icon" class="badge-icon" />
                    <div v-if="!badge.unlocked" class="lock-overlay">
                      <el-icon><Lock /></el-icon>
                    </div>
                  </div>
                  <div class="badge-info">
                    <h4 class="badge-name">{{ badge.name }}</h4>
                    <p class="badge-desc">{{ badge.description }}</p>
                    <div class="badge-meta" v-if="badge.unlocked">
                      <span class="obtained-date">{{ badge.obtainedDate }} 获得</span>
                    </div>
                    <div class="badge-meta" v-else>
                      <span class="progress-text">未解锁</span>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </el-tab-pane>

          <!-- 4. 账号安全 Tab -->
          <el-tab-pane label="账号安全" name="security">
            <div class="tab-content">
              <div class="security-list">
                <div class="security-item">
                  <div class="item-icon safe">
                    <el-icon><Lock /></el-icon>
                  </div>
                  <div class="item-content">
                    <h4>登录密码</h4>
                    <p>建议定期修改密码以保护账户安全</p>
                  </div>
                  <el-button v-if="!isChangingPassword" link type="primary" @click="startPasswordChange">修改</el-button>
                </div>
                
                <div v-if="isChangingPassword" class="password-change-form">
                  <el-form :model="passwordForm" :rules="passwordRules" ref="passwordFormRef" label-width="100px" style="margin-top: 20px; padding: 20px; background: #f9fafc; border-radius: 8px; border: 1px solid #f0f0f0;">
                    <el-form-item label="当前密码" prop="currentPassword">
                      <el-input v-model="passwordForm.currentPassword" type="password" placeholder="请输入当前密码" show-password />
                    </el-form-item>
                    <el-form-item label="新密码" prop="newPassword">
                      <el-input v-model="passwordForm.newPassword" type="password" placeholder="请输入新密码" show-password />
                    </el-form-item>
                    <el-form-item label="确认新密码" prop="confirmNewPassword">
                      <el-input v-model="passwordForm.confirmNewPassword" type="password" placeholder="请再次输入新密码" show-password />
                    </el-form-item>
                    <el-form-item>
                      <div class="password-actions">
                        <el-button @click="cancelPasswordChange">取消</el-button>
                        <el-button type="primary" :loading="isPasswordSaving" @click="changePassword">保存密码</el-button>
                      </div>
                    </el-form-item>
                  </el-form>
                </div>
              </div>
            </div>
          </el-tab-pane>
        </el-tabs>
      </div>
    </div>

    <!-- 考试详情弹窗 -->
    <el-dialog
      v-model="detailVisible"
      title="考试详情回顾"
      width="800px"
      destroy-on-close
      top="5vh"
    >
      <div v-if="currentExam" class="exam-detail-container">
        <div class="detail-header">
          <h2>{{ currentExam.name }}</h2>
          <div class="detail-score-card">
            <div class="score-item">
              <span class="label">最终得分</span>
              <span class="value highlight">{{ currentExam.finScore }}</span>
            </div>
            <div class="divider"></div>
            <div class="score-item">
              <span class="label">卷面总分</span>
              <span class="value">{{ currentExam.totalScore }}</span>
            </div>
          </div>
        </div>
        <div class="questions-list">
          <div 
            v-for="(q, index) in currentExam.answer" 
            :key="q.id" 
            class="question-card"
            :class="{ 'is-correct': q.result === '正确', 'is-wrong': q.result !== '正确' }"
          >
            <div class="q-header">
              <div class="q-meta">
                <span class="q-index">#{{ index + 1 }}</span>
                <el-tag size="small">{{ q.category }}</el-tag>
                <el-tag size="small" :type="q.result === '正确' ? 'success' : 'danger'" effect="dark">
                  {{ q.result }}
                </el-tag>
              </div>
              <div class="q-score">
                 得分: <span class="num">{{ q.result === '正确' ? q.score : 0 }}</span> / {{ q.score }}
              </div>
            </div>
            <div class="q-content">{{ q.question }}</div>
            <div class="q-answer-area">
              <div class="user-answer">
                <span class="label">你的答案：</span>
                <span class="text">{{ q.answer }}</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { useUserStore } from '../stores/user'
import { ref, reactive, computed, onUnmounted } from 'vue'
import { ElMessage } from 'element-plus'
import { 
  Loading, Camera, Edit, Lock, Document, Refresh, 
  Trophy, Medal, Star, Collection, DataLine 
} from '@element-plus/icons-vue'
import { uploadFileAPI, updateUserInfoAPI, updatePasswordAPI, getExamListAPI } from '../utils/api'

const userStore = useUserStore()
const activeTab = ref('info')
const uploadRef = ref(null)

// --- 1. 个人资料状态 ---
const isEditing = ref(false)
const isSaving = ref(false)
const isAvatarUploading = ref(false)
const previewAvatarUrl = ref('')
const form = reactive({
  nickname: '',
  gender: '',
  email: '',
  birthday: ''
})

// --- 2. 账号安全状态 ---
const isChangingPassword = ref(false)
const isPasswordSaving = ref(false)
const passwordFormRef = ref(null)
const passwordForm = reactive({
  currentPassword: '',
  newPassword: '',
  confirmNewPassword: ''
})

// --- 3. 考试模块状态 ---
const examState = reactive({
  loading: false,
  list: [],
  currentPage: 1,
  pageSize: 10,
  total: 0
})
const detailVisible = ref(false)
const currentExam = ref(null)

// --- 4. 勋章模块状态 (静态数据) ---
const badgesList = ref([
  {
    id: 1,
    name: '初入江湖',
    description: '注册账号并完善个人信息',
    icon: 'UserFilled', 
    icon: Trophy,
    unlocked: true,
    obtainedDate: '2026-01-01'
  },
  {
    id: 2,
    name: '学霸附体',
    description: '单次考试获得满分',
    icon: Medal,
    unlocked: true,
    obtainedDate: '2026-01-12'
  },
  {
    id: 3,
    name: '勤奋刻苦',
    description: '累计完成 10 次考试',
    icon: Collection,
    unlocked: false,
    obtainedDate: ''
  },
  {
    id: 4,
    name: '百里挑一',
    description: '总分排名进入全站前 100',
    icon: Star,
    unlocked: false,
    obtainedDate: ''
  },
  {
    id: 5,
    name: '数据达人',
    description: '连续 7 天登录系统',
    icon: DataLine,
    unlocked: false,
    obtainedDate: ''
  }
])

const unlockedBadgesCount = computed(() => {
  return badgesList.value.filter(b => b.unlocked).length
})

// --- 通用计算属性 & 方法 ---
const displayAvatar = computed(() => {
  if (previewAvatarUrl.value) return previewAvatarUrl.value
  return userStore.user?.avatar
})

const getUserInitial = () => {
  const name = userStore.user?.nickname || userStore.user?.username || 'U'
  return name.charAt(0).toUpperCase()
}

const formatDate = (dateString) => {
  if (!dateString) return null
  try {
    const date = new Date(dateString)
    if (isNaN(date.getTime())) return dateString
    return date.toLocaleDateString('zh-CN', { year: 'numeric', month: '2-digit', day: '2-digit' })
  } catch (e) { return dateString }
}

const getGenderText = (gender) => {
  const map = { 'male': '男', 'female': '女' }
  return map[gender] || '未知'
}

// --- Tab 切换逻辑 ---
const handleTabChange = (tabName) => {
  if (tabName === 'exams' && examState.list.length === 0) {
    fetchExamList()
  }
}

// --- 考试逻辑 ---
const fetchExamList = async () => {
  examState.loading = true
  try {
    const params = {
      currentPage: examState.currentPage,
      pageSize: examState.pageSize
    }
    const res = await getExamListAPI(params)
    if (res.data) {
      examState.list = res.data
      examState.total = res.total
      examState.currentPage = res.currentPage
    }
  } catch (error) {
    console.error(error)
    ElMessage.error('获取考试列表失败')
  } finally {
    examState.loading = false
  }
}

const handleExamPageChange = (page) => {
  examState.currentPage = page
  fetchExamList()
}

const getExamStatusType = (row) => {
  const ratio = row.finScore / row.totalScore
  if (ratio >= 0.8) return 'success'
  if (ratio >= 0.6) return 'primary'
  return 'danger'
}

const getExamStatusText = (row) => {
  const ratio = row.finScore / row.totalScore
  if (ratio >= 0.8) return '优秀'
  if (ratio >= 0.6) return '合格'
  return '不合格'
}

const viewExamDetail = (row) => {
  currentExam.value = row
  detailVisible.value = true
}

// --- 个人资料编辑 ---
const startEditing = () => {
  form.nickname = userStore.user?.nickname || ''
  form.gender = userStore.user?.gender || ''
  form.email = userStore.user?.email || ''
  form.birthday = userStore.user?.birthday || ''
  isEditing.value = true
}

const cancelEdit = () => {
  isEditing.value = false
}

const saveChanges = async () => {
  isSaving.value = true
  try {
    const userInfo = { id: userStore.user?.id, ...form }
    const result = await updateUserInfoAPI(userInfo)
    if (result) {
      userStore.updateUserInfo(userInfo)
      ElMessage.success('资料已保存')
      isEditing.value = false
    } else {
      ElMessage.error('保存失败')
    }
  } catch (error) {
    ElMessage.error('保存失败: ' + (error.message || '未知错误'))
  } finally {
    isSaving.value = false
  }
}

// --- 头像上传 ---
const handleAvatarChange = async (uploadFile) => {
  const file = uploadFile.raw
  if (!file) return
  if (!file.type.startsWith('image/')) {
    ElMessage.error('请选择图片文件')
    return
  }
  if (file.size / 1024 / 1024 > 2) {
    ElMessage.error('图片大小不能超过 2MB')
    return
  }
  try {
    isAvatarUploading.value = true
    if (previewAvatarUrl.value) URL.revokeObjectURL(previewAvatarUrl.value)
    previewAvatarUrl.value = URL.createObjectURL(file)
    const uploadResult = await uploadFileAPI(file)
    if (uploadResult) {
      await updateUserAvatar(uploadResult)
      previewAvatarUrl.value = ''
    } else {
      throw new Error('上传失败')
    }
  } catch (error) {
    ElMessage.error('头像上传失败')
    previewAvatarUrl.value = ''
  } finally {
    isAvatarUploading.value = false
    if (uploadRef.value) uploadRef.value.clearFiles()
  }
}

const updateUserAvatar = async (avatarUrl) => {
  const userInfo = { id: userStore.user?.id, avatar: avatarUrl }
  const result = await updateUserInfoAPI(userInfo)
  if (result) userStore.updateUserInfo(userInfo)
}

// --- 密码修改 ---
const validateNewPassword = (rule, value, callback) => {
  if (value === '') callback(new Error('请输入新密码'))
  else if (value.length < 6) callback(new Error('密码长度至少为6位'))
  else callback()
}
const validateConfirmPassword = (rule, value, callback) => {
  if (value === '') callback(new Error('请再次输入新密码'))
  else if (value !== passwordForm.newPassword) callback(new Error('两次输入的密码不一致'))
  else callback()
}
const passwordRules = {
  currentPassword: [{ required: true, message: '请输入当前密码', trigger: 'blur' }],
  newPassword: [{ validator: validateNewPassword, trigger: 'blur' }],
  confirmNewPassword: [{ validator: validateConfirmPassword, trigger: 'blur' }]
}

const startPasswordChange = () => {
  isChangingPassword.value = true
  Object.keys(passwordForm).forEach(key => passwordForm[key] = '')
}
const cancelPasswordChange = () => {
  isChangingPassword.value = false
  if (passwordFormRef.value) passwordFormRef.value.clearValidate()
}
const changePassword = async () => {
  if (passwordFormRef.value) {
    const valid = await passwordFormRef.value.validate().catch(() => false)
    if (!valid) return
  }
  isPasswordSaving.value = true
  try {
    const result = await updatePasswordAPI({
      username: userStore.user?.username,
      password: passwordForm.currentPassword,
      newPassword: passwordForm.newPassword
    })
    if (result) {
      ElMessage.success('密码修改成功')
      cancelPasswordChange()
    } else {
      ElMessage.error('密码修改失败')
    }
  } catch (error) {
    ElMessage.error('密码修改失败')
  } finally {
    isPasswordSaving.value = false
  }
}

onUnmounted(() => {
  if (previewAvatarUrl.value) URL.revokeObjectURL(previewAvatarUrl.value)
})
</script>

<style scoped>
/* 保持原有布局不变 */
.profile-container {
  padding: 40px 20px;
  background-color: #f0f2f5;
  min-height: 100vh;
  display: flex;
  justify-content: center;
  align-items: flex-start;
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif;
}

.profile-card {
  width: 100%;
  max-width: 850px;
  background: white;
  border-radius: 16px;
  box-shadow: 0 8px 30px rgba(0, 0, 0, 0.05);
  overflow: hidden;
}

.profile-header {
  position: relative;
  height: 220px;
  background-color: #fff;
}

.header-background {
  height: 140px;
  background: linear-gradient(120deg, #a1c4fd 0%, #c2e9fb 100%);
}

.header-content {
  position: absolute;
  bottom: 0;
  left: 0;
  width: 100%;
  padding: 0 40px 25px;
  display: flex;
  align-items: flex-end;
  gap: 25px;
}

.avatar-section { position: relative; }
.avatar-uploader :deep(.el-upload) { border: none; background: transparent; cursor: pointer; }
.profile-avatar-wrapper {
  position: relative; width: 130px; height: 130px; border-radius: 50%;
  border: 4px solid white; background: white; box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  overflow: hidden; transition: transform 0.2s;
}
.profile-avatar-wrapper:hover { transform: scale(1.02); }
.profile-avatar { width: 100%; height: 100%; border-radius: 50%; overflow: hidden; position: relative; }
.avatar-img { width: 100%; height: 100%; object-fit: cover; }
.avatar-placeholder { width: 100%; height: 100%; background: #eef1f6; color: #909399; display: flex; align-items: center; justify-content: center; font-size: 40px; font-weight: 600; }
.avatar-loading { position: absolute; top: 0; left: 0; width: 100%; height: 100%; background: rgba(255, 255, 255, 0.8); display: flex; align-items: center; justify-content: center; z-index: 10; }
.avatar-hover-mask { position: absolute; top: 0; left: 0; width: 100%; height: 100%; background: rgba(0, 0, 0, 0.5); display: flex; flex-direction: column; align-items: center; justify-content: center; color: white; opacity: 0; transition: opacity 0.3s; font-size: 12px; gap: 5px; }
.profile-avatar-wrapper:hover .avatar-hover-mask { opacity: 1; }
.avatar-loading .is-loading { animation: rotating 2s linear infinite; }

.user-basic-info { flex: 1; padding-bottom: 5px; }
.username { margin: 0 0 8px 0; font-size: 24px; font-weight: 700; color: #303133; }
.user-meta { display: flex; gap: 10px; }
.meta-tag { font-size: 12px; padding: 2px 8px; background: #f0f2f5; color: #606266; border-radius: 4px; }
.role-tag { background: #ecf5ff; color: #409eff; }

.profile-details { padding: 10px 0; }
.profile-tabs :deep(.el-tabs__header) { padding: 0 40px; margin-bottom: 0; }
.profile-tabs :deep(.el-tabs__nav-wrap::after) { height: 1px; background-color: #f0f0f0; }
.profile-tabs :deep(.el-tabs__item) { font-size: 15px; height: 55px; line-height: 55px; }
.tab-content { padding: 30px 40px 40px; }
.section-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 30px; }
.section-header h3 { margin: 0; font-size: 18px; color: #303133; }

.info-grid { display: grid; grid-template-columns: repeat(2, 1fr); gap: 30px 40px; }
.info-group { display: flex; flex-direction: column; }
.info-label { font-size: 14px; color: #909399; margin-bottom: 8px; }
.info-value { font-size: 15px; color: #303133; padding: 8px 0; border-bottom: 1px solid #f0f0f0; min-height: 36px; }
.info-value.disabled { color: #909399; }

.security-list { display: flex; flex-direction: column; gap: 20px; }
.security-item { display: flex; align-items: center; padding: 20px; background: #f9fafc; border-radius: 8px; border: 1px solid #f0f0f0; }
.item-icon { width: 40px; height: 40px; border-radius: 8px; display: flex; align-items: center; justify-content: center; font-size: 20px; margin-right: 15px; }
.item-icon.safe { background: #e1f3d8; color: #67c23a; }
.item-content { flex: 1; }
.item-content h4 { margin: 0 0 5px 0; font-size: 15px; color: #303133; }
.item-content p { margin: 0; font-size: 13px; color: #909399; }
.password-actions { display: flex; gap: 10px; justify-content: flex-end; }

/* 考试样式 */
.exam-table { margin-bottom: 20px; }
.exam-name-cell { display: flex; align-items: center; gap: 8px; font-weight: 500; color: #303133; }
.exam-icon { color: #409eff; font-size: 18px; }
.score-text { font-family: 'Roboto Mono', monospace; font-weight: 600; }
.my-score { color: #409eff; font-size: 16px; }
.separator { color: #909399; margin: 0 4px; }
.total-score { color: #909399; font-size: 13px; }
.pagination-wrapper { display: flex; justify-content: flex-end; margin-top: 20px; }

/* 考试弹窗样式 */
.exam-detail-container { padding: 0 10px; }
.detail-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 25px; padding-bottom: 15px; border-bottom: 1px solid #eee; }
.detail-header h2 { margin: 0; font-size: 22px; color: #303133; }
.detail-score-card { display: flex; align-items: center; background: #f5f7fa; padding: 10px 20px; border-radius: 8px; }
.score-item { display: flex; flex-direction: column; align-items: center; }
.score-item .label { font-size: 12px; color: #909399; margin-bottom: 2px; }
.score-item .value { font-size: 24px; font-weight: bold; color: #606266; font-family: 'Roboto Mono'; }
.score-item .value.highlight { color: #409eff; }
.divider { width: 1px; height: 30px; background: #dcdfe6; margin: 0 20px; }
.questions-list { display: flex; flex-direction: column; gap: 15px; max-height: 60vh; overflow-y: auto; padding-right: 5px; }
.question-card { border: 1px solid #e4e7ed; border-radius: 8px; padding: 15px; background: #fff; transition: all 0.3s; }
.question-card:hover { box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.05); }
.question-card.is-correct { border-left: 4px solid #67c23a; }
.question-card.is-wrong { border-left: 4px solid #f56c6c; }
.q-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 10px; }
.q-meta { display: flex; align-items: center; gap: 8px; }
.q-index { font-weight: bold; color: #909399; font-size: 14px; }
.q-score { font-size: 12px; color: #606266; background: #f5f7fa; padding: 2px 8px; border-radius: 4px; }
.q-score .num { font-weight: bold; }
.q-content { font-size: 15px; line-height: 1.5; color: #303133; margin-bottom: 12px; font-weight: 500; }
.q-answer-area { background: #fafafa; padding: 10px; border-radius: 4px; font-size: 14px; }
.user-answer .label { color: #909399; }
.user-answer .text { color: #303133; font-weight: 500; }

/* ------------------ 新增：勋章模块样式 ------------------ */
.badge-count { font-size: 14px; color: #909399; background: #f0f2f5; padding: 4px 10px; border-radius: 12px; }
.badges-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 20px;
}

.badge-card {
  background: #fff;
  border: 1px solid #e4e7ed;
  border-radius: 12px;
  padding: 25px 15px;
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  transition: all 0.3s ease;
  cursor: default;
}

.badge-card:hover:not(.is-locked) {
  transform: translateY(-5px);
  box-shadow: 0 8px 20px rgba(0, 0, 0, 0.1);
  border-color: #c6e2ff;
}

.badge-icon-wrapper {
  position: relative;
  width: 60px;
  height: 60px;
  border-radius: 50%;
  background: linear-gradient(135deg, #fdfbfb 0%, #ebedee 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 15px;
  font-size: 32px;
  color: #e6a23c; /* 金色图标 */
  box-shadow: inset 0 0 10px rgba(0,0,0,0.05);
}

.badge-info { width: 100%; }
.badge-name { margin: 0 0 5px 0; font-size: 16px; color: #303133; font-weight: 600; }
.badge-desc { margin: 0 0 10px 0; font-size: 12px; color: #909399; line-height: 1.4; height: 34px; overflow: hidden; } /* 固定高度防止卡片参差不齐 */

.badge-meta { font-size: 12px; border-top: 1px solid #f5f7fa; padding-top: 10px; width: 100%; }
.obtained-date { color: #67c23a; }
.progress-text { color: #909399; }

/* 未解锁状态样式 */
.badge-card.is-locked {
  background: #f9fafc;
  opacity: 0.8;
}

.badge-card.is-locked .badge-icon-wrapper {
  color: #c0c4cc; /* 灰色图标 */
}

.badge-card.is-locked .lock-overlay {
  position: absolute;
  top: 0; left: 0; right: 0; bottom: 0;
  background: rgba(255, 255, 255, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  color: #606266;
  font-size: 20px;
}

@keyframes rotating { from { transform: rotate(0deg); } to { transform: rotate(360deg); } }
</style>