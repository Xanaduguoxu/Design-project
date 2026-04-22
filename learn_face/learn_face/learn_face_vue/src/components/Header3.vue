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
              <el-button @click="openRegisterDialog" type="primary" class="auth-btn register-btn">免费注册</el-button>
            </template>
          </el-space>
        </el-col>
      </el-row>
    </div>

    <!-- 现代门户风登录弹窗 -->
    <el-dialog v-model="showAuthDialog" title="" width="420px" :show-close="false" class="portal-auth-dialog" center
      :close-on-click-modal="true" :modal-class="'portal-modal-backdrop'" :append-to-body="false">
      
      <div class="portal-container">
        
        <!-- 关闭按钮 (右上角内嵌) -->
        <button class="close-btn" @click="showAuthDialog = false">
          <el-icon><Close /></el-icon>
        </button>

        <!-- 顶部标题 -->
        <div class="portal-header">
          <div class="logo-icon-wrapper">
            <el-icon class="brand-icon"><Platform /></el-icon>
          </div>
          <h2 class="main-title">欢迎来到{{ appTitle }}</h2>
          <p class="sub-title">连接发现更多可能</p>
        </div>

        <!-- 胶囊滑动式 Tab 切换 -->
        <div class="portal-tabs-wrapper">
          <div class="portal-tabs">
            <div class="tab-slider" :class="{ 'is-right': isRegisterMode }"></div>
            <div class="tab-item" :class="{ active: !isRegisterMode }" @click="switchToLogin">账号登录</div>
            <div class="tab-item" :class="{ active: isRegisterMode }" @click="switchToRegister">注册新账号</div>
          </div>
        </div>

        <div class="form-container">
          <transition name="portal-slide" mode="out-in">
            <!-- 登录表单 -->
            <div v-if="!isRegisterMode" key="login" class="form-panel">
              <el-form :model="loginForm" :rules="loginRules" ref="loginFormRef" @submit.prevent="handleLogin">
                
                <el-form-item prop="email" class="portal-form-item">
                  <el-input ref="firstLoginInputRef" v-model="loginForm.email" placeholder="请输入账号" size="large" @keyup.enter="handleLogin" :disabled="loading" class="portal-input" clearable>
                    <template #prefix><el-icon class="input-icon"><UserIcon /></el-icon></template>
                  </el-input>
                </el-form-item>

                <el-form-item prop="password" class="portal-form-item">
                  <el-input v-model="loginForm.password" type="password" placeholder="请输入密码" show-password size="large" @keyup.enter="handleLogin" :disabled="loading" class="portal-input">
                    <template #prefix><el-icon class="input-icon"><Lock /></el-icon></template>
                  </el-input>
                </el-form-item>

                <div class="captcha-group">
                  <el-form-item prop="captcha" class="portal-form-item captcha-input">
                    <el-input v-model="loginForm.captcha" placeholder="输入验证码" size="large" @keyup.enter="handleLogin" :disabled="loading" class="portal-input" maxlength="4">
                      <template #prefix><el-icon class="input-icon"><Key /></el-icon></template>
                    </el-input>
                  </el-form-item>
                  <div class="captcha-box" @click="handleRefreshLoginCaptcha" title="看不清？点击刷新">
                    <canvas ref="loginCaptchaRef" width="110" height="44"></canvas>
                  </div>
                </div>

                <div class="form-options">
                  <el-checkbox v-model="rememberMe" class="portal-checkbox">记住我</el-checkbox>
                  <a href="#" class="forgot-link" @click.prevent="handleForgotPassword">忘记密码？</a>
                </div>

                <el-button @click="handleLogin" :loading="loading" type="primary" size="large" class="portal-submit-btn" :disabled="!isLoginFormValid">
                  <span v-if="!loading">登 录</span>
                  <span v-else>登录中...</span>
                </el-button>
              </el-form>
            </div>

            <!-- 注册表单 -->
            <div v-else key="register" class="form-panel">
              <el-form :model="registerForm" :rules="registerRules" ref="registerFormRef" @submit.prevent="handleRegister">
                
                <el-form-item prop="email" class="portal-form-item">
                  <el-input ref="firstRegisterInputRef" v-model="registerForm.email" placeholder="设置您的账号" size="large" :disabled="loading" class="portal-input" @input="validateEmailRealtime" clearable>
                    <template #prefix><el-icon class="input-icon"><UserIcon /></el-icon></template>
                    <template #suffix><el-icon v-if="emailValidated" class="success-icon"><Select /></el-icon></template>
                  </el-input>
                </el-form-item>

                <el-form-item prop="password" class="portal-form-item">
                  <el-input v-model="registerForm.password" type="password" placeholder="设置密码 (至少6位)" show-password size="large" :disabled="loading" class="portal-input">
                    <template #prefix><el-icon class="input-icon"><Lock /></el-icon></template>
                  </el-input>
                </el-form-item>

                <el-form-item prop="confirmPassword" class="portal-form-item">
                  <el-input v-model="registerForm.confirmPassword" type="password" placeholder="请再次输入密码确认" show-password size="large" :disabled="loading" class="portal-input" @input="validatePasswordMatch">
                    <template #prefix><el-icon class="input-icon"><Lock /></el-icon></template>
                    <template #suffix>
                      <el-icon v-if="passwordMatched" class="success-icon"><Select /></el-icon>
                      <el-icon v-else-if="registerForm.confirmPassword && !passwordMatched" class="error-icon"><CloseBold /></el-icon>
                    </template>
                  </el-input>
                </el-form-item>

                <div class="captcha-group">
                  <el-form-item prop="captcha" class="portal-form-item captcha-input">
                    <el-input v-model="registerForm.captcha" placeholder="输入验证码" size="large" @keyup.enter="handleRegister" :disabled="loading" class="portal-input" maxlength="4">
                      <template #prefix><el-icon class="input-icon"><Key /></el-icon></template>
                    </el-input>
                  </el-form-item>
                  <div class="captcha-box" @click="handleRefreshRegisterCaptcha" title="看不清？点击刷新">
                    <canvas ref="registerCaptchaRef" width="110" height="44"></canvas>
                  </div>
                </div>

                <el-button @click="handleRegister" :loading="loading" type="primary" size="large" class="portal-submit-btn" :disabled="!isRegisterFormValid">
                  <span v-if="!loading">同意协议并注册</span>
                  <span v-else>注册中...</span>
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
import { ElMessage, ElMessageBox } from 'element-plus'
// 导入需要的图标
import { User as UserIcon, Lock, ArrowDown, Key, Close, Platform, Select, CloseBold } from '@element-plus/icons-vue'
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
const rememberMe = ref(false) // 记住我
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

const appTitle = computed(() => import.meta.env.VITE_APP_TITLE || '门户平台')

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

// 门户标准的清晰验证码生成
const drawCaptcha = (canvasElement, text) => {
  if (!canvasElement) return
  const ctx = canvasElement.getContext('2d')
  const width = canvasElement.width
  const height = canvasElement.height

  ctx.clearRect(0, 0, width, height)
  
  // 浅灰背景，干净护眼
  ctx.fillStyle = '#F3F4F6'
  ctx.fillRect(0, 0, width, height)

  // 柔和的干扰线
  for (let i = 0; i < 4; i++) {
    ctx.strokeStyle = `rgba(${Math.random() * 150},${Math.random() * 150},${Math.random() * 150},0.2)`
    ctx.beginPath()
    ctx.moveTo(Math.random() * width, Math.random() * height)
    ctx.lineTo(Math.random() * width, Math.random() * height)
    ctx.stroke()
  }

  // 柔和的干扰点
  for (let i = 0; i < 20; i++) {
    ctx.fillStyle = `rgba(${Math.random() * 150},${Math.random() * 150},${Math.random() * 150},0.3)`
    ctx.beginPath()
    ctx.arc(Math.random() * width, Math.random() * height, 1, 0, 2 * Math.PI)
    ctx.fill()
  }

  // 字体清晰
  ctx.font = 'bold 22px Arial, sans-serif'
  ctx.textBaseline = 'middle'
  for (let i = 0; i < text.length; i++) {
    ctx.fillStyle = `rgb(${Math.random() * 100},${Math.random() * 100},${Math.random() * 100})`
    const x = 20 * i + 14
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

const handleRefreshLoginCaptcha = () => { refreshLoginCaptcha(); loginForm.captcha = '' }
const handleRefreshRegisterCaptcha = () => { refreshRegisterCaptcha(); registerForm.captcha = '' }

const isLoginFormValid = computed(() => {
  return loginForm.email && loginForm.password.length >= 6 &&
    loginForm.captcha.length === 4 && loginForm.captcha.toLowerCase() === loginCaptchaCode.value.toLowerCase()
})

const isRegisterFormValid = computed(() => {
  return registerForm.email && registerForm.password &&
    registerForm.password === registerForm.confirmPassword &&
    registerForm.captcha.length === 4 && registerForm.captcha.toLowerCase() === registerCaptchaCode.value.toLowerCase()
})

const validateConfirmPassword = (rule, value, callback) => {
  if (value !== registerForm.password) callback(new Error('两次输入的密码不一致'))
  else callback()
}
const validateLoginCaptcha = (rule, value, callback) => {
  if (!value) callback(new Error('请输入验证码'))
  else if (value.toLowerCase() !== loginCaptchaCode.value.toLowerCase()) callback(new Error('验证码错误'))
  else callback()
}
const validateRegisterCaptcha = (rule, value, callback) => {
  if (!value) callback(new Error('请输入验证码'))
  else if (value.toLowerCase() !== registerCaptchaCode.value.toLowerCase()) callback(new Error('验证码错误'))
  else callback()
}

const loginRules = reactive({
  email: [{ required: true, message: '请输入账号', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }, { min: 6, message: '密码不能少于6位', trigger: ['blur', 'change'] }],
  captcha: [{ required: true, validator: validateLoginCaptcha, trigger: 'blur' }]
})

const registerRules = reactive({
  email: [{ required: true, message: '请输入账号', trigger: 'blur' }, { min: 2, message: '账号至少2位字符', trigger: ['blur', 'change'] }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }, { min: 6, max: 20, message: '密码长度在6-20位之间', trigger: ['blur', 'change'] }],
  confirmPassword: [{ required: true, message: '请确认密码', trigger: 'blur' }, { validator: validateConfirmPassword, trigger: ['blur', 'change'] }],
  captcha: [{ required: true, validator: validateRegisterCaptcha, trigger: 'blur' }]
})

const handleMenuClick = (menuItem) => {
  if (menuItem.index === '/' || menuItem.index === '/home') { router.push(menuItem.index); return }
  if (!userStore.isLoggedIn) {
    pendingRoute.value = menuItem.index
    openLoginDialog()
    ElMessage({ message: '请先登录后再访问该页面', type: 'warning' })
  } else { router.push(menuItem.index) }
}

const logout = () => {
  userStore.logout()
  ElMessage({ message: '已安全退出登录', type: 'success' })
  router.push('/')
}

const resetForms = () => {
  loginForm.email = ''; loginForm.password = ''; loginForm.captcha = ''
  registerForm.email = ''; registerForm.password = ''; registerForm.confirmPassword = ''; registerForm.captcha = ''
  emailValidated.value = false; passwordMatched.value = false; loading.value = false
  nextTick(() => { loginFormRef.value?.clearValidate(); registerFormRef.value?.clearValidate() })
}

const focusFirstInput = () => {
  if (!isRegisterMode.value && firstLoginInputRef.value) firstLoginInputRef.value.focus()
  else if (isRegisterMode.value && firstRegisterInputRef.value) firstRegisterInputRef.value.focus()
}

const openLoginDialog = () => {
  isRegisterMode.value = false; showAuthDialog.value = true; resetForms()
  setTimeout(() => { refreshLoginCaptcha(); focusFirstInput() }, 100)
}

const openRegisterDialog = () => {
  isRegisterMode.value = true; showAuthDialog.value = true; resetForms()
  setTimeout(() => { refreshRegisterCaptcha(); focusFirstInput() }, 100)
}

const switchToLogin = () => {
  if (isRegisterMode.value) {
    isRegisterMode.value = false; resetForms()
    setTimeout(() => { refreshLoginCaptcha(); focusFirstInput() }, 300)
  }
}

const switchToRegister = () => {
  if (!isRegisterMode.value) {
    isRegisterMode.value = true; resetForms()
    setTimeout(() => { refreshRegisterCaptcha(); focusFirstInput() }, 300)
  }
}

const handleForgotPassword = () => {
  ElMessageBox.prompt('请输入您的账号，我们将发送验证链接', '找回密码', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    inputValidator: (value) => !!value || '账号不能为空'
  }).then(() => {
    ElMessage({ type: 'success', message: `请求已提交，请注意查收` })
  }).catch(() => {})
}

const handleLogin = async () => {
  if (loginAttempts.value >= maxLoginAttempts) return ElMessage({ message: '登录尝试次数过多，请稍后再试', type: 'error' })
  if (!loginFormRef.value) return
  await loginFormRef.value.validate(async (valid) => {
    if (valid) {
      loading.value = true
      try {
        const userData = await loginAPI(loginForm.email, loginForm.password)
        if (!userData) return
        userStore.login(userData)
        loginAttempts.value = 0; showAuthDialog.value = false
        ElMessage({ message: `欢迎回来，${userData.nickname || userData.username || '用户'}！`, type: 'success' })
        if (pendingRoute.value) { router.push(pendingRoute.value); pendingRoute.value = null }
      } catch (error) {
        loginAttempts.value++; handleRefreshLoginCaptcha()
        ElMessage({ message: error.message || '登录失败，请检查账号和密码', type: 'error' })
      } finally { loading.value = false }
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
          ElMessage({ message: '注册成功，欢迎加入我们！', type: 'success' })
          if (pendingRoute.value) { router.push(pendingRoute.value); pendingRoute.value = null }
        }
      } catch (error) {
        handleRefreshRegisterCaptcha()
        let errorMessage = '注册失败，请稍后重试'
        if (error.message === '用户名重复!') errorMessage = '该账号已被注册，请直接登录'
        else if (error.message) errorMessage = error.message
        ElMessage({ message: errorMessage, type: 'error' })
      } finally { loading.value = false }
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
    emitter.off('openLoginFromFooter'); emitter.off('openLoginFromHome'); emitter.off('openRegisterFromHome')
  }
  window.removeEventListener('showLoginDialog', handleApiLoginRequest)
})
</script>

<style scoped>
/* ==================== 顶部导航栏 (门户风格调整) ==================== */
/* 整体保持深色稳重，但去掉了纯黑，改用极深藏青色，更加高级商业 */
.header { background: #0B0F19; border-bottom: 1px solid #1E293B; position: sticky; top: 0; z-index: 1000; box-shadow: 0 4px 12px rgba(0,0,0,0.1); }
.container { max-width: 1200px; margin: 0 auto; padding: 0 24px; }
.logo { text-decoration: none; display: inline-block; transition: transform 0.3s ease; }
.logo-content { display: flex; align-items: center; gap: 8px; }
.logo h1 { color: #ffffff; font-size: 22px; font-weight: 700; margin: 0; letter-spacing: 0.5px; }
.nav-menu { border-bottom: none; background: transparent; justify-content: center; }
.nav-menu :deep(.el-menu-item) { color: #94A3B8; font-weight: 500; font-size: 15px; padding: 0 24px; border-bottom: 2px solid transparent; transition: all 0.3s ease; }
.nav-menu :deep(.el-menu-item:hover) { color: #ffffff; background: transparent; }
.nav-menu :deep(.el-menu-item.is-active) { color: #ffffff; background: transparent; border-bottom-color: #4F46E5; }
.user-actions { display: flex; justify-content: flex-end; align-items: center; }
.user-btn { display: flex; align-items: center; gap: 8px; padding: 6px 12px; border-radius: 20px; transition: all 0.3s ease; color: #E2E8F0; }
.user-btn:hover { background: rgba(255, 255, 255, 0.05); color: #ffffff; }
.user-avatar { background: #1E293B; color: #ffffff; font-weight: 600; font-size: 14px; border: 2px solid #334155; }
.user-name { font-size: 14px; font-weight: 500; max-width: 100px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.auth-btn { font-weight: 500; padding: 9px 20px; border-radius: 6px; font-size: 14px; transition: all 0.3s ease; }
.login-btn { color: #E2E8F0; border: 1px solid transparent; background: transparent !important; }
.login-btn:hover { color: #ffffff; background: rgba(255, 255, 255, 0.08) !important; }
/* 顶部的注册按钮也用主色调 */
.register-btn { background: #4F46E5 !important; color: #ffffff; border: none; }
.register-btn:hover { background: #4338CA !important; transform: translateY(-1px); box-shadow: 0 4px 12px rgba(79, 70, 229, 0.3); }

/* 下拉菜单 */
:deep(.el-dropdown-menu) { border-radius: 8px; box-shadow: 0 10px 25px rgba(0,0,0,0.1); border: 1px solid #E2E8F0; margin-top: 12px; padding: 4px; }
:deep(.el-dropdown-menu__item) { padding: 10px 20px; font-size: 14px; color: #334155; border-radius: 6px; margin: 2px 0; transition: all 0.2s ease; }
:deep(.el-dropdown-menu__item:hover) { background: #F1F5F9; color: #4F46E5; }

/* ==================== 现代微拟态风 弹窗样式 (Version 3: Portal Style) ==================== */

/* 遮罩层 - 轻盈透气感 */
:deep(.portal-modal-backdrop) {
  background: rgba(15, 23, 42, 0.6);
  backdrop-filter: blur(8px);
}

/* 弹窗圆角与阴影 */
:deep(.portal-auth-dialog) {
  border-radius: 20px;
  overflow: hidden;
}

:deep(.portal-auth-dialog .el-dialog) {
  background: #ffffff;
  border-radius: 20px;
  box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.25);
  margin: 0;
  padding: 0;
  position: relative;
}

:deep(.portal-auth-dialog .el-dialog__header) {
  display: none;
}

/* 右上角关闭按钮 */
.close-btn {
  position: absolute;
  top: 16px;
  right: 16px;
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: #F1F5F9;
  border: none;
  color: #64748B;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
  cursor: pointer;
  transition: all 0.2s ease;
  z-index: 10;
}
.close-btn:hover { background: #E2E8F0; color: #0F172A; transform: rotate(90deg); }

/* 主容器 */
.portal-container {
  padding: 40px 32px 32px;
  background: #ffffff;
}

/* 顶部标题区 */
.portal-header {
  text-align: center;
  margin-bottom: 28px;
}

.logo-icon-wrapper {
  width: 48px;
  height: 48px;
  background: linear-gradient(135deg, #EEF2FF 0%, #E0E7FF 100%);
  border-radius: 12px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 16px;
  box-shadow: 0 4px 10px rgba(79, 70, 229, 0.1);
}

.brand-icon {
  font-size: 24px;
  color: #4F46E5;
}

.main-title {
  font-size: 22px;
  font-weight: 700;
  color: #0F172A;
  margin: 0 0 8px 0;
}

.sub-title {
  font-size: 14px;
  color: #64748B;
  margin: 0;
}

/* 胶囊滑动式 Tab (Segmented Control) */
.portal-tabs-wrapper {
  background: #F1F5F9;
  padding: 4px;
  border-radius: 10px;
  margin-bottom: 32px;
}

.portal-tabs {
  position: relative;
  display: flex;
}

.tab-slider {
  position: absolute;
  top: 0;
  left: 0;
  width: 50%;
  height: 100%;
  background: #ffffff;
  border-radius: 8px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
  transition: transform 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}
.tab-slider.is-right { transform: translateX(100%); }

.tab-item {
  flex: 1;
  text-align: center;
  padding: 10px 0;
  font-size: 14px;
  font-weight: 600;
  color: #64748B;
  cursor: pointer;
  position: relative;
  z-index: 1;
  transition: color 0.3s ease;
}
.tab-item.active { color: #0F172A; }

/* 扁平微拟态表单元素 */
.portal-form-item {
  margin-bottom: 20px;
}

/* 输入框样式 */
:deep(.portal-input .el-input__wrapper) {
  background: #F8FAFC;
  border: 1px solid #E2E8F0;
  border-radius: 10px;
  padding: 0 14px;
  height: 48px;
  box-shadow: none !important;
  transition: all 0.2s ease;
}

:deep(.portal-input .el-input__wrapper:hover) {
  background: #ffffff;
  border-color: #CBD5E1;
}

:deep(.portal-input .el-input__wrapper.is-focus) {
  background: #ffffff;
  border-color: #4F46E5;
  box-shadow: 0 0 0 3px rgba(79, 70, 229, 0.1) !important;
}

:deep(.portal-input .el-input__inner) {
  font-size: 15px;
  color: #0F172A;
}

:deep(.portal-input .el-input__inner::placeholder) {
  color: #94A3B8;
}

.input-icon {
  color: #94A3B8;
  font-size: 18px;
  margin-right: 4px;
  transition: color 0.2s;
}

:deep(.portal-input .el-input__wrapper.is-focus) .input-icon {
  color: #4F46E5;
}

/* 图标状态颜色 */
.success-icon { color: #10B981; font-size: 18px; font-weight: bold; }
.error-icon { color: #EF4444; font-size: 18px; font-weight: bold; }

/* 验证码组 */
.captcha-group {
  display: flex;
  gap: 12px;
  margin-bottom: 20px;
}

.captcha-input {
  flex: 1;
  margin-bottom: 0;
}

.captcha-box {
  width: 110px;
  height: 48px;
  border-radius: 10px;
  border: 1px solid #E2E8F0;
  overflow: hidden;
  cursor: pointer;
  background: #F3F4F6;
  transition: all 0.2s;
}
.captcha-box:hover { border-color: #CBD5E1; opacity: 0.9; }
.captcha-box canvas { display: block; width: 100%; height: 100%; }

/* 记住我与忘记密码 */
.form-options {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

:deep(.portal-checkbox .el-checkbox__label) { color: #64748B; font-size: 14px; font-weight: normal; }
:deep(.portal-checkbox.is-checked .el-checkbox__label) { color: #0F172A; }
:deep(.el-checkbox__input.is-checked .el-checkbox__inner) { background-color: #4F46E5; border-color: #4F46E5; }

.forgot-link {
  color: #4F46E5;
  font-size: 14px;
  text-decoration: none;
  font-weight: 500;
  transition: color 0.2s;
}
.forgot-link:hover { color: #4338CA; text-decoration: underline; }

/* 主提交按钮 */
.portal-submit-btn {
  width: 100%;
  height: 48px;
  border-radius: 10px;
  font-size: 16px;
  font-weight: 600;
  background: #4F46E5;
  border: none;
  color: white;
  transition: all 0.3s ease;
  box-shadow: 0 4px 12px rgba(79, 70, 229, 0.2);
}

.portal-submit-btn:hover {
  background: #4338CA;
  transform: translateY(-2px);
  box-shadow: 0 6px 16px rgba(79, 70, 229, 0.3);
}

.portal-submit-btn:active { transform: translateY(0); }

.portal-submit-btn:disabled {
  background: #E2E8F0;
  color: #94A3B8;
  cursor: not-allowed;
  transform: none;
  box-shadow: none;
}

/* 错误提示 */
:deep(.el-form-item.is-error .portal-input .el-input__wrapper) {
  background: #FEF2F2;
  border-color: #EF4444;
  box-shadow: 0 0 0 3px rgba(239, 68, 68, 0.1) !important;
}
:deep(.el-form-item__error) { color: #EF4444; font-size: 12px; padding-top: 4px; }

/* 切换动画 */
.portal-slide-enter-active,
.portal-slide-leave-active { transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1); }
.portal-slide-enter-from { opacity: 0; transform: translateX(20px); }
.portal-slide-leave-to { opacity: 0; transform: translateX(-20px); }
</style>