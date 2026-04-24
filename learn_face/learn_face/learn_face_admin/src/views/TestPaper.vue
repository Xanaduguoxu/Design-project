<template>
  <div class="page-container">
    <div class="search-section">
      <div class="search-header">
        <h2 class="page-title">试卷管理</h2>
        <div class="search-controls">
          <el-input
            v-model="searchForm.keyword"
            placeholder="搜索试卷名称"
            clearable
            class="modern-search-input"
            @clear="handleSearch"
            @keyup.enter.native="handleSearch"
          >
            <i slot="prefix" class="el-input__icon el-icon-search"></i>
          </el-input>
          <el-button type="primary" @click="handleSearch">搜索</el-button>
          <el-button type="primary" icon="el-icon-plus" @click="openAddDialog">新增试卷</el-button>
        </div>
      </div>
    </div>

    <div class="table-container">
      <el-table :data="list" v-loading="loading" stripe row-key="name">
        <el-table-column type="index" label="#" width="60" align="center" />
        <el-table-column prop="name" label="试卷名称" min-width="180" />
        <el-table-column prop="difficultyTag" label="难度标签" width="120" align="center">
          <template slot-scope="scope">
            <el-tag size="small" :type="getTypeTagType(scope.row.difficultyTag || scope.row.type)">
              {{ scope.row.difficultyTag || scope.row.type || '未设置' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="totalScore" label="总分" width="100" align="center" />
        <el-table-column prop="questionCount" label="题目数量" width="110" align="center" />
        <el-table-column label="创建时间" min-width="180" align="center">
          <template slot-scope="scope">
            {{ formatTime(scope.row.createTime) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="220" align="center">
          <template slot-scope="scope">
            <el-button type="text" @click="openDetailDialog(scope.row)">查看/编辑题目</el-button>
            <el-button type="text" class="danger-text" @click="handleDelete(scope.row)">删除试卷</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <div class="pagination-wrapper">
      <div class="pagination-info">共 {{ total }} 条</div>
      <el-pagination
        background
        :current-page="currentPage"
        :page-size="pageSize"
        :page-sizes="[8, 16, 24, 32]"
        :total="total"
        layout="sizes, prev, pager, next, jumper"
        @size-change="handleSizeChange"
        @current-change="handleCurrentChange"
      />
    </div>

    <el-dialog title="新增试卷" :visible.sync="addDialogVisible" width="760px">
      <el-form ref="addFormRef" :model="addForm" :rules="addRules" label-width="110px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="试卷名称" prop="name">
              <el-input v-model="addForm.name" placeholder="请输入试卷名称" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="出卷方式" prop="createMode">
              <el-select v-model="addForm.createMode" style="width: 100%">
                <el-option label="自动出卷" value="auto" />
                <el-option label="手动出卷" value="manual" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="难度标签" prop="type">
              <el-select v-model="addForm.type" placeholder="请选择难度标签" style="width: 100%">
                <el-option label="简单" value="简单" />
                <el-option label="中等" value="中等" />
                <el-option label="困难" value="困难" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="试卷总分" prop="totalScore">
              <el-input-number v-model="addForm.totalScore" :min="1" :max="300" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item v-if="addForm.createMode === 'manual'" label="选择题目" prop="questionIds">
          <el-select
            v-model="addForm.questionIds"
            multiple
            filterable
            collapse-tags
            clearable
            style="width: 100%"
            placeholder="请选择要加入试卷的题目"
          >
            <el-option
              v-for="item in questionBank"
              :key="item.id"
              :label="renderQuestionLabel(item)"
              :value="item.id"
            />
          </el-select>
          <div class="helper-text">已选 {{ addForm.questionIds.length }} 题，保存后可继续在“查看/编辑题目”里修改题面内容。</div>
        </el-form-item>
      </el-form>
      <span slot="footer">
        <el-button @click="addDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="submitAddForm">确定</el-button>
      </span>
    </el-dialog>

    <el-dialog title="试卷题目详情" :visible.sync="detailDialogVisible" width="1020px">
      <div class="detail-header" v-if="currentPaper">
        <div>试卷名称：{{ currentPaper.name }}</div>
        <div>难度标签：{{ currentPaper.difficultyTag || currentPaper.type || '-' }}</div>
        <div>总分：{{ currentPaper.totalScore }}</div>
        <div>题目数：{{ detailList.length }}</div>
      </div>
      <el-table :data="detailList" v-loading="detailLoading" max-height="520">
        <el-table-column type="index" label="#" width="60" align="center" />
        <el-table-column prop="question" label="题目内容" min-width="260" show-overflow-tooltip />
        <el-table-column prop="category" label="题型" width="100" align="center" />
        <el-table-column prop="type" label="难度标签" width="100" align="center" />
        <el-table-column prop="knowledgePoint" label="知识点标签" min-width="130" show-overflow-tooltip />
        <el-table-column prop="score" label="分值" width="80" align="center" />
        <el-table-column prop="answer" label="答案" min-width="120" show-overflow-tooltip />
        <el-table-column label="操作" width="130" align="center">
          <template slot-scope="scope">
            <el-button type="text" @click="openEditQuestionDialog(scope.row)">编辑</el-button>
            <el-button type="text" class="danger-text" @click="handleDeleteQuestion(scope.row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <span slot="footer">
        <el-button @click="detailDialogVisible = false">关闭</el-button>
      </span>
    </el-dialog>

    <el-dialog title="编辑题目" :visible.sync="editDialogVisible" width="760px">
      <el-form ref="editFormRef" :model="editForm" :rules="editRules" label-width="100px">
        <el-form-item label="题目内容" prop="question">
          <el-input v-model="editForm.question" type="textarea" :rows="3" placeholder="请输入题目内容" />
        </el-form-item>
        <el-row :gutter="16">
          <el-col :span="8">
            <el-form-item label="题型" prop="category">
              <el-select v-model="editForm.category" style="width: 100%" @change="handleEditCategoryChange">
                <el-option label="单选题" value="单选题" />
                <el-option label="多选题" value="多选题" />
                <el-option label="判断题" value="判断题" />
                <el-option label="简答题" value="简答题" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="难度标签" prop="type">
              <el-select v-model="editForm.type" style="width: 100%">
                <el-option label="简单" value="简单" />
                <el-option label="中等" value="中等" />
                <el-option label="困难" value="困难" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="分值" prop="score">
              <el-input-number v-model="editForm.score" :min="1" :max="100" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="知识点标签" prop="knowledgePoint">
          <el-input v-model="editForm.knowledgePoint" placeholder="请输入知识点标签" />
        </el-form-item>
        <el-form-item v-if="isChoiceCategory(editForm.category)" label="选项" prop="choice">
          <div class="choice-input-container">
            <div v-for="(item, index) in editForm.choice" :key="index" class="choice-input-item">
              <el-input v-model="editForm.choice[index]" :placeholder="`选项${String.fromCharCode(65 + index)}`">
                <template slot="prepend">{{ String.fromCharCode(65 + index) }}.</template>
              </el-input>
              <el-button
                type="text"
                icon="el-icon-delete"
                class="danger-text"
                @click="removeEditChoice(index)"
                v-if="editForm.choice.length > 2"
              />
            </div>
            <el-button type="text" icon="el-icon-plus" @click="addEditChoice" v-if="editForm.choice.length < 6">添加选项</el-button>
          </div>
        </el-form-item>
        <el-form-item label="标准答案" prop="answer">
          <el-input v-model="editForm.answer" placeholder="请输入标准答案" />
        </el-form-item>
        <el-form-item label="题目解析" prop="parse">
          <el-input v-model="editForm.parse" type="textarea" :rows="2" placeholder="请输入题目解析" />
        </el-form-item>
      </el-form>
      <span slot="footer">
        <el-button @click="editDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="editSubmitLoading" @click="submitEditQuestion">保存</el-button>
      </span>
    </el-dialog>
  </div>
</template>

<script>
export default {
  name: 'TestPaper',
  data() {
    return {
      loading: false,
      detailLoading: false,
      submitLoading: false,
      editSubmitLoading: false,
      list: [],
      detailList: [],
      currentPaper: null,
      total: 0,
      currentPage: 1,
      pageSize: 8,
      searchForm: {
        keyword: ''
      },
      addDialogVisible: false,
      detailDialogVisible: false,
      editDialogVisible: false,
      addForm: {
        name: '',
        createMode: 'auto',
        type: '',
        totalScore: 100,
        questionIds: []
      },
      questionBank: [],
      addRules: {
        name: [{ required: true, message: '请输入试卷名称', trigger: 'blur' }],
        createMode: [{ required: true, message: '请选择出卷方式', trigger: 'change' }],
        type: [{ required: true, message: '请选择难度标签', trigger: 'change' }],
        totalScore: [{ required: true, message: '请输入试卷总分', trigger: 'blur' }],
        questionIds: [{
          validator: (rule, value, callback) => {
            if (this.addForm.createMode !== 'manual') {
              callback()
              return
            }
            if (Array.isArray(value) && value.length > 0) {
              callback()
              return
            }
            callback(new Error('手动出卷请至少选择一道题目'))
          },
          trigger: 'change'
        }]
      },
      editForm: {
        id: '',
        question: '',
        category: '单选题',
        type: '',
        knowledgePoint: '',
        score: 5,
        choice: ['', '', '', ''],
        answer: '',
        parse: ''
      },
      editRules: {
        question: [{ required: true, message: '请输入题目内容', trigger: 'blur' }],
        category: [{ required: true, message: '请选择题型', trigger: 'change' }],
        type: [{ required: true, message: '请选择难度标签', trigger: 'change' }],
        knowledgePoint: [{ required: true, message: '请输入知识点标签', trigger: 'blur' }],
        answer: [{ required: true, message: '请输入标准答案', trigger: 'blur' }],
        score: [{ required: true, message: '请输入分值', trigger: 'blur' }]
      }
    }
  },
  created() {
    this.fetchList()
  },
  methods: {
    fetchList() {
      this.loading = true
      this.$http.post('/v1/task/paperList', {
        currentPage: this.currentPage,
        pageSize: this.pageSize,
        keyword: this.searchForm.keyword
      }).then(res => {
        if (res.data.code === 200) {
          const pageData = res.data.data || {}
          this.list = (pageData.data || []).map(item => ({
            ...item,
            difficultyTag: item.difficultyTag || item.type || ''
          }))
          this.total = pageData.total || 0
        } else {
          this.$message.error(res.data.message || '获取试卷列表失败')
        }
      }).catch(() => {
        this.$message.error('获取试卷列表失败')
      }).finally(() => {
        this.loading = false
      })
    },
    handleSearch() {
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
    openAddDialog() {
      this.addForm = {
        name: '',
        createMode: 'auto',
        type: '',
        totalScore: 100,
        questionIds: []
      }
      this.addDialogVisible = true
      this.fetchQuestionBank()
      this.$nextTick(() => {
        if (this.$refs.addFormRef) {
          this.$refs.addFormRef.clearValidate()
        }
      })
    },
    fetchQuestionBank() {
      this.$http.post('/v1/question/list', {
        currentPage: 1,
        pageSize: 500,
        keyword: ''
      }).then(res => {
        if (res.data.code === 200) {
          const pageData = res.data.data || {}
          this.questionBank = pageData.data || []
        } else {
          this.$message.error(res.data.message || '获取题库失败')
        }
      }).catch(() => {
        this.$message.error('获取题库失败')
      })
    },
    renderQuestionLabel(item) {
      const title = item.question || '-'
      const category = item.category || '-'
      const score = item.score || 0
      return `[${category} ${score}分] ${title}`
    },
    submitAddForm() {
      this.$refs.addFormRef.validate(valid => {
        if (!valid) return
        this.submitLoading = true
        this.$http.post('/v1/task/add', {
          name: this.addForm.name,
          createMode: this.addForm.createMode,
          difficultyTag: this.addForm.type,
          type: this.addForm.type,
          totalScore: Number(this.addForm.totalScore),
          questionIds: this.addForm.questionIds
        }).then(res => {
          if (res.data.code === 200 && res.data.data !== false) {
            this.$message.success('新增试卷成功')
            this.addDialogVisible = false
            this.currentPage = 1
            this.fetchList()
          } else {
            this.$message.error(res.data.message || '新增试卷失败')
          }
        }).catch(() => {
          this.$message.error('新增试卷失败')
        }).finally(() => {
          this.submitLoading = false
        })
      })
    },
    openDetailDialog(row) {
      this.currentPaper = row
      this.detailDialogVisible = true
      this.detailLoading = true
      this.$http.get('/v1/task/paperDetails', {
        params: { name: row.name }
      }).then(res => {
        if (res.data.code === 200) {
          this.detailList = (res.data.data || []).map(item => this.normalizeTask(item))
        } else {
          this.$message.error(res.data.message || '获取试卷详情失败')
        }
      }).catch(() => {
        this.$message.error('获取试卷详情失败')
      }).finally(() => {
        this.detailLoading = false
      })
    },
    normalizeTask(task) {
      return {
        ...task,
        type: task.type || (this.currentPaper ? (this.currentPaper.difficultyTag || this.currentPaper.type) : ''),
        choice: this.parseChoice(task.choice),
        knowledgePoint: task.knowledgePoint || ''
      }
    },
    parseChoice(choiceValue) {
      if (Array.isArray(choiceValue)) return choiceValue
      if (typeof choiceValue !== 'string') return []
      const text = choiceValue.trim()
      if (!text) return []
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
      if (!normalized) return []

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
    isChoiceCategory(category) {
      return category === '单选题' || category === '多选题'
    },
    openEditQuestionDialog(row) {
      this.editForm = {
        id: row.id,
        question: row.question || '',
        category: row.category || '单选题',
        type: row.type || (this.currentPaper ? (this.currentPaper.difficultyTag || this.currentPaper.type) : ''),
        knowledgePoint: row.knowledgePoint || '',
        score: Number(row.score) || 5,
        choice: this.isChoiceCategory(row.category) ? (this.parseChoice(row.choice).length ? this.parseChoice(row.choice) : ['', '', '', '']) : [],
        answer: row.answer || '',
        parse: row.parse || ''
      }
      this.editDialogVisible = true
      this.$nextTick(() => {
        if (this.$refs.editFormRef) {
          this.$refs.editFormRef.clearValidate()
        }
      })
    },
    handleEditCategoryChange(value) {
      if (this.isChoiceCategory(value)) {
        if (!Array.isArray(this.editForm.choice) || this.editForm.choice.length === 0) {
          this.editForm.choice = ['', '', '', '']
        }
      } else {
        this.editForm.choice = []
      }
      this.editForm.answer = ''
    },
    addEditChoice() {
      if (this.editForm.choice.length < 6) {
        this.editForm.choice.push('')
      }
    },
    removeEditChoice(index) {
      if (this.editForm.choice.length > 2) {
        this.editForm.choice.splice(index, 1)
      }
    },
    submitEditQuestion() {
      this.$refs.editFormRef.validate(valid => {
        if (!valid) return
        this.editSubmitLoading = true
        const payload = {
          id: this.editForm.id,
          name: this.currentPaper ? this.currentPaper.name : '',
          question: this.editForm.question,
          category: this.editForm.category,
          type: this.editForm.type,
          knowledgePoint: this.editForm.knowledgePoint,
          answer: this.editForm.answer,
          parse: this.editForm.parse,
          score: Number(this.editForm.score),
          totalScore: this.currentPaper ? this.currentPaper.totalScore : null,
          choice: this.isChoiceCategory(this.editForm.category) ? JSON.stringify(this.editForm.choice.filter(Boolean)) : ''
        }
        this.$http.post('/v1/task/update', payload).then(res => {
          if (res.data.code === 200 && res.data.data !== false) {
            this.$message.success('题目更新成功')
            this.editDialogVisible = false
            this.openDetailDialog(this.currentPaper)
          } else {
            this.$message.error(res.data.message || '题目更新失败')
          }
        }).catch(() => {
          this.$message.error('题目更新失败')
        }).finally(() => {
          this.editSubmitLoading = false
        })
      })
    },
    handleDeleteQuestion(row) {
      this.$confirm('确认删除该题目吗？', '提示', {
        type: 'warning'
      }).then(() => {
        this.$http.delete('/v1/task/del', {
          params: { id: row.id }
        }).then(res => {
          if (res.data.code === 200 && res.data.data !== false) {
            this.$message.success('题目删除成功')
            this.openDetailDialog(this.currentPaper)
            this.fetchList()
          } else {
            this.$message.error(res.data.message || '题目删除失败')
          }
        }).catch(() => {
          this.$message.error('题目删除失败')
        })
      }).catch(() => {})
    },
    handleDelete(row) {
      this.$confirm(`确认删除试卷「${row.name}」吗？该试卷下所有题目将被删除。`, '提示', {
        type: 'warning'
      }).then(() => {
        this.$http.delete('/v1/task/delPaper', {
          params: { name: row.name }
        }).then(res => {
          if (res.data.code === 200) {
            this.$message.success('删除试卷成功')
            if (this.list.length === 1 && this.currentPage > 1) {
              this.currentPage -= 1
            }
            this.fetchList()
          } else {
            this.$message.error(res.data.message || '删除试卷失败')
          }
        }).catch(() => {
          this.$message.error('删除试卷失败')
        })
      }).catch(() => {})
    },
    getTypeTagType(type) {
      const map = {
        '简单': 'success',
        '中等': 'warning',
        '困难': 'danger'
      }
      return map[type] || 'info'
    },
    formatTime(timeValue) {
      if (!timeValue) return '-'
      if (typeof timeValue === 'string') {
        return timeValue
      }
      try {
        return new Date(timeValue).toLocaleString()
      } catch (e) {
        return '-'
      }
    }
  }
}
</script>

<style scoped>
.page-container {
  padding: 20px;
  background-color: #f5f7fa;
  min-height: calc(100vh - 40px);
}

.search-section,
.table-container,
.pagination-wrapper {
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.05);
}

.search-section {
  padding: 20px;
  margin-bottom: 20px;
}

.search-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.page-title {
  margin: 0;
  font-size: 20px;
  font-weight: 600;
  color: #303133;
}

.search-controls {
  display: flex;
  align-items: center;
  gap: 10px;
}

.modern-search-input {
  width: 280px;
}

.table-container {
  padding: 10px 20px 20px;
}

.pagination-wrapper {
  margin-top: 20px;
  padding: 16px 20px;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.pagination-info {
  color: #606266;
  font-size: 14px;
}

.danger-text {
  color: #f56c6c;
}

.detail-header {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
  margin-bottom: 14px;
  color: #606266;
  font-size: 14px;
}

.helper-text {
  margin-top: 8px;
  color: #909399;
  font-size: 12px;
}

.choice-input-container {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.choice-input-item {
  display: flex;
  align-items: center;
  gap: 8px;
}

.choice-input-item .el-input {
  flex: 1;
}
</style>
