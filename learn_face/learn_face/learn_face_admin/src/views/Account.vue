<template>
  <div class="page-container">
    <div class="page-header">
      <div class="header-left">
        <h2>账户信息</h2>
      </div>
      <div class="header-right">
        <el-input
          v-model="searchForm.keyword"
          placeholder="请输入用户名/昵称进行搜索"
          clearable
          @clear="handleSearch"
          class="search-input"
        />
        <el-button type="primary" @click="handleSearch">
          <i class="el-icon-search"></i>
          <span>搜索</span>
        </el-button>
        <el-button type="primary" @click="handleAdd">
          <i class="el-icon-plus"></i>
          <span>添加账号</span>
        </el-button>
      </div>
    </div>

    <el-table :data="accountList" border v-loading="loading" class="account-table">
      <el-table-column label="头像" width="80" align="center">
        <template slot-scope="scope">
          <el-avatar :size="40" :src="scope.row.avatar || scope.row.photo">
            {{ getUserInitial(scope.row.username) }}
          </el-avatar>
        </template>
      </el-table-column>

      <el-table-column prop="username" label="用户名" min-width="160" />
      <el-table-column prop="nickname" label="昵称" min-width="120" />

      <el-table-column label="性别" width="80" align="center">
        <template slot-scope="scope">
          <el-tag :type="scope.row.gender === 'male' ? 'primary' : 'danger'" size="small">
            {{ scope.row.gender === 'male' ? '男' : scope.row.gender === 'female' ? '女' : '-' }}
          </el-tag>
        </template>
      </el-table-column>

      <el-table-column prop="email" label="邮箱" min-width="180" />
      <el-table-column prop="birthday" label="出生日期" width="130" />

      <el-table-column prop="role" label="角色" width="100" align="center">
        <template slot-scope="scope">
          <el-tag :type="getRoleTagType(scope.row.role)">
            {{ getRoleLabel(scope.row.role) }}
          </el-tag>
        </template>
      </el-table-column>

      <el-table-column label="操作" width="160" align="center" fixed="right">
        <template slot-scope="scope">
          <el-button size="mini" type="primary" @click="handleEdit(scope.row)">编辑</el-button>
          <el-button size="mini" type="danger" @click="handleDelete(scope.row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="pagination-container">
      <el-pagination
        @size-change="handleSizeChange"
        @current-change="handleCurrentChange"
        :current-page="currentPage"
        :page-sizes="[10, 20]"
        :page-size="pageSize"
        layout="total, sizes, prev, pager, next, jumper"
        :total="total"
        background
        class="custom-pagination"
      />
    </div>

    <el-dialog
      :title="isEdit ? '编辑账号信息' : '创建新账号'"
      :visible.sync="dialogVisible"
      width="600px"
      :close-on-click-modal="false"
      @close="handleClose"
    >
      <el-form :model="registerForm" :rules="rules" ref="registerForm" label-width="90px">
        <el-form-item label="用户名" prop="username">
          <el-input
            v-model="registerForm.username"
            :placeholder="isEdit ? '用户名不可修改' : '请输入邮箱作为用户名'"
            :disabled="isEdit"
          />
        </el-form-item>

        <el-form-item label="昵称" prop="nickname">
          <el-input v-model="registerForm.nickname" placeholder="请输入昵称" />
        </el-form-item>

        <el-form-item label="性别" prop="gender">
          <el-select v-model="registerForm.gender" placeholder="请选择性别" style="width: 100%">
            <el-option label="男" value="male" />
            <el-option label="女" value="female" />
          </el-select>
        </el-form-item>

        <el-form-item label="邮箱" prop="email">
          <el-input v-model="registerForm.email" placeholder="请输入邮箱" />
        </el-form-item>

        <el-form-item label="出生日期" prop="birthday">
          <el-date-picker
            v-model="registerForm.birthday"
            type="date"
            placeholder="请选择出生日期"
            value-format="yyyy-MM-dd"
            style="width: 100%"
          />
        </el-form-item>

        <el-form-item label="角色" prop="role">
          <el-select v-model="registerForm.role" placeholder="请选择角色" style="width: 100%">
            <el-option v-for="item in roleOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
      </el-form>

      <div slot="footer">
        <el-button @click="handleClose">取消</el-button>
        <el-button type="primary" @click="submitForm" :loading="submitLoading">
          {{ isEdit ? '保存修改' : '创建账号' }}
        </el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
const EMAIL_REGEX = /^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$/

export default {
  name: 'Account',
  data() {
    const validateEmail = (rule, value, callback) => {
      const email = (value || '').trim()
      if (!email) {
        callback()
        return
      }

      // 编辑模式下如果邮箱没改动，直接放行，避免历史脏数据阻塞“仅改角色”
      if (this.isEdit && email === (this.originalForm.email || '').trim()) {
        callback()
        return
      }

      if (!EMAIL_REGEX.test(email)) {
        callback(new Error('请输入正确的邮箱格式'))
        return
      }

      callback()
    }

    const validateBirthday = (rule, value, callback) => {
      if (!value) {
        callback()
        return
      }

      const selectedDate = new Date(value)
      const today = new Date()
      if (selectedDate > today) {
        callback(new Error('出生日期不能晚于今天'))
        return
      }

      callback()
    }

    const validateUsernameEmail = (rule, value, callback) => {
      const username = (value || '').trim()
      if (!username) {
        callback(new Error('请输入用户名'))
        return
      }

      // 编辑模式用户名不可改，不校验历史格式
      if (this.isEdit) {
        callback()
        return
      }

      if (!EMAIL_REGEX.test(username)) {
        callback(new Error('用户名必须为邮箱格式'))
        return
      }

      callback()
    }

    return {
      loading: false,
      accountList: [],
      currentPage: 1,
      pageSize: 10,
      total: 0,
      dialogVisible: false,
      submitLoading: false,
      isEdit: false,
      editId: null,
      searchForm: {
        keyword: ''
      },
      registerForm: {
        username: '',
        nickname: '',
        gender: '',
        phone: '',
        email: '',
        role: '',
        bio: '',
        birthday: ''
      },
      originalForm: {
        username: '',
        email: ''
      },
      rules: {
        username: [
          { required: true, message: '请输入用户名', trigger: 'blur' },
          { validator: validateUsernameEmail, trigger: 'blur' }
        ],
        nickname: [
          { required: true, message: '请输入昵称', trigger: 'blur' },
          { min: 2, max: 20, message: '长度在 2 到 20 个字符', trigger: 'blur' }
        ],
        email: [
          { validator: validateEmail, trigger: 'blur' }
        ],
        birthday: [
          { validator: validateBirthday, trigger: 'change' }
        ],
        role: [
          { required: true, message: '请选择角色', trigger: 'change' }
        ]
      },
      roleOptions: [
        { value: 'root', label: '超级管理员' },
        { value: 'user', label: '用户' }
      ]
    }
  },
  created() {
    this.fetchAccountList()
  },
  methods: {
    getUserInitial(username) {
      const text = String(username || '').trim()
      return text ? text.charAt(0).toUpperCase() : 'U'
    },
    fetchAccountList() {
      this.loading = true
      this.$http.post('/v1/auth/list', {
        currentPage: this.currentPage,
        pageSize: this.pageSize,
        keyword: this.searchForm.keyword
      }).then(response => {
        if (response.data.code === 200) {
          const pageData = response.data.data || {}
          this.accountList = pageData.data || []
          this.total = pageData.total || 0
        } else {
          this.$message.error(response.data.message || '获取账号列表失败')
        }
      }).catch(error => {
        this.$message.error('获取账号列表失败')
        console.error(error)
      }).finally(() => {
        this.loading = false
      })
    },
    handleSizeChange(val) {
      this.pageSize = val
      this.currentPage = 1
      this.fetchAccountList()
    },
    handleCurrentChange(val) {
      this.currentPage = val
      this.fetchAccountList()
    },
    handleSearch() {
      this.currentPage = 1
      this.fetchAccountList()
    },
    handleAdd() {
      this.isEdit = false
      this.editId = null
      this.dialogVisible = true
      this.$nextTick(() => {
        this.$refs.registerForm && this.$refs.registerForm.resetFields()
        this.resetForm()
      })
    },
    handleEdit(row) {
      this.isEdit = true
      this.editId = row.id
      this.dialogVisible = true
      this.$nextTick(() => {
        this.$refs.registerForm && this.$refs.registerForm.resetFields()
        this.fillFormData(row)
      })
    },
    resetForm() {
      this.registerForm = {
        username: '',
        nickname: '',
        gender: '',
        phone: '',
        email: '',
        role: '',
        bio: '',
        birthday: ''
      }
      this.originalForm = {
        username: '',
        email: ''
      }
    },
    fillFormData(row) {
      this.registerForm = {
        username: row.username || '',
        nickname: row.nickname || '',
        gender: row.gender || '',
        phone: row.phone || '',
        email: row.email || '',
        role: row.role || '',
        bio: row.bio || '',
        birthday: row.birthday || ''
      }
      this.originalForm = {
        username: row.username || '',
        email: row.email || ''
      }
    },
    handleDelete(row) {
      this.$confirm('此操作将永久删除该账号，是否继续？', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        this.$http.delete(`/v1/auth/del?id=${row.id}`).then(response => {
          if (response.data.code === 200) {
            this.$message.success('删除成功')
            if (this.accountList.length === 1 && this.currentPage > 1) {
              this.currentPage -= 1
            }
            this.fetchAccountList()
          } else {
            this.$message.error(response.data.message || '删除失败')
          }
        }).catch(error => {
          this.$message.error(error.response?.data?.message || '删除失败')
        })
      }).catch(() => {
        this.$message.info('已取消删除')
      })
    },
    handleClose() {
      this.dialogVisible = false
      this.submitLoading = false
      this.isEdit = false
      this.editId = null
      this.resetForm()
      this.$nextTick(() => {
        this.$refs.registerForm && this.$refs.registerForm.clearValidate()
      })
    },
    submitForm() {
      this.$refs.registerForm.validate(valid => {
        if (!valid) {
          this.$message.error('请完善表单信息')
          return
        }

        this.submitLoading = true
        const userData = this.isEdit
          ? { ...this.registerForm, id: this.editId }
          : { ...this.registerForm }

        const url = this.isEdit ? '/v1/auth/update' : '/v1/auth/add'

        this.$http.post(url, userData).then(response => {
          if (response.data.code === 200) {
            this.$message.success(this.isEdit ? '修改成功' : '添加账号成功')
            this.handleClose()
            this.fetchAccountList()
          } else {
            this.$message.error(response.data.message || (this.isEdit ? '修改失败' : '添加账号失败'))
          }
        }).catch(error => {
          this.$message.error(error.response?.data?.message || (this.isEdit ? '修改失败' : '添加账号失败'))
        }).finally(() => {
          this.submitLoading = false
        })
      })
    },
    getRoleTagType(role) {
      const roleTypes = {
        root: 'danger',
        admin: 'warning',
        user: 'success'
      }
      return roleTypes[role] || 'info'
    },
    getRoleLabel(role) {
      const roleOption = this.roleOptions.find(option => option.value === role)
      return roleOption ? roleOption.label : role
    }
  }
}
</script>

<style scoped>
.page-container {
  padding: 20px;
  background-color: #f5f7fa;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  padding: 20px;
  background: var(--search-bg-color);
  border-radius: 12px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.05);
  border: 1px solid #ebeef5;
}

.header-left h2 {
  margin: 0;
  color: var(--search-title-color);
}

.header-right {
  display: flex;
  align-items: center;
  gap: 10px;
}

.search-input {
  width: 260px;
}

.account-table {
  margin-bottom: 20px;
}

.pagination-container {
  display: flex;
  justify-content: center;
}
</style>
