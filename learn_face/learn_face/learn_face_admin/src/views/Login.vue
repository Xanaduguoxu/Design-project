<template>
  <div class="login-container">
    <!-- 装饰：背景网格线 -->
    <div class="grid-line vertical v1"></div>
    <div class="grid-line horizontal h1"></div>

    <!-- 主布局容器：高度增加到 600px -->
    <div class="bauhaus-wrapper">
      
      <!-- 区块 1: 品牌展示 (左上 - 红) -->
      <!-- 高度压缩至 110px -->
      <div class="grid-block block-brand">
        <div class="brand-container">
          <div class="logo-square">
            <img src="../assets/logo.png" alt="logo" class="actual-logo">
          </div>
          <div class="title-box">
            <h1 class="app-name">{{ appTitle }}</h1>
            <span class="version-tag">VER. 1.0</span>
          </div>
        </div>
      </div>

      <!-- 区块 2: 装饰/状态 (右上 - 黄) -->
      <div class="grid-block block-status">
        <div class="status-indicator">
          <div class="dot blinking"></div>
          <span>READY</span>
        </div>
        <div class="geo-circle"></div>
      </div>

      <!-- 区块 3: 登录表单 (主区域 - 白) -->
      <div class="grid-block block-form">
        <el-card class="bauhaus-card" shadow="never">
          <el-form :model="loginForm" :rules="rules" ref="loginForm" class="bauhaus-form">
            
            <div class="form-title-group">
              <h2>LOGIN ACCESS</h2>
              <div class="title-bar"></div>
            </div>

            <!-- 输入框区域：紧凑排列 -->
            <div class="inputs-stack">
              <el-form-item prop="username" class="compact-form-item">
                <div class="input-label">USERNAME ID</div>
                <el-input v-model="loginForm.username" prefix-icon="el-icon-user" placeholder="Enter ID..." 
                  @keyup.enter.native="handleLogin" class="swiss-input"></el-input>
              </el-form-item>
              
              <el-form-item prop="password" class="compact-form-item">
                <div class="input-label">SECURE CODE</div>
                <el-input v-model="loginForm.password" prefix-icon="el-icon-lock" type="password" placeholder="Enter Password..." 
                  @keyup.enter.native="handleLogin" show-password class="swiss-input"></el-input>
              </el-form-item>
            </div>

            <!-- 操作区域：上移，确保可见 -->
            <div class="action-stack">
              <!-- 1. 登录按钮 (黑色) -->
              <el-form-item class="no-margin">
                <el-button type="primary" :loading="loading" @click="handleLogin" class="swiss-btn black-btn">
                  <span>AUTHENTICATE</span>
                  <i class="el-icon-right"></i>
                </el-button>
              </el-form-item>
              
              <!-- 2. 找回密码 (黄色实体按钮 - 绝对显眼) -->
              <div class="forgot-block">
                 <el-button @click="showFindPwdDialog" class="swiss-btn yellow-btn">
                  <i class="el-icon-warning-outline"></i> FORGOT PASSWORD
                </el-button>
              </div>
            </div>

          </el-form>
        </el-card>
      </div>

      <!-- 区块 4: 注册入口 (右侧竖条 - 蓝) -->
      <div class="grid-block block-register" @click="showRegisterDialog">
        <div class="register-inner">
          <span class="vertical-text">CREATE NEW ACCOUNT</span>
          <span class="plus-icon">+</span>
        </div>
      </div>

    </div>

    <!-- 注册弹窗 -->
    <el-dialog :visible.sync="registerVisible" :show-close="true" custom-class="swiss-dialog" width="480px" top="8vh">
      <template #title>
        <div class="dialog-header blue-bg">
          <h3>NEW USER REGISTRATION</h3>
        </div>
      </template>
      <div class="dialog-body">
        <el-form :model="registerForm" :rules="registerRules" ref="registerForm" class="swiss-form-stacked">
          <el-form-item label="EMAIL ADDRESS" prop="username">
            <el-input v-model="registerForm.username" placeholder="name@example.com" class="swiss-input small"></el-input>
          </el-form-item>
          <el-form-item label="SET PASSWORD" prop="password">
            <el-input v-model="registerForm.password" type="password" placeholder="******" show-password class="swiss-input small"></el-input>
          </el-form-item>
          <el-form-item label="CONFIRM PASSWORD" prop="confirmPassword">
            <el-input v-model="registerForm.confirmPassword" type="password" placeholder="******" show-password class="swiss-input small"></el-input>
          </el-form-item>
        </el-form>
      </div>
      <template #footer>
        <div class="dialog-footer">
          <el-button class="swiss-btn blue-btn full-width" :loading="registerLoading" @click="handleRegister">
            CONFIRM & JOIN
          </el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 找回密码弹窗 -->
    <el-dialog :visible.sync="findPwdVisible" :show-close="true" custom-class="swiss-dialog" width="480px" top="15vh">
      <template #title>
        <div class="dialog-header red-bg">
          <h3>PASSWORD RECOVERY</h3>
        </div>
      </template>
      <div class="dialog-body">
        <el-form :model="findPwdForm" :rules="findPwdRules" ref="findPwdForm" class="swiss-form-stacked">
          <el-form-item label="YOUR EMAIL" prop="email">
            <el-input v-model="findPwdForm.email" placeholder="email address" class="swiss-input small"></el-input>
          </el-form-item>
          <el-form-item label="VERIFICATION CODE" prop="code">
            <div class="swiss-code-row">
              <el-input v-model="findPwdForm.code" placeholder="XXXXXX" maxlength="6" class="swiss-input small"></el-input>
              <el-button :disabled="codeSending || countdown > 0" @click="sendVerifyCode" class="swiss-btn outline-btn">
                {{ countdown > 0 ? `${countdown}s` : 'SEND' }}
              </el-button>
            </div>
          </el-form-item>
          <el-form-item label="NEW PASSWORD" prop="newPassword">
            <el-input v-model="findPwdForm.newPassword" type="password" placeholder="new password" show-password class="swiss-input small"></el-input>
          </el-form-item>
        </el-form>
      </div>
      <template #footer>
        <div class="dialog-footer">
          <el-button class="swiss-btn red-btn full-width" :loading="findPwdLoading" @click="handleResetPassword">
            RESET ACCESS
          </el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script>
// ==========================================
// 逻辑部分完全保持不变 (JS Logic 100% Kept)
// ==========================================
const EMAIL_REGEX = /^[a-zA-Z0-9_-]+@[a-zA-Z0-9_-]+(\.[a-zA-Z0-9_-]+)+$/;

const VALIDATE_RULES = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 20, message: '长度在 3 到 20 个字符', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 20, message: '长度在 6 到 20 个字符', trigger: 'blur' }
  ],
  code: [
    { required: true, message: '请输入验证码', trigger: 'blur' },
    { min: 4, max: 6, message: '请输入 4-6 位验证码', trigger: 'blur' }
  ]
}

const validateMethods = {
  validatePass2: (rule, value, callback, source) => {
    if (value === '') {
      callback(new Error('请再次输入密码'));
    } else if (value !== source.password) {
      callback(new Error('两次输入密码不一致!'));
    } else {
      callback();
    }
  },
  validateEmail: (rule, value, callback) => {
    if (!value) {
      callback(new Error('请输入邮箱'));
    } else if (!EMAIL_REGEX.test(value)) {
      callback(new Error('请输入正确的邮箱格式'));
    } else {
      callback();
    }
  }
}

export default {
  name: 'Login',
  computed: {
    appTitle() {
      return import.meta.env.VITE_APP_TITLE || '管理系统'
    },
    loginBgImage() {
      return import.meta.env.VITE_LOGIN_BG_IMAGE || ''
    },
    containerStyle() {
      return this.loginBgImage
        ? { backgroundImage: `url(${this.loginBgImage})`, backgroundBlendMode: 'overlay', backgroundColor: '#eaeaea' }
        : { backgroundColor: '#eaeaea' }
    }
  },
  data() {
    const validateRegisterPass = (rule, value, callback) => {
      if (value === '') {
        callback(new Error('请输入密码'));
      } else {
        if (this.registerForm.confirmPassword !== '') {
          this.$refs.registerForm.validateField('confirmPassword');
        }
        callback();
      }
    };

    return {
      loading: false,
      loginForm: { username: '', password: '' },
      rules: {
        username: VALIDATE_RULES.username,
        password: VALIDATE_RULES.password
      },
      
      registerVisible: false,
      registerLoading: false,
      registerForm: {
        username: '',
        password: '',
        confirmPassword: '',
        email: '',
        flag: 0
      },
      registerRules: {
        username: [{ required: true, validator: validateMethods.validateEmail, trigger: 'blur' }],
        password: [{ required: true, validator: validateRegisterPass, trigger: 'blur' }, ...VALIDATE_RULES.password],
        confirmPassword: [{ required: true, validator: (r, v, c) => validateMethods.validatePass2(r, v, c, this.registerForm), trigger: 'blur' }],
        email: [{ required: true, validator: validateMethods.validateEmail, trigger: 'blur' }]
      },

      findPwdVisible: false,
      findPwdLoading: false,
      codeSending: false,
      countdown: 0,
      countdownTimer: null,
      findPwdForm: { email: '', code: '', newPassword: '' },
      findPwdRules: {
        email: [{ required: true, validator: validateMethods.validateEmail, trigger: 'blur' }],
        code: VALIDATE_RULES.code,
        newPassword: VALIDATE_RULES.password
      }
    }
  },
  methods: {
    resetForm(formName, formKey) {
      this.$nextTick(() => {
        this.$refs[formName]?.resetFields();
        if (formKey === 'findPwdForm') {
          this.findPwdForm.email = '';
          this.findPwdForm.code = '';
          this.findPwdForm.newPassword = '';
          this.countdown = 0;
          this.codeSending = false;
          this.clearTimer();
        }
      });
    },
    clearTimer() {
      if (this.countdownTimer) {
        clearInterval(this.countdownTimer);
        this.countdownTimer = null;
      }
    },
    handleLogin() {
      this.$refs.loginForm.validate(valid => {
        if (!valid) return;
        this.loading = true;
        this.$store.dispatch('login', this.loginForm)
          .then(() => this.$router.push('/analyse'))
          .catch(error => {
            const msg = error?.isBusinessError ? error.message : (error?.response?.data?.message || '网络异常，请稍后重试');
            this.$message[error?.isBusinessError ? 'warning' : 'error'](msg || '用户名或密码错误');
          })
          .finally(() => { this.loading = false; });
      });
    },
    showRegisterDialog() {
      this.registerVisible = true;
      this.resetForm('registerForm');
    },
    handleRegister() {
      this.$refs.registerForm.validate(valid => {
        if (!valid) return;
        this.registerLoading = true;
        this.$http.post('/v1/auth/register', this.registerForm)
          .then(() => {
            this.$message.success('注册成功，请登录');
            this.registerVisible = false;
            this.loginForm.username = this.registerForm.username;
          })
          .catch(err => this.$message.error(err.response?.data?.message || '注册失败'))
          .finally(() => { this.registerLoading = false; });
      });
    },
    showFindPwdDialog() {
      this.findPwdVisible = true;
      this.resetForm('findPwdForm', 'findPwdForm');
    },
    sendVerifyCode() {
      if (!this.findPwdForm.email) return this.$message.warning('请输入邮箱');
      if (!EMAIL_REGEX.test(this.findPwdForm.email)) return this.$message.error('请输入正确的邮箱格式');

      this.codeSending = true;
      this.$http.get('/v1/auth/sendCode', { params: { email: this.findPwdForm.email } })
        .then(() => {
          this.$message.success('验证码已发送，请查收邮箱');
          this.countdown = 60;
          this.clearTimer();
          this.countdownTimer = setInterval(() => {
            this.countdown--;
            if (this.countdown <= 0) this.clearTimer();
          }, 1000);
        })
        .catch(err => this.$message.error(err.response?.data?.message || '验证码发送失败'))
        .finally(() => { this.codeSending = false; });
    },
    handleResetPassword() {
      this.$refs.findPwdForm.validate(valid => {
        if (!valid) return;
        this.findPwdLoading = true;
        this.$http.post('/v1/auth/findPassword', this.findPwdForm)
          .then(() => {
            this.$message.success('密码重置成功，请使用新密码登录');
            this.findPwdVisible = false;
            this.loginForm.username = this.findPwdForm.email;
          })
          .catch(err => this.$message.error(err.response?.data?.message || '密码重置失败'))
          .finally(() => { this.findPwdLoading = false; });
      });
    }
  },
  beforeDestroy() {
    this.clearTimer();
  }
}
</script>

<style scoped>
/* =================================
   BAUHAUS / SWISS STYLE
   ================================= */
.login-container {
  height: 100vh;
  width: 100%;
  display: flex;
  justify-content: center;
  align-items: center;
  position: relative;
  overflow: hidden;
  font-family: 'Helvetica Neue', Helvetica, Arial, sans-serif;
  background-size: cover;
  background-position: center;
}

/* 装饰网格线 */
.grid-line {
  position: absolute;
  background: #000;
  z-index: 1;
}
.vertical { width: 3px; height: 100%; top: 0; }
.horizontal { height: 3px; width: 100%; left: 0; }
.v1 { left: 10%; }
.h1 { top: 15%; }

/* 主布局包装器：高度增加到 600px */
.bauhaus-wrapper {
  position: relative;
  z-index: 10;
  width: 900px;
  height: 600px;
  display: flex;
  border: 4px solid #000;
  box-shadow: 15px 15px 0px rgba(0,0,0,0.8);
  background: #fff;
}

/* 区块 1: 品牌 (红) - 高度调整为 110px */
.block-brand {
  position: absolute;
  top: 0;
  left: 0;
  width: calc(100% - 120px - 200px);
  height: 110px;
  background: #e63946;
  border-bottom: 4px solid #000;
  border-right: 4px solid #000;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0 40px;
  color: #fff;
}

.brand-container {
  display: flex;
  align-items: center;
  gap: 20px;
  width: 100%;
}

.logo-square {
  width: 60px;
  height: 60px;
  background: #fff;
  border: 3px solid #000;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 5px;
}
.actual-logo { width: 100%; height: 100%; object-fit: contain; }

.title-box { display: flex; flex-direction: column; }
.app-name {
  font-size: 32px;
  font-weight: 900;
  line-height: 1;
  margin: 0;
  letter-spacing: -1px;
  text-transform: uppercase;
}
.version-tag {
  font-family: monospace;
  font-size: 12px;
  margin-top: 5px;
  opacity: 0.8;
  letter-spacing: 1px;
}

/* 区块 2: 状态 (右上 - 黄) - 高度调整为 110px */
.block-status {
  position: absolute;
  top: 0;
  right: 120px;
  width: 200px;
  height: 110px;
  background: #f2c94c;
  border-bottom: 4px solid #000;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
}

.status-indicator {
  display: flex;
  align-items: center;
  gap: 10px;
  font-family: monospace;
  font-weight: 700;
  z-index: 2;
}
.dot { width: 12px; height: 12px; background: #000; border-radius: 50%; }
.blinking { animation: blink 1s infinite; }
@keyframes blink { 50% { opacity: 0; } }

.geo-circle {
  position: absolute;
  width: 100px;
  height: 100px;
  border: 4px solid #000;
  border-radius: 50%;
  right: -30px;
  bottom: -30px;
  opacity: 0.2;
}

/* 区块 3: 表单 (白) - 调整 top 为 110px */
.block-form {
  position: absolute;
  top: 110px;
  left: 0;
  right: 120px;
  bottom: 0;
  background: #fff;
  padding: 20px 60px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  overflow-y: auto; /* 防止极端情况下内容溢出 */
}

.bauhaus-card { border: none !important; box-shadow: none !important; background: transparent !important; }
.bauhaus-card :deep(.el-card__body) { padding: 0; }

.form-title-group { margin-bottom: 20px; }
.form-title-group h2 { font-size: 28px; font-weight: 900; margin: 0; letter-spacing: -1px; color: #000; }
.title-bar { width: 50px; height: 6px; background: #000; margin-top: 5px; }

/* 压缩输入框间距 */
.inputs-stack { margin-bottom: 20px; }
.compact-form-item { margin-bottom: 15px !important; } /* 强制覆盖 element 默认 */
.input-label { font-size: 12px; font-weight: 700; font-family: monospace; margin-bottom: 4px; color: #000; }

.swiss-input :deep(.el-input__inner) {
  border: 2px solid #000;
  border-radius: 0;
  height: 48px;
  font-weight: 700;
  font-size: 16px;
  color: #000;
  background: #f8f9fa;
  transition: all 0.2s;
}
.swiss-input :deep(.el-input__inner):focus {
  background: #fff;
  box-shadow: 4px 4px 0 #000;
  transform: translate(-2px, -2px);
}
.swiss-input :deep(.el-input__prefix) { color: #000; font-size: 18px; }

/* 按钮区 */
.action-stack {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.no-margin { margin-bottom: 0 !important; }

.swiss-btn {
  height: 50px;
  border-radius: 0;
  font-weight: 900;
  font-size: 16px;
  letter-spacing: 0.5px;
  border: 2px solid #000;
  width: 100%;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 25px;
  transition: all 0.2s;
  cursor: pointer;
}

.black-btn { background: #000; color: #fff; }
.black-btn:hover { background: #333; transform: translate(-2px, -2px); box-shadow: 4px 4px 0 rgba(0,0,0,0.2); }

/* 找回密码：显眼黄色块 */
.forgot-block { width: 100%; }
.yellow-btn {
  background: #f2c94c !important; /* 强制覆盖可能存在的默认背景 */
  color: #000 !important;
  justify-content: center;
  height: 44px; /* 略矮 */
  font-size: 14px;
}
.yellow-btn:hover {
  background: #e5b92e !important;
  transform: translate(-2px, -2px);
  box-shadow: 4px 4px 0 #000;
}

/* 区块 4: 注册 (右侧竖条 - 蓝) */
.block-register {
  position: absolute;
  top: 0;
  right: 0;
  bottom: 0;
  width: 120px;
  background: #2f80ed;
  color: #fff;
  border-left: 4px solid #000;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: background 0.3s;
}
.block-register:hover { background: #1a6cdb; }

.register-inner {
  display: flex;
  flex-direction: column;
  align-items: center;
  height: 100%;
  justify-content: center;
  gap: 20px;
}

.vertical-text {
  writing-mode: vertical-rl;
  text-orientation: mixed;
  transform: rotate(180deg);
  font-size: 20px;
  font-weight: 900;
  letter-spacing: 2px;
  white-space: nowrap;
}
.plus-icon { font-size: 40px; font-weight: 300; }

/* =================================
   弹窗样式
   ================================= */
:deep(.swiss-dialog) {
  border: 4px solid #000 !important;
  border-radius: 0 !important;
  box-shadow: 15px 15px 0px rgba(0,0,0,1) !important;
  background: #fff;
}
:deep(.swiss-dialog .el-dialog__header) { padding: 0; }
:deep(.swiss-dialog .el-dialog__headerbtn) { top: 15px; right: 15px; z-index: 10; }
:deep(.swiss-dialog .el-dialog__close) { color: #fff; font-size: 24px; font-weight: 900; }

.dialog-header { padding: 25px 30px; color: #fff; }
.dialog-header h3 { margin: 0; font-size: 24px; font-weight: 900; }
.blue-bg { background: #2f80ed; }
.red-bg { background: #e63946; }

.dialog-body { padding: 40px; }

/* 标签样式 */
:deep(.swiss-form-stacked .el-form-item__label) {
  font-family: monospace;
  font-weight: 700;
  color: #000;
  line-height: 1.5;
  padding-bottom: 5px;
}

.small :deep(.el-input__inner) { height: 42px; font-size: 15px; }

.dialog-footer { padding: 0 40px 40px; }

.full-width { width: 100%; justify-content: center; }
.blue-btn { background: #2f80ed; color: #fff; }
.blue-btn:hover { background: #1a6cdb; }
.red-btn { background: #e63946; color: #fff; }
.red-btn:hover { background: #d62828; }

.outline-btn { background: #fff; color: #000; border: 2px solid #000; }
.outline-btn:hover { background: #000; color: #fff; }

.swiss-code-row { display: flex; gap: 10px; }

/* =================================
   响应式调整
   ================================= */
@media (max-width: 1024px) {
  .bauhaus-wrapper {
    width: 90%;
    height: auto;
    display: flex;
    flex-direction: column;
    position: static;
  }
  
  .block-brand {
    position: relative;
    width: 100%;
    border-right: 4px solid #000;
  }
  
  .block-status { display: none; }
  
  .block-form {
    position: relative;
    top: auto; right: auto; left: auto; bottom: auto;
    width: 100%;
    border-bottom: 4px solid #000;
    border-right: 4px solid #000;
  }
  
  .block-register {
    position: relative;
    width: 100%;
    height: 80px;
    top: auto; right: auto; bottom: auto;
    border-left: 4px solid #000;
    border-right: 4px solid #000;
    border-bottom: 4px solid #000;
  }
  
  .register-inner { flex-direction: row; }
  .vertical-text { writing-mode: horizontal-tb; transform: none; }
}
</style>