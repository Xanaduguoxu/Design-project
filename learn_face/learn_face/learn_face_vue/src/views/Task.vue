<template>
  <div class="exam-container">
    <!-- 试卷选择页面 -->
    <div v-if="showExamSelect && !examStarted" class="exam-select-page">
      <div class="select-container">
        <h2 class="select-title">选择考试试卷</h2>
        <div class="exam-list">
          <div 
            v-for="examName in examNames" 
            :key="examName" 
            class="exam-item"
            :class="{ 'selected': selectedExamName === examName }"
            @click="selectExam(examName)"
          >
            <span class="exam-name">{{ examName }}</span>
            <span class="exam-icon">📄</span>
          </div>
        </div>
        
        <div class="select-actions">
          <button class="btn btn-random" @click="startRandomExam">
            <span class="btn-icon">🎲</span>
            随机试卷
          </button>
          <button 
            class="btn btn-confirm-select" 
            :disabled="!selectedExamName"
            @click="startSelectedExam"
          >
            开始考试
          </button>
        </div>
      </div>
    </div>

    <!-- 考试确认弹窗 -->
    <div v-if="showConfirmDialog" class="confirm-overlay">
      <div class="confirm-dialog">
        <h3 class="dialog-title">开始考试</h3>
        <p class="dialog-content">您即将开始考试：<strong>{{ currentExamName }}</strong>，请确认是否准备就绪？</p>
        <div class="dialog-actions">
          <button class="btn btn-cancel" @click="cancelExam">取消</button>
          <button class="btn btn-confirm" @click="startExam">确认开始</button>
        </div>
      </div>
    </div>

    <!-- 考试取消页面 -->
    <div v-if="examCancelled" class="exam-cancelled">
      <div class="cancelled-content">
        <h2>考试已取消</h2>
        <p>您可以稍后再次尝试考试，或者返回主页继续学习。</p>
        <div class="cancelled-actions">
          <button class="btn btn-secondary" @click="restartExam">重新考试</button>
          <button class="btn btn-primary" @click="goHome">返回主页</button>
        </div>
      </div>
    </div>

    <!-- 考试进行中页面 -->
    <div v-if="!showConfirmDialog && examStarted" class="exam-page">
      <div class="exam-header">
        <div class="exam-info">
          <h2>{{ currentExamName }}</h2>
          <div class="exam-timer">
            <span class="timer-label">考试时间:</span>
            <span class="timer-display">{{ formatTime(remainingTime) }}</span>
          </div>
        </div>
        <div class="exam-progress">
          <span class="progress-text">{{ currentQuestionIndex + 1 }} / {{ questions.length }}</span>
          <span class="total-score" v-if="questions.length > 0">总分: {{ totalScore }}</span>
        </div>
      </div>

      <div class="exam-content" v-if="questions.length > 0">
        <div v-for="(question, index) in questions" :key="question.id || index" 
             v-show="currentQuestionIndex === index" class="question-container">
          <div class="question-header">
            <h3 class="question-text">{{ index + 1 }}. {{ question.question }}</h3>
            <div class="question-meta">
              <span class="question-category">{{ getCategoryName(question.category) }}</span>
              <span class="question-score">分值: {{ question.score }}</span>
            </div>
          </div>

          <!-- 单选题 -->
          <div v-if="question.category === '单选题'" class="question-options">
            <div v-for="(option, optionIndex) in getChoiceOptions(question)" 
                 :key="optionIndex" 
                 class="option-item" 
                 :class="{ 'selected': isOptionSelected(question.id, optionIndex) }"
                 @click="selectOption(question.id, optionIndex)">
              <span class="option-prefix">{{ getOptionLetter(optionIndex) }}.</span>
              <span class="option-text">{{ option }}</span>
            </div>
          </div>

          <!-- 多选题 -->
          <div v-else-if="question.category === '多选题'" class="question-options">
            <div v-for="(option, optionIndex) in getChoiceOptions(question)" 
                 :key="optionIndex" 
                 class="option-item" 
                 :class="{ 'selected': isOptionSelected(question.id, optionIndex) }"
                 @click="selectOption(question.id, optionIndex)">
              <span class="option-prefix">{{ getOptionLetter(optionIndex) }}.</span>
              <span class="option-text">{{ option }}</span>
            </div>
            <div class="multi-hint">提示：多选题可选择多个答案</div>
          </div>

          <!-- 判断题 -->
          <div v-else-if="question.category === '判断题'" class="question-options">
            <div v-for="(option, optionIndex) in ['正确', '错误']" 
                 :key="optionIndex" 
                 class="option-item" 
                 :class="{ 'selected': isOptionSelected(question.id, optionIndex) }"
                 @click="selectOption(question.id, optionIndex)">
              <span class="option-prefix">{{ optionIndex === 0 ? '✓' : '✗' }}</span>
              <span class="option-text">{{ option }}</span>
            </div>
          </div>

          <!-- 简答题 -->
          <div v-else-if="question.category === '简答题'" class="question-answer">
            <textarea 
              v-model="question.userAnswer" 
              class="answer-textarea" 
              placeholder="请输入您的答案..."
              rows="6"
            ></textarea>
          </div>

          <!-- 其他类型（默认显示简答题） -->
          <div v-else class="question-answer">
            <textarea 
              v-model="question.userAnswer" 
              class="answer-textarea" 
              placeholder="请输入您的答案..."
              rows="6"
            ></textarea>
          </div>

          <!-- 导航按钮 -->
          <div class="question-navigation">
            <button 
              class="nav-btn prev-btn" 
              :disabled="currentQuestionIndex <= 0"
              @click="goToPreviousQuestion">
              上一题
            </button>
            <button 
              class="nav-btn next-btn" 
              :disabled="currentQuestionIndex >= questions.length - 1"
              @click="goToNextQuestion">
              下一题
            </button>
            <button 
              v-if="currentQuestionIndex === questions.length - 1"
              class="nav-btn submit-btn"
              @click="submitExam">
              提交试卷
            </button>
          </div>
        </div>
      </div>
      
      <!-- 无数据提示 -->
      <div v-else class="no-data">
        <p>暂无试题数据，请联系管理员或刷新重试。</p>
      </div>
    </div>

    <!-- 考试结束页面 -->
    <div v-if="examFinished" class="exam-result">
      <div class="result-content">
        <h2>考试结束</h2>
        <p>您的考试已完成，感谢您的参与！</p>
        <button class="btn btn-primary" @click="restartExam">重新考试</button>
      </div>
    </div>
  </div>
</template>

<script>
import { getTaskAPI, submitExamAPI, getTaskNamesAPI, selectTaskAPI } from '@/utils/api.js'
import { ElMessage } from 'element-plus'

export default {
  name: 'Task',
  data() {
    return {
      showExamSelect: true,
      showConfirmDialog: false,
      examStarted: false,
      examFinished: false,
      examCancelled: false,
      examNames: [],
      selectedExamName: '',
      questions: [],
      currentQuestionIndex: 0,
      selectedAnswers: {},
      remainingTime: 3600,
      timerInterval: null,
      currentExamName: '',
      routeExamLoaded: '',
    }
  },
  computed: {
    totalScore() {
      return this.questions.reduce((sum, q) => sum + (Number(q.score) || 0), 0)
    }
  },
  async mounted() {
    await this.fetchExamNames()
    await this.tryLoadExamFromRoute()
  },
  watch: {
    '$route.query.examName': {
      handler() {
        this.tryLoadExamFromRoute()
      }
    }
  },
  methods: {
    // 获取试卷列表
    async fetchExamNames() {
      try {
        const response = await getTaskNamesAPI()
        console.log('试卷列表接口返回:', response)
        
        let list = []
        if (response && response.code === 200 && Array.isArray(response.data)) {
          list = response.data
        } else if (Array.isArray(response)) {
          list = response
        } else if (response && Array.isArray(response.data)) {
          list = response.data
        }
        
        if (list && list.length > 0) {
          this.examNames = list
          console.log('试卷列表获取成功:', this.examNames)
        } else {
          console.warn('未获取到试卷列表')
          this.examNames = []
          ElMessage.warning('未获取到可用试卷')
        }
      } catch (error) {
        console.error('获取试卷列表失败:', error)
        ElMessage.error('获取试卷列表失败，请刷新重试')
      }
    },
    
    // 获取题型中文名称
    async tryLoadExamFromRoute() {
      const queryExamName = this.$route?.query?.examName
      const examName = typeof queryExamName === 'string' ? queryExamName.trim() : ''
      if (!examName) {
        return
      }
      if (this.routeExamLoaded === examName && this.questions.length > 0) {
        return
      }

      this.selectedExamName = examName
      try {
        await this.fetchExamDataByName(examName)
        this.showExamSelect = false
        this.showConfirmDialog = true
        this.routeExamLoaded = examName
      } catch (error) {
        this.routeExamLoaded = ''
        ElMessage.error(`自动加载试卷失败: ${examName}`)
      }
    },
    getCategoryName(category) {
      const categoryMap = {
        '单选题': '单选题',
        '多选题': '多选题',
        '判断题': '判断题',
        '简答题': '简答题',
        'single': '单选题',
        'multiple': '多选题',
        'judge': '判断题',
        'essay': '简答题'
      }
      return categoryMap[category] || category || '未知题型'
    },
    
    // 获取选项数组
    getChoiceOptions(question) {
      return this.parseChoice(question.choice)
    },

    parseChoice(rawChoice) {
      if (Array.isArray(rawChoice)) {
        return rawChoice
      }
      if (typeof rawChoice !== 'string') {
        return []
      }
      const text = rawChoice.trim()
      if (!text) {
        return []
      }
      try {
        const parsed = JSON.parse(text)
        return Array.isArray(parsed) ? parsed : []
      } catch (e) {
        return this.parseLegacyChoiceText(text)
      }
    },
    parseLegacyChoiceText(text) {
      let normalized = text
      if (normalized.startsWith('[') && normalized.endsWith(']')) {
        normalized = normalized.slice(1, -1).trim()
      }
      if (!normalized) {
        return []
      }

      const lines = normalized.split(/\r?\n/).map(item => item.trim()).filter(Boolean)
      if (lines.length > 1) {
        return lines
      }

      const byLabel = normalized
        .split(/(?=[A-H][\.\)]\s*)/i)
        .map(item => item.replace(/^[,;\s]+/, '').trim())
        .filter(Boolean)
      if (byLabel.length > 1) {
        return byLabel
      }

      return normalized.split(/\s*[,;]\s*/).map(item => item.trim()).filter(Boolean)
    },
    
    // 获取选项字母前缀
    getOptionLetter(index) {
      return String.fromCharCode(65 + index)
    },
    
    // 选择试卷
    selectExam(examName) {
      this.selectedExamName = examName
    },
    
    // 开始随机试卷
    async startRandomExam() {
      try {
        ElMessage.info('正在获取随机试卷...')
        await this.fetchRandomExamData()
        this.showExamSelect = false
        this.showConfirmDialog = true
      } catch (error) {
        console.error('获取随机试卷失败:', error)
        ElMessage.error('获取随机试卷失败，请重试')
      }
    },
    
    // 开始选中的试卷
    async startSelectedExam() {
      if (!this.selectedExamName) {
        ElMessage.warning('请先选择试卷')
        return
      }
      
      try {
        ElMessage.info(`正在加载试卷：${this.selectedExamName}...`)
        await this.fetchExamDataByName(this.selectedExamName)
        this.showExamSelect = false
        this.showConfirmDialog = true
      } catch (error) {
        console.error('获取试卷失败:', error)
        ElMessage.error('获取试卷失败，请重试')
      }
    },
    
    // 获取随机试卷数据
    async fetchRandomExamData() {
      try {
        const response = await getTaskAPI()
        console.log('随机试卷接口返回:', response)
        
        let list = []
        if (Array.isArray(response)) {
          list = response
        } else if (response && Array.isArray(response.data)) {
          list = response.data
        }
        
        if (list && list.length > 0) {
          this.processQuestionData(list)
          this.currentExamName = this.questions[0]?.examName || '随机考试'
          console.log('随机试题数据处理完成:', this.questions)
        } else {
          console.warn('解析后未发现试题数组')
          this.questions = []
          throw new Error('未获取到试题数据')
        }
      } catch (error) {
        console.error('获取随机试卷失败:', error)
        throw error
      }
    },
    
    // 根据试卷名称获取试题
    async fetchExamDataByName(examName) {
      try {
        const response = await selectTaskAPI(examName)
        console.log('指定试卷接口返回:', response)
        
        let list = []
        if (Array.isArray(response)) {
          list = response
        } else if (response && Array.isArray(response.data)) {
          list = response.data
        } else if (response && response.code === 200 && Array.isArray(response.data)) {
          list = response.data
        }
        
        if (list && list.length > 0) {
          this.processQuestionData(list)
          this.currentExamName = examName
          console.log('指定试题数据处理完成:', this.questions)
        } else {
          console.warn('解析后未发现试题数组')
          this.questions = []
          throw new Error('未获取到试题数据')
        }
      } catch (error) {
        console.error('获取指定试卷失败:', error)
        throw error
      }
    },
    
    // 统一处理题目数据
    processQuestionData(list) {
      this.questions = list.map((question, index) => {
        // 确保每个题目都有必要的字段
        const processedQuestion = {
          id: question.id || index,
          question: question.question || question.title || '',
          category: this.normalizeCategory(question.category),
          score: question.score || 2,
          userAnswer: '',
          choice: []
        }
        
        // 处理选项（如果是选择题）
        if (processedQuestion.category === '单选题' || processedQuestion.category === '多选题') {
          const parsedChoices = this.parseChoice(question.choice)
          const parsedOptions = this.parseChoice(question.options)
          if (parsedChoices.length > 0) {
            processedQuestion.choice = parsedChoices
          } else if (parsedOptions.length > 0) {
            processedQuestion.choice = parsedOptions
          } else {
            // 如果没有选项，提供默认选项（用于调试）
            processedQuestion.choice = ['选项A', '选项B', '选项C', '选项D']
            console.warn(`题目 ${processedQuestion.id} 缺少选项，使用默认选项`)
          }
        }
        
        // 处理判断题
        if (processedQuestion.category === '判断题') {
          // 判断题不需要choice数组
          processedQuestion.choice = []
        }
        
        return processedQuestion
      })
    },
    
    // 标准化题型
    normalizeCategory(category) {
      if (!category) return '简答题'
      
      const categoryLower = category.toLowerCase()
      if (categoryLower === 'single' || categoryLower === '单选题' || categoryLower === '单选') {
        return '单选题'
      }
      if (categoryLower === 'multiple' || categoryLower === '多选题' || categoryLower === '多选') {
        return '多选题'
      }
      if (categoryLower === 'judge' || categoryLower === '判断题' || categoryLower === '判断') {
        return '判断题'
      }
      if (categoryLower === 'essay' || categoryLower === '简答题' || categoryLower === '简答') {
        return '简答题'
      }
      return category
    },
    
    cancelExam() {
      this.showConfirmDialog = false
      this.examCancelled = true
    },
    
    startExam() {
      if (this.questions.length === 0) {
        ElMessage.warning('试题加载中或加载失败，无法开始考试')
        return
      }
      this.showConfirmDialog = false
      this.examStarted = true
      this.startTimer()
    },
    
    startTimer() {
      if (this.timerInterval) clearInterval(this.timerInterval)
      this.timerInterval = setInterval(() => {
        this.remainingTime--
        if (this.remainingTime <= 0) {
          this.endExam()
        }
      }, 1000)
    },
    
    endExam() {
      clearInterval(this.timerInterval)
      this.examStarted = false
      this.examFinished = true
    },
    
    formatTime(seconds) {
      if (seconds < 0) return '00:00:00'
      const hours = Math.floor(seconds / 3600)
      const minutes = Math.floor((seconds % 3600) / 60)
      const secs = seconds % 60
      return `${hours.toString().padStart(2, '0')}:${minutes.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`
    },
    
    selectOption(questionId, optionIndex) {
      const question = this.questions.find(q => q.id === questionId)
      if (!question) return

      if (!this.selectedAnswers[questionId]) {
        this.selectedAnswers[questionId] = []
      }

      const currentSelections = this.selectedAnswers[questionId]
      const idx = currentSelections.indexOf(optionIndex)

      // 判断是单选还是多选
      const isSingle = question.category === '单选题' || question.category === '判断题'

      if (isSingle) {
        // 单选：直接替换
        this.selectedAnswers[questionId] = [optionIndex]
      } else {
        // 多选：切换选中状态
        let newSelections
        if (idx > -1) {
          newSelections = currentSelections.filter(i => i !== optionIndex)
        } else {
          newSelections = [...currentSelections, optionIndex]
          newSelections.sort((a, b) => a - b)
        }
        this.selectedAnswers[questionId] = newSelections
      }
      
      // 强制更新视图
      this.$forceUpdate()
    },
    
    isOptionSelected(questionId, optionIndex) {
      const selections = this.selectedAnswers[questionId] || []
      return selections.includes(optionIndex)
    },
    
    goToNextQuestion() {
      if (this.currentQuestionIndex < this.questions.length - 1) {
        this.currentQuestionIndex++
      }
    },
    
    goToPreviousQuestion() {
      if (this.currentQuestionIndex > 0) {
        this.currentQuestionIndex--
      }
    },
    
    async submitExam() {
      console.log('提交的答案数据:', this.selectedAnswers)
      
      const confirmSubmit = confirm('确定要提交试卷吗？提交后无法修改。')
      if (confirmSubmit) {
        try {
          const examData = {
            name: this.currentExamName,
            totalScore: this.totalScore,
            answer: []
          };
          
          for (let i = 0; i < this.questions.length; i++) {
            const question = this.questions[i];
            let userAnswer = '';
            
            if (question.category === '单选题' || question.category === '多选题') {
              // 选择题处理
              const selectedIndices = this.selectedAnswers[question.id] || [];
              if (selectedIndices.length > 0) {
                if (question.category === '单选题') {
                  // 单选题只取第一个
                  const index = selectedIndices[0];
                  userAnswer = this.getOptionLetter(index);
                } else {
                  // 多选题取所有选中的字母
                  userAnswer = selectedIndices.map(index => this.getOptionLetter(index)).join(',');
                }
              }
            } else if (question.category === '判断题') {
              // 判断题处理
              const selectedIndices = this.selectedAnswers[question.id] || [];
              if (selectedIndices.length > 0) {
                userAnswer = selectedIndices[0] === 0 ? '正确' : '错误';
              }
            } else {
              // 简答题直接使用用户输入的答案
              userAnswer = question.userAnswer || '';
            }
            
            examData.answer.push({
              id: question.id,
              question: question.question,
              category: question.category,
              answer: userAnswer,
              score: question.score || 0
            });
          }
          
          console.log('提交的考试数据:', examData);
          
          const response = await submitExamAPI(examData);
          
          if (response) {
            console.log('考试提交成功:', response);
            ElMessage.success('考试提交成功！');
            this.endExam();
          } else {
            console.error('考试提交失败');
            ElMessage.error('考试提交失败，请重试');
          }
        } catch (error) {
          console.error('提交考试时出错:', error);
          ElMessage.error('提交考试时出错: ' + error.message);
        }
      }
    },
    
    restartExam() {
      // 清除计时器
      if (this.timerInterval) {
        clearInterval(this.timerInterval)
        this.timerInterval = null
      }
      
      this.examFinished = false
      this.examCancelled = false
      this.showConfirmDialog = false
      this.examStarted = false
      this.showExamSelect = true
      this.currentQuestionIndex = 0
      this.selectedAnswers = {}
      this.remainingTime = 3600
      this.selectedExamName = ''
      this.routeExamLoaded = ''
      this.questions = []
      this.fetchExamNames()
    },
    
    goHome() {
      if (this.timerInterval) {
        clearInterval(this.timerInterval)
      }
      this.$router.push('/')
    }
  },
  
  beforeUnmount() {
    if (this.timerInterval) {
      clearInterval(this.timerInterval)
    }
  },
  beforeDestroy() {
    if (this.timerInterval) {
      clearInterval(this.timerInterval)
    }
  }
}
</script>

<style scoped>
.exam-container {
  min-height: 100vh;
  background: #f5f7fa;
  padding: 20px;
  font-family: 'Helvetica Neue', Helvetica, 'PingFang SC', 'Hiragino Sans GB', 'Microsoft YaHei', Arial, sans-serif;
}

/* 试卷选择页面样式 */
.exam-select-page {
  max-width: 800px;
  margin: 50px auto;
  background: white;
  border-radius: 12px;
  box-shadow: 0 5px 20px rgba(0, 0, 0, 0.05);
  overflow: hidden;
  animation: slideIn 0.3s ease-out;
}

.select-container {
  padding: 40px;
}

.select-title {
  text-align: center;
  color: #2c3e50;
  margin: 0 0 30px 0;
  font-size: 1.8rem;
  font-weight: 500;
}

.exam-list {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(250px, 1fr));
  gap: 15px;
  margin-bottom: 30px;
}

.exam-item {
  padding: 20px;
  border: 2px solid #ecf0f1;
  border-radius: 10px;
  cursor: pointer;
  transition: all 0.3s;
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: white;
}

.exam-item:hover {
  border-color: #3498db;
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(52, 152, 219, 0.1);
}

.exam-item.selected {
  border-color: #3498db;
  background: #e8f4fc;
  box-shadow: 0 4px 12px rgba(52, 152, 219, 0.2);
}

.exam-name {
  font-size: 1.1rem;
  font-weight: 500;
  color: #2c3e50;
}

.exam-icon {
  font-size: 1.5rem;
}

.select-actions {
  display: flex;
  gap: 20px;
  justify-content: center;
  margin-top: 20px;
}

.btn-random {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  border: none;
  padding: 12px 30px;
  border-radius: 6px;
  font-size: 1rem;
  cursor: pointer;
  transition: all 0.3s;
  display: flex;
  align-items: center;
  gap: 8px;
}

.btn-random:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.4);
}

.btn-confirm-select {
  background: #3498db;
  color: white;
  border: none;
  padding: 12px 30px;
  border-radius: 6px;
  font-size: 1rem;
  cursor: pointer;
  transition: all 0.3s;
}

.btn-confirm-select:hover:not(:disabled) {
  background: #2980b9;
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(52, 152, 219, 0.4);
}

.btn-confirm-select:disabled {
  background: #bdc3c7;
  cursor: not-allowed;
  opacity: 0.6;
}

.btn-icon {
  font-size: 1.2rem;
}

/* 确认弹窗样式 */
.confirm-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.6);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
  backdrop-filter: blur(2px);
}

.confirm-dialog {
  background: white;
  padding: 40px;
  border-radius: 12px;
  box-shadow: 0 15px 40px rgba(0, 0, 0, 0.2);
  text-align: center;
  min-width: 400px;
  animation: slideIn 0.3s ease-out;
}

@keyframes slideIn {
  from { transform: translateY(-20px); opacity: 0; }
  to { transform: translateY(0); opacity: 1; }
}

.dialog-title {
  margin: 0 0 20px 0;
  color: #2c3e50;
  font-size: 1.8rem;
}

.dialog-content {
  margin: 0 0 30px 0;
  color: #7f8c8d;
  font-size: 1.1rem;
}

.dialog-content strong {
  color: #3498db;
}

.dialog-actions {
  display: flex;
  gap: 20px;
  justify-content: center;
}

.btn {
  padding: 12px 30px;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  font-size: 1rem;
  font-weight: 600;
  transition: all 0.2s;
}

.btn:active {
  transform: scale(0.98);
}

.btn-cancel {
  background: #ecf0f1;
  color: #7f8c8d;
}

.btn-cancel:hover {
  background: #dfe6e9;
  color: #2c3e50;
}

.btn-confirm {
  background: #3498db;
  color: white;
}

.btn-confirm:hover {
  background: #2980b9;
  box-shadow: 0 4px 10px rgba(52, 152, 219, 0.3);
}

/* 考试页面样式 */
.exam-page {
  max-width: 1000px;
  margin: 0 auto;
  background: white;
  border-radius: 12px;
  box-shadow: 0 5px 20px rgba(0, 0, 0, 0.05);
  overflow: hidden;
}

.exam-header {
  background: linear-gradient(135deg, #2c3e50, #34495e);
  color: white;
  padding: 25px;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.exam-info h2 {
  margin: 0 0 10px 0;
  font-size: 1.6rem;
  font-weight: 500;
}

.exam-timer {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 1.1rem;
}

.timer-display {
  font-family: 'Monaco', 'Consolas', monospace;
  font-size: 1.4rem;
  font-weight: bold;
  color: #f1c40f;
  background: rgba(255, 255, 255, 0.1);
  padding: 5px 12px;
  border-radius: 4px;
}

.exam-progress {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 8px;
}

.progress-text {
  font-size: 1.5rem;
  font-weight: bold;
}

.total-score {
  background: rgba(231, 76, 60, 0.2);
  color: #ffcccc;
  padding: 4px 10px;
  border-radius: 20px;
  font-size: 0.9rem;
}

.question-container {
  padding: 40px;
  animation: fadeIn 0.3s ease-in;
}

@keyframes fadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}

.question-header {
  margin-bottom: 30px;
  border-bottom: 1px solid #eee;
  padding-bottom: 20px;
}

.question-text {
  font-size: 1.3rem;
  margin: 0 0 15px 0;
  line-height: 1.6;
  color: #2c3e50;
}

.question-meta {
  display: flex;
  gap: 15px;
}

.question-category {
  background: #e8f4fc;
  color: #3498db;
  padding: 4px 12px;
  border-radius: 4px;
  font-size: 0.85rem;
  font-weight: 600;
}

.question-score {
  background: #fdeaea;
  color: #e74c3c;
  padding: 4px 12px;
  border-radius: 4px;
  font-size: 0.85rem;
  font-weight: 600;
}

.question-options {
  margin-bottom: 20px;
}

.option-item {
  padding: 12px 20px;
  border: 2px solid #ecf0f1;
  border-radius: 8px;
  margin-bottom: 12px;
  cursor: pointer;
  transition: all 0.2s;
  display: flex;
  align-items: center;
  gap: 12px;
}

.option-item:hover {
  border-color: #bdc3c7;
  background: #f9f9f9;
  transform: translateX(5px);
}

.option-item.selected {
  border-color: #3498db;
  background: #e8f4fc;
}

.option-prefix {
  font-weight: 600;
  color: #3498db;
  min-width: 30px;
}

.option-text {
  flex: 1;
  font-size: 1rem;
  line-height: 1.5;
}

.multi-hint {
  margin-top: 10px;
  padding: 8px 12px;
  background: #f8f9fa;
  border-radius: 4px;
  color: #7f8c8d;
  font-size: 0.85rem;
  text-align: center;
}

.answer-textarea {
  width: 100%;
  padding: 20px;
  border: 2px solid #ecf0f1;
  border-radius: 8px;
  font-size: 1.05rem;
  line-height: 1.6;
  resize: vertical;
  min-height: 150px;
  transition: border-color 0.3s;
}

.answer-textarea:focus {
  outline: none;
  border-color: #3498db;
  box-shadow: 0 0 0 3px rgba(52, 152, 219, 0.1);
}

.question-navigation {
  margin-top: 40px;
  padding-top: 20px;
  border-top: 1px dashed #eee;
  display: flex;
  gap: 20px;
  justify-content: center;
}

.nav-btn {
  padding: 12px 35px;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  font-size: 1rem;
  transition: all 0.2s;
  font-weight: 500;
}

.prev-btn {
  background: white;
  border: 1px solid #bdc3c7;
  color: #7f8c8d;
}

.prev-btn:hover:not(:disabled) {
  border-color: #7f8c8d;
  color: #2c3e50;
}

.prev-btn:disabled {
  background: #f5f6f7;
  color: #dcdcdc;
  border-color: #eee;
  cursor: not-allowed;
}

.next-btn {
  background: #3498db;
  color: white;
  box-shadow: 0 4px 6px rgba(52, 152, 219, 0.2);
}

.next-btn:hover:not(:disabled) {
  background: #2980b9;
  transform: translateY(-1px);
}

.submit-btn {
  background: #27ae60;
  color: white;
  box-shadow: 0 4px 6px rgba(39, 174, 96, 0.2);
}

.submit-btn:hover {
  background: #219150;
  transform: translateY(-1px);
}

/* 考试结束页面 */
.exam-result {
  position: fixed;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  background: white;
  padding: 50px;
  border-radius: 12px;
  text-align: center;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.2);
  width: 90%;
  max-width: 500px;
  z-index: 1100;
}

.btn-primary {
  background: #3498db;
  color: white;
  border: none;
  padding: 12px 30px;
  border-radius: 6px;
  font-size: 1rem;
  cursor: pointer;
}

.no-data {
  padding: 60px;
  text-align: center;
  color: #999;
  font-size: 1.2rem;
}

/* 考试取消页面 */
.exam-cancelled {
  position: fixed;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  background: white;
  padding: 50px;
  border-radius: 12px;
  text-align: center;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.2);
  width: 90%;
  max-width: 500px;
  z-index: 1100;
}

.cancelled-content h2 {
  margin: 0 0 15px 0;
  color: #e74c3c;
  font-size: 1.8rem;
}

.cancelled-content p {
  margin: 0 0 30px 0;
  color: #7f8c8d;
  font-size: 1.1rem;
}

.cancelled-actions {
  display: flex;
  gap: 15px;
  justify-content: center;
}

.btn-secondary {
  background: #95a5a6;
  color: white;
  border: none;
  padding: 12px 25px;
  border-radius: 6px;
  font-size: 1rem;
  cursor: pointer;
  transition: background 0.3s;
}

.btn-secondary:hover {
  background: #7f8c8d;
}
</style>
