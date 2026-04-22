<template>
  <div class="analyse-container">
    <div class="analyse-header">
      <h2>数据分析看板</h2>
      <p>学习进度与成绩分析</p>
    </div>

    <!-- 学习进度统计 -->
    <div class="section-card">
      <div class="section-header">
        <h3>课程学习进度</h3>
      </div>
      <div class="section-content">
        <el-table 
          :data="learnStats" 
          style="width: 100%" 
          border 
          stripe 
          v-loading="learnLoading"
        >
          <el-table-column prop="courseName" label="课程名称" min-width="200" />
          <el-table-column prop="createBy" label="创建者" width="150" />
          <el-table-column prop="learnedDuration" label="已学时长(小时)" width="120" align="center" />
          <el-table-column prop="courseTotalHours" label="课程总时长(小时)" width="140" align="center" />
          <el-table-column prop="progress" label="学习进度" width="120" align="center">
            <template slot-scope="scope">
              <el-progress 
                :percentage="parseFloat(scope.row.progress)" 
                :format="() => scope.row.progress" 
                :stroke-width="10"
              />
            </template>
          </el-table-column>
        </el-table>
      </div>
    </div>

    <!-- 目标完成情况 -->
    <div class="section-card">
      <div class="section-header">
        <h3>目标完成情况</h3>
      </div>
      <div class="section-content">
        <div class="goal-grid">
          <div 
            v-for="(goal, index) in goalStats" 
            :key="index" 
            class="goal-card"
            :class="{ 'completed': goal.status === 'completed', 'progress': goal.status === 'progress' }"
          >
            <div class="goal-title">{{ goal.title }}</div>
            <div class="goal-status">
              <el-tag 
                :type="goal.status === 'completed' ? 'success' : 'warning'" 
                size="small"
              >
                {{ goal.status === 'completed' ? '已完成' : '进行中' }}
              </el-tag>
            </div>
            <div class="goal-progress">
              <el-progress 
                :percentage="parseInt(goal.completionRate)" 
                :color="goal.status === 'completed' ? '#67C23A' : '#E6A23C'"
                :stroke-width="8"
              />
              <div class="completion-rate">{{ goal.completionRate }}</div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 成绩分布统计 -->
    <div class="section-card">
      <div class="section-header">
        <h3>成绩分布统计</h3>
      </div>
      <div class="section-content">
        <div class="charts-row">
          <div class="chart-container">
            <h4>分数段分布</h4>
            <div ref="scoreDistributionChart" class="chart"></div>
          </div>
          <div class="chart-container">
            <h4>通过状态统计</h4>
            <div ref="passStatusChart" class="chart"></div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import * as echarts from 'echarts'

export default {
  name: 'Analyse',
  data() {
    return {
      learnStats: [],
      goalStats: [],
      scoreStats: {},
      learnLoading: false,
      goalLoading: false,
      scoreLoading: false
    }
  },
  mounted() {
    this.fetchAllStats()
  },
  methods: {
    async fetchAllStats() {
      await Promise.all([
        this.fetchLearnStats(),
        this.fetchGoalStats(),
        this.fetchScoreStats()
      ])
      
      // 初始化图表
      this.$nextTick(() => {
        this.initCharts()
      })
    },

    // 获取学习统计数据
    fetchLearnStats() {
      this.learnLoading = true
      return this.$http.get('/v1/statistics/learn')
        .then(response => {
          if (response.data.code === 200) {
            this.learnStats = response.data.data || []
          } else {
            this.$message.error(response.data.message || '获取学习统计失败')
          }
        })
        .catch(error => {
          console.error('获取学习统计失败:', error)
          this.$message.error('获取学习统计失败')
        })
        .finally(() => {
          this.learnLoading = false
        })
    },

    // 获取目标统计数据
    fetchGoalStats() {
      this.goalLoading = true
      return this.$http.get('/v1/statistics/goalAnalyse')
        .then(response => {
          if (response.data.code === 200) {
            this.goalStats = response.data.data || []
          } else {
            this.$message.error(response.data.message || '获取目标统计失败')
          }
        })
        .catch(error => {
          console.error('获取目标统计失败:', error)
          this.$message.error('获取目标统计失败')
        })
        .finally(() => {
          this.goalLoading = false
        })
    },

    // 获取成绩统计数据
    fetchScoreStats() {
      this.scoreLoading = true
      return this.$http.get('/v1/statistics/score')
        .then(response => {
          if (response.data.code === 200) {
            this.scoreStats = response.data.data || {}
          } else {
            this.$message.error(response.data.message || '获取成绩统计失败')
          }
        })
        .catch(error => {
          console.error('获取成绩统计失败:', error)
          this.$message.error('获取成绩统计失败')
        })
        .finally(() => {
          this.scoreLoading = false
        })
    },

    // 初始化图表
    initCharts() {
      this.initScoreDistributionChart()
      this.initPassStatusChart()
    },

    // 初始化分数分布图表
    initScoreDistributionChart() {
      const chartDom = this.$refs.scoreDistributionChart
      if (!chartDom) return
      
      const myChart = echarts.init(chartDom)
      
      const distributionData = this.scoreStats.distribution || {}
      const labels = Object.keys(distributionData)
      const values = Object.values(distributionData)
      
      const option = {
        tooltip: {
          trigger: 'item',
          formatter: '{a} <br/>{b}: {c} ({d}%)'
        },
        legend: {
          orient: 'vertical',
          left: 'left',
        },
        series: [
          {
            name: '分数段分布',
            type: 'pie',
            radius: '50%',
            data: labels.map((label, index) => ({
              value: values[index],
              name: label
            })),
            emphasis: {
              itemStyle: {
                shadowBlur: 10,
                shadowOffsetX: 0,
                shadowColor: 'rgba(0, 0, 0, 0.5)'
              }
            }
          }
        ]
      }
      
      myChart.setOption(option)
      window.addEventListener('resize', () => myChart.resize())
    },

    // 初始化通过状态图表
    initPassStatusChart() {
      const chartDom = this.$refs.passStatusChart
      if (!chartDom) return
      
      const myChart = echarts.init(chartDom)
      
      const passStatusData = this.scoreStats.passStatus || {}
      const labels = Object.keys(passStatusData)
      const values = Object.values(passStatusData)
      
      const option = {
        tooltip: {
          trigger: 'axis',
          axisPointer: {
            type: 'shadow'
          }
        },
        grid: {
          left: '3%',
          right: '4%',
          bottom: '3%',
          containLabel: true
        },
        xAxis: {
          type: 'value'
        },
        yAxis: {
          type: 'category',
          data: labels
        },
        series: [
          {
            name: '通过状态',
            type: 'bar',
            data: values,
            label: {
              show: true,
              position: 'right'
            },
            emphasis: {
              itemStyle: {
                shadowBlur: 10,
                shadowOffsetX: 0,
                shadowColor: 'rgba(0, 0, 0, 0.5)'
              }
            }
          }
        ]
      }
      
      myChart.setOption(option)
      window.addEventListener('resize', () => myChart.resize())
    }
  }
}
</script>

<style scoped>
.analyse-container {
  padding: 20px;
  background-color: #f5f7fa;
  min-height: 100vh;
}

.analyse-header {
  margin-bottom: 20px;
}

.analyse-header h2 {
  margin: 0 0 5px 0;
  color: #303133;
  font-size: 24px;
  font-weight: 600;
}

.analyse-header p {
  margin: 0;
  color: #909399;
  font-size: 14px;
}

.section-card {
  background: #fff;
  margin-bottom: 20px;
  border-radius: 8px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
  overflow: hidden;
}

.section-header {
  padding: 16px 20px;
  border-bottom: 1px solid #ebeef5;
  background-color: #fafafa;
}

.section-header h3 {
  margin: 0;
  color: #303133;
  font-size: 16px;
  font-weight: 500;
}

.section-content {
  padding: 20px;
}

.goal-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 20px;
}

.goal-card {
  border: 1px solid #ebeef5;
  border-radius: 8px;
  padding: 16px;
  transition: all 0.3s ease;
}

.goal-card.completed {
  border-color: #67c23a;
  background-color: #f0f9eb;
}

.goal-card.progress {
  border-color: #e6a23c;
  background-color: #fdf6ec;
}

.goal-card:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.goal-title {
  font-size: 16px;
  font-weight: 500;
  color: #303133;
  margin-bottom: 12px;
}

.goal-status {
  margin-bottom: 12px;
}

.goal-progress {
  position: relative;
}

.completion-rate {
  position: absolute;
  right: 0;
  top: 50%;
  transform: translateY(-50%);
  font-size: 12px;
  color: #909399;
}

.charts-row {
  display: flex;
  gap: 20px;
}

.chart-container {
  flex: 1;
}

.chart-container h4 {
  margin: 0 0 16px 0;
  color: #303133;
  font-size: 16px;
  font-weight: 500;
}

.chart {
  height: 400px;
  width: 100%;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .charts-row {
    flex-direction: column;
  }
  
  .goal-grid {
    grid-template-columns: 1fr;
  }
}
</style>