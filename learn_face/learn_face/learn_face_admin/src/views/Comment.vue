<template>
    <div class="comments-container">
        <div class="comments-header">
            <div class="title-group">
                <h2>评论管理</h2>
                <span class="header-desc">管理所有用户评论信息</span>
            </div>
        </div>

        <div class="search-container">
            <el-form :inline="true" :model="searchForm" class="search-form">
                <el-form-item label="关键词">
                    <el-input v-model="searchForm.keyword" placeholder="请输入评论内容、创建人或接收者" @clear="handleSearch"
                        style="width: 300px"></el-input>
                </el-form-item>
                <el-form-item>
                    <el-button type="primary" @click="handleSearch" icon="el-icon-search">搜索</el-button>
                    <el-button @click="handleReset" icon="el-icon-refresh">重置</el-button>
                </el-form-item>
            </el-form>
        </div>

        <div class="comments-content" v-loading="loading">
            <el-table :data="commentList" style="width: 100%" border stripe highlight-current-row class="comment-table">

                <el-table-column prop="senderAvatar" label="用户头像" width="80" align="center">
                    <template slot-scope="scope">
                        <div class="avatar-wrapper">
                            <el-avatar :size="40" :src="scope.row.senderAvatar" fit="cover">
                                <i class="el-icon-user-solid" style="font-size: 20px;"></i>
                            </el-avatar>
                        </div>
                    </template>
                </el-table-column>

                <el-table-column prop="sender" label="评论人" width="120" align="center">
                    <template slot-scope="scope">
                        <div class="creator-wrapper">
                            <span class="creator-name">{{ scope.row.sender }}</span>
                        </div>
                    </template>
                </el-table-column>

                <el-table-column prop="receiver" label="接收者" width="120" align="center">
                    <template slot-scope="scope">
                        <div class="receiver-wrapper">
                            <span class="receiver-name">{{ scope.row.receiver || '无' }}</span>
                        </div>
                    </template>
                </el-table-column>

                <el-table-column prop="content" label="评论内容" min-width="250">
                    <template slot-scope="scope">
                        <div class="content-wrapper">
                            <span class="comment-content">{{ scope.row.content }}</span>
                        </div>
                    </template>
                </el-table-column>

                <el-table-column prop="belong" label="所属内容" min-width="150">
                    <template slot-scope="scope">
                        <span class="belong-text">{{ scope.row.belong }}</span>
                    </template>
                </el-table-column>

                <el-table-column prop="correlationId" label="关联ID" min-width="100">
                    <template slot-scope="scope">
                        <span class="correlation-id">{{ scope.row.correlationId }}</span>
                    </template>
                </el-table-column>

                <el-table-column prop="id" label="评论ID" width="180" align="center">
                    <template slot-scope="scope">
                        <div class="id-wrapper">
                            <span class="comment-id">{{ scope.row.id }}</span>
                        </div>
                    </template>
                </el-table-column>

                <el-table-column label="操作" width="150" align="center" fixed="right">
                    <template slot-scope="scope">
                        <el-button size="mini" type="primary" @click="handleView(scope.row)">
                            查看
                        </el-button>
                        <el-button size="mini" type="danger" @click="handleDelete(scope.row)" style="margin-left: 8px;"
                            v-if="$store.state.role !== 'volunteer'">
                            删除
                        </el-button>
                    </template>
                </el-table-column>
            </el-table>

            <div class="pagination-container">
                <el-pagination @size-change="handleSizeChange" @current-change="handleCurrentChange"
                    :current-page="pagination.currentPage" :page-sizes="[10, 20, 50, 100]"
                    :page-size="pagination.pageSize" layout="total, sizes, prev, pager, next, jumper"
                    :total="pagination.total" :pager-count="5" background>
                    <template slot="total">
                        <span class="total-text">共 {{ pagination.total }} 条记录</span>
                    </template>
                </el-pagination>
            </div>
        </div>

        <!-- 评论详情对话框 -->
        <el-dialog title="评论详情" :visible.sync="dialogVisible" width="600px" @close="handleDialogClose"
            class="comment-dialog">
            <el-form :model="commentDetail" ref="commentDetail" label-width="100px" class="dialog-form">
                <el-form-item label="用户头像">
                    <div class="detail-avatar">
                        <el-avatar :size="60" :src="commentDetail.senderAvatar" fit="cover">
                            <i class="el-icon-user-solid" style="font-size: 24px;"></i>
                        </el-avatar>
                    </div>
                </el-form-item>

                <el-form-item label="评论人">
                    <span class="detail-text">{{ commentDetail.sender }}</span>
                </el-form-item>

                <el-form-item label="接收者">
                    <span class="detail-text">{{ commentDetail.receiver || '无' }}</span>
                </el-form-item>

                <el-form-item label="评论内容">
                    <div class="content-detail">
                        <p>{{ commentDetail.content }}</p>
                    </div>
                </el-form-item>

                <el-form-item label="所属内容">
                    <span class="detail-text">{{ commentDetail.belong }}</span>
                </el-form-item>

                <el-form-item label="关联ID">
                    <span class="detail-text">{{ commentDetail.correlationId }}</span>
                </el-form-item>

                <el-form-item label="评论ID">
                    <span class="detail-text">{{ commentDetail.id }}</span>
                </el-form-item>

                <el-form-item label="父级ID" v-if="commentDetail.parentId">
                    <span class="detail-text">{{ commentDetail.parentId }}</span>
                </el-form-item>
            </el-form>

            <div slot="footer" class="dialog-footer">
                <el-button @click="dialogVisible = false" size="medium">关闭</el-button>
            </div>
        </el-dialog>
    </div>
</template>

<script>
export default {
    name: 'Comments',
    data() {
        return {
            loading: false,
            commentList: [],
            searchForm: {
                keyword: ''
            },
            pagination: {
                currentPage: 1,
                pageSize: 10,
                total: 0
            },
            dialogVisible: false,
            commentDetail: {
                id: '',
                sender: '',
                receiver: '',
                senderAvatar: '',
                content: '',
                belong: '',
                correlationId: '',
                parentId: ''
            }
        }
    },
    created() {
        this.fetchCommentList()
    },
    methods: {
        // 获取评论列表
        fetchCommentList() {
            this.loading = true
            const params = {
                currentPage: this.pagination.currentPage,
                pageSize: this.pagination.pageSize,
                keyword: this.searchForm.keyword || ''
            }

            this.$http.post('/v1/comments/list', params)
                .then(response => {
                    if (response.data.code === 200) {
                        this.commentList = response.data.data.data || []
                        this.pagination.total = response.data.data.total || 0
                    } else {
                        this.$message.error(response.data.message || '获取评论列表失败')
                    }
                })
                .catch(error => {
                    console.error('获取评论列表失败:', error)
                    this.$message.error('获取评论列表失败')
                })
                .finally(() => {
                    this.loading = false
                })
        },

        // 页码变化
        handleCurrentChange(page) {
            this.pagination.currentPage = page
            this.fetchCommentList()
        },

        // 每页条数变化
        handleSizeChange(size) {
            this.pagination.pageSize = size
            this.pagination.currentPage = 1
            this.fetchCommentList()
        },

        // 搜索
        handleSearch() {
            this.pagination.currentPage = 1
            this.fetchCommentList()
        },

        // 重置搜索条件
        handleReset() {
            this.searchForm.keyword = ''
            this.pagination.currentPage = 1
            this.fetchCommentList()
        },

        // 查看评论详情
        handleView(row) {
            this.commentDetail = {
                id: row.id,
                sender: row.sender,
                receiver: row.receiver,
                senderAvatar: row.senderAvatar,
                content: row.content,
                belong: row.belong,
                correlationId: row.correlationId,
                parentId: row.parentId
            }
            this.dialogVisible = true
        },

        // 删除评论
        handleDelete(row) {
            this.$confirm(`确定要删除「${row.sender}」的评论吗？`, '提示', {
                confirmButtonText: '确定删除',
                cancelButtonText: '取消',
                type: 'warning',
                confirmButtonClass: 'el-button--danger',
                center: true
            }).then(() => {
                // 这里需要根据实际接口调整删除请求
                this.$http.delete('/v1/comments/del', { params: { id: row.id } })
                    .then(response => {
                        if (response.data.code === 200) {
                            this.$message.success('删除成功')
                            // 如果删除的是当前页的最后一条数据，且不是第一页，则跳转到前一页
                            if (this.commentList.length === 1 && this.pagination.currentPage > 1) {
                                this.pagination.currentPage -= 1
                            }
                            this.fetchCommentList()
                        } else {
                            this.$message.error(response.data.message || '删除失败')
                        }
                    })
                    .catch(error => {
                        console.error('删除评论失败:', error)
                        this.$message.error('删除失败')
                    })
            }).catch(() => {
                this.$message.info('已取消删除')
            })
        },

        // 对话框关闭
        handleDialogClose() {
            this.commentDetail = {
                id: '',
                sender: '',
                receiver: '',
                senderAvatar: '',
                content: '',
                belong: '',
                correlationId: '',
                parentId: ''
            }
        }
    }
}
</script>

<style scoped>
.comments-container {
    padding: 20px;
    background-color: #ffffff;
    min-height: calc(100vh - 64px);
}

.comments-header {
    margin-bottom: 20px;
    display: flex;
    justify-content: space-between;
    align-items: center;
    background: #fff;
    padding: 20px;
    border-radius: 8px;
    box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.05);
}

.title-group {
    display: flex;
    align-items: baseline;
    gap: 12px;
}

.title-group h2 {
    margin: 0;
    font-size: 24px;
    color: #303133;
    font-weight: 600;
}

.header-desc {
    color: #909399;
    font-size: 14px;
}

.search-container {
    background-color: #fff;
    padding: 20px 24px;
    border-radius: 8px;
    box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.05);
    margin-bottom: 20px;
}

.search-form {
    display: flex;
    align-items: center;
    flex-wrap: wrap;
    gap: 16px;
}

:deep(.search-form .el-form-item) {
    margin-bottom: 0;
    margin-right: 20px;
}

:deep(.search-form .el-form-item__label) {
    font-weight: 500;
    color: #606266;
}

.comments-content {
    background-color: #fff;
    padding: 20px;
    border-radius: 8px;
    box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.05);
    min-height: 500px;
}

.comment-table {
    box-shadow: 0 1px 4px 0 rgba(0, 0, 0, 0.05);
    border: 1px solid #ebeef5;
}

:deep(.comment-table .el-table th) {
    background-color: #f8f9fa;
    color: #606266;
    font-weight: 600;
    height: 52px;
}

:deep(.comment-table .el-table td) {
    padding: 16px 0;
}

.avatar-wrapper {
    display: flex;
    align-items: center;
    justify-content: center;
}

.creator-wrapper,
.receiver-wrapper {
    display: flex;
    align-items: center;
    justify-content: center;
}

.creator-name,
.receiver-name {
    font-weight: 500;
    color: #303133;
}

.content-wrapper {
    max-height: 60px;
    overflow: hidden;
    text-overflow: ellipsis;
    display: -webkit-box;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
    line-height: 1.5;
}

.comment-content {
    color: #606266;
    font-size: 14px;
}

.belong-text,
.correlation-id {
    color: #606266;
    font-weight: 500;
}

.time-wrapper {
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 6px;
}

.time-wrapper i {
    color: #909399;
}

.create-time {
    color: #909399;
    font-size: 13px;
}

:deep(.comment-table .el-table--striped .el-table__body tr.el-table__row--striped td) {
    background: #fafbfc;
}

:deep(.comment-table .el-table__body tr:hover > td) {
    background-color: #f5f7fa !important;
}

:deep(.comment-table .el-table__body tr.current-row > td) {
    background-color: #ecf5ff !important;
}

.pagination-container {
    margin-top: 24px;
    display: flex;
    justify-content: flex-end;
    padding: 16px 0 8px;
}

.total-text {
    color: #606266;
    font-weight: 500;
}

:deep(.pagination-container .el-pagination.is-background .el-pager li:not(.disabled).active) {
    background-color: #409EFF;
    color: #fff;
    font-weight: 600;
}

/* 对话框样式优化 */
:deep(.comment-dialog .el-dialog) {
    border-radius: 12px;
    box-shadow: 0 8px 32px rgba(0, 0, 0, 0.15);
}

:deep(.comment-dialog .el-dialog__header) {
    padding: 24px 24px 16px;
    border-bottom: 1px solid #f0f0f0;
    background: linear-gradient(135deg, #f8f9fa 0%, #fff 100%);
}

:deep(.comment-dialog .el-dialog__title) {
    font-size: 18px;
    font-weight: 600;
    color: #303133;
}

:deep(.comment-dialog .el-dialog__headerbtn) {
    top: 24px;
    right: 24px;
    width: 24px;
    height: 24px;
    border-radius: 4px;
    background: #f5f7fa;
}

:deep(.comment-dialog .el-dialog__headerbtn:hover) {
    background: #e4e7ed;
}

:deep(.comment-dialog .el-dialog__headerbtn .el-dialog__close) {
    color: #909399;
}

:deep(.comment-dialog .el-dialog__body) {
    padding: 24px;
}

:deep(.comment-dialog .el-dialog__footer) {
    padding: 20px 24px 24px;
    border-top: 1px solid #f0f0f0;
    background: #fafbfc;
}

/* 表单样式优化 */
.dialog-form {
    padding: 0 8px;
}

:deep(.dialog-form .el-form-item) {
    margin-bottom: 20px;
    display: flex;
    align-items: flex-start;
}

:deep(.dialog-form .el-form-item__label) {
    padding-right: 16px;
    font-weight: 500;
    color: #606266;
    line-height: 40px;
    text-align: right;
}

:deep(.dialog-form .el-form-item__content) {
    flex: 1;
    line-height: 40px;
}

.detail-avatar {
    display: flex;
    justify-content: center;
}

.detail-text {
    color: #606266;
    font-size: 14px;
}

.content-detail {
    padding: 12px;
    background: #f8f9fa;
    border-radius: 6px;
    border: 1px solid #ebeef5;
    line-height: 1.6;
    color: #606266;
}

/* 底部按钮样式优化 */
.dialog-footer {
    display: flex;
    justify-content: flex-end;
    align-items: center;
    gap: 12px;
}

:deep(.dialog-footer .el-button) {
    min-width: 88px;
    height: 36px;
    border-radius: 6px;
    font-weight: 500;
    transition: all 0.3s ease;
}

:deep(.dialog-footer .el-button--default) {
    border: 1px solid #dcdfe6;
    color: #606266;
    background: #fff;
}

:deep(.dialog-footer .el-button--default:hover) {
    border-color: #409EFF;
    background-color: #ecf5ff;
    color: #409EFF;
    transform: translateY(-1px);
}

:deep(.dialog-footer .el-button--primary) {
    background: linear-gradient(135deg, #409EFF, #337ecc);
    border: none;
    box-shadow: 0 2px 6px rgba(64, 158, 255, 0.3);
}

:deep(.dialog-footer .el-button--primary:hover) {
    background: linear-gradient(135deg, #66b1ff, #409EFF);
    box-shadow: 0 4px 12px rgba(64, 158, 255, 0.4);
    transform: translateY(-1px);
}

:deep(.dialog-footer .el-button--primary:active) {
    transform: translateY(0);
}

@media (max-width: 768px) {
    .comments-container {
        padding: 16px;
    }

    .comments-header {
        flex-direction: column;
        gap: 16px;
        align-items: stretch;
    }

    .title-group {
        justify-content: center;
    }

    .search-form {
        flex-direction: column;
        align-items: stretch;
    }

    :deep(.search-form .el-form-item) {
        margin-right: 0;
        width: 100%;
    }

    :deep(.comment-dialog .el-dialog) {
        width: 90% !important;
        margin-top: 5vh !important;
    }

    :deep(.dialog-form .el-form-item) {
        flex-direction: column;
        align-items: stretch;
    }

    :deep(.dialog-form .el-form-item__label) {
        text-align: left;
        padding-bottom: 8px;
        line-height: 1.4;
    }
}

:deep(.el-loading-mask) {
    border-radius: 8px;
}

:deep(.el-loading-spinner .el-loading-text) {
    color: #409EFF;
    font-weight: 500;
}
</style>
