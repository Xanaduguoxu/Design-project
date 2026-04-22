<template>
  <div class="course-container">
    <div class="course-header">
      <div class="title-group">
        <h2>课程管理</h2>
        <span class="header-desc">管理所有课程信息</span>
      </div>
    </div>

    <div class="search-container">
      <el-form :inline="true" :model="searchForm" class="search-form">
        <el-form-item label="课程名称">
          <el-input v-model="searchForm.keyword" placeholder="请输入课程名称" clearable @clear="handleSearch"></el-input>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch" icon="el-icon-search">搜索</el-button>
          <el-button @click="handleReset" icon="el-icon-refresh">重置</el-button>
          <el-button type="success" @click="handleAdd" icon="el-icon-plus">新增</el-button>
        </el-form-item>
      </el-form>
    </div>

    <!-- Table part remains the same -->
    <div class="course-content" v-loading="loading">
      <el-table :data="courseList" style="width: 100%" border stripe highlight-current-row class="course-table">
        <el-table-column prop="name" label="课程名称" min-width="120">
          <template slot-scope="scope">
            <span class="course-name">{{ scope.row.name }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="brief" label="描述" width="800">
          <template slot-scope="scope">
            <span>{{ scope.row.brief }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="category" label="课程分类" width="100">
          <template slot-scope="scope">
            <span>{{ getLevelLabel(scope.row.category) }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="duration" label="时长" width="100">
          <template slot-scope="scope">
            <span>{{ scope.row.duration }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180" align="center" fixed="right">
          <template slot-scope="scope">
            <el-button size="mini" type="primary" @click="handleEdit(scope.row)">编辑</el-button>
            <el-button size="mini" type="danger" @click="handleDelete(scope.row)" style="margin-left: 8px;"
              v-if="$store.state.role !== 'volunteer'">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-container">
        <el-pagination @size-change="handleSizeChange" @current-change="handleCurrentChange"
          :current-page="pagination.currentPage" :page-sizes="[10, 20, 50, 100]" :page-size="pagination.pageSize"
          layout="total, prev, pager, next, sizes" :total="pagination.total" :pager-count="5" background>
          <template slot="total">
            <span class="total-text">共 {{ pagination.total }} 条</span>
          </template>
        </el-pagination>
      </div>
    </div>

    <el-dialog :title="dialogType === 'edit' ? '编辑课程' : '新增课程'" :visible.sync="dialogVisible" width="1000px"
      :close-on-click-modal="false" @closed="resetForm" top="5vh">
      <el-form :model="courseForm" :rules="rules" ref="courseForm" label-width="100px" label-position="right"
        class="course-form">
        <div class="form-grid">
          <el-form-item label="课程名称" prop="name">
            <el-input v-model="courseForm.name" placeholder="请输入课程名称"></el-input>
          </el-form-item>
          <el-form-item label="难度等级" prop="level">
            <el-select v-model="courseForm.level" placeholder="请选择难度等级" style="width: 100%;">
              <el-option v-for="item in levelOptions" :key="item.value" :label="item.label" :value="item.value">
              </el-option>
            </el-select>
          </el-form-item>
          <el-form-item label="总时长" prop="duration">
            <el-input v-model="courseForm.duration" placeholder="例如: 8小时20分钟"></el-input>
          </el-form-item>

          <el-form-item label="课程封面" prop="image" class="full-width">
            <el-upload class="image-uploader" action="#" :http-request="uploadImage" :show-file-list="false"
              :before-upload="beforeImageUpload">
              <div v-if="courseForm.image" class="image-preview-wrapper">
                <img :src="courseForm.image" class="image-preview">
                <div class="uploader-actions">
                  <i class="el-icon-delete" @click.stop="removeImage"></i>
                </div>
              </div>
              <i v-else class="el-icon-plus image-uploader-icon"></i>
            </el-upload>
          </el-form-item>
          
          <el-form-item label="课程描述" prop="description" class="full-width">
            <el-input v-model="courseForm.description" type="textarea" :rows="4" placeholder="请输入课程描述"></el-input>
          </el-form-item>

          <el-form-item label="课件" prop="courseware" class="full-width">
            <div v-if="courseForm.courseware" class="file-display-box">
              <i class="el-icon-document"></i>
              <span class="file-name" :title="courseForm.courseware">{{ getFileName(courseForm.courseware) }}</span>
              <i class="el-icon-circle-close file-delete-icon" @click="removeCourseware"></i>
            </div>
            <el-upload v-else class="courseware-uploader" action="#" :http-request="uploadCourseware"
              :show-file-list="false" :before-upload="beforeCoursewareUpload">
              <el-button size="small" type="primary">点击上传</el-button>
              <div slot="tip" class="el-upload__tip">支持 doc, pdf, ppt, xls 等格式，大小不超过10MB</div>
            </el-upload>
          </el-form-item>
          
          <!-- === Chapter and Lesson Section Refactored with el-collapse === -->
          <el-form-item label="课程章节" class="full-width">
            <div class="chapters-container">
              <el-collapse v-model="activeChapter" accordion class="chapter-collapse">
                <el-collapse-item v-for="(chapter, chapterIndex) in courseForm.chapters" :key="chapterIndex" :name="chapterIndex">
                  <template slot="title">
                    <div class="chapter-title-bar" @click.stop>
                      <span class="chapter-index">章节 {{ chapterIndex + 1 }}</span>
                      <el-input v-model="chapter.name" placeholder="请输入章节名称" size="small" class="chapter-name-input"></el-input>
                      <el-button type="danger" icon="el-icon-delete" circle size="mini" class="delete-chapter-btn" @click="removeChapter(chapterIndex)"></el-button>
                    </div>
                  </template>

                  <div class="lessons-area">
                    <div v-for="(lesson, lessonIndex) in chapter.lessons" :key="lessonIndex" class="lesson-row">
                      <el-input v-model="lesson.name" placeholder="课时名称" size="small"></el-input>
                      
                      <div class="lesson-video-uploader">
                        <div v-if="lesson.video" class="file-display-box video-box">
                          <i class="el-icon-video-camera-solid"></i>
                          <span class="file-name" :title="lesson.video">{{ getFileName(lesson.video) }}</span>
                          <i class="el-icon-circle-close file-delete-icon" @click="removeLessonVideo(chapterIndex, lessonIndex)"></i>
                        </div>
                        <el-upload v-else action="#" :show-file-list="false"
                          :http-request="(options) => uploadLessonVideo(options, chapterIndex, lessonIndex)"
                          :before-upload="beforeVideoUpload">
                          <el-button size="mini" type="primary" plain>上传视频</el-button>
                        </el-upload>
                      </div>

                      <el-button type="danger" icon="el-icon-delete" circle size="mini" @click="removeLesson(chapterIndex, lessonIndex)"></el-button>
                    </div>
                    <el-button type="primary" icon="el-icon-plus" size="mini" @click="addLesson(chapterIndex)" style="margin-top: 10px;">添加课时</el-button>
                  </div>
                </el-collapse-item>
              </el-collapse>
              <div v-if="courseForm.chapters.length === 0" class="empty-chapters">暂无章节</div>
              <el-button type="success" @click="addChapter" icon="el-icon-plus" style="margin-top: 15px;">添加新章节</el-button>
            </div>
          </el-form-item>
        </div>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button @click="dialogVisible = false" plain>取 消</el-button>
        <el-button type="primary" @click="submitForm" :loading="submitLoading">确 定</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
export default {
  name: 'Course',
  data() {
    return {
      activeChapter: 0, // For el-collapse
      loading: false,
      courseList: [],
      levelOptions: [],
      levelMap: {},
      searchForm: {
        keyword: ''
      },
      pagination: {
        currentPage: 1,
        pageSize: 10,
        total: 0
      },
      dialogVisible: false,
      dialogType: 'edit',
      submitLoading: false,
      courseForm: this.getDefaultCourseForm(),
      rules: {
        name: [
          { required: true, message: '请输入课程名称', trigger: 'blur' },
          { min: 2, max: 100, message: '长度在 2 到 100 个字符', trigger: 'blur' }
        ],
        level: [
          { required: true, message: '请选择难度等级', trigger: 'change' }
        ],
        duration: [
          { required: true, message: '请输入课程总时长', trigger: 'blur' }
        ]
      }
    }
  },
  created() {
    this.fetchCourseList()
    this.fetchLevelList()
  },
  methods: {
    getFileName(url) {
      if (!url) return '';
      return url.substring(url.lastIndexOf('/') + 1);
    },
    
    getDefaultCourseForm() {
      return {
        id: '',
        name: '',
        courseware: '',
        category: '',
        view: 0,
        duration: '',
        description: '',
        image: '',
        chapters: []
      }
    },
    fetchLevelList() {
      this.$http.get('/v1/dictionary/list?classify=course_level')
        .then(response => {
          if (response.data.code === 200) {
            const list = response.data.data || []
            this.levelOptions = list.map(item => ({
              label: item.value,
              value: item.code
            }))
            this.levelMap = list.reduce((acc, item) => {
              acc[item.code] = item.value
              return acc
            }, {})
          } else {
            this.$message.error(response.data.message || '获取难度等级列表失败')
          }
        })
        .catch(error => {
          console.error('获取难度等级列表失败:', error)
          this.$message.error('获取难度等级列表失败')
        })
    },
    getLevelLabel(level) {
      return this.levelMap[level] || level || ''
    },
    fetchCourseList() {
      this.loading = true
      this.$http.post('/v1/course/list', {
        currentPage: this.pagination.currentPage,
        pageSize: this.pagination.pageSize,
        keyword: this.searchForm.keyword || ''
      })
        .then(response => {
          if (response.data.code === 200) {
            this.courseList = response.data.data.data
            this.pagination.total = response.data.data.total
          } else {
            this.$message.error(response.data.message || '获取课程列表失败')
          }
        })
        .catch(error => {
          console.error('获取课程列表失败:', error)
          this.$message.error('获取课程列表失败')
        })
        .finally(() => {
          this.loading = false
        })
    },
    handleCurrentChange(page) {
      this.pagination.currentPage = page
      this.fetchCourseList()
    },
    handleSizeChange(size) {
      this.pagination.pageSize = size
      this.pagination.currentPage = 1
      this.fetchCourseList()
    },
    handleEdit(row) {
      this.dialogType = 'edit'
      this.dialogVisible = true
      this.courseForm = JSON.parse(JSON.stringify({
        ...this.getDefaultCourseForm(),
        ...row,
        chapters: row.chapters || []
      }))
    },
    handleAdd() {
      this.dialogType = 'add'
      this.dialogVisible = true
      this.resetForm()
    },
    resetForm() {
      if (this.$refs.courseForm) {
        this.$refs.courseForm.resetFields()
      }
      this.courseForm = this.getDefaultCourseForm()
      this.activeChapter = 0 // Reset collapse active state
    },
    submitForm() {
      this.$refs.courseForm.validate(valid => {
        if (!valid) return

        this.submitLoading = true
        const url = this.dialogType === 'edit' ? '/v1/course/update' : '/v1/course/add'
        const successMsg = this.dialogType === 'edit' ? '更新成功' : '新增成功'

        this.$http.post(url, this.courseForm)
          .then(response => {
            if (response.data.code === 200) {
              this.$message.success(successMsg)
              this.dialogVisible = false
              this.fetchCourseList()
            } else {
              this.$message.error(response.data.message || (this.dialogType === 'edit' ? '更新失败' : '新增失败'))
            }
          })
          .catch(error => {
            console.error(this.dialogType === 'edit' ? '更新失败:' : '新增失败:', error)
            this.$message.error(this.dialogType === 'edit' ? '更新失败' : '新增失败')
          })
          .finally(() => {
            this.submitLoading = false
          })
      })
    },
    handleSearch() {
      this.pagination.currentPage = 1
      this.fetchCourseList()
    },
    handleReset() {
      this.searchForm.keyword = ''
      this.handleSearch()
    },
    handleDelete(row) {
      this.$confirm(`确定要删除「${row.name}」吗？`, '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        this.$http.delete('/v1/course/del', { params: { id: row.id } })
          .then(response => {
            if (response.data.code === 200) {
              this.$message.success('删除成功')
              // Refresh list, go to first page if current page is empty after delete
              if (this.courseList.length === 1 && this.pagination.currentPage > 1) {
                  this.pagination.currentPage--;
              }
              this.fetchCourseList()
            } else {
              this.$message.error(response.data.message || '删除失败')
            }
          })
          .catch(error => {
            console.error('删除失败:', error)
            this.$message.error('删除失败')
          })
      })
    },
    beforeImageUpload(file) {
      const isImage = file.type.startsWith('image/')
      const isLt2M = file.size / 1024 / 1024 < 2

      if (!isImage) {
        this.$message.error('上传文件只能是图片格式!')
        return false
      }
      if (!isLt2M) {
        this.$message.error('上传图片大小不能超过 2MB!')
        return false
      }
      return true
    },
    uploadImage(options) {
      const formData = new FormData()
      formData.append('file', options.file)

      this.$http.post('/v1/common/file/upload', formData, {
        headers: {
          'Content-Type': 'multipart/form-data'
        }
      })
        .then(response => {
          if (response.data.code === 200) {
            this.courseForm.image = response.data.data
            options.onSuccess()
          } else {
            this.$message.error(response.data.message || '上传失败')
            options.onError()
          }
        })
        .catch(error => {
          console.error('上传图片失败:', error)
          this.$message.error('上传失败')
          options.onError()
        })
    },
    removeImage() {
      this.courseForm.image = ''
    },
    beforeCoursewareUpload(file) {
      const allowedTypes = [
        'application/msword',
        'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
        'application/pdf',
        'application/vnd.ms-powerpoint',
        'application/vnd.openxmlformats-officedocument.presentationml.presentation',
        'application/vnd.ms-excel',
        'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'
      ]
      const isAllowed = allowedTypes.includes(file.type)
      const isLt10M = file.size / 1024 / 1024 < 10

      if (!isAllowed) {
        this.$message.error('上传课件只能是 doc/docx/pdf/ppt/pptx/xls/xlsx 格式!')
        return false
      }
      if (!isLt10M) {
        this.$message.error('上传课件大小不能超过 10MB!')
        return false
      }
      return true
    },
    uploadCourseware(options) {
      const formData = new FormData()
      formData.append('file', options.file)

      this.$http.post('/v1/common/file/upload', formData, {
        headers: {
          'Content-Type': 'multipart/form-data'
        }
      })
        .then(response => {
          if (response.data.code === 200) {
            this.courseForm.courseware = response.data.data
            options.onSuccess()
          } else {
            this.$message.error(response.data.message || '上传失败')
            options.onError()
          }
        })
        .catch(error => {
          console.error('上传课件失败:', error)
          this.$message.error('上传失败')
          options.onError()
        })
    },
    removeCourseware() {
      this.courseForm.courseware = ''
    },
    addChapter() {
      this.courseForm.chapters.push({
        name: '',
        duration: '',
        lessons: []
      })
      // Open the newly added chapter
      this.activeChapter = this.courseForm.chapters.length - 1
    },
    removeChapter(index) {
      this.courseForm.chapters.splice(index, 1)
    },
    addLesson(chapterIndex) {
      // Ensure lessons array exists
      this.$set(this.courseForm.chapters[chapterIndex], 'lessons', this.courseForm.chapters[chapterIndex].lessons || [])
      this.courseForm.chapters[chapterIndex].lessons.push({
        name: '',
        video: '',
        duration: ''
      })
    },
    removeLesson(chapterIndex, lessonIndex) {
      this.courseForm.chapters[chapterIndex].lessons.splice(lessonIndex, 1)
    },
    beforeVideoUpload(file) {
      const isVideo = file.type.startsWith('video/')
      const isLt100M = file.size / 1024 / 1024 < 100

      if (!isVideo) {
        this.$message.error('上传文件只能是视频格式!')
        return false
      }
      if (!isLt100M) {
        this.$message.error('上传视频大小不能超过 100MB!')
        return false
      }
      return true
    },
    uploadLessonVideo(options, chapterIndex, lessonIndex) {
      const formData = new FormData()
      formData.append('file', options.file)

      this.$http.post('/v1/common/file/upload', formData, {
        headers: {
          'Content-Type': 'multipart/form-data'
        }
      })
        .then(response => {
          if (response.data.code === 200) {
            this.$set(this.courseForm.chapters[chapterIndex].lessons[lessonIndex], 'video', response.data.data)
            options.onSuccess()
          } else {
            this.$message.error(response.data.message || '上传失败')
            options.onError()
          }
        })
        .catch(error => {
          console.error('上传视频失败:', error)
          this.$message.error('上传失败')
          options.onError()
        })
    },
    removeLessonVideo(chapterIndex, lessonIndex) {
      if (this.courseForm.chapters[chapterIndex]?.lessons?.[lessonIndex]) {
        this.courseForm.chapters[chapterIndex].lessons[lessonIndex].video = ''
      }
    }
  }
}
</script>

<style scoped>
/* Keep existing styles for .course-container, .course-header, etc. */
.course-container {
  padding: 20px;
  background-color: #f0f2f5;
}

.course-header {
  margin-bottom: 20px;
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
}

.header-desc {
  color: #909399;
  font-size: 14px;
}

.search-container {
  background-color: #fff;
  padding: 18px 20px;
  border-radius: 8px;
  box-shadow: 0 1px 3px rgba(0,0,0,0.05);
  margin-bottom: 20px;
}

.search-form {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
}

:deep(.el-form-item) {
  margin-bottom: 0;
}

.course-content {
  background-color: #fff;
  padding: 20px;
  border-radius: 8px;
  box-shadow: 0 1px 3px rgba(0,0,0,0.05);
}

:deep(.el-table th) {
  background-color: #fafafa;
  color: #606266;
  font-weight: 600;
}

.course-name {
  font-weight: 500;
  color: #303133;
}

.pagination-container {
  margin-top: 20px;
  text-align: right;
}

.total-text {
  font-size: 13px;
  color: #606266;
  margin-right: 10px;
}

/* ==================== REFACTORED DIALOG STYLES ==================== */

:deep(.el-dialog) {
  border-radius: 8px;
}
:deep(.el-dialog__body) {
  padding: 20px 30px;
}

.course-form .form-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 0 25px; /* row-gap column-gap */
}
:deep(.course-form .el-form-item) {
  margin-bottom: 20px;
}

.course-form .full-width {
  grid-column: 1 / -1;
}

/* --- Image Uploader --- */
.image-uploader-icon {
  font-size: 28px;
  color: #8c939d;
  width: 150px;
  height: 150px;
  line-height: 150px;
  text-align: center;
}
.image-preview {
  width: 150px;
  height: 150px;
  display: block;
  object-fit: cover;
}
:deep(.image-uploader .el-upload) {
  border: 1px dashed #d9d9d9;
  border-radius: 6px;
  cursor: pointer;
  position: relative;
  overflow: hidden;
}
:deep(.image-uploader .el-upload:hover) {
  border-color: #409EFF;
}
.image-preview-wrapper {
  position: relative;
  width: 150px;
  height: 150px;
}
.image-preview-wrapper:hover .uploader-actions {
  opacity: 1;
}
.uploader-actions {
  position: absolute;
  width: 100%;
  height: 100%;
  left: 0;
  top: 0;
  cursor: default;
  display: inline-flex;
  justify-content: center;
  align-items: center;
  color: #fff;
  opacity: 0;
  font-size: 20px;
  background-color: rgba(0, 0, 0, .5);
  transition: opacity .3s;
}
.uploader-actions i {
  cursor: pointer;
}

/* --- Generic File Display --- */
.file-display-box {
  display: flex;
  align-items: center;
  padding: 5px 10px;
  background-color: #f5f7fa;
  border: 1px solid #e4e7ed;
  border-radius: 4px;
  font-size: 14px;
}
.file-display-box i:first-child {
  color: #909399;
  margin-right: 8px;
  font-size: 16px;
}
.file-name {
  flex: 1;
  color: #606266;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.file-delete-icon {
  margin-left: 10px;
  color: #909399;
  cursor: pointer;
  font-size: 16px;
}
.file-delete-icon:hover {
  color: #f56c6c;
}

/* --- Chapters and Lessons --- */
.chapters-container {
  padding: 15px;
  border: 1px solid #e4e7ed;
  border-radius: 4px;
  background-color: #fbfdff;
}
:deep(.chapter-collapse .el-collapse-item__header) {
  padding: 0 10px;
  height: 52px;
  line-height: 52px;
}
:deep(.chapter-collapse .el-collapse-item__wrap) {
  border-bottom: none;
}
:deep(.chapter-collapse .el-collapse-item__content) {
  padding-bottom: 10px;
}

.chapter-title-bar {
  display: flex;
  align-items: center;
  width: 100%;
  gap: 15px;
}
.chapter-index {
  font-weight: 500;
  color: #303133;
}
.chapter-name-input {
  flex: 1;
}
.delete-chapter-btn {
  margin-left: auto;
}
.empty-chapters {
  color: #909399;
  text-align: center;
  padding: 20px 0;
}

.lessons-area {
  padding: 10px 15px 0 35px;
}

.lesson-row {
  display: flex;
  align-items: center;
  gap: 15px;
  padding: 10px 0;
  border-bottom: 1px solid #f0f2f5;
}
.lesson-row:last-child {
  border-bottom: none;
}

.lesson-video-uploader {
  width: 250px;
}
.lesson-video-uploader .video-box {
  background-color: #e6f7ff;
  border-color: #91d5ff;
}
.lesson-video-uploader .video-box i:first-child {
  color: #1890ff;
}

</style>