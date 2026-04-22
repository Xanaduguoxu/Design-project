<template>
  <div class="page-container">
    <div class="search-section">
      <div class="search-header">
        <h2 class="page-title">组卷管理</h2>
        <div class="search-controls">
          <div class="search-input-wrapper">
            <el-input v-model="searchForm.keyword" placeholder="搜索试卷名称..." clearable @clear="handleSearch"
              @keyup.enter.native="handleSearch" class="modern-search-input">
              <i slot="prefix" class="el-input__icon el-icon-search"></i>
            </el-input>
          </div>
          <el-button type="primary" size="medium" icon="el-icon-plus" @click="openAddDialog" class="add-btn">
            新增试卷
          </el-button>
        </div>
      </div>
    </div>

    <div class="table-container">
      <el-table :data="list" v-loading="loading" class="modern-table" row-key="id" stripe>
        <el-table-column type="index" label="#" width="60" align="center"></el-table-column>
        
        <el-table-column prop="name" label="试卷名称" min-width="120" align="center">
          <template slot-scope="scope">
            <span class="name-text">{{ scope.row.name }}</span>
          </template>
        </el-table-column>

        <el-table-column prop="type" label="试卷类型" width="100" align="center">
          <template slot-scope="scope">
            <el-tag :type="getTypeTagType(scope.row.type)" size="small" effect="plain">
              {{ scope.row.type || '未分类' }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column prop="totalScore" label="总分" width="80" align="center">
          <template slot-scope="scope">
            <span class="score-badge">{{ scope.row.totalScore }}分</span>
          </template>
        </el-table-column>

        <el-table-column prop="question" label="包含题目内容" min-width="200" show-overflow-tooltip>
          <template slot-scope="scope">
            <div class="question-content">{{ scope.row.question }}</div>
          </template>
        </el-table-column>

        <el-table-column prop="category" label="题目题型" width="100" align="center">
          <template slot-scope="scope">
            <el-tag :type="getCategoryType(scope.row.category)" size="small" effect="plain">
              {{ scope.row.category }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column prop="answer" label="答案" width="80" align="center">
          <template slot-scope="scope">
            <div class="answer-tag">{{ scope.row.answer }}</div>
          </template>
        </el-table-column>

        <el-table-column label="操作" width="180" align="center">
          <template slot-scope="scope">
            <div class="action-buttons">
              <!-- 编辑操作：编辑具体的题目内容 -->
              <el-button type="text" size="small" icon="el-icon-edit" @click="openEditDialog(scope.row)" class="edit-btn">
                编辑题目
              </el-button>
              <el-button type="text" size="small" icon="el-icon-delete" @click="handleDelete(scope.row)" class="delete-btn">
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

    <!-- 新增试卷弹窗 -->
    <el-dialog title="新增试卷" :visible.sync="addDialogVisible" width="500px" class="task-dialog">
      <div class="dialog-content">
        <el-form :model="addForm" :rules="addRules" ref="addFormRef" label-width="90px" class="task-form">
          <el-form-item label="试卷名称" prop="name">
            <el-input v-model="addForm.name" placeholder="请输入试卷名称，如：2023年期末考试"></el-input>
          </el-form-item>
          <el-form-item label="试卷类型" prop="type">
            <el-select v-model="addForm.type" placeholder="请选择试题类型" style="width: 100%;">
              <el-option label="简单" value="简单" />
              <el-option label="困难" value="困难" />
              <el-option label="知识点" value="知识点" />
            </el-select>
          </el-form-item>
          <el-form-item label="试卷总分" prop="totalScore">
            <el-input-number v-model="addForm.totalScore" :min="1" :max="200" :step="5" placeholder="请输入总分" style="width: 100%;"></el-input-number>
          </el-form-item>
        </el-form>
      </div>
      <span slot="footer" class="dialog-footer">
        <el-button @click="addDialogVisible = false">取 消</el-button>
        <el-button type="primary" @click="submitAddForm">确 定</el-button>
      </span>
    </el-dialog>

    <!-- 编辑题目弹窗 (复用试题管理的编辑逻辑) -->
    <el-dialog title="编辑题目详情" :visible.sync="editDialogVisible" width="600px" class="question-dialog">
      <div class="dialog-content">
        <el-form :model="editForm" :rules="editRules" ref="editFormRef" label-width="90px" class="question-form">
          <el-form-item label="题目内容" prop="question">
            <el-input v-model="editForm.question" type="textarea" :rows="3" placeholder="请输入题目内容"></el-input>
          </el-form-item>
          <el-form-item label="题型" prop="category">
            <el-select v-model="editForm.category" placeholder="请选择题型" @change="handleCategoryChange" style="width: 100%;">
              <el-option label="单选题" value="单选题" />
              <el-option label="多选题" value="多选题" />
              <el-option label="判断题" value="判断题" />
              <el-option label="简答题" value="简答题" />
            </el-select>
          </el-form-item>
          <el-form-item label="选项" prop="choice" v-if="editForm.category === '单选题' || editForm.category === '多选题'">
            <div class="choice-input-container">
              <div v-for="(item, index) in editForm.choice" :key="index" class="choice-input-item">
                <el-input v-model="editForm.choice[index]" :placeholder="`选项${String.fromCharCode(65 + index)}`">
                  <template slot="prepend">{{ String.fromCharCode(65 + index) }}.</template>
                </el-input>
                <el-button type="text" icon="el-icon-delete" @click="removeChoice(index)" v-if="editForm.choice.length > 2"
                  class="delete-btn"></el-button>
              </div>
              <el-button type="text" icon="el-icon-plus" @click="addChoice" v-if="editForm.choice.length < 6"
                class="add-choice-btn">添加选项</el-button>
            </div>
          </el-form-item>
          <el-form-item label="正确答案" prop="answer">
            <el-input v-model="editForm.answer" :placeholder="getAnswerPlaceholder()"></el-input>
          </el-form-item>
          <el-form-item label="解析" prop="parse">
            <el-input v-model="editForm.parse" type="textarea" :rows="3" placeholder="请输入答案解析"></el-input>
          </el-form-item>
          <el-form-item label="分值" prop="score">
            <el-input-number v-model="editForm.score" :min="1" :max="100" :step="1" placeholder="请输入分值"></el-input-number>
          </el-form-item>
        </el-form>
      </div>
      <span slot="footer" class="dialog-footer">
        <el-button @click="editDialogVisible = false">取 消</el-button>
        <el-button type="primary" @click="submitEditForm">保 存</el-button>
      </span>
    </el-dialog>
  </div>
</template>

<script>
export default {
  name: 'Task',
  data() {
    return {
      loading: false,
      list: [],
      total: 0,
      currentPage: 1,
      pageSize: 8,
      searchForm: { keyword: '' },
      
      // 新增相关
      addDialogVisible: false,
      addForm: {
        name: '',
        type: '',
        totalScore: 100
      },
      addRules: {
        name: [{ required: true, message: '请输入试卷名称', trigger: 'blur' }],
        type: [{ required: true, message: '请选择试卷类型', trigger: 'change' }],
        totalScore: [{ required: true, message: '请输入试卷总分', trigger: 'blur' }]
      },

      // 编辑相关
      editDialogVisible: false,
      editForm: {
        id: '',
        question: '',
        choice: [],
        answer: '',
        parse: '',
        category: '',
        score: 5
      },
      editRules: {
        question: [{ required: true, message: '请输入题目内容', trigger: 'blur' }],
        category: [{ required: true, message: '请选择题型', trigger: 'change' }],
        answer: [{ required: true, message: '请输入正确答案', trigger: 'blur' }],
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
      this.$http.post('/v1/task/list', {
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

    // --- 新增逻辑 ---
    openAddDialog() {
      this.addForm = { name: '', type: '', totalScore: 100 }
      this.addDialogVisible = true
      this.$nextTick(() => this.$refs.addFormRef && this.$refs.addFormRef.clearValidate())
    },
    submitAddForm() {
      this.$refs.addFormRef.validate(valid => {
        if (!valid) return
        // 仅传递新增接口要求的三个参数
        const payload = {
          name: this.addForm.name,
          type: this.addForm.type,
          totalScore: String(this.addForm.totalScore) // 接口要的是字符串
        }
        this.$http.post('/v1/task/add', payload).then(() => {
          this.$message.success('新增成功')
          this.addDialogVisible = false
          this.fetchList()
        }).catch(() => {
          this.$message.error('新增失败')
        })
      })
    },

    // --- 编辑逻辑 (编辑题目详情) ---
    openEditDialog(row) {
      // 将列表数据填充到编辑表单
      // 注意：row.choice 在列表返回中可能是字符串 "[A. xxx, B. xxx]" 或者数组，这里做兼容处理
      let choiceData = [];
      if (row.choice) {
          if (Array.isArray(row.choice)) {
              choiceData = row.choice;
          } else if (typeof row.choice === 'string') {
              // 如果后端返回的是字符串形式的数组，尝试简单处理或直接展示
              // 这里假设展示即可，如果需要编辑，后端最好返回标准JSON数组
              // 为了简单演示，如果是字符串就分割一下或者置空
              try {
                  const parsed = JSON.parse(row.choice);
                  if (Array.isArray(parsed)) choiceData = parsed;
                  else choiceData = ['', '', '', '']; 
              } catch (e) {
                  // 如果不是JSON，按逗号分割或者给默认
                  choiceData = row.choice.split(',').map(s => s.trim());
              }
          }
      }

      this.editForm = {
        id: row.id,
        question: row.question,
        choice: choiceData.length > 0 ? choiceData : ['', '', '', ''],
        answer: row.answer,
        parse: row.parse,
        category: row.category,
        score: row.score || 5
      }
      this.editDialogVisible = true
      this.$nextTick(() => this.$refs.editFormRef && this.$refs.editFormRef.clearValidate())
    },
    submitEditForm() {
      this.$refs.editFormRef.validate(valid => {
        if (!valid) return
        const payload = { ...this.editForm }
        // 非选择题清空选项
        if (payload.category !== '单选题' && payload.category !== '多选题') {
          payload.choice = []
        }
        this.$http.post('/v1/task/update', payload).then(() => {
          this.$message.success('更新成功')
          this.editDialogVisible = false
          this.fetchList()
        }).catch(() => {
          this.$message.error('更新失败')
        })
      })
    },
    
    // 题目编辑辅助方法
    handleCategoryChange(value) {
      if (value === '单选题' || value === '多选题') {
        this.editForm.choice = ['', '', '', '']
      } else {
        this.editForm.choice = []
      }
      this.editForm.answer = ''
    },
    getAnswerPlaceholder() {
      const map = {
        '单选题': '如：A',
        '多选题': '如：AB',
        '判断题': '如：正确',
        '简答题': '请输入参考答案'
      }
      return map[this.editForm.category] || '请输入答案'
    },
    addChoice() {
      if (this.editForm.choice.length < 6) this.editForm.choice.push('')
    },
    removeChoice(index) {
      if (this.editForm.choice.length > 2) this.editForm.choice.splice(index, 1)
    },

    // --- 删除逻辑 ---
    handleDelete(row) {
      this.$confirm('确定删除该试卷吗？', '提示', { type: 'warning' }).then(() => {
        this.$http.delete(`/v1/task/del?id=${row.id}`).then(() => {
          this.$message.success('删除成功')
          this.fetchList()
        }).catch(() => {
          this.$message.error('删除失败')
        })
      }).catch(() => {})
    },

    // 样式辅助
    getCategoryType(category) {
      const map = { '单选题': '', '多选题': 'success', '判断题': 'warning', '简答题': 'info' }
      return map[category] || ''
    },
    getTypeTagType(type) {
      const map = { '简单': 'success', '困难': 'danger', '知识点': 'primary' }
      return map[type] || 'info'
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

.modern-search-input {
  width: 300px;
}

.modern-search-input:deep(.el-input__inner) {
  height: 40px;
  border-radius: 20px;
  padding-left: 40px;
  background-color: #f5f7fa;
  border: 1px solid #e4e7ed;
}

.add-btn {
  height: 40px;
  border-radius: 20px;
  padding: 0 20px;
  font-weight: 500;
  box-shadow: 0 2px 8px rgba(64, 158, 255, 0.3);
}

/* 表格样式 */
.table-container {
  background: #fff;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.05);
  margin-bottom: 20px;
}

.modern-table:deep(.el-table__header) th {
  background-color: #fafafa;
  color: #606266;
  font-weight: 500;
}

.modern-table:deep(.el-table__body) tr:hover>td {
  background-color: #f5f7fa;
}

.name-text { font-weight: 600; color: #303133; }
.question-content { line-height: 1.5; color: #606266; font-size: 13px; }

.answer-tag {
  display: inline-block;
  padding: 4px 8px;
  background-color: #f0f9ff;
  color: #409EFF;
  border-radius: 4px;
  font-size: 12px;
}

.score-badge {
  display: inline-block;
  padding: 4px 8px;
  background-color: #fff0f6;
  color: #F56C6C;
  border-radius: 4px;
  font-size: 12px;
  font-weight: 600;
}

.action-buttons { display: flex; justify-content: center; gap: 8px; }
.edit-btn { color: #409EFF; }
.delete-btn { color: #F56C6C; }

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

/* 弹窗样式 */
.task-dialog, .question-dialog { border-radius: 12px; }
.dialog-content { padding-bottom: 10px; }
.dialog-footer .el-button { border-radius: 6px; padding: 10px 20px; }

.choice-input-container { display: flex; flex-direction: column; gap: 10px; }
.choice-input-item { display: flex; align-items: center; gap: 10px; }
.choice-input-item .el-input { flex: 1; }
.add-choice-btn { color: #409EFF; align-self: flex-start; margin-top: 5px; }
</style>
