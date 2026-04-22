<template>
  <div class="page-container">
    <div class="page-header">
      <div class="header-left">
        <h2>账号信息</h2>
      </div>
      <div class="header-right">
        <el-input v-model="searchForm.keyword" placeholder="请输入搜索用户名/昵称/电话" clearable @clear="handleSearch"
          class="search-input">
        </el-input>
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
            {{ scope.row.username?.charAt(0)?.toUpperCase() }}
          </el-avatar>
        </template>
      </el-table-column>
      <el-table-column prop="username" label="用户名" min-width="120" />
      <el-table-column prop="nickname" label="昵称" min-width="100" />
      <el-table-column label="性别" width="80" align="center">
        <template slot-scope="scope">
          <el-tag :type="scope.row.gender === 'male' ? 'primary' : 'danger'" size="small">
            {{ scope.row.gender === 'male' ? '男' : '女' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="email" label="邮箱" min-width="150" />
      <el-table-column prop="birthday" label="出生日期" width="120" />
      <el-table-column prop="role" label="角色" width="100" align="center">
        <template slot-scope="scope">
          <el-tag :type="getRoleTagType(scope.row.role)">
            {{ getRoleLabel(scope.row.role) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="150" align="center" fixed="right">
        <template slot-scope="scope">
          <el-button size="mini" type="primary" @click="handleEdit(scope.row)">编辑</el-button>
          <el-button size="mini" type="danger" @click="handleDelete(scope.row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="pagination-container">
      <el-pagination @size-change="handleSizeChange" @current-change="handleCurrentChange" :current-page="currentPage"
        :page-sizes="[10, 20]" :page-size="pageSize" layout="total, sizes, prev, pager, next, jumper" :total="total"
        background class="custom-pagination" />
    </div>

    <!-- 添加/编辑账号对话框 -->
    <el-dialog :title="isEdit ? '编辑账号信息' : '创建新账号'" :visible.sync="dialogVisible" width="600px"
      :close-on-click-modal="false">
      <el-form :model="registerForm" :rules="rules" ref="registerForm" label-width="80px">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="registerForm.username" :placeholder="isEdit ? '用户名不可修改' : '请输入邮箱作为用户名'"
            :disabled="isEdit" />
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
          <el-date-picker v-model="registerForm.birthday" type="date" placeholder="请选择出生日期" value-format="yyyy-MM-dd"
            style="width: 100%" />
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
export default {
  name: 'Account',
  data() {
    // 邮箱验证规则
    const validateEmail = (rule, value, callback) => {
      if (value && !/^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$/.test(value)) {
        callback(new Error('请输入正确的邮箱格式'))
      } else {
        callback()
      }
    }

    // 出生日期验证规则
    const validateBirthday = (rule, value, callback) => {
      if (value) {
        const selectedDate = new Date(value)
        const today = new Date()
        if (selectedDate > today) {
          callback(new Error('出生日期不能晚于今天'))
        } else {
          callback()
        }
      } else {
        callback()
      }
    }

    // 用户名邮箱格式验证规则
    const validateUsernameEmail = (rule, value, callback) => {
      if (!value) {
        callback(new Error('请输入用户名'))
      } else if (!/^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$/.test(value)) {
        callback(new Error('用户名必须为邮箱格式'))
      } else {
        callback()
      }
    }

    return {
      loading: false,
      accountList: [],
      currentPage: 1,
      pageSize: 10,
      total: 0,
      searchForm: {
        keyword: ''
      },
      dialogVisible: false,
      submitLoading: false,
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
        { value: 'user', label: '用户' },
      ],
      isEdit: false,
      editId: null,
    }
  },
  methods: {
    // 获取账号列表
    fetchAccountList() {
      this.loading = true
      this.$http.post('/v1/auth/list', {
        currentPage: this.currentPage,
        pageSize: this.pageSize,
        keyword: this.searchForm.keyword
      })
        .then(response => {
          if (response.data.code === 200) {
            const { data } = response.data
            this.accountList = data.data
            this.total = data.total
          } else {
            this.$message.error(response.data.message || '获取账号列表失败')
          }
        })
        .catch(error => {
          this.$message.error('获取账号列表失败')
          console.error(error)
        })
        .finally(() => {
          this.loading = false
        })
    },
    // 处理分页大小改变
    handleSizeChange(val) {
      this.pageSize = val
      this.fetchAccountList()
    },
    // 处理页码改变
    handleCurrentChange(val) {
      this.currentPage = val
      this.fetchAccountList()
    },
    // 添加账号
    handleAdd() {
      this.isEdit = false
      this.editId = null
      this.dialogVisible = true
      this.$nextTick(() => {
        this.$refs.registerForm?.resetFields()
        this.resetForm()
      })
    },
    // 编辑账号
    handleEdit(row) {
      this.isEdit = true
      this.editId = row.id
      this.dialogVisible = true
      this.$nextTick(() => {
        this.$refs.registerForm?.resetFields()
        this.fillFormData(row)
      })
    },
    // 重置表单数据
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
    },
    // 填充表单数据
    fillFormData(row) {
      this.registerForm = {
        username: row.username,
        nickname: row.nickname,
        gender: row.gender === '男' ? 'male' : row.gender === '女' ? 'female' : row.gender,
        phone: row.phone,
        email: row.email,
        role: row.role,
        bio: row.bio,
        birthday: row.birthday
      }
    },
    // 删除账号
    handleDelete(row) {
      this.$confirm('此操作将永久删除该账号, 是否继续?', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        this.$http.delete(`/v1/auth/del?id=${row.id}`)
          .then(response => {
            if (response.data.code === 200) {
              this.$message.success('删除成功!')
              // 如果当前页只有一条数据，且不是第一页，则跳转到上一页
              if (this.accountList.length === 1 && this.currentPage > 1) {
                this.currentPage--
              }
              this.fetchAccountList()
            } else {
              this.$message.error(response.data.message || '删除失败')
            }
          })
          .catch(error => {
            this.$message.error(error.response?.data?.message || '删除失败')
          })
      }).catch(() => {
        this.$message.info('已取消删除')
      })
    },
    // 处理搜索
    handleSearch() {
      this.currentPage = 1
      this.fetchAccountList()
    },
    // 关闭对话框
    handleClose() {
      this.dialogVisible = false
      this.$refs.registerForm?.resetFields()
      this.submitLoading = false
      this.isEdit = false
      this.editId = null
    },
    // 提交表单
    submitForm() {
      this.$refs.registerForm.validate((valid) => {
        if (!valid) {
          this.$message.error('请完善表单信息')
          return
        }

        this.submitLoading = true

        const userData = this.isEdit
          ? { ...this.registerForm, id: this.editId }
          : { ...this.registerForm }

        const url = this.isEdit ? '/v1/auth/update' : '/v1/auth/add'

        this.$http.post(url, userData)
          .then(response => {
            if (response.data.code === 200) {
              this.$message.success(this.isEdit ? '修改成功' : '添加账号成功')
              this.handleClose()
              this.fetchAccountList()
            } else {
              this.$message.error(response.data.message || (this.isEdit ? '修改失败' : '添加账号失败'))
            }
          })
          .catch(error => {
            this.$message.error(error.response?.data?.message || (this.isEdit ? '修改失败' : '添加账号失败'))
          })
          .finally(() => {
            this.submitLoading = false
          })
      })
    },
    // 获取角色标签类型
    getRoleTagType(role) {
      const roleTypes = {
        'root': 'danger',
        'admin': 'warning',
        'user': 'success'
      }
      return roleTypes[role] || 'info'
    },
    // 获取角色显示标签
    getRoleLabel(role) {
      const roleOption = this.roleOptions.find(option => option.value === role)
      return roleOption ? roleOption.label : role
    }
  },
  created() {
    this.fetchAccountList()
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
  margin: 0 0 5px 0;
  color: var(--search-title-color);
}

.header-right {
  display: flex;
  align-items: center;
  gap: 10px;
}

.search-input {
  width: 250px;
}

.account-table {
  margin-bottom: 20px;
}

.pagination-container {
  display: flex;
  justify-content: center;
}
</style>