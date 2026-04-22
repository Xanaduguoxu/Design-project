<template>
  <div class="medal-container">
    <div class="search-box">
      <el-form :model="searchForm" inline class="search-form">
        <el-form-item label="关键词">
          <el-input v-model="searchForm.keyword" placeholder="请输入关键词" clearable></el-input>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">搜索</el-button>
          <el-button @click="handleReset">重置</el-button>
          <el-button type="success" @click="handleAdd">新增勋章</el-button>
        </el-form-item>
      </el-form>
    </div>

    <div class="table-container">
      <el-table :data="medalList" style="width: 100%" border stripe v-loading="loading">
        <el-table-column prop="name" label="勋章名称" min-width="150" />
        <el-table-column prop="brief" label="简介" min-width="200" />
        <el-table-column prop="icon" label="图标" width="100" align="center">
          <template slot-scope="scope">
            <i :class="getIconClass(scope.row.icon)" class="medal-icon"></i>
            <span>{{ scope.row.icon }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="requirements" label="获得要求" width="120" align="center" />

        <el-table-column label="操作" width="200" align="center">
          <template slot-scope="scope">
            <div class="operation-buttons">
              <el-button size="mini" type="primary" @click="handleEdit(scope.row)">编辑</el-button>
              <el-button size="mini" type="danger" @click="handleDelete(scope.row)">删除</el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-container">
        <el-pagination
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
          :current-page="currentPage"
          :page-sizes="[8, 16, 32, 64]"
          :page-size="pageSize"
          layout="total, sizes, prev, pager, next, jumper"
          :total="total"
          background
          class="custom-pagination"
        />
      </div>
    </div>

    <!-- 新增/编辑勋章弹窗 -->
    <el-dialog 
      :title="dialogMode === 'add' ? '新增勋章' : '编辑勋章'" 
      :visible.sync="dialogVisible" 
      width="600px"
      class="medal-dialog"
      :close-on-click-modal="false"
    >
      <el-form :model="form" :rules="rules" ref="medalForm" label-width="100px" class="medal-form">
        <el-form-item label="勋章名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入勋章名称" />
        </el-form-item>
        
        <el-form-item label="简介" prop="brief">
          <el-input v-model="form.brief" type="textarea" :rows="3" placeholder="请输入勋章简介" />
        </el-form-item>
        
        <el-form-item label="图标" prop="icon">
          <el-select v-model="form.icon" placeholder="请选择图标" style="width: 100%">
            <el-option label="Star" value="Star" />
            <el-option label="Collection" value="Collection" />
            <el-option label="Trophy" value="Trophy" />
            <el-option label="Medal" value="Medal" />
            <el-option label="Award" value="Award" />
            <el-option label="Diamond" value="Diamond" />
            <el-option label="Fire" value="Fire" />
            <el-option label="Heart" value="Heart" />
            <el-option label="Lock" value="Lock" />
            <el-option label="Unlock" value="Unlock" />
          </el-select>
        </el-form-item>
        
        <el-form-item label="获得要求" prop="requirements">
          <el-input-number 
            v-model="form.requirements" 
            :min="0" 
            :max="999999" 
            placeholder="请输入获得要求" 
            style="width: 100%"
          ></el-input-number>
        </el-form-item>
      </el-form>
      <span slot="footer" class="dialog-footer">
        <el-button @click="handleClose">取 消</el-button>
        <el-button type="primary" @click="submitForm" :loading="submitLoading">确 定</el-button>
      </span>
    </el-dialog>
  </div>
</template>

<script>
export default {
  name: 'MedalManagement',
  data() {
    return {
      loading: false,
      submitLoading: false,
      list: [],
      total: 0,
      currentPage: 1,
      pageSize: 8,
      searchForm: { keyword: '' },
      dialogVisible: false,
      dialogMode: 'add',
      form: {
        id: '',
        name: '',
        brief: '',
        icon: 'Trophy',
        requirements: 0
      },
      rules: {
        name: [
          { required: true, message: '请输入勋章名称', trigger: 'blur' },
          { min: 1, max: 50, message: '长度在 1 到 50 个字符', trigger: 'blur' }
        ],
        brief: [
          { required: true, message: '请输入勋章简介', trigger: 'blur' },
          { min: 1, max: 200, message: '长度在 1 到 200 个字符', trigger: 'blur' }
        ],
        icon: [
          { required: true, message: '请选择图标', trigger: 'change' }
        ],
        requirements: [
          { required: true, message: '请输入获得要求', trigger: 'blur' },
          { type: 'number', min: 0, max: 999999, message: '获得要求必须在 0-999999 之间', trigger: 'blur' }
        ]
      }
    }
  },
  computed: {
    medalList() {
      return this.list;
    }
  },
  created() {
    this.fetchList();
  },
  methods: {
    // 获取勋章列表
    fetchList() {
      this.loading = true;
      this.$http.post('/v1/medal/list', {
        currentPage: this.currentPage,
        pageSize: this.pageSize,
        keyword: this.searchForm.keyword
      })
        .then(response => {
          if (response.data.code === 200) {
            const data = response.data.data || { total: 0, data: [] };
            this.list = data.data || [];
            this.total = data.total || 0;
          } else {
            this.$message.error(response.data.message || '获取勋章列表失败');
          }
        })
        .catch(error => {
          this.$message.error('获取勋章列表失败');
          console.error(error);
        })
        .finally(() => {
          this.loading = false;
        });
    },

    // 搜索
    handleSearch() {
      this.currentPage = 1;
      this.fetchList();
    },

    // 重置
    handleReset() {
      this.searchForm.keyword = '';
      this.currentPage = 1;
      this.fetchList();
    },

    // 分页大小改变
    handleSizeChange(size) {
      this.pageSize = size;
      this.currentPage = 1;
      this.fetchList();
    },

    // 页码改变
    handleCurrentChange(page) {
      this.currentPage = page;
      this.fetchList();
    },

    // 新增勋章
    handleAdd() {
      this.dialogMode = 'add';
      this.resetForm();
      this.dialogVisible = true;
      this.$nextTick(() => {
        this.$refs.medalForm?.resetFields();
      });
    },

    // 编辑勋章
    handleEdit(row) {
      this.dialogMode = 'edit';
      this.form = { ...row };
      this.dialogVisible = true;
      this.$nextTick(() => {
        this.$refs.medalForm?.clearValidate();
      });
    },

    // 删除勋章
    handleDelete(row) {
      this.$confirm(`确定要删除勋章「${row.name}」吗？`, '提示', {
        confirmButtonText: '确定删除',
        cancelButtonText: '取消',
        type: 'warning',
        confirmButtonClass: 'el-button--danger',
        center: true
      }).then(() => {
        this.$http.delete(`/v1/medal/del?id=${row.id}`)
          .then(response => {
            if (response.data.code === 200) {
              this.$message.success('删除成功');
              // 如果删除的是当前页的最后一条数据，且不是第一页，则跳转到前一页
              if (this.medalList.length === 1 && this.currentPage > 1) {
                this.currentPage -= 1;
              }
              this.fetchList();
            } else {
              this.$message.error(response.data.message || '删除失败');
            }
          })
          .catch(error => {
            console.error('删除勋章失败:', error);
            this.$message.error('删除失败');
          });
      }).catch(() => {
        this.$message.info('已取消删除');
      });
    },

    // 提交表单
    submitForm() {
      this.$refs.medalForm.validate((valid) => {
        if (!valid) {
          this.$message.error('请完善表单信息');
          return;
        }

        this.submitLoading = true;

        // 根据当前模式确定API端点
        const url = this.dialogMode === 'add' ? '/v1/medal/add' : '/v1/medal/update';
        const payload = { ...this.form };

        this.$http.post(url, payload)
          .then(response => {
            if (response.data.code === 200) {
              this.$message.success(this.dialogMode === 'add' ? '新增成功' : '更新成功');
              this.dialogVisible = false;
              this.fetchList();
            } else {
              this.$message.error(response.data.message || (this.dialogMode === 'add' ? '新增失败' : '更新失败'));
            }
          })
          .catch(error => {
            console.error(this.dialogMode === 'add' ? '新增失败:' : '更新失败:', error);
            this.$message.error(this.dialogMode === 'add' ? '新增失败' : '更新失败');
          })
          .finally(() => {
            this.submitLoading = false;
          });
      });
    },

    // 关闭对话框
    handleClose() {
      this.dialogVisible = false;
      this.$refs.medalForm?.resetFields();
      this.submitLoading = false;
    },

    // 重置表单
    resetForm() {
      this.form = {
        id: '',
        name: '',
        brief: '',
        icon: 'Trophy',
        requirements: 0
      };
    },

    // 获取图标类名
    getIconClass(iconName) {
      const iconMap = {
        'Star': 'el-icon-star-on',
        'Collection': 'el-icon-collection',
        'Trophy': 'el-icon-trophy',
        'Medal': 'el-icon-medal',
        'Award': 'el-icon-aim',
        'Diamond': 'el-icon-present',
        'Fire': 'el-icon-hot-water',
        'Heart': 'el-icon-heart',
        'Lock': 'el-icon-lock',
        'Unlock': 'el-icon-unlock'
      };
      return iconMap[iconName] || 'el-icon-menu';
    }
  }
}
</script>

<style scoped>
.medal-container {
  padding: 20px;
}

.search-box {
  background: #fff;
  padding: 20px;
  margin-bottom: 20px;
  border-radius: 4px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
}

.search-form {
  margin: 0;
}

.search-form .el-form-item {
  margin-bottom: 0;
}

.table-container {
  background: #fff;
  padding: 20px;
  border-radius: 4px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
}

.operation-buttons .el-button {
  margin-right: 5px;
}

.operation-buttons .el-button:last-child {
  margin-right: 0;
}

.pagination-container {
  margin-top: 20px;
  text-align: center;
}

.medal-icon {
  margin-right: 5px;
  font-size: 16px;
  vertical-align: middle;
}

.medal-dialog .el-dialog__body {
  padding-top: 10px;
}

.medal-form {
  padding-right: 20px;
}
</style>