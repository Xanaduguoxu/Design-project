<template>
  <header class="header">
    <div class="container">
      <el-row align="middle" justify="space-between">
        <el-col :span="6">
          <router-link to="/" class="logo">
            <div class="logo-content">
              <h1>{{ appTitle }}</h1>
            </div>
          </router-link>
        </el-col>

        <el-col :span="12">
          <el-menu mode="horizontal" :default-active="route.path" class="nav-menu">
            <el-menu-item v-for="item in navMenu" :key="item.index" :index="item.index" @click="handleMenuClick(item)">
              {{ item.label }}
            </el-menu-item>
          </el-menu>
        </el-col>

        <el-col :span="6" class="user-actions">
          <el-space :size="16">
            <template v-if="userStore.isLoggedIn">
              <el-dropdown trigger="hover">
                <el-button text class="user-btn">
                  <el-avatar :size="32" :src="userStore.user.avatar" class="user-avatar">
                    <template #default>
                      {{ userInitial }}
                    </template>
                  </el-avatar>
                  <span class="user-name">{{ userStore.user.nickname || userStore.user.username }}</span>
                  <el-icon class="el-icon--right">
                    <ArrowDown />
                  </el-icon>
                </el-button>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item @click="router.push('/goal')">目标管理</el-dropdown-item>
                    <el-dropdown-item @click="router.push('/learningAnalysis')">学习分析</el-dropdown-item>
                    <el-dropdown-item @click="router.push('/profile')">个人中心</el-dropdown-item>
                    <el-dropdown-item divided @click="logout">退出登录</el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
            </template>

            <template v-else>
              <el-button @click="openLoginDialog" text class="auth-btn login-btn">登录</el-button>
              <el-button @click="openRegisterDialog" type="primary" class="auth-btn register-btn">注册</el-button>
            </template>
          </el-space>
        </el-col>
      </el-row>
    </div>

    <el-dialog v-model="showAuthDialog" title="" width="400px" :show-close="true" class="minimal-auth-dialog" center
      :close-on-click-modal="true" :modal-class="'minimal-modal-backdrop'" :append-to-body="false">
      <div class="auth-container">

        <div class="header-titles">
          <h2 class="main-title">{{ isRegisterMode ? '创建账户' : '欢迎登录' }}</h2>
          <p class="sub-title">{{ isRegisterMode ? '加入我们，开启新体验' : '输入您的账号以继续' }}</p>
        </div>

        <div class="auth-tabs">
          <div class="tab-item" :class="{ active: !isRegisterMode }" @click="switchToLogin">
            <span>登录</span>
          </div>
          <div class="tab-item" :class="{ active: isRegisterMode }" @click="switchToRegister">
            <span>注册</span>
          </div>
          <div class="tab-indicator" :class="{ 'slide-right': isRegisterMode }"></div>
        </div>

        <div class="form-container">
          <transition name="fade-slide" mode="out-in">
            <!-- 登录表单 -->
            <div v-if="!isRegisterMode" key="login" class="form-panel">
              <el-form :model="loginForm" :rules="loginRules" ref="loginFormRef" class="auth-form"
                @submit.prevent="handleLogin">

                <div class="input-group">
                  <label class="minimal-label">账号</label>
                  <el-form-item prop="email">
                    <el-input ref="firstLoginInputRef" v-model="loginForm.email" placeholder="请输入账号" size="large"
                      @keyup.enter="handleLogin" :disabled="loading" class="minimal-input" clearable>
                    </el-input>
                  </el-form-item>
                </div>

                <div class="input-group">
                  <label class="minimal-label">密码</label>
                  <el-form-item prop="password">
                    <el-input v-model="loginForm.password" type="password" placeholder="请输入密码" show-password
                      size="large" @keyup.enter="handleLogin" :disabled="loading" class="minimal-input">
                    </el-input>
                  </el-form-item>
                </div>

                <div class="input-group">
                  <label class="minimal-label">验证码</label>
                  <div class="captcha-flex">
                    <el-form-item prop="captcha" class="captcha-form-item">
                      <el-input v-model="loginForm.captcha" placeholder="4位字符" size="large" @keyup.enter="handleLogin"
                        :disabled="loading" class="minimal-input" maxlength="4">
                      </el-input>
                    </el-form-item>
                    <div class="captcha-canvas-container" @click="handleRefreshLoginCaptcha" title="点击刷新">
                      <canvas ref="loginCaptchaRef" width="110" height="48"></canvas>
                    </div>
                  </div>
                </div>

                <el-button @click="handleLogin" :loading="loading" type="primary" size="large"
                  class="minimal-submit-btn" :disabled="!isLoginFormValid">
                  <span v-if="!loading">登录</span>
                  <span v-else>登录中...</span>
                </el-button>
              </el-form>
            </div>

            <!-- 注册表单 -->
            <div v-else key="register" class="form-panel">
              <el-form :model="registerForm" :rules="registerRules" ref="registerFormRef" class="auth-form"
                @submit.prevent="handleRegister">

                <div class="input-group">
                  <label class="minimal-label">设置账号</label>
                  <el-form-item prop="email">
                    <el-input ref="firstRegisterInputRef" v-model="registerForm.email" placeholder="至少2位字符" size="large"
                      :disabled="loading" class="minimal-input" @input="validateEmailRealtime" clearable>
                    </el-input>
                  </el-form-item>
                </div>

                <div class="input-group">
                  <label class="minimal-label">设置密码</label>
                  <el-form-item prop="password">
                    <el-input v-model="registerForm.password" type="password" placeholder="至少6位字符" show-password
                      size="large" :disabled="loading" class="minimal-input">
                    </el-input>
                  </el-form-item>
                </div>

                <div class="input-group">
                  <label class="minimal-label">确认密码</label>
                  <el-form-item prop="confirmPassword">
                    <el-input v-model="registerForm.confirmPassword" type="password" placeholder="请再次输入密码" show-password
                      size="large" :disabled="loading" class="minimal-input" @input="validatePasswordMatch">
                    </el-input>
                  </el-form-item>
                </div>

                <div class="input-group">
                  <label class="minimal-label">验证码</label>
                  <div class="captcha-flex">
                    <el-form-item prop="captcha" class="captcha-form-item">
                      <el-input v-model="registerForm.captcha" placeholder="4位字符" size="large"
                        @keyup.enter="handleRegister" :disabled="loading" class="minimal-input" maxlength="4">
                      </el-input>
                    </el-form-item>
                    <div class="captcha-canvas-container" @click="handleRefreshRegisterCaptcha" title="点击刷新">
                      <canvas ref="registerCaptchaRef" width="110" height="48"></canvas>
                    </div>
                  </div>
                </div>

                <el-button @click="handleRegister" :loading="loading" type="primary" size="large"
                  class="minimal-submit-btn" :disabled="!isRegisterFormValid">
                  <span v-if="!loading">创建账户</span>
                  <span v-else>创建中...</span>
                </el-button>
              </el-form>
            </div>
          </transition>
        </div>
      </div>
    </el-dialog>
  </header>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onBeforeUnmount, nextTick, getCurrentInstance } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from '../stores/user'
import { ElMessage } from 'element-plus'
import { ArrowDown } from '@element-plus/icons-vue'
import { loginAPI, registerAPI } from '../utils/api'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const { proxy } = getCurrentInstance()
const emitter = proxy.$emitter

const showAuthDialog = ref(false)
const isRegisterMode = ref(false)
const loading = ref(false)
const emailValidated = ref(false)
const passwordMatched = ref(false)
const loginAttempts = ref(0)
const maxLoginAttempts = 5
const pendingRoute = ref(null)

const loginFormRef = ref(null)
const registerFormRef = ref(null)
const firstLoginInputRef = ref(null)
const firstRegisterInputRef = ref(null)
const loginCaptchaRef = ref(null)
const registerCaptchaRef = ref(null)

const loginCaptchaCode = ref('')
const registerCaptchaCode = ref('')

const loginForm = reactive({ email: '', password: '', captcha: '' })
const registerForm = reactive({ email: '', password: '', confirmPassword: '', captcha: '' })

const appTitle = computed(() => import.meta.env.VITE_APP_TITLE || '平台')

const navMenu = computed(() => {
  try {
    const menuConfig = import.meta.env.VITE_NAV_MENU
    if (menuConfig) return JSON.parse(menuConfig)
  } catch (error) {
    console.error('解析配置失败:', error)
  }
  return []
})

const userInitial = computed(() => {
  const name = userStore.user?.nickname || userStore.user?.username || '用户'
  return name.charAt(0).toUpperCase()
})

const generateRandomString = (length = 4) => {
  const chars = 'ABCDEFGHJKLMNPQRSTUVWXYZabcdefghjkmnpqrstuvwxyz23456789'
  let result = ''
  for (let i = 0; i < length; i++) {
    result += chars.charAt(Math.floor(Math.random() * chars.length))
  }
  return result
}

// 修改了验证码底色，以匹配极简风格
const drawCaptcha = (canvasElement, text) => {
  if (!canvasElement) return
  const ctx = canvasElement.getContext('2d')
  const width = canvasElement.width
  const height = canvasElement.height

  ctx.clearRect(0, 0, width, height)

  ctx.fillStyle = '#f5f5f7' // 极简灰底
  ctx.fillRect(0, 0, width, height)

  for (let i = 0; i < 4; i++) {
    ctx.strokeStyle = `rgba(0,0,0,0.1)`
    ctx.beginPath()
    ctx.moveTo(Math.random() * width, Math.random() * height)
    ctx.lineTo(Math.random() * width, Math.random() * height)
    ctx.stroke()
  }

  for (let i = 0; i < 20; i++) {
    ctx.fillStyle = `rgba(0,0,0,0.15)`
    ctx.beginPath()
    ctx.arc(Math.random() * width, Math.random() * height, 1, 0, 2 * Math.PI)
    ctx.fill()
  }

  ctx.font = 'bold 24px -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif'
  ctx.textBaseline = 'middle'
  for (let i = 0; i < text.length; i++) {
    ctx.fillStyle = `#1d1d1f` // 苹果黑
    const x = 22 * i + 12
    const y = height / 2 + (Math.random() * 4 - 2)
    const deg = (Math.random() * 40 - 20) * Math.PI / 180

    ctx.translate(x, y)
    ctx.rotate(deg)
    ctx.fillText(text[i], 0, 0)
    ctx.rotate(-deg)
    ctx.translate(-x, -y)
  }
}

const refreshLoginCaptcha = () => {
  loginCaptchaCode.value = generateRandomString()
  drawCaptcha(loginCaptchaRef.value, loginCaptchaCode.value)
}

const refreshRegisterCaptcha = () => {
  registerCaptchaCode.value = generateRandomString()
  drawCaptcha(registerCaptchaRef.value, registerCaptchaCode.value)
}

const handleRefreshLoginCaptcha = () => {
  refreshLoginCaptcha()
  loginForm.captcha = ''
}

const handleRefreshRegisterCaptcha = () => {
  refreshRegisterCaptcha()
  registerForm.captcha = ''
}

const isLoginFormValid = computed(() => {
  return loginForm.email &&
    loginForm.password &&
    loginForm.password.length >= 6 &&
    loginForm.captcha.length === 4 &&
    loginForm.captcha.toLowerCase() === loginCaptchaCode.value.toLowerCase()
})

const isRegisterFormValid = computed(() => {
  return registerForm.email &&
    registerForm.password &&
    registerForm.confirmPassword &&
    registerForm.password === registerForm.confirmPassword &&
    registerForm.captcha.length === 4 &&
    registerForm.captcha.toLowerCase() === registerCaptchaCode.value.toLowerCase()
})

const validateConfirmPassword = (rule, value, callback) => {
  if (value !== registerForm.password) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

const validateLoginCaptcha = (rule, value, callback) => {
  if (!value) {
    callback(new Error('请输入验证码'))
  } else if (value.toLowerCase() !== loginCaptchaCode.value.toLowerCase()) {
    callback(new Error('验证码错误'))
  } else {
    callback()
  }
}

const validateRegisterCaptcha = (rule, value, callback) => {
  if (!value) {
    callback(new Error('请输入验证码'))
  } else if (value.toLowerCase() !== registerCaptchaCode.value.toLowerCase()) {
    callback(new Error('验证码错误'))
  } else {
    callback()
  }
}

const loginRules = reactive({
  email: [{ required: true, message: '请输入账号', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }, { min: 6, message: '不能少于6位', trigger: ['blur', 'change'] }],
  captcha: [{ required: true, validator: validateLoginCaptcha, trigger: 'blur' }]
})

const registerRules = reactive({
  email: [{ required: true, message: '请输入账号', trigger: 'blur' }, { min: 2, message: '至少2位', trigger: ['blur', 'change'] }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }, { min: 6, max: 20, message: '6-20位之间', trigger: ['blur', 'change'] }],
  confirmPassword: [{ required: true, message: '请确认密码', trigger: 'blur' }, { validator: validateConfirmPassword, trigger: ['blur', 'change'] }],
  captcha: [{ required: true, validator: validateRegisterCaptcha, trigger: 'blur' }]
})

const handleMenuClick = (menuItem) => {
  if (menuItem.index === '/' || menuItem.index === '/home') {
    router.push(menuItem.index)
    return
  }
  if (!userStore.isLoggedIn) {
    pendingRoute.value = menuItem.index
    openLoginDialog()
    ElMessage({ message: '请先登录后再访问该页面', type: 'warning', duration: 2000 })
  } else {
    router.push(menuItem.index)
  }
}

const logout = () => {
  userStore.logout()
  ElMessage({ message: '已安全退出登录', type: 'success' })
  router.push('/')
}

const resetForms = () => {
  loginForm.email = ''
  loginForm.password = ''
  loginForm.captcha = ''
  registerForm.email = ''
  registerForm.password = ''
  registerForm.confirmPassword = ''
  registerForm.captcha = ''
  emailValidated.value = false
  passwordMatched.value = false
  loading.value = false
  nextTick(() => {
    loginFormRef.value?.clearValidate()
    registerFormRef.value?.clearValidate()
  })
}

const focusFirstInput = () => {
  if (!isRegisterMode.value && firstLoginInputRef.value) firstLoginInputRef.value.focus()
  else if (isRegisterMode.value && firstRegisterInputRef.value) firstRegisterInputRef.value.focus()
}

const openLoginDialog = () => {
  isRegisterMode.value = false
  showAuthDialog.value = true
  resetForms()
  setTimeout(() => { refreshLoginCaptcha(); focusFirstInput() }, 100)
}

const openRegisterDialog = () => {
  isRegisterMode.value = true
  showAuthDialog.value = true
  resetForms()
  setTimeout(() => { refreshRegisterCaptcha(); focusFirstInput() }, 100)
}

const switchToLogin = () => {
  if (isRegisterMode.value) {
    isRegisterMode.value = false
    resetForms()
    setTimeout(() => { refreshLoginCaptcha(); focusFirstInput() }, 300)
  }
}

const switchToRegister = () => {
  if (!isRegisterMode.value) {
    isRegisterMode.value = true
    resetForms()
    setTimeout(() => { refreshRegisterCaptcha(); focusFirstInput() }, 300)
  }
}

const handleLogin = async () => {
  if (loginAttempts.value >= maxLoginAttempts) return ElMessage({ message: '尝试次数过多，请稍后再试', type: 'error' })
  if (!loginFormRef.value) return
  await loginFormRef.value.validate(async (valid) => {
    if (valid) {
      loading.value = true
      try {
        const userData = await loginAPI(loginForm.email, loginForm.password)
        if (!userData) return
        userStore.login(userData)
        loginAttempts.value = 0
        showAuthDialog.value = false
        ElMessage({ message: `欢迎回来，${userData.nickname || userData.username || '用户'}！`, type: 'success' })
        if (pendingRoute.value) { router.push(pendingRoute.value); pendingRoute.value = null }
      } catch (error) {
        loginAttempts.value++
        handleRefreshLoginCaptcha()
        ElMessage({ message: error.message || '登录失败，请检查账号和密码', type: 'error' })
      } finally {
        loading.value = false
      }
    }
  })
}

const handleRegister = async () => {
  if (!registerFormRef.value) return
  await registerFormRef.value.validate(async (valid) => {
    if (valid) {
      loading.value = true
      try {
        const result = await registerAPI(registerForm.email, registerForm.password)
        if (result === true) {
          userStore.login({ id: Date.now(), username: registerForm.email, email: registerForm.email })
          showAuthDialog.value = false
          ElMessage({ message: '注册成功！欢迎加入！', type: 'success' })
          if (pendingRoute.value) { router.push(pendingRoute.value); pendingRoute.value = null }
        }
      } catch (error) {
        handleRefreshRegisterCaptcha()
        let errorMessage = '注册失败，请稍后重试'
        if (error.message === '用户名重复!') errorMessage = '该账号已被注册'
        else if (error.message) errorMessage = error.message
        ElMessage({ message: errorMessage, type: 'error' })
      } finally {
        loading.value = false
      }
    }
  })
}

const validateEmailRealtime = () => { emailValidated.value = registerForm.email.trim().length >= 2 }
const validatePasswordMatch = () => { passwordMatched.value = registerForm.password === registerForm.confirmPassword && registerForm.confirmPassword.length > 0 }
const handleApiLoginRequest = () => openLoginDialog()

onMounted(() => {
  if (emitter) {
    emitter.on('openLoginFromFooter', (routePath) => { pendingRoute.value = routePath; openLoginDialog() })
    emitter.on('openLoginFromHome', (routePath) => { pendingRoute.value = routePath; openLoginDialog() })
    emitter.on('openRegisterFromHome', (routePath) => { pendingRoute.value = routePath; openRegisterDialog() })
  }
  window.addEventListener('showLoginDialog', handleApiLoginRequest)
})

onBeforeUnmount(() => {
  if (emitter) {
    emitter.off('openLoginFromFooter')
    emitter.off('openLoginFromHome')
    emitter.off('openRegisterFromHome')
  }
  window.removeEventListener('showLoginDialog', handleApiLoginRequest)
})
</script>

<style scoped>
.header {
  background: #000000;
  border-bottom: 1px solid #333333;
  position: sticky;
  top: 0;
  z-index: 1000;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.3);
}

.container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 24px;
}

.logo {
  text-decoration: none;
  display: inline-block;
  transition: transform 0.3s ease;
}

.logo:hover {
  transform: translateY(-2px);
}

.logo-content {
  display: flex;
  align-items: center;
  gap: 8px;
}

.logo h1 {
  color: #ffffff;
  font-size: 24px;
  font-weight: 700;
  margin: 0;
  letter-spacing: -0.5px;
}

.nav-menu {
  border-bottom: none;
  background: transparent;
  justify-content: center;
}

.nav-menu :deep(.el-menu-item) {
  color: #ffffff;
  font-weight: 600;
  font-size: 15px;
  padding: 0 20px;
  border-bottom: 2px solid transparent;
  transition: all 0.3s ease;
}

.nav-menu :deep(.el-menu-item:hover),
.nav-menu :deep(.el-menu-item.is-active) {
  color: #ffffff;
  background: rgba(255, 255, 255, 0.1);
  border-radius: 8px;
  border-bottom-color: transparent;
}

.user-actions {
  display: flex;
  justify-content: flex-end;
  align-items: center;
}

.user-btn {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  border-radius: 20px;
  transition: all 0.3s ease;
  color: #ffffff;
}

.user-btn:hover {
  background: rgba(0, 0, 0, 0.1);
  transform: translateY(-1px);
}

.user-avatar {
  background: rgba(255, 255, 255, 0.2);
  color: #ffffff;
  font-weight: 600;
  font-size: 14px;
  border: 2px solid rgba(255, 255, 255, 0.2);
  transition: all 0.3s ease;
}

.user-name {
  font-size: 14px;
  font-weight: 500;
  max-width: 100px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.auth-btn {
  font-weight: 600;
  padding: 10px 20px;
  border-radius: 25px;
  transition: all 0.3s ease;
}

.login-btn {
  color: #ffffff;
  border: 2px solid rgba(255, 255, 255, 0.5);
  background: transparent !important;
}

.login-btn:hover {
  background: rgba(255, 255, 255, 0.1) !important;
  border-color: #ffffff;
}

.register-btn {
  background: #ffffff !important;
  color: #000000;
  border: none;
}

.register-btn:hover {
  background: #f8f9fa !important;
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

:deep(.el-dropdown-menu) {
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  border: 1px solid #e5e7eb;
  margin-top: 8px;
}

:deep(.el-dropdown-menu__item) {
  padding: 10px 16px;
  font-size: 14px;
  color: #000000;
  transition: all 0.3s ease;
}

/* ==================== 极简苹果风 弹窗样式 (Version 1) ==================== */

/* 遮罩层 - 较深色的纯净遮罩 */
:deep(.minimal-modal-backdrop) {
  background: rgba(0, 0, 0, 0.6);
  backdrop-filter: blur(4px);
}

/* 弹窗容器 - 极简白卡片 */
:deep(.minimal-auth-dialog) {
  border-radius: 20px;
  overflow: hidden;
}

:deep(.minimal-auth-dialog .el-dialog) {
  background: #ffffff;
  border-radius: 20px;
  box-shadow: 0 24px 48px rgba(0, 0, 0, 0.12);
  margin: 0;
  padding: 0;
}

:deep(.minimal-auth-dialog .el-dialog__header) {
  display: none;
  /* 隐藏自带header */
}

/* 主容器 */
.auth-container {
  padding: 40px 36px;
  background: #ffffff;
  min-height: 480px;
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif;
}

/* 顶部标题区 */
.header-titles {
  text-align: left;
  margin-bottom: 24px;
}

.main-title {
  font-size: 26px;
  font-weight: 700;
  color: #1d1d1f;
  /* 苹果深空灰 */
  margin: 0 0 8px 0;
  letter-spacing: -0.5px;
}

.sub-title {
  font-size: 14px;
  color: #86868b;
  margin: 0;
  font-weight: 400;
}

/* 极简下划线 Tabs */
.auth-tabs {
  display: flex;
  position: relative;
  border-bottom: 1px solid #e5e5ea;
  margin-bottom: 32px;
}

.tab-item {
  flex: 1;
  text-align: center;
  padding: 12px 0;
  font-size: 15px;
  font-weight: 600;
  color: #86868b;
  cursor: pointer;
  transition: color 0.3s ease;
}

.tab-item.active {
  color: #1d1d1f;
}

.tab-indicator {
  position: absolute;
  bottom: -1px;
  left: 0;
  width: 50%;
  height: 2px;
  background: #1d1d1f;
  transition: transform 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.tab-indicator.slide-right {
  transform: translateX(100%);
}

/* 表单组 */
.input-group {
  margin-bottom: 20px;
}

.minimal-label {
  display: block;
  font-size: 13px;
  font-weight: 600;
  color: #1d1d1f;
  margin-bottom: 8px;
}

/* 极简输入框 - 扁平灰底色，获得焦点时变白加边框 */
:deep(.minimal-input .el-input__wrapper) {
  background: #f5f5f7;
  border: 1px solid transparent;
  border-radius: 12px;
  padding: 0 16px;
  height: 48px;
  box-shadow: none !important;
  transition: all 0.2s ease;
}

:deep(.minimal-input .el-input__wrapper:hover) {
  background: #ebebef;
}

:deep(.minimal-input .el-input__wrapper.is-focus) {
  background: #ffffff;
  border-color: #0066cc;
  /* 苹果蓝 */
  box-shadow: 0 0 0 3px rgba(0, 102, 204, 0.15) !important;
}

:deep(.minimal-input .el-input__inner) {
  font-size: 15px;
  color: #1d1d1f;
}

:deep(.minimal-input .el-input__inner::placeholder) {
  color: #86868b;
  font-weight: 400;
}

/* 验证码特殊布局 */
.captcha-flex {
  display: flex;
  gap: 12px;
}

.captcha-form-item {
  flex: 1;
  margin-bottom: 0;
}

.captcha-canvas-container {
  width: 110px;
  height: 48px;
  border-radius: 12px;
  overflow: hidden;
  background: #f5f5f7;
  cursor: pointer;
  border: 1px solid transparent;
  transition: border-color 0.2s;
  flex-shrink: 0;
}

.captcha-canvas-container:hover {
  border-color: #d1d1d6;
}

.captcha-canvas-container canvas {
  display: block;
  width: 100%;
  height: 100%;
}

/* 极简黑按钮 */
.minimal-submit-btn {
  width: 100%;
  height: 48px;
  border-radius: 12px;
  font-size: 16px;
  font-weight: 600;
  background: #1d1d1f;
  border: none;
  color: white;
  margin-top: 12px;
  transition: all 0.2s ease;
}

.minimal-submit-btn:hover {
  background: #424245;
}

.minimal-submit-btn:active {
  transform: scale(0.98);
}

.minimal-submit-btn:disabled {
  background: #e5e5ea;
  color: #a1a1a6;
  cursor: not-allowed;
  transform: none;
}

/* 错误提示文字 */
:deep(.el-form-item__error) {
  color: #ff3b30;
  /* 苹果红 */
  font-size: 12px;
  padding-top: 4px;
}

/* 表单切换动画 */
.fade-slide-enter-active,
.fade-slide-leave-active {
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.fade-slide-enter-from {
  opacity: 0;
  transform: translateY(10px);
}

.fade-slide-leave-to {
  opacity: 0;
  transform: translateY(-10px);
}
</style>