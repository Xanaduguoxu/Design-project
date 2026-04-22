<template>
  <div class="task-container">
    <div class="search-box">
      <el-form :model="searchForm" inline class="search-form">
        <el-form-item label="关键词">
          <el-input v-model="searchForm.keyword" placeholder="请输入关键词" clearable></el-input>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">搜索</el-button>
          <el-button @click="handleReset">重置</el-button>
          <el-button type="success" @click="handleAdd">新增任务</el-button>
        </el-form-item>
      </el-form>
    </div>

    <div class="table-container">
      <el-table :data="taskList" style="width: 100%" border stripe v-loading="loading">
        <el-table-column prop="name" label="任务名称" min-width="150" />
        <el-table-column prop="question" label="题目" min-width="200" />
        <el-table-column prop="category" label="题型" width="100" align="center">
          <template slot-scope="scope">
            <el-tag :type="getCategoryType(scope.row.category)">
              {{ scope.row.category }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="answer" label="答案" width="80" align="center" />
        <el-table-column prop="parse" label="解析" min-width="150" />
        <el-table-column prop="score" label="分值" width="80" align="center">
          <template slot-scope="scope">
            <span class="score-badge">{{ scope.row.score }}分</span>
          </template>
        </el-table-column>
        <el-table-column prop="totalScore" label="总分" width="80" align="center">
          <template slot-scope="scope">
            <span class="total-score-badge">{{ scope.row.totalScore }}分</span>
          </template>
        </el-table-column>
        <el-table-column prop="id" label="ID" width="180" />

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

    <!-- 新增/编辑任务弹窗 -->
    <el-dialog 
      :title="dialogMode === 'add' ? '新增任务' : '编辑任务'" 
      :visible.sync="dialogVisible" 
      width="600px"
      class="task-dialog"
      :close-on-click-modal="false"
    >
      <el-form :model="form" :rules="rules" ref="taskForm" label-width="100px" class="task-form">
        
        <!-- 新增模式：仅显示名称和总分 -->
        <template v-if="dialogMode === 'add'">
          <el-form-item label="任务名称" prop="name">
            <el-input v-model="form.name" placeholder="请输入任务名称" />
          </el-form-item>
          <el-form-item label="总分" prop="totalScore">
            <el-input-number v-model="form.totalScore" :min="1" :max="500" placeholder="请输入总分" style="width: 100%"></el-input-number>
          </el-form-item>
        </template>

        <!-- 编辑模式：显示全部字段 -->
        <template v-else>
          <el-form-item label="任务名称" prop="name">
            <el-input v-model="form.name" placeholder="请输入任务名称" />
          </el-form-item>
          <el-form-item label="题目" prop="question">
            <el-input v-model="form.question" type="textarea" :rows="4" placeholder="请输入题目内容" />
          </el-form-item>
          <el-form-item label="题型" prop="category">
            <el-select v-model="form.category" placeholder="请选择题型" style="width: 100%">
              <el-option label="单选题" value="单选题" />
              <el-option label="多选题" value="多选题" />
              <el-option label="判断题" value="判断题" />
              <el-option label="简答题" value="简答题" />
            </el-select>
          </el-form-item>
          <el-form-item 
            label="选项" 
            prop="choice" 
            v-if="form.category === '单选题' || form.category === '多选题'"
          >
            <div class="choice-input-container">
              <div v-for="(item, index) in form.choice" :key="index" class="choice-input-item">
                <el-input v-model="form.choice[index]" :placeholder="`选项${String.fromCharCode(65 + index)}`">
                  <template slot="prepend">{{ String.fromCharCode(65 + index) }}.</template>
                </el-input>
                <el-button 
                  type="text" 
                  icon="el-icon-delete" 
                  @click="removeChoice(index)" 
                  v-if="form.choice.length > 2"
                  class="delete-btn"
                ></el-button>
              </div>
              <el-button 
                type="text" 
                icon="el-icon-plus" 
                @click="addChoice" 
                v-if="form.choice.length < 6"
                class="add-choice-btn"
              >添加选项</el-button>
            </div>
          </el-form-item>
          <el-form-item label="正确答案" prop="answer">
            <el-input v-model="form.answer" placeholder="请输入正确答案" />
          </el-form-item>
          <el-form-item label="解析" prop="parse">
            <el-input v-model="form.parse" type="textarea" :rows="3" placeholder="请输入题目解析" />
          </el-form-item>
          <el-form-item label="分值" prop="score">
            <el-input-number v-model="form.score" :min="1" :max="100" placeholder="请输入分值" style="width: 100%"></el-input-number>
          </el-form-item>
          <el-form-item label="总分" prop="totalScore">
            <el-input-number v-model="form.totalScore" :min="1" :max="500" placeholder="请输入总分" style="width: 100%"></el-input-number>
          </el-form-item>
        </template>

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
  name: 'TaskManagement',
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
        question: '',
        choice: ['', '', '', ''],
        answer: '',
        parse: '',
        category: '单选题',
        score: 5,
        totalScore: 100
      },
      rules: {
        name: [
          { required: true, message: '请输入任务名称', trigger: 'blur' },
          { min: 1, max: 50, message: '长度在 1 到 50 个字符', trigger: 'blur' }
        ],
        question: [
          { required: true, message: '请输入题目内容', trigger: 'blur' }
        ],
        category: [
          { required: true, message: '请选择题型', trigger: 'change' }
        ],
        answer: [
          { required: true, message: '请输入正确答案', trigger: 'blur' }
        ],
        score: [
          { required: true, message: '请输入分值', trigger: 'blur' },
          { type: 'number', min: 1, max: 100, message: '分值必须在 1-100 之间', trigger: 'blur' }
        ],
        totalScore: [
          { required: true, message: '请输入总分', trigger: 'blur' },
          { type: 'number', min: 1, max: 500, message: '总分必须在 1-500 之间', trigger: 'blur' }
        ]
      }
    }
  },
  computed: {
    taskList() {
      return this.list;
    }
  },
  created() {
    this.fetchList();
  },
  methods: {
    // 获取任务列表
    fetchList() {
      this.loading = true;
      this.$http.post('/v1/task/list', {
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
            this.$message.error(response.data.message || '获取任务列表失败');
          }
        })
        .catch(error => {
          this.$message.error('获取任务列表失败');
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

    // 新增任务
    handleAdd() {
      this.dialogMode = 'add';
      this.resetForm();
      this.dialogVisible = true;
      this.$nextTick(() => {
        this.$refs.taskForm?.resetFields();
      });
    },

    // 编辑任务
    handleEdit(row) {
      this.dialogMode = 'edit';
      this.form = { ...row };
      // 如果选项为空，初始化为4个空字符串
      if (!this.form.choice || !Array.isArray(this.form.choice)) {
        this.form.choice = ['', '', '', ''];
      }
      this.dialogVisible = true;
      this.$nextTick(() => {
        this.$refs.taskForm?.clearValidate();
      });
    },

    // 删除任务
    handleDelete(row) {
      this.$confirm(`确定要删除任务「${row.name}」吗？`, '提示', {
        confirmButtonText: '确定删除',
        cancelButtonText: '取消',
        type: 'warning',
        confirmButtonClass: 'el-button--danger',
        center: true
      }).then(() => {
        this.$http.delete(`/v1/task/del?id=${row.id}`)
          .then(response => {
            if (response.data.code === 200) {
              this.$message.success('删除成功');
              // 如果删除的是当前页的最后一条数据，且不是第一页，则跳转到前一页
              if (this.taskList.length === 1 && this.currentPage > 1) {
                this.currentPage -= 1;
              }
              this.fetchList();
            } else {
              this.$message.error(response.data.message || '删除失败');
            }
          })
          .catch(error => {
            console.error('删除任务失败:', error);
            this.$message.error('删除失败');
          });
      }).catch(() => {
        this.$message.info('已取消删除');
      });
    },

    // 提交表单
    submitForm() {
      this.$refs.taskForm.validate((valid) => {
        if (!valid) {
          this.$message.error('请完善表单信息');
          return;
        }

        this.submitLoading = true;

        // 根据当前模式确定API端点
        const url = this.dialogMode === 'add' ? '/v1/task/add' : '/v1/task/update';
        const payload = { ...this.form };

        // 如果不是选择题类型，清空选项数组
        if (payload.category !== '单选题' && payload.category !== '多选题') {
          payload.choice = [];
        }

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
      this.$refs.taskForm?.resetFields();
      this.submitLoading = false;
    },

    // 重置表单
    resetForm() {
      this.form = {
        id: '',
        name: '',
        question: '',
        choice: ['', '', '', ''],
        answer: '',
        parse: '',
        category: '单选题',
        score: 5,
        totalScore: 100
      };
    },

    // 添加选项
    addChoice() {
      if (this.form.choice.length < 6) {
        this.form.choice.push('');
      }
    },

    // 删除选项
    removeChoice(index) {
      if (this.form.choice.length > 2) {
        this.form.choice.splice(index, 1);
      }
    },

    // 获取题型标签类型
    getCategoryType(category) {
      const typeMap = {
        '单选题': '',
        '多选题': 'success',
        '判断题': 'warning',
        '简答题': 'info'
      };
      return typeMap[category] || '';
    }
  }
}
</script>

<style scoped>
.task-container {
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

.choice-input-item {
  display: flex;
  align-items: center;
  margin-bottom: 10px;
}

.choice-input-item .el-input {
  flex: 1;
  margin-right: 10px;
}

.choice-input-item .delete-btn {
  margin-left: 10px;
}

.add-choice-btn {
  color: #409EFF;
}

.score-badge {
  background-color: #ecf5ff;
  color: #409eff;
  padding: 2px 6px;
  border-radius: 4px;
  font-size: 12px;
}

.total-score-badge {
  background-color: #f0f9eb;
  color: #67c23a;
  padding: 2px 6px;
  border-radius: 4px;
  font-size: 12px;
}

.task-dialog .el-dialog__body {
  padding-top: 10px;
}

.task-form {
  padding-right: 20px;
}
</style>
