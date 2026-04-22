import { Message, MessageBox } from 'element-ui'

/**
 * 密码管理工具包
 * 提供密码修改相关的验证、请求和结果处理功能
 */
export const PasswordManager = {
  /**
   * 验证密码表单
   * @param {Object} passwordForm - 密码表单数据
   * @returns {boolean} 验证是否通过
   */
  validateForm(passwordForm) {
    if (!passwordForm.oldPassword) {
      Message.error('请输入当前密码')
      return false
    }

    if (!passwordForm.newPassword) {
      Message.error('请输入新密码')
      return false
    }

    if (passwordForm.newPassword !== passwordForm.confirmPassword) {
      Message.error('两次输入的新密码不一致')
      return false
    }

    // 密码强度验证
    if (!this.checkPasswordStrength(passwordForm.newPassword)) {
      Message.error('密码必须包含字母和数字，且长度至少6位')
      return false
    }

    return true
  },

  /**
   * 验证密码强度
   * @param {string} password - 密码
   * @returns {boolean} 是否满足强度要求
   */
  checkPasswordStrength(password) {
    const passwordRegex = /^(?=.*[A-Za-z])(?=.*\d)[A-Za-z\d]{6,}$/
    return passwordRegex.test(password)
  },

  /**
   * 执行密码修改请求
   * @param {Object} passwordData - 密码数据
   * @param {Object} http - HTTP实例
   * @returns {Promise} Promise对象
   */
  async executeUpdate(passwordData, http) {
    try {
      const response = await http.post('/v1/auth/password', passwordData, {
        headers: {
          'Content-Type': 'application/json',
          'Authorization': localStorage.getItem('token')
        }
      })

      if (response.data.code === 200 && response.data.message === 'success!') {
        return Promise.resolve(response.data)
      } else {
        return Promise.reject(new Error(response.data.message || '密码修改失败'))
      }
    } catch (error) {
      const errorMsg = error.response?.data?.message || '密码修改失败'
      return Promise.reject(new Error(errorMsg))
    }
  },

  /**
   * 处理修改密码成功
   * @param {Function} logout - 登出函数
   * @param {Function} routerPush - 路由跳转函数
   */
  handleSuccess(logout, routerPush) {
    return MessageBox.confirm('密码修改成功，请重新登录', '提示', {
      confirmButtonText: '确定',
      showCancelButton: false,
      type: 'success'
    }).then(() => {
      logout()
      routerPush('/login')
    })
  },

  /**
   * 处理修改密码错误
   * @param {Error} error - 错误对象
   */
  handleError(error) {
    const errorMsg = error.message
    if (errorMsg.includes('密码') || errorMsg.includes('不正确')) {
      Message.error('原密码不正确')
    } else {
      Message.error(errorMsg)
    }
  },

  /**
   * 获取初始密码表单
   * @returns {Object} 重置后的密码表单
   */
  getInitialForm() {
    return {
      oldPassword: '',
      newPassword: '',
      confirmPassword: ''
    }
  }
}

export default PasswordManager