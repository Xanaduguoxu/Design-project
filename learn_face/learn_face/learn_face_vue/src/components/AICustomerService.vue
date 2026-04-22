<template>
  <div 
    v-if="shouldShowCustomerService" 
    class="ai-customer-service" 
    :class="{ 'expanded': isExpanded }"
    ref="customerServiceRef"
  >
    <!-- AI客服图标 -->
    <!-- 优化点1：添加 .stop 阻止冒泡，防止触发 document 的关闭监听 -->
    <div class="ai-icon" @click.stop="toggleChat">
      <el-icon :size="24">
        <ChatDotRound v-if="!isExpanded" />
        <Close v-else />
      </el-icon>
      <!-- 脉冲圈 -->
      <div class="pulse-ring"></div>
    </div>
    
    <!-- 聊天窗口 -->
    <transition name="chat-slide">
      <!-- 优化点2：@click.stop 防止聊天窗口内的点击触发关闭 -->
      <div v-if="isExpanded" class="chat-window" @click.stop>
        <div class="chat-header">
          <div class="ai-avatar">
            <el-icon><Robot /></el-icon>
          </div>
          <div class="ai-info">
            <h4>AI智能助手</h4>
            <span class="status">
              <span class="status-dot"></span>
              在线服务
            </span>
          </div>
          <div class="header-actions">
            <!-- 最小化按钮也需要阻止冒泡 -->
            <el-button text size="small" @click.stop="toggleChat">
              <el-icon><Minus /></el-icon>
            </el-button>
          </div>
        </div>
        
        <div class="chat-content" ref="chatContentRef">
          <!-- 欢迎消息 -->
          <div v-if="messages.length === 0" class="welcome-message">
            <div class="ai-avatar-small">
              <el-icon><Robot /></el-icon>
            </div>
            <div class="message-bubble ai-bubble">
              <div class="typing-indicator" v-if="isTyping">
                <span></span>
                <span></span>
                <span></span>
              </div>
              <div v-else>
                您好！我是AI智能客服 🤖<br>
                有什么可以帮助您的吗？
              </div>
            </div>
          </div>
          
          <!-- 聊天消息历史 -->
          <div v-for="(message, index) in messages" :key="index" class="message-item">
            <!-- 用户消息 -->
            <div v-if="message.type === 'user'" class="user-message">
              <div class="message-bubble user-bubble">
                {{ message.content }}
              </div>
              <div class="user-avatar">
                <el-icon><User /></el-icon>
              </div>
            </div>
            
            <!-- AI消息 -->
            <div v-else class="ai-message">
              <div class="ai-avatar-small">
                <el-icon><Robot /></el-icon>
              </div>
              <div class="message-bubble ai-bubble" :class="{ 'error-bubble': message.isError }">
                <div v-if="message.isTyping" class="typing-indicator">
                  <span></span>
                  <span></span>
                  <span></span>
                </div>
                <!-- 优化点3：直接显示处理好的 htmlContent，而不是实时调用函数 -->
                <div v-else v-html="message.htmlContent || renderMarkdown(message.content)"></div>
              </div>
            </div>
          </div>
        </div>
        
        <div class="chat-input">
          <el-input
            ref="inputRef"
            v-model="userMessage"
            placeholder="请输入您的问题..."
            @keyup.enter="sendMessage"
            :disabled="isSending"
            class="message-input"
          >
            <template #suffix>
              <el-button 
                text 
                @click="sendMessage"
                :disabled="!userMessage.trim() || isSending"
                :loading="isSending"
                class="send-button"
              >
                <el-icon v-if="!isSending"><Position /></el-icon>
              </el-button>
            </template>
          </el-input>
          <div class="powered-by">
            <span v-if="isSending">AI正在思考中...</span>
            <span v-else>Powered by AI</span>
          </div>
        </div>
      </div>
    </transition>
  </div>
</template>

<script>
import { ref, computed, onMounted, onUnmounted, nextTick } from 'vue'
import { useRoute } from 'vue-router'
import { chatWithAI } from '../utils/api'
import { ElMessage } from 'element-plus'
import { marked } from 'marked'

export default {
  name: 'AICustomerService',
  setup() {
    const route = useRoute()
    const isExpanded = ref(false)
    const userMessage = ref('')
    const customerServiceRef = ref(null)
    const chatContentRef = ref(null)
    const inputRef = ref(null) // 新增输入框引用
    
    const isTyping = ref(false)
    const isSending = ref(false)
    const sessionId = ref('')
    const messages = ref([])
    
    // 配置 marked 选项（可选：防止XSS等）
    marked.setOptions({
      breaks: true, // 支持回车换行
      gfm: true     // GitHub 风格 Markdown
    })

    const shouldShowCustomerService = computed(() => {
      const hiddenRoutes = ['/login', '/register']
      return !hiddenRoutes.includes(route.path)
    })
    
    const scrollToBottom = async () => {
      await nextTick()
      if (chatContentRef.value) {
        chatContentRef.value.scrollTop = chatContentRef.value.scrollHeight
      }
    }
    
    // 渲染Markdown内容
    const renderMarkdown = (content) => {
      if (!content) return ''
      try {
        return marked(content)
      } catch (error) {
        console.error('Markdown渲染错误:', error)
        return content
      }
    }

    // 优化后的切换逻辑
    const toggleChat = async () => {
      isExpanded.value = !isExpanded.value
      
      if (isExpanded.value) {
        // 打开时
        // 1. 自动聚焦输入框
        nextTick(() => {
          if (inputRef.value) {
            inputRef.value.focus()
          }
        })
        
        // 2. 显示欢迎动画
        if (messages.value.length === 0) {
          isTyping.value = true
          setTimeout(() => {
            isTyping.value = false
            scrollToBottom()
          }, 1000)
        } else {
          scrollToBottom()
        }
      }
    }
    
    const sendMessage = async () => {
      const question = userMessage.value.trim()
      if (!question || isSending.value) return
      
      userMessage.value = ''
      isSending.value = true
      
      // 用户消息
      messages.value.push({
        type: 'user',
        content: question,
        timestamp: new Date()
      })
      
      scrollToBottom()
      
      // AI 消息占位
      const aiMessageIndex = messages.value.length
      messages.value.push({
        type: 'ai',
        content: '',
        htmlContent: '', // 预留HTML内容字段
        timestamp: new Date(),
        isTyping: true
      })
      
      scrollToBottom()
      
      try {
        let fullResponse = ''
        
        await chatWithAI(
          question,
          sessionId.value,
          // onMessage (流式接收)
          (data) => {
            if (data.sessionId && !sessionId.value) {
              sessionId.value = data.sessionId
            }
            
            let newContent = ''
            if (data.content) {
              newContent = data.content
            } else if (typeof data === 'string') {
              newContent = data
            }

            if (newContent) {
              fullResponse += newContent
              const currentMsg = messages.value[aiMessageIndex]
              currentMsg.content = fullResponse
              // 实时渲染 Markdown 会消耗性能，如果卡顿可以只在 onComplete 渲染
              // 这里为了体验选择实时渲染
              currentMsg.htmlContent = renderMarkdown(fullResponse) 
              currentMsg.isTyping = false
              scrollToBottom()
            }
          },
          // onError
          (error) => {
            console.error('聊天错误:', error)
            messages.value[aiMessageIndex] = {
              type: 'ai',
              content: '抱歉，我遇到了一些问题，请稍后再试。',
              htmlContent: '抱歉，我遇到了一些问题，请稍后再试。',
              timestamp: new Date(),
              isError: true
            }
            ElMessage.error(typeof error === 'string' ? error : '发送失败')
            scrollToBottom()
          },
          // onComplete
          () => {
            const currentMsg = messages.value[aiMessageIndex]
            currentMsg.isTyping = false
            
            if (!fullResponse) {
              currentMsg.content = '抱歉，我没有收到有效的回复，请稍后再试。'
              currentMsg.htmlContent = '抱歉，我没有收到有效的回复，请稍后再试。'
              currentMsg.isError = true
            } else {
              // 最终确保 Markdown 渲染正确
              currentMsg.content = fullResponse
              currentMsg.htmlContent = renderMarkdown(fullResponse)
            }
            scrollToBottom()
          }
        )
      } catch (error) {
        console.error('发送消息失败:', error)
        messages.value[aiMessageIndex] = {
          type: 'ai',
          content: '网络连接异常，请检查网络。',
          htmlContent: '网络连接异常，请检查网络。',
          timestamp: new Date(),
          isError: true
        }
      } finally {
        isSending.value = false
        // 发送完成后再次聚焦输入框
        nextTick(() => {
          if (inputRef.value) inputRef.value.focus()
        })
      }
    }
    
    // 点击外部区域关闭聊天窗口
    const handleClickOutside = (event) => {
      // 只有在展开状态下才进行检测
      if (!isExpanded.value) return

      // 检查点击目标是否在组件外部
      // 注意：由于我们在 toggleChat 上加了 .stop，点击图标不会触发这里
      // 所以这里只需要判断是否点击了组件内部即可
      if (customerServiceRef.value && !customerServiceRef.value.contains(event.target)) {
        isExpanded.value = false
      }
    }
    
    onMounted(() => {
      // 使用 mousedown 往往比 click 反应更快，体验更好，但 click 兼容性更稳
      document.addEventListener('click', handleClickOutside)
    })
    
    onUnmounted(() => {
      document.removeEventListener('click', handleClickOutside)
    })
    
    return {
      isExpanded,
      userMessage,
      shouldShowCustomerService,
      customerServiceRef,
      chatContentRef,
      inputRef,
      isTyping,
      isSending,
      messages,
      toggleChat,
      sendMessage,
      renderMarkdown
    }
  }
}
</script>

<style scoped>
.ai-customer-service {
  --primary-color: #3b82f6;
  --primary-gradient: linear-gradient(135deg, #3b82f6 0%, #2563eb 100%);
  --secondary-bg: #f8fafc;
  --glass-bg: rgba(255, 255, 255, 0.9); /* 稍微增加不透明度提升阅读体验 */
  --glass-border: 1px solid rgba(255, 255, 255, 0.6);
  --shadow-lg: 0 20px 25px -5px rgba(0, 0, 0, 0.1), 0 10px 10px -5px rgba(0, 0, 0, 0.04);
  --shadow-glow: 0 0 20px rgba(59, 130, 246, 0.4);
  
  position: fixed;
  right: 30px;
  bottom: 100px;
  z-index: 9999;
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif;
}

/* ================== 图标按钮 ================== */
.ai-icon {
  width: 60px;
  height: 60px;
  background: var(--primary-gradient);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  cursor: pointer;
  box-shadow: 0 4px 15px rgba(37, 99, 235, 0.3);
  transition: all 0.3s cubic-bezier(0.34, 1.56, 0.64, 1);
  position: relative;
  z-index: 100;
  user-select: none; /* 防止双击导致选中文本 */
}

.ai-icon:hover {
  transform: scale(1.1) rotate(5deg);
  box-shadow: var(--shadow-glow);
}

.ai-icon:active {
  transform: scale(0.95);
}

.pulse-ring {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  border-radius: 50%;
  border: 2px solid rgba(59, 130, 246, 0.5);
  animation: pulse-ring 2s cubic-bezier(0.215, 0.61, 0.355, 1) infinite;
  pointer-events: none; /* 关键：确保不遮挡点击事件 */
}

@keyframes pulse-ring {
  0% { transform: scale(0.95); opacity: 0.8; }
  100% { transform: scale(2); opacity: 0; }
}

/* ================== 聊天主窗口 ================== */
.chat-window {
  position: absolute;
  right: 0; 
  bottom: 80px; 
  width: 380px;
  height: 600px;
  max-height: 80vh; 
  background: var(--glass-bg);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border: var(--glass-border);
  border-radius: 24px;
  box-shadow: var(--shadow-lg);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.chat-header {
  background: rgba(255, 255, 255, 0.8);
  border-bottom: 1px solid rgba(0, 0, 0, 0.05);
  padding: 16px 20px;
  display: flex;
  align-items: center;
  gap: 12px;
  flex-shrink: 0;
  user-select: none;
}

.ai-avatar {
  width: 40px;
  height: 40px;
  background: linear-gradient(135deg, #e0f2fe 0%, #bae6fd 100%);
  color: #0284c7;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  box-shadow: inset 0 0 0 1px rgba(255,255,255,0.5);
}

.ai-info h4 {
  margin: 0;
  font-size: 16px;
  font-weight: 700;
  color: #1e293b;
}

.status {
  font-size: 12px;
  color: #64748b;
  display: flex;
  align-items: center;
  gap: 6px;
  margin-top: 2px;
}

.status-dot {
  width: 6px;
  height: 6px;
  background: #22c55e;
  border-radius: 50%;
  box-shadow: 0 0 0 2px rgba(34, 197, 94, 0.2);
}

/* ================== 内容区域 ================== */
.chat-content {
  flex: 1;
  padding: 20px;
  overflow-y: auto;
  background: var(--secondary-bg);
  scroll-behavior: smooth;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.welcome-message {
  display: flex;
  gap: 12px;
  margin-bottom: 10px;
}

.message-item {
  width: 100%;
}

.user-message {
  display: flex;
  flex-direction: row-reverse; 
  gap: 12px;
  align-items: flex-end;
}

.ai-message {
  display: flex;
  flex-direction: row;
  gap: 12px;
  align-items: flex-start;
}

.user-avatar, .ai-avatar-small {
  width: 32px;
  height: 32px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  font-size: 16px;
}

.user-avatar {
  background: var(--primary-gradient);
  color: white;
  box-shadow: 0 2px 8px rgba(37, 99, 235, 0.2);
}

.ai-avatar-small {
  background: white;
  color: #3b82f6;
  border: 1px solid #e2e8f0;
}

.message-bubble {
  max-width: 80%;
  padding: 12px 16px;
  font-size: 14px;
  line-height: 1.6;
  position: relative;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.02);
  word-wrap: break-word; /* 防止长单词溢出 */
}

.ai-bubble {
  background: white;
  color: #334155;
  border-radius: 4px 20px 20px 20px;
  border: 1px solid #f1f5f9;
}

.user-bubble {
  background: var(--primary-gradient);
  color: white;
  border-radius: 20px 4px 20px 20px;
}

.error-bubble {
  background: #fef2f2;
  border-color: #fecaca;
  color: #b91c1c;
}

/* ================== 输入区域 ================== */
.chat-input {
  padding: 16px 20px;
  background: white;
  border-top: 1px solid #f1f5f9;
}

.message-input :deep(.el-input__wrapper) {
  background-color: #f8fafc;
  border-radius: 24px;
  box-shadow: none !important;
  border: 1px solid #e2e8f0;
  padding: 4px 12px;
  transition: all 0.2s;
}

.message-input :deep(.el-input__wrapper.is-focus) {
  background-color: white;
  border-color: #3b82f6;
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.1) !important;
}

.send-button {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;
}

.send-button:hover:not(:disabled) {
  background: #eff6ff;
  color: #2563eb;
}

.powered-by {
  font-size: 10px;
  color: #94a3b8;
  text-align: center;
  margin-top: 8px;
  font-weight: 500;
  letter-spacing: 0.5px;
  user-select: none;
}

/* ================== Markdown 样式 ================== */
.ai-bubble :deep(p) { margin: 0 0 8px 0; }
.ai-bubble :deep(p:last-child) { margin-bottom: 0; }
.ai-bubble :deep(strong) { color: #0f172a; font-weight: 600; }
.ai-bubble :deep(pre) {
  background: #1e293b;
  color: #e2e8f0;
  padding: 12px;
  border-radius: 8px;
  overflow-x: auto;
  margin: 10px 0;
  font-family: 'Menlo', 'Monaco', 'Courier New', monospace;
  font-size: 12px;
  border: 1px solid #334155;
}
.ai-bubble :deep(code) {
  font-family: 'Menlo', 'Monaco', 'Courier New', monospace;
  background: #f1f5f9;
  color: #ea580c;
  padding: 2px 5px;
  border-radius: 4px;
  font-size: 0.9em;
}
.ai-bubble :deep(pre) :deep(code) { background: transparent; color: inherit; padding: 0; }
.ai-bubble :deep(ul), .ai-bubble :deep(ol) { padding-left: 20px; margin: 8px 0; }
.ai-bubble :deep(li) { margin-bottom: 4px; }
.ai-bubble :deep(blockquote) {
  border-left: 3px solid #cbd5e1;
  margin: 8px 0;
  padding-left: 12px;
  color: #64748b;
  font-style: italic;
}
.ai-bubble :deep(a) {
  color: #2563eb;
  text-decoration: none;
  border-bottom: 1px dashed #2563eb;
}
.ai-bubble :deep(a:hover) { border-bottom-style: solid; }

/* ================== 动画 ================== */
.chat-slide-enter-active,
.chat-slide-leave-active {
  transition: all 0.4s cubic-bezier(0.16, 1, 0.3, 1);
}

.chat-slide-enter-from,
.chat-slide-leave-to {
  opacity: 0;
  transform: translateY(20px) scale(0.95);
  pointer-events: none;
}

.typing-indicator span {
  width: 6px;
  height: 6px;
  background: #94a3b8;
  display: inline-block;
  border-radius: 50%;
  animation: typing-bounce 1.4s infinite ease-in-out both;
}
.typing-indicator span:nth-child(1) { animation-delay: -0.32s; }
.typing-indicator span:nth-child(2) { animation-delay: -0.16s; }
@keyframes typing-bounce {
  0%, 80%, 100% { transform: scale(0); }
  40% { transform: scale(1); }
}
</style>