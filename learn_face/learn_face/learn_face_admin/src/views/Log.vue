<template>
  <div class="contract-container">
    <div class="contract-header">
      <div class="header-left">
        <h2>日志管理</h2>
      </div>
    </div>

    <div class="search-container">
      <el-form :inline="true" :model="searchForm" class="search-form">
        <el-form-item label="">
          <el-input v-model="searchForm.keyword" placeholder="请输入操作类型/URL" clearable @clear="handleSearch"></el-input>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch" icon="el-icon-search">搜索</el-button>
          <el-button @click="handleReset" icon="el-icon-refresh">重置</el-button>
        </el-form-item>

      </el-form>
    </div>

    <div class="contract-content">
      <el-table :data="logList" style="width: 100%" border stripe v-loading="loading">
        <el-table-column prop="id" label="日志ID" width="200" />
        <el-table-column prop="username" label="用户名" width="200" />
        <el-table-column prop="operation" label="操作类型" width="200" />
        <el-table-column prop="url" label="请求URL" min-width="200" />
        <el-table-column prop="args" label="请求参数" min-width="200">
          <template slot-scope="scope">
            <el-tooltip class="item" effect="dark" :content="scope.row.args" placement="top-start">
              <div class="args-content">{{ scope.row.args }}</div>
            </el-tooltip>
          </template>
        </el-table-column>
        <el-table-column prop="ip" label="IP地址" width="140" />
        <el-table-column prop="createTime" label="创建时间" width="160" />

        <el-table-column label="操作" width="100" fixed="right" align="center">
          <template slot-scope="scope">
            <div class="operation-buttons">
              <el-button size="mini" type="primary" @click="handleViewDetail(scope.row)">查看
              </el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-container">
        <el-pagination 
        @size-change="handleSizeChange" 
        @current-change="handleCurrentChange" 
        :current-page="currentPage"
        :page-sizes="[10, 20]" 
        :page-size="pageSize" layout="total, sizes, prev, pager, next, jumper"
        :total="total" background class="custom-pagination" />
      </div>
    </div>

    <el-dialog :title="'日志详情'" :visible.sync="dialogVisible" width="60%" :close-on-click-modal="false"
      custom-class="log-detail-dialog">
      <el-descriptions :column="1" border>
        <el-descriptions-item label="日志ID">{{ logDetail.id }}</el-descriptions-item>
        <el-descriptions-item label="用户名">{{ logDetail.username }}</el-descriptions-item>
        <el-descriptions-item label="操作类型">{{ logDetail.operation }}</el-descriptions-item>
        <el-descriptions-item label="请求URL">{{ logDetail.url }}</el-descriptions-item>
        <el-descriptions-item label="IP地址">{{ logDetail.ip }}</el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ logDetail.createTime }}</el-descriptions-item>
        <el-descriptions-item label="请求参数">
          <pre class="log-args">{{ formatJson(logDetail.args) }}</pre>
        </el-descriptions-item>
      </el-descriptions>

      <div slot="footer" class="dialog-footer">
        <el-button @click="dialogVisible = false">关 闭</el-button>
      </div>

    </el-dialog>
  </div>
</template>

<script>
export default {
  name: 'LogManagement',
  data() {
    return {
      loading: false,
      currentPage: 1,
      pageSize: 10,
      total: 0,
      dialogVisible: false,
      logDetail: {
        id: '',
        username: '',
        args: '',
        operation: '',
        url: '',
        ip: '',
        createTime: ''
      },
      searchForm: {
        keyword: ''
      },
      logList: []
    }
  },
  mounted() {
    this.getLogList()
  },
  methods: {
    getLogList() {
      this.loading = true
      this.$http.post('/v1/log/list', {
        currentPage: this.currentPage,
        pageSize: this.pageSize,
        keyword: this.searchForm.keyword
      })
        .then(response => {
          if (response.data.code === 200) {
            const { data } = response.data
            this.logList = data.data || []
            this.total = data.total || 0
          } else {
            this.$message.error(response.data.message || '获取日志列表失败')
          }
        })
        .catch(error => {
          this.$message.error('获取日志列表失败')
          console.error(error)
        })
        .finally(() => {
          this.loading = false
        })
    },
    formatJson(jsonString) {
      try {
        const obj = JSON.parse(jsonString);
        return JSON.stringify(obj, null, 2);
      } catch (e) {
        return jsonString;
      }
    },
    handleViewDetail(row) {
      this.dialogVisible = true
      this.logDetail = { ...row }
    },
    handleSizeChange(val) {
      this.pageSize = val;
      this.currentPage = 1;
      this.getLogList();
    },
    handleCurrentChange(val) {
      this.currentPage = val;
      this.getLogList();
    },
    handleSearch() {
      this.currentPage = 1
      this.getLogList()
    },
    handleReset() {
      this.searchForm.keyword = ''
      this.handleSearch()
    },
  },

}
</script>

<style scoped>
.contract-container {
  padding: 20px;
}

.search-container {
  background-color: var(--search-bg-color);
  padding: 20px;
  margin-bottom: 20px;
  border-radius: 4px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
}

.search-form .el-form-item {
  margin-bottom: 0;
  margin-right: 20px;
}

.search-form .el-input,
.search-form .el-select {
  width: 200px;
}

.contract-header {
  margin-bottom: 20px;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header-left {
  display: flex;
  align-items: center;
}

.header-left h2 {
  margin: 0;
  margin-right: 10px;
  color: var(--search-title-color);
}

.contract-content {
  background-color: var(--search-bg-color);
  padding: 20px;
  border-radius: 4px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
}

.operation-buttons {
  display: flex;
  justify-content: center;
  gap: 8px;
}

.operation-buttons .el-button {
  margin: 0;
  padding: 6px 12px;
}

.pagination-container {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
  background-color: var(--search-bg-color);
  padding: 15px 20px;
  border-radius: 4px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
}

.args-content {
  max-width: 200px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.log-args {
  margin: 0;
  white-space: pre-wrap;
  word-break: break-all;
  background-color: #f5f7fa;
  padding: 10px;
  border-radius: 4px;
  max-height: 300px;
  overflow-y: auto;
}

.log-detail-dialog .el-descriptions-item__label {
  width: 120px;
}
</style>
