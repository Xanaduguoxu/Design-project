<template>
  <main class="course-detail-page">
    <div class="video-terminal-wrapper">
      <div class="terminal-header">
        <div class="terminal-dots">
          <span class="dot red"></span>
          <span class="dot yellow"></span>
          <span class="dot green"></span>
        </div>
        <div class="terminal-title">player.exe - {{ courseDetail.name }}</div>
      </div>
      <div class="course-video-section">
        <video 
          ref="videoPlayer"
          :src="currentVideoSrc" 
          :poster="courseDetail.cover"
          controls
          class="course-video"
          @play="handleVideoPlay"
          @pause="handleVideoPause"
          @ended="handleVideoPause"
        >
          您的浏览器不支持视频播放
        </video>
      </div>
    </div>

    <div class="container">
      <el-row :gutter="24">
        <el-col :span="16">
          <div class="course-info-panel tech-panel">
            <div class="course-header">
              <div class="course-title-section">
                <h1 class="course-title">
                  <span class="prompt">></span> {{ courseDetail.name }}<span class="cursor">_</span>
                </h1>
                <div class="level-badge">
                  <span class="bracket">[</span>
                  <span :class="['level-text', courseDetail.category]">{{ getCourseLevelDisplayValue(courseDetail.category) }}</span>
                  <span class="bracket">]</span>
                </div>
              </div>
              
              <div class="course-meta">
                <span class="meta-divider">|</span>
                <span class="meta-item">
                  <el-icon><View /></el-icon>
                  <span class="meta-label">Views:</span> {{ courseDetail.views }}
                </span>
              </div>
            </div>

            <div class="course-description">
              <h3 class="section-title">
                <span class="hash">#</span> README.md
              </h3>
              <div class="description-content">
                <p class="description-text">{{ courseDetail.brief }}</p>
              </div>
            </div>

            <div class="course-chapters">
              <h3 class="section-title">
                <span class="hash">#</span> SRC / CHAPTERS
              </h3>
              <el-collapse v-model="activeChapter" accordion class="tech-collapse">
                <el-collapse-item 
                  v-for="(chapter, index) in courseDetail.chapters" 
                  :key="chapter.id" 
                  :name="chapter.id"
                >
                  <template #title>
                    <div class="chapter-header">
                      <div class="folder-info">
                        <el-icon class="folder-icon"><Folder /></el-icon>
                        <span class="chapter-index">{{ String(index + 1).padStart(2, '0') }}_</span>
                        <span class="chapter-title">{{ chapter.title }}</span>
                      </div>
                      <span class="chapter-meta code-font">
                        {{ chapter.lessons.length }} files · {{ chapter.duration }}
                      </span>
                    </div>
                  </template>
                  <div class="chapter-content">
                    <div 
                      v-for="(lesson, lIndex) in chapter.lessons" 
                      :key="lesson.id" 
                      class="lesson-item"
                      @click="playLessonVideo(chapter, lesson)"
                    >
                      <div class="lesson-left">
                        <span class="line-number">{{ lIndex + 1 }}</span>
                        <!-- 高亮当前播放的小节 -->
                        <el-icon class="file-icon" :class="{ 'active-icon': currentLesson && currentLesson.id === lesson.id }">
                          <VideoCamera />
                        </el-icon>
                        <span class="lesson-title" :class="{ 'active-text': currentLesson && currentLesson.id === lesson.id }">
                          {{ lesson.title }}
                        </span>
                      </div>
                      <span class="lesson-duration">{{ lesson.duration }}</span>
                    </div>
                  </div>
                </el-collapse-item>
              </el-collapse>
            </div>
          </div>
        </el-col>
      </el-row>
    </div>
  </main>
</template>

<script>
import { View, VideoCamera, Folder } from '@element-plus/icons-vue'
import { getCourseDetailAPI, getDictionaryAPI, addLearningRecordAPI } from '@/utils/api.js'

export default {
  name: 'CourseDetail',
  components: { View, VideoCamera, Folder },
  data() {
    return {
      courseId: null,
      activeChapter: '',
      currentVideoSrc: '',
      // 初始化对象，防止报错
      currentChapter: null,
      currentLesson: null,
      recordTimer: null,
      
      courseLevelOptions: [],
      courseDetail: {
        id: null,
        name: '',
        category: '',
        cover: '',
        views: '',
        brief: '',
        courseware: '',
        chapters: []
      }
    }
  },
  
  mounted() {
    this.fetchCourseLevelDictionary()
    this.loadCourseDetail()
  },

  beforeUnmount() {
    this.stopRecording()
  },
  
  methods: {
    async fetchCourseLevelDictionary() {
      try {
        const response = await getDictionaryAPI('course_level')
        if (response && Array.isArray(response)) this.courseLevelOptions = response
      } catch (error) {
        this.courseLevelOptions = [{ code: 'beginner', value: '入门' }, { code: 'intermediate', value: '进阶' }, { code: 'advanced', value: '高阶' }]
      }
    },
    
    getCourseLevelDisplayValue(code) {
      const level = this.courseLevelOptions.find(item => item.code === code)
      return level ? level.value : code
    },
    
    // 切换章节视频
    playLessonVideo(chapter, lesson) {
      if (lesson.video) {
        this.stopRecording() // 切换前停止旧计时器

        this.currentChapter = chapter
        this.currentLesson = lesson
        this.currentVideoSrc = lesson.video
        
        this.$nextTick(() => {
          const videoSection = document.querySelector('.course-video-section')
          if (videoSection) videoSection.scrollIntoView({ behavior: 'smooth' })
          
          const videoEl = this.$refs.videoPlayer
          if(videoEl) {
            videoEl.load()
            videoEl.play().catch(e => console.log('自动播放拦截:', e))
          }
        })
      }
    },

    // 视频播放开始
    handleVideoPlay() {
      // 只要 currentLesson 有值（无论是刚才初始化的导读，还是点击的小节），都开始计时
      if (this.currentLesson && this.currentChapter) {
        this.stopRecording() // 防抖
        // 开启 10秒 循环
        this.recordTimer = setInterval(() => {
          this.sendLearningRecord()
        }, 10000)
      }
    },

    // 视频暂停/结束
    handleVideoPause() {
      this.stopRecording()
    },

    stopRecording() {
      if (this.recordTimer) {
        clearInterval(this.recordTimer)
        this.recordTimer = null
      }
    },

    // 发送记录
    async sendLearningRecord() {
      if (!this.currentLesson || !this.courseDetail.id) return

      const recordData = {
        learnId: this.courseDetail.id,
        name: this.courseDetail.name,
        // 如果是导读，这里会发送 "课程导读" 和 "视频概览/预告"
        chapters: this.currentChapter.title,
        lessons: this.currentLesson.title,
        duration: 10
      }

      try {
        await addLearningRecordAPI(recordData)
        // console.log('记录发送成功:', recordData)
      } catch (error) {
        console.error('记录发送失败:', error)
      }
    },
    
    async loadCourseDetail() {
      this.courseId = this.$route.params.id || 1
      try {
        const response = await getCourseDetailAPI(this.courseId)
        
        this.courseDetail = {
          id: response.id,
          name: response.name,
          category: response.category,
          cover: response.image,
          views: response.views,
          brief: response.brief,
          courseware: response.courseware,
          chapters: response.chapters.map(chapter => ({
            id: chapter.id,
            title: chapter.name,
            duration: chapter.duration,
            lessons: chapter.lessons.map(lesson => ({
              id: lesson.id,
              title: lesson.name,
              duration: lesson.duration,
              video: lesson.video 
            }))
          }))
        }
        
        // --- 核心优化部分 ---
        this.currentVideoSrc = response.courseware
        
        // 既然要记录导读视频的时长，这里必须手动构造一个“虚拟章节”对象
        // 这样 handleVideoPlay 里的 if 判断就会通过
        this.currentChapter = { title: '课程导读' }
        this.currentLesson = { 
          id: 'intro_001', 
          title: '视频概览/预告', 
          video: response.courseware 
        }
        
      } catch (error) {
        console.error('获取课程详情失败：', error)
      }
    }
  }
}
</script>

<style scoped>
/* 高亮样式 */
.active-icon { color: var(--tech-green) !important; }
.active-text { color: var(--tech-green) !important; font-weight: bold; }

/* 基础样式 */
:root {
  --tech-bg: #0f172a;
  --tech-panel-bg: #1e293b;
  --tech-border: #334155;
  --tech-blue: #38bdf8;
  --tech-green: #4ade80;
  --tech-text-main: #ffffff;
  --tech-text-sub: #cbd5e1;
  --font-mono: 'Fira Code', 'Consolas', 'Monaco', monospace;
  --font-sans: 'Inter', -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
}
.course-detail-page { background-color: var(--tech-bg); min-height: 100vh; padding-bottom: 60px; color: var(--tech-text-main); font-family: var(--font-sans); }
.video-terminal-wrapper { background: #000; border-bottom: 1px solid var(--tech-border); margin-bottom: 40px; box-shadow: 0 10px 30px rgba(0,0,0,0.5); }
.terminal-header { background: #1e1e1e; padding: 10px 20px; display: flex; align-items: center; border-bottom: 1px solid #333; }
.terminal-dots { display: flex; gap: 8px; margin-right: 20px; }
.dot { width: 12px; height: 12px; border-radius: 50%; }
.dot.red { background: #ff5f56; }
.dot.yellow { background: #ffbd2e; }
.dot.green { background: #27c93f; }
.terminal-title { color: var(--tech-text-sub); font-family: var(--font-mono); font-size: 0.85rem; }
.course-video-section { position: relative; height: 500px; background: #000; display: flex; align-items: center; justify-content: center; max-width: 1200px; margin: 0 auto; }
.course-video { width: 100%; height: 100%; object-fit: contain; }
.container { max-width: 1200px; margin: 0 auto; padding: 0 20px; }
.tech-panel { background: var(--tech-panel-bg); border: 1px solid var(--tech-border); border-radius: 8px; padding: 30px; margin-bottom: 30px; }
.course-header { margin-bottom: 40px; border-bottom: 1px solid var(--tech-border); padding-bottom: 20px; }
.course-title-section { margin-bottom: 15px; }
.course-title { font-family: var(--font-mono); font-size: 2rem; font-weight: 700; color: var(--tech-text-main); margin: 0 0 15px 0; line-height: 1.2; }
.prompt { color: var(--tech-blue); margin-right: 10px; }
.cursor { display: inline-block; width: 10px; height: 28px; background: var(--tech-blue); animation: blink 1s infinite; vertical-align: middle; margin-left: 5px; }
@keyframes blink { 0%, 100% { opacity: 1; } 50% { opacity: 0; } }
.level-badge { font-family: var(--font-mono); font-size: 0.9rem; display: inline-block; }
.level-badge .bracket { color: var(--tech-text-sub); }
.level-text { font-weight: 700; }
.level-text.beginner { color: var(--tech-green); }
.level-text.intermediate { color: #facc15; }
.level-text.advanced { color: #f87171; }
.course-meta { display: flex; gap: 15px; color: var(--tech-text-sub); font-family: var(--font-mono); font-size: 0.85rem; align-items: center; }
.meta-label { color: var(--tech-blue); margin-right: 5px; }
.meta-divider { color: #475569; }
.meta-item { display: flex; align-items: center; gap: 6px; }
.meta-item :deep(.el-icon) { color: var(--tech-text-sub); }
.section-title { font-family: var(--font-mono); font-size: 1.2rem; font-weight: 600; color: var(--tech-text-main); margin-bottom: 20px; display: flex; align-items: center; gap: 8px; }
.section-title .hash { color: var(--tech-blue); }
.description-content { background: rgba(15, 23, 42, 0.6); border: 1px solid var(--tech-border); padding: 20px; border-radius: 4px; border-left: 3px solid var(--tech-blue); margin-bottom: 30px; }
.description-text { color: var(--tech-text-main); line-height: 1.8; font-family: var(--font-mono); font-size: 0.95rem; margin: 0; }
.course-chapters { margin-top: 40px; }
:deep(.tech-collapse) { border: none; background: transparent; }
:deep(.el-collapse-item__header) { background: transparent; color: var(--tech-text-main); border-bottom: 1px solid var(--tech-border); height: auto; line-height: 1.5; padding: 16px 0; font-family: var(--font-mono); font-size: 1rem; transition: background 0.3s; }
:deep(.el-collapse-item__header:hover) { background: rgba(255,255,255,0.03); }
:deep(.el-collapse-item__arrow) { color: var(--tech-text-sub); transition: transform 0.3s; }
:deep(.el-collapse-item__wrap) { background: transparent; border-bottom: none; }
:deep(.el-collapse-item__content) { padding: 10px 0 20px 0; color: var(--tech-text-main); }
.chapter-header { display: flex; justify-content: space-between; width: 100%; align-items: center; padding-right: 10px; }
.folder-info { display: flex; align-items: center; gap: 10px; }
.folder-icon { color: #facc15; font-size: 1.2rem; }
.chapter-index { color: var(--tech-text-sub); font-size: 0.9rem; }
.chapter-title { font-weight: 600; color: var(--tech-text-main); }
.code-font { font-family: var(--font-mono); font-size: 0.85rem; color: var(--tech-text-sub); }
.lesson-item { display: flex; justify-content: space-between; align-items: center; padding: 12px 15px; border-left: 2px solid transparent; margin-left: 20px; transition: all 0.2s ease; cursor: pointer; font-family: var(--font-mono); font-size: 0.9rem; border-radius: 0 4px 4px 0; }
.lesson-item:hover { background: rgba(56, 189, 248, 0.1); border-left-color: var(--tech-blue); color: var(--tech-text-main); }
.lesson-left { display: flex; align-items: center; gap: 12px; }
.line-number { color: var(--tech-text-sub); width: 20px; text-align: right; font-size: 0.8rem; }
.file-icon { color: var(--tech-blue); }
.lesson-title { color: var(--tech-text-main); }
.lesson-duration { color: var(--tech-text-sub); font-size: 0.85rem; }
:deep(.el-icon) { color: inherit; }
</style>