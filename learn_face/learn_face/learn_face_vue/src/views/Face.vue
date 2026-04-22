<template>
  <div class="face-register-page">
    <div class="register-container">
      
      <!-- 头部导航/返回 -->
      <div class="header-bar">
        <el-button link @click="goBack" class="back-btn">
          <el-icon><ArrowLeft /></el-icon> 返回
        </el-button>
        <h2 class="title">绑定人脸信息</h2>
        <div class="placeholder"></div> <!-- 占位符保持标题居中 -->
      </div>

      <!-- 核心内容区 -->
      <div class="content-box">
        
        <!-- 步骤提示 -->
        <div class="steps-indicator" v-if="!isSuccess">
          <h3 v-if="status === 'idle'">第一步：开启摄像头</h3>
          <h3 v-else-if="status === 'ready'">第二步：正对摄像头，点击开始录入</h3>
          <h3 v-else-if="status === 'capturing'">第三步：录入中，请保持面部在取景框内...</h3>
          <h3 v-else-if="status === 'submitting'">正在生成面部模型，请稍候...</h3>
          <p class="sub-tip" v-if="status === 'ready' || status === 'capturing'">
            小贴士：录入时可以微微转动头部，以获取多角度特征
          </p>
        </div>

        <!-- 成功状态 -->
        <div class="success-box" v-if="isSuccess">
          <el-icon class="success-icon"><CircleCheckFilled /></el-icon>
          <h3>人脸录入成功！</h3>
          <p>您现在可以使用刷脸快捷登录系统了。</p>
          <el-button type="primary" class="minimal-btn finish-btn" @click="goBack">完成并返回</el-button>
        </div>

        <!-- 摄像头与取景框 -->
        <div class="camera-section" v-show="!isSuccess">
          <div class="camera-wrapper" :class="{ 'is-capturing': status === 'capturing' }">
            
            <video ref="videoRef" autoplay playsinline class="face-video" v-show="isCameraOn"></video>
            
            <!-- 未开启摄像头的占位图 -->
            <div class="camera-placeholder" v-show="!isCameraOn">
              <el-icon size="48" color="#a1a1a6"><Camera /></el-icon>
              <p>摄像头未开启</p>
            </div>

            <!-- 取景框UI -->
            <div class="scan-overlay" v-show="isCameraOn">
              <div class="scan-frame">
                <div class="scan-corner top-left"></div>
                <div class="scan-corner top-right"></div>
                <div class="scan-corner bottom-left"></div>
                <div class="scan-corner bottom-right"></div>
              </div>
            </div>

            <!-- 抓拍时的闪烁白光特效 -->
            <div class="flash-overlay" :class="{ 'flash-active': showFlash }"></div>
          </div>

          <!-- 进度条 (录入中显示) -->
          <div class="progress-container" v-show="status === 'capturing' || status === 'submitting'">
            <el-progress 
              :percentage="captureProgress" 
              :stroke-width="10" 
              :color="customColors" 
              :show-text="false"
            />
            <p class="progress-text">已采集 {{ capturedImages.length }} / {{ targetFrames }} 张</p>
          </div>
        </div>

        <!-- 底部操作按钮 -->
        <div class="action-footer" v-show="!isSuccess">
          <el-button 
            v-if="status === 'idle'" 
            type="primary" 
            class="minimal-btn start-cam-btn" 
            @click="startCamera"
          >
            允许访问摄像头
          </el-button>

          <el-button 
            v-if="status === 'ready'" 
            type="primary" 
            class="minimal-btn start-capture-btn" 
            @click="startCapture"
          >
            开始采集特征
          </el-button>

          <el-button 
            v-if="status === 'submitting'" 
            type="primary" 
            class="minimal-btn" 
            loading 
            disabled
          >
            正在加密上传...
          </el-button>
        </div>

      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onBeforeUnmount, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowLeft, Camera, CircleCheckFilled } from '@element-plus/icons-vue'
// import request from '../utils/request' // 替换为你的 axios 封装路径
import { apiRequest } from '../utils/api'

const router = useRouter()

// DOM 引用
const videoRef = ref(null)

// 状态管理
// idle(未开启) -> ready(已开启准备抓拍) -> capturing(抓拍中) -> submitting(上传中)
const status = ref('idle') 
const isCameraOn = ref(false)
const isSuccess = ref(false)
const showFlash = ref(false) // 拍照闪光灯特效

// 采集参数
const targetFrames = 15 // 总共需要采集多少张照片
const capturedImages = ref([]) // 存储 base64 数组
const captureProgress = ref(0)
let mediaStream = null
let captureTimer = null

// 进度条颜色渐变
const customColors = [
  { color: '#f56c6c', percentage: 20 },
  { color: '#e6a23c', percentage: 40 },
  { color: '#5cb87a', percentage: 80 },
  { color: '#1989fa', percentage: 100 },
]

// ================= 摄像头控制 =================
const startCamera = async () => {
  try {
    if (navigator.mediaDevices && navigator.mediaDevices.getUserMedia) {
      mediaStream = await navigator.mediaDevices.getUserMedia({ 
        video: { facingMode: "user", width: 640, height: 480 } 
      })
      
      isCameraOn.value = true
      status.value = 'ready'

      // 轮询绑定流，防止 DOM 未渲染完成
      const tryAttachStream = () => {
        if (videoRef.value) {
          videoRef.value.srcObject = mediaStream
        } else {
          setTimeout(tryAttachStream, 50)
        }
      }
      tryAttachStream()

    } else {
      ElMessage.error('当前浏览器不支持摄像头调用')
    }
  } catch (error) {
    ElMessage.error('无法访问摄像头，请检查浏览器权限设置')
  }
}

const stopCamera = () => {
  if (mediaStream) {
    mediaStream.getTracks().forEach(track => track.stop())
    mediaStream = null
    isCameraOn.value = false
  }
}

// ================= 采集逻辑 =================
const startCapture = () => {
  if (!videoRef.value) return
  
  status.value = 'capturing'
  capturedImages.value = []
  captureProgress.value = 0

  // 创建一个离屏 Canvas 用于截图
  const canvas = document.createElement('canvas')
  canvas.width = videoRef.value.videoWidth || 640
  canvas.height = videoRef.value.videoHeight || 480
  const ctx = canvas.getContext('2d')

  // 定时器：每 300 毫秒抓拍一次
  captureTimer = setInterval(() => {
    // 闪光灯特效
    showFlash.value = true
    setTimeout(() => { showFlash.value = false }, 50)

    // 绘制视频帧到 canvas
    ctx.drawImage(videoRef.value, 0, 0, canvas.width, canvas.height)
    
    // 转为 Base64 并存入数组 (压缩率0.8以减少数据量)
    const base64 = canvas.toDataURL('image/jpeg', 0.8)
    capturedImages.value.push(base64)

    // 更新进度条
    captureProgress.value = Math.floor((capturedImages.value.length / targetFrames) * 100)

    // 判断是否采集完成
    if (capturedImages.value.length >= targetFrames) {
      clearInterval(captureTimer)
      submitFaceData()
    }
  }, 300) // 300ms 间隔，15张大约需要 4.5 秒
}

// ================= 提交给 Spring Boot =================
const submitFaceData = async () => {
  status.value = 'submitting'
  
  try {
    // 调用 Spring Boot 接口 (假设你的后端接口是 /v1/user/face-register)
    // 注意：这里由于用户已经登录，你的 SpringBoot 应该能从 Header 的 JWT 中解析出当前是谁
    // 所以前端只需要传 images 数组过去即可
    await apiRequest('/auth/faceRegister', {
        method: 'POST',
        body: JSON.stringify({ imagesBase64: capturedImages.value })
    })

    // 成功处理
    stopCamera()
    isSuccess.value = true
    status.value = 'idle'
    
  } catch (error) {
    ElMessage.error(error.response?.data?.message || '面部数据上传失败，请重试')
    // 失败后允许重新录入
    status.value = 'ready' 
    capturedImages.value = []
    captureProgress.value = 0
  }
}

// ================= 路由与清理 =================
const goBack = () => {
  stopCamera()
  router.back() // 或者 router.push('/profile')
}

onBeforeUnmount(() => {
  if (captureTimer) clearInterval(captureTimer)
  stopCamera()
})
</script>

<style scoped>
/* 极简苹果风样式 */
.face-register-page {
  min-height: calc(100vh - 60px);
  background-color: #f5f5f7;
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 20px;
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
}

.register-container {
  width: 100%;
  max-width: 500px;
  background: #ffffff;
  border-radius: 24px;
  box-shadow: 0 12px 36px rgba(0, 0, 0, 0.08);
  overflow: hidden;
}

.header-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  border-bottom: 1px solid #ebebef;
}

.back-btn {
  font-size: 15px;
  color: #0066cc;
}

.title {
  font-size: 18px;
  font-weight: 600;
  color: #1d1d1f;
  margin: 0;
}

.placeholder {
  width: 60px; /* 和 back-btn 宽度相近，确保标题严格居中 */
}

.content-box {
  padding: 32px;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.steps-indicator {
  text-align: center;
  margin-bottom: 24px;
}

.steps-indicator h3 {
  font-size: 20px;
  color: #1d1d1f;
  margin: 0 0 8px 0;
}

.sub-tip {
  font-size: 13px;
  color: #86868b;
  margin: 0;
}

/* 摄像头区域 */
.camera-section {
  width: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.camera-wrapper {
  position: relative;
  width: 300px;
  height: 300px;
  background: #f5f5f7;
  border-radius: 50%; /* 圆形取景框更符合人脸录入直觉 */
  overflow: hidden;
  box-shadow: inset 0 0 0 4px #ebebef;
  transition: all 0.3s ease;
  margin-bottom: 24px;
}

/* 录入中边缘发光效果 */
.camera-wrapper.is-capturing {
  box-shadow: inset 0 0 0 4px #0066cc, 0 0 20px rgba(0, 102, 204, 0.3);
}

.face-video {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transform: scaleX(-1); /* 镜像翻转 */
}

.camera-placeholder {
  position: absolute;
  top: 0; left: 0; right: 0; bottom: 0;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  color: #a1a1a6;
}

.camera-placeholder p {
  margin-top: 12px;
  font-size: 14px;
  font-weight: 500;
}

/* 扫描对焦框 */
.scan-overlay {
  position: absolute;
  top: 10%; left: 10%; right: 10%; bottom: 10%;
  pointer-events: none;
}
.scan-frame {
  position: relative;
  width: 100%; height: 100%;
}
.scan-corner {
  position: absolute;
  width: 30px; height: 30px;
  border-color: rgba(255, 255, 255, 0.8);
  border-style: solid;
}
.top-left { top: 0; left: 0; border-width: 4px 0 0 4px; border-top-left-radius: 12px; }
.top-right { top: 0; right: 0; border-width: 4px 4px 0 0; border-top-right-radius: 12px; }
.bottom-left { bottom: 0; left: 0; border-width: 0 0 4px 4px; border-bottom-left-radius: 12px; }
.bottom-right { bottom: 0; right: 0; border-width: 0 4px 4px 0; border-bottom-right-radius: 12px; }

/* 闪光灯特效 */
.flash-overlay {
  position: absolute;
  top: 0; left: 0; right: 0; bottom: 0;
  background: white;
  opacity: 0;
  pointer-events: none;
  transition: opacity 0.05s ease;
}
.flash-active {
  opacity: 0.8;
}

/* 进度条 */
.progress-container {
  width: 80%;
  text-align: center;
}
.progress-text {
  font-size: 13px;
  color: #86868b;
  margin-top: 8px;
}

/* 成功状态区 */
.success-box {
  text-align: center;
  padding: 40px 0;
}
.success-icon {
  font-size: 64px;
  color: #34c759; /* 苹果绿 */
  margin-bottom: 16px;
}
.success-box h3 {
  font-size: 22px;
  color: #1d1d1f;
  margin-bottom: 8px;
}
.success-box p {
  color: #86868b;
  margin-bottom: 32px;
}
.finish-btn {
  width: 200px;
}

/* 按钮样式 */
.action-footer {
  width: 100%;
  display: flex;
  justify-content: center;
}

.minimal-btn {
  width: 80%;
  height: 48px;
  border-radius: 24px;
  font-size: 16px;
  font-weight: 600;
  border: none;
  transition: all 0.2s ease;
}

.start-cam-btn {
  background: #1d1d1f;
  color: white;
}
.start-cam-btn:hover { background: #424245; }

.start-capture-btn {
  background: #0066cc;
  color: white;
  box-shadow: 0 4px 12px rgba(0, 102, 204, 0.3);
}
.start-capture-btn:hover {
  background: #005bb5;
  transform: translateY(-2px);
  box-shadow: 0 6px 16px rgba(0, 102, 204, 0.4);
}
</style>