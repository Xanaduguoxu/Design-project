<template>
  <div class="page-container">
    <div class="page-header toolbar">
      <div class="header-left">
        <h2 class="page-title">配置字典</h2>
      </div>
      <div class="header-right">
        <el-input v-model="searchForm.keyword" placeholder="请输入编码或值" clearable @clear="handleSearch"
          @keyup.enter.native="handleSearch" class="search-input minimal">
          <i slot="suffix" class="el-input__icon el-icon-search" @click="handleSearch"></i>
        </el-input>
        <el-button type="primary" size="small" icon="el-icon-plus" @click="openAddDialog">新增</el-button>
      </div>
    </div>

    <el-table :data="list" border style="width: 100%" v-loading="loading" class="custom-table">
      <el-table-column type="index" label="#" width="60" align="center"></el-table-column>
      <el-table-column prop="code" label="编码" min-width="120" align="center"></el-table-column>
      <el-table-column prop="value" label="值" min-width="120" align="center"></el-table-column>
      <el-table-column prop="classify" label="分类" min-width="120" align="center"></el-table-column>
      <el-table-column label="图片" width="120" align="center">
        <template slot-scope="scope">
          <div class="avatar-cell">
            <el-avatar :size="40" fit="cover" :src="normalizeImage(scope.row.image)" @error="handleImageError">
              {{ scope.row.value?.charAt(0) }}
            </el-avatar>
          </div>
        </template>
      </el-table-column>
      <el-table-column prop="status" label="是否生效" width="120" align="center"></el-table-column>
      <el-table-column label="创建时间" prop="createTime" min-width="160" align="center"></el-table-column>
      <el-table-column label="操作" width="180" align="center">
        <template slot-scope="scope">
          <el-button type="primary" size="mini" @click="openEditDialog(scope.row)">编辑</el-button>
          <el-button type="danger" size="mini" @click="handleDelete(scope.row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="pagination-container">
      <el-pagination @size-change="handleSizeChange" @current-change="handleCurrentChange" :current-page="currentPage"
        :page-sizes="[10, 20]" :page-size="pageSize" layout="total, sizes, prev, pager, next, jumper"
        :total="total" background class="custom-pagination" />
    </div>

    <!-- 新增/编辑弹窗 -->
    <el-dialog :title="dialogMode === 'add' ? '新增字典' : '编辑字典'" :visible.sync="dialogVisible" width="520px"
      class="register-dialog">
      <div class="dialog-content">
        <el-form :model="form" :rules="rules" ref="formRef" label-width="90px" class="register-form">
          <el-form-item label="编码" prop="code">
            <el-input v-model="form.code"></el-input>
          </el-form-item>
          <el-form-item label="值" prop="value">
            <el-input v-model="form.value"></el-input>
          </el-form-item>
          <el-form-item label="分类" prop="classify">
            <el-input v-model="form.classify"></el-input>
          </el-form-item>
          <el-form-item label="图片" prop="image">
            <div class="upload-container">
              <el-upload ref="dictUploaderRef" class="image-uploader" :show-file-list="false"
                :http-request="uploadDictImage" :before-upload="beforeImageUpload" accept="image/*">
                <div v-if="form.image" class="image-preview-wrapper">
                  <img :src="normalizeImage(form.image)" class="image-preview" />
                  <div class="uploader-actions">
                    <el-button size="mini" type="text" @click.stop="removeImage">移除</el-button>
                  </div>
                </div>
                <i v-else class="el-icon-plus image-uploader-icon"></i>
              </el-upload>
            </div>
          </el-form-item>
          <el-form-item label="状态" prop="status">
            <el-select v-model="form.status" placeholder="请选择">
              <el-option label="是" value="是" />
              <el-option label="否" value="否" />
            </el-select>
          </el-form-item>
        </el-form>
      </div>
      <span slot="footer" class="dialog-footer">
        <el-button @click="dialogVisible = false">取 消</el-button>
        <el-button type="primary" @click="submitForm">保 存</el-button>
      </span>
    </el-dialog>
  </div>
</template>

<script>
export default {
  name: 'Dictionary',
  data() {
    return {
      loading: false,
      list: [],
      total: 0,
      currentPage: 1,
      pageSize: 10,
      searchForm: { keyword: '' },
      dialogVisible: false,
      dialogMode: 'add',
      form: { id: '', code: '', value: '', image: '', classify: '', status: '是' },
      rules: {
        code: [{ required: true, message: '请输入编码', trigger: 'blur' }],
        value: [{ required: true, message: '请输入值', trigger: 'blur' }],
        classify: [{ required: true, message: '请输入分类', trigger: 'blur' }],
        status: [{ required: true, message: '请选择状态', trigger: 'change' }]
      },
      defaultImage: 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png'
    }
  },
  created() {
    this.fetchList()
  },
  methods: {
    fetchList() {
      this.loading = true
      this.$http.post('/v1/dictionary/admin/list', {
        currentPage: this.currentPage,
        pageSize: this.pageSize,
        keyword: this.searchForm.keyword
      }).then(res => {
        const data = res.data?.data || { total: 0, data: [] }
        this.list = data.data || []
        this.total = data.total || 0
      }).finally(() => {
        this.loading = false
      })
    },
    handleSearch() {
      this.currentPage = 1
      this.fetchList()
    },
    handleSizeChange(size) {
      this.pageSize = size
      this.currentPage = 1
      this.fetchList()
    },
    handleCurrentChange(page) {
      this.currentPage = page
      this.fetchList()
    },
    openAddDialog() {
      this.dialogMode = 'add'
      this.form = { id: '', code: '', value: '', image: '', status: '是' }
      this.dialogVisible = true
      this.$nextTick(() => this.$refs.formRef && this.$refs.formRef.clearValidate())
    },
    openEditDialog(row) {
      this.dialogMode = 'edit'
      this.form = { id: row.id, code: row.code, value: row.value, classify: row.classify, image: row.image, status: row.status }
      this.dialogVisible = true
      this.$nextTick(() => this.$refs.formRef && this.$refs.formRef.clearValidate())
    },
    submitForm() {
      this.$refs.formRef.validate(valid => {
        if (!valid) return
        const payload = { ...this.form }
        // 清洗图片 URL，去除反引号/空格
        payload.image = this.normalizeImage(payload.image)
        const url = this.dialogMode === 'add' ? '/v1/dictionary/add' : '/v1/dictionary/update'
        this.$http.post(url, payload).then(() => {
          this.$message.success('保存成功')
          this.dialogVisible = false
          this.fetchList()
        }).catch(() => {
          this.$message.error('保存失败，请稍后重试')
        })
      })
    },
    // 图片上传前的验证
    beforeImageUpload(file) {
      const isImage = file.type.startsWith('image/')
      const isLt2M = file.size / 1024 / 1024 < 2
      if (!isImage) this.$message.error('上传文件只能是图片格式!')
      if (!isLt2M) this.$message.error('上传图片大小不能超过 2MB!')
      return isImage && isLt2M
    },
    // 自定义上传图片方法
    uploadDictImage(options) {
      const formData = new FormData()
      formData.append('file', options.file)
      this.$http.post('/v1/common/file/upload', formData, {
        headers: { 'Content-Type': 'multipart/form-data' }
      }).then(response => {
        if (response.data && response.data.code === 200) {
          this.form.image = this.normalizeImage(response.data.data)
          options.onSuccess && options.onSuccess()
        } else {
          this.$message.error(response.data?.message || '上传失败')
          options.onError && options.onError()
        }
      }).catch(err => {
        console.error('上传图片失败:', err)
        this.$message.error('上传失败')
        options.onError && options.onError()
      })
    },
    // 移除已上传的图片
    removeImage() {
      this.form.image = ''
    },
    handleDelete(row) {
      this.$confirm('确定删除该字典项吗？', '提示', { type: 'warning' }).then(() => {
        this.$http.delete(`/v1/dictionary/del?id=${row.id}`).then(() => {
          this.$message.success('删除成功')
          this.fetchList()
        }).catch(() => {
          this.$message.error('删除失败，请稍后重试')
        })
      }).catch(() => { })
    },
    normalizeImage(url) {
      // 后端可能返回带反引号或空格的 URL，进行清洗
      return (url || '').replace(/`/g, '').trim()
    },
    handleImageError(e) {
      e.target.src = this.defaultImage
    }
  }
}
</script>

<style scoped>
.page-container {
  padding: 20px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.toolbar {
  background: var(--search-bg-color);
  border-radius: 10px;
  padding: 12px 16px;
  border: 1px solid #ebeef5;
}

.page-title {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  color: var(--search-title-color);
}

.header-right {
  display: flex;
  align-items: center;
  gap: 10px;
}

.avatar-cell {
  display: flex;
  justify-content: center;
  align-items: center;
}

.pagination-container {
  margin-top: 20px;
  padding: 15px 20px;
  background: var(--search-bg-color);
  border-radius: 8px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.05);
  display: flex;
  justify-content: center;
  align-items: center;
}

/* 弹窗样式适配 */
.register-dialog {
  border-radius: 12px;
}

.dialog-content {
  padding-bottom: 10px;
}

.register-form :deep(.el-input__inner),
.register-form :deep(.el-select .el-input__inner) {
  height: 40px;
}

:deep(.el-avatar) {
  background-color: transparent !important;
}

:deep(.el-avatar__img) {
  width: 100% !important;
  height: 100% !important;
  object-fit: cover !important;
}

/* 上传样式 */
.upload-container {
  display: flex;
  align-items: center;
  gap: 12px;
}

.image-uploader {
  width: 120px;
  height: 120px;
  border: 1px dashed #dcdfe6;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
}

.image-uploader-icon {
  font-size: 24px;
  color: #909399;
}

.image-preview {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

/* 预览包装及悬浮操作栏 */
.image-preview-wrapper {
  position: relative;
  width: 100%;
  height: 100%;
}

.uploader-actions {
  position: absolute;
  left: 0;
  right: 0;
  bottom: 0;
  padding: 6px 8px;
  background: rgba(0, 0, 0, 0.45);
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  opacity: 0;
  transition: opacity 0.2s ease;
}

.image-preview-wrapper:hover .uploader-actions {
  opacity: 1;
}
</style>