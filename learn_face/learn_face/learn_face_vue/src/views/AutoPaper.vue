<template>
  <div class="auto-paper-page">
    <div class="header card">
      <div>
        <h2>智能组卷</h2>
        <p>基于你的错题薄弱点自动生成针对性试卷</p>
      </div>
      <el-space>
        <el-button @click="loadWeaknessProfile" :loading="loadingProfile">刷新画像</el-button>
        <el-button type="primary" @click="generatePaper" :loading="generating">一键生成试卷</el-button>
      </el-space>
    </div>

    <div class="grid">
      <div class="card profile-card" v-loading="loadingProfile">
        <h3>薄弱点画像</h3>
        <div class="summary-row">
          <div class="summary-item">
            <div class="label">错题总量</div>
            <div class="value">{{ profileSummary.totalWrongQuestions || 0 }}</div>
          </div>
          <div class="summary-item">
            <div class="label">推荐题量</div>
            <div class="value">{{ profileSummary.recommend?.questionCount || 15 }}</div>
          </div>
          <div class="summary-item">
            <div class="label">推荐时长</div>
            <div class="value">{{ profileSummary.recommend?.durationMinutes || 45 }} min</div>
          </div>
        </div>

        <div class="section-title">知识点薄弱排名</div>
        <el-empty v-if="!topKnowledgePoints.length" description="暂无薄弱点数据" :image-size="80" />
        <div v-else class="chips-wrap">
          <el-tag
            v-for="item in topKnowledgePoints"
            :key="item.knowledgePoint"
            type="danger"
            effect="light"
            class="chip"
          >
            {{ item.knowledgePoint }}（{{ item.weight }}）
          </el-tag>
        </div>

        <div class="section-title">题型薄弱排名</div>
        <el-empty v-if="!topCategories.length" description="暂无题型薄弱数据" :image-size="80" />
        <div v-else class="chips-wrap">
          <el-tag
            v-for="item in topCategories"
            :key="item.category"
            type="warning"
            effect="light"
            class="chip"
          >
            {{ item.category }}（{{ item.weight }}）
          </el-tag>
        </div>
      </div>

      <div class="card config-card">
        <h3>组卷配置</h3>
        <el-form label-width="110px">
          <el-form-item label="试卷名称">
            <el-input v-model="form.paperName" placeholder="留空则自动命名" />
          </el-form-item>

          <el-form-item label="题目数量">
            <el-input-number v-model="form.questionCount" :min="5" :max="50" :step="1" />
          </el-form-item>

          <el-form-item label="建议时长">
            <el-input-number v-model="form.durationMinutes" :min="15" :max="180" :step="5" />
            <span class="inline-unit">分钟</span>
          </el-form-item>

          <el-form-item label="包含已掌握">
            <el-switch v-model="form.includeMastered" />
          </el-form-item>

          <el-form-item label="难度比例">
            <div class="ratio-row">
              <div class="ratio-cell">
                <span>简单</span>
                <el-input-number v-model="form.easyRatio" :min="0" :max="1" :step="0.1" :precision="1" />
              </div>
              <div class="ratio-cell">
                <span>中等</span>
                <el-input-number v-model="form.mediumRatio" :min="0" :max="1" :step="0.1" :precision="1" />
              </div>
              <div class="ratio-cell">
                <span>困难</span>
                <el-input-number v-model="form.hardRatio" :min="0" :max="1" :step="0.1" :precision="1" />
              </div>
            </div>
            <div class="hint">总和可不等于 1，系统会自动归一化。</div>
          </el-form-item>
        </el-form>
      </div>
    </div>

    <div class="card result-card" v-if="generateResult">
      <h3>生成结果</h3>
      <div class="result-grid">
        <div class="result-item"><span>试卷名称：</span><strong>{{ generateResult.paperName }}</strong></div>
        <div class="result-item"><span>题目数量：</span><strong>{{ generateResult.questionCount }}</strong></div>
        <div class="result-item"><span>总分：</span><strong>{{ generateResult.totalScore }}</strong></div>
        <div class="result-item"><span>建议时长：</span><strong>{{ generateResult.durationMinutes }} 分钟</strong></div>
      </div>
      <el-space>
        <el-button type="primary" @click="goToExam">去考试页使用该试卷</el-button>
        <el-button @click="copyPaperName">复制试卷名称</el-button>
      </el-space>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { generatePersonalizedPaperAPI, getWrongWeaknessProfileAPI } from '@/utils/api.js'

const router = useRouter()

const loadingProfile = ref(false)
const generating = ref(false)
const profileSummary = ref({})
const generateResult = ref(null)

const form = reactive({
  paperName: '',
  questionCount: 15,
  durationMinutes: 45,
  includeMastered: false,
  easyRatio: 0.4,
  mediumRatio: 0.4,
  hardRatio: 0.2
})

const topKnowledgePoints = computed(() => profileSummary.value?.topKnowledgePoints || [])
const topCategories = computed(() => profileSummary.value?.topCategories || [])

const loadWeaknessProfile = async () => {
  loadingProfile.value = true
  try {
    const res = await getWrongWeaknessProfileAPI({})
    if (!res || res.success === false) {
      ElMessage.warning('暂无可用薄弱点数据')
      profileSummary.value = {}
      return
    }
    profileSummary.value = res.summary || {}
    const recommend = profileSummary.value.recommend || {}
    if (recommend.questionCount) {
      form.questionCount = Number(recommend.questionCount)
    }
    if (recommend.durationMinutes) {
      form.durationMinutes = Number(recommend.durationMinutes)
    }
  } catch (error) {
    ElMessage.error('加载薄弱点画像失败')
  } finally {
    loadingProfile.value = false
  }
}

const generatePaper = async () => {
  if (form.easyRatio + form.mediumRatio + form.hardRatio <= 0) {
    ElMessage.warning('难度比例总和不能为 0')
    return
  }

  generating.value = true
  try {
    const payload = {
      paperName: form.paperName,
      questionCount: form.questionCount,
      durationMinutes: form.durationMinutes,
      includeMastered: form.includeMastered,
      easyRatio: form.easyRatio,
      mediumRatio: form.mediumRatio,
      hardRatio: form.hardRatio
    }
    const res = await generatePersonalizedPaperAPI(payload)
    if (!res || res.success === false) {
      const msgMap = {
        not_login: '登录已失效，请重新登录',
        no_wrong_question: '当前错题不足，无法生成针对性试卷',
        no_task_source: '题库为空，无法组卷',
        no_task_selected: '暂未匹配到可用题目，请调整配置后重试'
      }
      ElMessage.error(msgMap[res?.message] || '组卷失败')
      return
    }
    generateResult.value = res
    ElMessage.success('个性化试卷生成成功')
    await loadWeaknessProfile()
  } catch (error) {
    ElMessage.error('组卷失败，请稍后重试')
  } finally {
    generating.value = false
  }
}

const goToExam = () => {
  if (!generateResult.value?.paperName) return
  router.push({ path: '/task', query: { examName: generateResult.value.paperName } })
}

const copyPaperName = async () => {
  const name = generateResult.value?.paperName
  if (!name) return
  try {
    await navigator.clipboard.writeText(name)
    ElMessage.success('试卷名称已复制')
  } catch (error) {
    ElMessage.warning(`请手动复制：${name}`)
  }
}

onMounted(() => {
  loadWeaknessProfile()
})
</script>

<style scoped>
.auto-paper-page {
  padding: 20px;
  min-height: 100vh;
  background: #f6f8fb;
}

.card {
  background: #fff;
  border-radius: 10px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.06);
  padding: 16px;
  margin-bottom: 16px;
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
}

.header h2 {
  margin: 0;
}

.header p {
  margin: 6px 0 0;
  color: #909399;
}

.grid {
  display: grid;
  grid-template-columns: 1.2fr 1fr;
  gap: 16px;
}

.summary-row {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
  margin-bottom: 14px;
}

.summary-item {
  border: 1px solid #ebeef5;
  border-radius: 8px;
  padding: 10px;
}

.summary-item .label {
  font-size: 12px;
  color: #909399;
}

.summary-item .value {
  margin-top: 4px;
  font-size: 20px;
  font-weight: 600;
  color: #303133;
}

.section-title {
  font-weight: 600;
  color: #303133;
  margin: 10px 0;
}

.chips-wrap {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.chip {
  margin: 0;
}

.ratio-row {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.ratio-cell {
  display: flex;
  align-items: center;
  gap: 8px;
}

.inline-unit {
  margin-left: 8px;
  color: #909399;
}

.hint {
  margin-top: 6px;
  font-size: 12px;
  color: #909399;
}

.result-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 10px;
  margin-bottom: 12px;
}

.result-item {
  color: #606266;
}

@media (max-width: 900px) {
  .grid {
    grid-template-columns: 1fr;
  }

  .summary-row {
    grid-template-columns: 1fr;
  }

  .result-grid {
    grid-template-columns: 1fr;
  }

  .header {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
