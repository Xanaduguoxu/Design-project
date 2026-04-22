<template>
  <div class="exam-container">
    <div class="search-box">
      <el-form :model="searchForm" inline class="search-form">
        <el-form-item label="关键词">
          <el-input v-model="searchForm.keyword" placeholder="请输入关键词" clearable></el-input>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">搜索</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </div>

    <div class="table-container">
      <el-table :data="examList" style="width: 100%" border stripe v-loading="loading">
        <el-table-column prop="id" label="ID" width="180" align="center" />
        <el-table-column prop="createBy" label="答题人" width="180" align="center" />
        <el-table-column prop="name" label="试卷名称" min-width="150" />
        <el-table-column prop="totalScore" label="总分" width="100" align="center">
          <template slot-scope="scope">
            <span class="total-score-badge">{{ scope.row.totalScore }}分</span>
          </template>
        </el-table-column>
        <el-table-column prop="finScore" label="最终得分" width="100" align="center">
          <template slot-scope="scope">
            <span class="final-score-badge">{{ scope.row.finScore || 0 }}分</span>
          </template>
        </el-table-column>

        <el-table-column label="操作" width="160" align="center">
          <template slot-scope="scope">
            <div class="operation-buttons">
              <el-button size="mini" type="primary" @click="handleView(scope.row)">查看</el-button>
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

    <!-- 查看试卷详情弹窗 -->
    <el-dialog 
      title="试卷详情" 
      :visible.sync="detailDialogVisible" 
      width="80%"
      class="detail-dialog"
      :close-on-click-modal="false"
    >
      <div class="exam-detail-content">
        <h3 class="exam-name">{{ currentExam.name }}</h3>
        <p class="exam-total-score">总分: <span class="score-highlight">{{ currentExam.totalScore }}</span> 分</p>
        <p class="exam-final-score">最终得分: <span class="score-highlight">{{ calculatedFinScore }}</span> 分</p>
        
        <div class="questions-list">
          <h4>试题列表:</h4>
          <div 
            v-for="(question, index) in currentExam.answer" 
            :key="question.id" 
            class="question-item"
          >
            <div class="question-header">
              <span class="question-index">{{ index + 1 }}.</span>
              <span class="question-category">
                <el-tag :type="getCategoryType(question.category)">
                  {{ question.category }}
                </el-tag>
              </span>
            </div>
            <div class="question-content">{{ question.question }}</div>
            <div class="question-answer">答案: <span class="answer-highlight">{{ question.answer }}</span></div>
            <div class="question-score">得分: <span class="score-highlight">{{ question.score || 0 }}</span> 分</div>
            <div class="question-result">
              <span class="result-label">评判结果:</span>
              <el-select v-model="question.result" placeholder="请选择结果">
                <el-option label="正确" value="正确"></el-option>
                <el-option label="错误" value="错误"></el-option>
              </el-select>
            </div>
          </div>
        </div>
      </div>
      <span slot="footer" class="dialog-footer">
        <el-button @click="detailDialogVisible = false">取 消</el-button>
        <el-button type="primary" @click="submitGrading" :loading="gradingLoading">提交阅卷</el-button>
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
      searchForm: { keyword: '' },
      detailDialogVisible: false,
      gradingLoading: false,
      currentExam: {}
    }
  },
  created() {
    this.fetchList();
  },
  computed: {
    calculatedFinScore() {
      if (!this.currentExam.answer) {
        return 0;
      }
      
      return this.currentExam.answer.reduce((total, question) => {
        if (question.result === '正确') {
          return total + (question.score || 0);
        }
        return total;
      }, 0);
    }
  },
  methods: {
    // 获取考试列表
    fetchList() {
      this.loading = true;
      this.$http.post('/v1/exam/list', {
        currentPage: this.currentPage,
        pageSize: this.pageSize,
        keyword: this.searchForm.keyword
      })
        .then(response => {
          if (response.data.code === 200) {
            const data = response.data.data || { total: 0, data: [] };
            this.examList = data.data || [];
            this.total = data.total || 0;
          } else {
            this.$message.error(response.data.message || '获取考试列表失败');
          }
        })
        .catch(error => {
          this.$message.error('获取考试列表失败');
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

    // 查看考试详情
    handleView(row) {
      // 深拷贝考试数据
      this.currentExam = JSON.parse(JSON.stringify(row));
      
      // 确保每个问题都有result字段
      this.currentExam.answer = this.currentExam.answer.map(question => {
        return {
          ...question,
          result: question.result || ''
        };
      });
      
      this.detailDialogVisible = true;
    },

    // 删除考试
    handleDelete(row) {
      this.$confirm(`确定要删除考试「${row.name}」吗？`, '提示', {
        confirmButtonText: '确定删除',
        cancelButtonText: '取消',
        type: 'warning',
        confirmButtonClass: 'el-button--danger',
        center: true
      }).then(() => {
        this.$http.delete(`/v1/exam/del?id=${row.id}`)
          .then(response => {
            if (response.data.code === 200) {
              this.$message.success('删除成功');
              // 如果删除的是当前页的最后一条数据，且不是第一页，则跳转到前一页
              if (this.examList.length === 1 && this.currentPage > 1) {
                this.currentPage -= 1;
              }
              this.fetchList();
            } else {
              this.$message.error(response.data.message || '删除失败');
            }
          })
          .catch(error => {
            console.error('删除考试失败:', error);
            this.$message.error('删除失败');
          });
      }).catch(() => {
        this.$message.info('已取消删除');
      });
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
    },
    
    // 提交阅卷结果
    submitGrading() {
      // 验证是否所有题目都有评判结果
      const hasUngraded = this.currentExam.answer.some(question => !question.result);
      if (hasUngraded) {
        this.$message.warning('请为所有题目设置评判结果');
        return;
      }
      
      // 验证最终得分不超过总分
      const calculatedScore = this.calculatedFinScore;
      if (calculatedScore > this.currentExam.totalScore) {
        this.$message.error('最终得分不能超过总分');
        return;
      }
      
      this.gradingLoading = true;
      
      // 准备提交数据
      const payload = {
        id: this.currentExam.id,
        name: this.currentExam.name,
        totalScore: this.currentExam.totalScore,
        finScore: calculatedScore,
        answer: this.currentExam.answer
      };
      
      this.$http.post('/v1/exam/update', payload)
        .then(response => {
          if (response.data.code === 200) {
            this.$message.success('阅卷提交成功');
            this.detailDialogVisible = false;
            this.fetchList();
          } else {
            this.$message.error(response.data.message || '阅卷提交失败');
          }
        })
        .catch(error => {
          console.error('阅卷提交失败:', error);
          this.$message.error('阅卷提交失败');
        })
        .finally(() => {
          this.gradingLoading = false;
        });
    }
  }
}
</script>

<style scoped>
.exam-container {
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

.total-score-badge {
  background-color: #f0f9eb;
  color: #67c23a;
  padding: 2px 6px;
  border-radius: 4px;
  font-size: 12px;
}

.detail-dialog .el-dialog__body {
  padding: 20px;
}

.exam-detail-content .exam-name {
  margin: 0 0 10px 0;
  font-size: 18px;
  font-weight: bold;
  color: #303133;
}

.exam-detail-content .exam-total-score {
  margin: 0 0 10px 0;
  font-size: 16px;
  color: #606266;
}

.exam-detail-content .exam-final-score {
  margin: 0 0 20px 0;
  font-size: 16px;
  color: #606266;
  display: flex;
  align-items: center;
}

.exam-detail-content .score-highlight {
  color: #409EFF;
  font-weight: bold;
}

.questions-list h4 {
  margin: 20px 0 15px 0;
  font-size: 16px;
  color: #303133;
  border-bottom: 1px solid #dcdfe6;
  padding-bottom: 5px;
}

.question-item {
  margin-bottom: 15px;
  padding: 10px;
  border: 1px solid #ebeef5;
  border-radius: 4px;
  background-color: #fafafa;
}

.question-header {
  display: flex;
  align-items: center;
  margin-bottom: 8px;
}

.question-index {
  font-weight: bold;
  margin-right: 10px;
  color: #606266;
}

.question-category {
  margin-left: auto;
}

.question-content {
  margin-bottom: 8px;
  color: #303133;
  line-height: 1.5;
}

.question-answer {
  color: #909399;
  font-style: italic;
}

.answer-highlight {
  color: #409EFF;
  font-weight: bold;
}

.question-score {
  margin-top: 5px;
  color: #67c23a;
  font-weight: bold;
}

.question-result {
  margin-top: 8px;
  display: flex;
  align-items: center;
}

.result-label {
  margin-right: 10px;
  color: #606266;
  font-size: 14px;
}

.question-result .el-select {
  width: 120px;
}
</style>