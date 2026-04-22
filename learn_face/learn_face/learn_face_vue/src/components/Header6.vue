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

    <!-- 高奢生活方式风 无缝弹窗 (已修复隐形BUG) -->
    <el-dialog v-model="showAuthDialog" title="" width="880px" :show-close="false" class="aurora-auth-dialog" center
      :close-on-click-modal="true" :modal-class="'aurora-modal-backdrop'" :append-to-body="false">
      
      <div class="aurora-container">
        
        <!-- 左侧：极光弥散渐变区 -->
        <div class="aurora-panel">
          <!-- 极光色块 -->
          <div class="mesh-blob color-1"></div>
          <div class="mesh-blob color-2"></div>
          <div class="mesh-blob color-3"></div>
          
          <!-- 磨砂玻璃质感的文字容器 -->
          <div class="aurora-content-glass">
            <div class="brand-chip">Inspiration</div>
            <h2 class="aurora-title">唤醒你的<br>每一份灵感</h2>
            <p class="aurora-desc">加入千万创作者的聚集地，发现生活美学，遇见品味相投的伙伴。</p>
          </div>
        </div>

        <!-- 右侧：高雅留白的表单操作区 -->
        <div class="aurora-form-panel">
          <!-- 右上角精巧关闭按钮 -->
          <button class="aurora-close-btn" @click="showAuthDialog = false">
            <el-icon><Close /></el-icon>
          </button>

          <!-- 优雅的文字底衬 Tabs -->
          <div class="aurora-tabs">
            <div class="tab-item" :class="{ active: !isRegisterMode }" @click="switchToLogin">账号登录</div>
            <div class="tab-item" :class="{ active: isRegisterMode }" @click="switchToRegister">注册新号</div>
            <div class="tab-indicator" :class="{ 'is-right': isRegisterMode }"></div>
          </div>

          <div class="aurora-form-body">
            <transition name="aurora-fade" mode="out-in">
              
              <!-- 登录表单 -->
              <div v-if="!isRegisterMode" key="login" class="form-wrapper">
                <div class="greeting-text">欢迎回来，继续你的探索之旅</div>
                <el-form :model="loginForm" :rules="loginRules" ref="loginFormRef" @submit.prevent="handleLogin">
                  
                  <div class="aurora-input-group">
                    <el-form-item prop="email">
                      <!-- 全胶囊输入框 -->
                      <el-input ref="firstLoginInputRef" v-model="loginForm.email" placeholder="手机号 / 邮箱" size="large" @keyup.enter="handleLogin" :disabled="loading" class="pill-input" clearable>
                        <template #prefix><el-icon class="aurora-icon"><User /></el-icon></template>
                      </el-input>
                    </el-form-item>
                  </div>

                  <div class="aurora-input-group">
                    <el-form-item prop="password">
                      <el-input v-model="loginForm.password" type="password" placeholder="输入密码" show-password size="large" @keyup.enter="handleLogin" :disabled="loading" class="pill-input">
                        <template #prefix><el-icon class="aurora-icon"><Lock /></el-icon></template>
                      </el-input>
                    </el-form-item>
                  </div>

                  <div class="aurora-input-group">
                    <div class="captcha-flex">
                      <el-form-item prop="captcha" class="flex-1">
                        <el-input v-model="loginForm.captcha" placeholder="验证码" size="large" @keyup.enter="handleLogin" :disabled="loading" class="pill-input" maxlength="4">
                          <template #prefix><el-icon class="aurora-icon"><Key /></el-icon></template>
                        </el-input>
                      </el-form-item>
                      <div class="aurora-captcha-box" @click="handleRefreshLoginCaptcha" title="换一张">
                        <canvas ref="loginCaptchaRef" width="116" height="52"></canvas>
                      </div>
                    </div>
                  </div>

                  <div class="aurora-form-options">
                    <el-checkbox v-model="rememberMe">保持登录</el-checkbox>
                    <a href="#" class="aurora-forgot" @click.prevent="handleForgotPassword">忘记密码？</a>
                  </div>

                  <el-button @click="handleLogin" :loading="loading" type="primary" size="large" class="aurora-submit-btn" :disabled="!isLoginFormValid">
                    {{ loading ? '验证中...' : '登 录' }}
                  </el-button>
                </el-form>
              </div>

              <!-- 注册表单 -->
              <div v-else key="register" class="form-wrapper">
                <div class="greeting-text">只需几步，开启你的专属社区</div>
                <el-form :model="registerForm" :rules="registerRules" ref="registerFormRef" @submit.prevent="handleRegister">
                  
                  <div class="aurora-input-group">
                    <el-form-item prop="email">
                      <el-input ref="firstRegisterInputRef" v-model="registerForm.email" placeholder="设置你的专属账号" size="large" :disabled="loading" class="pill-input" @input="validateEmailRealtime" clearable>
                        <template #prefix><el-icon class="aurora-icon"><User /></el-icon></template>
                        <template #suffix><el-icon v-if="emailValidated" class="aurora-status success"><Check /></el-icon></template>
                      </el-input>
                    </el-form-item>
                  </div>

                  <div class="aurora-input-group">
                    <el-form-item prop="password">
                      <el-input v-model="registerForm.password" type="password" placeholder="设置密码 (最少6位)" show-password size="large" :disabled="loading" class="pill-input">
                        <template #prefix><el-icon class="aurora-icon"><Lock /></el-icon></template>
                      </el-input>
                    </el-form-item>
                  </div>

                  <div class="aurora-input-group">
                    <el-form-item prop="confirmPassword">
                      <el-input v-model="registerForm.confirmPassword" type="password" placeholder="再次确认密码" show-password size="large" :disabled="loading" class="pill-input" @input="validatePasswordMatch">
                        <template #prefix><el-icon class="aurora-icon"><Lock /></el-icon></template>
                        <template #suffix>
                          <el-icon v-if="passwordMatched" class="aurora-status success"><Check /></el-icon>
                          <el-icon v-else-if="registerForm.confirmPassword && !passwordMatched" class="aurora-status error"><CircleClose /></el-icon>
                        </template>
                      </el-input>
                    </el-form-item>
                  </div>

                  <div class="aurora-input-group">
                    <div class="captcha-flex">
                      <el-form-item prop="captcha" class="flex-1">
                        <el-input v-model="registerForm.captcha" placeholder="验证码" size="large" @keyup.enter="handleRegister" :disabled="loading" class="pill-input" maxlength="4">
                          <template #prefix><el-icon class="aurora-icon"><Key /></el-icon></template>
                        </el-input>
                      </el-form-item>
                      <div class="aurora-captcha-box" @click="handleRefreshRegisterCaptcha" title="换一张">
                        <canvas ref="registerCaptchaRef" width="116" height="52"></canvas>
                      </div>
                    </div>
                  </div>

                  <el-button @click="handleRegister" :loading="loading" type="primary" size="large" class="aurora-submit-btn" :disabled="!isRegisterFormValid">
                    {{ loading ? '注册中...' : '注册并加入' }}
                  </el-button>
                  
                  <div class="aurora-agreement">
                    注册代表你同意 <a href="#">《用户协议》</a>
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
import { User, Lock, ArrowDown, Key, Close, Check, CircleClose } from '@element-plus/icons-vue'
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

const appTitle = computed(() => import.meta.env.VITE_APP_TITLE || '灵感社区')

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

// 极光风验证码：柔和的淡紫底色，清爽不杂乱
const drawCaptcha = (canvasElement, text) => {
  if (!canvasElement) return
  const ctx = canvasElement.getContext('2d')
  const width = canvasElement.width
  const height = canvasElement.height

  ctx.clearRect(0, 0, width, height)
  
  ctx.fillStyle = '#F8F7FA' // 淡紫灰
  ctx.fillRect(0, 0, width, height)

  for (let i = 0; i < 3; i++) {
    ctx.strokeStyle = `rgba(167, 139, 250, 0.2)` // 紫色干扰线
    ctx.lineWidth = 2
    ctx.beginPath()
    ctx.moveTo(Math.random() * width, Math.random() * height)
    ctx.lineTo(Math.random() * width, Math.random() * height)
    ctx.stroke()
  }

  for (let i = 0; i < 20; i++) {
    ctx.fillStyle = `rgba(167, 139, 250, 0.2)`
    ctx.beginPath()
    ctx.arc(Math.random() * width, Math.random() * height, 1, 0, 2 * Math.PI)
    ctx.fill()
  }

  ctx.font = 'bold 24px Arial, sans-serif'
  ctx.textBaseline = 'middle'
  for (let i = 0; i < text.length; i++) {
    ctx.fillStyle = `#8B5CF6` // 品牌紫
    const x = 24 * i + 14
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


/* ==================== 高奢极光分栏风 弹窗样式 (Version 10 FIX) ==================== */

/* 遮罩层，轻量高级 */
:deep(.aurora-modal-backdrop) {
  background: rgba(17, 24, 39, 0.6);
  backdrop-filter: blur(8px);
}

/* ★ 彻底剥离 Element Plus 默认样式，修复隐形BUG ★ */
:deep(.el-dialog.aurora-auth-dialog) {
  background: transparent !important;
  box-shadow: none !important;
  border-radius: 0 !important;
  padding: 0 !important;
  border: none !important;
}

/* 隐藏原生头部 */
:deep(.el-dialog.aurora-auth-dialog .el-dialog__header) {
  display: none !important;
}

/* 清除原生身体内边距，但【绝对不能隐藏】！ */
:deep(.el-dialog.aurora-auth-dialog .el-dialog__body) {
  padding: 0 !important;
  background: transparent !important;
}

/* 核心 Flex 容器，接管边框与裁切 */
.aurora-container {
  display: flex;
  width: 100%;
  height: 560px;
  background: #ffffff;
  border-radius: 28px; /* 极度圆润的外壳 */
  overflow: hidden;    /* 完美裁切掉内部的直角，实现无缝贴边 */
  box-shadow: 0 30px 60px -12px rgba(0, 0, 0, 0.3);
}

/* --- 左侧：极光弥散渐变区 --- */
.aurora-panel {
  width: 400px;
  position: relative;
  background: #FDF4FF; /* 底色 */
  overflow: hidden;
  display: flex;
  flex-direction: column;
  justify-content: flex-end;
  padding: 48px;
}

/* 纯 CSS 实现的动态流光极光 (Mesh Gradient) */
.mesh-blob {
  position: absolute;
  border-radius: 50%;
  filter: blur(60px);
  opacity: 0.8;
  z-index: 0;
  animation: float 10s ease-in-out infinite alternate;
}
.color-1 { width: 300px; height: 300px; background: #FDA4AF; top: -50px; left: -50px; animation-delay: 0s; } /* 柔和桃红 */
.color-2 { width: 280px; height: 280px; background: #C084FC; bottom: -20px; right: -80px; animation-delay: -2s; } /* 优雅紫 */
.color-3 { width: 200px; height: 200px; background: #93C5FD; top: 40%; left: 10%; animation-delay: -4s; } /* 天际蓝 */

@keyframes float {
  0% { transform: translateY(0) scale(1); }
  100% { transform: translateY(-20px) scale(1.1); }
}

/* 悬浮在极光之上的毛玻璃内容区 */
.aurora-content-glass {
  position: relative;
  z-index: 1;
  background: rgba(255, 255, 255, 0.3);
  border: 1px solid rgba(255, 255, 255, 0.5);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  padding: 32px 24px;
  border-radius: 24px;
  box-shadow: 0 12px 32px rgba(0,0,0,0.05);
}

.brand-chip {
  display: inline-block;
  background: #ffffff;
  color: #8B5CF6;
  padding: 6px 14px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 1px;
  text-transform: uppercase;
  margin-bottom: 20px;
  box-shadow: 0 4px 10px rgba(139, 92, 246, 0.15);
}

.aurora-title {
  font-size: 32px;
  font-weight: 800;
  color: #1F2937;
  margin: 0 0 16px 0;
  line-height: 1.3;
}

.aurora-desc {
  font-size: 15px;
  color: #4B5563;
  line-height: 1.6;
  margin: 0;
}

/* --- 右侧：高雅留白的表单区 --- */
.aurora-form-panel {
  flex: 1;
  background: #ffffff;
  position: relative;
  padding: 40px 56px;
  display: flex;
  flex-direction: column;
}

/* 浅灰色精致关闭按钮 */
.aurora-close-btn {
  position: absolute;
  top: 24px;
  right: 24px;
  background: #F3F4F6;
  border: none;
  color: #9CA3AF;
  width: 36px;
  height: 36px;
  border-radius: 50%;
  font-size: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.2s;
  z-index: 10;
}
.aurora-close-btn:hover { background: #E5E7EB; color: #1F2937; transform: rotate(90deg); }

/* 文字指示 Tabs (极其克制高级) */
.aurora-tabs {
  display: flex;
  position: relative;
  margin-bottom: 24px;
  margin-top: 10px;
}

.tab-item {
  font-size: 18px;
  font-weight: 600;
  color: #9CA3AF;
  margin-right: 32px;
  cursor: pointer;
  transition: all 0.3s;
  padding-bottom: 8px;
}
.tab-item:hover { color: #4B5563; }
.tab-item.active { color: #111827; font-weight: 800; }

.tab-indicator {
  position: absolute;
  bottom: 0;
  left: 0;
  width: 32px;
  height: 4px;
  background: #8B5CF6; /* 品牌紫 */
  border-radius: 4px;
  transition: transform 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}
.tab-indicator.is-right {
  transform: translateX(104px); /* 计算移动距离 */
}

/* 问候语 */
.greeting-text {
  font-size: 14px;
  color: #6B7280;
  margin-bottom: 32px;
}

/* 表单输入区 */
.aurora-form-body {
  flex: 1;
}

.aurora-input-group {
  margin-bottom: 20px;
}

/* ★ 全胶囊状输入框 (Pill-shaped) ★ */
:deep(.pill-input .el-input__wrapper) {
  background: #F3F4F6;
  border: 1px solid transparent;
  border-radius: 50px; /* 极端圆润，C端最爱 */
  padding: 0 20px;
  height: 52px;
  box-shadow: none !important;
  transition: all 0.3s ease;
}

:deep(.pill-input .el-input__wrapper:hover) {
  background: #E5E7EB;
}

:deep(.pill-input .el-input__wrapper.is-focus) {
  background: #ffffff;
  border-color: #8B5CF6;
  box-shadow: 0 0 0 4px rgba(139, 92, 246, 0.15) !important;
}

:deep(.pill-input .el-input__inner) {
  font-size: 15px;
  color: #111827;
}

:deep(.pill-input .el-input__inner::placeholder) { color: #9CA3AF; }

.aurora-icon {
  color: #9CA3AF;
  font-size: 18px;
  margin-right: 8px;
  transition: color 0.3s;
}
:deep(.pill-input .el-input__wrapper.is-focus) .aurora-icon { color: #8B5CF6; }

/* 状态图标 */
.aurora-status { font-size: 18px; font-weight: bold; }
.aurora-status.success { color: #10B981; }
.aurora-status.error { color: #EF4444; }

/* 验证码布局 */
.captcha-flex {
  display: flex;
  gap: 12px;
}
.flex-1 { flex: 1; margin-bottom: 0; }

.aurora-captcha-box {
  width: 120px;
  height: 52px;
  border-radius: 50px; /* 保持与胶囊输入框一致 */
  border: 1px solid transparent;
  overflow: hidden;
  cursor: pointer;
  background: #F3F4F6;
  flex-shrink: 0;
  transition: background 0.2s;
}
.aurora-captcha-box:hover { background: #E5E7EB; }
.aurora-captcha-box canvas { display: block; width: 100%; height: 100%; }

/* 记住我与忘记密码 */
.aurora-form-options {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 32px;
  padding: 0 8px; /* 稍微往里缩一点，配合胶囊框 */
}

:deep(.el-checkbox__label) { color: #6B7280; font-size: 14px; }
:deep(.el-checkbox__input.is-checked .el-checkbox__inner) { background-color: #8B5CF6; border-color: #8B5CF6; }

.aurora-forgot {
  font-size: 14px;
  color: #6B7280;
  text-decoration: none;
  transition: color 0.2s;
}
.aurora-forgot:hover { color: #8B5CF6; }

/* 核心胶囊按钮 */
.aurora-submit-btn {
  width: 100%;
  height: 54px;
  border-radius: 50px; /* 胶囊圆角 */
  font-size: 16px;
  font-weight: bold;
  background: linear-gradient(135deg, #8B5CF6, #D946EF); /* 优雅紫粉渐变 */
  border: none;
  color: white;
  transition: all 0.3s;
  box-shadow: 0 8px 20px rgba(139, 92, 246, 0.25);
  letter-spacing: 1px;
}

.aurora-submit-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 12px 24px rgba(139, 92, 246, 0.35);
}

.aurora-submit-btn:active { transform: translateY(0); }

.aurora-submit-btn:disabled {
  background: #E5E7EB;
  color: #9CA3AF;
  cursor: not-allowed;
  transform: none;
  box-shadow: none;
}

/* 底部协议 */
.aurora-agreement {
  text-align: center;
  font-size: 12px;
  color: #9CA3AF;
  margin-top: 24px;
}
.aurora-agreement a {
  color: #8B5CF6;
  text-decoration: none;
}

/* 错误提示 */
:deep(.el-form-item.is-error .pill-input .el-input__wrapper) { border-color: #EF4444; background: #FEF2F2;}
:deep(.el-form-item__error) { color: #EF4444; font-size: 12px; padding-top: 4px; padding-left: 16px; }

/* 淡入淡出动画 */
.aurora-fade-enter-active, .aurora-fade-leave-active { transition: opacity 0.2s ease, transform 0.2s ease; }
.aurora-fade-enter-from { opacity: 0; transform: translateX(10px); }
.aurora-fade-leave-to { opacity: 0; transform: translateX(-10px); }

/* 响应式 */
@media (max-width: 850px) {
  :deep(.el-dialog.aurora-auth-dialog) { width: 90% !important; max-width: 440px; }
  .aurora-panel { display: none; }
  .aurora-form-panel { padding: 40px 32px; }
}
</style>