<template>
  <el-container class="layout-v7">
    <!-- 1. 左侧悬浮侧边栏 -->
    <el-aside :width="isCollapse ? '80px' : '250px'" class="float-sidebar">
      <!-- Logo 区域 -->
      <div class="sidebar-header">
        <div class="logo-square">
          <i class="el-icon-coffee" v-if="!isCollapse"></i>
          <span v-else>M</span>
        </div>
        <transition name="fade">
          <h1 class="app-brand" v-show="!isCollapse">{{ appTitle }}</h1>
        </transition>
      </div>

      <!-- 菜单区域 (只会在这里出现滚动) -->
      <el-menu
        :default-active="$route.path"
        background-color="transparent"
        text-color="#8d8076"
        active-text-color="#5e5045"
        router
        :collapse="isCollapse"
        :collapse-transition="true"
        class="mocha-menu"
      >
        <el-menu-item 
          v-for="item in currentMenuList" 
          :key="item.index" 
          :index="item.index" 
          class="mocha-item"
        >
          <img :src="item.icon" class="menu-icon" :alt="item.alt">
          <span slot="title">{{ item.label }}</span>
        </el-menu-item>
      </el-menu>

      <!-- 底部折叠按钮 -->
      <div class="sidebar-bottom" @click="toggleAside">
         <i :class="isCollapse ? 'el-icon-s-unfold' : 'el-icon-s-fold'"></i>
      </div>
    </el-aside>

    <!-- 2. 右侧悬浮内容容器 -->
    <el-container class="float-main-container">
      <!-- 顶部 Header -->
      <el-header class="mocha-header" height="70px">
        <div class="header-left">
          <div class="page-title-box">
             <span class="sub-txt">Current Page</span>
             <h2 class="main-txt">{{ currentPath }}</h2>
          </div>
        </div>

        <div class="header-right">
          <div class="date-pill">{{ currentTime.split(' ')[0] }}</div>
          
          <el-dropdown @command="handleCommand" trigger="click">
            <div class="user-profile-mocha">
              <el-avatar :size="38" :src="userPhoto" @error="handleAvatarError" class="mocha-avatar">
                {{ userInitials }}
              </el-avatar>
              <div class="user-info">
                <span class="u-name">{{ username }}</span>
                <span class="u-role">{{ role === 'root' ? 'Admin' : 'Staff' }}</span>
              </div>
              <i class="el-icon-arrow-down arrow-down"></i>
            </div>
            <el-dropdown-menu slot="dropdown" class="mocha-dropdown">
              <el-dropdown-item command="profile">个人中心</el-dropdown-item>
              <el-dropdown-item command="password">修改密码</el-dropdown-item>
              <el-dropdown-item command="logout" divided style="color:#d96d6d;">退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </el-dropdown>
        </div>
      </el-header>

      <!-- 内容区域 -->
      <el-main class="mocha-content">
        <div class="content-scroll-wrapper">
           <transition name="slide-fade" mode="out-in">
             <router-view class="inner-page"></router-view>
           </transition>
        </div>
      </el-main>
    </el-container>

    <!-- 弹窗 -->
    <el-dialog title="个人设置" :visible.sync="profileVisible" width="500px" :before-close="handleClose" custom-class="mocha-dialog">
       <el-form ref="profileForm" :model="userInfo" label-width="80px">
          <div class="avatar-section">
            <el-upload class="avatar-uploader" action="/v1/common/file/upload" :show-file-list="false"
              :on-success="handleAvatarSuccess" :before-upload="beforeAvatarUpload" :headers="uploadHeaders">
              <el-avatar :size="100" :src="userPhoto" class="profile-avatar" @error="handleAvatarError" fit="cover">{{ userInitials }}</el-avatar>
              <div class="avatar-uploader-tip">点击更换</div>
            </el-upload>
          </div>
          <el-form-item label="用户名"><el-input v-model="userInfo.username" disabled></el-input></el-form-item>
          <el-form-item label="姓名"><el-input v-model="userInfo.nickname"></el-input></el-form-item>
          <el-form-item label="性别">
            <el-select v-model="userInfo.gender" style="width: 100%;"><el-option label="男" value="male"></el-option><el-option label="女" value="female"></el-option></el-select>
          </el-form-item>
          <el-form-item label="手机号"><el-input v-model="userInfo.phone"></el-input></el-form-item>
          <el-form-item label="地址"><el-input v-model="userInfo.address"></el-input></el-form-item>
        </el-form>
        <span slot="footer" class="dialog-footer">
          <el-button @click="profileVisible = false">取消</el-button>
          <el-button type="primary" class="mocha-btn" @click="handleSaveProfile">保存</el-button>
        </span>
    </el-dialog>

    <el-dialog title="修改密码" :visible.sync="passwordVisible" width="400px" :before-close="handleClose" custom-class="mocha-dialog">
      <el-form ref="passwordForm" :model="passwordForm" label-width="80px">
        <el-form-item label="旧密码"><el-input v-model="passwordForm.oldPassword" type="password" show-password></el-input></el-form-item>
        <el-form-item label="新密码"><el-input v-model="passwordForm.newPassword" type="password" show-password></el-input></el-form-item>
        <el-form-item label="确认密码"><el-input v-model="passwordForm.confirmPassword" type="password" show-password></el-input></el-form-item>
      </el-form>
      <span slot="footer" class="dialog-footer">
          <el-button @click="passwordVisible = false">取消</el-button>
          <el-button type="primary" class="mocha-btn" @click="handleUpdatePassword">确定</el-button>
        </span>
    </el-dialog>
  </el-container>
</template>

<script>
import analysisIcon from '@/assets/analysis.png'
import bannerIcon from '@/assets/banner.png'
import forumIcon from '@/assets/comments.png'
import ordersIcon from '@/assets/orders.png'
import accountIcon from '@/assets/account.png'
import logIcon from '@/assets/log.png'
import examIcon from '@/assets/warehouse.png'

export default {
  name: 'LayoutV7',
  data() {
    return {
      isCollapse: false,
      defaultAvatar: 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png',
      currentTime: '',
      timer: null,
      profileVisible: false,
      passwordVisible: false,
      passwordForm: { oldPassword: '', newPassword: '', confirmPassword: '' },
      menuMap: {
        root: [
          { index: '/analyse', tooltip: '数据统计管理', icon: analysisIcon, alt: 'analyse', label: '数据统计管理' },
          { index: '/banner', tooltip: '轮播图管理', icon: bannerIcon, alt: 'dashboard', label: '轮播图管理' },
          { index: '/forum', tooltip: '论坛信息管理', icon: forumIcon, alt: 'forum', label: '论坛信息管理' },
          { index: '/comment', tooltip: '评论信息管理', icon: forumIcon, alt: 'comment', label: '评论信息管理' },
          { index: '/question', tooltip: '题目信息管理', icon: examIcon, alt: 'question', label: '题目信息管理' },
          { index: '/testpaper', tooltip: '考试试卷管理', icon: examIcon, alt: 'testpaper', label: '考试试卷管理' },
          { index: '/exam', tooltip: '考试信息管理', icon: examIcon, alt: 'exam', label: '考试信息管理' },
          { index: '/course', tooltip: '课程信息管理', icon: examIcon, alt: 'course', label: '课程信息管理' },
          { index: '/goal', tooltip: '目标信息管理', icon: ordersIcon, alt: 'goal', label: '目标信息管理' },
          { index: '/medal', tooltip: '勋章信息管理', icon: logIcon, alt: 'medal', label: '勋章信息管理' },
          { index: '/llm', tooltip: 'AI对话管理', icon: logIcon, alt: 'llm', label: 'AI对话管理' },
          { index: '/log', tooltip: '日志记录管理', icon: logIcon, alt: 'log', label: '日志记录管理' },
          { index: '/dictionary', tooltip: '字典信息管理', icon: logIcon, alt: 'dictionary', label: '字典信息管理' },
          { index: '/account', tooltip: '账号信息管理', icon: accountIcon, alt: 'account', label: '账号信息管理' }
        ],
        admin: [
          { index: '/analyse', tooltip: '数据统计管理', icon: analysisIcon, alt: 'analyse', label: '数据统计管理' },
          { index: '/question', tooltip: '题目信息管理', icon: examIcon, alt: 'question', label: '题目信息管理' },
          { index: '/testpaper', tooltip: '考试试卷管理', icon: examIcon, alt: 'testpaper', label: '考试试卷管理' },
          { index: '/exam', tooltip: '考试信息管理', icon: examIcon, alt: 'exam', label: '考试信息管理' },
          { index: '/course', tooltip: '课程信息管理', icon: examIcon, alt: 'course', label: '课程信息管理' },
          { index: '/goal', tooltip: '目标信息管理', icon: ordersIcon, alt: 'goal', label: '目标信息管理' },
        ]
      }
    }
  },
  computed: {
    role() { return this.$store.state.role || 'root' },
    currentMenuList() { return this.menuMap[this.role] || [] },
    username() { return (this.$store.state.userInfo && this.$store.state.userInfo.username) || '用户' },
    currentPath() {
      let menu = this.currentMenuList.find(item => item.index === this.$route.path)
      if (menu) return menu.label
      const allMenus = Object.values(this.menuMap).flat()
      menu = allMenus.find(item => item.index === this.$route.path)
      if (menu) return menu.label
      const staticRoutes = { '/': '首页', '/batch/trace': '批次追溯管理' }
      return staticRoutes[this.$route.path] || '未知页面'
    },
    userPhoto() { return this.$store.state.userInfo?.avatar || this.defaultAvatar },
    userInitials() { return this.username.charAt(0).toUpperCase() },
    userInfo() { return this.$store.state.userInfo || {} },
    appTitle() { return import.meta.env.VITE_APP_TITLE || '管理系统' },
    uploadHeaders() { return { 'Authorization': localStorage.getItem('token') } }
  },
  methods: {
    handleCommand(command) {
      if (command === 'profile') this.profileVisible = true
      else if (command === 'password') this.passwordVisible = true
      else if (command === 'logout') {
        this.$store.dispatch('logout')
        this.$router.push('/login')
      }
    },
    toggleAside() { this.isCollapse = !this.isCollapse },
    handleAvatarError(e) { e.target.src = this.defaultAvatar },
    updateTime() {
      const now = new Date()
      this.currentTime = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-${String(now.getDate()).padStart(2, '0')} ${String(now.getHours()).padStart(2, '0')}:${String(now.getMinutes()).padStart(2, '0')}`
    },
    handleSaveProfile() {
      const userData = { id: this.userInfo.id, nickname: this.userInfo.nickname, gender: this.userInfo.gender, address: this.userInfo.address, avatar: this.userInfo.avatar }
      this.updateUserInfo(userData)
    },
    handleUpdatePassword() {
      if (!this.passwordForm.oldPassword) return this.$message.error('请输入当前密码')
      if (!this.passwordForm.newPassword) return this.$message.error('请输入新密码')
      if (this.passwordForm.newPassword !== this.passwordForm.confirmPassword) return this.$message.error('两次输入的新密码不一致')
      
      const passwordData = { username: this.username, password: this.passwordForm.oldPassword, newPassword: this.passwordForm.newPassword }
      this.$http.post('/v1/auth/password', passwordData, { headers: { 'Content-Type': 'application/json', 'Authorization': localStorage.getItem('token') } })
        .then(res => {
          if (res.data.code === 200) {
            this.handleClose()
            this.$confirm('密码修改成功，请重新登录', '提示', { confirmButtonText: '确定', showCancelButton: false, type: 'success' }).then(() => {
              this.$store.dispatch('logout'); this.$router.push('/login')
            })
          } else { this.$message.error(res.data.message || '修改失败') }
        }).catch(err => { this.$message.error(err.response?.data?.message || '修改失败') })
    },
    updateUserInfo(data) {
      this.$http.post('/v1/auth/update', data, { headers: { 'Content-Type': 'application/json', 'Authorization': localStorage.getItem('token') } })
        .then(res => {
          if (res.data.code === 200) {
            this.handleClose()
            this.$message.success('更新成功')
            this.$store.commit('SET_USER_INFO', { ...this.userInfo, ...data })
          } else { this.$message.error(res.data.message || '失败') }
        }).catch(err => this.$message.error(err.response?.data?.message || '失败'))
    },
    handleClose() {
      this.passwordForm = { oldPassword: '', newPassword: '', confirmPassword: '' }
      this.profileVisible = false; this.passwordVisible = false
    },
    handleAvatarSuccess(res) {
      if (res.code === 200) {
        this.$store.commit('SET_USER_INFO', { ...this.userInfo, avatar: res.data })
        this.$message.success('头像上传成功')
      } else { this.$message.error('上传失败') }
    },
    beforeAvatarUpload(file) {
      if (!file.type.startsWith('image/')) { this.$message.error('请选择图片'); return false }
      if (file.size / 1024 / 1024 > 2) { this.$message.error('图片不超过2MB'); return false }
      return true
    }
  },
  created() { this.updateTime(); this.timer = setInterval(this.updateTime, 1000) },
  beforeDestroy() { clearInterval(this.timer) }
}
</script>

<style scoped>
/* 全局背景：暖灰色 */
.layout-v7 {
  height: 100vh;
  width: 100vw;
  background-color: #f0f2f5; 
  padding: 15px; /* 全局留白，营造悬浮感 */
  box-sizing: border-box;
  font-family: 'Helvetica Neue', Helvetica, 'PingFang SC', 'Hiragino Sans GB', Arial, sans-serif;
  overflow: hidden; /* 修复关键：防止最外层出现滚动条 */
}

/* ================= 1. 悬浮侧边栏 ================= */
.float-sidebar {
  background: #ffffff;
  border-radius: 20px;
  box-shadow: 0 4px 12px rgba(0,0,0,0.03);
  margin-right: 20px;
  display: flex;
  flex-direction: column;
  height: 100%; /* 修复关键：填满父容器高度 */
  overflow: hidden; /* 修复关键：防止侧边栏整体滚动 */
  transition: width 0.35s ease;
  z-index: 10;
}

.sidebar-header {
  height: 80px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0; /* 防止被挤压 */
}
.logo-square {
  width: 40px; height: 40px;
  background: #bfa588; /* 浅咖色 */
  color: #fff;
  border-radius: 12px;
  display: flex; align-items: center; justify-content: center;
  font-size: 20px; font-weight: bold;
  box-shadow: 0 4px 8px rgba(191, 165, 136, 0.4);
}
.app-brand {
  margin-left: 12px; font-size: 18px; font-weight: 700; color: #5e5045; /* 深咖色 */
  white-space: nowrap;
}

.mocha-menu {
  flex: 1; /* 占据剩余空间 */
  border: none; 
  overflow-y: auto; /* 仅在此处允许滚动 */
  padding: 10px 0;
  
  /* 隐藏滚动条 */
  scrollbar-width: none;
  -ms-overflow-style: none;
}
.mocha-menu::-webkit-scrollbar { width: 0 !important; display: none; }

.mocha-item {
  margin: 4px 12px;
  height: 50px; line-height: 50px;
  border-radius: 12px;
  transition: all 0.3s;
  color: #8d8076 !important;
}

.menu-icon {
  width: 18px; height: 18px; margin-right: 12px;
  filter: sepia(0.2) grayscale(0.8);
  opacity: 0.7;
  transition: all 0.3s;
}

.mocha-item:hover {
  background-color: #f9f7f5 !important;
  color: #5e5045 !important;
}

.mocha-item.is-active {
  background-color: #f4ece4 !important;
  color: #5e5045 !important;
  font-weight: 600;
  position: relative;
}
.mocha-item.is-active::before {
  content: '';
  position: absolute; left: 0; top: 12px; bottom: 12px;
  width: 4px; border-radius: 0 4px 4px 0;
  background-color: #bfa588;
}
.mocha-item.is-active .menu-icon {
  filter: sepia(0.5) saturate(1.5);
  opacity: 1;
}

/* 侧边栏底部折叠按钮 */
.sidebar-bottom {
  height: 50px;
  border-top: 1px solid #f0f0f0;
  display: flex; align-items: center; justify-content: center;
  cursor: pointer; color: #8d8076; font-size: 20px;
  transition: background 0.3s;
  flex-shrink: 0; /* 防止被挤压 */
}
.sidebar-bottom:hover { background: #f9f9f9; color: #bfa588; }

/* ================= 2. 悬浮内容区 ================= */
.float-main-container {
  background: #ffffff;
  border-radius: 20px;
  box-shadow: 0 4px 12px rgba(0,0,0,0.03);
  overflow: hidden;
  display: flex;
  flex-direction: column;
  height: 100%; /* 修复关键：填满父容器 */
}

/* Header */
.mocha-header {
  padding: 0 30px;
  display: flex; align-items: center; justify-content: space-between;
  border-bottom: 1px solid #f5f5f5;
  flex-shrink: 0;
}

.header-left { display: flex; align-items: center; }
.page-title-box { display: flex; flex-direction: column; justify-content: center; }
.sub-txt { font-size: 11px; color: #b0a49b; text-transform: uppercase; letter-spacing: 1px; }
.main-txt { font-size: 20px; color: #5e5045; margin: 2px 0 0 0; font-weight: 700; }

.header-right { display: flex; align-items: center; }
.date-pill {
  background: #f9f7f5; color: #8d8076; padding: 6px 14px;
  border-radius: 20px; font-size: 13px; margin-right: 24px;
  font-family: monospace;
}

.user-profile-mocha {
  display: flex; align-items: center; cursor: pointer;
  padding: 4px; border-radius: 30px;
  transition: all 0.3s;
  border: 1px solid transparent;
}
.user-profile-mocha:hover { background: #f9f7f5; border-color: #f0f0f0; }
.mocha-avatar { background: #d6c6b8; color: #fff; }
.user-info { margin: 0 10px; display: flex; flex-direction: column; }
.u-name { font-size: 14px; font-weight: 600; color: #5e5045; line-height: 1.2; }
.u-role { font-size: 12px; color: #b0a49b; }
.arrow-down { color: #ccc; }

/* 内容区域 */
.mocha-content {
  padding: 0;
  position: relative;
  background-color: #fff;
  flex: 1; /* 占据剩余高度 */
  overflow: hidden; /* 交给内部wrapper滚动 */
}
.content-scroll-wrapper {
  height: 100%;
  padding: 24px;
  overflow-y: auto;
}
.content-scroll-wrapper::-webkit-scrollbar { width: 6px; }
.content-scroll-wrapper::-webkit-scrollbar-thumb { background: #e0d8cf; border-radius: 3px; }
.content-scroll-wrapper::-webkit-scrollbar-track { background: transparent; }

.inner-page {
  min-height: 100%;
}

.slide-fade-enter-active, .slide-fade-leave-active { transition: all 0.3s ease; }
.slide-fade-enter { transform: translateX(10px); opacity: 0; }
.slide-fade-leave-to { transform: translateX(-10px); opacity: 0; }

.mocha-btn { background-color: #bfa588 !important; border-color: #bfa588 !important; }
.mocha-btn:hover { background-color: #a89075 !important; border-color: #a89075 !important; }
.mocha-dropdown { border: none !important; box-shadow: 0 4px 16px rgba(0,0,0,0.08) !important; }
.mocha-dropdown .el-dropdown-menu__item:hover { background-color: #f9f7f5 !important; color: #5e5045 !important; }

.avatar-section { text-align: center; margin-bottom: 20px; }
.avatar-uploader-tip { margin-top: 8px; color: #999; font-size: 12px; }
</style>