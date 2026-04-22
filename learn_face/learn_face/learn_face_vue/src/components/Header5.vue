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
                  <el-icon class="el-icon--right"><ArrowDown /></el-icon>
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
              <el-button @click="openRegisterDialog" type="primary" class="auth-btn register-btn">免费注册</el-button>
            </template>
          </el-space>
        </el-col>
      </el-row>
    </div>

    <!-- 沉浸式卡片 App 风弹窗 -->
    <el-dialog v-model="showAuthDialog" title="" width="420px" :show-close="false" class="app-auth-dialog" center
      :close-on-click-modal="true" :modal-class="'app-modal-backdrop'" :append-to-body="false">
      
      <div class="app-container">
        
        <!-- 上半部分：沉浸式视觉背景 -->
        <div class="app-header-bg">
          <!-- 装饰性光晕与气泡 -->
          <div class="bubble b-1"></div>
          <div class="bubble b-2"></div>
          <div class="bubble b-3"></div>

          <!-- 右上角透明关闭按钮 -->
          <button class="app-close-btn" @click="showAuthDialog = false">
            <el-icon><Close /></el-icon>
          </button>

          <div class="app-header-content">
            <h2>Hi, 欢迎来到这里 👋</h2>
            <p>连接彼此，分享生活中的每一个心动瞬间</p>
          </div>
        </div>

        <!-- 下半部分：App 抽屉式白色表单卡片 -->
        <div class="app-sheet">
          <!-- 模拟 App 的抽屉拉手胶囊 -->
          <div class="drag-handle"></div>

          <!-- 极简融合的 Tab 切换 -->
          <div class="app-tabs">
            <div class="tab-item" :class="{ active: !isRegisterMode }" @click="switchToLogin">账号登录</div>
            <div class="tab-item" :class="{ active: isRegisterMode }" @click="switchToRegister">新用户注册</div>
            <div class="tab-indicator" :class="{ 'is-right': isRegisterMode }"></div>
          </div>

          <div class="app-form-body">
            <transition name="app-fade" mode="out-in">
              
              <!-- 登录表单 -->
              <div v-if="!isRegisterMode" key="login" class="form-wrapper">
                <el-form :model="loginForm" :rules="loginRules" ref="loginFormRef" @submit.prevent="handleLogin">
                  
                  <div class="app-input-group">
                    <el-form-item prop="email">
                      <el-input ref="firstLoginInputRef" v-model="loginForm.email" placeholder="手机号 / 邮箱" size="large" @keyup.enter="handleLogin" :disabled="loading" class="app-input" clearable>
                        <template #prefix><el-icon class="app-icon"><UserIcon /></el-icon></template>
                      </el-input>
                    </el-form-item>
                  </div>

                  <div class="app-input-group">
                    <el-form-item prop="password">
                      <el-input v-model="loginForm.password" type="password" placeholder="请输入密码" show-password size="large" @keyup.enter="handleLogin" :disabled="loading" class="app-input">
                        <template #prefix><el-icon class="app-icon"><Lock /></el-icon></template>
                      </el-input>
                    </el-form-item>
                  </div>

                  <div class="app-input-group">
                    <div class="captcha-flex">
                      <el-form-item prop="captcha" class="flex-1">
                        <el-input v-model="loginForm.captcha" placeholder="输入验证码" size="large" @keyup.enter="handleLogin" :disabled="loading" class="app-input" maxlength="4">
                          <template #prefix><el-icon class="app-icon"><Key /></el-icon></template>
                        </el-input>
                      </el-form-item>
                      <div class="app-captcha-box" @click="handleRefreshLoginCaptcha" title="换一张">
                        <canvas ref="loginCaptchaRef" width="110" height="52"></canvas>
                      </div>
                    </div>
                  </div>

                  <div class="app-form-options">
                    <el-checkbox v-model="rememberMe">保持登录</el-checkbox>
                    <a href="#" class="app-forgot-link" @click.prevent="handleForgotPassword">忘记密码？</a>
                  </div>

                  <el-button @click="handleLogin" :loading="loading" type="primary" size="large" class="app-submit-btn" :disabled="!isLoginFormValid">
                    {{ loading ? '登录中...' : '登 录' }}
                  </el-button>
                </el-form>
              </div>

              <!-- 注册表单 -->
              <div v-else key="register" class="form-wrapper">
                <el-form :model="registerForm" :rules="registerRules" ref="registerFormRef" @submit.prevent="handleRegister">
                  
                  <div class="app-input-group">
                    <el-form-item prop="email">
                      <el-input ref="firstRegisterInputRef" v-model="registerForm.email" placeholder="设置你的账号" size="large" :disabled="loading" class="app-input" @input="validateEmailRealtime" clearable>
                        <template #prefix><el-icon class="app-icon"><UserIcon /></el-icon></template>
                        <template #suffix><el-icon v-if="emailValidated" class="app-status success"><Check /></el-icon></template>
                      </el-input>
                    </el-form-item>
                  </div>

                  <div class="app-input-group">
                    <el-form-item prop="password">
                      <el-input v-model="registerForm.password" type="password" placeholder="设置密码 (6位以上)" show-password size="large" :disabled="loading" class="app-input">
                        <template #prefix><el-icon class="app-icon"><Lock /></el-icon></template>
                      </el-input>
                    </el-form-item>
                  </div>

                  <div class="app-input-group">
                    <el-form-item prop="confirmPassword">
                      <el-input v-model="registerForm.confirmPassword" type="password" placeholder="再次确认密码" show-password size="large" :disabled="loading" class="app-input" @input="validatePasswordMatch">
                        <template #prefix><el-icon class="app-icon"><Lock /></el-icon></template>
                        <template #suffix>
                          <el-icon v-if="passwordMatched" class="app-status success"><Check /></el-icon>
                          <el-icon v-else-if="registerForm.confirmPassword && !passwordMatched" class="app-status error"><CircleClose /></el-icon>
                        </template>
                      </el-input>
                    </el-form-item>
                  </div>

                  <div class="app-input-group">
                    <div class="captcha-flex">
                      <el-form-item prop="captcha" class="flex-1">
                        <el-input v-model="registerForm.captcha" placeholder="输入验证码" size="large" @keyup.enter="handleRegister" :disabled="loading" class="app-input" maxlength="4">
                          <template #prefix><el-icon class="app-icon"><Key /></el-icon></template>
                        </el-input>
                      </el-form-item>
                      <div class="app-captcha-box" @click="handleRefreshRegisterCaptcha" title="换一张">
                        <canvas ref="registerCaptchaRef" width="110" height="52"></canvas>
                      </div>
                    </div>
                  </div>

                  <el-button @click="handleRegister" :loading="loading" type="primary" size="large" class="app-submit-btn" :disabled="!isRegisterFormValid">
                    {{ loading ? '注册中...' : '注册并加入' }}
                  </el-button>
                  
                  <div class="app-agreement">
                    注册即代表同意 <a href="#">《用户服务协议》</a>
                  </div>
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
import { User as UserIcon, Lock, ArrowDown, Key, Close, Check, CircleClose } from '@element-plus/icons-vue'
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

const appTitle = computed(() => import.meta.env.VITE_APP_TITLE || '精彩社区')

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

const drawCaptcha = (canvasElement, text) => {
  if (!canvasElement) return
  const ctx = canvasElement.getContext('2d')
  const width = canvasElement.width
  const height = canvasElement.height

  ctx.clearRect(0, 0, width, height)
  
  ctx.fillStyle = '#FFF5F7'
  ctx.fillRect(0, 0, width, height)

  for (let i = 0; i < 4; i++) {
    ctx.strokeStyle = `rgba(255, 117, 140, 0.2)`
    ctx.lineWidth = 1
    ctx.beginPath()
    ctx.moveTo(Math.random() * width, Math.random() * height)
    ctx.lineTo(Math.random() * width, Math.random() * height)
    ctx.stroke()
  }

  for (let i = 0; i < 20; i++) {
    ctx.fillStyle = `rgba(255, 117, 140, 0.2)`
    ctx.beginPath()
    ctx.arc(Math.random() * width, Math.random() * height, 1, 0, 2 * Math.PI)
    ctx.fill()
  }

  ctx.font = 'bold 24px Arial, sans-serif'
  ctx.textBaseline = 'middle'
  for (let i = 0; i < text.length; i++) {
    ctx.fillStyle = `#FF758C`
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
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }, { min: 6, message: '密码错误', trigger: ['blur', 'change'] }],
  captcha: [{ required: true, validator: validateLoginCaptcha, trigger: 'blur' }]
})

const registerRules = reactive({
  email: [{ required: true, message: '请输入账号', trigger: 'blur' }, { min: 2, message: '账号太短啦', trigger: ['blur', 'change'] }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }, { min: 6, max: 20, message: '密码要在6-20位之间哦', trigger: ['blur', 'change'] }],
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
    .catch(() => {})
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
/* ==================== 顶部导航栏 (原版纯黑底白字，绝不更改) ==================== */
.header { background: #000000; border-bottom: 1px solid #333333; position: sticky; top: 0; z-index: 1000; box-shadow: 0 2px 8px rgba(0, 0, 0, 0.3); }
.container { max-width: 1200px; margin: 0 auto; padding: 0 24px; }
.logo { text-decoration: none; display: inline-block; transition: transform 0.3s ease; }
.logo:hover { transform: translateY(-2px); }
.logo-content { display: flex; align-items: center; gap: 8px; }
.logo h1 { color: #ffffff; font-size: 24px; font-weight: 700; margin: 0; letter-spacing: -0.5px; }
.nav-menu { border-bottom: none; background: transparent; justify-content: center; }
.nav-menu :deep(.el-menu-item) { color: #ffffff; font-weight: 600; font-size: 15px; padding: 0 20px; border-bottom: 2px solid transparent; transition: all 0.3s ease; }
.nav-menu :deep(.el-menu-item:hover) { color: #ffffff; background: rgba(255, 255, 255, 0.1); border-radius: 8px; }
.nav-menu :deep(.el-menu-item.is-active) { color: #ffffff; background: rgba(255, 255, 255, 0.15); border-radius: 8px; border-bottom-color: transparent; }
.user-actions { display: flex; justify-content: flex-end; align-items: center; }
.user-btn { display: flex; align-items: center; gap: 8px; padding: 8px 12px; border-radius: 20px; transition: all 0.3s ease; color: #ffffff; }
.user-btn:hover { background: rgba(0, 0, 0, 0.1); transform: translateY(-1px); }
.user-avatar { background: rgba(255, 255, 255, 0.2); color: #ffffff; font-weight: 600; font-size: 14px; border: 2px solid rgba(255, 255, 255, 0.2); transition: all 0.3s ease; }
.user-btn:hover .user-avatar { border-color: rgba(255, 255, 255, 0.4); transform: scale(1.05); }
.user-name { font-size: 14px; font-weight: 500; max-width: 100px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.auth-btn { font-weight: 600; padding: 10px 20px; border-radius: 25px; transition: all 0.3s ease; }
.login-btn { color: #ffffff; border: 2px solid rgba(255, 255, 255, 0.5); background: transparent !important; }
.login-btn:hover { color: #ffffff; background: rgba(255, 255, 255, 0.1) !important; border-color: #ffffff; }
.register-btn { background: #ffffff !important; color: #000000; border: none; }
.register-btn:hover { background: #f8f9fa !important; color: #000000 !important; transform: translateY(-2px); box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15); border: 1px solid #e9ecef; }
:deep(.el-dropdown-menu) { border-radius: 8px; box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15); border: 1px solid #e5e7eb; background: #ffffff; margin-top: 8px; }
:deep(.el-dropdown-menu__item) { padding: 10px 16px; font-size: 14px; color: #000000; transition: all 0.3s ease; }
:deep(.el-dropdown-menu__item:hover) { background: rgba(0, 0, 0, 0.05); color: #000000; }

/* ==================== 沉浸式卡片叠层风 弹窗样式 (Version 8) ==================== */

/* 遮罩层 */
:deep(.app-modal-backdrop) {
  background: rgba(0, 0, 0, 0.6);
  backdrop-filter: blur(5px);
}

/* ★ 关键修复：彻底去掉 Element UI 弹窗自带的所有壳子和内边距 */
:deep(.el-dialog.app-auth-dialog) {
  background: transparent !important;
  box-shadow: none !important;
  border-radius: 0 !important;
  padding: 0 !important;
  border: none !important;
}

:deep(.el-dialog.app-auth-dialog .el-dialog__header) {
  display: none !important;
}

/* ★ 将 body 背景透明，并移除所有 padding，让内容自己撑开 */
:deep(.el-dialog.app-auth-dialog .el-dialog__body) {
  padding: 0 !important;
  background: transparent !important;
}

/* 核心容器：限制宽度，自身带大圆角 */
.app-container {
  width: 100%;
  max-width: 420px;
  margin: 0 auto;
  background: transparent;
  display: flex;
  flex-direction: column;
  position: relative;
  /* 这是整个面板的圆角 */
  border-radius: 28px;
  overflow: hidden;
  box-shadow: 0 24px 60px rgba(0, 0, 0, 0.3);
}

/* --- 上半部分：沉浸式视觉背景 --- */
.app-header-bg {
  width: 100%;
  height: 200px; /* 给足视觉空间 */
  background: linear-gradient(135deg, #FF758C 0%, #FF7EB3 100%);
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
}

/* 装饰性气泡流动感 */
.bubble {
  position: absolute;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.15);
  filter: blur(2px);
}
.b-1 { width: 150px; height: 150px; top: -40px; left: -40px; }
.b-2 { width: 100px; height: 100px; bottom: 20px; right: -20px; background: rgba(255, 255, 255, 0.1); }
.b-3 { width: 60px; height: 60px; top: 30px; right: 50px; background: rgba(255, 255, 255, 0.2); }

/* 右上角透明关闭按钮 */
.app-close-btn {
  position: absolute;
  top: 16px;
  right: 16px;
  width: 32px;
  height: 32px;
  background: rgba(255, 255, 255, 0.2);
  border: none;
  color: #ffffff;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  cursor: pointer;
  z-index: 10;
  backdrop-filter: blur(4px);
  transition: all 0.2s;
}
.app-close-btn:hover {
  background: rgba(255, 255, 255, 0.3);
  transform: rotate(90deg);
}

.app-header-content {
  position: relative;
  z-index: 2;
  text-align: center;
  color: #ffffff;
  padding: 0 24px;
  margin-top: -20px; /* 稍微靠上，给底部的白色卡片留空间 */
}

.app-header-content h2 {
  font-size: 26px;
  font-weight: 800;
  margin: 0 0 8px 0;
  text-shadow: 0 2px 8px rgba(0,0,0,0.1);
}

.app-header-content p {
  font-size: 14px;
  margin: 0;
  opacity: 0.95;
}

/* --- 下半部分：App 抽屉式白色表单卡片 --- */
.app-sheet {
  background: #ffffff;
  flex: 1;
  /* 核心魔法：向上偏移，覆盖在背景之上，并设置大圆角，制造出抽屉感 */
  margin-top: -30px;
  border-radius: 28px 28px 0 0;
  position: relative;
  z-index: 5;
  padding: 24px 32px 36px;
  display: flex;
  flex-direction: column;
}

/* 模拟手机底部的拉手胶囊 */
.drag-handle {
  width: 40px;
  height: 5px;
  background: #E5E7EB;
  border-radius: 10px;
  margin: 0 auto 24px;
}

/* 极简融合的 Tab 切换 */
.app-tabs {
  display: flex;
  position: relative;
  margin-bottom: 30px;
  border-bottom: 2px solid #F3F4F6;
}

.tab-item {
  flex: 1;
  text-align: center;
  padding: 12px 0;
  font-size: 16px;
  font-weight: 600;
  color: #9CA3AF;
  cursor: pointer;
  transition: color 0.3s;
}
.tab-item.active {
  color: #111827;
  font-weight: 700;
}

/* 底部滑动的短划线 */
.tab-indicator {
  position: absolute;
  bottom: -2px;
  left: 15%; /* 缩短长度，显得更精致 */
  width: 20%;
  height: 3px;
  background: #FF758C;
  border-radius: 3px;
  transition: transform 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}
.tab-indicator.is-right {
  transform: translateX(250%); /* 因为宽度是 20%，跨越半区需要计算 */
}

/* 表单输入区 */
.app-form-body {
  flex: 1;
}

.app-input-group {
  margin-bottom: 20px;
}

/* 胖乎乎的极度亲和输入框 (无边框，纯底色) */
:deep(.app-input .el-input__wrapper) {
  background: #F3F4F6;
  border: 1px solid transparent;
  border-radius: 16px; /* 更大的圆角 */
  padding: 0 16px;
  height: 52px;
  box-shadow: none !important;
  transition: all 0.2s ease;
}

:deep(.app-input .el-input__wrapper:hover) {
  background: #E5E7EB;
}

:deep(.app-input .el-input__wrapper.is-focus) {
  background: #ffffff;
  border-color: #FF758C;
  box-shadow: 0 0 0 3px rgba(255, 117, 140, 0.15) !important;
}

:deep(.app-input .el-input__inner) {
  font-size: 15px;
  color: #111827;
}
:deep(.app-input .el-input__inner::placeholder) { color: #9CA3AF; }

.app-icon {
  color: #9CA3AF;
  font-size: 18px;
  margin-right: 8px;
}
:deep(.app-input .el-input__wrapper.is-focus) .app-icon { color: #FF758C; }

/* 状态图标 */
.app-status { font-size: 18px; font-weight: bold; }
.app-status.success { color: #10B981; }
.app-status.error { color: #EF4444; }

/* 验证码布局 */
.captcha-flex {
  display: flex;
  gap: 12px;
}
.flex-1 { flex: 1; margin-bottom: 0; }

.app-captcha-box {
  width: 110px;
  height: 52px;
  border-radius: 16px;
  border: 1px solid transparent;
  background: #F3F4F6;
  overflow: hidden;
  cursor: pointer;
  flex-shrink: 0;
  transition: background 0.2s;
}
.app-captcha-box:hover { background: #E5E7EB; }
.app-captcha-box canvas { display: block; width: 100%; height: 100%; }

/* 选项 */
.app-form-options {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

:deep(.el-checkbox__label) { color: #6B7280; font-size: 14px; }
:deep(.el-checkbox__input.is-checked .el-checkbox__inner) { background-color: #FF758C; border-color: #FF758C; }

.app-forgot-link {
  font-size: 14px;
  color: #6B7280;
  text-decoration: none;
}
.app-forgot-link:hover { color: #FF758C; }

/* 核心圆润渐变按钮 */
.app-submit-btn {
  width: 100%;
  height: 52px;
  border-radius: 26px; /* 极致圆润 */
  font-size: 16px;
  font-weight: 700;
  background: linear-gradient(to right, #FF758C, #FF7EB3);
  border: none;
  color: white;
  transition: all 0.2s;
  box-shadow: 0 4px 14px rgba(255, 117, 140, 0.3);
  letter-spacing: 1px;
}

.app-submit-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(255, 117, 140, 0.4);
}

.app-submit-btn:active { transform: translateY(0); }

.app-submit-btn:disabled {
  background: #E5E7EB;
  color: #9CA3AF;
  cursor: not-allowed;
  transform: none;
  box-shadow: none;
}

/* 底部协议 */
.app-agreement {
  text-align: center;
  font-size: 12px;
  color: #9CA3AF;
  margin-top: 24px;
}
.app-agreement a {
  color: #FF758C;
  text-decoration: none;
}

/* 错误提示 */
:deep(.el-form-item.is-error .app-input .el-input__wrapper) { 
  border-color: #EF4444; 
  background: #FEF2F2;
}
:deep(.el-form-item__error) { color: #EF4444; font-size: 12px; padding-top: 4px; margin-left: 12px; }

/* 淡入淡出动画 */
.app-fade-enter-active, .app-fade-leave-active { transition: opacity 0.2s ease, transform 0.2s ease; }
.app-fade-enter-from { opacity: 0; transform: translateY(5px); }
.app-fade-leave-to { opacity: 0; transform: translateY(-5px); }
</style>