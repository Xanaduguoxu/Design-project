<template>
  <div class="exam-container">
    <div class="search-box">
      <el-form :model="searchForm" inline class="search-form">
        <el-form-item label="考试名称">
          <el-input
            v-model="searchForm.keyword"
            placeholder="请输入考试名称"
            clearable
            @keyup.enter.native="handleSearch"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">搜索</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </div>

    <div class="table-container">
      <el-table :data="examList" border stripe v-loading="loading">
        <el-table-column prop="id" label="ID" width="180" align="center" />
        <el-table-column prop="createBy" label="提交人" width="140" align="center" />
        <el-table-column prop="name" label="考试名称" min-width="180" />
        <el-table-column prop="totalScore" label="总分" width="90" align="center" />
        <el-table-column label="最终得分" width="100" align="center">
          <template slot-scope="scope">
            <span v-if="scope.row.finScore !== null && scope.row.finScore !== undefined">{{ scope.row.finScore }}</span>
            <span v-else class="pending-text">待批改</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100" align="center">
          <template slot-scope="scope">
            <el-tag :type="getExamStatus(scope.row).type" effect="light" size="small">
              {{ getExamStatus(scope.row).text }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="170" align="center">
          <template slot-scope="scope">
            <el-button size="mini" type="primary" @click="handleView(scope.row)">阅卷</el-button>
            <el-button size="mini" type="danger" @click="handleDelete(scope.row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-container">
        <el-pagination
          background
          layout="total, sizes, prev, pager, next, jumper"
          :current-page="currentPage"
          :page-sizes="[8, 16, 32, 64]"
          :page-size="pageSize"
          :total="total"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        />
      </div>
    </div>

    <el-dialog
      title="考试阅卷"
      :visible.sync="detailDialogVisible"
      width="86%"
      :close-on-click-modal="false"
      class="detail-dialog"
    >
      <div class="exam-detail-content" v-if="currentExam && Array.isArray(currentExam.answer)">
        <div class="summary">
          <div>考试名称：{{ currentExam.name }}</div>
          <div>总分：{{ currentExam.totalScore }}</div>
          <div>当前计算得分：{{ calculatedFinScore }}</div>
          <div>待批改主观题：{{ pendingManualCount }}</div>
        </div>

        <div class="questions-list">
          <div v-for="(question, index) in currentExam.answer" :key="question.id || index" class="question-item">
            <div class="question-top">
              <div class="left">
                <span class="index">{{ index + 1 }}.</span>
                <el-tag size="small">{{ question.category || '-' }}</el-tag>
                <el-tag size="small" :type="question.manualRequired ? 'warning' : 'success'">
                  {{ question.manualRequired ? '主观题' : '客观题' }}
                </el-tag>
              </div>
              <div class="right">满分 {{ question.score }} 分</div>
            </div>

            <div class="question-content">{{ question.question || '-' }}</div>

            <div class="line">
              <span class="label">学生答案：</span>
              <span class="value">{{ question.answer || '-' }}</span>
            </div>
            <div class="line">
              <span class="label">标准答案：</span>
              <span class="value">{{ question.correctAnswer || '-' }}</span>
            </div>

            <div v-if="!question.manualRequired" class="line">
              <span class="label">自动得分：</span>
              <span class="value strong">{{ question.obtainedScore || 0 }}</span>
              <span class="muted">判定：{{ question.result || '未知' }}</span>
            </div>

            <div v-else class="manual-area">
              <span class="label">教师评分：</span>
              <el-input-number
                v-model="question.teacherScore"
                :min="0"
                :max="question.score || 0"
                :step="1"
                size="small"
              />
              <span class="muted" v-if="question.teacherScore === null || question.teacherScore === undefined">未评分</span>
              <span class="muted" v-else>已评分</span>
            </div>
          </div>
        </div>
      </div>
      <span slot="footer">
        <el-button @click="detailDialogVisible = false">关闭</el-button>
        <el-button type="primary" :loading="gradingLoading" @click="submitGrading">提交阅卷</el-button>
      </span>
    </el-dialog>
  </div>
</template>

<script>
export default {
  name: 'ExamManagement',
  data() {
    return {
      loading: false,
      examList: [],
      total: 0,
      currentPage: 1,
      pageSize: 8,
      searchForm: {
        keyword: ''
      },
      detailDialogVisible: false,
      gradingLoading: false,
      currentExam: null
    }
  },
  created() {
    this.fetchList()
  },
  computed: {
    calculatedFinScore() {
      if (!this.currentExam || !Array.isArray(this.currentExam.answer)) {
        return 0
      }
      return this.currentExam.answer.reduce((sum, q) => {
        if (q.manualRequired) {
          return sum + (q.teacherScore !== null && q.teacherScore !== undefined ? Number(q.teacherScore) : 0)
        }
        return sum + (Number(q.obtainedScore) || 0)
      }, 0)
    },
    pendingManualCount() {
      if (!this.currentExam || !Array.isArray(this.currentExam.answer)) {
        return 0
      }
      return this.currentExam.answer.filter(q => q.manualRequired && (q.teacherScore === null || q.teacherScore === undefined)).length
    }
  },
  methods: {
    fetchList() {
      this.loading = true
      this.$http.post('/v1/exam/list', {
        currentPage: this.currentPage,
        pageSize: this.pageSize,
        keyword: this.searchForm.keyword
      }).then(response => {
        if (response.data.code === 200) {
          const page = response.data.data || {}
          const records = Array.isArray(page.data) ? page.data : []
          this.examList = records.map(item => this.normalizeExamRow(item))
          this.total = page.total || 0
        } else {
          this.$message.error(response.data.message || '获取考试列表失败')
        }
      }).catch(() => {
        this.$message.error('获取考试列表失败')
      }).finally(() => {
        this.loading = false
      })
    },
    normalizeExamRow(row) {
      const copied = JSON.parse(JSON.stringify(row || {}))
      const answerArray = this.parseAnswerArray(copied.answer)
      copied.answer = answerArray.map(item => this.normalizeQuestionForView(item))
      return copied
    },
    parseAnswerArray(answerValue) {
      if (Array.isArray(answerValue)) {
        return answerValue
      }
      if (typeof answerValue === 'string' && answerValue.trim()) {
        try {
          const parsed = JSON.parse(answerValue)
          return Array.isArray(parsed) ? parsed : []
        } catch (e) {
          return []
        }
      }
      return []
    },
    handleSearch() {
      this.currentPage = 1
      this.fetchList()
    },
    handleReset() {
      this.searchForm.keyword = ''
      this.currentPage = 1
      this.fetchList()
    },
    handleSizeChange(size) {
      this.pageSize = size
      this.currentPage = 1
      this.fetchList()
    },
    handleCurrentChange(page) {
      this.currentPage = page
      this.fetchList()
    },
    handleView(row) {
      this.currentExam = this.normalizeExamRow(row)
      this.detailDialogVisible = true
    },
    normalizeQuestionForView(question) {
      const score = Number(question.score) || 0
      const manualRequired = question.manualRequired !== undefined && question.manualRequired !== null
        ? !!question.manualRequired
        : this.isSubjective(question.category)

      let obtainedScore = Number(question.obtainedScore)
      if (Number.isNaN(obtainedScore)) {
        obtainedScore = this.isCorrectResult(question.result) ? score : 0
      }

      let teacherScore = question.teacherScore
      if (manualRequired) {
        if (teacherScore === null || teacherScore === undefined || teacherScore === '') {
          teacherScore = question.manualGraded === true ? obtainedScore : null
        } else {
          teacherScore = Number(teacherScore)
        }
      } else {
        teacherScore = null
      }

      return {
        ...question,
        score,
        manualRequired,
        manualGraded: !!question.manualGraded,
        obtainedScore,
        teacherScore
      }
    },
    isCorrectResult(result) {
      const text = String(result || '').trim().toLowerCase()
      if (!text) return false
      return text === '正确' || text === 'correct' || text === '对' || text.includes('正确') || text.includes('correct') || text.includes('姝ｇ')
    },
    isSubjective(category) {
      const text = String(category || '').toLowerCase()
      return text.includes('简答') || text.includes('essay') || text.includes('绠€绛')
    },
    submitGrading() {
      if (!this.currentExam) return

      const hasUngradedManual = this.currentExam.answer.some(q => q.manualRequired && (q.teacherScore === null || q.teacherScore === undefined))
      if (hasUngradedManual) {
        this.$message.warning('请先完成所有主观题评分再提交')
        return
      }

      const payload = {
        id: this.currentExam.id,
        answer: this.currentExam.answer.map(q => ({
          id: q.id,
          teacherScore: q.manualRequired ? Number(q.teacherScore) : null
        }))
      }

      this.gradingLoading = true
      this.$http.post('/v1/exam/update', payload).then(response => {
        if (response.data.code === 200) {
          this.$message.success('阅卷提交成功')
          this.detailDialogVisible = false
          this.fetchList()
        } else {
          this.$message.error(response.data.message || '阅卷提交失败')
        }
      }).catch(() => {
        this.$message.error('阅卷提交失败')
      }).finally(() => {
        this.gradingLoading = false
      })
    },
    getExamStatus(row) {
      if (row.finScore === null || row.finScore === undefined) {
        return { text: '待批改', type: 'warning' }
      }
      const total = Number(row.totalScore) || 0
      const score = Number(row.finScore) || 0
      const ratio = total > 0 ? score / total : 0
      if (ratio >= 0.8) return { text: '优秀', type: 'success' }
      if (ratio >= 0.6) return { text: '及格', type: 'primary' }
      return { text: '不及格', type: 'danger' }
    },
    handleDelete(row) {
      this.$confirm(`确认删除考试【${row.name || row.id}】吗？`, '提示', {
        confirmButtonText: '确认',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        this.$http.delete(`/v1/exam/del?id=${row.id}`).then(response => {
          if (response.data.code === 200) {
            this.$message.success('删除成功')
            if (this.examList.length === 1 && this.currentPage > 1) {
              this.currentPage -= 1
            }
            this.fetchList()
          } else {
            this.$message.error(response.data.message || '删除失败')
          }
        }).catch(() => {
          this.$message.error('删除失败')
        })
      }).catch(() => {})
    }
  }
}
</script>

<style scoped>
.exam-container {
  padding: 20px;
}

.search-box,
.table-container {
  background: #fff;
  padding: 20px;
  border-radius: 6px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
}

.search-box {
  margin-bottom: 16px;
}

.pagination-container {
  margin-top: 20px;
  text-align: center;
}

.pending-text {
  color: #e6a23c;
}

.exam-detail-content {
  max-height: 70vh;
  overflow: auto;
}

.summary {
  display: flex;
  flex-wrap: wrap;
  gap: 20px;
  margin-bottom: 16px;
  font-size: 14px;
  color: #606266;
}

.questions-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.question-item {
  border: 1px solid #ebeef5;
  border-radius: 6px;
  padding: 12px;
  background: #fafafa;
}

.question-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.question-top .left {
  display: flex;
  align-items: center;
  gap: 8px;
}

.index {
  font-weight: 700;
}

.question-content {
  line-height: 1.6;
  margin-bottom: 8px;
  color: #303133;
}

.line,
.manual-area {
  margin-top: 6px;
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
}

.label {
  color: #909399;
}

.value {
  color: #303133;
}

.strong {
  font-weight: 700;
}

.muted {
  color: #909399;
  font-size: 12px;
}
</style>
