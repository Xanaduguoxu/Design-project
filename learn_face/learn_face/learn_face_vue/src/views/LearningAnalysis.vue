<template>
  <div class="analysis-brutal-container">
    <!-- 头部区域 -->
    <header class="header-brutal">
      <div class="header-content">
        <h2 class="title-brutal">DATA_CENTER</h2>
        <p class="sub-brutal">// TRACKING SYSTEM V.1.0</p>
      </div>
      <div class="date-badge-brutal">
        <span>DATE: {{ currentDate }}</span>
      </div>
    </header>

    <!-- 1. 核心指标卡片 -->
    <div class="metrics-grid-brutal">
      <div v-for="(item, index) in metrics" :key="index" class="metric-box-brutal">
        <div class="metric-meta">
          <span class="label-brutal">{{ item.label.toUpperCase() }}</span>
          <div class="value-brutal">
            <span class="num">{{ item.value }}</span>
            <span class="unit">{{ item.unit }}</span>
          </div>
        </div>
        <div class="metric-icon-border">
          <el-icon><component :is="item.icon" /></el-icon>
        </div>
      </div>
    </div>

    <div class="content-grid-brutal">
      <!-- 2. 课程学习时长统计 -->
      <div class="panel-brutal course-panel">
        <div class="panel-header-brutal">
          <div class="title-group">
            <span class="header-icon">▣</span>
            <h3>TIME_DISTRIBUTION</h3>
          </div>
          <span class="tag-brutal">SORTED</span>
        </div>
        
        <div class="course-list-brutal">
          <div v-for="(course, idx) in courseData" :key="course.id" class="course-row-brutal">
            <div class="rank-block" :class="{'top-rank': idx < 3}">
              <span>{{ String(idx + 1).padStart(2, '0') }}</span>
            </div>
            
            <div class="course-info-brutal">
              <div class="info-top-row">
                <span class="name-brutal">{{ course.name }}</span>
                <div class="stats-brutal">
                  <span class="current-val">{{ course.hours }}h</span>
                  <span class="separator">/</span>
                  <span class="total-val">{{ course.totalHours }}h</span>
                </div>
              </div>
              <div class="progress-track-brutal">
                <div 
                  class="progress-fill-brutal" 
                  :style="{ width: course.progress, backgroundColor: course.color }"
                ></div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 3. 目标完成情况 -->
      <div class="panel-brutal goal-panel">
        <div class="panel-header-brutal">
          <div class="title-group">
            <span class="header-icon">◎</span>
            <h3>TARGET_STATUS</h3>
          </div>
        </div>

        <div class="goals-list-brutal">
          <div v-for="(goal, index) in goalsData" :key="index" class="goal-row-brutal">
            <div class="goal-chart-brutal">
              <div class="chart-grid">
                <span class="grid-text">{{ Math.round(goal.progress) }}%</span>
              </div>
              <el-progress 
                type="dashboard" 
                :percentage="Math.round(goal.progress)" 
                :width="70"
                :stroke-width="4"
                :color="getGoalColor(index)"
                :show-text="false"
              />
            </div>
            <div class="goal-meta-brutal">
              <span class="goal-name">{{ goal.title }}</span>
              <span class="goal-stat">PROGRESS: {{ Math.round(goal.progress) }}%</span>
            </div>
          </div>
          
          <div v-if="goalsData.length === 0" class="empty-state-brutal">
            <span>NO_TARGETS_FOUND</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { reactive, computed, ref, onMounted } from 'vue'
import { 
  Timer, DataAnalysis, Trophy, Histogram, Aim 
} from '@element-plus/icons-vue'
import { getStatisticsMetricsAPI, getSelfLearningStatsAPI, getGoalsAPI } from '@/utils/api.js'
import { ElMessage } from 'element-plus'

const currentDate = new Date().toLocaleDateString()

const metrics = reactive([])

const iconMap = {
  'Timer': Timer,
  'DataAnalysis': DataAnalysis,
  'Trophy': Trophy,
  'Histogram': Histogram,
  'Aim': Aim
}

const initMetrics = async () => {
  try {
    const response = await getStatisticsMetricsAPI()
    if (response) {
      metrics.splice(0, metrics.length)
      
      response.forEach(item => {
        const iconComponent = iconMap[item.icon] || Timer
        metrics.push({
          label: item.label,
          value: item.value,
          unit: item.unit,
          icon: iconComponent,
          colorClass: item.colorClass
        })
      })
    } else {
      throw new Error(response.message || '获取统计数据失败')
    }
  } catch (error) {
    console.error('获取统计数据失败:', error)
    ElMessage.error('获取统计数据失败: ' + (error.message || '未知错误'))
    
    metrics.splice(0, metrics.length)
    metrics.push(
      { label: '总学习时长', value: 0, unit: '小时', icon: Timer, colorClass: 'icon-primary' },
      { label: '学习课程', value: 0, unit: '门', icon: Trophy, colorClass: 'icon-warning' },
      { label: '刷题数量', value: 0, unit: '道', icon: DataAnalysis, colorClass: 'icon-success' }
    )
  }
}

const courseData = reactive([])

const initCourseData = async () => {
  try {
    const response = await getSelfLearningStatsAPI()
    if (response) {
      courseData.splice(0, courseData.length)
      
      response.forEach((item, index) => {
        const colors = ['#111111', '#333333', '#666666', '#999999', '#BBBBBB', '#555555', '#777777']
        courseData.push({
          id: index + 1,
          name: item.courseName,
          hours: parseFloat(item.learnedDuration),
          totalHours: item.courseTotalHours,
          progress: item.progress,
          color: colors[index % colors.length]
        })
      })
    } else {
      throw new Error('获取学习统计数据失败')
    }
  } catch (error) {
    console.error('获取学习统计数据失败:', error)
    ElMessage.error('获取学习统计数据失败: ' + (error.message || '未知错误'))
    
    courseData.splice(0, courseData.length)
    courseData.push(
      { id: 1, name: '暂无数据', hours: 0, totalHours: 0, progress: '0%', color: '#c0c4cc' },
    )
  }
}

const goalsData = reactive([])

const getGoalColor = (index) => {
  const colors = ['#111111', '#444444', '#777777', '#AAAAAA', '#DDDDDD']
  return colors[index % colors.length]
}

const initGoalsData = async () => {
  try {
    const response = await getGoalsAPI()
    if (response) {
      goalsData.splice(0, goalsData.length)
      
      response.forEach(item => {
        goalsData.push({
          title: item.title,
          progress: item.progress
        })
      })
    } else {
      throw new Error('获取目标数据失败')
    }
  } catch (error) {
    console.error('获取目标数据失败:', error)
    ElMessage.error('获取目标数据失败: ' + (error.message || '未知错误'))
    
    goalsData.splice(0, goalsData.length)
  }
}

onMounted(async () => {
  await initMetrics()
  await initCourseData()
  await initGoalsData()
})

const totalCourseHours = computed(() => {
  return courseData.reduce((acc, cur) => acc + cur.hours, 0)
})
</script>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=JetBrains+Mono:wght@400;600;800&display=swap');

.analysis-brutal-container {
  padding: 40px;
  background-color: #E5E5E5;
  min-height: 100vh;
  font-family: 'JetBrains Mono', monospace;
  color: #111;
}

.header-brutal {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  margin-bottom: 30px;
  border-bottom: 4px solid #111;
  padding-bottom: 20px;
}

.title-brutal {
  font-size: 2.5rem;
  font-weight: 800;
  margin: 0;
  text-transform: uppercase;
  letter-spacing: -2px;
}

.sub-brutal {
  margin: 5px 0 0 0;
  font-size: 0.85rem;
  color: #666;
  font-weight: 600;
}

.date-badge-brutal {
  background: #111;
  color: #fff;
  padding: 8px 16px;
  font-weight: 700;
  font-size: 12px;
  border: 2px solid #111;
}

/* Metrics Grid */
.metrics-grid-brutal {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 20px;
  margin-bottom: 30px;
}

.metric-box-brutal {
  background: #fff;
  border: 2px solid #111;
  padding: 20px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  transition: all 0.15s ease;
}

.metric-box-brutal:hover {
  transform: translate(-4px, -4px);
  box-shadow: 8px 8px 0 #111;
}

.metric-meta {
  display: flex;
  flex-direction: column;
}

.label-brutal {
  font-size: 12px;
  font-weight: 700;
  color: #666;
  margin-bottom: 10px;
}

.value-brutal {
  display: flex;
  align-items: baseline;
  gap: 4px;
}

.num {
  font-size: 2.5rem;
  font-weight: 800;
  line-height: 1;
}

.unit {
  font-size: 14px;
  font-weight: 600;
  color: #666;
}

.metric-icon-border {
  width: 48px;
  height: 48px;
  border: 2px solid #111;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
}

/* Main Grid */
.content-grid-brutal {
  display: grid;
  grid-template-columns: 2fr 1fr;
  gap: 20px;
}

.panel-brutal {
  background: #fff;
  border: 2px solid #111;
  padding: 24px;
}

.panel-header-brutal {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 25px;
  padding-bottom: 15px;
  border-bottom: 2px solid #111;
}

.title-group {
  display: flex;
  align-items: center;
  gap: 10px;
}

.header-icon {
  font-size: 18px;
}

.panel-header-brutal h3 {
  margin: 0;
  font-size: 16px;
  text-transform: uppercase;
  letter-spacing: 1px;
}

.tag-brutal {
  background: #111;
  color: #fff;
  padding: 4px 10px;
  font-size: 10px;
  font-weight: 700;
}

/* Course List */
.course-list-brutal {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.course-row-brutal {
  display: flex;
  gap: 15px;
  padding-bottom: 15px;
  border-bottom: 1px solid #eee;
}

.course-row-brutal:last-child {
  border-bottom: none;
  padding-bottom: 0;
}

.rank-block {
  width: 32px;
  height: 32px;
  border: 1px solid #ccc;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 700;
  font-size: 14px;
  color: #999;
}

.rank-block.top-rank {
  background: #111;
  color: #fff;
  border-color: #111;
}

.course-info-brutal {
  flex: 1;
}

.info-top-row {
  display: flex;
  justify-content: space-between;
  margin-bottom: 8px;
  font-size: 13px;
}

.name-brutal {
  font-weight: 700;
  text-transform: capitalize;
}

.stats-brutal {
  font-weight: 600;
}

.separator {
  margin: 0 5px;
  color: #ccc;
}

.progress-track-brutal {
  height: 8px;
  background: #f0f0f0;
  border: 1px solid #111;
  position: relative;
}

.progress-fill-brutal {
  height: 100%;
}

/* Goals */
.goals-list-brutal {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.goal-row-brutal {
  display: flex;
  align-items: center;
  gap: 20px;
}

.goal-chart-brutal {
  position: relative;
  width: 70px;
  height: 70px;
}

.chart-grid {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1;
}

.grid-text {
  font-size: 14px;
  font-weight: 800;
  background: #fff;
  padding: 2px;
}

.goal-meta-brutal {
  flex: 1;
}

.goal-name {
  display: block;
  font-weight: 700;
  margin-bottom: 5px;
  font-size: 14px;
}

.goal-stat {
  font-size: 12px;
  color: #666;
  padding: 2px 6px;
  border: 1px solid #ccc;
}

.empty-state-brutal {
  text-align: center;
  padding: 40px 0;
  color: #999;
  font-weight: 600;
}

@media (max-width: 1024px) {
  .metrics-grid-brutal {
    grid-template-columns: 1fr;
  }
  
  .content-grid-brutal {
    grid-template-columns: 1fr;
  }
}
</style>