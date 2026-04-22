<template>
  <div class="banner-container">
    <div class="banner-header">
      <div class="header-left">
        <h2>轮播图管理</h2>
        <span class="header-desc">管理首页轮播图展示</span>
      </div>
      <div class="header-right">
        <div class="search-container">
          <el-input placeholder="搜索标题或描述" v-model="searchKeyword" class="search-input" prefix-icon="el-icon-search"
            clearable @clear="handleSearch" @keyup.enter.native="handleSearch">
          </el-input>
          <el-button type="primary" class="search-button" @click="handleSearch">
            <span>搜索</span>
          </el-button>
        </div>
        <el-button type="primary" @click="handleAdd" class="add-button">
          <i class="el-icon-plus"></i>
          <span>添加轮播图</span>
        </el-button>
      </div>
    </div>

    <el-table :data="bannerList" border style="width: 100%" v-loading="loading" class="custom-table">
      <el-table-column prop="name" label="标题" min-width="120" />
      <el-table-column label="图片" width="200" align="center">
        <template slot-scope="scope">
          <el-image :src="scope.row.image" :preview-src-list="[scope.row.image]" fit="cover"
            style="width: 180px; height: 100px; border-radius: 4px;">
            <div slot="error" class="image-slot">
              <i class="el-icon-picture-outline"></i>
            </div>
          </el-image>
        </template>
      </el-table-column>
      <el-table-column prop="brief" label="描述" min-width="200" />
      <el-table-column prop="status" label="是否生效" min-width="100" align="center">
        <template slot-scope="scope">
          <el-tag :type="scope.row.status === '是' ? 'success' : 'danger'">{{ scope.row.status === '是' ? '是' : '否'
            }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createBy" label="创建人" min-width="200" align="center" />
      <el-table-column prop="createTime" label="创建时间" min-width="160" align="center" />
      <el-table-column prop="updateTime" label="更新时间" min-width="160" align="center" />
      <el-table-column label="操作" width="220" align="center" fixed="right">
        <template slot-scope="scope">
          <el-switch
            v-model="scope.row.status"
            :active-value="'是'"
            :inactive-value="'否'"
            @change="handleStatusChange(scope.row)"
            style="margin-right: 12px"
            active-color="#13ce66"
            inactive-color="#ff4949">
          </el-switch>
          <el-button size="mini" type="primary" plain style="margin-right: 8px"
            @click="handleEdit(scope.row)">编辑</el-button>
          <el-button size="mini" type="danger" plain @click="handleDelete(scope.row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 分页组件 -->
    <div class="pagination-container">
      <el-pagination @size-change="handleSizeChange" @current-change="handleCurrentChange"
        :current-page="pagination.currentPage" :page-sizes="[8, 16, 24, 32]" :page-size="pagination.pageSize"
        layout="total, sizes, prev, pager, next, jumper" :total="pagination.total" background>
      </el-pagination>
    </div>

    <!-- 添加/编辑轮播图对话框 -->
    <el-dialog :title="isEdit ? '编辑' : '添加'" :visible.sync="dialogVisible" width="500px"
      :close-on-click-modal="false" @close="handleDialogClose">
      <el-form :model="bannerForm" :rules="rules" ref="bannerForm" label-width="80px">
        <el-form-item label="标题" prop="name">
          <el-input v-model="bannerForm.name" placeholder="请输入轮播图标题"></el-input>
        </el-form-item>

        <el-form-item label="图片" prop="image">
          <el-upload class="banner-uploader" action="#" :http-request="handleUpload" :show-file-list="false"
            :before-upload="beforeUpload">
            <img v-if="imageUrl" :src="imageUrl" class="banner-image">
            <i v-else class="el-icon-plus banner-uploader-icon"></i>
          </el-upload>
          <div class="upload-tip">建议尺寸：1920x600px，格式：JPG、PNG</div>
        </el-form-item>

        <el-form-item label="描述" prop="brief">
          <el-input v-model="bannerForm.brief" type="textarea" :rows="3" placeholder="请输入轮播图描述"></el-input>
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button @click="dialogVisible = false">取 消</el-button>
        <el-button type="primary" @click="submitForm" :loading="submitLoading">确 定</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
export default {
  name: 'Banner',
  data() {
    return {
      loading: false,
      bannerList: [],
      searchKeyword: '',
      dialogVisible: false,
      isEdit: false,
      imageUrl: '',
      submitLoading: false,
      pagination: {
        currentPage: 1,
        pageSize: 8,
        total: 0
      },
      bannerForm: {
        name: '',
        image: '',
        brief: ''
      },
      rules: {
        name: [
          { required: true, message: '请输入标题', trigger: 'blur' },
          { min: 2, max: 50, message: '长度在 2 到 50 个字符', trigger: 'blur' }
        ],
        brief: [
          { required: true, message: '请输入描述', trigger: 'blur' },
          { min: 2, max: 200, message: '长度在 2 到 200 个字符', trigger: 'blur' }
        ]
      }
    }
  },
  created() {
    this.fetchBannerList()
  },
  methods: {
    fetchBannerList() {
      this.loading = true
      const requestData = {
        currentPage: this.pagination.currentPage,
        pageSize: this.pagination.pageSize,
        keyword: this.searchKeyword || ''
      }

      this.$http.post('/v1/banner/admin/list', requestData)
        .then(response => {
          if (response.data.code === 200) {
            const data = response.data.data
            this.bannerList = data.data || data
            if (data.total !== undefined) {
              this.pagination.total = data.total
            }
            if (data.currentPage !== undefined) {
              this.pagination.currentPage = data.currentPage
            }
            if (data.pageSize !== undefined) {
              this.pagination.pageSize = data.pageSize
            }
          } else {
            this.$message.error(response.data.message || '获取轮播图列表失败')
          }
        })
        .catch(error => {
          console.error('获取轮播图列表失败:', error)
          this.$message.error('获取轮播图列表失败')
        })
        .finally(() => {
          this.loading = false
        })
    },

    handleSearch() {
      this.pagination.currentPage = 1
      this.fetchBannerList()
    },

    handleSizeChange(newSize) {
      this.pagination.pageSize = newSize
      this.pagination.currentPage = 1
      this.fetchBannerList()
    },

    handleCurrentChange(newPage) {
      this.pagination.currentPage = newPage
      this.fetchBannerList()
    },

    handleAdd() {
      this.isEdit = false
      this.dialogVisible = true
      this.imageUrl = ''
      this.$nextTick(() => {
        this.$refs.bannerForm?.resetFields()
      })
    },

    handleEdit(row) {
      this.isEdit = true
      this.dialogVisible = true
      this.imageUrl = row.image
      this.$nextTick(() => {
        this.bannerForm = {
          id: row.id,
          name: row.name,
          brief: row.brief,
          image: row.image
        }
      }).catch(() => {
        // 取消操作
        const activeBtn = document.activeElement;
        if (activeBtn.tagName === 'BUTTON') {
          activeBtn.blur(); // 主动移除焦点
        }
      })
    },

    handleDelete(row) {
      this.$confirm('确定要删除该轮播图吗？', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning',
        customClass: 'delete-confirm-dialog'
      }).then(() => {
        this.loading = true
        this.$http.delete(`/v1/banner/del?id=${row.id}`)
          .then(response => {
            if (response.data.code === 200) {
              this.$message.success('删除成功')
              if (this.bannerList.length === 1 && this.pagination.currentPage > 1) {
                this.pagination.currentPage -= 1
              }
              this.fetchBannerList()
            } else {
              this.$message.error(response.data.message || '删除失败')
            }
          })
          .catch(error => {
            console.error('删除轮播图失败:', error)
            this.$message.error('删除失败')
          })
          .finally(() => {
            this.loading = false
          })
      }).catch(() => {
        // 取消操作
        const activeBtn = document.activeElement;
        if (activeBtn.tagName === 'BUTTON') {
          activeBtn.blur(); // 主动移除焦点
        }
      })
    },

    // 新增：处理状态切换
    handleStatusChange(row) {
      const newStatus = row.status === '是'
      const statusText = newStatus ? '启用' : '禁用'
      
      this.$confirm(`确定要${statusText}该轮播图吗？`, '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        this.$http.get(`/v1/banner/activate?id=${row.id}&status=${newStatus ? 1 : 0}`)
          .then(response => {
            if (response.data.code === 200) {
              this.$message.success(`${statusText}成功`)
              // 可以在这里更新本地数据，或者重新获取列表
              this.fetchBannerList()
            } else {
              // 如果请求失败，回退状态
              row.status = newStatus ? '否' : '是'
              this.$message.error(response.data.message || `${statusText}失败`)
            }
          })
          .catch(error => {
            console.error(`${statusText}轮播图失败:`, error)
            // 如果请求失败，回退状态
            row.status = newStatus ? '否' : '是'
            this.$message.error(`${statusText}失败`)
          })
      }).catch(() => {
        // 取消操作，回退状态
        row.status = newStatus ? '否' : '是'
        const activeBtn = document.activeElement;
        if (activeBtn.tagName === 'BUTTON') {
          activeBtn.blur();
        }
      })
    },

    handleDialogClose() {
      this.dialogVisible = false
      this.imageUrl = ''
      this.$refs.bannerForm?.resetFields()
    },

    beforeUpload(file) {
      const isImage = file.type.startsWith('image/')
      const isLt2M = file.size / 1024 / 1024 < 2

      if (!isImage) {
        this.$message.error('只能上传图片文件!')
        return false
      }
      if (!isLt2M) {
        this.$message.error('图片大小不能超过 2MB!')
        return false
      }
      return true
    },

    handleUpload(params) {
      const formData = new FormData()
      formData.append('file', params.file)

      const loading = this.$loading({
        lock: true,
        text: '图片上传中...',
        spinner: 'el-icon-loading',
        background: 'rgba(0, 0, 0, 0.7)'
      })

      this.$http.post('/v1/common/file/upload', formData)
        .then(response => {
          if (response.data.code === 200) {
            this.imageUrl = response.data.data
            this.bannerForm.image = this.imageUrl
            this.$message.success('图片上传成功')
          } else {
            this.$message.error(response.data.message || '图片上传失败')
          }
        })
        .catch(error => {
          console.error('图片上传失败:', error)
          this.$message.error('图片上传失败')
        })
        .finally(() => {
          loading.close()
        })
    },

    submitForm() {
      this.$refs.bannerForm.validate(valid => {
        if (valid) {
          this.submitLoading = true

          const bannerData = {
            name: this.bannerForm.name,
            brief: this.bannerForm.brief
          }

          if (this.isEdit) {
            bannerData.id = this.bannerForm.id
          }
          if (this.bannerForm.image) {
            bannerData.image = this.bannerForm.image
          }

          const url = this.isEdit ? '/v1/banner/update' : '/v1/banner/add'
          this.$http.post(url, bannerData)
            .then(response => {
              if (response.data.code === 200) {
                this.$message.success(this.isEdit ? '编辑成功' : '添加成功')
                this.dialogVisible = false
                this.fetchBannerList()
              } else {
                this.$message.error(response.data.message || (this.isEdit ? '编辑失败' : '添加失败'))
              }
            })
            .catch(error => {
              console.error(this.isEdit ? '编辑失败:' : '添加失败:', error)
              this.$message.error(this.isEdit ? '编辑失败' : '添加失败')
            })
            .finally(() => {
              this.submitLoading = false
            })
        }
      })
    }
  }
}
</script>

<style scoped>
.banner-container {
  padding: 24px;
  background-color: #ffffff;
  min-height: calc(100vh - 60px);
}

.banner-header {
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

.add-button {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 20px;
  border-radius: 6px;
  transition: all 0.3s;
  background: linear-gradient(135deg, #409EFF, #3a8ee6);
  border: none;
  color: white;
  font-weight: 500;
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
}

.search-input {
  width: 240px;
  transition: all 0.3s;
}

.search-input:focus-within {
  width: 280px;
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

.add-button:hover {
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
  background-color: #f5f7fa;
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

:deep(.el-image) {
  border: 1px solid #ebeef5;
  transition: all 0.3s;
  border-radius: 6px;
  overflow: hidden;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
}

:deep(.el-image:hover) {
  transform: scale(1.05);
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.1);
}

.image-slot {
  display: flex;
  justify-content: center;
  align-items: center;
  width: 100%;
  height: 100%;
  background: #f5f7fa;
  color: #909399;
  font-size: 24px;
  border-radius: 6px;
}

:deep(.el-button--mini) {
  padding: 7px 12px;
  font-size: 12px;
  border-radius: 4px;
  transition: all 0.3s;
}

:deep(.el-button--mini:hover) {
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

:deep(.el-button--primary.is-plain) {
  border-color: #b3d8ff;
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
  padding: 24px 30px;
}

:deep(.el-dialog__footer) {
  border-top: 1px solid #ebeef5;
  padding: 16px 20px;
}

:deep(.el-form-item__label) {
  font-weight: 500;
}

.banner-uploader {
  border: 2px dashed #d9d9d9;
  border-radius: 8px;
  cursor: pointer;
  position: relative;
  overflow: hidden;
  width: 360px;
  height: 200px;
  transition: all 0.3s;
  display: flex;
  justify-content: center;
  align-items: center;
  background-color: #fafafa;
}

.banner-uploader:hover {
  border-color: #409EFF;
  background-color: #f0f9ff;
  box-shadow: 0 0 10px rgba(64, 158, 255, 0.2);
}

.banner-uploader-icon {
  font-size: 32px;
  color: #8c939d;
  width: 360px;
  height: 200px;
  line-height: 200px;
  text-align: center;
  transition: all 0.3s;
}

.banner-uploader:hover .banner-uploader-icon {
  color: #409EFF;
  transform: scale(1.1);
}

.banner-image {
  width: 360px;
  height: 200px;
  display: block;
  object-fit: cover;
  border-radius: 6px;
  transition: all 0.3s;
}

.upload-tip {
  font-size: 12px;
  color: #909399;
  margin-top: 10px;
  line-height: 1.5;
}

.pagination-container {
  display: flex;
  justify-content: flex-end;
  margin-top: 20px;
  padding: 16px 0;
}

/* 开关按钮样式 */
:deep(.el-switch) {
  margin-right: 12px;
}

:deep(.el-switch__core) {
  border-radius: 10px;
  height: 20px;
}

:deep(.el-switch__core:after) {
  width: 16px;
  height: 16px;
  border-radius: 50%;
}
</style>