<template>
  <div class="page-container">
    <div class="search-section">
      <div class="search-header">
        <h2 class="page-title">试题管理</h2>
        <div class="search-controls">
          <div class="search-input-wrapper">
            <el-input v-model="searchForm.keyword" placeholder="搜索题目内容..." clearable @clear="handleSearch"
              @keyup.enter.native="handleSearch" class="modern-search-input">
              <i slot="prefix" class="el-input__icon el-icon-search"></i>
            </el-input>
          </div>
          <el-button type="primary" size="medium" icon="el-icon-plus" @click="openAddDialog" class="add-btn">
            新增试题
          </el-button>
          <el-button type="success" size="medium" icon="el-icon-magic-stick" @click="openAIDialog" class="ai-btn">
            AI出题
          </el-button>
        </div>
      </div>
    </div>

    <div class="table-container">
      <el-table :data="list" v-loading="loading" class="modern-table" row-key="id" stripe>
        <el-table-column type="index" label="#" width="60" align="center"></el-table-column>
        <el-table-column prop="question" label="题目内容" min-width="180" show-overflow-tooltip>
          <template slot-scope="scope">
            <div class="question-content">{{ scope.row.question }}</div>
          </template>
        </el-table-column>
        <el-table-column prop="category" label="题型" width="120" align="center">
          <template slot-scope="scope">
            <el-tag :type="getCategoryType(scope.row.category)" size="small" effect="plain">
              {{ scope.row.category }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="type" label="难度标签" width="120" align="center">
          <template slot-scope="scope">
            <el-tag :type="getTypeTagType(scope.row.type)" size="small" effect="plain">
              {{ scope.row.type || '未设置' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="knowledgePoint" label="知识点标签" min-width="130" align="center" show-overflow-tooltip />
        <el-table-column prop="choice" label="选项" min-width="300" align="center">
          <template slot-scope="scope">
            <div v-if="scope.row.category === '单选题' || scope.row.category === '多选题'" class="choice-container">
              <div v-for="(item, index) in scope.row.choice" :key="index" class="choice-item">
                {{ item }}
              </div>
            </div>
            <span v-else class="no-choice">-</span>
          </template>
        </el-table-column>
        <el-table-column prop="answer" label="正确答案" width="120" align="center">
          <template slot-scope="scope">
            <div class="answer-tag">{{ scope.row.answer }}</div>
          </template>
        </el-table-column>
        <el-table-column prop="parse" label="解析" width="300" align="center">
          <template slot-scope="scope">
            <div>{{ scope.row.parse }}</div>
          </template>
        </el-table-column>
        <el-table-column prop="score" label="分值" width="80" align="center">
          <template slot-scope="scope">
            <span class="score-badge">{{ scope.row.score }}分</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180" align="center">
          <template slot-scope="scope">
            <div class="action-buttons">
              <el-button type="text" size="small" icon="el-icon-edit" @click="openEditDialog(scope.row)"
                class="edit-btn">
                编辑
              </el-button>
              <el-button type="text" size="small" icon="el-icon-delete" @click="handleDelete(scope.row)"
                class="delete-btn">
                删除
              </el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <div class="pagination-wrapper">
      <div class="pagination-info">
        <span>共 {{ total }} 条记录</span>
      </div>
      <el-pagination @size-change="handleSizeChange" @current-change="handleCurrentChange" :current-page="currentPage"
        :page-sizes="[8, 16, 24, 32]" :page-size="pageSize" layout="sizes, prev, pager, next, jumper" :total="total"
        background class="modern-pagination">
      </el-pagination>
    </div>

    <el-dialog title="AI智能出题" :visible.sync="aiDialogVisible" width="500px" class="ai-dialog">
      <div class="ai-dialog-content">
        <div class="ai-tips">
          <i class="el-icon-info"></i>
          <span>请描述您想要的题目要求，AI将为您生成相应的试题</span>
        </div>
        <el-form :model="aiForm" ref="aiFormRef" label-width="80px">
          <el-form-item label="题目要求" prop="requirement">
            <el-input v-model="aiForm.requirement" type="textarea" :rows="4"
              placeholder="例如：生成一道关于JavaScript闭包的单选题，难度中等" maxlength="500" show-word-limit>
            </el-input>
          </el-form-item>
        </el-form>
      </div>
      <span slot="footer" class="dialog-footer">
        <el-button @click="aiDialogVisible = false">取 消</el-button>
        <el-button type="success" :loading="aiGenerating" @click="generateQuestion">
          {{ aiGenerating ? '生成中...' : '开始生成' }}
        </el-button>
      </span>
    </el-dialog>

    <el-dialog :title="dialogMode === 'add' ? '新增试题' : '编辑试题'" :visible.sync="dialogVisible" width="600px"
      class="question-dialog">
      <div class="dialog-content">
        <el-form :model="form" :rules="rules" ref="formRef" label-width="90px" class="question-form">
          <el-form-item label="题目内容" prop="question">
            <el-input v-model="form.question" type="textarea" :rows="3" placeholder="请输入题目内容"></el-input>
          </el-form-item>
          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="题型" prop="category">
                <el-select v-model="form.category" placeholder="请选择题型" @change="handleCategoryChange" style="width: 100%;">
                  <el-option label="单选题" value="单选题" />
                  <el-option label="多选题" value="多选题" />
                  <el-option label="判断题" value="判断题" />
                  <el-option label="简答题" value="简答题" />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="难度标签" prop="type">
                <el-select v-model="form.type" placeholder="请选择难度标签" style="width: 100%;">
                  <el-option label="简单" value="简单" />
                  <el-option label="中等" value="中等" />
                  <el-option label="困难" value="困难" />
                </el-select>
              </el-form-item>
            </el-col>
          </el-row>
          <el-form-item label="知识点标签" prop="knowledgePoint">
            <el-input v-model="form.knowledgePoint" placeholder="请输入知识点标签"></el-input>
          </el-form-item>
          <el-form-item label="选项" prop="choice" v-if="form.category === '单选题' || form.category === '多选题'">
            <div class="choice-input-container">
              <div v-for="(item, index) in form.choice" :key="index" class="choice-input-item">
                <el-input v-model="form.choice[index]" :placeholder="`选项${String.fromCharCode(65 + index)}`">
                  <template slot="prepend">{{ String.fromCharCode(65 + index) }}.</template>
                </el-input>
                <el-button type="text" icon="el-icon-delete" @click="removeChoice(index)" v-if="form.choice.length > 2"
                  class="delete-btn"></el-button>
              </div>
              <el-button type="text" icon="el-icon-plus" @click="addChoice" v-if="form.choice.length < 6"
                class="add-choice-btn">添加选项</el-button>
            </div>
          </el-form-item>
          <el-form-item label="正确答案" prop="answer">
            <el-input v-model="form.answer" :placeholder="getAnswerPlaceholder()"></el-input>
          </el-form-item>
          <el-form-item label="解析" prop="parse">
            <el-input v-model="form.parse" type="textarea" :rows="3" placeholder="请输入答案解析"></el-input>
          </el-form-item>
          <el-form-item label="分值" prop="score">
            <el-input-number v-model="form.score" :min="1" :max="100" :step="1" placeholder="请输入分值"></el-input-number>
          </el-form-item>
        </el-form>
      </div>
      <span slot="footer" class="dialog-footer">
        <el-button @click="dialogVisible = false">取 消</el-button>
        <el-button type="primary" @click="submitForm">保 存</el-button>
      </span>
    </el-dialog>
  </div>
</template>

<script>
export default {
  name: 'Question',
  data() {
    return {
      loading: false,
      list: [],
      total: 0,
      currentPage: 1,
      pageSize: 8,
      searchForm: { keyword: '' },
      dialogVisible: false,
      dialogMode: 'add',
      form: {
        id: '',
        question: '',
        choice: ['', '', '', ''],
        answer: '',
        parse: '',
        category: '单选题',
        type: '',
        knowledgePoint: '',
        score: 5
      },
      rules: {
        question: [{ required: true, message: '请输入题目内容', trigger: 'blur' }],
        category: [{ required: true, message: '请选择题型', trigger: 'change' }],
        type: [{ required: true, message: '请选择难度标签', trigger: 'change' }],
        knowledgePoint: [{ required: true, message: '请输入知识点标签', trigger: 'blur' }],
        answer: [{ required: true, message: '请输入正确答案', trigger: 'blur' }],
        score: [{ required: true, message: '请输入分值', trigger: 'blur' }]
      },
      // AI出题相关
      aiDialogVisible: false,
      aiGenerating: false,
      aiForm: {
        requirement: ''
      }
    }
  },
  created() {
    this.fetchList()
  },
  methods: {
    fetchList() {
      this.loading = true
      this.$http.post('/v1/question/list', {
        currentPage: this.currentPage,
        pageSize: this.pageSize,
        keyword: this.searchForm.keyword
      }).then(res => {
        const data = res.data?.data || { total: 0, data: [] }
        this.list = data.data || []
        this.total = data.total || 0
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
      this.dialogMode = 'add'
      this.resetForm()
      this.dialogVisible = true
      this.$nextTick(() => this.$refs.formRef && this.$refs.formRef.clearValidate())
    },
    openEditDialog(row) {
      this.dialogMode = 'edit'
      this.form = {
        id: row.id,
        question: row.question,
        choice: row.choice || ['', '', '', ''],
        answer: row.answer,
        parse: row.parse || '',
        category: row.category,
        type: row.type || '',
        knowledgePoint: row.knowledgePoint || '',
        score: row.score || 5
      }
      this.dialogVisible = true
      this.$nextTick(() => this.$refs.formRef && this.$refs.formRef.clearValidate())
    },
    submitForm() {
      this.$refs.formRef.validate(valid => {
        if (!valid) return
        const payload = { ...this.form }
        // 非选择题时清空choice数组
        if (payload.category !== '单选题' && payload.category !== '多选题') {
          payload.choice = []
        }
        const url = this.dialogMode === 'add' ? '/v1/question/add' : '/v1/question/update'
        this.$http.post(url, payload).then(() => {
          this.$message.success('保存成功')
          this.dialogVisible = false
          this.fetchList()
        }).catch(() => {
          this.$message.error('保存失败，请稍后重试')
        })
      })
    },
    handleDelete(row) {
      this.$confirm('确定删除该试题吗？', '提示', { type: 'warning' }).then(() => {
        this.$http.delete(`/v1/question/del?id=${row.id}`).then(() => {
          this.$message.success('删除成功')
          this.fetchList()
        }).catch(() => {
          this.$message.error('删除失败，请稍后重试')
        })
      }).catch(() => { })
    },
    resetForm() {
      this.form = {
        id: '',
        question: '',
        choice: ['', '', '', ''],
        answer: '',
        parse: '',
        category: '单选题',
        type: '',
        knowledgePoint: '',
        score: 5
      }
    },
    getCategoryType(category) {
      const typeMap = {
        '单选题': '',
        '多选题': 'success',
        '判断题': 'warning',
        '简答题': 'info'
      }
      return typeMap[category] || ''
    },
    getTypeTagType(type) {
      const typeMap = {
        '简单': 'success',
        '中等': 'warning',
        '困难': 'danger'
      }
      return typeMap[type] || 'info'
    },
    handleCategoryChange(value) {
      // 切换题型时重置选项
      if (value === '单选题' || value === '多选题') {
        this.form.choice = ['', '', '', '']
      } else {
        this.form.choice = []
      }
      // 清空答案
      this.form.answer = ''
    },
    getAnswerPlaceholder() {
      const placeholderMap = {
        '单选题': '请输入正确选项，如：A',
        '多选题': '请输入正确选项，如：AB',
        '判断题': '请输入正确答案，如：正确 或 错误',
        '简答题': '请输入参考答案'
      }
      return placeholderMap[this.form.category] || '请输入正确答案'
    },
    addChoice() {
      if (this.form.choice.length < 6) {
        this.form.choice.push('')
      }
    },
    removeChoice(index) {
      if (this.form.choice.length > 2) {
        this.form.choice.splice(index, 1)
      }
    },
    // AI出题相关方法
    openAIDialog() {
      this.aiForm.requirement = ''
      this.aiDialogVisible = true
    },
    generateQuestion() {
      if (!this.aiForm.requirement.trim()) {
        this.$message.warning('请输入题目要求')
        return
      }

      this.aiGenerating = true
      this.$http.get('/v1/question/ai', {
        params: { param: this.aiForm.requirement }
      }).then(res => {
        if (res.data?.code === 200 && res.data?.data) {
          const aiData = res.data.data
          // 关闭AI弹窗
          this.aiDialogVisible = false
          this.dialogMode = 'add'
          this.form = {
            id: '',
            question: aiData.question || '',
            choice: aiData.choice || ['', '', '', ''],
            answer: aiData.answer || '',
            parse: aiData.parse || '',
            category: aiData.category || '单选题',
            type: aiData.type || '',
            knowledgePoint: aiData.knowledgePoint || '',
            score: aiData.score || 5
          }
          this.dialogVisible = true
          this.$nextTick(() => {
            this.$refs.formRef && this.$refs.formRef.clearValidate()
            this.$message.success('AI题目生成成功，请检查并保存')
          })
        } else {
          this.$message.error(res.data?.message || 'AI生成失败')
        }
      }).catch(error => {
        console.error('AI生成题目失败:', error)
        this.$message.error('AI生成失败，请稍后重试')
      }).finally(() => {
        this.aiGenerating = false
      })
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

/* 搜索栏样式 */
.search-section {
  background: #fff;
  border-radius: 12px;
  padding: 20px;
  margin-bottom: 20px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.05);
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
  gap: 16px;
}

.search-input-wrapper {
  position: relative;
}

.modern-search-input {
  width: 300px;
}

.modern-search-input:deep(.el-input__inner) {
  height: 40px;
  border-radius: 20px;
  padding-left: 40px;
  background-color: #f5f7fa;
  border: 1px solid #e4e7ed;
  transition: all 0.3s;
}

.modern-search-input:deep(.el-input__inner:focus) {
  background-color: #fff;
  border-color: #409EFF;
  box-shadow: 0 0 0 2px rgba(64, 158, 255, 0.2);
}

.modern-search-input:deep(.el-input__prefix) {
  left: 12px;
}

.add-btn {
  height: 40px;
  border-radius: 20px;
  padding: 0 20px;
  font-weight: 500;
  box-shadow: 0 2px 8px rgba(64, 158, 255, 0.3);
}

.ai-btn {
  height: 40px;
  border-radius: 20px;
  padding: 0 20px;
  font-weight: 500;
  box-shadow: 0 2px 8px rgba(103, 194, 58, 0.3);
  background: linear-gradient(135deg, #67c23a, #85ce61);
  border: none;
}

.ai-btn:hover {
  background: linear-gradient(135deg, #85ce61, #67c23a);
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(103, 194, 58, 0.4);
}

/* 表格样式 */
.table-container {
  background: #fff;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.05);
  margin-bottom: 20px;
}

.modern-table {
  width: 100%;
  border: none;
}

.modern-table:deep(.el-table__header-wrapper) {
  background-color: #fafafa;
}

.modern-table:deep(.el-table__header) th {
  background-color: #fafafa;
  color: #606266;
  font-weight: 500;
  border-bottom: 1px solid #ebeef5;
}

.modern-table:deep(.el-table__body) tr {
  transition: background-color 0.2s;
}

.modern-table:deep(.el-table__body) tr:hover>td {
  background-color: #f5f7fa;
}

.modern-table:deep(.el-table__body) td {
  border-bottom: 1px solid #f0f0f0;
  padding: 12px 0;
}

.question-content {
  line-height: 1.5;
  color: #303133;
}

.choice-container {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.choice-item {
  font-size: 12px;
  color: #606266;
  line-height: 1.4;
  text-align: left;
  padding: 2px 0;
}

.no-choice {
  color: #c0c4cc;
}

.answer-tag {
  display: inline-block;
  padding: 4px 8px;
  background-color: #f0f9ff;
  color: #409EFF;
  border-radius: 4px;
  font-size: 12px;
  font-weight: 500;
}

.score-badge {
  display: inline-block;
  padding: 4px 8px;
  background-color: #f0f9ff;
  color: #409EFF;
  border-radius: 4px;
  font-size: 12px;
  font-weight: 600;
}

.action-buttons {
  display: flex;
  justify-content: center;
  gap: 8px;
}

.edit-btn {
  color: #409EFF;
}

.edit-btn:hover {
  color: #66b1ff;
}

.delete-btn {
  color: #F56C6C;
}

.delete-btn:hover {
  color: #f78989;
}

/* 分页样式 */
.pagination-wrapper {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: #fff;
  padding: 16px 20px;
  border-radius: 12px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.05);
}

.pagination-info {
  color: #606266;
  font-size: 14px;
}

.modern-pagination:deep(.el-pager li) {
  border-radius: 6px;
  margin: 0 2px;
  min-width: 32px;
  height: 32px;
  line-height: 30px;
  font-weight: 500;
}

.modern-pagination:deep(.el-pager li.active) {
  background-color: #409EFF;
  color: #fff;
}

.modern-pagination:deep(.btn-prev),
.modern-pagination:deep(.btn-next) {
  border-radius: 6px;
  margin: 0 2px;
  min-width: 32px;
  height: 32px;
}

.modern-pagination:deep(.el-pagination__sizes) {
  margin-right: 16px;
}

.modern-pagination:deep(.el-select .el-input .el-input__inner) {
  border-radius: 6px;
  height: 32px;
}

.modern-pagination:deep(.el-pagination__jump) {
  margin-left: 16px;
}

.modern-pagination:deep(.el-pagination__jump .el-input .el-input__inner) {
  border-radius: 6px;
  height: 32px;
}

/* AI弹窗样式 */
.ai-dialog {
  border-radius: 12px;
}

.ai-dialog-content {
  padding: 10px 0;
}

.ai-tips {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 16px;
  background-color: #f0f9ff;
  border-radius: 8px;
  margin-bottom: 20px;
  color: #409EFF;
  font-size: 14px;
}

.ai-tips i {
  font-size: 16px;
}

/* 弹窗样式 */
.question-dialog {
  border-radius: 12px;
}

.dialog-content {
  padding-bottom: 10px;
}

.question-form :deep(.el-input__inner),
.question-form :deep(.el-textarea__inner) {
  border-radius: 6px;
}

.choice-input-container {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.choice-input-item {
  display: flex;
  align-items: center;
  gap: 10px;
}

.choice-input-item .el-input {
  flex: 1;
}

.delete-btn {
  color: #F56C6C;
  padding: 0 8px;
}

.add-choice-btn {
  color: #409EFF;
  align-self: flex-start;
  margin-top: 5px;
}

:deep(.el-input-number) {
  width: 150px;
}

:deep(.el-input-number .el-input__inner) {
  text-align: left;
}

.dialog-footer .el-button {
  border-radius: 6px;
  padding: 10px 20px;
}
</style>
