<template>
  <main class="post-detail-brutal">
    <div class="container">
      <el-row>
        <el-col :span="24">
          <div class="nav-back-bar">
            <div class="back-btn-brutal" @click="goBack">
              <el-icon><ArrowLeft /></el-icon>
              <span>BACK TO FORUM</span>
            </div>
          </div>

          <div class="post-card-brutal main-post">
            <div class="post-header-section">
              <div class="author-badge-brutal">
                <div class="avatar-placeholder-lg">
                  <span>{{ postDetail.userName ? postDetail.userName.charAt(0).toUpperCase() : 'U' }}</span>
                </div>
                <div class="author-meta">
                  <span class="author-name-brutal">{{ postDetail.userName }}</span>
                </div>
              </div>
            </div>

            <h1 class="title-brutal">{{ postDetail.title }}</h1>

            <div class="tags-bar-brutal">
              <span v-for="tag in postDetail.tags" :key="tag" class="tag-item">
                #{{ tag }}
              </span>
            </div>

            <div class="content-block-brutal" v-html="formattedContent"></div>

            <div v-if="postDetail.images && postDetail.images.length > 0" class="images-grid-brutal">
              <div v-for="(img, index) in postDetail.images" :key="index" class="image-wrapper">
                <el-image :src="img" :preview-src-list="postDetail.images" :initial-index="index" fit="cover" />
              </div>
            </div>
          </div>

          <div class="reply-card-brutal">
            <div class="section-header-brutal">
              <h3>COMMENTS [{{ replies.length }}]</h3>
              <div class="stripe-border"></div>
            </div>

            <div class="reply-editor-brutal">
              <div class="editor-avatar">
                <div class="avatar-placeholder-sm">
                   <span>{{ currentUser.name ? currentUser.name.charAt(0).toUpperCase() : 'G' }}</span>
                </div>
              </div>
              <div class="editor-main">
                <el-input v-model="replyContent" type="textarea" :rows="4" placeholder="WRITE_YOUR_COMMENT_HERE..."
                  maxlength="500" show-word-limit resize="none" class="input-brutal" />
                <div class="editor-actions-brutal">
                  <el-button type="primary" @click="submitReply" :loading="replying" class="btn-brutal">
                    SUBMIT COMMENT
                  </el-button>
                </div>
              </div>
            </div>

            <div class="replies-list-brutal">
              <el-empty v-if="replies.length === 0" description="NO COMMENTS YET" :image-size="100" />

              <div v-for="reply in sortedReplies" :key="reply.id" class="reply-item-brutal">
                <div class="reply-avatar">
                   <div class="avatar-placeholder-xs">
                     <span>{{ reply.userName ? reply.userName.charAt(0).toUpperCase() : 'U' }}</span>
                   </div>
                </div>

                <div class="reply-main">
                  <div class="reply-meta-row">
                    <span class="username">{{ reply.userName }}</span>
                    <el-tag v-if="reply.isAuthor" size="small" effect="dark" class="author-tag">OP</el-tag>
                    <span class="time-stamp">{{ reply.time }}</span>
                  </div>

                  <div class="reply-text-content">{{ reply.content }}</div>

                  <div v-if="reply.images && reply.images.length > 0" class="reply-images">
                    <el-image v-for="(img, index) in reply.images" :key="index" :src="img"
                      :preview-src-list="reply.images" fit="cover" class="r-img" />
                  </div>

                  <div class="reply-actions-row">
                    <div class="action-btn-brutal" @click="showReplyToReply(reply)">
                      <el-icon><ChatDotRound /></el-icon>
                      <span>REPLY</span>
                    </div>
                  </div>

                  <div v-if="reply.replies && reply.replies.length > 0" class="nested-section">
                    <div v-for="nested in reply.replies" :key="nested.id" class="nested-item">
                      <div class="nested-avatar">
                        <div class="dot-avatar"></div>
                      </div>
                      <div class="nested-content">
                        <div class="nested-meta">
                          <span class="n-user">{{ nested.userName }}</span>
                          <span class="arrow-icon">→</span>
                          <span class="n-reply-to">{{ nested.replyTo }}</span>
                          <span class="n-time">{{ nested.time }}</span>
                        </div>
                        <div class="nested-text">{{ nested.content }}</div>
                      </div>
                    </div>

                    <div v-if="reply.replies && reply.replies.length > 3" class="load-more-brutal">
                      <span @click="loadMoreReplies(reply)">LOAD MORE</span>
                    </div>
                  </div>

                  <transition name="el-fade-in">
                    <div v-if="replyingTo === reply.id" class="nested-editor-brutal">
                      <el-input v-model="nestedReplyContent" type="textarea" :rows="2"
                        :placeholder="`REPLY_TO_${reply.userName}`" maxlength="500" resize="none" class="input-brutal" />
                      <div class="nested-actions-brutal">
                        <el-button size="small" @click="replyingTo = null" plain class="btn-brutal-outline">CANCEL</el-button>
                        <el-button size="small" type="primary" @click="submitNestedReply(reply)" class="btn-brutal">SEND</el-button>
                      </div>
                    </div>
                  </transition>
                </div>
              </div>
            </div>
          </div>
        </el-col>
      </el-row>
    </div>
  </main>
</template>

<script>
import {
  ArrowLeft, ChatDotRound
} from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { getForumPostDetailAPI, getCommentsAPI, addCommentAPI } from '@/utils/api.js'
import { useUserStore } from '@/stores/user'

export default {
  name: 'PostDetail',
  components: {
    ArrowLeft, ChatDotRound
  },
  data() {
    const userStore = useUserStore()
    const currentUser = userStore.user ? {
      name: userStore.user.nickname || userStore.user.username || '匿名用户',
      avatar: userStore.user.avatar || ''
    } : {
      name: '游客',
      avatar: ''
    }

    return {
      postId: null,
      replyContent: '',
      replying: false,
      replyingTo: null,
      nestedReplyContent: '',

      postDetail: {
        id: null,
        userName: '',
        userAvatar: '',
        title: '',
        content: '',
        images: [],
        tags: [],
        views: 0,
        likes: 0,
        time: '',
        isEssence: false,
        isHot: false
      },

      replies: [],

      currentUser: currentUser
    }
  },

  computed: {
    formattedContent() {
      if (!this.postDetail.content) return ''
      return this.postDetail.content
        .replace(/### (.*)/g, '<h3>$1</h3>')
        .replace(/## (.*)/g, '<h2>$1</h2>')
        .replace(/\n\n/g, '</p><p>')
        .replace(/\n/g, '<br>')
        .replace(/^(.*)$/, '<p>$1</p>')
    },

    sortedReplies() {
      return this.replies
    }
  },

  mounted() {
    this.postId = this.$route.params.id
    this.loadPostDetail()
  },

  methods: {
    async loadPostDetail() {
      if (!this.postId) return

      try {
        const response = await getForumPostDetailAPI(this.postId)
        if (response) {
          this.postDetail = {
            id: response.id,
            userName: response.author || '匿名用户',
            userAvatar: response.avatar || '',
            title: response.title || '无标题',
            content: response.content || '',
            images: response.imageArr || [],
            tags: response.tags || [],
            views: response.views || 0,
            likes: response.likes || 0,
            time: response.createTime || '未知时间',
            isEssence: response.isEssence || false,
            isHot: (response.views > 1000)
          }
        }
      } catch (error) {
        console.error('获取帖子详情失败:', error)
        ElMessage.error('获取帖子详情失败')
      } finally {
        this.loadComments()
      }
    },

    async loadComments() {
      if (!this.postId) return
      try {
        const response = await getCommentsAPI(this.postId)
        const commentList = Array.isArray(response) ? response : []
        this.replies = commentList.map(comment => this.normalizeRootComment(comment))
        this.replyingTo = null
      } catch (error) {
        console.error('获取评论列表失败:', error)
      }
    },

    normalizeRootComment(comment) {
      const nestedReplies = this.collectNestedReplies(comment.children || [])
      return {
        id: String(comment.id),
        userName: comment.sender,
        userAvatar: comment.senderAvatar,
        content: comment.content,
        time: this.formatTime(comment.createTime),
        likes: comment.likes || 0,
        replies: nestedReplies
      }
    },

    normalizeNestedReply(reply) {
      return {
        id: String(reply.id),
        userName: reply.sender,
        userAvatar: reply.senderAvatar,
        replyTo: reply.receiver || '',
        content: reply.content,
        time: this.formatTime(reply.createTime)
      }
    },

    collectNestedReplies(children, container = []) {
      if (!Array.isArray(children) || children.length === 0) {
        return container
      }
      children.forEach(child => {
        container.push(this.normalizeNestedReply(child))
        this.collectNestedReplies(child.children || [], container)
      })
      return container
    },

    getNumericId(value) {
      const parsed = Number(value)
      return Number.isNaN(parsed) ? value : parsed
    },

    resetReplyEditor() {
      this.replyingTo = null
      this.nestedReplyContent = ''
    },

    formatTime(timeString) {
      if (!timeString) return ''
      return timeString.includes(' ') ? timeString.split(' ')[0] : timeString
    },

    goBack() {
      this.$router.back()
    },

    async submitReply() {
      if (!this.replyContent.trim()) {
        ElMessage.warning('请输入回复内容')
        return
      }

      this.replying = true

      try {
        const commentData = {
          correlationId: this.postId,
          parentId: null,
          sender: this.currentUser.name || '匿名用户',
          senderAvatar: this.currentUser.avatar || '',
          receiver: '',
          content: this.replyContent,
          belong: 'post'
        }

        const response = await addCommentAPI(commentData)

        if (response) {
          const comment = {
            id: Date.now(),
            userName: this.currentUser.name,
            userAvatar: this.currentUser.avatar,
            content: this.replyContent,
            time: '刚刚',
            likes: 0,
            replies: []
          }
          void comment
          await this.loadComments()
          this.resetReplyEditor()
          this.replyContent = ''
          ElMessage.success('评论发表成功')
        } else {
          ElMessage.error('评论发表失败')
        }
      } catch (error) {
        console.error('发表评论失败:', error)
        ElMessage.error('发表评论失败')
      } finally {
        this.replying = false
      }
    },

    showReplyToReply(reply) {
      this.replyingTo = String(reply.id)
      this.nestedReplyContent = ''
    },

    async submitNestedReply(reply) {
      if (!this.nestedReplyContent.trim()) {
        ElMessage.warning('请输入回复内容')
        return
      }

      try {
        const replyData = {
          correlationId: this.postId,
          parentId: this.getNumericId(reply.id),
          sender: this.currentUser.name || '匿名用户',
          senderAvatar: this.currentUser.avatar || '',
          receiver: reply.userName,
          content: this.nestedReplyContent,
          belong: 'post'
        }

        const response = await addCommentAPI(replyData)

        if (response) {
          if (!reply.replies) reply.replies = []

          const newReply = {
            id: Date.now(),
            userName: this.currentUser.name,
            userAvatar: this.currentUser.avatar,
            replyTo: reply.userName,
            content: this.nestedReplyContent,
            time: '刚刚'
          }
          void newReply
          await this.loadComments()
          ElMessage.success('回复成功')
          this.resetReplyEditor()
        }
      } catch (error) {
        console.error('发表回复失败:', error)
        ElMessage.error('发表回复失败')
      }
    },

    loadMoreReplies() {
      ElMessage.info('没有更多回复了')
    }
  }
}
</script>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=JetBrains+Mono:wght@400;600;700&display=swap');

.post-detail-brutal {
  background-color: #E5E5E5;
  min-height: 100vh;
  padding: 40px 0;
  font-family: 'JetBrains Mono', monospace;
  color: #111;
}

.container {
  max-width: 900px;
  margin: 0 auto;
  padding: 0 20px;
}

.nav-back-bar {
  margin-bottom: 30px;
}

.back-btn-brutal {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  background: #fff;
  border: 2px solid #111;
  padding: 10px 20px;
  font-weight: 700;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.1s;
  text-transform: uppercase;
}

.back-btn-brutal:hover {
  background: #111;
  color: #fff;
}

.post-card-brutal.main-post {
  background: #fff;
  border: 2px solid #111;
  padding: 40px;
  margin-bottom: 40px;
  box-shadow: 10px 10px 0 #111;
}

.author-badge-brutal {
  display: flex;
  align-items: center;
  gap: 20px;
  margin-bottom: 30px;
  padding-bottom: 20px;
  border-bottom: 2px solid #111;
}

.avatar-placeholder-lg {
  width: 48px;
  height: 48px;
  background: #111;
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  font-weight: 700;
  border: 2px solid #111;
}

.author-name-brutal {
  font-size: 1.1rem;
  font-weight: 700;
  text-transform: uppercase;
}

.title-brutal {
  font-size: 2.5rem;
  font-weight: 700;
  margin: 0 0 20px 0;
  line-height: 1.2;
  text-transform: uppercase;
}

.tags-bar-brutal {
  display: flex;
  gap: 10px;
  margin-bottom: 30px;
}

.tag-item {
  padding: 4px 10px;
  border: 1px solid #111;
  font-size: 12px;
  font-weight: 600;
}

.content-block-brutal {
  font-size: 1rem;
  line-height: 1.7;
  margin-bottom: 30px;
  color: #333;
}

.content-block-brutal :deep(h2),
.content-block-brutal :deep(h3) {
  margin-top: 1.5em;
  margin-bottom: 0.5em;
  text-transform: uppercase;
  border-left: 4px solid #111;
  padding-left: 10px;
}

.images-grid-brutal {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 10px;
  border-top: 2px dashed #ccc;
  padding-top: 20px;
}

.image-wrapper {
  border: 1px solid #111;
  aspect-ratio: 1;
  overflow: hidden;
}

.image-wrapper :deep(.el-image) {
  width: 100%;
  height: 100%;
}

.image-wrapper :deep(img) {
  width: 100%;
  height: 100%;
  object-fit: cover;
  filter: grayscale(100%);
  transition: filter 0.3s;
}

.image-wrapper:hover :deep(img) {
  filter: grayscale(0%);
}

/* Reply Section */
.reply-card-brutal {
  background: #fff;
  border: 2px solid #111;
  padding: 30px;
}

.section-header-brutal h3 {
  font-size: 1.2rem;
  margin: 0 0 10px 0;
  text-transform: uppercase;
}

.stripe-border {
  height: 4px;
  background: repeating-linear-gradient(45deg, #111, #111 2px, transparent 2px, transparent 4px);
  margin-bottom: 30px;
}

.reply-editor-brutal {
  display: flex;
  gap: 20px;
  padding-bottom: 30px;
  margin-bottom: 30px;
  border-bottom: 2px solid #111;
}

.avatar-placeholder-sm {
  width: 40px;
  height: 40px;
  background: #111;
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 700;
  border: 2px solid #111;
}

.editor-main {
  flex: 1;
}

.input-brutal :deep(.el-textarea__inner) {
  border: 2px solid #111;
  border-radius: 0;
  font-family: 'JetBrains Mono', monospace;
  box-shadow: none;
  background: #fff;
  color: #111;
}

.input-brutal :deep(.el-textarea__inner:focus) {
  border-color: #111;
}

.editor-actions-brutal {
  margin-top: 15px;
  text-align: right;
}

.btn-brutal {
  background: #111;
  border: 2px solid #111;
  color: #fff;
  font-family: 'JetBrains Mono', monospace;
  font-weight: 700;
  text-transform: uppercase;
  border-radius: 0;
}

.btn-brutal:hover {
  background: #333;
  border-color: #333;
}

.btn-brutal-outline {
  background: transparent;
  border: 2px solid #111;
  color: #111;
  font-family: 'JetBrains Mono', monospace;
  font-weight: 700;
  border-radius: 0;
}

/* Reply List */
.replies-list-brutal {
  display: flex;
  flex-direction: column;
  gap: 25px;
}

.reply-item-brutal {
  display: flex;
  gap: 15px;
}

.avatar-placeholder-xs {
  width: 32px;
  height: 32px;
  background: #ddd;
  color: #111;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 700;
  font-size: 14px;
  border: 1px solid #111;
}

.reply-main {
  flex: 1;
  border-bottom: 1px solid #eee;
  padding-bottom: 20px;
}

.reply-meta-row {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 10px;
}

.username {
  font-weight: 700;
  text-transform: uppercase;
  font-size: 0.9rem;
}

.author-tag {
  background: #111;
  color: #fff;
  border: none;
  border-radius: 0;
  font-size: 10px;
  font-weight: 700;
}

.time-stamp {
  font-size: 0.8rem;
  color: #999;
  margin-left: auto;
}

.reply-text-content {
  line-height: 1.6;
  margin-bottom: 10px;
  font-size: 0.95rem;
}

.reply-actions-row {
  display: flex;
  gap: 15px;
}

.action-btn-brutal {
  display: flex;
  align-items: center;
  gap: 5px;
  font-size: 12px;
  font-weight: 700;
  cursor: pointer;
  text-transform: uppercase;
}

.action-btn-brutal:hover {
  text-decoration: underline;
}

/* Nested Section */
.nested-section {
  margin-top: 20px;
  border-left: 2px solid #111;
  padding-left: 20px;
  background: #fafafa;
  padding: 15px;
}

.nested-item {
  display: flex;
  gap: 10px;
  margin-bottom: 15px;
}

.dot-avatar {
  width: 8px;
  height: 8px;
  background: #111;
  margin-top: 6px;
}

.nested-meta {
  font-size: 0.85rem;
  margin-bottom: 4px;
  display: flex;
  align-items: center;
  gap: 6px;
}

.n-user {
  font-weight: 700;
}

.arrow-icon {
  color: #999;
}

.n-reply-to {
  font-weight: 600;
  color: #666;
}

.n-time {
  margin-left: auto;
  color: #bbb;
  font-size: 0.75rem;
}

.nested-text {
  font-size: 0.9rem;
  line-height: 1.5;
}

.load-more-brutal {
  font-size: 0.8rem;
  text-transform: uppercase;
  font-weight: 700;
  cursor: pointer;
  text-align: center;
  margin-top: 10px;
  padding: 5px;
  border: 1px dashed #ccc;
}

.nested-editor-brutal {
  margin-top: 20px;
  background: #f0f0f0;
  padding: 15px;
  border: 1px solid #111;
}

.nested-actions-brutal {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 10px;
}

@media (max-width: 768px) {
  .post-card-brutal.main-post {
    padding: 20px;
  }
  
  .title-brutal {
    font-size: 1.8rem;
  }
  
  .images-grid-brutal {
    grid-template-columns: 1fr;
  }
}
</style>
