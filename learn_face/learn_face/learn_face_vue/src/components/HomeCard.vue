<template>
  <div class="immersive-card" @click="viewDetail(data.id)">
    <!-- 背景图片层 -->
    <div class="card-bg-wrapper">
      <img :src="data.image" :alt="data.title" class="card-bg-image" />
      <div class="card-gradient-overlay"></div>
    </div>

    <!-- 内容层 -->
    <div class="card-content">
      <!-- 顶部标签和热度 -->
      <div class="card-top-bar">
        <el-tag effect="dark" round size="small" :color="getCategoryColor(data.category)" class="category-tag">
          {{ data.category }}
        </el-tag>
        <div class="views-badge">
          <el-icon><View /></el-icon>
          <span>{{ formatNumber(data.views) }}</span>
        </div>
      </div>

      <!-- 底部信息区 -->
      <div class="card-info-box">
        <h3 class="card-title">{{ data.title }}</h3>
        
        <div class="card-footer">
          <span class="duration-badge" v-if="data.duration">
            <el-icon><Timer /></el-icon> {{ data.duration }}
          </span>
          <!-- 悬浮时显示的按钮 -->
          <button class="play-btn">
            <el-icon><VideoPlay /></el-icon>
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { View, VideoPlay, Timer } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import { ElMessage } from 'element-plus'

export default {
  name: 'HomeCard',
  components: { View, VideoPlay, Timer },
  props: {
    data: { type: Object, required: true },
    detailPath: { type: String, default: '/course/:id' }
  },
  methods: {
    viewDetail(id) {
      const userStore = useUserStore()
      const actualPath = this.detailPath.replace(':id', id)

      if (!userStore.isLoggedIn) {
        this.$emitter.emit('openLoginFromHome', actualPath)
        ElMessage.warning('请登录以解锁完整体验')
        return
      }
      this.$router.push(actualPath)
    },
    formatNumber(num) {
      return num > 1000 ? (num / 1000).toFixed(1) + 'k' : num
    },
    getCategoryColor(category) {
      const colors = { '初级': '#10b981', '中级': '#f59e0b', '高级': '#ef4444' }
      return colors[category] || '#3b82f6'
    }
  }
}
</script>

<style scoped>
.immersive-card {
  position: relative;
  height: 380px; /* 加高卡片，更有海报感 */
  border-radius: 24px;
  overflow: hidden;
  cursor: pointer;
  box-shadow: 0 10px 30px -10px rgba(0, 0, 0, 0.15);
  transition: all 0.4s cubic-bezier(0.34, 1.56, 0.64, 1);
  background: #000;
  isolation: isolate;
}

.immersive-card:hover {
  transform: translateY(-8px) scale(1.02);
  box-shadow: 0 20px 40px -10px rgba(0, 0, 0, 0.3);
  z-index: 2;
}

/* 图片背景 */
.card-bg-wrapper {
  position: absolute;
  inset: 0;
  z-index: 0;
}

.card-bg-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.6s ease;
}

.immersive-card:hover .card-bg-image {
  transform: scale(1.1);
}

/* 渐变遮罩：保证文字清晰 */
.card-gradient-overlay {
  position: absolute;
  inset: 0;
  background: linear-gradient(
    to bottom,
    rgba(0, 0, 0, 0.1) 0%,
    rgba(0, 0, 0, 0) 40%,
    rgba(0, 0, 0, 0.6) 70%,
    rgba(0, 0, 0, 0.9) 100%
  );
  transition: opacity 0.3s;
}

.immersive-card:hover .card-gradient-overlay {
  background: linear-gradient(
    to bottom,
    rgba(0, 0, 0, 0.1) 0%,
    rgba(0, 0, 0, 0) 30%,
    rgba(0, 0, 0, 0.8) 100%
  );
}

/* 内容层布局 */
.card-content {
  position: absolute;
  inset: 0;
  z-index: 1;
  padding: 20px;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
}

/* 顶部栏 */
.card-top-bar {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  opacity: 0.9;
}

.category-tag {
  border: none;
  font-weight: 600;
  backdrop-filter: blur(4px);
}

.views-badge {
  display: flex;
  align-items: center;
  gap: 4px;
  background: rgba(0, 0, 0, 0.4);
  backdrop-filter: blur(8px);
  padding: 4px 10px;
  border-radius: 20px;
  color: rgba(255, 255, 255, 0.9);
  font-size: 12px;
  font-weight: 600;
}

/* 底部信息 */
.card-info-box {
  transform: translateY(10px);
  transition: transform 0.3s ease;
}

.immersive-card:hover .card-info-box {
  transform: translateY(0);
}

.card-title {
  font-size: 1.25rem;
  font-weight: 700;
  color: white;
  margin: 0 0 12px 0;
  line-height: 1.4;
  text-shadow: 0 2px 4px rgba(0,0,0,0.5);
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.card-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  height: 40px; /* 固定高度以防抖动 */
}

.duration-badge {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: rgba(255, 255, 255, 0.7);
}

/* 播放按钮动画 */
.play-btn {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: white;
  color: black;
  border: none;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  opacity: 0;
  transform: scale(0.5);
  transition: all 0.3s cubic-bezier(0.175, 0.885, 0.32, 1.275);
  cursor: pointer;
}

.play-btn:hover {
  background: #3b82f6;
  color: white;
  box-shadow: 0 0 15px rgba(59, 130, 246, 0.6);
}

.immersive-card:hover .play-btn {
  opacity: 1;
  transform: scale(1);
}
</style>