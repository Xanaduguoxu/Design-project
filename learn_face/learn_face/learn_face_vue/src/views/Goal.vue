<template>
  <div class="goal-brutalist-container">
    
    <!-- 1. 顶部操作栏 -->
    <header class="header-brutal">
      <div class="header-content">
        <div class="title-block">
          <h2>TARGET<span class="accent">_</span>SYSTEM</h2>
          <p class="sys-info">// 设定清晰的目标，追踪每一步成长</p>
        </div>
        <button class="btn-create-brutal" @click="openModal('create')">
          <span class="cross">+</span> New Target
        </button>
      </div>
      <div class="header-border"></div>
    </header>

    <!-- 2. 状态筛选 Tabs -->
    <div class="tabs-brutal">
      <span 
        v-for="tab in tabs" 
        :key="tab.value"
        :class="['tab-brutal-item', { active: currentTab === tab.value }]"
        @click="currentTab = tab.value"
      >
        [{{ tab.label }}]
      </span>
    </div>

    <!-- 3. 目标卡片列表 -->
    <div class="grid-brutal">
      <div 
        v-for="goal in filteredGoals" 
        :key="goal.id" 
        class="card-brutal"
        :class="{ 'is-done': goal.status === 1 }"
      >
        <!-- 卡片头部 -->
        <div class="card-header-brutal">
          <span class="status-tag" :class="getStatusClass(goal.status)">
            {{ getStatusText(goal.status) }}
          </span>
          <div class="action-icons">
            <span class="icon-action" @click="openModal('edit', goal)">✏️</span>
            <span class="icon-action" @click="deleteGoal(goal.id)">🗑️</span>
          </div>
        </div>

        <!-- 标题与描述 -->
        <div class="card-title-block">
          <h3>{{ goal.title }}</h3>
          <p>{{ goal.description || 'No Description' }}</p>
        </div>
        
        <!-- 时间范围 -->
        <div class="date-block">
          <span class="date-label">RANGE:</span>
          {{ formatDate(goal.start_date) }} ➔ {{ formatDate(goal.end_date) }}
        </div>

        <!-- 进度可视化 -->
        <div class="progress-block-brutal">
          <div class="progress-meta">
            <span class="meta-label">PROGRESS</span>
            <span class="meta-value" :style="{ color: getProgressColor(goal) }">
              {{ calculatePercent(goal) }}%
            </span>
          </div>
          <div class="track-brutal">
            <div class="bar-brutal" 
              :style="{ width: calculatePercent(goal) + '%', background: getProgressColor(goal) }">
            </div>
          </div>
          <div class="stats-row">
            <span>CUR: {{ goal.current_value || 0 }}</span>
            <span>TGT: {{ goal.target_value }}</span>
          </div>
        </div>

      </div>

      <!-- 空状态 -->
      <div v-if="filteredGoals.length === 0" class="empty-brutal">
        <span class="empty-icon">[ ]</span>
        <p>SYSTEM_EMPTY: No targets found.</p>
      </div>
    </div>

    <!-- 分页组件 -->
    <div class="pagination-brutal" v-if="total > 0">
      <div class="page-stat">
        TOTAL: {{ total }} // PAGE: {{ currentPage }} / {{ Math.ceil(total / pageSize) }}
      </div>
      <div class="page-ctrl">
        <button 
          class="page-btn-brutal"
          :disabled="currentPage <= 1"
          @click="changePage(currentPage - 1)"
        >
          ◀ PREV
        </button>
        
        <span class="current-p">{{ currentPage }}</span>
        
        <button 
          class="page-btn-brutal"
          :disabled="currentPage >= Math.ceil(total / pageSize)"
          @click="changePage(currentPage + 1)"
        >
          NEXT ▶
        </button>
      </div>
    </div>

    <!-- 4. 创建/编辑 弹窗 -->
    <div v-if="showModal" class="modal-brutal-overlay">
      <div class="modal-brutal-content">
        <div class="modal-brutal-head">
          <h3>{{ isEditing ? 'EDIT_TARGET' : 'NEW_TARGET' }}</h3>
          <button class="close-x" @click="closeModal">✕</button>
        </div>
        
        <div class="modal-brutal-body form-layout-brutal">
          <div class="form-item-brutal full-w">
            <label>TITLE:</label>
            <input v-model="form.title" type="text" placeholder="Enter target name..." />
          </div>

          <div class="form-item-brutal full-w">
            <label>DESCRIPTION:</label>
            <textarea v-model="form.description" rows="3" placeholder="Details..."></textarea>
          </div>

          <div class="form-item-brutal">
            <label>TARGET_VALUE:</label>
            <input v-model.number="form.target_value" type="number" />
          </div>

          <div class="form-item-brutal">
            <label>CURRENT_VALUE:</label>
            <input v-model.number="form.current_value" type="number" />
          </div>

          <div class="form-item-brutal">
            <label>START_DATE:</label>
            <input v-model="form.start_date" type="date" />
          </div>

          <div class="form-item-brutal">
            <label>END_DATE:</label>
            <input v-model="form.end_date" type="date" />
          </div>
        </div>

        <div class="modal-brutal-footer">
          <button class="btn-cancel-brutal" @click="closeModal">CANCEL</button>
          <button class="btn-save-brutal" @click="saveGoal">SAVE</button>
        </div>
      </div>
    </div>

  </div>
</template>

<script setup>
import { ref, computed, reactive, onMounted, watch } from 'vue';
import { createGoalAPI, getGoalListAPI, updateGoalStatusAPI, deleteGoalAPI, updateGoalAPI } from '../utils/api';
import { ElMessage } from 'element-plus';

const goals = ref([]);
const currentPage = ref(1);
const pageSize = ref(10);
const total = ref(0);

const tabs = [
  { label: 'ALL', value: 'all' },
  { label: 'IN_PROGRESS', value: 0 },
  { label: 'COMPLETED', value: 1 }
];
const currentTab = ref('all');

const showModal = ref(false);
const isEditing = ref(false);
const form = reactive({
  id: null,
  title: '',
  description: '',
  target_value: 100,
  current_value: 0,
  start_date: '',
  end_date: '',
  status: 0
});

const filteredGoals = computed(() => {
  if (currentTab.value === 'all') return goals.value;
  return goals.value.filter(g => g.status === currentTab.value);
});

const calculatePercent = (goal) => {
  if (!goal.target_value) return 0;
  const currentValue = goal.current_value || 0;
  const p = Math.round((currentValue / goal.target_value) * 100);
  return p > 100 ? 100 : p;
};

const getProgressColor = (goal) => {
  if (goal.status === 1) return '#000000';
  const p = calculatePercent(goal);
  if (p < 30) return '#ef4444';
  if (p < 70) return '#eab308';
  return '#111111';
};

const getStatusText = (status) => status === 1 ? 'DONE' : 'ACTIVE';
const getStatusClass = (status) => status === 1 ? 'tag-done' : 'tag-active';

const formatDate = (dateStr) => {
  if (!dateStr) return '??';
  return dateStr.replace(/-/g, '/');
};

const openModal = (type, goal = null) => {
  isEditing.value = type === 'edit';
  showModal.value = true;
  
  if (type === 'edit' && goal) {
    Object.assign(form, goal);
  } else {
    Object.assign(form, {
      id: null, title: '', description: '', 
      target_value: 10,
      current_value: 0,
      start_date: new Date().toISOString().split('T')[0],
      end_date: '', status: 0
    });
  }
};

const closeModal = () => showModal.value = false;

const saveGoal = async () => {
  if (!form.title) return alert('Title is required');

  if (isEditing.value) {
    try {
      const goalData = {
        id: form.id,
        title: form.title,
        description: form.description,
        startDate: `${form.start_date} 00:00:00`,
        endDate: `${form.end_date} 23:59:59`,
        targetValue: form.target_value,
        currentValue: form.current_value
      };
      
      const result = await updateGoalAPI(goalData);
      
      if (result) {
        await fetchGoals();
        ElMessage.success('Target updated');
        closeModal();
      } else {
        ElMessage.error('Update failed');
      }
    } catch (error) {
      console.error(error);
      ElMessage.error('Error: ' + (error.message || 'Unknown'));
    }
  } else {
    try {
      const goalData = {
        title: form.title,
        description: form.description,
        startDate: `${form.start_date} 00:00:00`,
        endDate: `${form.end_date} 23:59:59`,
        targetValue: form.target_value,
        currentValue: form.current_value
      };
      
      const result = await createGoalAPI(goalData);
      
      if (result) {
        await fetchGoals();
        ElMessage.success('Target created');
        closeModal();
      } else {
        ElMessage.error('Creation failed');
      }
    } catch (error) {
      console.error(error);
      ElMessage.error('Error: ' + (error.message || 'Unknown'));
    }
  }
};

const deleteGoal = async (id) => {
  if(confirm('Delete this target?')) {
    try {
      const result = await deleteGoalAPI(id);
      
      if (result !== undefined) {
        await fetchGoals();
        ElMessage.success('Target deleted');
      } else {
        ElMessage.error('Delete failed');
      }
    } catch (error) {
      console.error(error);
      ElMessage.error('Error: ' + (error.message || 'Unknown'));
    }
  }
};

const fetchGoals = async () => {
  try {
    let statusParam = "";
    if(currentTab.value !== 'all') {
      statusParam = currentTab.value === 0 ? 'progress' : 'completed';
    }
    
    const params = {
      currentPage: currentPage.value,
      pageSize: pageSize.value,
      status: statusParam
    };
    
    const response = await getGoalListAPI(params);
    
    if(response && response.data) {
      goals.value = response.data.map(item => ({
        id: item.id,
        title: item.title,
        description: item.description,
        start_date: item.startDate.split(' ')[0],
        end_date: item.endDate.split(' ')[0],
        target_value: item.targetValue,
        current_value: item.currentValue,
        status: item.status === 'completed' ? 1 : 0
      }));
      
      total.value = response.data.length;
    }
  } catch (error) {
    console.error(error);
    ElMessage.error('Fetch failed');
  }
};

onMounted(() => {
  fetchGoals();
});

watch(currentTab, () => {
  currentPage.value = 1;
  fetchGoals();
});

const changePage = (page) => {
  if (page < 1 || page > Math.ceil(total.value / pageSize.value)) {
    return;
  }
  currentPage.value = page;
  fetchGoals();
};
</script>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=JetBrains+Mono:wght@400;600;800&display=swap');

.goal-brutalist-container {
  padding: 40px;
  background-color: #E5E5E5;
  min-height: 100vh;
  font-family: 'JetBrains Mono', monospace;
  color: #111;
}

.header-brutal {
  margin-bottom: 30px;
}

.header-content {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  margin-bottom: 15px;
}

.title-block h2 {
  font-size: 2.5rem;
  margin: 0;
  font-weight: 800;
  text-transform: uppercase;
  letter-spacing: -2px;
}

.title-block .accent {
  color: #666;
}

.sys-info {
  margin: 5px 0 0 0;
  font-size: 0.85rem;
  color: #666;
}

.header-border {
  height: 4px;
  background: #111;
}

.btn-create-brutal {
  background: #111;
  color: #fff;
  border: none;
  padding: 12px 24px;
  font-weight: 700;
  text-transform: uppercase;
  cursor: pointer;
  font-family: inherit;
  display: flex;
  align-items: center;
  gap: 8px;
  transition: transform 0.1s;
}

.btn-create-brutal:hover {
  transform: translate(-2px, -2px);
  box-shadow: 4px 4px 0 #666;
}

.cross {
  font-size: 1.2rem;
}

/* Tabs */
.tabs-brutal {
  display: flex;
  gap: 15px;
  margin-bottom: 30px;
}

.tab-brutal-item {
  padding: 8px 16px;
  background: #fff;
  border: 2px solid #111;
  cursor: pointer;
  font-weight: 600;
  transition: all 0.1s;
}

.tab-brutal-item.active {
  background: #111;
  color: #fff;
}

/* Grid */
.grid-brutal {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 25px;
}

.card-brutal {
  background: #fff;
  border: 2px solid #111;
  padding: 24px;
  display: flex;
  flex-direction: column;
  transition: all 0.15s ease;
}

.card-brutal.is-done {
  background: #f0f0f0;
  border-style: dashed;
}

.card-brutal:hover {
  transform: translate(-4px, -4px);
  box-shadow: 8px 8px 0 #111;
}

.card-header-brutal {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.status-tag {
  padding: 2px 8px;
  font-size: 11px;
  font-weight: 700;
}

.tag-active { background: #111; color: #fff; }
.tag-done { background: transparent; border: 1px solid #111; color: #666; }

.action-icons {
  display: flex;
  gap: 8px;
  opacity: 0.5;
  transition: opacity 0.2s;
}

.card-brutal:hover .action-icons {
  opacity: 1;
}

.icon-action {
  cursor: pointer;
}

.card-title-block h3 {
  margin: 0 0 10px 0;
  font-size: 1.25rem;
  text-transform: uppercase;
}

.card-title-block p {
  margin: 0 0 15px 0;
  font-size: 0.85rem;
  color: #666;
  min-height: 40px;
  border-left: 2px solid #ccc;
  padding-left: 10px;
}

.date-block {
  font-size: 0.75rem;
  color: #444;
  margin-bottom: 20px;
  background: #f5f5f5;
  padding: 5px;
}

.date-label {
  font-weight: 700;
  margin-right: 5px;
}

/* Progress */
.progress-block-brutal {
  margin-top: auto;
}

.progress-meta {
  display: flex;
  justify-content: space-between;
  font-size: 12px;
  margin-bottom: 5px;
}

.meta-label {
  font-weight: 700;
}

.meta-value {
  font-weight: 800;
  font-size: 14px;
}

.track-brutal {
  height: 12px;
  background: #eee;
  border: 1px solid #111;
  position: relative;
}

.bar-brutal {
  height: 100%;
  transition: width 0.5s ease;
}

.stats-row {
  display: flex;
  justify-content: space-between;
  margin-top: 5px;
  font-size: 10px;
  color: #666;
}

/* Empty */
.empty-brutal {
  grid-column: 1 / -1;
  text-align: center;
  padding: 60px;
  border: 2px dashed #aaa;
  color: #666;
}

.empty-icon {
  font-size: 2rem;
  margin-bottom: 10px;
  display: block;
}

/* Pagination */
.pagination-brutal {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 40px;
  padding: 15px;
  background: #fff;
  border: 2px solid #111;
}

.page-stat {
  font-size: 12px;
  font-weight: 600;
}

.page-ctrl {
  display: flex;
  align-items: center;
  gap: 10px;
}

.page-btn-brutal {
  background: #fff;
  border: 2px solid #111;
  padding: 6px 12px;
  font-weight: 700;
  font-family: inherit;
  cursor: pointer;
  transition: all 0.1s;
}

.page-btn-brutal:hover:not(:disabled) {
  background: #111;
  color: #fff;
}

.page-btn-brutal:disabled {
  opacity: 0.3;
  cursor: not-allowed;
}

.current-p {
  font-weight: 800;
  padding: 0 10px;
  border-top: 2px solid #111;
  border-bottom: 2px solid #111;
}

/* Modal */
.modal-brutal-overlay {
  position: fixed; top: 0; left: 0; right: 0; bottom: 0;
  background: rgba(255, 255, 255, 0.9);
  display: flex; align-items: center; justify-content: center; z-index: 100;
  border: 10px solid #111;
}

.modal-brutal-content {
  background: #fff;
  width: 500px;
  border: 2px solid #111;
  box-shadow: 10px 10px 0 #111;
}

.modal-brutal-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 15px 20px;
  background: #111;
  color: #fff;
}

.modal-brutal-head h3 { margin: 0; text-transform: uppercase; letter-spacing: 1px; }
.close-x { background: none; border: none; color: #fff; font-size: 20px; cursor: pointer; }

.form-layout-brutal {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 15px;
  padding: 20px;
}

.full-w { grid-column: span 2; }

.form-item-brutal label { 
  display: block; 
  font-size: 11px; 
  font-weight: 700; 
  margin-bottom: 5px;
  text-transform: uppercase;
}

.form-item-brutal input, 
.form-item-brutal textarea {
  width: 100%; 
  padding: 8px 10px; 
  border: 2px solid #111; 
  font-family: inherit; 
  box-sizing: border-box;
  font-size: 14px;
}

.form-item-brutal input:focus, 
.form-item-brutal textarea:focus { 
  outline: none; 
  background: #f0f0f0; 
}

.modal-brutal-footer {
  display: flex; 
  justify-content: flex-end; 
  gap: 10px; 
  padding: 20px;
  border-top: 2px solid #111;
}

.btn-cancel-brutal, .btn-save-brutal {
  padding: 10px 20px;
  font-weight: 700;
  text-transform: uppercase;
  font-family: inherit;
  cursor: pointer;
}

.btn-cancel-brutal {
  background: #fff;
  border: 2px solid #111;
}

.btn-save-brutal {
  background: #111;
  color: #fff;
  border: 2px solid #111;
}
</style>