<template>
  <main class="brutalist-community">
    <!-- Top Banner -->
    <section class="hero-poster-section">
      <div class="container">
        <div class="poster-content">
          <div class="poster-label">SYS.FORUM</div>
          <h1 class="poster-title">
            <span class="bracket">[</span>
            计算机学习交流社区
            <span class="bracket">]</span>
          </h1>
          <div class="poster-sub">DEBUG YOUR CODE, SHARE YOUR WORLD</div>
        </div>
        <div class="decorative-grid"></div>
      </div>
    </section>

    <div class="container main-body">
      <el-row :gutter="0">
        <el-col :span="24">
          <div class="action-bar-brutal">
            <div class="filter-left">
              <span class="filter-label">FILTER:</span>
              <div class="filter-tabs-brutal">
                <span class="tab-brutal active" :class="{ active: activeTab === 'latest' }"
                  @click="activeTab = 'latest'; handleTabChange()">
                  LATEST
                </span>
              </div>
            </div>

            <div class="controls-right">
              <div class="view-group-brutal">
                <div class="view-btn-brutal" :class="{ active: viewMode === 'list' }" @click="viewMode = 'list'">
                  <el-icon><Menu /></el-icon>
                </div>
                <div class="view-btn-brutal" :class="{ active: viewMode === 'card' }" @click="viewMode = 'card'">
                  <el-icon><Grid /></el-icon>
                </div>
              </div>

              <el-button type="primary" class="post-btn-brutal" @click="showPostDialog = true">
                <el-icon><Edit /></el-icon>
                <span>NEW POST</span>
              </el-button>
            </div>
          </div>

          <!-- Loading -->
          <div v-if="loading" class="loading-box">
            <div class="loading-spinner-brutal"></div>
            <p>COMPILING DATA...</p>
          </div>

          <!-- Posts Container -->
          <div v-else :class="['posts-container-brutal', `mode-${viewMode}`]">
            <div v-for="post in displayedPosts" :key="post.id" class="post-card-brutal" @click="viewPost(post)">
              <!-- Card Cover -->
              <div v-if="viewMode === 'card' && post.images && post.images.length > 0" class="card-img-box">
                <img :src="post.images[0]" alt="cover" />
                <div class="img-count-badge" v-if="post.images.length > 1">
                  +{{ post.images.length }}
                </div>
              </div>

              <div class="post-content-box">
                <div class="post-top-bar">
                  <div class="user-info-brutal">
                    <div class="avatar-placeholder">
                      <span>{{ post.userName ? post.userName.charAt(0).toUpperCase() : 'U' }}</span>
                    </div>
                    <div class="user-text">
                      <span class="name-text">@{{ post.userName }}</span>
                      <span class="time-text">{{ post.time }}</span>
                    </div>
                  </div>
                </div>

                <h3 class="title-text-brutal">{{ post.title }}</h3>

                <!-- List Excerpt -->
                <p class="excerpt-text-brutal">{{ post.excerpt }}</p>

                <!-- List Images -->
                <div v-if="viewMode === 'list' && post.images && post.images.length > 0" class="list-img-strip">
                  <div v-for="(img, index) in post.images.slice(0, 3)" :key="index" class="strip-img">
                    <img :src="img" :alt="`img${index}`" />
                  </div>
                  <div v-if="post.images.length > 3" class="strip-more">
                    +{{ post.images.length - 3 }}
                  </div>
                </div>

                <div class="post-bottom-bar">
                  <div class="tags-strip">
                    <span v-for="tag in post.tags.slice(0, viewMode === 'card' ? 2 : 4)" :key="tag" class="tag-item-brutal">
                      #{{ tag }}
                    </span>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <!-- Pagination -->
          <div class="pagination-box-brutal">
            <el-pagination v-model:current-page="currentPage" :page-size="pageSize" :total="totalPosts" background
              layout="prev, pager, next" @current-change="handlePageChange" class="pager-brutal" />
          </div>
        </el-col>
      </el-row>
    </div>

    <!-- New Post Dialog -->
    <el-dialog v-model="showPostDialog" title="CREATE NEW THREAD" width="700px" :close-on-click-modal="false"
      class="dialog-brutal">
      <el-form :model="postForm" label-position="top" class="form-brutal">
        <el-form-item label="TITLE" required>
          <el-input v-model="postForm.title" placeholder="INPUT_TITLE_HERE" maxlength="50" show-word-limit />
        </el-form-item>

        <el-form-item label="TAGS" required>
          <el-select
            v-model="postForm.tags"
            multiple
            filterable
            allow-create
            default-first-option
            reserve-keyword
            :teleported="false"
            placeholder="TYPE_TAG_AND_PRESS_ENTER"
            style="width: 100%"
          >
            <el-option v-for="tag in availableTags" :key="tag" :label="tag" :value="tag" />
          </el-select>
          <div class="tag-hint">Support custom tags. Type and press Enter to add.</div>
        </el-form-item>

        <el-form-item label="CONTENT" required>
          <el-input v-model="postForm.content" type="textarea" :rows="10"
            placeholder="WRITE_CONTENT_HERE..." maxlength="2000" show-word-limit />
        </el-form-item>

        <el-form-item label="FILES">
          <el-upload action="#" list-type="picture-card" :auto-upload="false" :limit="9" :file-list="postForm.images"
            :on-change="handleImageUpload" :on-remove="handleImageRemove">
            <el-icon><Plus /></el-icon>
          </el-upload>
        </el-form-item>
      </el-form>

      <template #footer>
        <div class="dialog-actions-brutal">
          <el-button @click="showPostDialog = false" plain>CANCEL</el-button>
          <el-button type="primary" @click="submitPost" :loading="posting">
            COMMIT
          </el-button>
        </div>
      </template>
    </el-dialog>
  </main>
</template>

<script>
import { Edit, Plus, Menu, Grid } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { getForumPostsAPI, addForumPostAPI, uploadFileAPI } from '@/utils/api.js'

const createDefaultPostForm = () => ({
  title: '',
  content: '',
  tags: [],
  images: [],
  imageUrls: []
})

export default {
  name: 'Community',
  components: {
    Edit,
    Plus,
    Menu,
    Grid
  },
  data() {
    return {
      activeTab: 'latest',
      viewMode: 'list',
      currentPage: 1,
      pageSize: 8,
      totalPosts: 0,
      showPostDialog: false,
      posting: false,
      postForm: createDefaultPostForm(),
      availableTags: ['提问', '经验分享', '学习笔记'],
      posts: [],
      loading: false
    }
  },
  mounted() {
    this.fetchPosts()
  },
  computed: {
    displayedPosts() {
      return [...this.posts]
    }
  },
  methods: {
    normalizeTags(tags) {
      if (!Array.isArray(tags)) return []
      return [...new Set(tags.map(tag => String(tag || '').trim()).filter(Boolean))].slice(0, 10)
    },

    collectAvailableTags(posts) {
      const tagSet = new Set(this.availableTags)
      posts.forEach(post => {
        this.normalizeTags(post.tags).forEach(tag => tagSet.add(tag))
      })
      return Array.from(tagSet).slice(0, 50)
    },

    normalizeImageUrls(urls) {
      if (!Array.isArray(urls)) return []
      return urls.map(url => String(url || '').trim()).filter(Boolean)
    },

    generateExcerpt(content) {
      if (!content || typeof content !== 'string') return ''
      const cleanContent = content.replace(/[#*`~\[\]()<>]/g, '').replace(/\n/g, ' ')
      return cleanContent.length > 100 ? `${cleanContent.substring(0, 100)}...` : cleanContent
    },

    formatTime(time) {
      if (!time) return '刚刚'
      const normalizedTime = typeof time === 'string' ? time.replace(' ', 'T') : time
      const date = new Date(normalizedTime)
      if (Number.isNaN(date.getTime())) return String(time)
      return date.toLocaleDateString()
    },

    resetPostForm() {
      this.postForm = createDefaultPostForm()
    },

    async fetchPosts() {
      this.loading = true
      try {
        const params = {
          currentPage: this.currentPage,
          pageSize: this.pageSize,
          category: ''
        }
        const response = await getForumPostsAPI(params)
        const list = response && Array.isArray(response.data) ? response.data : []

        this.posts = list.map(item => ({
          id: item.id,
          userName: item.author || '匿名用户',
          userAvatar: item.avatar || '',
          title: item.title || '无标题',
          excerpt: this.generateExcerpt(item.content),
          content: item.content || '',
          images: Array.isArray(item.imageArr) ? item.imageArr : [],
          tags: this.normalizeTags(item.tags),
          views: item.views || 0,
          likes: item.likes || 0,
          time: this.formatTime(item.createTime),
          isEssence: Boolean(item.isEssence),
          isHot: (item.views || 0) > 1000
        }))

        this.totalPosts = typeof response?.total === 'number' ? response.total : list.length
        this.availableTags = this.collectAvailableTags(this.posts)
      } catch (error) {
        console.error('fetch forum posts failed:', error)
        ElMessage.error('获取帖子列表失败，请稍后重试')
        this.posts = []
        this.totalPosts = 0
      } finally {
        this.loading = false
      }
    },

    handleTabChange() {
      this.currentPage = 1
      this.fetchPosts()
    },

    handlePageChange(page) {
      this.currentPage = page
      this.fetchPosts()
      window.scrollTo({ top: 0, behavior: 'smooth' })
    },

    viewPost(post) {
      if (post && post.id) {
        this.$router.push(`/post/${post.id}`)
        return
      }
      console.error('invalid post data:', post)
      ElMessage.error('无法查看帖子详情')
    },

    async handleImageUpload(file) {
      if (!file || !file.raw) return
      try {
        const imageUrl = await uploadFileAPI(file.raw)
        if (!imageUrl) return
        file.url = imageUrl
        if (!this.postForm.imageUrls.includes(imageUrl)) {
          this.postForm.imageUrls.push(imageUrl)
        }
      } catch (error) {
        ElMessage.error(`图片上传失败: ${error.message || '未知错误'}`)
      }
    },

    handleImageRemove(file) {
      const removedUrl = file && file.url ? file.url : ''
      if (!removedUrl) return
      this.postForm.imageUrls = this.postForm.imageUrls.filter(url => url !== removedUrl)
    },

    async submitPost() {
      const title = String(this.postForm.title || '').trim()
      const content = String(this.postForm.content || '').trim()
      const tags = this.normalizeTags(this.postForm.tags)
      const imageArr = this.normalizeImageUrls(this.postForm.imageUrls)

      if (!title) {
        ElMessage.warning('请输入帖子标题')
        return
      }

      if (!content) {
        ElMessage.warning('请输入帖子内容')
        return
      }

      if (tags.length === 0) {
        ElMessage.warning('请至少添加一个标签')
        return
      }

      this.posting = true
      try {
        const postData = {
          bio: '',
          title,
          content,
          imageArr,
          tags
        }

        const response = await addForumPostAPI(postData)
        const isSuccess = response === true
        if (!isSuccess) {
          ElMessage.error('帖子发布失败')
          return
        }

        ElMessage.success('帖子发布成功')
        this.showPostDialog = false
        this.resetPostForm()
        this.currentPage = 1
        await this.fetchPosts()
      } catch (error) {
        ElMessage.error(`发布帖子失败: ${error.message || '未知错误'}`)
      } finally {
        this.posting = false
      }
    }
  }
}
</script>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=JetBrains+Mono:wght@400;600;700&display=swap');

.brutalist-community {
  background-color: #E5E5E5;
  min-height: 100vh;
  font-family: 'JetBrains Mono', monospace;
  color: #111;
}

.container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 20px;
}

/* Hero Section */
.hero-poster-section {
  background: #111;
  color: #fff;
  padding: 100px 0;
  position: relative;
  overflow: hidden;
  border-bottom: 4px solid #111;
}

.decorative-grid {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-image: linear-gradient(rgba(255, 255, 255, 0.05) 1px, transparent 1px),
    linear-gradient(90deg, rgba(255, 255, 255, 0.05) 1px, transparent 1px);
  background-size: 40px 40px;
  pointer-events: none;
}

.poster-content {
  position: relative;
  z-index: 1;
  border: 2px solid #fff;
  padding: 40px;
  display: inline-block;
  background: rgba(0, 0, 0, 0.2);
}

.poster-label {
  font-size: 12px;
  letter-spacing: 4px;
  margin-bottom: 20px;
  opacity: 0.7;
}

.poster-title {
  font-size: 3.5rem;
  font-weight: 700;
  margin: 0 0 20px 0;
  text-transform: uppercase;
  line-height: 1;
}

.poster-title .bracket {
  color: #666;
}

.poster-sub {
  font-size: 1rem;
  letter-spacing: 2px;
  color: #888;
}

/* Action Bar */
.action-bar-brutal {
  background: #fff;
  border: 2px solid #111;
  padding: 15px 20px;
  margin: 40px 0 30px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  box-shadow: 8px 8px 0 #111;
}

.filter-left {
  display: flex;
  align-items: center;
  gap: 15px;
}

.filter-label {
  font-weight: 700;
  font-size: 14px;
}

.tab-brutal {
  padding: 8px 16px;
  border: 2px solid #111;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.1s;
  font-size: 12px;
}

.tab-brutal.active {
  background: #111;
  color: #fff;
}

.controls-right {
  display: flex;
  align-items: center;
  gap: 20px;
}

.view-group-brutal {
  display: flex;
  gap: 5px;
}

.view-btn-brutal {
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 2px solid #111;
  cursor: pointer;
  transition: all 0.1s;
}

.view-btn-brutal.active {
  background: #111;
  color: #fff;
}

.post-btn-brutal {
  background: #111;
  border: none;
  color: #fff;
  font-weight: 700;
  text-transform: uppercase;
  font-family: inherit;
}

.post-btn-brutal:hover {
  background: #333;
}

/* Loading */
.loading-box {
  text-align: center;
  padding: 80px 0;
}

.loading-spinner-brutal {
  width: 40px;
  height: 40px;
  border: 4px solid #111;
  border-top-color: transparent;
  border-radius: 50%;
  animation: spin 1s linear infinite;
  margin: 0 auto 20px;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

/* Posts Grid */
.posts-container-brutal {
  margin-bottom: 60px;
}

.mode-list {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.mode-card {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 20px;
}

.post-card-brutal {
  background: #fff;
  border: 2px solid #111;
  transition: all 0.15s ease;
  cursor: pointer;
}

.post-card-brutal:hover {
  transform: translate(-4px, -4px);
  box-shadow: 8px 8px 0 #111;
}

.mode-list .post-card-brutal {
  display: flex;
}

.post-content-box {
  padding: 20px;
  flex: 1;
}

.card-img-box {
  width: 100%;
  height: 180px;
  border-bottom: 2px solid #111;
  overflow: hidden;
  position: relative;
}

.card-img-box img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  filter: grayscale(100%);
  transition: filter 0.3s;
}

.post-card-brutal:hover .card-img-box img {
  filter: grayscale(0%);
}

.img-count-badge {
  position: absolute;
  bottom: 10px;
  right: 10px;
  background: #111;
  color: #fff;
  padding: 2px 8px;
  font-size: 12px;
}

.user-info-brutal {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 15px;
}

.avatar-placeholder {
  width: 28px;
  height: 28px;
  background: #111;
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
}

.user-text {
  display: flex;
  flex-direction: column;
}

.name-text {
  font-weight: 700;
  font-size: 12px;
}

.time-text {
  font-size: 10px;
  color: #666;
}

.title-text-brutal {
  font-size: 1.1rem;
  font-weight: 700;
  margin: 0 0 10px 0;
  text-transform: capitalize;
}

.excerpt-text-brutal {
  font-size: 0.85rem;
  color: #444;
  line-height: 1.4;
  margin-bottom: 15px;
}

.list-img-strip {
  display: flex;
  gap: 10px;
  margin-bottom: 15px;
}

.strip-img {
  width: 60px;
  height: 60px;
  border: 1px solid #111;
}

.strip-img img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.strip-more {
  width: 60px;
  height: 60px;
  background: #ddd;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: 700;
}

.post-bottom-bar {
  border-top: 1px solid #eee;
  padding-top: 10px;
}

.tags-strip {
  display: flex;
  flex-wrap: wrap;
  gap: 5px;
}

.tag-item-brutal {
  font-size: 10px;
  padding: 2px 6px;
  border: 1px solid #111;
  color: #111;
}

.tag-hint {
  margin-top: 6px;
  font-size: 12px;
  color: #666;
  line-height: 1.4;
}

/* Dialog */
:deep(.dialog-brutal) {
  border: 2px solid #111;
}

:deep(.el-dialog__header) {
  background: #111;
  color: #fff;
  padding: 15px;
  margin-right: 0;
  border-bottom: 2px solid #111;
}

:deep(.el-dialog__title) {
  color: #fff;
  font-weight: 700;
  font-family: inherit;
}

:deep(.el-dialog__body) {
  padding: 20px;
  background: #fff;
}

:deep(.el-form-item__label) {
  font-family: inherit;
  font-weight: 700;
  font-size: 12px;
}

:deep(.el-input__wrapper),
:deep(.el-textarea__inner) {
  border: 2px solid #111;
  box-shadow: none;
  border-radius: 0;
}

:deep(.el-input__wrapper.is-focus),
:deep(.el-textarea__inner:focus) {
  border-color: #111;
  box-shadow: none;
}

.dialog-actions-brutal {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

.dialog-actions-brutal .el-button {
  border: 2px solid #111;
  font-family: inherit;
  font-weight: 700;
  border-radius: 0;
}

.dialog-actions-brutal .el-button--primary {
  background: #111;
  color: #fff;
}

/* Pagination */
.pagination-box-brutal {
  display: flex;
  justify-content: center;
  margin-top: 40px;
}

:deep(.pager-brutal.is-background .el-pager li) {
  background: #fff;
  border: 2px solid #111;
  border-radius: 0;
  margin: 0 4px;
  font-weight: 700;
  color: #111;
}

:deep(.pager-brutal.is-background .el-pager li.is-active) {
  background: #111;
  color: #fff;
}

@media (max-width: 768px) {
  .poster-title {
    font-size: 2rem;
  }

  .action-bar-brutal {
    flex-direction: column;
    gap: 15px;
    align-items: flex-start;
  }

  .controls-right {
    width: 100%;
    justify-content: space-between;
  }
}
</style>
