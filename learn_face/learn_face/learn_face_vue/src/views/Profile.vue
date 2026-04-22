<template>
  <div class="profile-container">
    <div class="profile-card">
      <div class="profile-header">
        <div class="header-background"></div>
        <div class="header-content">
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

          <div class="user-basic-info">
            <h1 class="username">{{ userStore.user?.nickname || userStore.user?.username || '未登录用户' }}</h1>
            <div class="user-meta">
              <span class="meta-tag">ID: {{ userStore.user?.id || 'N/A' }}</span>
              <span class="meta-tag role-tag">{{ userStore.user?.role || '普通用户' }}</span>
            </div>
          </div>
        </div>
      </div>

      <div class="profile-details">
        <el-tabs v-model="activeTab" class="profile-tabs" @tab-change="handleTabChange">
          <el-tab-pane label="个人信息" name="info">
            <div class="tab-content">
              <div class="section-header">
                <h3>基础信息</h3>
                <el-button v-if="!isEditing" type="primary" plain :icon="Edit" @click="startEditing">编辑信息</el-button>
                <div v-else class="action-buttons">
                  <el-button @click="cancelEdit">取消</el-button>
                  <el-button type="primary" :loading="isSaving" @click="saveChanges">保存</el-button>
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
                  <label class="info-label">邮箱</label>
                  <div v-if="!isEditing" class="info-value">{{ userStore.user?.email || '-' }}</div>
                  <el-input v-else v-model="form.email" placeholder="请输入邮箱" />
                </div>
                <div class="info-group">
                  <label class="info-label">生日</label>
                  <div v-if="!isEditing" class="info-value">{{ formatDate(userStore.user?.birthday) || '-' }}</div>
                  <el-date-picker
                    v-else
                    v-model="form.birthday"
                    type="date"
                    placeholder="请选择生日"
                    style="width: 100%"
                    format="YYYY-MM-DD"
                    value-format="YYYY-MM-DD"
                  />
                </div>
              </div>
            </div>
          </el-tab-pane>

          <el-tab-pane label="考试记录" name="exams">
            <div class="tab-content">
              <div class="section-header">
                <h3>考试记录</h3>
                <el-button :icon="Refresh" circle @click="fetchExamList" :loading="examState.loading" />
              </div>

              <el-table :data="examState.list" v-loading="examState.loading" style="width: 100%" class="exam-table">
                <el-table-column prop="name" label="考试名称" min-width="180">
                  <template #default="{ row }">
                    <div class="exam-name-cell">
                      <el-icon class="exam-icon"><Document /></el-icon>
                      <span>{{ row.name || '-' }}</span>
                    </div>
                  </template>
                </el-table-column>

                <el-table-column label="得分 / 总分" width="150" align="center">
                  <template #default="{ row }">
                    <span class="score-text">
                      <span class="my-score">{{ getExamDisplayScore(row) }}</span>
                      <span class="separator">/</span>
                      <span class="total-score">{{ row.totalScore || 0 }}</span>
                    </span>
                  </template>
                </el-table-column>

                <el-table-column label="状态" width="110" align="center">
                  <template #default="{ row }">
                    <el-tag :type="getExamStatusType(row)" effect="light" round>
                      {{ getExamStatusLabel(row) }}
                    </el-tag>
                  </template>
                </el-table-column>

                <el-table-column label="提交人" prop="createBy" min-width="180" show-overflow-tooltip />

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

          <el-tab-pane label="账号安全" name="security">
            <div class="tab-content">
              <div class="security-list">
                <div class="security-item">
                  <div class="item-icon safe">
                    <el-icon><Lock /></el-icon>
                  </div>
                  <div class="item-content">
                    <h4>登录密码</h4>
                    <p>建议定期更新密码，提升账号安全性。</p>
                  </div>
                  <el-button v-if="!isChangingPassword" link type="primary" @click="startPasswordChange">修改</el-button>
                </div>

                <div v-if="isChangingPassword" class="password-change-form">
                  <el-form
                    :model="passwordForm"
                    :rules="passwordRules"
                    ref="passwordFormRef"
                    label-width="100px"
                    style="margin-top: 20px; padding: 20px; background: #f9fafc; border-radius: 8px; border: 1px solid #f0f0f0;"
                  >
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
                        <el-button type="primary" :loading="isPasswordSaving" @click="changePassword">保存新密码</el-button>
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

    <el-dialog
      v-model="detailVisible"
      title="考试详情"
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
              <span class="value highlight">{{ getExamDisplayScore(currentExam) }}</span>
            </div>
            <div class="divider"></div>
            <div class="score-item">
              <span class="label">总分</span>
              <span class="value">{{ currentExam.totalScore || 0 }}</span>
            </div>
          </div>
        </div>

        <div class="questions-list">
          <div
            v-for="(q, index) in currentExam.answer"
            :key="q.id || index"
            class="question-card"
            :class="getQuestionCardClass(q)"
          >
            <div class="q-header">
              <div class="q-meta">
                <span class="q-index">#{{ index + 1 }}</span>
                <el-tag size="small">{{ q.category || '-' }}</el-tag>
                <el-tag size="small" :type="getQuestionTagType(q)" effect="dark">
                  {{ q.result || '待批改' }}
                </el-tag>
              </div>
              <div class="q-score">
                得分 <span class="num">{{ getQuestionDisplayScore(q) }}</span> / {{ q.score || 0 }}
              </div>
            </div>
            <div class="q-content">{{ q.question || '-' }}</div>
            <div class="q-answer-area">
              <div class="user-answer">
                <span class="label">学生答案：</span>
                <span class="text">{{ q.answer || '-' }}</span>
              </div>
              <div class="user-answer">
                <span class="label">标准答案：</span>
                <span class="text">{{ q.correctAnswer || '-' }}</span>
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
import { ref, reactive, computed, watch, onMounted, onUnmounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Loading, Camera, Edit, Lock, Document, Refresh } from '@element-plus/icons-vue'
import { uploadFileAPI, updateUserInfoAPI, updatePasswordAPI, getExamListAPI } from '../utils/api'

const userStore = useUserStore()
const activeTab = ref('info')
const uploadRef = ref(null)

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

const isChangingPassword = ref(false)
const isPasswordSaving = ref(false)
const passwordFormRef = ref(null)
const passwordForm = reactive({
  currentPassword: '',
  newPassword: '',
  confirmNewPassword: ''
})

const examState = reactive({
  loading: false,
  list: [],
  currentPage: 1,
  pageSize: 10,
  total: 0
})
const detailVisible = ref(false)
const currentExam = ref(null)

const displayAvatar = computed(() => {
  if (previewAvatarUrl.value) return previewAvatarUrl.value
  return userStore.user?.avatar
})

const initFormFromUser = () => {
  form.nickname = userStore.user?.nickname || ''
  form.gender = userStore.user?.gender || ''
  form.email = userStore.user?.email || ''
  form.birthday = userStore.user?.birthday || ''
}

const getUserInitial = () => {
  const name = userStore.user?.nickname || userStore.user?.username || 'U'
  return name.charAt(0).toUpperCase()
}

const formatDate = (dateString) => {
  if (!dateString) return null
  try {
    const date = new Date(dateString)
    if (Number.isNaN(date.getTime())) return dateString
    return date.toLocaleDateString('zh-CN', { year: 'numeric', month: '2-digit', day: '2-digit' })
  } catch (e) {
    return dateString
  }
}

const getGenderText = (gender) => {
  const map = { male: '男', female: '女', secret: '保密' }
  return map[gender] || '未设置'
}

const startEditing = () => {
  initFormFromUser()
  isEditing.value = true
}

const cancelEdit = () => {
  isEditing.value = false
  initFormFromUser()
}

const saveChanges = async () => {
  if (!userStore.user?.id) {
    ElMessage.error('用户未登录')
    return
  }
  isSaving.value = true
  try {
    const payload = {
      id: userStore.user.id,
      nickname: form.nickname,
      gender: form.gender,
      email: form.email,
      birthday: form.birthday
    }
    const result = await updateUserInfoAPI(payload)
    if (result !== null && result !== undefined) {
      userStore.updateUserInfo(payload)
      ElMessage.success('信息保存成功')
      isEditing.value = false
    } else {
      ElMessage.error('信息保存失败')
    }
  } catch (error) {
    ElMessage.error('信息保存失败')
  } finally {
    isSaving.value = false
  }
}

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

    const avatarUrl = await uploadFileAPI(file)
    if (!avatarUrl) {
      throw new Error('上传失败')
    }

    const payload = { id: userStore.user?.id, avatar: avatarUrl }
    const result = await updateUserInfoAPI(payload)
    if (result !== null && result !== undefined) {
      userStore.updateUserInfo(payload)
      ElMessage.success('头像更新成功')
    } else {
      throw new Error('头像保存失败')
    }
  } catch (error) {
    ElMessage.error('头像更新失败')
  } finally {
    if (previewAvatarUrl.value) {
      URL.revokeObjectURL(previewAvatarUrl.value)
      previewAvatarUrl.value = ''
    }
    isAvatarUploading.value = false
    if (uploadRef.value) uploadRef.value.clearFiles()
  }
}

const handleTabChange = (tabName) => {
  if (tabName === 'exams' && examState.list.length === 0 && !examState.loading) {
    fetchExamList()
  }
}

watch(activeTab, (tabName) => {
  if (tabName === 'exams' && examState.list.length === 0 && !examState.loading) {
    fetchExamList()
  }
})

watch(() => userStore.user, () => {
  initFormFromUser()
}, { immediate: true })

const normalizeExamPageData = (res) => {
  if (Array.isArray(res)) {
    return {
      data: res,
      total: res.length,
      currentPage: examState.currentPage
    }
  }

  if (res && Array.isArray(res.data)) {
    return {
      data: res.data,
      total: Number(res.total) || res.data.length,
      currentPage: Number(res.currentPage) || examState.currentPage
    }
  }

  if (res && res.data && Array.isArray(res.data.data)) {
    return {
      data: res.data.data,
      total: Number(res.data.total) || res.data.data.length,
      currentPage: Number(res.data.currentPage) || examState.currentPage
    }
  }

  if (res && Array.isArray(res.records)) {
    return {
      data: res.records,
      total: Number(res.total) || res.records.length,
      currentPage: Number(res.current) || examState.currentPage
    }
  }

  return {
    data: [],
    total: 0,
    currentPage: examState.currentPage
  }
}

const parseAnswerArray = (answerValue) => {
  if (Array.isArray(answerValue)) return answerValue
  if (typeof answerValue === 'string' && answerValue.trim()) {
    try {
      const parsed = JSON.parse(answerValue)
      return Array.isArray(parsed) ? parsed : []
    } catch (e) {
      return []
    }
  }
  return []
}

const isSubjective = (category) => {
  const text = String(category || '').toLowerCase()
  return text.includes('简答') || text.includes('essay') || text.includes('绠€绛')
}

const isCorrectResult = (result) => {
  const text = String(result || '').trim().toLowerCase()
  if (!text) return false
  return text === '正确' || text === 'correct' || text === '对' || text.includes('正确') || text.includes('correct') || text.includes('姝ｇ')
}

const normalizeQuestion = (question) => {
  const score = Number(question.score) || 0
  const manualRequired = question.manualRequired !== undefined && question.manualRequired !== null
    ? !!question.manualRequired
    : isSubjective(question.category)

  let obtainedScore = Number(question.obtainedScore)
  if (Number.isNaN(obtainedScore)) {
    obtainedScore = isCorrectResult(question.result) ? score : 0
  }

  return {
    ...question,
    score,
    manualRequired,
    obtainedScore,
    result: question.result || (manualRequired ? '待批改' : (obtainedScore > 0 ? '正确' : '错误'))
  }
}

const normalizeExamRow = (row) => {
  const copied = JSON.parse(JSON.stringify(row || {}))
  copied.answer = parseAnswerArray(copied.answer).map(normalizeQuestion)
  return copied
}

const fetchExamList = async () => {
  examState.loading = true
  try {
    const params = {
      currentPage: examState.currentPage,
      pageSize: examState.pageSize
    }
    const res = await getExamListAPI(params)
    const pageData = normalizeExamPageData(res)

    examState.list = Array.isArray(pageData.data) ? pageData.data.map(normalizeExamRow) : []
    examState.total = Number(pageData.total) || examState.list.length
    examState.currentPage = Number(pageData.currentPage) || examState.currentPage
  } catch (error) {
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
  if (!row || row.finScore === null || row.finScore === undefined) return 'warning'
  const total = Number(row.totalScore) || 0
  const score = Number(row.finScore) || 0
  const ratio = total > 0 ? score / total : 0
  if (ratio >= 0.8) return 'success'
  if (ratio >= 0.6) return 'primary'
  return 'danger'
}

const getExamStatusLabel = (row) => {
  if (!row || row.finScore === null || row.finScore === undefined) return '待批改'
  const total = Number(row.totalScore) || 0
  const score = Number(row.finScore) || 0
  const ratio = total > 0 ? score / total : 0
  if (ratio >= 0.8) return '优秀'
  if (ratio >= 0.6) return '及格'
  return '不及格'
}

const getExamDisplayScore = (row) => {
  if (!row) return '--'
  return row.finScore === null || row.finScore === undefined ? '待批改' : row.finScore
}

const getQuestionDisplayScore = (question) => {
  if (!question) return 0
  const obtained = Number(question.obtainedScore)
  if (!Number.isNaN(obtained)) return obtained
  return isCorrectResult(question.result) ? Number(question.score) || 0 : 0
}

const getQuestionTagType = (question) => {
  if (!question || !question.result) return 'warning'
  const resultText = String(question.result)
  if (resultText.includes('待') || resultText.toLowerCase().includes('pending')) return 'warning'
  if (isCorrectResult(resultText)) return 'success'
  if (resultText.includes('部分')) return 'primary'
  return 'danger'
}

const getQuestionCardClass = (question) => {
  if (!question || !question.result) return 'is-pending'
  const resultText = String(question.result)
  if (resultText.includes('待') || resultText.toLowerCase().includes('pending')) return 'is-pending'
  if (isCorrectResult(resultText)) return 'is-correct'
  return 'is-wrong'
}

const viewExamDetail = (row) => {
  currentExam.value = normalizeExamRow(row)
  detailVisible.value = true
}

const validateNewPassword = (rule, value, callback) => {
  if (!value) {
    callback(new Error('请输入新密码'))
    return
  }
  if (value.length < 6) {
    callback(new Error('新密码至少 6 位'))
    return
  }
  callback()
}

const validateConfirmPassword = (rule, value, callback) => {
  if (!value) {
    callback(new Error('请再次输入新密码'))
    return
  }
  if (value !== passwordForm.newPassword) {
    callback(new Error('两次输入的新密码不一致'))
    return
  }
  callback()
}

const passwordRules = {
  currentPassword: [{ required: true, message: '请输入当前密码', trigger: 'blur' }],
  newPassword: [{ validator: validateNewPassword, trigger: 'blur' }],
  confirmNewPassword: [{ validator: validateConfirmPassword, trigger: 'blur' }]
}

const startPasswordChange = () => {
  isChangingPassword.value = true
  Object.keys(passwordForm).forEach(key => {
    passwordForm[key] = ''
  })
}

const cancelPasswordChange = () => {
  isChangingPassword.value = false
  if (passwordFormRef.value) passwordFormRef.value.clearValidate()
}

const changePassword = async () => {
  if (!passwordFormRef.value) return
  const valid = await passwordFormRef.value.validate().catch(() => false)
  if (!valid) return

  isPasswordSaving.value = true
  try {
    const result = await updatePasswordAPI({
      username: userStore.user?.username,
      password: passwordForm.currentPassword,
      newPassword: passwordForm.newPassword
    })

    if (result !== null && result !== undefined) {
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

onMounted(() => {
  if (activeTab.value === 'exams') {
    fetchExamList()
  }
})

onUnmounted(() => {
  if (previewAvatarUrl.value) {
    URL.revokeObjectURL(previewAvatarUrl.value)
  }
})
</script>

<style scoped>
.profile-container {
  padding: 40px 20px;
  background-color: #f0f2f5;
  min-height: 100vh;
  display: flex;
  justify-content: center;
  align-items: flex-start;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
}

.profile-card {
  width: 100%;
  max-width: 850px;
  background: #fff;
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

.avatar-section {
  position: relative;
}

.avatar-uploader :deep(.el-upload) {
  border: none;
  background: transparent;
  cursor: pointer;
}

.profile-avatar-wrapper {
  position: relative;
  width: 130px;
  height: 130px;
  border-radius: 50%;
  border: 4px solid #fff;
  background: #fff;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  overflow: hidden;
  transition: transform 0.2s;
}

.profile-avatar-wrapper:hover {
  transform: scale(1.02);
}

.profile-avatar {
  width: 100%;
  height: 100%;
  border-radius: 50%;
  overflow: hidden;
  position: relative;
}

.avatar-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.avatar-placeholder {
  width: 100%;
  height: 100%;
  background: #eef1f6;
  color: #909399;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 40px;
  font-weight: 600;
}

.avatar-loading {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: rgba(255, 255, 255, 0.8);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 10;
}

.avatar-hover-mask {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: #fff;
  opacity: 0;
  transition: opacity 0.3s;
  font-size: 12px;
  gap: 5px;
}

.profile-avatar-wrapper:hover .avatar-hover-mask {
  opacity: 1;
}

.avatar-loading .is-loading {
  animation: rotating 2s linear infinite;
}

.user-basic-info {
  flex: 1;
  padding-bottom: 5px;
}

.username {
  margin: 0 0 8px 0;
  font-size: 24px;
  font-weight: 700;
  color: #303133;
}

.user-meta {
  display: flex;
  gap: 10px;
}

.meta-tag {
  font-size: 12px;
  padding: 2px 8px;
  background: #f0f2f5;
  color: #606266;
  border-radius: 4px;
}

.role-tag {
  background: #ecf5ff;
  color: #409eff;
}

.profile-details {
  padding: 10px 0;
}

.profile-tabs :deep(.el-tabs__header) {
  padding: 0 40px;
  margin-bottom: 0;
}

.profile-tabs :deep(.el-tabs__nav-wrap::after) {
  height: 1px;
  background-color: #f0f0f0;
}

.profile-tabs :deep(.el-tabs__item) {
  font-size: 15px;
  height: 55px;
  line-height: 55px;
}

.tab-content {
  padding: 30px 40px 40px;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 30px;
}

.section-header h3 {
  margin: 0;
  font-size: 18px;
  color: #303133;
}

.info-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 30px 40px;
}

.info-group {
  display: flex;
  flex-direction: column;
}

.info-label {
  font-size: 14px;
  color: #909399;
  margin-bottom: 8px;
}

.info-value {
  font-size: 15px;
  color: #303133;
  padding: 8px 0;
  border-bottom: 1px solid #f0f0f0;
  min-height: 36px;
}

.info-value.disabled {
  color: #909399;
}

.security-list {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.security-item {
  display: flex;
  align-items: center;
  padding: 20px;
  background: #f9fafc;
  border-radius: 8px;
  border: 1px solid #f0f0f0;
}

.item-icon {
  width: 40px;
  height: 40px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  margin-right: 15px;
}

.item-icon.safe {
  background: #e1f3d8;
  color: #67c23a;
}

.item-content {
  flex: 1;
}

.item-content h4 {
  margin: 0 0 5px 0;
  font-size: 15px;
  color: #303133;
}

.item-content p {
  margin: 0;
  font-size: 13px;
  color: #909399;
}

.password-actions {
  display: flex;
  gap: 10px;
  justify-content: flex-end;
}

.exam-table {
  margin-bottom: 20px;
}

.exam-name-cell {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 500;
  color: #303133;
}

.exam-icon {
  color: #409eff;
  font-size: 18px;
}

.score-text {
  font-family: 'Roboto Mono', monospace;
  font-weight: 600;
}

.my-score {
  color: #409eff;
  font-size: 16px;
}

.separator {
  color: #909399;
  margin: 0 4px;
}

.total-score {
  color: #909399;
  font-size: 13px;
}

.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  margin-top: 20px;
}

.exam-detail-container {
  padding: 0 10px;
}

.detail-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 25px;
  padding-bottom: 15px;
  border-bottom: 1px solid #eee;
}

.detail-header h2 {
  margin: 0;
  font-size: 22px;
  color: #303133;
}

.detail-score-card {
  display: flex;
  align-items: center;
  background: #f5f7fa;
  padding: 10px 20px;
  border-radius: 8px;
}

.score-item {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.score-item .label {
  font-size: 12px;
  color: #909399;
  margin-bottom: 2px;
}

.score-item .value {
  font-size: 24px;
  font-weight: bold;
  color: #606266;
  font-family: 'Roboto Mono', monospace;
}

.score-item .value.highlight {
  color: #409eff;
}

.divider {
  width: 1px;
  height: 30px;
  background: #dcdfe6;
  margin: 0 20px;
}

.questions-list {
  display: flex;
  flex-direction: column;
  gap: 15px;
  max-height: 60vh;
  overflow-y: auto;
  padding-right: 5px;
}

.question-card {
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  padding: 15px;
  background: #fff;
  transition: all 0.3s;
}

.question-card:hover {
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.05);
}

.question-card.is-pending {
  border-left: 4px solid #e6a23c;
}

.question-card.is-correct {
  border-left: 4px solid #67c23a;
}

.question-card.is-wrong {
  border-left: 4px solid #f56c6c;
}

.q-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}

.q-meta {
  display: flex;
  align-items: center;
  gap: 8px;
}

.q-index {
  font-weight: bold;
  color: #909399;
  font-size: 14px;
}

.q-score {
  font-size: 12px;
  color: #606266;
  background: #f5f7fa;
  padding: 2px 8px;
  border-radius: 4px;
}

.q-score .num {
  font-weight: bold;
}

.q-content {
  font-size: 15px;
  line-height: 1.5;
  color: #303133;
  margin-bottom: 12px;
  font-weight: 500;
}

.q-answer-area {
  background: #fafafa;
  padding: 10px;
  border-radius: 4px;
  font-size: 14px;
}

.user-answer .label {
  color: #909399;
}

.user-answer .text {
  color: #303133;
  font-weight: 500;
}

@keyframes rotating {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}
</style>
