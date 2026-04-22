<template>
  <div>
    <!-- 网格视图 -->
    <div v-if="viewMode === 'grid'" class="grid-view">
      <el-row :gutter="20">
        <el-col :xs="24" :sm="12" :md="8" :lg="6" v-for="work in works" :key="work.id">
          <div class="work-card" @click="handleCardClick(work)">
            <div class="work-image-wrapper">
              <img :src="work.image" :alt="work.title" class="work-image" loading="lazy" />
              <div class="work-overlay">
                <el-button circle class="quick-view-btn" @click.stop="handleQuickView(work)">
                  <el-icon>
                    <View />
                  </el-icon>
                </el-button>
              </div>
              <div class="work-tags">
                <el-tag size="small">{{ work.category }}</el-tag>
              </div>
            </div>
            <div class="work-info">
              <h3 class="work-title">{{ work.title }}</h3>
              <p class="work-author">作者：{{ work.author }}</p>
              <div class="work-stats">
                <span class="stat-item">
                  <el-icon>
                    <View />
                  </el-icon>
                  {{ work.views }}
                </span>
                <span class="stat-item">
                  <el-icon>
                    <Star />
                  </el-icon>
                  {{ work.likes }}
                </span>
                <span class="stat-item">
                  <el-icon>
                    <ChatDotRound />
                  </el-icon>
                  {{ work.comments }}
                </span>
              </div>
            </div>
          </div>
        </el-col>
      </el-row>
    </div>

    <!-- 列表视图 -->
    <div v-else class="list-view">
      <div class="work-list-item" v-for="work in works" :key="work.id" @click="handleCardClick(work)">
        <div class="item-image">
          <img :src="work.image" :alt="work.title" loading="lazy" />
        </div>
        <div class="item-content">
          <div class="item-header">
            <h3 class="item-title">{{ work.title }}</h3>
            <el-tag size="small">{{ work.category }}</el-tag>
          </div>
          <p class="item-desc">{{ work.description }}</p>
          <div class="item-meta">
            <span class="meta-author">作者：{{ work.author }}</span>
            <span class="meta-time">发布时间：{{ work.publishTime }}</span>
          </div>
        </div>
        <div class="item-stats">
          <div class="stat-box">
            <el-icon>
              <View />
            </el-icon>
            <span>{{ work.views }}</span>
          </div>
          <div class="stat-box">
            <el-icon>
              <Star />
            </el-icon>
            <span>{{ work.likes }}</span>
          </div>
          <div class="stat-box">
            <el-icon>
              <ChatDotRound />
            </el-icon>
            <span>{{ work.comments }}</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import {
  View,
  Star,
  ChatDotRound
} from '@element-plus/icons-vue'

export default {
  name: 'ListCard',
  components: {
    View,
    Star,
    ChatDotRound
  },
  props: {
    works: {
      type: Array,
      required: true
    },
    viewMode: {
      type: String,
      default: 'grid',
      validator: (value) => ['grid', 'list'].includes(value)
    }
  },
  methods: {
    handleCardClick(work) {
      // 通过事件将整个作品对象传递给父组件
      this.$emit('card-click', work)
    },
    handleQuickView(work) {
      // 快速查看按钮点击事件，也传递整个作品对象
      this.$emit('quick-view', work)
    }
  }
}
</script>

<style scoped>
/* 样式代码保持不变 */
.grid-view {
  margin-bottom: 40px;
}

.work-card {
  background: white;
  border-radius: 12px;
  overflow: hidden;
  cursor: pointer;
  transition: all 0.3s ease;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  margin-bottom: 20px;
}

.work-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.12);
}

.work-image-wrapper {
  position: relative;
  height: 280px;
  overflow: hidden;
  background: #f5f5f5;
}

.work-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.3s ease;
}

.work-card:hover .work-image {
  transform: scale(1.1);
}

.work-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.4);
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0;
  transition: opacity 0.3s ease;
}

.work-card:hover .work-overlay {
  opacity: 1;
}

.quick-view-btn {
  background: white;
  border: none;
  color: #667eea;
  font-size: 20px;
}

.work-tags {
  position: absolute;
  top: 10px;
  left: 10px;
}

.work-info {
  padding: 16px;
}

.work-title {
  font: 1rem;
  font-weight: 600;
  color: #2c3e50;
  margin: 0 0 8px 0;
  display: -webkit-box;
  -webkit-line-clamp: 1;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.work-author {
  color: #7f8c8d;
  font-size: 0.85rem;
  margin-bottom: 12px;
}

.work-stats {
  display: flex;
  gap: 15px;
  padding-top: 12px;
  border-top: 1px solid #f0f0f0;
}

.list-view {
  display: flex;
  flex-direction: column;
  gap: 15px;
  margin-bottom: 40px;
}

.work-list-item {
  background: white;
  border-radius: 12px;
  padding: 20px;
  display: flex;
  gap: 20px;
  cursor: pointer;
  transition: all 0.3s ease;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.work-list-item:hover {
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.1);
  transform: translateY(-2px);
}

.item-image {
  width: 160px;
  height: 120px;
  flex-shrink: 0;
  border-radius: 8px;
  overflow: hidden;
}

.item-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.item-content {
  flex: 1;
}

.item-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.item-title {
  font-size: 1.1rem;
  font-weight: 600;
  color: #2c3e50;
  margin: 0;
}

.item-desc {
  color: #7f8c8d;
  font-size: 0.9rem;
  margin: 8px 0;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.item-meta {
  display: flex;
  gap: 20px;
  color: #95a5a6;
  font-size: 0.85rem;
}

.item-stats {
  display: flex;
  flex-direction: column;
  gap: 10px;
  justify-content: center;
  padding-left: 20px;
  border-left: 1px solid #f0f0f0;
}

.work-stats .stat-item,
.stat-box {
  display: flex;
  align-items: center;
  gap: 4px;
  color: #95a5a6;
  font-size: 0.85rem;
}

.stat-box {
  color: #7f8c8d;
  font-size: 0.9rem;
  gap: 6px;
}
</style>
