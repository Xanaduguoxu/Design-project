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
                    <template #default>{{ userInitial }}</template>
                  </el-avatar>
                  <span class="user-name">{{ userStore.user.nickname || userStore.user.username }}</span>
                  <el-icon class="el-icon--right">
                    <ArrowDown />
                  </el-icon>
                </el-button>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item @click="router.push('/goal')">我的主页</el-dropdown-item>
                    <el-dropdown-item @click="router.push('/learningAnalysis')">数据中心</el-dropdown-item>
                    <el-dropdown-item @click="router.push('/profile')">账号设置</el-dropdown-item>
                    <el-dropdown-item divided @click="logout">退出登录</el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
            </template>

            <template v-else>
              <el-button @click="openLoginDialog" text class="auth-btn login-btn">登录</el-button>
              <el-button @click="openRegisterDialog" type="primary" class="auth-btn register-btn">立即加入</el-button>
            </template>
          </el-space>
        </el-col>
      </el-row>
    </div>

    <!-- VIP 邀请函风弹窗 -->
    <el-dialog v-model="showAuthDialog" title="" width="420px" :show-close="false" class="invite-auth-dialog" center
      :close-on-click-modal="true" :modal-class="'invite-modal-backdrop'" :append-to-body="false">

      <div class="invite-container">

        <!-- 绝对定位的关闭按钮 (白色透明) -->
        <button class="invite-close-btn" @click="showAuthDialog = false">
          <el-icon>
            <Close />
          </el-icon>
        </button>

        <!-- 顶部：绚丽画报区 -->
        <div class="invite-banner">
          <div class="banner-overlay"></div>
          <!-- 悬浮在交界处的徽标 (打破常规布局的灵魂) -->
          <div class="invite-avatar">
            <el-icon>
              <MagicStick />
            </el-icon>
          </div>
        </div>

        <!-- 底部：纯白表单区 -->
        <div class="invite-body">

          <div class="invite-header-text">
            <h2>{{ isRegisterMode ? '开启全新旅程' : '欢迎回到社区' }}</h2>
            <p>{{ isRegisterMode ? '加入我们，探索无限可能' : '输入你的专属凭证以继续' }}</p>
          </div>

          <!-- 居中微胶囊切换 -->
          <div class="invite-toggle-wrapper">
            <div class="invite-toggle">
              <div class="toggle-slider" :class="{ 'is-right': isRegisterMode }"></div>
              <div class="toggle-item" :class="{ active: !isRegisterMode }" @click="switchToLogin">登录</div>
              <div class="toggle-item" :class="{ active: isRegisterMode }" @click="switchToRegister">注册</div>
            </div>
          </div>

          <div class="invite-form-area">
            <transition name="invite-fade" mode="out-in">

              <!-- 登录表单 -->
              <div v-if="!isRegisterMode" key="login" class="form-content">
                <el-form :model="loginForm" :rules="loginRules" ref="loginFormRef" @submit.prevent="handleLogin">

                  <div class="invite-input-group">
                    <el-form-item prop="email">
                      <el-input ref="firstLoginInputRef" v-model="loginForm.email" placeholder="输入手机号或邮箱" size="large"
                        @keyup.enter="handleLogin" :disabled="loading" class="invite-input" clearable>
                        <template #prefix><el-icon class="invite-icon">
                            <User />
                          </el-icon></template>
                      </el-input>
                    </el-form-item>
                  </div>

                  <div class="invite-input-group">
                    <el-form-item prop="password">
                      <el-input v-model="loginForm.password" type="password" placeholder="输入密码" show-password
                        size="large" @keyup.enter="handleLogin" :disabled="loading" class="invite-input">
                        <template #prefix><el-icon class="invite-icon">
                            <Lock />
                          </el-icon></template>
                      </el-input>
                    </el-form-item>
                  </div>

                  <div class="invite-input-group">
                    <div class="captcha-flex">
                      <el-form-item prop="captcha" class="flex-1">
                        <el-input v-model="loginForm.captcha" placeholder="输入验证码" size="large"
                          @keyup.enter="handleLogin" :disabled="loading" class="invite-input" maxlength="4">
                          <template #prefix><el-icon class="invite-icon">
                              <Key />
                            </el-icon></template>
                        </el-input>
                      </el-form-item>
                      <div class="invite-captcha-box" @click="handleRefreshLoginCaptcha" title="看不清？换一张">
                        <canvas ref="loginCaptchaRef" width="110" height="48"></canvas>
                      </div>
                    </div>
                  </div>

                  <div class="invite-options">
                    <el-checkbox v-model="rememberMe">下次自动登录</el-checkbox>
                    <a href="#" class="invite-forgot" @click.prevent="handleForgotPassword">忘记密码?</a>
                  </div>

                  <el-button @click="handleLogin" :loading="loading" type="primary" size="large"
                    class="invite-submit-btn" :disabled="!isLoginFormValid">
                    {{ loading ? '登录中...' : '进入社区' }}
                  </el-button>
                </el-form>
              </div>

              <!-- 注册表单 -->
              <div v-else key="register" class="form-content">
                <el-form :model="registerForm" :rules="registerRules" ref="registerFormRef"
                  @submit.prevent="handleRegister">

                  <div class="invite-input-group">
                    <el-form-item prop="email">
                      <el-input ref="firstRegisterInputRef" v-model="registerForm.email" placeholder="给自己起个账号名"
                        size="large" :disabled="loading" class="invite-input" @input="validateEmailRealtime" clearable>
                        <template #prefix><el-icon class="invite-icon">
                            <User />
                          </el-icon></template>
                        <template #suffix><el-icon v-if="emailValidated" class="invite-status success">
                            <Check />
                          </el-icon></template>
                      </el-input>
                    </el-form-item>
                  </div>

                  <div class="invite-input-group">
                    <el-form-item prop="password">
                      <el-input v-model="registerForm.password" type="password" placeholder="设置一个安全的密码" show-password
                        size="large" :disabled="loading" class="invite-input">
                        <template #prefix><el-icon class="invite-icon">
                            <Lock />
                          </el-icon></template>
                      </el-input>
                    </el-form-item>
                  </div>

                  <div class="invite-input-group">
                    <el-form-item prop="confirmPassword">
                      <el-input v-model="registerForm.confirmPassword" type="password" placeholder="再次输入密码确认"
                        show-password size="large" :disabled="loading" class="invite-input"
                        @input="validatePasswordMatch">
                        <template #prefix><el-icon class="invite-icon">
                            <Lock />
                          </el-icon></template>
                        <template #suffix>
                          <el-icon v-if="passwordMatched" class="invite-status success">
                            <Check />
                          </el-icon>
                          <el-icon v-else-if="registerForm.confirmPassword && !passwordMatched"
                            class="invite-status error">
                            <CircleClose />
                          </el-icon>
                        </template>
                      </el-input>
                    </el-form-item>
                  </div>

                  <div class="invite-input-group">
                    <div class="captcha-flex">
                      <el-form-item prop="captcha" class="flex-1">
                        <el-input v-model="registerForm.captcha" placeholder="输入验证码" size="large"
                          @keyup.enter="handleRegister" :disabled="loading" class="invite-input" maxlength="4">
                          <template #prefix><el-icon class="invite-icon">
                              <Key />
                            </el-icon></template>
                        </el-input>
                      </el-form-item>
                      <div class="invite-captcha-box" @click="handleRefreshRegisterCaptcha" title="看不清？换一张">
                        <canvas ref="registerCaptchaRef" width="110" height="48"></canvas>
                      </div>
                    </div>
                  </div>

                  <el-button @click="handleRegister" :loading="loading" type="primary" size="large"
                    class="invite-submit-btn" :disabled="!isRegisterFormValid">
                    {{ loading ? '正在注册...' : '立即注册加入' }}
                  </el-button>
                </el-form>
              </div>
            </transition>
          </div>
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
import { User, Lock, ArrowDown, Key, Close, Check, CircleClose, MagicStick } from '@element-plus/icons-vue'
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
const rememberMe = ref(true)
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

const appTitle = computed(() => import.meta.env.VITE_APP_TITLE || '潮流空间')

const navMenu = computed(() => {
  try {
    const menuConfig = import.meta.env.VITE_NAV_MENU
    if (menuConfig) return JSON.parse(menuConfig)
  } catch (error) { return [] }
  return []
})

const userInitial = computed(() => {
  const name = userStore.user?.nickname || userStore.user?.username || '我'
  return name.charAt(0).toUpperCase()
})

const generateRandomString = (length = 4) => {
  const chars = 'ABCDEFGHJKLMNPQRSTUVWXYZabcdefghjkmnpqrstuvwxyz23456789'
  let result = ''
  for (let i = 0; i < length; i++) result += chars.charAt(Math.floor(Math.random() * chars.length))
  return result
}

// 验证码生成：配合紫色渐变的画报风
const drawCaptcha = (canvasElement, text) => {
  if (!canvasElement) return
  const ctx = canvasElement.getContext('2d')
  const width = canvasElement.width
  const height = canvasElement.height

  ctx.clearRect(0, 0, width, height)

  // 浅粉紫底色
  ctx.fillStyle = '#FCF5FF'
  ctx.fillRect(0, 0, width, height)

  for (let i = 0; i < 4; i++) {
    ctx.strokeStyle = `rgba(200, 80, 192, 0.2)`
    ctx.lineWidth = 1
    ctx.beginPath()
    ctx.moveTo(Math.random() * width, Math.random() * height)
    ctx.lineTo(Math.random() * width, Math.random() * height)
    ctx.stroke()
  }

  for (let i = 0; i < 20; i++) {
    ctx.fillStyle = `rgba(200, 80, 192, 0.15)`
    ctx.beginPath()
    ctx.arc(Math.random() * width, Math.random() * height, 1, 0, 2 * Math.PI)
    ctx.fill()
  }

  ctx.font = 'bold 24px Arial, sans-serif'
  ctx.textBaseline = 'middle'
  for (let i = 0; i < text.length; i++) {
    ctx.fillStyle = `#C850C0`
    const x = 22 * i + 14
    const y = height / 2
    const deg = (Math.random() * 20 - 10) * Math.PI / 180

    ctx.translate(x, y)
    ctx.rotate(deg)
    ctx.fillText(text[i], 0, 0)
    ctx.rotate(-deg)
    ctx.translate(-x, -y)
  }
}

const refreshLoginCaptcha = () => { loginCaptchaCode.value = generateRandomString(); drawCaptcha(loginCaptchaRef.value, loginCaptchaCode.value) }
const refreshRegisterCaptcha = () => { registerCaptchaCode.value = generateRandomString(); drawCaptcha(registerCaptchaRef.value, registerCaptchaCode.value) }
const handleRefreshLoginCaptcha = () => { refreshLoginCaptcha(); loginForm.captcha = '' }
const handleRefreshRegisterCaptcha = () => { refreshRegisterCaptcha(); registerForm.captcha = '' }

const isLoginFormValid = computed(() => loginForm.email && loginForm.password.length >= 6 && loginForm.captcha.length === 4 && loginForm.captcha.toLowerCase() === loginCaptchaCode.value.toLowerCase())
const isRegisterFormValid = computed(() => registerForm.email && registerForm.password && registerForm.password === registerForm.confirmPassword && registerForm.captcha.length === 4 && registerForm.captcha.toLowerCase() === registerCaptchaCode.value.toLowerCase())

const validateConfirmPassword = (rule, value, callback) => { value !== registerForm.password ? callback(new Error('两次输入的密码不一致')) : callback() }
const validateLoginCaptcha = (rule, value, callback) => { !value ? callback(new Error('请输入验证码')) : (value.toLowerCase() !== loginCaptchaCode.value.toLowerCase() ? callback(new Error('验证码不正确')) : callback()) }
const validateRegisterCaptcha = (rule, value, callback) => { !value ? callback(new Error('请输入验证码')) : (value.toLowerCase() !== registerCaptchaCode.value.toLowerCase() ? callback(new Error('验证码不正确')) : callback()) }

const loginRules = reactive({
  email: [{ required: true, message: '请输入账号', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }, { min: 6, message: '密码不能少于6位', trigger: ['blur', 'change'] }],
  captcha: [{ required: true, validator: validateLoginCaptcha, trigger: 'blur' }]
})

const registerRules = reactive({
  email: [{ required: true, message: '请输入账号', trigger: 'blur' }, { min: 2, message: '账号不能少于2位', trigger: ['blur', 'change'] }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }, { min: 6, max: 20, message: '密码需在6-20位之间', trigger: ['blur', 'change'] }],
  confirmPassword: [{ required: true, message: '请再次输入密码', trigger: 'blur' }, { validator: validateConfirmPassword, trigger: ['blur', 'change'] }],
  captcha: [{ required: true, validator: validateRegisterCaptcha, trigger: 'blur' }]
})

const handleMenuClick = (menuItem) => {
  if (menuItem.index === '/' || menuItem.index === '/home') { router.push(menuItem.index); return }
  if (!userStore.isLoggedIn) { pendingRoute.value = menuItem.index; openLoginDialog(); ElMessage({ message: '请先登录哦', type: 'warning' }) }
  else { router.push(menuItem.index) }
}

const logout = () => { userStore.logout(); ElMessage({ message: '期待你的下次访问！', type: 'success' }); router.push('/') }

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

const openLoginDialog = () => { isRegisterMode.value = false; showAuthDialog.value = true; resetForms(); setTimeout(() => { refreshLoginCaptcha(); focusFirstInput() }, 100) }
const openRegisterDialog = () => { isRegisterMode.value = true; showAuthDialog.value = true; resetForms(); setTimeout(() => { refreshRegisterCaptcha(); focusFirstInput() }, 100) }
const switchToLogin = () => { isRegisterMode.value = false; resetForms(); setTimeout(() => { refreshLoginCaptcha(); focusFirstInput() }, 200) }
const switchToRegister = () => { isRegisterMode.value = true; resetForms(); setTimeout(() => { refreshRegisterCaptcha(); focusFirstInput() }, 200) }

const handleForgotPassword = () => {
  ElMessageBox.prompt('请输入你的账号，我们帮你找回密码', '找回密码', { confirmButtonText: '确定', cancelButtonText: '取消', inputValidator: (val) => !!val || '账号不能为空哦' })
    .then(() => ElMessage.success('请求已发送，请注意查收邮件/短信'))
    .catch(() => { })
}

const handleLogin = async () => {
  if (loginAttempts.value >= maxLoginAttempts) return ElMessage.error('尝试次数过多，请休息一下再试')
  if (!loginFormRef.value) return
  await loginFormRef.value.validate(async (valid) => {
    if (valid) {
      loading.value = true
      try {
        const userData = await loginAPI(loginForm.email, loginForm.password)
        if (!userData) return
        userStore.login(userData); loginAttempts.value = 0; showAuthDialog.value = false
        ElMessage.success(`欢迎回来，${userData.nickname || userData.username || '小伙伴'}！`)
        if (pendingRoute.value) { router.push(pendingRoute.value); pendingRoute.value = null }
      } catch (error) {
        loginAttempts.value++; handleRefreshLoginCaptcha()
        ElMessage.error(error.message || '账号或密码不对哦，请再试一次')
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
          showAuthDialog.value = false; ElMessage.success('注册成功，欢迎加入大家庭！')
          if (pendingRoute.value) { router.push(pendingRoute.value); pendingRoute.value = null }
        }
      } catch (error) {
        handleRefreshRegisterCaptcha()
        ElMessage.error(error.message === '用户名重复!' ? '这个账号已经被注册啦' : error.message || '注册失败，请稍后重试')
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
  if (emitter) { emitter.off('openLoginFromFooter'); emitter.off('openLoginFromHome'); emitter.off('openRegisterFromHome') }
  window.removeEventListener('showLoginDialog', handleApiLoginRequest)
})
</script>

<style scoped>
/* ==================== 顶部导航栏 (原版纯黑，不更改) ==================== */
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
  border-color: #ffffff;
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

/* ==================== VIP邀请函/画报叠层风 弹窗样式 ==================== */

/* 暗色高斯模糊遮罩 */
:deep(.invite-modal-backdrop) {
  background: rgba(10, 10, 15, 0.7);
  backdrop-filter: blur(6px);
}

/* ★ 清除原生壳子 ★ */
:deep(.el-dialog.invite-auth-dialog) {
  background: transparent !important;
  box-shadow: none !important;
  border-radius: 0 !important;
  padding: 0 !important;
  border: none !important;
}

:deep(.el-dialog.zen-auth-dialog .el-dialog__header) {
  display: none !important;
}

:deep(.el-dialog.zen-auth-dialog .el-dialog__body) {
  padding: 0 !important;
  background: transparent !important;
}


/* 核心容器：窄屏设计，类似手机卡片，边缘裁切 */
.invite-container {
  width: 100%;
  max-width: 400px;
  margin: 0 auto;
  background: #ffffff;
  border-radius: 24px;
  position: relative;
  /* 不加 overflow: hidden，以便让中间的头像能浮出来！ */
  box-shadow: 0 30px 60px rgba(0, 0, 0, 0.3);
}

/* --- 顶部绚丽画报区 --- */
.invite-banner {
  width: 100%;
  height: 140px;
  /* 黄金比例头部高度 */
  /* 落日星空高奢渐变：深蓝 -> 玫紫 -> 蜜桃黄 */
  background: linear-gradient(135deg, #4158D0 0%, #C850C0 46%, #FFCC70 100%);
  border-radius: 24px 24px 0 0;
  position: relative;
}

.banner-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-image: radial-gradient(rgba(255, 255, 255, 0.2) 1px, transparent 1px);
  background-size: 10px 10px;
  border-radius: 24px 24px 0 0;
  opacity: 0.5;
}

/* 右上角白色透明关闭按钮 */
.invite-close-btn {
  position: absolute;
  top: 16px;
  right: 16px;
  background: rgba(255, 255, 255, 0.2);
  border: none;
  color: #ffffff;
  width: 30px;
  height: 30px;
  border-radius: 50%;
  font-size: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.2s;
  z-index: 20;
  backdrop-filter: blur(4px);
}

.invite-close-btn:hover {
  background: rgba(255, 255, 255, 0.4);
  transform: rotate(90deg);
}

/* --- 核心灵魂：悬浮在交界处的徽标头像 --- */
.invite-avatar {
  width: 80px;
  height: 80px;
  background: #ffffff;
  border: 4px solid #ffffff;
  /* 粗白边，切断上下两部分 */
  border-radius: 50%;
  position: absolute;
  bottom: -40px;
  /* 一半浮在外面 */
  left: 50%;
  transform: translateX(-50%);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.15);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 36px;
  color: #C850C0;
  z-index: 10;
}

/* --- 底部表单区 --- */
.invite-body {
  padding: 60px 32px 36px;
  /* 顶部留出 60px 给头像 */
  background: #ffffff;
  border-radius: 0 0 24px 24px;
}

.invite-header-text {
  text-align: center;
  margin-bottom: 24px;
}

.invite-header-text h2 {
  font-size: 22px;
  font-weight: 800;
  color: #111827;
  margin: 0 0 8px 0;
}

.invite-header-text p {
  font-size: 14px;
  color: #6B7280;
  margin: 0;
}

/* 居中小胶囊切换 (极简克制) */
.invite-toggle-wrapper {
  display: flex;
  justify-content: center;
  margin-bottom: 32px;
}

.invite-toggle {
  background: #F3F4F6;
  padding: 4px;
  border-radius: 12px;
  display: inline-flex;
  position: relative;
  width: 200px;
  /* 固定宽度 */
}

.toggle-slider {
  position: absolute;
  top: 4px;
  left: 4px;
  width: calc(50% - 4px);
  height: calc(100% - 8px);
  background: #ffffff;
  border-radius: 8px;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.08);
  transition: transform 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.toggle-slider.is-right {
  transform: translateX(100%);
}

.toggle-item {
  flex: 1;
  text-align: center;
  padding: 8px 0;
  font-size: 14px;
  font-weight: 600;
  color: #9CA3AF;
  cursor: pointer;
  position: relative;
  z-index: 1;
  transition: color 0.3s;
}

.toggle-item.active {
  color: #111827;
}

/* 表单输入组 */
.invite-input-group {
  margin-bottom: 20px;
}

/* 清新干净的输入框 */
:deep(.invite-input .el-input__wrapper) {
  background: #F9FAFB;
  border: 1px solid #E5E7EB;
  border-radius: 14px;
  padding: 0 16px;
  height: 48px;
  box-shadow: none !important;
  transition: all 0.2s ease;
}

:deep(.invite-input .el-input__wrapper:hover) {
  border-color: #D1D5DB;
}

:deep(.invite-input .el-input__wrapper.is-focus) {
  background: #ffffff;
  border-color: #C850C0;
  box-shadow: 0 0 0 3px rgba(200, 80, 192, 0.15) !important;
}

:deep(.invite-input .el-input__inner) {
  font-size: 15px;
  color: #111827;
}

:deep(.invite-input .el-input__inner::placeholder) {
  color: #9CA3AF;
}

.invite-icon {
  color: #9CA3AF;
  font-size: 18px;
  margin-right: 8px;
  transition: color 0.2s;
}

:deep(.invite-input .el-input__wrapper.is-focus) .invite-icon {
  color: #C850C0;
}

/* 状态图标 */
.invite-status {
  font-size: 18px;
  font-weight: bold;
}

.invite-status.success {
  color: #10B981;
}

.invite-status.error {
  color: #EF4444;
}

/* 验证码布局 */
.captcha-flex {
  display: flex;
  gap: 12px;
}

.flex-1 {
  flex: 1;
  margin-bottom: 0;
}

.invite-captcha-box {
  width: 110px;
  height: 48px;
  border-radius: 14px;
  border: 1px solid #E5E7EB;
  overflow: hidden;
  cursor: pointer;
  background: #F9FAFB;
  flex-shrink: 0;
  transition: border-color 0.2s;
}

.invite-captcha-box:hover {
  border-color: #D1D5DB;
}

.invite-captcha-box canvas {
  display: block;
  width: 100%;
  height: 100%;
}

/* 底部选项 */
.invite-options {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 28px;
  padding: 0 4px;
}

:deep(.el-checkbox__label) {
  color: #6B7280;
  font-size: 14px;
}

:deep(.el-checkbox__input.is-checked .el-checkbox__inner) {
  background-color: #C850C0;
  border-color: #C850C0;
}

.invite-forgot {
  font-size: 14px;
  color: #6B7280;
  text-decoration: none;
  transition: color 0.2s;
}

.invite-forgot:hover {
  color: #C850C0;
  text-decoration: underline;
}

/* 主按钮：与头部渐变呼应 */
.invite-submit-btn {
  width: 100%;
  height: 50px;
  border-radius: 14px;
  font-size: 16px;
  font-weight: bold;
  background: linear-gradient(135deg, #4158D0 0%, #C850C0 50%, #FFCC70 100%);
  border: none;
  color: white;
  transition: all 0.3s;
  box-shadow: 0 8px 20px rgba(200, 80, 192, 0.3);
  letter-spacing: 1px;
}

.invite-submit-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 12px 24px rgba(200, 80, 192, 0.4);
}

.invite-submit-btn:active {
  transform: translateY(0);
}

.invite-submit-btn:disabled {
  background: #E5E7EB;
  color: #9CA3AF;
  cursor: not-allowed;
  transform: none;
  box-shadow: none;
}

/* 错误提示 */
:deep(.el-form-item.is-error .invite-input .el-input__wrapper) {
  border-color: #EF4444;
  background: #FEF2F2;
}

:deep(.el-form-item__error) {
  color: #EF4444;
  font-size: 12px;
  padding-top: 4px;
}

/* 切换动画 */
.invite-fade-enter-active,
.invite-fade-leave-active {
  transition: opacity 0.2s ease, transform 0.2s ease;
}

.invite-fade-enter-from {
  opacity: 0;
  transform: translateY(10px);
}

.invite-fade-leave-to {
  opacity: 0;
  transform: translateY(-10px);
}
</style>