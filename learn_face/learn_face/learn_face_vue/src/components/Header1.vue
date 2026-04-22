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

    <el-dialog v-model="showAuthDialog" title="" width="420px" :show-close="false" class="modern-auth-dialog" center
      :close-on-click-modal="true" :modal-class="'auth-modal-backdrop'" :append-to-body="false">
      <div class="auth-container">
        <div class="auth-tabs">
          <div class="tab-item" :class="{ active: !isRegisterMode }" @click="switchToLogin">
            <span class="tab-icon">👋</span>
            <span>登录</span>
          </div>
          <div class="tab-item" :class="{ active: isRegisterMode }" @click="switchToRegister">
            <span class="tab-icon">🚀</span>
            <span>注册</span>
          </div>
          <div class="tab-indicator" :class="{ 'slide-right': isRegisterMode }"></div>
        </div>

        <div class="form-container">
          <transition name="form-slide" mode="out-in">
            <!-- 登录表单 -->
            <div v-if="!isRegisterMode" key="login" class="form-panel">
              <div class="form-title">
                <h2>欢迎回来</h2>
              </div>

              <el-form :model="loginForm" :rules="loginRules" ref="loginFormRef" class="auth-form"
                @submit.prevent="handleLogin">
                <div class="input-group">
                  <el-form-item prop="email">
                    <el-input ref="firstLoginInputRef" v-model="loginForm.email" placeholder="请输入账号" size="large" @keyup.enter="handleLogin"
                      :disabled="loading" clearable class="modern-input">
                      <template #prefix><el-icon class="input-icon"><UserIcon /></el-icon></template>
                    </el-input>
                  </el-form-item>
                </div>

                <div class="input-group">
                  <el-form-item prop="password">
                    <el-input v-model="loginForm.password" type="password" placeholder="密码" show-password size="large"
                      @keyup.enter="handleLogin" :disabled="loading" class="modern-input">
                      <template #prefix><el-icon class="input-icon"><Lock /></el-icon></template>
                    </el-input>
                  </el-form-item>
                </div>

                <!-- 登录验证码 -->
                <div class="input-group">
                  <div class="captcha-flex">
                    <el-form-item prop="captcha" class="captcha-form-item">
                      <el-input v-model="loginForm.captcha" placeholder="请输入验证码" size="large"
                        @keyup.enter="handleLogin" :disabled="loading" clearable class="modern-input" maxlength="4">
                        <template #prefix><el-icon class="input-icon"><Key /></el-icon></template>
                      </el-input>
                    </el-form-item>
                    <div class="captcha-canvas-container" @click="handleRefreshLoginCaptcha" title="看不清？点击刷新">
                      <canvas ref="loginCaptchaRef" width="120" height="52"></canvas>
                    </div>
                  </div>
                </div>

                <div class="form-options"></div>

                <el-button @click="handleLogin" :loading="loading" type="primary" size="large" class="submit-btn"
                  :disabled="!isLoginFormValid">
                  <span v-if="!loading">登录</span>
                  <span v-else>登录中...</span>
                </el-button>
              </el-form>
            </div>

            <!-- 注册表单 -->
            <div v-else key="register" class="form-panel">
              <div class="form-title">
                <h2>创建账户</h2>
              </div>

              <el-form :model="registerForm" :rules="registerRules" ref="registerFormRef"
                class="auth-form" @submit.prevent="handleRegister">
                <div class="input-group enhanced">
                  <label class="input-label">账号</label>
                  <el-form-item prop="email">
                    <el-input ref="firstRegisterInputRef" v-model="registerForm.email" placeholder="请输入任意账号" size="large"
                      :disabled="loading" clearable class="modern-input enhanced-input" @input="validateEmailRealtime">
                      <template #prefix><el-icon class="input-icon"><UserIcon /></el-icon></template>
                      <template #suffix><el-icon v-if="emailValidated" class="success-icon">✓</el-icon></template>
                    </el-input>
                  </el-form-item>
                </div>

                <div class="input-group enhanced">
                  <label class="input-label">密码</label>
                  <el-form-item prop="password">
                    <el-input v-model="registerForm.password" type="password" placeholder="至少6位字符" show-password
                      size="large" :disabled="loading" class="modern-input enhanced-input">
                      <template #prefix><el-icon class="input-icon"><Lock /></el-icon></template>
                    </el-input>
                  </el-form-item>
                </div>

                <div class="input-group enhanced">
                  <label class="input-label">确认密码</label>
                  <el-form-item prop="confirmPassword">
                    <el-input v-model="registerForm.confirmPassword" type="password" placeholder="再次输入密码" show-password
                      size="large" :disabled="loading" class="modern-input enhanced-input"
                      @input="validatePasswordMatch">
                      <template #prefix><el-icon class="input-icon"><Lock /></el-icon></template>
                      <template #suffix>
                        <el-icon v-if="passwordMatched" class="success-icon">✓</el-icon>
                        <el-icon v-else-if="registerForm.confirmPassword && !passwordMatched" class="error-icon">✗</el-icon>
                      </template>
                    </el-input>
                  </el-form-item>
                </div>

                <!-- 注册验证码 -->
                <div class="input-group enhanced">
                  <label class="input-label">图形验证码</label>
                  <div class="captcha-flex">
                    <el-form-item prop="captcha" class="captcha-form-item">
                      <el-input v-model="registerForm.captcha" placeholder="请输入右侧字母" size="large"
                        @keyup.enter="handleRegister" :disabled="loading" clearable class="modern-input enhanced-input" maxlength="4">
                        <template #prefix><el-icon class="input-icon"><Key /></el-icon></template>
                      </el-input>
                    </el-form-item>
                    <div class="captcha-canvas-container" @click="handleRefreshRegisterCaptcha" title="看不清？点击刷新">
                      <canvas ref="registerCaptchaRef" width="120" height="56"></canvas>
                    </div>
                  </div>
                </div>

                <div class="submit-section">
                  <el-button @click="handleRegister" :loading="loading" type="primary" size="large"
                    class="submit-btn enhanced-submit" :disabled="!isRegisterFormValid">
                    <span v-if="!loading" class="submit-content">
                      <el-icon class="submit-icon">🚀</el-icon>
                      <span>创建账户</span>
                    </span>
                    <span v-else class="loading-content">
                      <span class="loading-dots">创建中</span>
                    </span>
                  </el-button>
                </div>
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
import { ElMessage, ElMessageBox } from 'element-plus'
import { User as UserIcon, Lock, ArrowDown, Key } from '@element-plus/icons-vue' // 引入了 Key 图标
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

// DOM Refs
const loginFormRef = ref(null)
const registerFormRef = ref(null)
const firstLoginInputRef = ref(null)
const firstRegisterInputRef = ref(null)
const loginCaptchaRef = ref(null)
const registerCaptchaRef = ref(null)

// 验证码数据源
const loginCaptchaCode = ref('')
const registerCaptchaCode = ref('')

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
    if (menuConfig) return JSON.parse(menuConfig)
  } catch (error) {
    console.error('解析导航菜单配置失败:', error)
  }
  return []
})

const userInitial = computed(() => {
  const name = userStore.user?.nickname || userStore.user?.username || '用户'
  return name.charAt(0).toUpperCase()
})

// --- 纯前端验证码生成核心逻辑 ---
const generateRandomString = (length = 4) => {
  // 排除掉容易混淆的 o O 0 i I l 1
  const chars = 'ABCDEFGHJKLMNPQRSTUVWXYZabcdefghjkmnpqrstuvwxyz23456789'
  let result = ''
  for (let i = 0; i < length; i++) {
    result += chars.charAt(Math.floor(Math.random() * chars.length))
  }
  return result
}

const drawCaptcha = (canvasElement, text) => {
  if (!canvasElement) return
  const ctx = canvasElement.getContext('2d')
  const width = canvasElement.width
  const height = canvasElement.height

  ctx.clearRect(0, 0, width, height)
  
  // 绘制背景
  ctx.fillStyle = '#f8fafc'
  ctx.fillRect(0, 0, width, height)

  // 绘制干扰线
  for (let i = 0; i < 5; i++) {
    ctx.strokeStyle = `rgba(${Math.random() * 200},${Math.random() * 200},${Math.random() * 200},0.4)`
    ctx.beginPath()
    ctx.moveTo(Math.random() * width, Math.random() * height)
    ctx.lineTo(Math.random() * width, Math.random() * height)
    ctx.stroke()
  }

  // 绘制干扰点
  for (let i = 0; i < 30; i++) {
    ctx.fillStyle = `rgba(${Math.random() * 200},${Math.random() * 200},${Math.random() * 200},0.5)`
    ctx.beginPath()
    ctx.arc(Math.random() * width, Math.random() * height, 1, 0, 2 * Math.PI)
    ctx.fill()
  }

  // 绘制文字
  ctx.font = 'bold 26px Arial, sans-serif'
  ctx.textBaseline = 'middle'
  for (let i = 0; i < text.length; i++) {
    ctx.fillStyle = `rgb(${Math.random() * 100 + 30},${Math.random() * 100 + 30},${Math.random() * 100 + 30})`
    const x = 24 * i + 15
    const y = height / 2 + (Math.random() * 6 - 3)
    const deg = (Math.random() * 60 - 30) * Math.PI / 180
    
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

// 用户手动点击验证码图刷新
const handleRefreshLoginCaptcha = () => {
  refreshLoginCaptcha()
  loginForm.captcha = ''
}

const handleRefreshRegisterCaptcha = () => {
  refreshRegisterCaptcha()
  registerForm.captcha = ''
}

// --- 表单提交状态拦截（带上验证码校验） ---
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

// --- 表单规则 ---
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
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码长度不能少于6位', trigger: ['blur', 'change'] }
  ],
  captcha: [
    { required: true, validator: validateLoginCaptcha, trigger: 'blur' }
  ]
})

const registerRules = reactive({
  email: [
    { required: true, message: '请输入账号', trigger: 'blur' },
    { min: 2, message: '账号长度至少2位', trigger: ['blur', 'change'] }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度应在6-20位之间', trigger: ['blur', 'change'] }
  ],
  confirmPassword: [
    { required: true, message: '请确认密码', trigger: 'blur' },
    { validator: validateConfirmPassword, trigger: ['blur', 'change'] }
  ],
  captcha: [
    { required: true, validator: validateRegisterCaptcha, trigger: 'blur' }
  ]
})

// --- 方法逻辑 ---
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
  if (!isRegisterMode.value && firstLoginInputRef.value) {
    firstLoginInputRef.value.focus()
  } else if (isRegisterMode.value && firstRegisterInputRef.value) {
    firstRegisterInputRef.value.focus()
  }
}

// 弹窗状态管理（等待 transition 动画结束后渲染验证码）
const openLoginDialog = () => {
  isRegisterMode.value = false
  showAuthDialog.value = true
  resetForms()
  setTimeout(() => {
    refreshLoginCaptcha()
    focusFirstInput()
  }, 100)
}

const openRegisterDialog = () => {
  isRegisterMode.value = true
  showAuthDialog.value = true
  resetForms()
  setTimeout(() => {
    refreshRegisterCaptcha()
    focusFirstInput()
  }, 100)
}

const switchToLogin = () => {
  if (isRegisterMode.value) {
    isRegisterMode.value = false
    resetForms()
    setTimeout(() => {
      refreshLoginCaptcha()
      focusFirstInput()
    }, 450) // 必须等待 mode="out-in" 动画结束
  }
}

const switchToRegister = () => {
  if (!isRegisterMode.value) {
    isRegisterMode.value = true
    resetForms()
    setTimeout(() => {
      refreshRegisterCaptcha()
      focusFirstInput()
    }, 450)
  }
}

const handleLogin = async () => {
  if (loginAttempts.value >= maxLoginAttempts) {
    ElMessage({ message: '登录尝试次数过多，请稍后再试', type: 'error' })
    return
  }

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

        ElMessage({
          message: `🎉 登录成功！欢迎回来，${userData.nickname || userData.username || '用户'}！`,
          type: 'success',
          duration: 3000
        })

        if (pendingRoute.value) {
          router.push(pendingRoute.value)
          pendingRoute.value = null
        }
      } catch (error) {
        loginAttempts.value++
        // --- 登录失败自动刷新验证码 ---
        handleRefreshLoginCaptcha()
        
        ElMessage({
          message: error.message || '登录失败，请检查账号和密码',
          type: 'error',
          duration: 3000
        })
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
          userStore.login({
            id: Date.now(),
            username: registerForm.email,
            email: registerForm.email
          })

          showAuthDialog.value = false
          ElMessage({ message: '🎉 注册成功！欢迎加入！', type: 'success', duration: 3000 })

          if (pendingRoute.value) {
            router.push(pendingRoute.value)
            pendingRoute.value = null
          }
        }
      } catch (error) {
        // --- 注册失败自动刷新验证码 ---
        handleRefreshRegisterCaptcha()

        let errorMessage = '注册失败，请稍后重试'
        if (error.message === '用户名重复!') {
          errorMessage = '该账号已被注册，请使用其他账号或直接登录'
        } else if (error.message) {
          errorMessage = error.message
        }
        ElMessage({ message: errorMessage, type: 'error', duration: 3000 })
      } finally {
        loading.value = false
      }
    }
  })
}

const validateEmailRealtime = () => {
  emailValidated.value = registerForm.email.trim().length >= 2
}

const validatePasswordMatch = () => {
  passwordMatched.value = registerForm.password === registerForm.confirmPassword && registerForm.confirmPassword.length > 0
}

const handleApiLoginRequest = () => openLoginDialog()

onMounted(() => {
  if (emitter) {
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
  if (emitter) {
    emitter.off('openLoginFromFooter')
    emitter.off('openLoginFromHome')
    emitter.off('openRegisterFromHome')
  }
  window.removeEventListener('showLoginDialog', handleApiLoginRequest)
})
</script>

<style scoped>
/* =========== 这里添加了验证码模块的 CSS =========== */
.captcha-flex {
  display: flex;
  gap: 12px;
  align-items: flex-start;
}

.captcha-form-item {
  flex: 1;
  margin-bottom: 0;
}

.captcha-canvas-container {
  width: 120px;
  border-radius: 12px;
  overflow: hidden;
  border: 1.5px solid rgba(226, 232, 240, 0.8);
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  background: rgba(255, 255, 255, 0.8);
  flex-shrink: 0;
}

.captcha-canvas-container canvas {
  display: block;
  width: 100%;
  height: 100%;
}

.captcha-canvas-container:hover {
  border-color: rgba(99, 102, 241, 0.4);
  transform: translateY(-2px);
  box-shadow: 0 4px 8px rgba(0, 0, 0, 0.08);
}
/* =========== 原有 CSS 保持不变下面 =========== */

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

.logo-icon {
  font-size: 28px;
  animation: rotate 20s linear infinite;
}

.logo h1 {
  color: #ffffff;
  font-size: 24px;
  font-weight: 700;
  margin: 0;
  letter-spacing: -0.5px;
}

@keyframes rotate {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
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

.nav-menu :deep(.el-menu-item:hover) {
  color: #ffffff;
  background: rgba(255, 255, 255, 0.1);
  border-radius: 8px;
}

.nav-menu :deep(.el-menu-item.is-active) {
  color: #ffffff;
  background: rgba(255, 255, 255, 0.15);
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

.user-btn:hover .user-avatar {
  border-color: rgba(255, 255, 255, 0.4);
  transform: scale(1.05);
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
  color: #ffffff;
  background: rgba(255, 255, 255, 0.1) !important;
  border-color: rgba(255, 255, 255, 0.8);
}

.register-btn {
  background: #ffffff !important;
  color: #000000;
  border: none;
}

.register-btn:hover {
  background: #f8f9fa !important;
  color: #000000 !important;
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  border: 1px solid #e9ecef;
}

:deep(.el-dropdown-menu) {
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  border: 1px solid #e5e7eb;
  background: #ffffff;
  margin-top: 8px;
}

:deep(.el-dropdown-menu__item) {
  padding: 10px 16px;
  font-size: 14px;
  color: #000000;
  transition: all 0.3s ease;
}

:deep(.el-dropdown-menu__item:hover) {
  background: rgba(0, 0, 0, 0.05);
  color: #000000;
}

.input-group.enhanced {
  margin-bottom: 28px;
  position: relative;
}

.input-label {
  display: block;
  font-size: 14px;
  font-weight: 600;
  color: #374151;
  margin-bottom: 8px;
  padding-left: 4px;
}

:deep(.enhanced-input .el-input__wrapper) {
  background: rgba(255, 255, 255, 0.9);
  border: 2px solid rgba(226, 232, 240, 0.8);
  border-radius: 14px;
  padding: 0 18px;
  height: 56px;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  box-shadow:
    0 2px 4px rgba(0, 0, 0, 0.04),
    inset 0 1px 0 rgba(255, 255, 255, 0.9);
}

:deep(.enhanced-input .el-input__wrapper:hover) {
  border-color: rgba(99, 102, 241, 0.5);
  background: rgba(255, 255, 255, 0.95);
  box-shadow:
    0 4px 8px rgba(0, 0, 0, 0.06),
    inset 0 1px 0 rgba(255, 255, 255, 1);
  transform: translateY(-1px);
}

:deep(.enhanced-input .el-input__wrapper.is-focus) {
  border-color: #6366f1;
  background: rgba(255, 255, 255, 1);
  box-shadow:
    0 0 0 4px rgba(99, 102, 241, 0.12),
    0 6px 16px rgba(0, 0, 0, 0.08),
    inset 0 1px 0 rgba(255, 255, 255, 1);
  transform: translateY(-2px);
}

.success-icon {
  color: #10b981;
  font-size: 18px;
  font-weight: bold;
}

.error-icon {
  color: #ef4444;
  font-size: 18px;
  font-weight: bold;
}

.submit-section {
  text-align: center;
}

.enhanced-submit {
  width: 100%;
  height: 56px;
  border-radius: 14px;
  font-size: 16px;
  font-weight: 700;
  background: linear-gradient(135deg, #6366f1 0%, #8b5cf6 50%, #ec4899 100%);
  background-size: 200% 100%;
  border: none;
  color: white;
  cursor: pointer;
  transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
  box-shadow:
    0 8px 20px rgba(99, 102, 241, 0.3),
    inset 0 1px 0 rgba(255, 255, 255, 0.2);
  position: relative;
  overflow: hidden;
  animation: gradientShift 3s ease-in-out infinite;
}

@keyframes gradientShift {
  0%, 100% { background-position: 0% 50%; }
  50% { background-position: 100% 50%; }
}

.enhanced-submit:hover {
  transform: translateY(-3px);
  box-shadow:
    0 12px 28px rgba(99, 102, 241, 0.4),
    inset 0 1px 0 rgba(255, 255, 255, 0.3);
  animation-duration: 1.5s;
}

.enhanced-submit:active {
  transform: translateY(-1px);
}

.enhanced-submit:disabled {
  opacity: 0.6;
  cursor: not-allowed;
  transform: none !important;
  animation: none;
}

.submit-content {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
}

.submit-icon {
  font-size: 18px;
}

.loading-content {
  display: flex;
  align-items: center;
  justify-content: center;
}

.loading-dots::after {
  content: '';
  animation: dots 1.5s infinite;
}

@keyframes dots {
  0%, 20% { content: ''; }
  40% { content: '.'; }
  60% { content: '..'; }
  80%, 100% { content: '...'; }
}

:deep(.auth-modal-backdrop) {
  background: rgba(0, 0, 0, 0.75);
  backdrop-filter: blur(20px) saturate(180%);
  -webkit-backdrop-filter: blur(20px) saturate(180%);
}

:deep(.modern-auth-dialog) {
  border-radius: 24px;
  overflow: hidden;
}

:deep(.modern-auth-dialog .el-dialog) {
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(40px) saturate(200%);
  -webkit-backdrop-filter: blur(40px) saturate(200%);
  border-radius: 24px;
  border: 1px solid rgba(255, 255, 255, 0.2);
  box-shadow:
    0 32px 64px rgba(0, 0, 0, 0.2),
    0 0 0 1px rgba(255, 255, 255, 0.1),
    inset 0 1px 0 rgba(255, 255, 255, 0.8);
  margin: 0;
  padding: 0;
  position: relative;
  overflow: hidden;
}

:deep(.modern-auth-dialog .el-dialog__header) {
  display: none;
}

:deep(.modern-auth-dialog .el-dialog__body) {
  padding: 0;
  margin: 0;
}

.auth-container {
  position: relative;
  padding: 48px;
  min-height: 520px;
  background:
    radial-gradient(circle at 30% 20%, rgba(99, 102, 241, 0.03) 0%, transparent 50%),
    radial-gradient(circle at 70% 80%, rgba(236, 72, 153, 0.03) 0%, transparent 50%),
    linear-gradient(135deg, rgba(255, 255, 255, 0.9) 0%, rgba(248, 250, 252, 0.9) 100%);
}

.auth-tabs {
  position: relative;
  display: flex;
  background: rgba(248, 250, 252, 0.8);
  border-radius: 16px;
  padding: 4px;
  margin-bottom: 40px;
  border: 1px solid rgba(226, 232, 240, 0.5);
}

.tab-item {
  flex: 1;
  text-align: center;
  padding: 14px 0;
  font-size: 15px;
  font-weight: 600;
  color: #64748b;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  border-radius: 12px;
  position: relative;
  z-index: 2;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
}

.tab-item.active {
  color: #1e293b;
}

.tab-icon {
  font-size: 16px;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.tab-item:hover .tab-icon {
  transform: scale(1.1);
}

.tab-indicator {
  position: absolute;
  top: 4px;
  left: 4px;
  width: calc(50% - 4px);
  height: calc(100% - 8px);
  background: #ffffff;
  border-radius: 12px;
  box-shadow:
    0 2px 8px rgba(0, 0, 0, 0.1),
    0 1px 2px rgba(0, 0, 0, 0.05);
  transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
  z-index: 1;
}

.tab-indicator.slide-right {
  transform: translateX(100%);
}

.form-container {
  position: relative;
  overflow: hidden;
}

.form-panel {
  width: 100%;
}

.form-title {
  text-align: center;
  margin-bottom: 36px;
}

.form-title h2 {
  font-size: 28px;
  font-weight: 700;
  color: #0f172a;
  margin: 0 0 12px 0;
  letter-spacing: -0.5px;
}

.form-title p {
  font-size: 15px;
  color: #64748b;
  margin: 0;
  font-weight: 500;
  line-height: 1.5;
}

.input-group {
  margin-bottom: 24px;
}

:deep(.modern-input .el-input__wrapper) {
  background: rgba(255, 255, 255, 0.8);
  border: 1.5px solid rgba(226, 232, 240, 0.8);
  border-radius: 12px;
  padding: 0 16px;
  height: 52px;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
}

:deep(.modern-input .el-input__wrapper:hover) {
  border-color: rgba(99, 102, 241, 0.4);
  background: rgba(255, 255, 255, 0.9);
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.08);
}

:deep(.modern-input .el-input__wrapper.is-focus) {
  border-color: #6366f1;
  background: rgba(255, 255, 255, 0.95);
  box-shadow:
    0 0 0 3px rgba(99, 102, 241, 0.1),
    0 2px 8px rgba(0, 0, 0, 0.1);
}

:deep(.modern-input .el-input__inner) {
  font-size: 15px;
  font-weight: 500;
  color: #0f172a;
  height: 100%;
}

:deep(.modern-input .el-input__inner::placeholder) {
  color: #94a3b8;
  font-weight: 400;
}

.input-icon {
  color: #94a3b8;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  font-size: 18px;
}

:deep(.modern-input .el-input__wrapper.is-focus) .input-icon {
  color: #6366f1;
}

.form-options {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 32px;
  font-size: 14px;
}

:deep(.el-checkbox__input.is-checked .el-checkbox__inner) {
  background: #6366f1;
  border-color: #6366f1;
}

:deep(.el-checkbox__inner) {
  border-radius: 4px;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.forgot-link {
  color: #6366f1;
  text-decoration: none;
  font-weight: 600;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  padding: 4px 8px;
  border-radius: 6px;
}

.forgot-link:hover {
  color: #4f46e5;
  background: rgba(99, 102, 241, 0.05);
}

.submit-btn {
  width: 100%;
  height: 52px;
  border-radius: 12px;
  font-size: 16px;
  font-weight: 600;
  background: linear-gradient(135deg, #6366f1 0%, #8b5cf6 100%);
  border: none;
  color: white;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  box-shadow: 0 4px 12px rgba(99, 102, 241, 0.3);
  position: relative;
  overflow: hidden;
}

.submit-btn::before {
  content: '';
  position: absolute;
  top: 0;
  left: -100%;
  width: 100%;
  height: 100%;
  background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.2), transparent);
  transition: left 0.5s ease;
}

.submit-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 20px rgba(99, 102, 241, 0.4);
}

.submit-btn:hover::before {
  left: 100%;
}

.submit-btn:active {
  transform: translateY(0);
  box-shadow: 0 4px 12px rgba(99, 102, 241, 0.3);
}

.submit-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
  transform: none !important;
  box-shadow: 0 2px 8px rgba(99, 102, 241, 0.2) !important;
}

.submit-btn:disabled::before {
  display: none;
}

.form-slide-enter-active,
.form-slide-leave-active {
  transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
}

.form-slide-enter-from {
  opacity: 0;
  transform: translateX(30px) scale(0.98);
}

.form-slide-leave-to {
  opacity: 0;
  transform: translateX(-30px) scale(0.98);
}

:deep(.el-form-item.is-error .modern-input .el-input__wrapper) {
  border-color: #ef4444;
  background: rgba(254, 242, 242, 0.8);
  box-shadow:
    0 0 0 3px rgba(239, 68, 68, 0.1),
    0 2px 6px rgba(239, 68, 68, 0.1);
}

:deep(.el-form-item__error) {
  color: #ef4444;
  font-size: 13px;
  font-weight: 500;
  margin-top: 6px;
  padding-left: 4px;
}
</style>