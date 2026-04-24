<template>
  <div class="page-container">
    <div class="search-section">
      <div class="search-header">
        <h2 class="page-title">Paper Management</h2>
        <div class="search-controls">
          <el-input
            v-model.trim="searchForm.keyword"
            placeholder="Search paper name"
            clearable
            class="modern-search-input"
            @clear="handleSearch"
            @keyup.enter.native="handleSearch"
          >
            <i slot="prefix" class="el-input__icon el-icon-search"></i>
          </el-input>
          <el-button type="primary" @click="handleSearch">Search</el-button>
          <el-button
            :type="searchForm.paperSource === PAPER_SOURCE_TEACHER ? 'primary' : 'default'"
            @click="changePaperSource(PAPER_SOURCE_TEACHER)"
          >
            Teacher Papers
          </el-button>
          <el-button
            :type="searchForm.paperSource === PAPER_SOURCE_STUDENT ? 'primary' : 'default'"
            @click="changePaperSource(PAPER_SOURCE_STUDENT)"
          >
            Student Papers
          </el-button>
          <el-button
            type="primary"
            icon="el-icon-plus"
            :disabled="searchForm.paperSource === PAPER_SOURCE_STUDENT"
            @click="openAddDialog"
          >
            New Paper
          </el-button>
        </div>
      </div>
    </div>

    <div class="table-container">
      <el-table :data="list" v-loading="loading" stripe :row-key="getRowKey">
        <el-table-column type="index" label="#" width="60" align="center" />
        <el-table-column prop="name" label="Paper Name" min-width="180" />

        <el-table-column prop="paperSource" label="Source" width="110" align="center">
          <template slot-scope="scope">
            <el-tag size="small" :type="getSourceTagType(scope.row.paperSource)">
              {{ getSourceLabel(scope.row.paperSource) }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column
          v-if="searchForm.paperSource === PAPER_SOURCE_STUDENT"
          prop="createBy"
          label="Owner"
          width="130"
          align="center"
        />

        <el-table-column prop="difficultyTag" label="Difficulty" width="120" align="center">
          <template slot-scope="scope">
            <el-tag size="small" :type="getTypeTagType(scope.row.difficultyTag || scope.row.type)">
              {{ formatDifficulty(scope.row.difficultyTag || scope.row.type) || "-" }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column prop="totalScore" label="Total Score" width="110" align="center" />
        <el-table-column prop="questionCount" label="Questions" width="110" align="center" />

        <el-table-column label="Created At" min-width="180" align="center">
          <template slot-scope="scope">
            {{ formatTime(scope.row.createTime) }}
          </template>
        </el-table-column>

        <el-table-column label="Actions" width="220" align="center">
          <template slot-scope="scope">
            <el-button type="text" @click="openDetailDialog(scope.row)">View / Edit Questions</el-button>
            <el-button type="text" class="danger-text" @click="handleDelete(scope.row)">Delete Paper</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <div class="pagination-wrapper">
      <div class="pagination-info">Total {{ total }}</div>
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

    <el-dialog title="Create Paper" :visible.sync="addDialogVisible" width="760px" :close-on-click-modal="false">
      <el-form ref="addFormRef" :model="addForm" :rules="addRules" label-width="120px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="Paper Name" prop="name">
              <el-input v-model.trim="addForm.name" placeholder="Please input paper name" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="Create Mode" prop="createMode">
              <el-select v-model="addForm.createMode" style="width: 100%">
                <el-option label="Auto" value="auto" />
                <el-option label="Manual" value="manual" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="Difficulty" prop="type">
              <el-select v-model="addForm.type" placeholder="Select difficulty" style="width: 100%">
                <el-option
                  v-for="item in difficultyOptions"
                  :key="item.value"
                  :label="item.label"
                  :value="item.value"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="Total Score" prop="totalScore">
              <el-input-number v-model="addForm.totalScore" :min="1" :max="300" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item v-if="addForm.createMode === 'manual'" label="Questions" prop="questionIds">
          <el-select
            v-model="addForm.questionIds"
            multiple
            filterable
            collapse-tags
            clearable
            style="width: 100%"
            placeholder="Select questions to include"
          >
            <el-option
              v-for="item in questionBank"
              :key="item.id"
              :label="renderQuestionLabel(item)"
              :value="item.id"
            />
          </el-select>
          <div class="helper-text">
            Selected {{ addForm.questionIds.length }} questions. You can continue editing questions after creating the paper.
          </div>
        </el-form-item>
      </el-form>
      <span slot="footer">
        <el-button @click="addDialogVisible = false">Cancel</el-button>
        <el-button type="primary" :loading="submitLoading" @click="submitAddForm">Confirm</el-button>
      </span>
    </el-dialog>

    <el-dialog title="Paper Details" :visible.sync="detailDialogVisible" width="1020px">
      <div class="detail-header" v-if="currentPaper">
        <div>Paper: {{ currentPaper.name }}</div>
        <div>Source: {{ getSourceLabel(currentPaper.paperSource) }}</div>
        <div>Difficulty: {{ formatDifficulty(currentPaper.difficultyTag || currentPaper.type) || "-" }}</div>
        <div>Total: {{ currentPaper.totalScore }}</div>
        <div>Questions: {{ detailList.length }}</div>
      </div>

      <el-table :data="detailList" v-loading="detailLoading" max-height="520">
        <el-table-column type="index" label="#" width="60" align="center" />
        <el-table-column prop="question" label="Question" min-width="260" show-overflow-tooltip />
        <el-table-column prop="category" label="Category" width="120" align="center">
          <template slot-scope="scope">
            {{ formatCategory(scope.row.category) }}
          </template>
        </el-table-column>
        <el-table-column prop="type" label="Difficulty" width="100" align="center">
          <template slot-scope="scope">
            {{ formatDifficulty(scope.row.type) || "-" }}
          </template>
        </el-table-column>
        <el-table-column prop="knowledgePoint" label="Knowledge Point" min-width="150" show-overflow-tooltip />
        <el-table-column prop="score" label="Score" width="80" align="center" />
        <el-table-column prop="answer" label="Answer" min-width="120" show-overflow-tooltip />
        <el-table-column label="Actions" width="130" align="center">
          <template slot-scope="scope">
            <el-button type="text" @click="openEditQuestionDialog(scope.row)">Edit</el-button>
            <el-button type="text" class="danger-text" @click="handleDeleteQuestion(scope.row)">Delete</el-button>
          </template>
        </el-table-column>
      </el-table>
      <span slot="footer">
        <el-button @click="detailDialogVisible = false">Close</el-button>
      </span>
    </el-dialog>

    <el-dialog title="Edit Question" :visible.sync="editDialogVisible" width="760px" :close-on-click-modal="false">
      <el-form ref="editFormRef" :model="editForm" :rules="editRules" label-width="120px">
        <el-form-item label="Question" prop="question">
          <el-input v-model.trim="editForm.question" type="textarea" :rows="3" placeholder="Please input question" />
        </el-form-item>

        <el-row :gutter="16">
          <el-col :span="8">
            <el-form-item label="Category" prop="category">
              <el-select v-model="editForm.category" style="width: 100%" @change="handleEditCategoryChange">
                <el-option
                  v-for="item in categoryOptions"
                  :key="item.value"
                  :label="item.label"
                  :value="item.value"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="Difficulty" prop="type">
              <el-select v-model="editForm.type" style="width: 100%">
                <el-option
                  v-for="item in difficultyOptions"
                  :key="item.value"
                  :label="item.label"
                  :value="item.value"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="Score" prop="score">
              <el-input-number v-model="editForm.score" :min="1" :max="100" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="Knowledge Point" prop="knowledgePoint">
          <el-input v-model.trim="editForm.knowledgePoint" placeholder="Please input knowledge point" />
        </el-form-item>

        <el-form-item v-if="isChoiceCategory(editForm.category)" label="Options" prop="choice">
          <div class="choice-input-container">
            <div v-for="(item, index) in editForm.choice" :key="index" class="choice-input-item">
              <el-input v-model.trim="editForm.choice[index]" :placeholder="`Option ${String.fromCharCode(65 + index)}`">
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
            <el-button type="text" icon="el-icon-plus" @click="addEditChoice" v-if="editForm.choice.length < 6">
              Add Option
            </el-button>
          </div>
        </el-form-item>

        <el-form-item label="Answer" prop="answer">
          <el-input v-model.trim="editForm.answer" placeholder="Please input answer" />
        </el-form-item>
        <el-form-item label="Explanation" prop="parse">
          <el-input v-model.trim="editForm.parse" type="textarea" :rows="2" placeholder="Please input explanation" />
        </el-form-item>
      </el-form>
      <span slot="footer">
        <el-button @click="editDialogVisible = false">Cancel</el-button>
        <el-button type="primary" :loading="editSubmitLoading" @click="submitEditQuestion">Save</el-button>
      </span>
    </el-dialog>
  </div>
</template>

<script>
const PAPER_SOURCE_TEACHER = 'teacher'
const PAPER_SOURCE_STUDENT = 'student'

const CATEGORY_SINGLE = '\u5355\u9009\u9898'
const CATEGORY_MULTI = '\u591a\u9009\u9898'
const CATEGORY_JUDGE = '\u5224\u65ad\u9898'
const CATEGORY_ESSAY = '\u7b80\u7b54\u9898'

const DIFF_EASY = '\u7b80\u5355'
const DIFF_MEDIUM = '\u4e2d\u7b49'
const DIFF_HARD = '\u56f0\u96be'

const CATEGORY_LABEL_MAP = {
  [CATEGORY_SINGLE]: 'Single Choice',
  [CATEGORY_MULTI]: 'Multiple Choice',
  [CATEGORY_JUDGE]: 'True / False',
  [CATEGORY_ESSAY]: 'Essay'
}

const DIFF_LABEL_MAP = {
  [DIFF_EASY]: 'Easy',
  [DIFF_MEDIUM]: 'Medium',
  [DIFF_HARD]: 'Hard'
}

export default {
  name: 'TestPaper',
  data() {
    return {
      PAPER_SOURCE_TEACHER,
      PAPER_SOURCE_STUDENT,
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
        keyword: '',
        paperSource: PAPER_SOURCE_TEACHER
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
      difficultyOptions: [
        { label: 'Easy', value: DIFF_EASY },
        { label: 'Medium', value: DIFF_MEDIUM },
        { label: 'Hard', value: DIFF_HARD }
      ],
      categoryOptions: [
        { label: 'Single Choice', value: CATEGORY_SINGLE },
        { label: 'Multiple Choice', value: CATEGORY_MULTI },
        { label: 'True / False', value: CATEGORY_JUDGE },
        { label: 'Essay', value: CATEGORY_ESSAY }
      ],
      addRules: {
        name: [{ required: true, message: 'Please input paper name', trigger: 'blur' }],
        createMode: [{ required: true, message: 'Please select create mode', trigger: 'change' }],
        type: [{ required: true, message: 'Please select difficulty', trigger: 'change' }],
        totalScore: [{ required: true, message: 'Please input total score', trigger: 'blur' }],
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
            callback(new Error('Manual mode requires at least one question'))
          },
          trigger: 'change'
        }]
      },
      editForm: {
        id: '',
        question: '',
        category: CATEGORY_SINGLE,
        type: DIFF_EASY,
        knowledgePoint: '',
        score: 5,
        choice: ['', '', '', ''],
        answer: '',
        parse: ''
      },
      editRules: {
        question: [{ required: true, message: 'Please input question', trigger: 'blur' }],
        category: [{ required: true, message: 'Please select category', trigger: 'change' }],
        type: [{ required: true, message: 'Please select difficulty', trigger: 'change' }],
        knowledgePoint: [{ required: true, message: 'Please input knowledge point', trigger: 'blur' }],
        answer: [{ required: true, message: 'Please input answer', trigger: 'blur' }],
        score: [{ required: true, message: 'Please input score', trigger: 'blur' }]
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
        keyword: this.searchForm.keyword,
        paperSource: this.searchForm.paperSource
      }).then(res => {
        if (res.data.code === 200) {
          const pageData = res.data.data || {}
          const rows = Array.isArray(pageData.data) ? pageData.data : []
          this.list = rows.map(item => this.normalizePaperRow(item))
          this.total = Number(pageData.total || 0)
        } else {
          this.$message.error(res.data.message || 'Failed to get paper list')
        }
      }).catch(() => {
        this.$message.error('Failed to get paper list')
      }).finally(() => {
        this.loading = false
      })
    },

    normalizePaperRow(row) {
      const source = row.paperSource === PAPER_SOURCE_STUDENT ? PAPER_SOURCE_STUDENT : PAPER_SOURCE_TEACHER
      return {
        ...row,
        paperSource: source,
        difficultyTag: this.normalizeDifficulty(row.difficultyTag || row.type),
        ownerUserId: row.ownerUserId || null
      }
    },

    getRowKey(row) {
      return `${row.name || ''}-${row.paperSource || PAPER_SOURCE_TEACHER}-${row.ownerUserId || 0}-${row.id || ''}`
    },

    changePaperSource(source) {
      this.searchForm.paperSource = source === PAPER_SOURCE_STUDENT ? PAPER_SOURCE_STUDENT : PAPER_SOURCE_TEACHER
      this.currentPage = 1
      this.fetchList()
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
      if (this.searchForm.paperSource === PAPER_SOURCE_STUDENT) {
        this.$message.warning('Student papers are generated by students and cannot be created here.')
        return
      }

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
          this.questionBank = Array.isArray(pageData.data) ? pageData.data : []
        } else {
          this.$message.error(res.data.message || 'Failed to get question bank')
        }
      }).catch(() => {
        this.$message.error('Failed to get question bank')
      })
    },

    renderQuestionLabel(item) {
      const categoryLabel = this.formatCategory(item.category)
      const score = Number(item.score || 0)
      const question = String(item.question || '').replace(/\s+/g, ' ').trim()
      const briefQuestion = question.length > 60 ? `${question.slice(0, 60)}...` : question
      return `[${categoryLabel} ${score}pt] ${briefQuestion}`
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
            this.$message.success('Paper created successfully')
            this.addDialogVisible = false
            this.currentPage = 1
            this.fetchList()
          } else {
            this.$message.error(res.data.message || 'Failed to create paper')
          }
        }).catch(() => {
          this.$message.error('Failed to create paper')
        }).finally(() => {
          this.submitLoading = false
        })
      })
    },

    openDetailDialog(row) {
      this.currentPaper = this.normalizePaperRow(row)
      this.detailDialogVisible = true
      this.detailLoading = true

      const params = {
        name: row.name,
        paperSource: row.paperSource || this.searchForm.paperSource
      }
      if (row.ownerUserId) {
        params.ownerUserId = row.ownerUserId
      }

      this.$http.get('/v1/task/paperDetails', { params }).then(res => {
        if (res.data.code === 200) {
          const details = Array.isArray(res.data.data) ? res.data.data : []
          this.detailList = details.map(item => this.normalizeTask(item))
        } else {
          this.$message.error(res.data.message || 'Failed to get paper details')
        }
      }).catch(() => {
        this.$message.error('Failed to get paper details')
      }).finally(() => {
        this.detailLoading = false
      })
    },

    normalizeTask(task) {
      const category = this.normalizeCategory(task.category)
      const difficulty = this.normalizeDifficulty(task.type || (this.currentPaper ? this.currentPaper.difficultyTag : ''))
      return {
        ...task,
        category,
        type: difficulty,
        choice: this.parseChoice(task.choice),
        knowledgePoint: task.knowledgePoint || ''
      }
    },

    parseChoice(choiceValue) {
      if (Array.isArray(choiceValue)) {
        return choiceValue.map(item => String(item).trim()).filter(Boolean)
      }
      if (typeof choiceValue !== 'string') {
        return []
      }

      const text = choiceValue.trim()
      if (!text) {
        return []
      }

      try {
        const parsed = JSON.parse(text)
        return Array.isArray(parsed) ? parsed.map(item => String(item).trim()).filter(Boolean) : []
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

      const lines = normalized.split(/\r?\n/).map(item => this.cleanupChoiceItem(item)).filter(Boolean)
      if (lines.length > 1) {
        return lines
      }

      const byLabel = normalized
        .split(/(?=[A-H][\.\)]\s*)/i)
        .map(item => this.cleanupChoiceItem(item))
        .filter(Boolean)
      if (byLabel.length > 1) {
        return byLabel
      }

      return normalized
        .split(/\s*[,;，；]\s*/)
        .map(item => this.cleanupChoiceItem(item))
        .filter(Boolean)
    },

    cleanupChoiceItem(text) {
      return String(text || '')
        .replace(/^[,;\s]+/, '')
        .replace(/^[A-H][\.\)]\s*/i, '')
        .trim()
    },

    isChoiceCategory(category) {
      const normalized = this.normalizeCategory(category)
      return normalized === CATEGORY_SINGLE || normalized === CATEGORY_MULTI
    },

    openEditQuestionDialog(row) {
      const task = this.normalizeTask(row)
      const choice = this.isChoiceCategory(task.category) ? task.choice.slice() : []
      const normalizedChoice = choice.length > 0 ? choice : ['', '', '', '']

      this.editForm = {
        id: task.id,
        question: task.question || '',
        category: task.category || CATEGORY_SINGLE,
        type: task.type || (this.currentPaper ? this.currentPaper.difficultyTag : DIFF_EASY),
        knowledgePoint: task.knowledgePoint || '',
        score: Number(task.score || 5),
        choice: this.isChoiceCategory(task.category) ? normalizedChoice : [],
        answer: task.answer || '',
        parse: task.parse || ''
      }

      this.editDialogVisible = true
      this.$nextTick(() => {
        if (this.$refs.editFormRef) {
          this.$refs.editFormRef.clearValidate()
        }
      })
    },

    handleEditCategoryChange(value) {
      const normalized = this.normalizeCategory(value)
      this.editForm.category = normalized
      if (this.isChoiceCategory(normalized)) {
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
          category: this.normalizeCategory(this.editForm.category),
          type: this.normalizeDifficulty(this.editForm.type),
          knowledgePoint: this.editForm.knowledgePoint,
          answer: this.editForm.answer,
          parse: this.editForm.parse,
          score: Number(this.editForm.score),
          totalScore: this.currentPaper ? Number(this.currentPaper.totalScore || 0) : null,
          choice: this.isChoiceCategory(this.editForm.category)
            ? JSON.stringify(this.editForm.choice.map(item => String(item).trim()).filter(Boolean))
            : '',
          paperSource: this.currentPaper ? this.currentPaper.paperSource : undefined,
          ownerUserId: this.currentPaper ? this.currentPaper.ownerUserId : undefined
        }

        this.$http.post('/v1/task/update', payload).then(res => {
          if (res.data.code === 200 && res.data.data !== false) {
            this.$message.success('Question updated successfully')
            this.editDialogVisible = false
            if (this.currentPaper) {
              this.openDetailDialog(this.currentPaper)
            }
            this.fetchList()
          } else {
            this.$message.error(res.data.message || 'Failed to update question')
          }
        }).catch(() => {
          this.$message.error('Failed to update question')
        }).finally(() => {
          this.editSubmitLoading = false
        })
      })
    },

    handleDeleteQuestion(row) {
      this.$confirm('Delete this question?', 'Warning', {
        type: 'warning'
      }).then(() => {
        this.$http.delete('/v1/task/del', {
          params: { id: row.id }
        }).then(res => {
          if (res.data.code === 200 && res.data.data !== false) {
            this.$message.success('Question deleted successfully')
            if (this.currentPaper) {
              this.openDetailDialog(this.currentPaper)
            }
            this.fetchList()
          } else {
            this.$message.error(res.data.message || 'Failed to delete question')
          }
        }).catch(() => {
          this.$message.error('Failed to delete question')
        })
      }).catch(() => {})
    },

    handleDelete(row) {
      this.$confirm(`Delete paper "${row.name}"? All questions in this paper will be removed.`, 'Warning', {
        type: 'warning'
      }).then(() => {
        const params = {
          name: row.name,
          paperSource: row.paperSource || this.searchForm.paperSource
        }
        if (row.ownerUserId) {
          params.ownerUserId = row.ownerUserId
        }

        this.$http.delete('/v1/task/delPaper', { params }).then(res => {
          if (res.data.code === 200 && res.data.data !== false) {
            this.$message.success('Paper deleted successfully')
            if (this.list.length === 1 && this.currentPage > 1) {
              this.currentPage -= 1
            }
            this.fetchList()
          } else {
            this.$message.error(res.data.message || 'Failed to delete paper')
          }
        }).catch(() => {
          this.$message.error('Failed to delete paper')
        })
      }).catch(() => {})
    },

    normalizeCategory(value) {
      if (!value) {
        return CATEGORY_ESSAY
      }
      const raw = String(value).trim()
      const lower = raw.toLowerCase()

      if (raw === CATEGORY_SINGLE || lower === 'single') return CATEGORY_SINGLE
      if (raw === CATEGORY_MULTI || lower === 'multiple') return CATEGORY_MULTI
      if (raw === CATEGORY_JUDGE || lower === 'judge') return CATEGORY_JUDGE
      if (raw === CATEGORY_ESSAY || lower === 'essay') return CATEGORY_ESSAY
      if (raw === '\u5355\u9009') return CATEGORY_SINGLE
      if (raw === '\u591a\u9009') return CATEGORY_MULTI
      if (raw === '\u5224\u65ad') return CATEGORY_JUDGE
      if (raw === '\u7b80\u7b54') return CATEGORY_ESSAY
      return raw
    },

    normalizeDifficulty(value) {
      if (!value) {
        return ''
      }
      const raw = String(value).trim()
      const lower = raw.toLowerCase()

      if (raw === DIFF_EASY || lower === 'easy') return DIFF_EASY
      if (raw === DIFF_MEDIUM || lower === 'medium') return DIFF_MEDIUM
      if (raw === DIFF_HARD || lower === 'hard' || lower === 'difficult') return DIFF_HARD
      return raw
    },

    formatCategory(value) {
      const normalized = this.normalizeCategory(value)
      return CATEGORY_LABEL_MAP[normalized] || normalized || '-'
    },

    formatDifficulty(value) {
      const normalized = this.normalizeDifficulty(value)
      return DIFF_LABEL_MAP[normalized] || normalized || '-'
    },

    getSourceLabel(source) {
      return source === PAPER_SOURCE_STUDENT ? 'Student' : 'Teacher'
    },

    getSourceTagType(source) {
      return source === PAPER_SOURCE_STUDENT ? 'warning' : 'success'
    },

    getTypeTagType(type) {
      const normalized = this.normalizeDifficulty(type)
      const map = {
        [DIFF_EASY]: 'success',
        [DIFF_MEDIUM]: 'warning',
        [DIFF_HARD]: 'danger'
      }
      return map[normalized] || 'info'
    },

    formatTime(timeValue) {
      if (!timeValue) {
        return '-'
      }

      if (typeof timeValue === 'string') {
        const text = timeValue.trim()
        if (!text) {
          return '-'
        }
        if (/^\d+$/.test(text)) {
          const numeric = Number(text)
          if (!Number.isNaN(numeric)) {
            const timestamp = numeric > 9999999999 ? numeric : numeric * 1000
            return new Date(timestamp).toLocaleString()
          }
        }
        const date = new Date(text)
        if (!Number.isNaN(date.getTime())) {
          return date.toLocaleString()
        }
        return text
      }

      if (typeof timeValue === 'number') {
        const timestamp = timeValue > 9999999999 ? timeValue : timeValue * 1000
        return new Date(timestamp).toLocaleString()
      }

      try {
        const date = new Date(timeValue)
        return Number.isNaN(date.getTime()) ? '-' : date.toLocaleString()
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
