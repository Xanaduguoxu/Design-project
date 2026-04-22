import Vue from 'vue'
import App from './App.vue'
import router from './router'
import store from './store'
import ElementUI from 'element-ui'
import 'element-ui/lib/theme-chalk/index.css'
import axios from 'axios'
import './styles/variables.css'


Vue.use(ElementUI)
Vue.config.productionTip = false

// 配置axios
axios.defaults.baseURL = ''  // 使用相对路径，让代理生效

// 请求拦截器
axios.interceptors.request.use(
  config => {
    const token = localStorage.getItem('token')
    if (token && !config.url.includes('/auth/login')) {
      config.headers['Authorization'] = token
    }
    config.headers['Content-Type'] = 'application/json'
    return config
  },
  error => {
    return Promise.reject(error)
  }
)

// 响应拦截器
axios.interceptors.response.use(
  response => {
    // 检查多种可能的未登录状态
    if (
      // 检查特定的未登录响应格式
      (response.data &&
        response.data.code === 200 &&
        response.data.message === "fail!" &&
        response.data.data === "未登陆,请先登陆!")
    ) {
      console.log('检测到未登录状态，准备跳转到登录页面')
      // 清除用户信息
      store.dispatch('logout')
      // 确保在下一个事件循环中执行路由跳转，避免可能的冲突
      setTimeout(() => {
        router.push('/login')
      }, 0)
    }
    return response
  },
  error => {
    // 处理未授权错误
    if (error.response && error.response.data.code === 200 && error.response.data.data === "未登陆,请先登陆!") {
      console.log('检测到未授权错误，准备跳转到登录页面')
      // 清除用户信息
      store.dispatch('logout')
      // 确保在下一个事件循环中执行路由跳转，避免可能的冲突
      setTimeout(() => {
        router.push('/login')
        ElementUI.Message.error('账号失效请重新登陆!')
      }, 0)
    }
    return Promise.reject(error)
  }
)

Vue.prototype.$http = axios

new Vue({
  router,
  store,
  render: h => h(App)
}).$mount('#app')