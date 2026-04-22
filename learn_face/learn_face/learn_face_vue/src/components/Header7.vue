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
              <el-button @click="openRegisterDialog" type="primary" class="auth-btn register-btn">免费加入</el-button>
            </template>
          </el-space>
        </el-col>
      </el-row>
    </div>

    <!-- 氧气治愈风 无缝弹窗 -->
    <el-dialog v-model="showAuthDialog" title="" width="860px" :show-close="false" class="zen-auth-dialog" center
      :close-on-click-modal="true" :modal-class="'zen-modal-backdrop'" :append-to-body="false">
      
      <div class="zen-container">
        
        <!-- 左侧：治愈系有机流体区域 -->
        <div class="zen-cover-panel">
          
          <!-- 纯 CSS 缓慢变形的有机色块 -->
          <div class="morph-blob blob-1"></div>
          <div class="morph-blob blob-2"></div>

          <div class="zen-cover-content">
            <div class="zen-tag">Lifestyle</div>
            <h2 class="zen-title">互联网上的<br>一处静谧角落</h2>
            <p class="zen-desc">深呼吸，慢下来。在这里记录生活，遇见同频的灵魂，找回内心的平静。</p>
            
            <div class="zen-features">
              <div class="feature-line">
                <div class="f-icon"><el-icon><Sunny /></el-icon></div>
                <span>开启元气满满的一天</span>
              </div>
              <div class="feature-line">
                <div class="f-icon"><el-icon><ChatDotRound /></el-icon></div>
                <span>温暖、友善的交流氛围</span>
              </div>
              <div class="feature-line">
                <div class="f-icon"><el-icon><Compass /></el-icon></div>
                <span>发现广阔的生活灵感</span>
              </div>
            </div>
          </div>
        </div>

        <!-- 右侧：干净呼吸感的表单区 -->
        <div class="zen-form-panel">
          <!-- 右上角关闭按钮 -->
          <button class="zen-close-btn" @click="showAuthDialog = false">
            <el-icon><Close /></el-icon>
          </button>

          <!-- 优雅的无下划线大字号 Tabs -->
          <div class="zen-tabs">
            <div class="zen-tab-item" :class="{ active: !isRegisterMode }" @click="switchToLogin">登录</div>
            <div class="zen-tab-item" :class="{ active: isRegisterMode }" @click="switchToRegister">注册</div>
          </div>

          <div class="zen-form-body">
            <transition name="zen-fade" mode="out-in">
              
              <!-- 登录表单 -->
              <div v-if="!isRegisterMode" key="login" class="form-wrapper">
                <el-form :model="loginForm" :rules="loginRules" ref="loginFormRef" @submit.prevent="handleLogin">
                  
                  <div class="zen-input-group">
                    <el-form-item prop="email">
                      <el-input ref="firstLoginInputRef" v-model="loginForm.email" placeholder="输入手机号 / 邮箱" size="large" @keyup.enter="handleLogin" :disabled="loading" class="zen-input" clearable>
                        <template #prefix><el-icon class="zen-icon"><UserIcon /></el-icon></template>
                      </el-input>
                    </el-form-item>
                  </div>

                  <div class="zen-input-group">
                    <el-form-item prop="password">
                      <el-input v-model="loginForm.password" type="password" placeholder="输入密码" show-password size="large" @keyup.enter="handleLogin" :disabled="loading" class="zen-input">
                        <template #prefix><el-icon class="zen-icon"><Lock /></el-icon></template>
                      </el-input>
                    </el-form-item>
                  </div>

                  <div class="zen-input-group">
                    <div class="captcha-flex">
                      <el-form-item prop="captcha" class="flex-1">
                        <el-input v-model="loginForm.captcha" placeholder="输入验证码" size="large" @keyup.enter="handleLogin" :disabled="loading" class="zen-input" maxlength="4">
                          <template #prefix><el-icon class="zen-icon"><Key /></el-icon></template>
                        </el-input>
                      </el-form-item>
                      <div class="zen-captcha-box" @click="handleRefreshLoginCaptcha" title="点击换一张">
                        <canvas ref="loginCaptchaRef" width="116" height="52"></canvas>
                      </div>
                    </div>
                  </div>

                  <div class="zen-form-options">
                    <el-checkbox v-model="rememberMe">保持登录状态</el-checkbox>
                    <a href="#" class="zen-forgot" @click.prevent="handleForgotPassword">忘记密码？</a>
                  </div>

                  <el-button @click="handleLogin" :loading="loading" type="primary" size="large" class="zen-submit-btn" :disabled="!isLoginFormValid">
                    {{ loading ? '登录中...' : '登 录' }}
                  </el-button>
                </el-form>
              </div>

              <!-- 注册表单 -->
              <div v-else key="register" class="form-wrapper">
                <el-form :model="registerForm" :rules="registerRules" ref="registerFormRef" @submit.prevent="handleRegister">
                  
                  <div class="zen-input-group">
                    <el-form-item prop="email">
                      <el-input ref="firstRegisterInputRef" v-model="registerForm.email" placeholder="设置一个好记的账号" size="large" :disabled="loading" class="zen-input" @input="validateEmailRealtime" clearable>
                        <template #prefix><el-icon class="zen-icon"><UserIcon /></el-icon></template>
                        <template #suffix><el-icon v-if="emailValidated" class="zen-status success"><Check /></el-icon></template>
                      </el-input>
                    </el-form-item>
                  </div>

                  <div class="zen-input-group">
                    <el-form-item prop="password">
                      <el-input v-model="registerForm.password" type="password" placeholder="设置密码 (6位以上)" show-password size="large" :disabled="loading" class="zen-input">
                        <template #prefix><el-icon class="zen-icon"><Lock /></el-icon></template>
                      </el-input>
                    </el-form-item>
                  </div>

                  <div class="zen-input-group">
                    <el-form-item prop="confirmPassword">
                      <el-input v-model="registerForm.confirmPassword" type="password" placeholder="再次输入密码" show-password size="large" :disabled="loading" class="zen-input" @input="validatePasswordMatch">
                        <template #prefix><el-icon class="zen-icon"><Lock /></el-icon></template>
                        <template #suffix>
                          <el-icon v-if="passwordMatched" class="zen-status success"><Check /></el-icon>
                          <el-icon v-else-if="registerForm.confirmPassword && !passwordMatched" class="zen-status error"><CircleClose /></el-icon>
                        </template>
                      </el-input>
                    </el-form-item>
                  </div>

                  <div class="zen-input-group">
                    <div class="captcha-flex">
                      <el-form-item prop="captcha" class="flex-1">
                        <el-input v-model="registerForm.captcha" placeholder="输入验证码" size="large" @keyup.enter="handleRegister" :disabled="loading" class="zen-input" maxlength="4">
                          <template #prefix><el-icon class="zen-icon"><Key /></el-icon></template>
                        </el-input>
                      </el-form-item>
                      <div class="zen-captcha-box" @click="handleRefreshRegisterCaptcha" title="点击换一张">
                        <canvas ref="registerCaptchaRef" width="116" height="52"></canvas>
                      </div>
                    </div>
                  </div>

                  <el-button @click="handleRegister" :loading="loading" type="primary" size="large" class="zen-submit-btn" :disabled="!isRegisterFormValid">
                    {{ loading ? '注册中...' : '同 意 并 注 册' }}
                  </el-button>
                  
                  <div class="zen-agreement">
                    加入即代表您同意我们的 <a href="#">服务协议</a> 与 <a href="#">隐私政策</a>
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
import { User as UserIcon, Lock, ArrowDown, Key, Close, Sunny, Compass, ChatDotRound, Check, CircleClose } from '@element-plus/icons-vue'
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

const appTitle = computed(() => import.meta.env.VITE_APP_TITLE || '治愈社区')

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

// 验证码生成：配合治愈系的护眼绿底色
const drawCaptcha = (canvasElement, text) => {
  if (!canvasElement) return
  const ctx = canvasElement.getContext('2d')
  const width = canvasElement.width
  const height = canvasElement.height

  ctx.clearRect(0, 0, width, height)
  
  // 护眼薄荷灰底
  ctx.fillStyle = '#F0FDF4'
  ctx.fillRect(0, 0, width, height)

  // 柔和的绿色干扰线
  for (let i = 0; i < 4; i++) {
    ctx.strokeStyle = `rgba(16, 185, 129, 0.2)`
    ctx.lineWidth = 1
    ctx.beginPath()
    ctx.moveTo(Math.random() * width, Math.random() * height)
    ctx.lineTo(Math.random() * width, Math.random() * height)
    ctx.stroke()
  }

  // 噪点
  for (let i = 0; i < 20; i++) {
    ctx.fillStyle = `rgba(16, 185, 129, 0.2)`
    ctx.beginPath()
    ctx.arc(Math.random() * width, Math.random() * height, 1, 0, 2 * Math.PI)
    ctx.fill()
  }

  // 字体：深森林绿
  ctx.font = 'bold 24px Arial, sans-serif'
  ctx.textBaseline = 'middle'
  for (let i = 0; i < text.length; i++) {
    ctx.fillStyle = `#064E3B`
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

/* ==================== 氧气治愈森系风 (Version 11) ==================== */

/* 遮罩层 */
:deep(.zen-modal-backdrop) {
  background: rgba(17, 24, 39, 0.5);
  backdrop-filter: blur(4px);
}

/* ★ 剥离原生外壳 ★ */
:deep(.el-dialog.zen-auth-dialog) {
  background: transparent !important;
  box-shadow: none !important;
  border-radius: 0 !important;
  padding: 0 !important;
  border: none !important;
}

:deep(.el-dialog.zen-auth-dialog .el-dialog__header) { display: none !important; }
:deep(.el-dialog.zen-auth-dialog .el-dialog__body) { padding: 0 !important; background: transparent !important; }

/* 主容器：无缝贴边，圆角裁切 */
.zen-container {
  display: flex;
  width: 100%;
  height: 580px;
  background: #ffffff;
  border-radius: 24px;
  overflow: hidden;
  box-shadow: 0 24px 50px rgba(0, 0, 0, 0.2);
}

/* --- 左侧：森林氧气渐变区 --- */
.zen-cover-panel {
  width: 380px; 
  background: linear-gradient(135deg, #ECFDF5 0%, #D1FAE5 100%); /* 清晨薄荷绿 */
  position: relative;
  padding: 48px 40px;
  display: flex;
  flex-direction: column;
  justify-content: center;
}

/* 有机流体色块动画 (仿佛在呼吸) */
.morph-blob {
  position: absolute;
  background: linear-gradient(135deg, rgba(255,255,255,0.6), rgba(255,255,255,0.1));
  backdrop-filter: blur(5px);
  animation: morph 8s ease-in-out infinite;
  z-index: 0;
}
.blob-1 {
  width: 280px; height: 280px; top: -50px; left: -50px;
  border-radius: 40% 60% 60% 40% / 60% 30% 70% 40%;
}
.blob-2 {
  width: 200px; height: 200px; bottom: 20px; right: -40px;
  background: linear-gradient(135deg, rgba(16, 185, 129, 0.1), rgba(16, 185, 129, 0.05));
  border-radius: 30% 70% 70% 30% / 30% 30% 70% 70%;
  animation-delay: -4s;
  animation-direction: alternate-reverse;
}

@keyframes morph {
  0% { border-radius: 40% 60% 60% 40% / 60% 30% 70% 40%; }
  50% { border-radius: 30% 60% 70% 40% / 50% 60% 30% 60%; }
  100% { border-radius: 40% 60% 60% 40% / 60% 30% 70% 40%; }
}

.zen-cover-content {
  position: relative;
  z-index: 1;
}

.zen-tag {
  display: inline-block;
  padding: 6px 14px;
  background: rgba(255, 255, 255, 0.5);
  color: #064E3B; /* 深林绿 */
  border-radius: 20px;
  font-size: 13px;
  font-weight: 700;
  letter-spacing: 1px;
  margin-bottom: 24px;
  box-shadow: 0 4px 10px rgba(16, 185, 129, 0.1);
}

.zen-title {
  font-size: 32px;
  font-weight: 800;
  color: #064E3B;
  margin: 0 0 16px 0;
  line-height: 1.4;
}

.zen-desc {
  font-size: 15px;
  color: #047857;
  line-height: 1.7;
  margin: 0 0 48px 0;
}

.zen-features {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.feature-line {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 15px;
  font-weight: 600;
  color: #064E3B;
}

.f-icon {
  width: 38px;
  height: 38px;
  background: #ffffff;
  color: #10B981;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  box-shadow: 0 4px 12px rgba(16, 185, 129, 0.15);
}

/* --- 右侧：表单操作区 --- */
.zen-form-panel {
  flex: 1;
  position: relative;
  padding: 48px 56px; 
  background: #ffffff;
  display: flex;
  flex-direction: column;
}

/* 关闭按钮 */
.zen-close-btn {
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
  transition: all 0.3s;
  z-index: 10;
}
.zen-close-btn:hover { background: #E5E7EB; color: #064E3B; transform: rotate(90deg); }

/* 极其优雅的字体缩放 Tabs (无下划线，全靠字号对比) */
.zen-tabs {
  display: flex;
  align-items: baseline;
  gap: 24px;
  margin-bottom: 40px;
}

.zen-tab-item {
  font-size: 18px;
  font-weight: 600;
  color: #9CA3AF;
  cursor: pointer;
  transition: all 0.3s ease;
}
.zen-tab-item:hover { color: #047857; }
.zen-tab-item.active {
  color: #064E3B;
  font-size: 28px;
  font-weight: 800;
}

/* 表单输入区 */
.zen-form-body {
  flex: 1;
}

.zen-input-group {
  margin-bottom: 24px; 
}

/* 呼吸感输入框 */
:deep(.zen-input .el-input__wrapper) {
  background: #F9FAFB;
  border: 1px solid transparent;
  border-radius: 14px;
  padding: 0 16px;
  height: 54px;
  box-shadow: none !important;
  transition: all 0.3s ease;
}

:deep(.zen-input .el-input__wrapper:hover) {
  background: #F3F4F6;
}

:deep(.zen-input .el-input__wrapper.is-focus) {
  background: #ffffff;
  border-color: #10B981;
  box-shadow: 0 0 0 4px rgba(16, 185, 129, 0.15) !important;
}

:deep(.zen-input .el-input__inner) {
  font-size: 15px;
  color: #111827;
}

:deep(.zen-input .el-input__inner::placeholder) { color: #9CA3AF; }

.zen-icon {
  color: #9CA3AF;
  font-size: 20px;
  margin-right: 8px;
  transition: color 0.3s;
}
:deep(.zen-input .el-input__wrapper.is-focus) .zen-icon { color: #10B981; }

/* 状态图标 */
.zen-status { font-size: 20px; font-weight: bold; }
.zen-status.success { color: #10B981; }
.zen-status.error { color: #EF4444; }

/* 验证码布局 */
.captcha-flex {
  display: flex;
  gap: 12px;
}
.flex-1 { flex: 1; margin-bottom: 0; }

.zen-captcha-box {
  width: 120px;
  height: 54px;
  border-radius: 14px;
  border: 1px solid transparent;
  overflow: hidden;
  cursor: pointer;
  background: #F9FAFB;
  flex-shrink: 0;
  transition: background 0.3s;
}
.zen-captcha-box:hover { background: #F3F4F6; }
.zen-captcha-box canvas { display: block; width: 100%; height: 100%; }

/* 底部选项 */
.zen-form-options {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 32px;
}

:deep(.el-checkbox__label) { color: #6B7280; font-size: 14px; }
:deep(.el-checkbox__input.is-checked .el-checkbox__inner) { background-color: #10B981; border-color: #10B981; }

.zen-forgot {
  font-size: 14px;
  color: #6B7280;
  text-decoration: none;
  transition: color 0.2s;
}
.zen-forgot:hover { color: #10B981; }

/* 生机翡翠绿按钮 */
.zen-submit-btn {
  width: 100%;
  height: 54px;
  border-radius: 14px;
  font-size: 16px;
  font-weight: bold;
  background: #10B981;
  border: none;
  color: white;
  transition: all 0.3s;
  box-shadow: 0 4px 14px rgba(16, 185, 129, 0.25);
  letter-spacing: 1px;
}

.zen-submit-btn:hover {
  background: #059669;
  transform: translateY(-2px);
  box-shadow: 0 8px 20px rgba(16, 185, 129, 0.35);
}

.zen-submit-btn:active { transform: translateY(0); }

.zen-submit-btn:disabled {
  background: #F3F4F6;
  color: #9CA3AF;
  cursor: not-allowed;
  transform: none;
  box-shadow: none;
}

/* 底部协议 */
.zen-agreement {
  text-align: center;
  font-size: 13px;
  color: #9CA3AF;
  margin-top: 24px;
}
.zen-agreement a {
  color: #10B981;
  text-decoration: none;
}

/* 错误提示 */
:deep(.el-form-item.is-error .zen-input .el-input__wrapper) { border-color: #EF4444; background: #FEF2F2; }
:deep(.el-form-item__error) { color: #EF4444; font-size: 12px; padding-top: 4px; }

/* 淡入淡出动画 */
.zen-fade-enter-active, .zen-fade-leave-active { transition: opacity 0.2s ease, transform 0.2s ease; }
.zen-fade-enter-from { opacity: 0; transform: translateY(5px); }
.zen-fade-leave-to { opacity: 0; transform: translateY(-5px); }

/* 响应式 */
@media (max-width: 850px) {
  :deep(.el-dialog.zen-auth-dialog) { width: 90% !important; max-width: 440px; }
  .zen-cover-panel { display: none; }
  .zen-form-panel { padding: 40px 32px; }
}
</style>