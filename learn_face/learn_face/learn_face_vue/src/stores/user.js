import { defineStore } from 'pinia'

export const useUserStore = defineStore('user', {
  state: () => ({
    user: null,
    isLoggedIn: false,
    token: null
  }),
  
  actions: {
    login(userData) {
      this.user = {
        id: userData.id,
        username: userData.username,
        nickname: userData.nickname,
        avatar: userData.avatar,
        gender: userData.gender,
        birthday:userData.birthday,
        phone: userData.phone,
        email: userData.email,
        bio: userData.bio,
        address: userData.address,
        role: userData.role
      }
      this.token = userData.token
      this.isLoggedIn = true
      
      // 保存到localStorage
      localStorage.setItem('user', JSON.stringify(this.user))
      localStorage.setItem('token', userData.token)
    },
    
    logout() {
      this.user = null
      this.token = null
      this.isLoggedIn = false
      localStorage.removeItem('user')
      localStorage.removeItem('token')
    },
    
    checkAuth() {
      const userData = localStorage.getItem('user')
      const token = localStorage.getItem('token')
      if (userData && token) {
        this.user = JSON.parse(userData)
        this.token = token
        this.isLoggedIn = true
      }
    },
    
    // 获取请求头中的token
    getAuthHeaders() {
      return this.token ? {
        'Authorization': `${this.token}`,
        'Content-Type': 'application/json'
      } : {
        'Content-Type': 'application/json'
      }
    },
    
    // 更新用户信息
    updateUserInfo(userInfo) {
      this.user = { ...this.user, ...userInfo }
      // 如果需要持久化，也可以保存到localStorage
      localStorage.setItem('userInfo', JSON.stringify(this.user))
    },
    
    getAuthHeaders() {
      return this.token ? {
        'Authorization': `${this.token}`,
        'Content-Type': 'application/json'
      } : {
        'Content-Type': 'application/json'
      }
    }
  }
})