import Vue from 'vue'
import Vuex from 'vuex'

Vue.use(Vuex)

export default new Vuex.Store({
  state: {
    token: localStorage.getItem('token') || '',
    role: localStorage.getItem('role') || '',
    userInfo: JSON.parse(localStorage.getItem('userInfo')) || null
  },
  mutations: {
    SET_TOKEN(state, token) {
      state.token = token
      localStorage.setItem('token', token)
    },
    SET_ROLE(state, role) {
      state.role = role
      localStorage.setItem('role', role)
    },
    SET_USER_INFO(state, userInfo) {
      state.userInfo = userInfo
      localStorage.setItem('userInfo', JSON.stringify(userInfo))
    },
    CLEAR_USER_INFO(state) {
      state.token = ''
      state.role = ''
      state.userInfo = null
      localStorage.removeItem('token')
      localStorage.removeItem('role')
      localStorage.removeItem('userInfo')
    }
  },
  actions: {
    login({ commit }, userInfo) {
      return new Promise((resolve, reject) => {
        Vue.prototype.$http.post('/v1/auth/login', userInfo)
          .then(response => {
            // 后端有时会返回 200 + message 的业务失败，这里更稳妥地判断
            const res = response.data || {}
            const data = res.data || null

            // 成功：data 中存在 token 等必要信息
            if (data && data.token) {
              commit('SET_USER_INFO', {
                id: data.id,
                username: data.username,
                nickname: data.nickname,
                gender: data.gender,
                phone: data.phone,
                address: data.address,
                email: data.email,
                avatar: data.avatar,
                role: data.role
              })
              commit('SET_TOKEN', data.token)
              commit('SET_ROLE', data.role)
              resolve(data)
              return
            }

            // 业务失败：没有 data/token，透传后端 message 给上层做友好提示
            const message = res.message || '用户名或密码错误'
            reject({ isBusinessError: true, message })
          })
          .catch(error => {
            // 网络或系统异常，交由上层统一处理
            reject(error)
          })
      })
    },
    logout({ commit }) {
      commit('CLEAR_USER_INFO')
    }
  }
})