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
            <el-menu-item
              v-for="item in navMenu"
              :key="item.index"
              :index="item.index"
              @click="handleMenuClick(item)"
            >
              {{ item.label }}
            </el-menu-item>
          </el-menu>
        </el-col>

        <el-col :span="6" class="user-actions">
          <el-space :size="16">
            <template v-if="userStore.isLoggedIn">
              <el-dropdown trigger="hover">
                <el-button text class="user-btn">
                  <el-avatar :size="32" :src="userStore.user?.avatar" class="user-avatar">
                    <template #default>
                      {{ userInitial }}
                    </template>
                  </el-avatar>
                  <span class="user-name">{{ userStore.user?.nickname || userStore.user?.username }}</span>
                  <el-icon class="el-icon--right">
                    <ArrowDown />
                  </el-icon>
                </el-button>

                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item @click="router.push('/goal')">目标管理</el-dropdown-item>
                    <el-dropdown-item @click="router.push('/wrongbook')">错题本</el-dropdown-item>
                    <el-dropdown-item @click="router.push('/learningAnalysis')">学习分析</el-dropdown-item>
                    <el-dropdown-item @click="router.push('/profile')">个人中心</el-dropdown-item>
                    <el-dropdown-item @click="router.push('/face')">人脸录入</el-dropdown-item>
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

    <el-dialog
      v-model="showAuthDialog"
      title=""
      width="400px"
      :show-close="true"
      class="minimal-auth-dialog"
      center
      :close-on-click-modal="true"
      :modal-class="'minimal-modal-backdrop'"
      :append-to-body="false"
      @close="handleDialogClose"
    >
      <div class="auth-container">
        <div class="header-titles">
          <h2 class="main-title">{{ modalTitle }}</h2>
          <p class="sub-title">{{ modalSubTitle }}</p>
        </div>

        <div class="auth-tabs">
          <div class="tab-item" :class="{ active: activeTab === 'login' }" @click="switchTab('login')">
            <span>账号登录</span>
          </div>
          <div class="tab-item" :class="{ active: activeTab === 'face' }" @click="switchTab('face')">
            <span>刷脸登录</span>
          </div>
          <div class="tab-item" :class="{ active: activeTab === 'register' }" @click="switchTab('register')">
            <span>注册</span>
          </div>
          <div class="tab-indicator" :class="`slide-${activeTab}`"></div>
        </div>

        <div class="form-container">
          <transition name="fade-slide" mode="out-in">
            <div v-if="activeTab === 'login'" key="login" class="form-panel">
              <el-form
                ref="loginFormRef"
                :model="loginForm"
                :rules="loginRules"
                class="auth-form"
                @submit.prevent="handleLogin"
              >
                <div class="input-group">
                  <label class="minimal-label">账号</label>
                  <el-form-item prop="email">
                    <el-input
                      ref="firstLoginInputRef"
                      v-model="loginForm.email"
                      placeholder="请输入账号"
                      size="large"
                      :disabled="loading"
                      class="minimal-input"
                      clearable
                      @keyup.enter="handleLogin"
                    />
                  </el-form-item>
                </div>

                <div class="input-group">
                  <label class="minimal-label">密码</label>
                  <el-form-item prop="password">
                    <el-input
                      v-model="loginForm.password"
                      type="password"
                      placeholder="请输入密码"
                      show-password
                      size="large"
                      :disabled="loading"
                      class="minimal-input"
                      @keyup.enter="handleLogin"
                    />
                  </el-form-item>
                </div>

                <div class="input-group">
                  <label class="minimal-label">验证码</label>
                  <div class="captcha-flex">
                    <el-form-item prop="captcha" class="captcha-form-item">
                      <el-input
                        v-model="loginForm.captcha"
                        placeholder="4位字符"
                        size="large"
                        :disabled="loading"
                        class="minimal-input"
                        maxlength="4"
                        @keyup.enter="handleLogin"
                      />
                    </el-form-item>
                    <div class="captcha-canvas-container" title="点击刷新" @click="handleRefreshLoginCaptcha">
                      <canvas ref="loginCaptchaRef" width="110" height="48"></canvas>
                    </div>
                  </div>
                </div>

                <el-button
                  type="primary"
                  size="large"
                  class="minimal-submit-btn"
                  :loading="loading"
                  :disabled="!isLoginFormValid"
                  @click="handleLogin"
                >
                  <span v-if="!loading">登录</span>
                  <span v-else>登录中...</span>
                </el-button>
              </el-form>
            </div>

            <div v-else-if="activeTab === 'face'" key="face" class="form-panel face-login-panel">
              <div class="camera-wrapper">
                <video ref="videoRef" autoplay playsinline class="face-video"></video>
                <div class="scan-overlay">
                  <div class="scan-corner top-left"></div>
                  <div class="scan-corner top-right"></div>
                  <div class="scan-corner bottom-left"></div>
                  <div class="scan-corner bottom-right"></div>
                </div>
              </div>

              <el-button
                type="primary"
                size="large"
                class="minimal-submit-btn face-btn"
                :loading="loading"
                @click="handleFaceLogin"
              >
                <span v-if="!loading">立即刷脸登录</span>
                <span v-else>识别中...</span>
              </el-button>
              <p class="face-tips">请保证光线充足，并正对摄像头</p>
            </div>

            <div v-else key="register" class="form-panel">
              <el-form
                ref="registerFormRef"
                :model="registerForm"
                :rules="registerRules"
                class="auth-form"
                @submit.prevent="handleRegister"
              >
                <div class="input-group">
                  <label class="minimal-label">设置账号</label>
                  <el-form-item prop="email">
                    <el-input
                      ref="firstRegisterInputRef"
                      v-model="registerForm.email"
                      placeholder="至少2位字符"
                      size="large"
                      :disabled="loading"
                      class="minimal-input"
                      clearable
                      @input="validateEmailRealtime"
                    />
                  </el-form-item>
                </div>

                <div class="input-group">
                  <label class="minimal-label">设置密码</label>
                  <el-form-item prop="password">
                    <el-input
                      v-model="registerForm.password"
                      type="password"
                      placeholder="至少6位字符"
                      show-password
                      size="large"
                      :disabled="loading"
                      class="minimal-input"
                    />
                  </el-form-item>
                </div>

                <div class="input-group">
                  <label class="minimal-label">确认密码</label>
                  <el-form-item prop="confirmPassword">
                    <el-input
                      v-model="registerForm.confirmPassword"
                      type="password"
                      placeholder="请再次输入密码"
                      show-password
                      size="large"
                      :disabled="loading"
                      class="minimal-input"
                      @input="validatePasswordMatch"
                    />
                  </el-form-item>
                </div>

                <div class="input-group">
                  <label class="minimal-label">验证码</label>
                  <div class="captcha-flex">
                    <el-form-item prop="captcha" class="captcha-form-item">
                      <el-input
                        v-model="registerForm.captcha"
                        placeholder="4位字符"
                        size="large"
                        :disabled="loading"
                        class="minimal-input"
                        maxlength="4"
                        @keyup.enter="handleRegister"
                      />
                    </el-form-item>
                    <div class="captcha-canvas-container" title="点击刷新" @click="handleRefreshRegisterCaptcha">
                      <canvas ref="registerCaptchaRef" width="110" height="48"></canvas>
                    </div>
                  </div>
                </div>

                <el-button
                  type="primary"
                  size="large"
                  class="minimal-submit-btn"
                  :loading="loading"
                  :disabled="!isRegisterFormValid"
                  @click="handleRegister"
                >
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
import { loginAPI, registerAPI, faceLoginAPI } from '../utils/api'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const instance = getCurrentInstance()
const emitter = instance?.proxy?.$emitter || null

const showAuthDialog = ref(false)
const activeTab = ref('login')
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
const videoRef = ref(null)

const loginCaptchaCode = ref('')
const registerCaptchaCode = ref('')

let mediaStream = null

const loginForm = reactive({
  email: '',
  password: '',
  captcha: ''
})

const registerForm = reactive({
  email: '',
  password: '',
  confirmPassword: '',
  captcha: ''
})

const appTitle = computed(() => import.meta.env.VITE_APP_TITLE || '平台')

const navMenu = computed(() => {
  try {
    const menuConfig = import.meta.env.VITE_NAV_MENU
    if (menuConfig) {
      return JSON.parse(menuConfig)
    }
  } catch (error) {
    console.error('解析导航配置失败:', error)
  }
  return []
})

const userInitial = computed(() => {
  const name = userStore.user?.nickname || userStore.user?.username || '用户'
  return name.charAt(0).toUpperCase()
})

const modalTitle = computed(() => {
  if (activeTab.value === 'register') return '创建账户'
  if (activeTab.value === 'face') return '刷脸登录'
  return '欢迎登录'
})

const modalSubTitle = computed(() => {
  if (activeTab.value === 'register') return '加入平台，开始你的学习之旅'
  if (activeTab.value === 'face') return '请正对摄像头进行人脸识别'
  return '输入账号密码继续'
})

const generateRandomString = (length = 4) => {
  const chars = 'ABCDEFGHJKLMNPQRSTUVWXYZabcdefghjkmnpqrstuvwxyz23456789'
  let result = ''
  for (let i = 0; i < length; i += 1) {
    result += chars.charAt(Math.floor(Math.random() * chars.length))
  }
  return result
}

const drawCaptcha = (canvasElement, text) => {
  if (!canvasElement || !text) return

  const ctx = canvasElement.getContext('2d')
  const width = canvasElement.width
  const height = canvasElement.height

  ctx.clearRect(0, 0, width, height)
  ctx.fillStyle = '#f5f5f7'
  ctx.fillRect(0, 0, width, height)

  for (let i = 0; i < 4; i += 1) {
    ctx.strokeStyle = 'rgba(0, 0, 0, 0.1)'
    ctx.beginPath()
    ctx.moveTo(Math.random() * width, Math.random() * height)
    ctx.lineTo(Math.random() * width, Math.random() * height)
    ctx.stroke()
  }

  for (let i = 0; i < 20; i += 1) {
    ctx.fillStyle = 'rgba(0, 0, 0, 0.15)'
    ctx.beginPath()
    ctx.arc(Math.random() * width, Math.random() * height, 1, 0, 2 * Math.PI)
    ctx.fill()
  }

  ctx.font = 'bold 24px -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif'
  ctx.textBaseline = 'middle'
  for (let i = 0; i < text.length; i += 1) {
    ctx.fillStyle = '#1d1d1f'
    const x = 22 * i + 12
    const y = height / 2 + (Math.random() * 4 - 2)
    const deg = ((Math.random() * 40 - 20) * Math.PI) / 180

    ctx.translate(x, y)
    ctx.rotate(deg)
    ctx.fillText(text[i], 0, 0)
    ctx.rotate(-deg)
    ctx.translate(-x, -y)
  }
}

const refreshLoginCaptcha = () => {
  loginCaptchaCode.value = generateRandomString(4)
  drawCaptcha(loginCaptchaRef.value, loginCaptchaCode.value)
}

const refreshRegisterCaptcha = () => {
  registerCaptchaCode.value = generateRandomString(4)
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
  return (
    !!loginForm.email &&
    !!loginForm.password &&
    loginForm.password.length >= 6 &&
    loginForm.captcha.length === 4 &&
    loginForm.captcha.toLowerCase() === loginCaptchaCode.value.toLowerCase()
  )
})

const isRegisterFormValid = computed(() => {
  return (
    !!registerForm.email &&
    !!registerForm.password &&
    registerForm.password.length >= 6 &&
    registerForm.password === registerForm.confirmPassword &&
    registerForm.captcha.length === 4 &&
    registerForm.captcha.toLowerCase() === registerCaptchaCode.value.toLowerCase()
  )
})

const validateConfirmPassword = (rule, value, callback) => {
  if (value !== registerForm.password) {
    callback(new Error('两次输入的密码不一致'))
    return
  }
  callback()
}

const validateLoginCaptcha = (rule, value, callback) => {
  if (!value) {
    callback(new Error('请输入验证码'))
    return
  }
  if (value.toLowerCase() !== loginCaptchaCode.value.toLowerCase()) {
    callback(new Error('验证码错误'))
    return
  }
  callback()
}

const validateRegisterCaptcha = (rule, value, callback) => {
  if (!value) {
    callback(new Error('请输入验证码'))
    return
  }
  if (value.toLowerCase() !== registerCaptchaCode.value.toLowerCase()) {
    callback(new Error('验证码错误'))
    return
  }
  callback()
}

const loginRules = reactive({
  email: [{ required: true, message: '请输入账号', trigger: 'blur' }],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码不能少于6位', trigger: ['blur', 'change'] }
  ],
  captcha: [{ required: true, validator: validateLoginCaptcha, trigger: 'blur' }]
})

const registerRules = reactive({
  email: [
    { required: true, message: '请输入账号', trigger: 'blur' },
    { min: 2, message: '账号至少2位', trigger: ['blur', 'change'] }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度为6-20位', trigger: ['blur', 'change'] }
  ],
  confirmPassword: [
    { required: true, message: '请确认密码', trigger: 'blur' },
    { validator: validateConfirmPassword, trigger: ['blur', 'change'] }
  ],
  captcha: [{ required: true, validator: validateRegisterCaptcha, trigger: 'blur' }]
})

const startCamera = async () => {
  try {
    if (!navigator.mediaDevices || !navigator.mediaDevices.getUserMedia) {
      ElMessage.error('当前浏览器不支持摄像头调用')
      return
    }

    mediaStream = await navigator.mediaDevices.getUserMedia({
      video: { facingMode: 'user', width: 640, height: 480 }
    })

    const tryAttachStream = () => {
      if (videoRef.value) {
        videoRef.value.srcObject = mediaStream
      } else if (activeTab.value === 'face' && showAuthDialog.value) {
        setTimeout(tryAttachStream, 50)
      } else {
        stopCamera()
      }
    }

    tryAttachStream()
  } catch (error) {
    console.error('Camera access error:', error)
    ElMessage.error('无法访问摄像头，请检查浏览器权限设置')
  }
}

const stopCamera = () => {
  if (!mediaStream) return
  mediaStream.getTracks().forEach((track) => track.stop())
  mediaStream = null
}

const handleDialogClose = () => {
  stopCamera()
  resetForms()
  activeTab.value = 'login'
}

const focusFirstInput = () => {
  if (activeTab.value === 'login' && firstLoginInputRef.value) {
    firstLoginInputRef.value.focus()
  }
  if (activeTab.value === 'register' && firstRegisterInputRef.value) {
    firstRegisterInputRef.value.focus()
  }
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

const switchTab = (tabName) => {
  if (activeTab.value === tabName) {
    if (tabName === 'face' && showAuthDialog.value) {
      nextTick(() => startCamera())
    }
    return
  }

  if (activeTab.value === 'face' && tabName !== 'face') {
    stopCamera()
  }

  activeTab.value = tabName
  resetForms()

  nextTick(() => {
    if (tabName === 'login') {
      refreshLoginCaptcha()
      focusFirstInput()
    }
    if (tabName === 'register') {
      refreshRegisterCaptcha()
      focusFirstInput()
    }
    if (tabName === 'face' && showAuthDialog.value) {
      startCamera()
    }
  })
}

const openLoginDialog = () => {
  showAuthDialog.value = true
  switchTab('login')
  nextTick(() => {
    refreshLoginCaptcha()
    focusFirstInput()
  })
}

const openRegisterDialog = () => {
  showAuthDialog.value = true
  switchTab('register')
  nextTick(() => {
    refreshRegisterCaptcha()
    focusFirstInput()
  })
}

const handleMenuClick = (menuItem) => {
  const path = menuItem?.index
  if (!path) return

  if (path === '/' || path === '/home') {
    router.push(path)
    return
  }

  if (!userStore.isLoggedIn) {
    pendingRoute.value = path
    openLoginDialog()
    ElMessage({ message: '请先登录后再访问该页面', type: 'warning', duration: 2000 })
    return
  }

  router.push(path)
}

const loginSuccessHelper = (userData) => {
  userStore.login(userData)
  loginAttempts.value = 0
  showAuthDialog.value = false
  stopCamera()
  ElMessage({ message: `欢迎回来，${userData.nickname || userData.username || '用户'}`, type: 'success' })

  if (pendingRoute.value) {
    router.push(pendingRoute.value)
    pendingRoute.value = null
  }
}

const handleLogin = async () => {
  if (loginAttempts.value >= maxLoginAttempts) {
    ElMessage({ message: '尝试次数过多，请稍后重试', type: 'error' })
    return
  }
  if (!loginFormRef.value) return

  await loginFormRef.value.validate(async (valid) => {
    if (!valid) return

    loading.value = true
    try {
      const userData = await loginAPI(loginForm.email, loginForm.password)
      if (!userData) return
      loginSuccessHelper(userData)
    } catch (error) {
      loginAttempts.value += 1
      handleRefreshLoginCaptcha()
      ElMessage({ message: error.message || '登录失败，请检查账号和密码', type: 'error' })
    } finally {
      loading.value = false
    }
  })
}

const handleFaceLogin = async () => {
  if (!videoRef.value) return
  loading.value = true

  const canvas = document.createElement('canvas')
  canvas.width = videoRef.value.videoWidth
  canvas.height = videoRef.value.videoHeight
  const ctx = canvas.getContext('2d')
  ctx.drawImage(videoRef.value, 0, 0, canvas.width, canvas.height)

  const base64Image = canvas.toDataURL('image/jpeg', 0.8)
  try {
    const userData = await faceLoginAPI(base64Image)
    if (!userData) return
    loginSuccessHelper(userData)
  } catch (error) {
    ElMessage({ message: error.message || '未识别到有效人脸，请重试', type: 'error' })
  } finally {
    loading.value = false
  }
}

const handleRegister = async () => {
  if (!registerFormRef.value) return

  await registerFormRef.value.validate(async (valid) => {
    if (!valid) return

    loading.value = true
    try {
      const result = await registerAPI(registerForm.email, registerForm.password)
      if (result === true) {
        userStore.login({ id: Date.now(), username: registerForm.email, email: registerForm.email })
        showAuthDialog.value = false
        ElMessage({ message: '注册成功，欢迎加入', type: 'success' })

        if (pendingRoute.value) {
          router.push(pendingRoute.value)
          pendingRoute.value = null
        }
      }
    } catch (error) {
      handleRefreshRegisterCaptcha()
      let errorMessage = '注册失败，请稍后重试'
      if (error.message?.includes('重名') || error.message?.includes('已存在')) {
        errorMessage = '该账号已被注册'
      } else if (error.message) {
        errorMessage = error.message
      }
      ElMessage({ message: errorMessage, type: 'error' })
    } finally {
      loading.value = false
    }
  })
}

const logout = () => {
  userStore.logout()
  ElMessage({ message: '已安全退出登录', type: 'success' })
  router.push('/')
}

const validateEmailRealtime = () => {
  emailValidated.value = registerForm.email.trim().length >= 2
}

const validatePasswordMatch = () => {
  passwordMatched.value =
    registerForm.password === registerForm.confirmPassword && registerForm.confirmPassword.length > 0
}

const handleApiLoginRequest = () => {
  openLoginDialog()
}

onMounted(() => {
  if (emitter?.on) {
    emitter.on('openLoginFromFooter', (routePath) => {
      pendingRoute.value = routePath
      openLoginDialog()
    })
    emitter.on('openLoginFromHome', (routePath) => {
      pendingRoute.value = routePath
      openLoginDialog()
    })
    emitter.on('openRegisterFromHome', (routePath) => {
      pendingRoute.value = routePath
      openRegisterDialog()
    })
  }

  window.addEventListener('showLoginDialog', handleApiLoginRequest)
})

onBeforeUnmount(() => {
  if (emitter?.off) {
    emitter.off('openLoginFromFooter')
    emitter.off('openLoginFromHome')
    emitter.off('openRegisterFromHome')
  }

  window.removeEventListener('showLoginDialog', handleApiLoginRequest)
  stopCamera()
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

/* ==================== 鏋佺畝鑻规灉椋?寮圭獥鏍峰紡 ==================== */
:deep(.minimal-modal-backdrop) {
  background: rgba(0, 0, 0, 0.6);
  backdrop-filter: blur(4px);
}

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
}

.auth-container {
  padding: 40px 36px;
  background: #ffffff;
  min-height: 480px;
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif;
}

.header-titles {
  text-align: left;
  margin-bottom: 24px;
}

.main-title {
  font-size: 26px;
  font-weight: 700;
  color: #1d1d1f;
  margin: 0 0 8px 0;
  letter-spacing: -0.5px;
}

.sub-title {
  font-size: 14px;
  color: #86868b;
  margin: 0;
  font-weight: 400;
}

/* 鏋佺畝涓嬪垝绾?涓?Tabs */
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
  font-size: 14px;
  font-weight: 600;
  color: #86868b;
  cursor: pointer;
  transition: color 0.3s ease;
}

.tab-item.active {
  color: #1d1d1f;
}

/* 鎸囩ず鍣ㄥ钩鍒嗕负涓夌瓑浠?*/
.tab-indicator {
  position: absolute;
  bottom: -1px;
  left: 0;
  width: 33.333%;
  height: 2px;
  background: #1d1d1f;
  transition: transform 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.slide-login {
  transform: translateX(0);
}

.slide-face {
  transform: translateX(100%);
}

.slide-register {
  transform: translateX(200%);
}

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

:deep(.el-form-item__error) {
  color: #ff3b30;
  font-size: 12px;
  padding-top: 4px;
}

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

/* =========== 鍒疯劯鐧诲綍涓撳睘鏍峰紡 =========== */
.face-login-panel {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.camera-wrapper {
  position: relative;
  width: 240px;
  height: 240px;
  margin: 0 auto 20px auto;
  border-radius: 24px;
  overflow: hidden;
  background: #f5f5f7;
  box-shadow: inset 0 0 0 1px rgba(0, 0, 0, 0.05);
}

.face-video {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transform: scaleX(-1);
  /* 闀滃儚缈昏浆锛岀鍚堢収闀滃瓙鐩磋 */
}

.scan-overlay {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  pointer-events: none;
}

.scan-corner {
  position: absolute;
  width: 20px;
  height: 20px;
  border-color: #0066cc;
  border-style: solid;
}

.top-left {
  top: 15px;
  left: 15px;
  border-width: 3px 0 0 3px;
  border-top-left-radius: 8px;
}

.top-right {
  top: 15px;
  right: 15px;
  border-width: 3px 3px 0 0;
  border-top-right-radius: 8px;
}

.bottom-left {
  bottom: 15px;
  left: 15px;
  border-width: 0 0 3px 3px;
  border-bottom-left-radius: 8px;
}

.bottom-right {
  bottom: 15px;
  right: 15px;
  border-width: 0 3px 3px 0;
  border-bottom-right-radius: 8px;
}

.face-btn {
  margin-top: 0;
}

.face-tips {
  font-size: 12px;
  color: #86868b;
  margin-top: 16px;
  text-align: center;
}
</style>
