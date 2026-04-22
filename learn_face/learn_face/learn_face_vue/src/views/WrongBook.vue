<template>
  <div class="wrong-book-page">
    <div class="toolbar card">
      <div class="toolbar-row">
        <el-input v-model="filters.keyword" placeholder="搜索题干/试卷/知识点" clearable class="w-260" @keyup.enter="handleSearch" />
        <el-select v-model="filters.status" placeholder="状态" clearable class="w-160">
          <el-option label="待判分" value="pending_manual" />
          <el-option label="待巩固" value="active" />
          <el-option label="已掌握" value="mastered" />
        </el-select>
        <el-select v-model="filters.category" placeholder="题型" clearable class="w-160">
          <el-option label="单选题" value="单选题" />
          <el-option label="多选题" value="多选题" />
          <el-option label="判断题" value="判断题" />
          <el-option label="简答题" value="简答题" />
        </el-select>
        <el-button type="primary" @click="handleSearch">搜索</el-button>
        <el-button @click="handleReset">重置</el-button>
      </div>
      <div class="toolbar-row">
        <el-button type="success" :disabled="selectedIds.length === 0" @click="startPractice">开始练习（{{ selectedIds.length }}）</el-button>
        <el-button type="danger" plain :disabled="selectedIds.length === 0" @click="removeSelected">批量移除（{{ selectedIds.length }}）</el-button>
        <el-button type="danger" plain @click="removeMastered">移除已掌握</el-button>
        <span class="tip">先勾选题目再开始，客观题提交后立即显示答案，主观题先看答案再自评。</span>
      </div>
    </div>

    <div class="list-panel card" v-loading="listState.loading">
      <el-table :data="listState.list" @selection-change="handleSelectionChange" row-key="id" border>
        <el-table-column type="selection" width="48" />
        <el-table-column prop="paperName" label="试卷" width="140" show-overflow-tooltip />
        <el-table-column prop="category" label="题型" width="96" align="center" />
        <el-table-column prop="question" label="题目" min-width="320" show-overflow-tooltip />
        <el-table-column prop="knowledgePoint" label="知识点" width="140" show-overflow-tooltip />
        <el-table-column prop="wrongCount" label="错题次数" width="96" align="center" />
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)" size="small">{{ statusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
      </el-table>

      <div class="pager">
        <el-pagination
          background
          layout="total, sizes, prev, pager, next"
          :current-page="listState.currentPage"
          :page-size="listState.pageSize"
          :page-sizes="[10, 20, 50]"
          :total="listState.total"
          @size-change="handleSizeChange"
          @current-change="handlePageChange"
        />
      </div>
    </div>

    <div class="practice-panel card" v-if="practiceState.active && currentQuestion">
      <div class="practice-header">
        <div class="left">
          <h3>错题练习</h3>
          <span>{{ practiceState.index + 1 }} / {{ practiceState.questions.length }}</span>
        </div>
        <div class="right">
          <el-tag size="small">{{ currentQuestion.category }}</el-tag>
          <el-tag size="small" :type="statusTagType(currentQuestion.status)">{{ statusText(currentQuestion.status) }}</el-tag>
          <el-button size="small" @click="stopPractice">结束练习</el-button>
        </div>
      </div>

      <div class="question-box">
        <div class="q-title">{{ currentQuestion.question }}</div>

        <template v-if="isObjective(currentQuestion.category)">
          <div class="options" :class="{ disabled: objectiveState.submitted }">
            <div
              v-for="(option, idx) in displayOptions(currentQuestion)"
              :key="idx"
              class="option"
              :class="objectiveOptionClass(currentQuestion, idx)"
              @click="chooseObjectiveOption(currentQuestion, idx)"
            >
              <span class="prefix">{{ optionPrefix(currentQuestion, idx) }}</span>
              <span>{{ option }}</span>
            </div>
          </div>

          <div class="actions">
            <el-button type="primary" :disabled="objectiveState.submitted" @click="submitObjective">确定</el-button>
            <el-button v-if="objectiveState.submitted" type="success" @click="nextQuestion">下一题</el-button>
          </div>

          <div class="feedback" v-if="objectiveState.submitted && objectiveState.result">
            <el-alert :type="objectiveState.result.correct ? 'success' : 'error'" :closable="false" show-icon>
              <template #title>
                {{ objectiveState.result.correct ? '回答正确' : '回答错误' }}
              </template>
            </el-alert>
            <div class="meta-line">正确答案：{{ objectiveState.result.correctAnswer || '-' }}</div>
            <div class="meta-line">解析：{{ objectiveState.result.parse || '暂无解析' }}</div>
          </div>
        </template>

        <template v-else>
          <div class="actions">
            <el-button type="primary" :disabled="subjectiveState.revealed" @click="revealSubjectiveAnswer">看答案</el-button>
          </div>

          <div class="feedback" v-if="subjectiveState.revealed">
            <div class="meta-line">参考答案：{{ currentQuestion.correctAnswer || '-' }}</div>
            <div class="meta-line">解析：{{ currentQuestion.parse || '暂无解析' }}</div>

            <div class="actions" v-if="!subjectiveState.marked">
              <el-button type="success" @click="markSubjective(true)">我会了</el-button>
              <el-button type="warning" @click="markSubjective(false)">还不会</el-button>
            </div>
            <div class="actions" v-else>
              <el-button type="success" @click="nextQuestion">下一题</el-button>
            </div>
          </div>
        </template>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  deleteMasteredWrongQuestionsAPI,
  deleteWrongQuestionsAPI,
  getWrongQuestionListAPI,
  submitWrongObjectiveAPI,
  submitWrongSubjectiveAPI
} from '../utils/api'

const filters = reactive({
  keyword: '',
  status: '',
  category: ''
})

const listState = reactive({
  loading: false,
  list: [],
  currentPage: 1,
  pageSize: 10,
  total: 0
})

const selectedRows = ref([])
const selectedIds = computed(() => selectedRows.value.map(item => item.id))

const practiceState = reactive({
  active: false,
  questions: [],
  index: 0,
  done: 0,
  correct: 0
})

const objectiveState = reactive({
  submitted: false,
  singleChoice: null,
  multiChoices: [],
  result: null
})

const subjectiveState = reactive({
  revealed: false,
  marked: false
})

const currentQuestion = computed(() => practiceState.questions[practiceState.index] || null)

onMounted(() => {
  fetchWrongList()
})

const normalizePageData = (res) => {
  if (Array.isArray(res)) {
    return { data: res, total: res.length, currentPage: listState.currentPage }
  }
  if (res && Array.isArray(res.data)) {
    return {
      data: res.data,
      total: Number(res.total) || res.data.length,
      currentPage: Number(res.currentPage) || listState.currentPage
    }
  }
  if (res && res.data && Array.isArray(res.data.data)) {
    return {
      data: res.data.data,
      total: Number(res.data.total) || res.data.data.length,
      currentPage: Number(res.data.currentPage) || listState.currentPage
    }
  }
  return { data: [], total: 0, currentPage: listState.currentPage }
}

const parseChoice = (choice) => {
  if (Array.isArray(choice)) return choice
  if (typeof choice === 'string' && choice.trim()) {
    try {
      const parsed = JSON.parse(choice)
      return Array.isArray(parsed) ? parsed : []
    } catch (e) {
      return choice.split(/\s*[,;]\s*/).map(v => v.trim()).filter(Boolean)
    }
  }
  return []
}

const normalizeQuestion = (item) => {
  return {
    ...item,
    category: item.category || '',
    choice: parseChoice(item.choice),
    wrongCount: Number(item.wrongCount) || 0,
    correctStreak: Number(item.correctStreak) || 0
  }
}

const fetchWrongList = async () => {
  listState.loading = true
  try {
    const params = {
      currentPage: listState.currentPage,
      pageSize: listState.pageSize,
      keyword: filters.keyword,
      status: filters.status,
      category: filters.category
    }
    const res = await getWrongQuestionListAPI(params)
    const page = normalizePageData(res)
    listState.list = (page.data || []).map(normalizeQuestion)
    listState.total = Number(page.total) || listState.list.length
    listState.currentPage = Number(page.currentPage) || listState.currentPage
  } catch (error) {
    ElMessage.error('获取错题列表失败')
  } finally {
    listState.loading = false
  }
}

const handleSearch = () => {
  listState.currentPage = 1
  fetchWrongList()
}

const handleReset = () => {
  filters.keyword = ''
  filters.status = ''
  filters.category = ''
  listState.currentPage = 1
  fetchWrongList()
}

const handleSizeChange = (size) => {
  listState.pageSize = size
  listState.currentPage = 1
  fetchWrongList()
}

const handlePageChange = (page) => {
  listState.currentPage = page
  fetchWrongList()
}

const handleSelectionChange = (rows) => {
  selectedRows.value = rows || []
}

const statusText = (status) => {
  if (status === 'pending_manual') return '待判分'
  if (status === 'mastered') return '已掌握'
  return '待巩固'
}

const statusTagType = (status) => {
  if (status === 'pending_manual') return 'warning'
  if (status === 'mastered') return 'success'
  return 'danger'
}

const normalizeCategory = (category) => {
  const text = String(category || '').toLowerCase()
  if (text.includes('single') || text.includes('单选')) return 'single'
  if (text.includes('multiple') || text.includes('多选')) return 'multiple'
  if (text.includes('judge') || text.includes('判断')) return 'judge'
  return 'subjective'
}

const isObjective = (category) => {
  const type = normalizeCategory(category)
  return type === 'single' || type === 'multiple' || type === 'judge'
}

const isMulti = (category) => normalizeCategory(category) === 'multiple'

const isJudge = (category) => normalizeCategory(category) === 'judge'

const displayOptions = (question) => {
  if (isJudge(question.category)) return ['正确', '错误']
  return Array.isArray(question.choice) ? question.choice : []
}

const optionPrefix = (question, idx) => {
  if (isJudge(question.category)) return idx === 0 ? '√' : '×'
  return `${String.fromCharCode(65 + idx)}.`
}

const resetPracticeQuestionState = () => {
  objectiveState.submitted = false
  objectiveState.singleChoice = null
  objectiveState.multiChoices = []
  objectiveState.result = null
  subjectiveState.revealed = false
  subjectiveState.marked = false
}

const startPractice = () => {
  if (selectedIds.value.length === 0) {
    ElMessage.warning('请先勾选要练习的错题')
    return
  }

  practiceState.questions = listState.list.filter(item => selectedIds.value.includes(item.id))
  if (practiceState.questions.length === 0) {
    ElMessage.warning('未找到可练习题目')
    return
  }

  practiceState.active = true
  practiceState.index = 0
  practiceState.done = 0
  practiceState.correct = 0
  resetPracticeQuestionState()
}

const stopPractice = () => {
  practiceState.active = false
  practiceState.questions = []
  practiceState.index = 0
  resetPracticeQuestionState()
}

const chooseObjectiveOption = (question, idx) => {
  if (objectiveState.submitted) return
  if (isMulti(question.category)) {
    const pos = objectiveState.multiChoices.indexOf(idx)
    if (pos >= 0) {
      objectiveState.multiChoices.splice(pos, 1)
    } else {
      objectiveState.multiChoices.push(idx)
      objectiveState.multiChoices.sort((a, b) => a - b)
    }
    return
  }
  objectiveState.singleChoice = idx
}

const objectiveOptionClass = (question, idx) => {
  if (isMulti(question.category)) {
    return objectiveState.multiChoices.includes(idx) ? 'selected' : ''
  }
  return objectiveState.singleChoice === idx ? 'selected' : ''
}

const buildObjectiveAnswer = (question) => {
  if (isJudge(question.category)) {
    if (objectiveState.singleChoice === null) return ''
    return objectiveState.singleChoice === 0 ? '正确' : '错误'
  }

  if (isMulti(question.category)) {
    if (objectiveState.multiChoices.length === 0) return ''
    return objectiveState.multiChoices.map(i => String.fromCharCode(65 + i)).join(',')
  }

  if (objectiveState.singleChoice === null) return ''
  return String.fromCharCode(65 + objectiveState.singleChoice)
}

const submitObjective = async () => {
  if (!currentQuestion.value) return
  const answer = buildObjectiveAnswer(currentQuestion.value)
  if (!answer) {
    ElMessage.warning('请先选择答案')
    return
  }

  try {
    const res = await submitWrongObjectiveAPI({
      id: currentQuestion.value.id,
      answer
    })

    if (!res || res.success === false) {
      ElMessage.error('提交练习结果失败')
      return
    }

    objectiveState.result = res
    objectiveState.submitted = true

    practiceState.done += 1
    if (res.correct) {
      practiceState.correct += 1
    }
  } catch (error) {
    ElMessage.error('提交练习结果失败')
  }
}

const revealSubjectiveAnswer = () => {
  subjectiveState.revealed = true
}

const markSubjective = async (mastered) => {
  if (!currentQuestion.value) return
  try {
    const res = await submitWrongSubjectiveAPI({
      id: currentQuestion.value.id,
      mastered
    })

    if (!res || res.success === false) {
      ElMessage.error('提交自评结果失败')
      return
    }

    subjectiveState.marked = true
    practiceState.done += 1
    if (mastered) {
      practiceState.correct += 1
    }
  } catch (error) {
    ElMessage.error('提交自评结果失败')
  }
}

const nextQuestion = () => {
  if (!currentQuestion.value) return

  if (isObjective(currentQuestion.value.category)) {
    if (!objectiveState.submitted) {
      ElMessage.warning('请先点击确定查看答案')
      return
    }
  } else {
    if (!subjectiveState.revealed) {
      ElMessage.warning('请先点击看答案')
      return
    }
    if (!subjectiveState.marked) {
      ElMessage.warning('请先完成主观题自评')
      return
    }
  }

  if (practiceState.index >= practiceState.questions.length - 1) {
    const accuracy = practiceState.done > 0
      ? Math.round((practiceState.correct / practiceState.done) * 100)
      : 0
    ElMessage.success(`练习完成，正确率 ${accuracy}%`)
    stopPractice()
    fetchWrongList()
    return
  }

  practiceState.index += 1
  resetPracticeQuestionState()
}

const removeSelected = async () => {
  if (selectedIds.value.length === 0) {
    ElMessage.warning('请先勾选要移除的错题')
    return
  }

  try {
    await ElMessageBox.confirm(
      `确认移除已勾选的 ${selectedIds.value.length} 道错题吗？`,
      '提示',
      { type: 'warning', confirmButtonText: '确认移除', cancelButtonText: '取消' }
    )
  } catch (e) {
    return
  }

  listState.loading = true
  try {
    const res = await deleteWrongQuestionsAPI(selectedIds.value)
    if (!res || res.success === false) {
      ElMessage.error('批量移除失败')
      return
    }

    const removed = Number(res.removed) || 0
    ElMessage.success(`已移除 ${removed} 道错题`)
    selectedRows.value = []
    if (practiceState.active) {
      stopPractice()
    }
    await fetchWrongList()
  } catch (error) {
    ElMessage.error('批量移除失败')
  } finally {
    listState.loading = false
  }
}

const removeMastered = async () => {
  try {
    await ElMessageBox.confirm(
      '确认移除全部“已掌握”错题吗？该操作不可恢复。',
      '提示',
      { type: 'warning', confirmButtonText: '确认移除', cancelButtonText: '取消' }
    )
  } catch (e) {
    return
  }

  listState.loading = true
  try {
    const res = await deleteMasteredWrongQuestionsAPI()
    if (!res || res.success === false) {
      ElMessage.error('移除已掌握错题失败')
      return
    }

    const removed = Number(res.removed) || 0
    ElMessage.success(`已移除 ${removed} 道已掌握错题`)
    selectedRows.value = []
    if (practiceState.active) {
      stopPractice()
    }
    await fetchWrongList()
  } catch (error) {
    ElMessage.error('移除已掌握错题失败')
  } finally {
    listState.loading = false
  }
}
</script>

<style scoped>
.wrong-book-page {
  padding: 20px;
  background: #f6f8fb;
  min-height: 100vh;
}

.card {
  background: #fff;
  border-radius: 10px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.06);
  padding: 16px;
  margin-bottom: 16px;
}

.toolbar-row {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 10px;
}

.toolbar-row:last-child {
  margin-bottom: 0;
}

.tip {
  color: #909399;
  font-size: 12px;
}

.w-260 {
  width: 260px;
}

.w-160 {
  width: 160px;
}

.pager {
  margin-top: 14px;
  display: flex;
  justify-content: center;
}

.practice-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-bottom: 1px solid #ebeef5;
  padding-bottom: 10px;
}

.practice-header .left {
  display: flex;
  align-items: center;
  gap: 8px;
}

.practice-header .left h3 {
  margin: 0;
}

.practice-header .right {
  display: flex;
  align-items: center;
  gap: 8px;
}

.question-box {
  margin-top: 14px;
}

.q-title {
  font-size: 17px;
  color: #303133;
  line-height: 1.7;
  margin-bottom: 14px;
}

.options {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.option {
  border: 1px solid #dcdfe6;
  border-radius: 8px;
  padding: 10px 12px;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 8px;
  transition: all 0.2s;
}

.option:hover {
  border-color: #409eff;
}

.option.selected {
  border-color: #409eff;
  background: #ecf5ff;
}

.options.disabled .option {
  cursor: not-allowed;
}

.prefix {
  width: 28px;
  color: #409eff;
  font-weight: 600;
}

.actions {
  margin-top: 14px;
  display: flex;
  gap: 10px;
}

.feedback {
  margin-top: 14px;
  border: 1px solid #ebeef5;
  border-radius: 8px;
  padding: 12px;
  background: #fafcff;
}

.meta-line {
  margin-top: 8px;
  color: #606266;
  line-height: 1.6;
}

@media (max-width: 768px) {
  .toolbar-row {
    flex-wrap: wrap;
  }

  .w-260,
  .w-160 {
    width: 100%;
  }

  .practice-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 8px;
  }
}
</style>
