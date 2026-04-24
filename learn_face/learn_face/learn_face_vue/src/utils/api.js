import { useUserStore } from '../stores/user'
import { ElMessage } from 'element-plus'

// API基础URL
const BASE_URL = 'http://localhost:9099/v1'

// 白名单列表
const WHITELIST_ENDPOINTS = [
  '/auth/login',
  '/auth/register',
  '/auth/faceLogin'
]

// 检查接口是否在白名单中
function isWhitelistEndpoint(endpoint) {
  return WHITELIST_ENDPOINTS.some(whiteEndpoint => endpoint.startsWith(whiteEndpoint))
}

// 通用请求函数
export async function apiRequest(endpoint, options = {}) {
  const userStore = useUserStore()

  const config = {
    method: 'GET',
    headers: {
      'Content-Type': 'application/json',
      // 只有非白名单接口才添加认证头
      ...(isWhitelistEndpoint(endpoint) ? {} : userStore.getAuthHeaders())
    },
    ...options
  }

  try {
    const response = await fetch(`${BASE_URL}${endpoint}`, config)
    const result = await response.json()
    if (result.code === 200 && result.message === '用户名或密码错误') {
      ElMessage.error('用户名或密码错误')
      return null
    }

    if (result.code === 200) {
      // 检查是否为未登录的特殊情况
      if (result.message === 'fail!' && result.data === '未登陆,请先登陆!') {
        // 清除用户登录状态
        userStore.logout()

        // 显示登录提示
        ElMessage({
          message: '登录已过期，请重新登录',
          type: 'warning',
          duration: 800
        })

        // 触发登录弹窗
        triggerLoginDialog()

        throw new Error('未登录，请先登录！')
      }

      // 对于注册接口，需要特殊处理
      if (endpoint === '/auth/register') {
        if (result.message === 'success!') {
          return result.data
        } else {
          // 注册失败，抛出错误
          throw new Error(result.message || '注册失败')
        }
      }

      return result.data
    } else {
      throw new Error(result.message || '请求失败')
    }
  } catch (error) {
    console.error('API请求错误:', error)
    throw error
  }
}

// 触发登录弹窗的函数
function triggerLoginDialog() {
  // 使用自定义事件在window对象上触发
  const loginEvent = new CustomEvent('showLoginDialog', {
    detail: { source: 'api' }
  })
  window.dispatchEvent(loginEvent)
}

// AI聊天接口 - 支持SSE流式数据
export async function chatWithAI(question, sessionId = '', onMessage, onError, onComplete) {
  const userStore = useUserStore()
  
  try {
    console.log('发送聊天请求:', { question, sessionId })
    
    const response = await fetch(`${BASE_URL}/chat/chats`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Accept': 'text/event-stream',
        'Cache-Control': 'no-cache',
        ...userStore.getAuthHeaders()
      },
      body: JSON.stringify({
        question,
        sessionId
      })
    })

    if (!response.ok) {
      throw new Error(`HTTP error! status: ${response.status}`)
    }
    
    console.log('收到响应:', response.status, response.statusText)

    // 使用ReadableStream API处理SSE流式数据
    const reader = response.body.getReader()
    const decoder = new TextDecoder('utf-8')
    let buffer = ''
    let responseData = null

    const processChunk = async () => {
      try {
        const { done, value } = await reader.read()
        
        if (done) {
          console.log('数据流结束')
          if (onComplete) onComplete()
          return
        }

        const chunk = decoder.decode(value, { stream: true })
        console.log('收到数据块:', chunk)
        
        buffer += chunk
        
        // 处理可能的多行数据
        const lines = buffer.split('\n')
        buffer = lines.pop() || '' // 保留最后一个可能不完整的行
        
        for (const line of lines) {
          if (line.trim() === '') continue
          
          // 处理SSE格式数据
          if (line.startsWith('data:')) {
            const data = line.substring(5).trim()
            
            if (data === '[DONE]') {
              if (onComplete) onComplete()
              return
            }
            
            try {
              // 尝试解析JSON
              responseData = JSON.parse(data)
              console.log('解析的JSON数据:', responseData)
              
              if (responseData && responseData.code === 200 && responseData.data) {
                if (onMessage) {
                  console.log('调用onMessage回调:', responseData.data)
                  onMessage(responseData.data)
                }
              } else if (responseData && typeof responseData === 'object') {
                // 直接使用响应数据
                console.log('直接使用响应数据:', responseData)
                onMessage(responseData)
              } else {
                console.warn('响应数据格式不符合预期:', responseData)
                // 尝试直接使用数据
                if (data && onMessage) {
                  onMessage(data)
                }
              }
            } catch (parseError) {
              console.warn('解析JSON失败:', parseError, '原始数据:', data)
              
              // 尝试直接使用数据
              if (data && onMessage) {
                // 直接传递字符串数据
                console.log('使用原始字符串数据:', data)
                onMessage(data)
              }
            }
          } else if (line.includes('data:')) {
            // 处理可能的非标准格式
            const dataIndex = line.indexOf('data:')
            if (dataIndex >= 0) {
              const data = line.substring(dataIndex + 5).trim()
              console.log('非标准格式数据:', data)
              
              try {
                responseData = JSON.parse(data)
                if (responseData && responseData.data && onMessage) {
                  onMessage(responseData.data)
                }
              } catch (e) {
                console.warn('解析非标准格式失败:', e)
              }
            }
          }
        }
        
        // 继续处理下一个数据块
        processChunk()
      } catch (chunkError) {
        console.error('处理数据块错误:', chunkError)
        if (onError) onError(chunkError.message)
      }
    }
    
    // 开始处理数据流
    processChunk()
  } catch (error) {
    console.error('聊天接口错误:', error)
    if (onError) {
      onError(error.message || '网络连接失败，请稍后重试')
    }
  }
}

// 文件上传API
export async function uploadFileAPI(file) {
  const formData = new FormData()
  formData.append('file', file)

  const userStore = useUserStore()

  const config = {
    method: 'POST',
    headers: {
      'Authorization': userStore.token,
      'Accept': '*/*'
    },
    body: formData
  }

  try {
    const response = await fetch(`${BASE_URL}/common/file/upload`, config)
    const result = await response.json()

    if (result.code === 200) {
      return result.data
    } else {
      throw new Error(result.message || '上传失败')
    }
  } catch (error) {
    console.error('文件上传错误:', error)
    throw error
  }
}


// 登录API
export async function loginAPI(username, password) {
  return apiRequest('/auth/login', {
    method: 'POST',
    body: JSON.stringify({ username, password })
  })
}

// 注册API
export async function registerAPI(username, password) {
  return apiRequest('/auth/register', {
    method: 'POST',
    body: JSON.stringify({ username, password })
  })
}

// 轮播图API
export async function getBannersAPI() {
  const data = await apiRequest('/banner/list')
  return data.map(item => ({
    name: item.name,
    description: item.brief,
    image: item.image
  }))
}


// 更新个人信息API
export async function updateUserInfoAPI(userInfo) {
  return apiRequest('/auth/update', {
    method: 'POST',
    body: JSON.stringify(userInfo)
  })
}

// 修改密码API
export async function updatePasswordAPI(data) {
  return apiRequest('/auth/password', {
    method: 'POST',
    body: JSON.stringify(data)
  })
}

// 获取字典数据API
export async function getDictionaryAPI(classify) {
  return apiRequest(`/dictionary/list?classify=${classify}`)
}

// 获取作品列表API
export async function getWorksAPI(params) {
  return apiRequest('/work/list', {
    method: 'POST',
    body: JSON.stringify(params)
  })
}

// 获取作品详情API
export async function getWorkDetailAPI(id) {
  return apiRequest(`/work/details?id=${id}`)
}

// 获取推荐作品API
export async function getRecommendedWorksAPI() {
  return apiRequest('/work/recommend')
}

// 获取推荐课程API
export async function getRecommendedCoursesAPI() {
  return apiRequest('/course/recommend')
}

// 获取评论列表API
export async function getCommentsAPI(workId, belong = '') {
  const belongParam = belong ? `&belong=${encodeURIComponent(belong)}` : ''
  return apiRequest(`/comments/allComments?id=${workId}${belongParam}`)
}

// 获取课程列表API
export async function getCoursesAPI(params) {
  return apiRequest('/course/list', {
    method: 'POST',
    body: JSON.stringify(params)
  })
}

// 获取字典数据API (for course levels)
export async function getCourseLevelDictionaryAPI() {
  return apiRequest('/dictionary/list?classify=course_level')
}

// 获取课程详情API
export async function getCourseDetailAPI(courseId) {
  return apiRequest(`/course/details?id=${courseId}`)
}

// 获取活动列表API
export async function getActivitiesAPI(params) {
  return apiRequest('/activate/list', {
    method: 'POST',
    body: JSON.stringify(params)
  })
}

// 获取论坛帖子列表API
export async function getForumPostsAPI(params) {
  return apiRequest('/forum/list', {
    method: 'POST',
    body: JSON.stringify(params)
  })
}

// 获取论坛帖子详情API
export async function getForumPostDetailAPI(id) {
  return apiRequest(`/forum/details?id=${id}`)
}

// 添加论坛帖子API
export async function addForumPostAPI(postData) {
  return apiRequest('/forum/add', {
    method: 'POST',
    body: JSON.stringify(postData)
  })
}

// 活动报名API
export async function registerForActivityAPI(registrationData) {
  return apiRequest('/signup/add', {
    method: 'POST',
    body: JSON.stringify(registrationData)
  })
}

// 获取我的报名列表API
export async function getMyRegistrationsAPI(params) {
  return apiRequest('/signup/list', {
    method: 'POST',
    body: JSON.stringify(params)
  })
}

// 取消报名API
export async function cancelRegistrationAPI(id) {
  return apiRequest(`/signup/del?id=${id}`, {
    method: 'DELETE'
  })
}

// 添加评论API
export async function addCommentAPI(commentData) {
  return apiRequest('/comments/add', {
    method: 'POST',
    body: JSON.stringify(commentData)
  })
}

// 获取论坛热门话题API
export async function getTopForumTopicsAPI() {
  return apiRequest('/forum/top')
}

// 获取考试任务API
export async function getTaskAPI(params = {}) {
  const query = params.paperSource ? `?paperSource=${encodeURIComponent(params.paperSource)}` : ''
  return apiRequest(`/task/ranTask${query}`)
}

// 提交考试API
export async function submitExamAPI(examData) {
  return apiRequest('/exam/add', {
    method: 'POST',
    body: JSON.stringify(examData)
  })
}

// 获取考试列表API
export async function getExamListAPI(params) {
  return apiRequest('/exam/list', {
    method: 'POST',
    body: JSON.stringify(params)
  })
}

// 创建目标API
export async function createGoalAPI(goalData) {
  return apiRequest('/goal/add', {
    method: 'POST',
    body: JSON.stringify(goalData)
  })
}

// 获取目标列表API
export async function getGoalListAPI(params) {
  return apiRequest('/goal/list', {
    method: 'POST',
    body: JSON.stringify(params)
  })
}

// 更新目标状态API
export async function updateGoalStatusAPI(goalId, status) {
  return apiRequest(`/goal/updateStatus`, {
    method: 'POST',
    body: JSON.stringify({ id: goalId, status: status })
  })
}

// 删除目标API
export async function deleteGoalAPI(goalId) {
  return apiRequest(`/goal/del?id=${goalId}`, {
    method: 'DELETE'
  })
}

// 更新目标API
export async function updateGoalAPI(goalData) {
  return apiRequest('/goal/update', {
    method: 'POST',
    body: JSON.stringify(goalData)
  })
}

export async function addLearningRecordAPI(recordData) {
  return apiRequest('/learnLog/add', {
    method: 'POST',
    body: JSON.stringify(recordData)
  })
}

// 获取统计指标API
export async function getStatisticsMetricsAPI() {
  return apiRequest('/statistics/metrics', {
    method: 'GET'
  })
}

// 获取个人学习统计数据API
export async function getSelfLearningStatsAPI() {
  return apiRequest('/statistics/oneselfLearn', {
    method: 'GET'
  })
}

// 获取目标数据API
export async function getGoalsAPI() {
  return apiRequest('/statistics/goal', {
    method: 'GET'
  })
}

export async function faceLoginAPI(imageBase64) {
  return apiRequest('/auth/faceLogin', {
    method: 'POST',
    body: JSON.stringify({ imageBase64: imageBase64 })
  })
}

// 获取试卷名称列表
export async function getTaskNamesAPI(params = {}) {
  const query = params.paperSource ? `?paperSource=${encodeURIComponent(params.paperSource)}` : ''
  return apiRequest(`/task/taskName${query}`)
}

// 根据试卷名称获取试题列表
export async function selectTaskAPI(name, params = {}) {
  const query = new URLSearchParams({ name: String(name || '') })
  if (params.paperSource) {
    query.append('paperSource', params.paperSource)
  }
  if (params.ownerUserId !== null && params.ownerUserId !== undefined) {
    query.append('ownerUserId', String(params.ownerUserId))
  }
  return apiRequest(`/task/selectTask?${query.toString()}`)
}

export async function deleteMyPaperAPI(name) {
  const query = new URLSearchParams({ name: String(name || '') })
  return apiRequest(`/task/delOwnPaper?${query.toString()}`, {
    method: 'DELETE'
  })
}

// 错题本列表
export async function getWrongQuestionListAPI(params) {
  return apiRequest('/wrong/list', {
    method: 'POST',
    body: JSON.stringify(params)
  })
}

// 错题本客观题练习提交
export async function submitWrongObjectiveAPI(data) {
  return apiRequest('/wrong/practice/objective', {
    method: 'POST',
    body: JSON.stringify(data)
  })
}

// 错题本主观题自评提交
export async function submitWrongSubjectiveAPI(data) {
  return apiRequest('/wrong/practice/subjective', {
    method: 'POST',
    body: JSON.stringify(data)
  })
}

// 错题本批量移除
export async function deleteWrongQuestionsAPI(ids) {
  return apiRequest('/wrong/delBatch', {
    method: 'POST',
    body: JSON.stringify({ ids })
  })
}

// 错题本移除全部已掌握
export async function deleteMasteredWrongQuestionsAPI() {
  return apiRequest('/wrong/delMastered', {
    method: 'POST'
  })
}

// 薄弱点画像
export async function getWrongWeaknessProfileAPI(params = {}) {
  return apiRequest('/wrong/weaknessProfile', {
    method: 'POST',
    body: JSON.stringify(params)
  })
}

// 个性化自动组卷
export async function generatePersonalizedPaperAPI(params) {
  return apiRequest('/wrong/generatePaper', {
    method: 'POST',
    body: JSON.stringify(params)
  })
}
