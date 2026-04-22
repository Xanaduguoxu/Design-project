<template>
  <div class="chat-container">
    <div class="chat-header">
      <div class="header-left">
        <h2>会话记录管理</h2>
        <span class="header-desc">管理用户与AI的对话历史</span>
      </div>
      <div class="header-right">
        <div class="search-container">
          <el-input placeholder="搜索会话主题" v-model="searchForm.keyword" class="search-input" prefix-icon="el-icon-search"
            clearable @clear="handleSearch" @keyup.enter.native="handleSearch">
          </el-input>
          <el-button type="primary" class="search-button" @click="handleSearch">
            <span>搜索</span>
          </el-button>
        </div>
      </div>
    </div>

    <el-table :data="chatList" border style="width: 100%" v-loading="loading" class="custom-table">
      <el-table-column prop="sessionId" label="会话ID" width="180" align="center" show-overflow-tooltip />
      <el-table-column prop="topic" label="会话主题" min-width="200" align="center" show-overflow-tooltip />
      <el-table-column label="操作" width="200" align="center" fixed="right">
        <template slot-scope="scope">
          <div class="action-buttons">
            <el-button size="mini" type="primary" plain @click="handleView(scope.row)">查看详情</el-button>
            <el-button size="mini" type="danger" plain @click="handleDelete(scope.row)">删除</el-button>
          </div>
        </template>
      </el-table-column>
    </el-table>

    <div class="pagination-container">
      <el-pagination @size-change="handleSizeChange" @current-change="handleCurrentChange"
        :current-page="pagination.currentPage" :page-sizes="[5, 8, 10, 20]" :page-size="pagination.pageSize"
        layout="total, sizes, prev, pager, next, jumper" :total="pagination.total">
      </el-pagination>
    </div>

    <el-dialog title="会话详情" :visible.sync="detailDialogVisible" width="800px" :close-on-click-modal="false"
      @close="handleDetailDialogClose">
      <div class="dialog-content">
        <div class="session-info">
          <el-descriptions :column="2" border size="medium">
            <el-descriptions-item label="会话ID">{{ currentSession.sessionId || '-' }}</el-descriptions-item>
            <el-descriptions-item label="会话主题">{{ currentSession.topic || '-' }}</el-descriptions-item>
            <el-descriptions-item label="消息数量">{{ chatDetail.length || 0 }} 条</el-descriptions-item>
            <el-descriptions-item label="创建时间">{{ currentSession.createTime || '-' }}</el-descriptions-item>
          </el-descriptions>
        </div>

        <div class="chat-history">
          <h3 class="history-title">对话记录</h3>
          <div class="message-list" v-loading="detailLoading">
            <div v-for="(message, index) in chatDetail" :key="index" class="message-item" :class="message.role">
              <div class="message-avatar">
                <el-avatar :size="36"
                  :style="message.role === 'user' ? { backgroundColor: '#409EFF' } : { backgroundColor: '#67C23A' }">
                  {{ message.role === 'user' ? '用户' : 'AI' }}
                </el-avatar>
              </div>
              <div class="message-content">
                <div class="message-header">
                  <span class="role-name">{{ message.role === 'user' ? '用户' : 'AI助手' }}</span>
                  <span class="message-time">{{ message.createTime }}</span>
                </div>
                <div class="message-text" :class="message.role">
                  {{ message.content }}
                </div>
              </div>
            </div>

            <div v-if="chatDetail.length === 0 && !detailLoading" class="empty-message">
              <el-empty description="暂无对话记录" :image-size="80"></el-empty>
            </div>
          </div>
        </div>
      </div>
      <div slot="footer" class="dialog-footer">
        <el-button @click="detailDialogVisible = false">关 闭</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
export default {
  name: 'ChatHistoryManagement',
  data() {
    return {
      loading: false,
      detailLoading: false,
      chatList: [],
      searchForm: {
        keyword: ''
      },
      detailDialogVisible: false,
      pagination: {
        currentPage: 1,
        pageSize: 8,
        total: 0
      },
      currentSession: {
        id: '',
        sessionId: '',
        topic: '',
        answer: '',
        messageCount: '',
        createTime: ''
      },
      chatDetail: [] // 存储详细的对话记录
    }
  },
  created() {
    this.fetchChatList()
  },
  methods: {
    // 获取会话记录列表
    fetchChatList() {
      this.loading = true
      this.$http.post('/v1/chat/history', {
        currentPage: this.pagination.currentPage,
        pageSize: this.pagination.pageSize,
        keyword: this.searchForm.keyword || ''
      })
        .then(response => {
          if (response.data.code === 200) {
            const { data, total, currentPage, pageSize } = response.data.data
            this.chatList = data
            this.pagination = {
              currentPage,
              pageSize,
              total
            }
          } else {
            this.$message.error(response.data.message || '获取会话记录失败')
          }
        })
        .catch(error => {
          console.error('获取会话记录失败:', error)
          this.$message.error('获取会话记录失败')
        })
        .finally(() => {
          this.loading = false
        })
    },

    // 搜索会话记录
    handleSearch() {
      this.pagination.currentPage = 1
      this.fetchChatList()
    },

    // 查看会话详情
    handleView(row) {
      this.currentSession = { ...row }
      this.detailDialogVisible = true
      this.fetchChatDetail(row.sessionId)
    },

    // 获取会话详情
    fetchChatDetail(sessionId) {
      this.detailLoading = true
      this.chatDetail = []

      this.$http.get('/v1/chat/history/detail', {
        params: { sessionId }
      })
        .then(response => {
          if (response.data.code === 200) {
            this.chatDetail = response.data.data || []
          } else {
            this.$message.error(response.data.message || '获取会话详情失败')
          }
        })
        .catch(error => {
          console.error('获取会话详情失败:', error)
          this.$message.error('获取会话详情失败')
        })
        .finally(() => {
          this.detailLoading = false
        })
    },

    // 删除会话记录
    handleDelete(row) {
      this.$confirm('确定要删除吗？此操作不可恢复。', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning',
        customClass: 'delete-confirm-dialog'
      }).then(() => {
        this.loading = true
        this.$http.delete('/v1/chat/history/del', { params: { sessionId: row.sessionId } })
          .then(response => {
            if (response.data.code === 200) {
              this.$message.success('删除成功')
              this.fetchChatList()
            } else {
              this.$message.error(response.data.message || '删除失败')
            }
          })
          .catch(error => {
            console.error('删除失败:', error)
            this.$message.error('删除失败')
          })
          .finally(() => {
            this.loading = false
          })
      }).catch(() => {
        const activeBtn = document.activeElement;
        if (activeBtn.tagName === 'BUTTON') {
          activeBtn.blur();
        }
      })
    },

    handleDetailDialogClose() {
      this.detailDialogVisible = false
      this.chatDetail = []
      this.currentSession = {
        id: '',
        sessionId: '',
        topic: '',
        answer: '',
        messageCount: '',
        createTime: ''
      }
    },

    // 处理页码改变
    handleCurrentChange(page) {
      this.pagination.currentPage = page
      this.fetchChatList()
    },

    // 处理每页条数改变
    handleSizeChange(size) {
      this.pagination.pageSize = size
      this.pagination.currentPage = 1
      this.fetchChatList()
    }
  }
}
</script>

<style scoped>
.chat-container {
  padding: 24px;
  background-color: #ffffff;
  min-height: calc(100vh - 60px);
}

.chat-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
  background: #fff;
  padding: 16px 20px;
  border-radius: 8px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04);
}

.header-left {
  display: flex;
  align-items: baseline;
  gap: 14px;
}

.header-left h2 {
  margin: 0;
  font-size: 24px;
  color: #303133;
  font-weight: 600;
  position: relative;
}

.header-left h2::after {
  content: '';
  position: absolute;
  bottom: -8px;
  left: 0;
  width: 40px;
  height: 3px;
  background: linear-gradient(90deg, #409EFF, #67c23a);
  border-radius: 3px;
}

.header-desc {
  color: #909399;
  font-size: 14px;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 16px;
}

.search-container {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.search-input {
  width: 180px;
  transition: all 0.3s;
}

.search-input:focus-within {
  width: 200px;
}

.search-button {
  padding: 10px 15px;
  border-radius: 6px;
  transition: all 0.3s;
  background: #409EFF;
  border: none;
  color: white;
  font-weight: 500;
}

.search-button:hover {
  background: #66b1ff;
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(64, 158, 255, 0.3);
}

:deep(.custom-table) {
  margin-top: 20px;
  border-radius: 8px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.05);
  overflow: hidden;
  background: white;
}

:deep(.el-table) {
  border: none;
}

:deep(.el-table::before) {
  display: none;
}

:deep(.el-table th) {
  background-color: #ffffff;
  color: #606266;
  font-weight: 600;
  font-size: 14px;
  padding: 16px 0;
  border-bottom: 2px solid #ebeef5;
}

:deep(.el-table td) {
  padding: 16px 0;
  border-bottom: 1px solid #ebeef5;
}

:deep(.el-table--striped .el-table__body tr.el-table__row--striped td) {
  background-color: #fafafa;
}

:deep(.el-table__row) {
  transition: all 0.3s;
}

:deep(.el-table__row:hover) {
  background-color: #f0f9ff !important;
}

:deep(.el-dialog) {
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 4px 24px rgba(0, 0, 0, 0.1);
}

:deep(.el-dialog__header) {
  background: #f5f7fa;
  padding: 16px 20px;
  border-bottom: 1px solid #ebeef5;
}

:deep(.el-dialog__title) {
  font-weight: 600;
  color: #303133;
  font-size: 18px;
}

:deep(.el-dialog__body) {
  padding: 20px;
}

:deep(.el-dialog__footer) {
  border-top: 1px solid #ebeef5;
  padding: 16px 20px;
}

:deep(.el-form-item__label) {
  font-weight: 500;
}

.pagination-container {
  display: flex;
  justify-content: center;
  margin-top: 20px;
  padding: 16px 0;
  background: white;
  border-radius: 8px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.05);
}

:deep(.el-pagination) {
  padding: 0;
  font-weight: normal;
}

:deep(.el-pagination.is-background .el-pager li:not(.disabled).active) {
  background-color: #409EFF;
  color: white;
  font-weight: bold;
}

:deep(.el-pagination.is-background .el-pager li:not(.disabled):hover) {
  color: #409EFF;
}

.action-buttons {
  display: flex;
  justify-content: center;
  gap: 8px;
}

/* 详情弹窗样式 */
.dialog-content {
  max-height: 60vh;
  overflow-y: auto;
}

.session-info {
  margin-bottom: 20px;
}

.history-title {
  margin: 0 0 16px 0;
  font-size: 16px;
  color: #303133;
  font-weight: 600;
  padding-bottom: 8px;
  border-bottom: 1px solid #ebeef5;
}

.message-list {
  min-height: 200px;
}

.message-item {
  display: flex;
  margin-bottom: 20px;
  gap: 12px;
}

.message-item.user {
  flex-direction: row;
}

.message-item.assistant {
  flex-direction: row;
}

.message-avatar {
  flex-shrink: 0;
}

.message-content {
  flex: 1;
  min-width: 0;
}

.message-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 6px;
}

.role-name {
  font-weight: 600;
  font-size: 14px;
  color: #303133;
}

.message-time {
  font-size: 12px;
  color: #909399;
}

.message-text {
  padding: 12px 16px;
  border-radius: 8px;
  line-height: 1.5;
  word-wrap: break-word;
  white-space: pre-wrap;
}

.message-text.user {
  background-color: #ecf5ff;
  border: 1px solid #d9ecff;
  color: #303133;
}

.message-text.assistant {
  background-color: #f0f9eb;
  border: 1px solid #e1f3d8;
  color: #303133;
}

.empty-message {
  text-align: center;
  padding: 40px 0;
  color: #909399;
}

/* 滚动条样式 */
.dialog-content::-webkit-scrollbar {
  width: 6px;
}

.dialog-content::-webkit-scrollbar-track {
  background: #f1f1f1;
  border-radius: 3px;
}

.dialog-content::-webkit-scrollbar-thumb {
  background: #c1c1c1;
  border-radius: 3px;
}

.dialog-content::-webkit-scrollbar-thumb:hover {
  background: #a8a8a8;
}
</style>