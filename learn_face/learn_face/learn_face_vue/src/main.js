import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'
import App from './App.vue'
import router from './router'
import './style.css'
import zhCn from 'element-plus/dist/locale/zh-cn.mjs'
import mitt from 'mitt'
import { useUserStore } from './stores/user.js'

const app = createApp(App)
const emitter = mitt()
app.config.globalProperties.$emitter = emitter

// 注册所有图标
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component)
}

const pinia = createPinia()
app.use(pinia)

// 在Pinia创建后立即检查登录状态
const userStore = useUserStore()
userStore.checkAuth()

app.use(router)
app.use(ElementPlus, {
  locale: zhCn,
})

app.mount('#app')