<template>
    <div class="posts-container">
        <div class="posts-header">
            <div class="title-group">
                <h2>帖子管理</h2>
                <span class="header-desc">管理所有论坛帖子信息</span>
            </div>
        </div>

        <div class="search-container">
            <el-form :inline="true" :model="searchForm" class="search-form">
                <el-form-item label="关键词">
                    <el-input v-model="searchForm.keyword" placeholder="请输入帖子标题或内容" @clear="handleSearch"
                        style="width: 260px"></el-input>
                </el-form-item>
                <el-form-item>
                    <el-button type="primary" @click="handleSearch" icon="el-icon-search">搜索</el-button>
                    <el-button @click="handleReset" icon="el-icon-refresh">重置</el-button>
                </el-form-item>
            </el-form>
        </div>

        <div class="posts-content" v-loading="loading">
            <el-table :data="postList" style="width: 100%" border stripe highlight-current-row class="post-table">

                <el-table-column prop="title" label="帖子标题" min-width="200">
                    <template slot-scope="scope">
                        <div class="title-wrapper">
                            <span class="post-title">{{ scope.row.title }}</span>
                            <el-tag v-if="scope.row.isTop === 1" type="warning" size="mini" style="margin-left: 8px;">
                                置顶
                            </el-tag>
                        </div>
                    </template>
                </el-table-column>

                <el-table-column prop="author" label="发布者" width="120" align="center">
                    <template slot-scope="scope">
                        <div class="author-wrapper">
                            <span class="author-name">{{ scope.row.author }}</span>
                        </div>
                    </template>
                </el-table-column>

                <el-table-column prop="views" label="浏览量" width="100" align="center">
                    <template slot-scope="scope">
                        <div class="views-wrapper">
                            <i class="el-icon-view"></i>
                            <span class="views-count">{{ scope.row.views }}</span>
                        </div>
                    </template>
                </el-table-column>

                <el-table-column prop="content" label="简要描述" min-width="200">
                    <template slot-scope="scope">
                        <span class="brief-text">{{ scope.row.content }}</span>
                    </template>
                </el-table-column>

                <el-table-column label="图片预览" width="120" align="center">
                    <template slot-scope="scope">
                        <div class="image-preview">
                            <el-image v-if="scope.row.imageArr && scope.row.imageArr.length > 0" :src="scope.row.imageArr[0]"
                                :preview-src-list="scope.row.imageArr" fit="cover"
                                style="width: 60px; height: 60px; border-radius: 4px;">
                                <div slot="error" class="image-slot">
                                    <i class="el-icon-picture-outline"></i>
                                </div>
                            </el-image>
                            <span v-else class="no-image">无图片</span>
                        </div>
                    </template>
                </el-table-column>



                <el-table-column label="操作" width="180" align="center" fixed="right">
                    <template slot-scope="scope">
                        <el-button size="mini" type="primary" @click="handleDetail(scope.row)">
                            详情
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

        <!-- 详情对话框 -->
        <el-dialog title="帖子详情" :visible.sync="detailDialogVisible" width="700px" class="detail-dialog">
            <div class="detail-content" v-if="currentPost">
                <div class="detail-header">
                    <h3 class="detail-title">{{ currentPost.title }}</h3>
                    <div class="detail-meta">
                        <span class="meta-item">
                            <i class="el-icon-user"></i>
                            发布者：{{ currentPost.author }}
                        </span>
                        <span class="meta-item">
                            <i class="el-icon-view"></i>
                            浏览量：{{ currentPost.views }}
                        </span>
                        <span class="meta-item">
                            <i class="el-icon-star-off"></i>
                            点赞数：{{ currentPost.likes }}
                        </span>
                        <span class="meta-item" v-if="currentPost.tags && currentPost.tags.length > 0">
                            <i class="el-icon-collection-tag"></i>
                            标签：
                            <el-tag v-for="(tag, index) in currentPost.tags" :key="index" size="small" style="margin-right: 4px;">
                                {{ tag }}
                            </el-tag>
                        </span>
                    </div>
                </div>

                <div class="detail-section" v-if="currentPost.bio">
                    <h4 class="section-title">作者简介</h4>
                    <div class="section-content bio-content">{{ currentPost.bio }}</div>
                </div>

                <div class="detail-section">
                    <h4 class="section-title">详细内容</h4>
                    <div class="section-content description-content">{{ currentPost.content }}</div>
                </div>

                <div class="detail-section" v-if="currentPost.imageArr && currentPost.imageArr.length > 0">
                    <h4 class="section-title">图片展示</h4>
                    <div class="image-gallery">
                        <el-image v-for="(img, index) in currentPost.imageArr" :key="index" :src="img"
                            :preview-src-list="currentPost.imageArr" fit="cover" class="gallery-image">
                            <div slot="error" class="image-slot">
                                <i class="el-icon-picture-outline"></i>
                            </div>
                        </el-image>
                    </div>
                </div>
            </div>

            <div slot="footer" class="dialog-footer">
                <el-button @click="detailDialogVisible = false" size="medium">关闭</el-button>
            </div>
        </el-dialog>
    </div>
</template>

<script>
export default {
    name: 'Posts',
    data() {
        return {
            loading: false,
            postList: [],
            searchForm: {
                keyword: ''
            },
            pagination: {
                currentPage: 1,
                pageSize: 10,
                total: 0
            },
            detailDialogVisible: false,
            currentPost: null
        }
    },
    created() {
        this.fetchPostList()
    },
    methods: {
        // 获取帖子列表
        fetchPostList() {
            this.loading = true
            const params = {
                currentPage: this.pagination.currentPage,
                pageSize: this.pagination.pageSize,
                keyword: this.searchForm.keyword || ''
            }

            this.$http.post('/v1/forum/list', params)
                .then(response => {
                    if (response.data.code === 200) {
                        this.postList = response.data.data.data || []
                        this.pagination.total = response.data.data.total || 0
                    } else {
                        this.$message.error(response.data.message || '获取帖子列表失败')
                    }
                })
                .catch(error => {
                    console.error('获取帖子列表失败:', error)
                    this.$message.error('获取帖子列表失败')
                })
                .finally(() => {
                    this.loading = false
                })
        },

        // 页码变化
        handleCurrentChange(page) {
            this.pagination.currentPage = page
            this.fetchPostList()
        },

        // 每页条数变化
        handleSizeChange(size) {
            this.pagination.pageSize = size
            this.pagination.currentPage = 1
            this.fetchPostList()
        },

        // 搜索
        handleSearch() {
            this.pagination.currentPage = 1
            this.fetchPostList()
        },

        // 重置搜索条件
        handleReset() {
            this.searchForm.keyword = ''
            this.pagination.currentPage = 1
            this.fetchPostList()
        },

        // 查看详情
        handleDetail(row) {
            this.currentPost = { ...row }
            this.detailDialogVisible = true
        },

        // 删除帖子
        handleDelete(row) {
            this.$confirm(`确定要删除这篇帖子吗？`, '提示', {
                confirmButtonText: '确定删除',
                cancelButtonText: '取消',
                type: 'warning',
                confirmButtonClass: 'el-button--danger',
                center: true
            }).then(() => {
                this.$http.delete('/v1/forum/del', { params: { id: row.id } })
                    .then(response => {
                        if (response.data.code === 200) {
                            this.$message.success('删除成功')
                            // 如果删除的是当前页的最后一条数据，且不是第一页，则跳转到前一页
                            if (this.postList.length === 1 && this.pagination.currentPage > 1) {
                                this.pagination.currentPage -= 1
                            }
                            this.fetchPostList()
                        } else {
                            this.$message.error(response.data.message || '删除失败')
                        }
                    })
                    .catch(error => {
                        console.error('删除帖子失败:', error)
                        this.$message.error('删除失败')
                    })
            }).catch(() => {
                this.$message.info('已取消删除')
            })
        }
    }
}
</script>

<style scoped>
.posts-container {
    padding: 20px;
    background-color: #ffffff;
    min-height: calc(100vh - 64px);
}

.posts-header {
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

.posts-content {
    background-color: #fff;
    padding: 20px;
    border-radius: 8px;
    box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.05);
    min-height: 500px;
}

.post-table {
    box-shadow: 0 1px 4px 0 rgba(0, 0, 0, 0.05);
    border: 1px solid #ebeef5;
}

:deep(.post-table .el-table th) {
    background-color: #f8f9fa;
    color: #606266;
    font-weight: 600;
    height: 52px;
}

:deep(.post-table .el-table td) {
    padding: 16px 0;
}

.title-wrapper {
    display: flex;
    align-items: center;
}

.post-title {
    font-weight: 500;
    color: #303133;
}

.author-wrapper {
    display: flex;
    align-items: center;
    justify-content: center;
}

.author-name {
    font-weight: 500;
    color: #409EFF;
}

.views-wrapper {
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 6px;
}

.views-wrapper i {
    color: #909399;
}

.views-count {
    color: #606266;
    font-weight: 500;
}

.brief-text {
    color: #606266;
    line-height: 1.5;
}

.image-preview {
    display: flex;
    align-items: center;
    justify-content: center;
}

.no-image {
    color: #c0c4cc;
    font-size: 12px;
}

.image-slot {
    display: flex;
    justify-content: center;
    align-items: center;
    width: 100%;
    height: 100%;
    background: #f5f7fa;
    color: #909399;
    font-size: 20px;
}

:deep(.post-table .el-table--striped .el-table__body tr.el-table__row--striped td) {
    background: #fafbfc;
}

:deep(.post-table .el-table__body tr:hover > td) {
    background-color: #f5f7fa !important;
}

:deep(.post-table .el-table__body tr.current-row > td) {
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

/* 详情对话框样式 */
:deep(.detail-dialog .el-dialog) {
    border-radius: 12px;
    box-shadow: 0 8px 32px rgba(0, 0, 0, 0.15);
}

:deep(.detail-dialog .el-dialog__header) {
    padding: 24px 24px 16px;
    border-bottom: 1px solid #f0f0f0;
    background: linear-gradient(135deg, #f8f9fa 0%, #fff 100%);
}

:deep(.detail-dialog .el-dialog__title) {
    font-size: 18px;
    font-weight: 600;
    color: #303133;
}

:deep(.detail-dialog .el-dialog__body) {
    padding: 24px;
    max-height: 70vh;
    overflow-y: auto;
}

.detail-content {
    padding: 0 8px;
}

.detail-header {
    margin-bottom: 24px;
    padding-bottom: 16px;
    border-bottom: 1px solid #f0f0f0;
}

.detail-title {
    margin: 0 0 16px 0;
    font-size: 20px;
    font-weight: 600;
    color: #303133;
    line-height: 1.4;
}

.detail-meta {
    display: flex;
    flex-wrap: wrap;
    gap: 20px;
}

.meta-item {
    display: flex;
    align-items: center;
    gap: 6px;
    color: #606266;
    font-size: 14px;
}

.meta-item i {
    color: #909399;
}

.detail-section {
    margin-bottom: 24px;
}

.section-title {
    margin: 0 0 12px 0;
    font-size: 16px;
    font-weight: 600;
    color: #303133;
    padding-left: 8px;
    border-left: 4px solid #409EFF;
}

.section-content {
    padding: 16px;
    background: #f8f9fa;
    border-radius: 6px;
    line-height: 1.6;
    color: #606266;
}

.brief-content {
    font-weight: 500;
}

.description-content {
    white-space: pre-wrap;
    word-break: break-word;
}

.image-gallery {
    display: flex;
    flex-wrap: wrap;
    gap: 12px;
    margin-top: 12px;
}

.gallery-image {
    width: 120px;
    height: 120px;
    border-radius: 6px;
    cursor: pointer;
    transition: transform 0.3s ease;
    border: 1px solid #ebeef5;
}

.gallery-image:hover {
    transform: scale(1.05);
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

@media (max-width: 768px) {
    .posts-container {
        padding: 16px;
    }

    .posts-header {
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

    :deep(.detail-dialog .el-dialog) {
        width: 90% !important;
        margin-top: 5vh !important;
    }

    .detail-meta {
        flex-direction: column;
        gap: 12px;
    }

    .image-gallery {
        justify-content: center;
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