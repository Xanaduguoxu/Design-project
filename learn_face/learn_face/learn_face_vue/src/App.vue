<template>
  <div id="app">
    <Header v-if="!isAuthPage" />
    <router-view />
    <Footer v-if="isFooterVisible" />
    <AICustomerService />
  </div>
</template>

<script>
import Header from './components/Header.vue'
import Footer from './components/Footer.vue'
import AICustomerService from './components/AICustomerService.vue'

export default {
  name: 'App',
  components: {
    Header,
    Footer,
    AICustomerService
  },
  computed: {
    isAuthPage() {
      return ['/login', '/register'].includes(this.$route.path)
    },
    isFooterVisible() {
      // 登录/注册页不显示 Footer，其它页默认显示。
      // 对于 /gym-home：由事件控制在最后一屏才显示。
      if (this.isAuthPage) return false
      if (this.$route.path === '/gym-home') return this.footerVisibleFlag
      return true
    }
  },
  data() {
    return {
      footerVisibleFlag: true
    }
  },
  mounted() {
    // 监听来自 GymHome 的 Footer 显隐事件
    this.$emitter.on('gymHome:footer:visible', (visible) => {
      this.footerVisibleFlag = !!visible
    })
  },
  beforeUnmount() {
    this.$emitter.off('gymHome:footer:visible')
  }
}
</script>

<style>
#app {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

main {
  flex: 1;
}
</style>