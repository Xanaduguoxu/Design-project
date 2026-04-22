<template>
  <main class="brutalist-home">
    <section class="hero-grid-section">
      <el-carousel 
        trigger="click" 
        height="100vh" 
        :interval="8000" 
        arrow="never"
        class="hero-fullscreen-carousel"
        indicator-position="none"
      >
        <el-carousel-item v-for="banner in banners" :key="banner.id">
          <div class="hero-frame">
            <div class="hero-img-container">
              <div class="hero-bg-img" :style="{ backgroundImage: `url(${banner.image})` }"></div>
            </div>
            <div class="hero-content-layer">
              <div class="container hero-grid">
                <div class="hero-meta">
                  <span class="meta-tag">EDITOR'S PICK</span>
                </div>
                <div class="hero-main">
                  <h1 class="hero-title-brutal">{{ banner.title }}</h1>
                  <div class="horizontal-line"></div>
                  <p class="hero-desc-brutal">{{ banner.description }}</p>
                </div>
              </div>
            </div>
          </div>
        </el-carousel-item>
      </el-carousel>
    </section>

    <div class="nav-bar-fixed">
      <div class="nav-inner container">
        <div class="nav-groups">
          <div 
            v-for="category in categories" 
            :key="category.id"
            class="nav-item-block"
            @click="filterByCategory(category)"
          >
            <div class="nav-icon-box">
              <img :src="category.image" :alt="category.name">
            </div>
            <span class="nav-label-brutal">{{ category.name }}</span>
          </div>
        </div>
      </div>
    </div>

    <section class="content-layout container">
      <el-row :gutter="0" class="main-row">
        <el-col :md="18" class="feed-column">
          <div class="section-header-row">
            <div class="header-left">
              <h2 class="section-title-box">发现探索</h2>
              <span class="section-sub-box">DISCOVERY</span>
            </div>
            <el-button link class="view-all-brutal" @click="viewAllItems">
              VIEW ALL &rarr;
            </el-button>
          </div>
          
          <div class="feed-grid-layout">
            <div 
              class="feed-card-wrapper" 
              v-for="(item, index) in recommendedItems" 
              :key="item.id"
              :style="{ animationDelay: `${index * 0.05}s` }"
            >
              <HomeCard :data="item" detail-path="/course/:id" />
            </div>
          </div>
          
          <div class="load-action-area">
            <button class="btn-brutal-outline" @click="viewAllItems">
              <span>加载更多</span>
            </button>
          </div>
        </el-col>

        <el-col :md="6" class="sidebar-column">
          <div class="sidebar-sticky-area">
            <div class="trending-widget">
              <div class="widget-title-row">
                <h3>TRENDING</h3>
                <div class="status-badge">LIVE</div>
              </div>
              
              <div class="trending-feed">
                <div 
                  v-for="(topic, index) in hotTopics" 
                  :key="topic.id" 
                  class="trending-row-item"
                  @click="viewTopic(topic)"
                >
                  <div class="rank-badge" :class="{'top-rank': index < 3}">
                    {{ String(index + 1).padStart(2, '0') }}
                  </div>
                  <div class="trending-content">
                    <h4>{{ topic.title }}</h4>
                    <span class="trending-meta">Hot Topic</span>
                  </div>
                </div>
              </div>
            </div>

            <div class="creator-card">
              <div class="creator-header">
                <h3>Creator Program</h3>
                <div class="icon-box-arrow">
                  <el-icon><ArrowRight /></el-icon>
                </div>
              </div>
              <p>分享知识，改变世界</p>
              <div class="gradient-bar"></div>
            </div>
          </div>
        </el-col>
      </el-row>
    </section>
  </main>
</template>

<script>
import { useUserStore } from '@/stores/user'
import { ElMessage } from 'element-plus'
import { ArrowRight } from '@element-plus/icons-vue'
import HomeCard from '@/components/HomeCard.vue'
import { 
  getBannersAPI, 
  getDictionaryAPI, 
  getRecommendedCoursesAPI,
  getTopForumTopicsAPI
} from '@/utils/api.js'

export default {
  name: 'Home',
  components: { ArrowRight, HomeCard },
  data() {
    return {
      banners: [],
      categories: [],
      recommendedItems: [],
      hotTopics: []
    }
  },
  mounted() {
    this.fetchBanners()
    this.fetchCategories()
    this.fetchRecommendedCourses()
    this.fetchHotTopics()
  },
  methods: {
    async fetchRecommendedCourses() {
      try {
        const coursesData = await getRecommendedCoursesAPI()
        this.recommendedItems = coursesData.map(item => ({
          id: item.id,
          title: item.name,
          category: this.mapLevelCodeToName(item.category),
          image: item.image,
          duration: item.duration,
          views: item.views || 0
        }))
      } catch (error) { console.error(error); this.recommendedItems = [] }
    },
    mapLevelCodeToName(code) {
      const map = { 'beginner': '初级', 'intermediate': '中级', 'advanced': '高级' }
      return map[code] || code
    },
    async fetchCategories() {
      try {
        const data = await getDictionaryAPI('course_level')
        this.categories = data.map((item, index) => ({
          id: index + 1, name: item.value, code: item.code, image: item.image
        }))
      } catch (error) { console.error(error) }
    },
    async fetchBanners() {
      try {
        const data = await getBannersAPI()
        this.banners = data.map((item, index) => ({
          id: index + 1, title: item.name, description: item.brief, image: item.image
        }))
      } catch (error) { console.error(error) }
    },
    filterByCategory(category) {
      const userStore = useUserStore()
      if (!userStore.isLoggedIn) {
        this.$emitter.emit('openLoginFromHome', `/learning?category=${category.code}`)
        ElMessage.warning('请先登录')
        return
      }
      this.$router.push({ path: '/learning', query: { category: category.code } })
    },
    viewAllItems() {
      const userStore = useUserStore()
      if (!userStore.isLoggedIn) {
        this.$emitter.emit('openLoginFromHome', '/learning')
        ElMessage.warning('请先登录')
        return
      }
      this.$router.push('/learning')
    },
    viewTopic(topic) {
      const userStore = useUserStore()
      if (!userStore.isLoggedIn) {
        this.$emitter.emit('openLoginFromHome', `/post/${topic.id}`)
        ElMessage.warning('请先登录')
        return
      }
      this.$router.push(`/post/${topic.id}`)
    },
    async fetchHotTopics() {
      try {
        const data = await getTopForumTopicsAPI()
        this.hotTopics = data.map(item => ({
          id: item.id, title: item.title, description: item.bio || ''
        }))
      } catch (error) { console.error(error) }
    }
  }
}
</script>

<style scoped>
.brutalist-home {
  background-color: #F0F0F0;
  color: #000;
  font-family: 'Courier New', Courier, monospace;
  min-height: 100vh;
}

.hero-grid-section {
  height: 100vh;
  position: relative;
  border-bottom: 4px solid #000;
}

.hero-fullscreen-carousel {
  height: 100%;
}

.hero-frame {
  position: relative;
  height: 100%;
  width: 100%;
  display: grid;
  grid-template-columns: 1fr 1fr;
}

.hero-img-container {
  height: 100%;
  overflow: hidden;
}

.hero-bg-img {
  width: 100%;
  height: 100%;
  background-size: cover;
  background-position: center;
  filter: grayscale(100%) contrast(1.2);
  transition: transform 6s ease;
}

.hero-frame:hover .hero-bg-img {
  transform: scale(1.1);
}

.hero-content-layer {
  position: absolute;
  inset: 0;
  background: rgba(255, 255, 255, 0.92);
  display: flex;
  align-items: center;
  justify-content: flex-end;
  padding: 60px;
  z-index: 2;
}

.hero-grid {
  width: 100%;
  max-width: 600px;
}

.hero-meta {
  margin-bottom: 40px;
}

.meta-tag {
  border: 2px solid #000;
  padding: 8px 16px;
  font-weight: bold;
  font-size: 14px;
  text-transform: uppercase;
}

.hero-title-brutal {
  font-size: 5rem;
  font-weight: 900;
  line-height: 0.9;
  margin: 0 0 30px 0;
  text-transform: uppercase;
  letter-spacing: -2px;
}

.horizontal-line {
  width: 100px;
  height: 8px;
  background: #000;
  margin-bottom: 30px;
}

.hero-desc-brutal {
  font-size: 1rem;
  line-height: 1.5;
  max-width: 400px;
  color: #333;
}

.nav-bar-fixed {
  position: sticky;
  top: 0;
  z-index: 100;
  background: #fff;
  border-bottom: 2px solid #000;
  padding: 15px 0;
}

.nav-inner {
  display: flex;
  justify-content: center;
}

.nav-groups {
  display: flex;
  gap: 20px;
}

.nav-item-block {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 24px;
  border: 2px solid transparent;
  background: #eee;
  cursor: pointer;
  transition: all 0.1s ease;
}

.nav-item-block:hover {
  border-color: #000;
  background: #fff;
  transform: translate(-2px, -2px);
  box-shadow: 4px 4px 0 #000;
}

.nav-icon-box {
  width: 24px;
  height: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.nav-icon-box img {
  max-width: 100%;
  max-height: 100%;
  filter: grayscale(1);
}

.nav-label-brutal {
  font-weight: 800;
  text-transform: uppercase;
  font-size: 12px;
}

.content-layout {
  padding: 80px 0;
}

.section-header-row {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  margin-bottom: 40px;
  border-bottom: 2px solid #000;
  padding-bottom: 20px;
}

.section-title-box {
  font-size: 2.5rem;
  font-weight: 900;
  margin: 0;
  text-transform: uppercase;
}

.section-sub-box {
  font-size: 1rem;
  color: #666;
  display: block;
  margin-top: 5px;
}

.view-all-brutal {
  font-size: 1rem;
  font-weight: 800;
  color: #000;
  text-transform: uppercase;
}

.feed-grid-layout {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 30px;
  margin-bottom: 60px;
}

.feed-card-wrapper {
  opacity: 0;
  transform: translateY(20px);
  animation: slideUpFast 0.4s cubic-bezier(0.25, 0.46, 0.45, 0.94) forwards;
}

@keyframes slideUpFast {
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.load-action-area {
  text-align: center;
  margin-top: 40px;
}

.btn-brutal-outline {
  background: transparent;
  border: 2px solid #000;
  padding: 15px 50px;
  font-weight: 800;
  text-transform: uppercase;
  cursor: pointer;
  transition: all 0.2s ease;
  font-family: inherit;
}

.btn-brutal-outline:hover {
  background: #000;
  color: #fff;
}

.sidebar-sticky-area {
  position: sticky;
  top: 100px;
  padding-left: 40px;
}

.trending-widget {
  background: #fff;
  border: 2px solid #000;
  padding: 25px;
  margin-bottom: 30px;
}

.widget-title-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 25px;
}

.widget-title-row h3 {
  font-size: 1.5rem;
  margin: 0;
  text-transform: uppercase;
}

.status-badge {
  background: #000;
  color: #fff;
  padding: 4px 10px;
  font-size: 12px;
  font-weight: bold;
}

.trending-feed {
  display: flex;
  flex-direction: column;
  gap: 15px;
}

.trending-row-item {
  display: flex;
  align-items: center;
  gap: 15px;
  padding: 15px 0;
  border-bottom: 1px solid #eee;
  cursor: pointer;
}

.trending-row-item:last-child {
  border-bottom: none;
}

.rank-badge {
  font-size: 1.5rem;
  font-weight: 900;
  color: #ccc;
  min-width: 40px;
}

.rank-badge.top-rank {
  color: #000;
}

.trending-content h4 {
  font-size: 1rem;
  margin: 0 0 5px 0;
  font-weight: 800;
}

.trending-meta {
  font-size: 12px;
  color: #666;
  text-transform: uppercase;
}

.creator-card {
  background: #000;
  color: #fff;
  padding: 30px;
  position: relative;
  overflow: hidden;
}

.creator-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 20px;
}

.creator-header h3 {
  font-size: 1.5rem;
  margin: 0;
  text-transform: uppercase;
}

.icon-box-arrow {
  border: 1px solid #fff;
  padding: 5px;
}

.creator-card p {
  margin: 0 0 20px 0;
  opacity: 0.7;
}

.gradient-bar {
  height: 4px;
  width: 100%;
  background: repeating-linear-gradient(
    45deg,
    #fff,
    #fff 10px,
    #333 10px,
    #333 20px
  );
}

@media (max-width: 768px) {
  .hero-frame {
    grid-template-columns: 1fr;
  }
  
  .hero-img-container {
    position: absolute;
    inset: 0;
    z-index: 1;
  }
  
  .hero-content-layer {
    justify-content: flex-start;
    background: rgba(255, 255, 255, 0.95);
  }

  .hero-title-brutal {
    font-size: 3rem;
  }

  .feed-grid-layout {
    grid-template-columns: 1fr;
  }

  .sidebar-sticky-area {
    padding-left: 0;
    margin-top: 60px;
  }
}
</style>