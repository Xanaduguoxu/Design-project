<template>
  <footer class="footer">
    <div class="container">
      <el-row :gutter="20">
        <el-col :xs="24" :sm="8" :md="6">
          <div class="footer-section">
            <h4 class="footer-title">{{ appTitle }}</h4>
            <p class="footer-desc">为您提供优质的体验</p>
          </div>
        </el-col>
        <el-col :xs="24" :sm="8" :md="6">
          <div class="footer-section">
            <h4 class="footer-title">快速链接</h4>
            <ul class="footer-links">
              <li v-for="item in navMenu" :key="item.index">
                <a href="#" @click.prevent="handleFooterLinkClick(item)">{{ item.label }}</a>
              </li>
            </ul>
          </div>
        </el-col>
        <el-col :xs="24" :sm="8" :md="6">
          <div class="footer-section">
            <h4 class="footer-title">关注我们</h4>
            <div class="social-links">
              <a href="#" class="social-link">微信</a>
              <a href="#" class="social-link">微博</a>
              <a href="#" class="social-link">抖音</a>
            </div>
          </div>
        </el-col>
      </el-row>
      <el-divider class="footer-divider"></el-divider>
      <el-row>
        <el-col :span="24" class="text-center">
          <p class="copyright">&copy; {{ appYear }} {{ appTitle }}. All rights reserved.</p>
        </el-col>
      </el-row>
    </div>
  </footer>
</template>

<script>
import { useUserStore } from '../stores/user'
import { ElMessage } from 'element-plus'

export default {
  name: 'Footer',
  setup() {
    const userStore = useUserStore()
    return {
      userStore
    }
  },
  computed: {
    appTitle() {
      return import.meta.env.VITE_APP_TITLE || '平台'
    },
    appYear() {
      return import.meta.env.VITE_APP_YEAR || '2025'
    },
    navMenu() {
      try {
        const menuData = import.meta.env.VITE_NAV_MENU
        if (menuData) {
          return JSON.parse(menuData)
        }
      } catch (error) {
        console.warn('解析导航菜单配置失败:', error)
      }
      return []
    }
  },
  methods: {
    // 处理底部导航链接点击事件
    handleFooterLinkClick(menuItem) {
      // 首页不需要登录检测，直接跳转
      if (menuItem.index === '/' || menuItem.index === '/home') {
        this.$router.push(menuItem.index)
        return
      }

      // 其他页面需要检查登录状态
      if (!this.userStore.isLoggedIn) {
        // 未登录，触发Header中的登录弹窗
        this.$emitter.emit('openLoginFromFooter', menuItem.index)
        ElMessage({
          message: '请先登录后再访问该页面',
          type: 'warning',
          duration: 2000
        })
      } else {
        // 已登录，直接跳转
        this.$router.push(menuItem.index)
      }
    }
  }
}
</script>

<style scoped>
.footer {
  background: #000000;
  color: #ffffff;
  padding: 40px 0 20px;
  margin-top: auto;
  position: relative;
  border-top: 1px solid #333333;
}

.container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 20px;
}

.footer-section {
  margin-bottom: 30px;
}

.footer-title {
  color: #ffffff;
  font-size: 18px;
  font-weight: 600;
  margin-bottom: 15px;
  position: relative;
  padding-bottom: 8px;
}

.footer-title::after {
  content: '';
  position: absolute;
  bottom: 0;
  left: 0;
  width: 30px;
  height: 2px;
  background: #ffffff;
  border-radius: 1px;
}

.footer-desc {
  color: #ffffff;
  font-size: 14px;
  line-height: 1.6;
  margin: 0;
  opacity: 0.8;
}

.footer-links {
  list-style: none;
  padding: 0;
  margin: 0;
}

.footer-links li {
  margin-bottom: 8px;
}

.footer-links a {
  color: #ffffff;
  text-decoration: none;
  font-size: 14px;
  transition: all 0.3s ease;
  position: relative;
  opacity: 0.8;
  cursor: pointer;
}

.footer-links a:hover {
  color: #ffffff;
  opacity: 1;
  padding-left: 5px;
}

.social-links {
  display: flex;
  gap: 15px;
  flex-wrap: wrap;
}

.social-link {
  display: inline-block;
  padding: 8px 16px;
  background: rgba(255, 255, 255, 0.1);
  color: #ffffff;
  text-decoration: none;
  border-radius: 20px;
  font-size: 12px;
  transition: all 0.3s ease;
  border: 1px solid rgba(255, 255, 255, 0.2);
}

.social-link:hover {
  background: rgba(255, 255, 255, 0.2);
  color: #ffffff;
  transform: translateY(-2px);
  border-color: rgba(255, 255, 255, 0.4);
}

.footer-divider {
  border-color: rgba(255, 255, 255, 0.2);
  margin: 20px 0;
}

.copyright {
  color: #ffffff;
  font-size: 13px;
  margin: 0;
  padding: 10px 0;
  opacity: 0.7;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .footer {
    padding: 30px 0 15px;
  }
  
  .footer-section {
    margin-bottom: 25px;
    text-align: center;
  }
  
  .footer-title::after {
    left: 50%;
    transform: translateX(-50%);
  }
  
  .social-links {
    justify-content: center;
  }
}

@media (max-width: 480px) {
  .container {
    padding: 0 15px;
  }
  
  .footer-title {
    font-size: 16px;
  }
  
  .social-link {
    padding: 6px 12px;
    font-size: 11px;
  }
}
</style>