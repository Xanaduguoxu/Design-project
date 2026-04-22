<template>
  <div class="tech-card-light" @click="handleClick">
    <div class="card-cover-wrapper">
      <img 
        :src="course.cover || 'https://images.unsplash.com/photo-1555066931-4365d14bab8c?q=80&w=2070&auto=format&fit=crop'" 
        class="course-img" 
        alt="course cover"
        loading="lazy"
      />
      
      <div class="hover-overlay">
        <div class="play-btn-circle">
          <el-icon :size="28"><VideoPlay /></el-icon>
        </div>
      </div>
      
      <span class="category-badge">{{ course.category }}</span>
      <span class="duration-badge">{{ course.duration }}</span>
    </div>

    <div class="card-content">
      <h3 class="course-title" :title="course.name">
        {{ course.name }}
      </h3>
      
      <div class="course-meta">
        <div class="teacher-info">
          <span class="label">Dev:</span>
          <span class="value">{{ course.teacher || 'Unknown' }}</span>
        </div>
        <div class="views-info">
          <el-icon><View /></el-icon>
          <span>{{ course.views }}</span>
        </div>
      </div>
    </div>
    
    <!-- 底部装饰条 -->
    <div class="tech-bar"></div>
  </div>
</template>

<script>
import { VideoPlay, View } from '@element-plus/icons-vue'

export default {
  name: 'ListCard1',
  components: {
    VideoPlay, View
  },
  props: {
    course: {
      type: Object,
      required: true
    }
  },
  emits: ['click'],
  methods: {
    handleClick() {
      this.$emit('click', this.course)
    }
  }
}
</script>

<style scoped>
/* 局部变量 - 浅色科技风 */
.tech-card-light {
  --card-bg: #ffffff;
  --card-border: #e2e8f0;
  --primary-blue: #3b82f6;
  --text-main: #1e293b;
  --text-sub: #64748b;
  
  background: var(--card-bg);
  border: 1px solid var(--card-border);
  border-radius: 8px;
  overflow: hidden;
  cursor: pointer;
  transition: all 0.3s ease;
  height: 100%;
  display: flex;
  flex-direction: column;
  position: relative;
}

.tech-card-light:hover {
  transform: translateY(-4px);
  box-shadow: 0 12px 24px -8px rgba(59, 130, 246, 0.15);
  border-color: rgba(59, 130, 246, 0.4);
}

.card-cover-wrapper {
  position: relative;
  height: 160px;
  overflow: hidden;
  background-color: #f1f5f9;
}

.course-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.5s ease;
}

.tech-card-light:hover .course-img {
  transform: scale(1.08);
}

/* 遮罩交互 */
.hover-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(15, 23, 42, 0.3);
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0;
  transition: opacity 0.3s ease;
  backdrop-filter: blur(2px);
}

.tech-card-light:hover .hover-overlay {
  opacity: 1;
}

.play-btn-circle {
  width: 50px;
  height: 50px;
  background: white;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--primary-blue);
  transform: scale(0.8);
  transition: transform 0.3s cubic-bezier(0.34, 1.56, 0.64, 1);
  box-shadow: 0 4px 12px rgba(0,0,0,0.2);
}

.tech-card-light:hover .play-btn-circle {
  transform: scale(1);
}

/* 徽标样式 */
.category-badge {
  position: absolute;
  top: 10px;
  left: 10px;
  background: rgba(255, 255, 255, 0.95);
  color: var(--primary-blue);
  font-size: 0.7rem;
  font-weight: 700;
  padding: 4px 8px;
  border-radius: 4px;
  font-family: 'Fira Code', monospace;
  box-shadow: 0 2px 4px rgba(0,0,0,0.1);
  z-index: 2;
}

.duration-badge {
  position: absolute;
  bottom: 8px;
  right: 8px;
  background: rgba(15, 23, 42, 0.8);
  color: #fff;
  font-size: 0.7rem;
  padding: 2px 6px;
  border-radius: 2px;
  font-family: 'Fira Code', monospace;
  z-index: 2;
}

.card-content {
  padding: 16px;
  flex: 1;
  display: flex;
  flex-direction: column;
}

.course-title {
  font-size: 1rem;
  font-weight: 600;
  color: var(--text-main);
  margin: 0 0 12px 0;
  line-height: 1.5;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  height: 3rem; /* 固定高度防止抖动 */
}

.course-meta {
  margin-top: auto;
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 0.8rem;
  color: var(--text-sub);
}

.teacher-info {
  display: flex;
  align-items: center;
  gap: 4px;
}

.teacher-info .label {
  font-family: 'Fira Code', monospace;
  font-size: 0.7rem;
  opacity: 0.7;
}

.views-info {
  display: flex;
  align-items: center;
  gap: 4px;
}

/* 底部装饰条 */
.tech-bar {
  height: 3px;
  background: linear-gradient(90deg, var(--primary-blue), transparent);
  width: 0;
  transition: width 0.3s ease;
}

.tech-card-light:hover .tech-bar {
  width: 100%;
}
</style>