<template>
  <div class="page-container">
    <el-card class="header-card">
      <div class="page-header">
        <div class="title">目标管理</div>
        <div class="actions">
          <el-input v-model="searchForm.keyword" placeholder="按标题/描述搜索" clearable class="action-item"
            @keyup.enter.native="handleSearch" style="width: 220px;" />
          <el-select v-model="searchForm.status" placeholder="状态" clearable class="action-item" style="width: 120px;"
            @change="handleSearch" @clear="handleSearch">
            <el-option v-for="d in dictionaries" :key="d.code" :label="d.value" :value="d.code" />
          </el-select>
          <el-button type="primary" class="action-item" @click="handleSearch">查询</el-button>
          <el-button class="action-item" @click="resetSearch">重置</el-button>
          <el-button type="success" class="action-item" @click="openAddDialog">新增目标</el-button>
        </div>
      </div>
    </el-card>

    <el-card class="table-card">
      <el-table :data="list" stripe border style="width:100%" v-loading="loading">
        <el-table-column prop="title" label="目标标题" min-width="160" />
        <el-table-column prop="description" label="目标描述" min-width="200" />
        <el-table-column prop="startDate" label="开始时间" width="150" />
        <el-table-column prop="endDate" label="结束时间" width="150" />
        <el-table-column prop="targetValue" label="目标值" width="100" />
        <el-table-column prop="currentValue" label="当前值" width="100" />
        <el-table-column label="进度" width="120">
          <template slot-scope="scope">
            <el-progress :percentage="calculateProgress(scope.row)" :stroke-width="20" />
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="110">
          <template slot-scope="scope">
            {{ getStatusLabel(scope.row.status) || '未知' }}
          </template>
        </el-table-column>
        <el-table-column label="操作" fixed="right" width="180">
          <template slot-scope="scope">
            <el-button type="primary" size="mini" @click="openEditDialog(scope.row)">编辑</el-button>
            <el-button type="danger" size="mini" @click="handleDelete(scope.row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-container">
        <el-pagination @size-change="handleSizeChange" @current-change="handleCurrentChange" :current-page="currentPage"
          :page-sizes="[10, 20]" :page-size="pageSize" layout="total, sizes, prev, pager, next, jumper" :total="total"
          background class="custom-pagination" />
      </div>
    </el-card>

    <el-dialog :title="dialogTitle" :visible.sync="dialogVisible" width="700px" :before-close="handleClose"
      :close-on-click-modal="false">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="120px">
        <el-form-item label="目标标题" prop="title">
          <el-input v-model="form.title" />
        </el-form-item>
        <el-form-item label="目标描述" prop="description">
          <el-input type="textarea" v-model="form.description" :autosize="{ minRows: 2, maxRows: 4 }" />
        </el-form-item>
        <el-form-item label="开始时间" prop="startDate">
          <el-date-picker v-model="form.startDate" type="datetime" placeholder="选择开始时间" format="yyyy-MM-dd HH:mm:ss" value-format="yyyy-MM-dd HH:mm:ss" />
        </el-form-item>
        <el-form-item label="结束时间" prop="endDate">
          <el-date-picker v-model="form.endDate" type="datetime" placeholder="选择结束时间" format="yyyy-MM-dd HH:mm:ss" value-format="yyyy-MM-dd HH:mm:ss" />
        </el-form-item>
        <el-form-item label="目标值" prop="targetValue">
          <el-input-number v-model="form.targetValue" :min="1" />
        </el-form-item>
        <el-form-item label="当前值" prop="currentValue">
          <el-input-number v-model="form.currentValue" :min="0" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-select v-model="form.status" placeholder="请选择状态">
            <el-option v-for="d in dictionaries" :key="d.code" :label="d.value" :value="d.code" />
          </el-select>
        </el-form-item>
      </el-form>

      <div slot="footer" class="dialog-footer">
        <el-button @click="handleClose">取 消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="submitForm">确 定</el-button>
      </div>
    </el-dialog>

    
  </div>
</template>

<script>
import { fetchDictionary } from '@/util/dictionary'
export default {
  name: 'Goal',
  data() {
    return {
      searchForm: { keyword: '', status: '' },
      currentPage: 1,
      pageSize: 10,
      total: 0,
      list: [],
      loading: false,
      dialogVisible: false,
      dialogMode: 'add',
      submitLoading: false,

      dictionaries: [],

      form: {
        id: '',
        title: '',
        description: '',
        startDate: '',
        endDate: '',
        targetValue: null,
        currentValue: 0,
        status: 'progress'
      },
      rules: {
        title: [{ required: true, message: '请输入目标标题', trigger: 'blur' }],
        description: [{ required: true, message: '请输入目标描述', trigger: 'blur' }],
        startDate: [{ required: true, message: '请选择开始日期', trigger: 'change' }],
        endDate: [{ required: true, message: '请选择结束日期', trigger: 'change' }],
        targetValue: [
          { required: true, message: '请输入目标值', trigger: 'blur' },
          { pattern: /^\d+$/, message: '目标值必须为数字', trigger: 'blur' }
        ],
        status: [{ required: true, message: '请选择状态', trigger: 'change' }]
      }
    }
  },
  computed: {
    dialogTitle() {
      return this.dialogMode === 'add' ? '新增目标' : '编辑目标'
    }
  },
  created() {
    this.fetchList()
    this.fetchDictionaries()
  },
  methods: {
    fetchList() {
      this.loading = true
      const payload = {
        currentPage: this.currentPage,
        pageSize: this.pageSize,
        keyword: this.searchForm.keyword || '',
        status: this.searchForm.status || undefined
      }
      this.$http.post('/v1/goal/list', payload)
        .then(res => {
          const code = res?.data?.code
          if (code === 200) {
            const data = res?.data?.data || { total: 0, data: [] }
            this.list = Array.isArray(data.data) ? data.data : []
            this.total = Number(data.total || 0)
          } else {
            this.$message.error(res?.data?.message || '获取目标列表失败')
          }
        })
        .catch(err => {
          console.error('fetch goal list error:', err)
          this.$message.error(err?.response?.data?.message || '获取目标列表失败')
        })
        .finally(() => {
          this.loading = false
        })
    },

    handleDelete(row) {
      this.$confirm('此操作将永久删除, 是否继续?', '提示', {
        type: 'warning',
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        closeOnClickModal: false,
        confirmButtonClass: 'el-button--danger'
      }).then(() => {
        this.$http.delete(`/v1/goal/del?id=${row.id}`)
          .then(response => {
            if (response.data.code === 200) {
              this.$message.success('删除成功!')
              if (this.list.length === 1 && this.currentPage > 1) this.currentPage--
              this.fetchList()
            } else {
              this.$message.error(response.data.message || '删除失败')
            }
          })
          .catch(error => {
            console.error('delete goal error:', error)
            this.$message.error(error.response?.data?.message || '删除失败')
          })
      }).catch(() => {
        this.$message.info('已取消删除')
      })
    },

    openAddDialog() {
      this.dialogMode = 'add'
      this.form = {
        id: '',
        title: '',
        description: '',
        startDate: '',
        endDate: '',
        targetValue: null,
        currentValue: 0,
        status: 'progress'
      }
      this.dialogVisible = true
      this.$nextTick(() => this.$refs.formRef && this.$refs.formRef.clearValidate())
    },

    openEditDialog(row) {
      this.dialogMode = 'edit'
      this.form = { ...row }
      this.dialogVisible = true
      this.$nextTick(() => this.$refs.formRef && this.$refs.formRef.clearValidate())
    },

    handleClose() {
      this.dialogVisible = false
      this.submitLoading = false
      this.dialogMode = 'add'
      this.form = {
        id: '',
        title: '',
        description: '',
        startDate: '',
        endDate: '',
        targetValue: null,
        currentValue: 0,
        status: 'progress'
      }
    },

    submitForm() {
      this.$refs.formRef.validate(valid => {
        if (!valid) return
        this.submitLoading = true
        const url = this.dialogMode === 'add' ? '/v1/goal/add' : '/v1/goal/update'
        const payload = { ...this.form }
        this.$http.post(url, payload, { headers: { 'Content-Type': 'application/json' } })
          .then(response => {
            if (response.data.code === 200) {
              this.$message.success(this.dialogMode === 'add' ? '新增成功' : '修改成功')
              this.handleClose()
              if (this.dialogMode === 'add') this.currentPage = 1
              this.fetchList()
            } else {
              this.$message.error(response.data.message || '保存失败')
            }
          })
          .catch(error => {
            console.error('save goal error:', error)
            this.$message.error(error.response?.data?.message || '保存失败')
          })
          .finally(() => {
            this.submitLoading = false
          })
      })
    },

    

    async fetchDictionaries() {
      try {
        const r = await fetchDictionary(this.$http, 'goal_status')
        if (r.code === 200) {
          this.dictionaries = r.list
        } else {
          this.$message.error(r.message || '获取字典失败')
        }
      } catch (err) {
        this.$message.error(err?.response?.data?.message || '获取字典失败')
      }
    },

    

    getStatusLabel(code) {
      const item = this.dictionaries.find(d => d.code === code)
      return item ? item.value : ''
    },

    calculateProgress(row) {
      if (!row.targetValue || row.targetValue <= 0) return 0;
      const percentage = Math.min(100, (row.currentValue / row.targetValue) * 100);
      return Math.round(percentage);
    },

    handleSearch() {
      this.currentPage = 1
      this.fetchList()
    },
    resetSearch() {
      this.searchForm = { keyword: '', status: '' }
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
  }
}
</script>

<style scoped>
.page-container {
  padding: 16px;
  background-color: #f5f7fa;
}

.header-card {
  margin-bottom: -1px;
  border-bottom-left-radius: 0;
  border-bottom-right-radius: 0;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.05);
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 0;
}

.title {
  font-size: 18px;
  font-weight: 600;
  color: var(--search-title-color);
  padding-left: 8px;
}

.actions {
  display: flex;
  align-items: center;
  padding-right: 8px;
}

.action-item {
  margin-left: 8px;
}

.table-card {
  border-top-left-radius: 0;
  border-top-right-radius: 0;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
}

.pagination-container {
  margin-top: 12px;
  text-align: right;
}

.ellipsis {
  display: inline-block;
  max-width: 260px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
</style>