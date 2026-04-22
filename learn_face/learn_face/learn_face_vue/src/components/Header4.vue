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

    <!-- 终极修复版弹窗：剥离原生外壳 -->
    <el-dialog v-model="showAuthDialog" title="" width="860px" :show-close="false" class="c-auth-dialog" center
      :close-on-click-modal="true" :modal-class="'c-modal-backdrop'" :append-to-body="false">
      
      <div class="c-container">
        
        <!-- 左侧：无缝平铺充满左边的区域 -->
        <div class="c-cover-panel">
          <!-- 背景装饰圆 -->
          <div class="shape circle-1"></div>
          <div class="shape circle-2"></div>

          <div class="c-cover-content">
            <div class="brand-tag">Welcome</div>
            <h2 class="cover-title">探索你的<br>精彩世界</h2>
            <p class="cover-desc">在这里，与数百万用户一起分享生活、发现灵感、结识有趣的灵魂。</p>
            
            <div class="benefit-list">
              <div class="benefit-item">
                <div class="b-icon"><el-icon><Star /></el-icon></div>
                <span>记录每一个闪光瞬间</span>
              </div>
              <div class="benefit-item">
                <div class="b-icon"><el-icon><ChatDotRound /></el-icon></div>
                <span>与同好畅快交流</span>
              </div>
              <div class="benefit-item">
                <div class="b-icon"><el-icon><Compass /></el-icon></div>
                <span>发现懂你的圈子</span>
              </div>
            </div>
          </div>
        </div>

        <!-- 右侧：表单操作区 -->
        <div class="c-form-panel">
          <!-- 右上角关闭 -->
          <button class="c-close-btn" @click="showAuthDialog = false">
            <el-icon><Close /></el-icon>
          </button>

          <!-- 登录/注册 大字号切换 -->
          <div class="c-tabs">
            <h3 class="tab-title" :class="{ active: !isRegisterMode }" @click="switchToLogin">登录</h3>
            <span class="tab-divider">·</span>
            <h3 class="tab-title" :class="{ active: isRegisterMode }" @click="switchToRegister">注册</h3>
          </div>

          <div class="c-form-body">
            <transition name="c-fade" mode="out-in">
              
              <!-- 登录表单 -->
              <div v-if="!isRegisterMode" key="login" class="form-wrapper">
                <el-form :model="loginForm" :rules="loginRules" ref="loginFormRef" @submit.prevent="handleLogin">
                  
                  <div class="c-input-group">
                    <el-form-item prop="email">
                      <el-input ref="firstLoginInputRef" v-model="loginForm.email" placeholder="请输入手机号或邮箱" size="large" @keyup.enter="handleLogin" :disabled="loading" class="c-input" clearable>
                        <template #prefix><el-icon class="c-icon"><UserIcon /></el-icon></template>
                      </el-input>
                    </el-form-item>
                  </div>

                  <div class="c-input-group">
                    <el-form-item prop="password">
                      <el-input v-model="loginForm.password" type="password" placeholder="请输入密码" show-password size="large" @keyup.enter="handleLogin" :disabled="loading" class="c-input">
                        <template #prefix><el-icon class="c-icon"><Lock /></el-icon></template>
                      </el-input>
                    </el-form-item>
                  </div>

                  <div class="c-input-group">
                    <div class="captcha-flex">
                      <el-form-item prop="captcha" class="flex-1">
                        <el-input v-model="loginForm.captcha" placeholder="输入验证码" size="large" @keyup.enter="handleLogin" :disabled="loading" class="c-input" maxlength="4">
                          <template #prefix><el-icon class="c-icon"><Key /></el-icon></template>
                        </el-input>
                      </el-form-item>
                      <div class="c-captcha-box" @click="handleRefreshLoginCaptcha" title="看不清？换一张">
                        <canvas ref="loginCaptchaRef" width="110" height="48"></canvas>
                      </div>
                    </div>
                  </div>

                  <div class="c-form-options">
                    <el-checkbox v-model="rememberMe">自动登录</el-checkbox>
                    <a href="#" class="c-forgot-link" @click.prevent="handleForgotPassword">忘记密码？</a>
                  </div>

                  <el-button @click="handleLogin" :loading="loading" type="primary" size="large" class="c-submit-btn" :disabled="!isLoginFormValid">
                    {{ loading ? '登录中...' : '登录' }}
                  </el-button>
                </el-form>
              </div>

              <!-- 注册表单 -->
              <div v-else key="register" class="form-wrapper">
                <el-form :model="registerForm" :rules="registerRules" ref="registerFormRef" @submit.prevent="handleRegister">
                  
                  <div class="c-input-group">
                    <el-form-item prop="email">
                      <el-input ref="firstRegisterInputRef" v-model="registerForm.email" placeholder="请设置登录账号" size="large" :disabled="loading" class="c-input" @input="validateEmailRealtime" clearable>
                        <template #prefix><el-icon class="c-icon"><UserIcon /></el-icon></template>
                        <template #suffix><el-icon v-if="emailValidated" class="c-status success"><Check /></el-icon></template>
                      </el-input>
                    </el-form-item>
                  </div>

                  <div class="c-input-group">
                    <el-form-item prop="password">
                      <el-input v-model="registerForm.password" type="password" placeholder="请设置密码" show-password size="large" :disabled="loading" class="c-input">
                        <template #prefix><el-icon class="c-icon"><Lock /></el-icon></template>
                      </el-input>
                    </el-form-item>
                  </div>

                  <div class="c-input-group">
                    <el-form-item prop="confirmPassword">
                      <el-input v-model="registerForm.confirmPassword" type="password" placeholder="请再次输入密码" show-password size="large" :disabled="loading" class="c-input" @input="validatePasswordMatch">
                        <template #prefix><el-icon class="c-icon"><Lock /></el-icon></template>
                        <template #suffix>
                          <el-icon v-if="passwordMatched" class="c-status success"><Check /></el-icon>
                          <el-icon v-else-if="registerForm.confirmPassword && !passwordMatched" class="c-status error"><CircleClose /></el-icon>
                        </template>
                      </el-input>
                    </el-form-item>
                  </div>

                  <div class="c-input-group">
                    <div class="captcha-flex">
                      <el-form-item prop="captcha" class="flex-1">
                        <el-input v-model="registerForm.captcha" placeholder="输入验证码" size="large" @keyup.enter="handleRegister" :disabled="loading" class="c-input" maxlength="4">
                          <template #prefix><el-icon class="c-icon"><Key /></el-icon></template>
                        </el-input>
                      </el-form-item>
                      <div class="c-captcha-box" @click="handleRefreshRegisterCaptcha" title="看不清？换一张">
                        <canvas ref="registerCaptchaRef" width="110" height="48"></canvas>
                      </div>
                    </div>
                  </div>

                  <el-button @click="handleRegister" :loading="loading" type="primary" size="large" class="c-submit-btn" :disabled="!isRegisterFormValid">
                    {{ loading ? '注册中...' : '注册' }}
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
import { User as UserIcon, Lock, ArrowDown, Key, Close, Star, Compass, ChatDotRound, Check, CircleClose } from '@element-plus/icons-vue'
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
  
  ctx.fillStyle = '#FFF5F5'
  ctx.fillRect(0, 0, width, height)

  for (let i = 0; i < 4; i++) {
    ctx.strokeStyle = `rgba(255, 107, 107, 0.2)`
    ctx.lineWidth = 1
    ctx.beginPath()
    ctx.moveTo(Math.random() * width, Math.random() * height)
    ctx.lineTo(Math.random() * width, Math.random() * height)
    ctx.stroke()
  }

  for (let i = 0; i < 20; i++) {
    ctx.fillStyle = `rgba(255, 107, 107, 0.15)`
    ctx.beginPath()
    ctx.arc(Math.random() * width, Math.random() * height, 1, 0, 2 * Math.PI)
    ctx.fill()
  }

  ctx.font = 'bold 24px Arial, sans-serif'
  ctx.textBaseline = 'middle'
  for (let i = 0; i < text.length; i++) {
    ctx.fillStyle = `#FF6B6B`
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
/* ==================== 顶部导航栏 (完全保留原版黑底白字，绝不更改) ==================== */
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

/* ==================== 彻底消除白边与重叠外壳的样式 ==================== */

/* 遮罩层背景 */
:deep(.c-modal-backdrop) {
  background: rgba(0, 0, 0, 0.6);
  backdrop-filter: blur(4px);
}

/* ★ 终极绝杀：强制清除 Vue3 + Element Plus 的原生 dialog 样式 */
:deep(.el-dialog.c-auth-dialog) {
  background: transparent !important; /* 透明化原生容器 */
  box-shadow: none !important;        /* 移除原生阴影 */
  border-radius: 0 !important;        /* 移除原生圆角 */
  padding: 0 !important;              /* 移除原生边距 */
  border: none !important;
}

:deep(.el-dialog.c-auth-dialog .el-dialog__header) {
  display: none !important;
}

/* 必须将 .el-dialog__body 也清空，否则会残留白色 padding */
:deep(.el-dialog.c-auth-dialog .el-dialog__body) {
  padding: 0 !important;
  background: transparent !important;
}

/* ★ 内部真正的容器：接管排版、圆角、背景和阴影 */
.c-container {
  display: flex;
  width: 100%;
  height: 560px;
  background: #ffffff;
  border-radius: 24px;       /* 整个弹窗的大圆角在这里 */
  overflow: hidden;          /* 【核心】完美裁切掉左右子面板的四个直角 */
  box-shadow: 0 24px 60px rgba(0, 0, 0, 0.25);
}

/* --- 左侧：红色面板 (无需自己设圆角，靠父级裁切) --- */
.c-cover-panel {
  width: 380px; 
  background: linear-gradient(135deg, #FF7B7B 0%, #FFA86A 100%);
  position: relative;
  /* 只需要管内部的 padding 即可 */
  padding: 48px 40px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  color: #ffffff;
}

/* 装饰几何图形 */
.shape {
  position: absolute;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.08);
}
.circle-1 { width: 350px; height: 350px; top: -120px; left: -120px; }
.circle-2 { width: 250px; height: 250px; bottom: -80px; right: -80px; background: rgba(255, 255, 255, 0.12); }

.c-cover-content {
  position: relative;
  z-index: 1;
}

.brand-tag {
  display: inline-block;
  padding: 4px 14px;
  background: rgba(255, 255, 255, 0.25);
  border-radius: 20px;
  font-size: 13px;
  font-weight: 700;
  letter-spacing: 1px;
  margin-bottom: 24px;
  backdrop-filter: blur(4px);
}

.cover-title {
  font-size: 32px;
  font-weight: 800;
  margin: 0 0 16px 0;
  line-height: 1.35;
  text-shadow: 0 2px 10px rgba(0,0,0,0.1);
}

.cover-desc {
  font-size: 14px;
  color: rgba(255, 255, 255, 0.95);
  line-height: 1.6;
  margin: 0 0 40px 0;
}

.benefit-list {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.benefit-item {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 15px;
  font-weight: 500;
}

.b-icon {
  width: 36px;
  height: 36px;
  background: #ffffff;
  color: #FF6B6B;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  box-shadow: 0 4px 10px rgba(0,0,0,0.1);
}

/* --- 右侧：表单操作区 --- */
.c-form-panel {
  flex: 1;
  position: relative;
  padding: 40px 56px; 
  background: #ffffff;
  display: flex;
  flex-direction: column;
}

/* 右上角关闭按钮 */
.c-close-btn {
  position: absolute;
  top: 20px;
  right: 20px;
  background: #F3F4F6;
  border: none;
  color: #9CA3AF;
  width: 32px;
  height: 32px;
  border-radius: 50%;
  font-size: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.2s;
  z-index: 10;
}
.c-close-btn:hover { background: #E5E7EB; color: #4B5563; }

/* Tabs 切换 */
.c-tabs {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 36px;
  margin-top: 10px;
}

.tab-title {
  font-size: 24px;
  font-weight: 700;
  color: #9CA3AF;
  margin: 0;
  cursor: pointer;
  transition: all 0.3s;
}
.tab-title:hover { color: #4B5563; }
.tab-title.active { color: #111827; font-size: 26px; }

.tab-divider {
  font-size: 24px;
  color: #D1D5DB;
  font-weight: bold;
}

/* 表单输入区 */
.c-form-body {
  flex: 1;
}

.c-input-group {
  margin-bottom: 24px; 
}

/* 亲和的大圆角输入框 */
:deep(.c-input .el-input__wrapper) {
  background: #ffffff;
  border: 1px solid #E5E7EB;
  border-radius: 12px;
  padding: 0 16px;
  height: 52px;
  box-shadow: none !important;
  transition: all 0.2s ease;
}

:deep(.c-input .el-input__wrapper:hover) {
  border-color: #D1D5DB;
}

:deep(.c-input .el-input__wrapper.is-focus) {
  border-color: #FF6B6B;
  box-shadow: 0 0 0 3px rgba(255, 107, 107, 0.1) !important;
}

:deep(.c-input .el-input__inner) {
  font-size: 15px;
  color: #111827;
}

:deep(.c-input .el-input__inner::placeholder) { color: #9CA3AF; }

.c-icon {
  color: #9CA3AF;
  font-size: 18px;
  margin-right: 8px;
}
:deep(.c-input .el-input__wrapper.is-focus) .c-icon { color: #FF6B6B; }

/* 状态图标 */
.c-status { font-size: 18px; font-weight: bold; }
.c-status.success { color: #10B981; }
.c-status.error { color: #EF4444; }

/* 验证码布局 */
.captcha-flex {
  display: flex;
  gap: 12px;
}
.flex-1 { flex: 1; margin-bottom: 0; }

.c-captcha-box {
  width: 120px;
  height: 52px;
  border-radius: 12px;
  border: 1px solid #E5E7EB;
  overflow: hidden;
  cursor: pointer;
  background: #ffffff;
  flex-shrink: 0;
  transition: border-color 0.2s;
}
.c-captcha-box:hover {
  border-color: #D1D5DB;
}
.c-captcha-box canvas { display: block; width: 100%; height: 100%; }

/* 底部选项 */
.c-form-options {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 30px;
}

:deep(.el-checkbox__label) { color: #6B7280; font-size: 14px; font-weight: normal; }
:deep(.el-checkbox__input.is-checked .el-checkbox__inner) { background-color: #FF6B6B; border-color: #FF6B6B; }

.c-forgot-link {
  font-size: 14px;
  color: #6B7280;
  text-decoration: none;
}
.c-forgot-link:hover { color: #FF6B6B; text-decoration: underline; }

/* 核心红色大按钮 */
.c-submit-btn {
  width: 100%;
  height: 52px;
  border-radius: 12px;
  font-size: 16px;
  font-weight: bold;
  background: #FF6B6B;
  border: none;
  color: white;
  transition: all 0.2s;
}

.c-submit-btn:hover {
  background: #FC5C5C;
  transform: translateY(-1px);
  box-shadow: 0 6px 16px rgba(255, 107, 107, 0.3);
}

.c-submit-btn:active { transform: translateY(0); }

.c-submit-btn:disabled {
  background: #F3F4F6;
  color: #D1D5DB;
  cursor: not-allowed;
  transform: none;
  box-shadow: none;
}

/* 错误提示 */
:deep(.el-form-item.is-error .c-input .el-input__wrapper) { border-color: #EF4444; }
:deep(.el-form-item__error) { color: #EF4444; font-size: 12px; padding-top: 4px; }

/* 切换动画 */
.c-fade-enter-active, .c-fade-leave-active { transition: opacity 0.2s ease; }
.c-fade-enter-from, .c-fade-leave-to { opacity: 0; }

/* 响应式 */
@media (max-width: 850px) {
  :deep(.el-dialog.c-auth-dialog) { width: 90% !important; max-width: 440px; }
  .c-cover-panel { display: none; }
  .c-form-panel { padding: 40px 32px; }
}
</style>