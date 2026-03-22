<template>
  <div class="mock-home">
    <div class="page-header">
      <h2 class="page-title">
        <el-icon class="title-icon"><Timer /></el-icon>
        模考模式
      </h2>
      <p class="page-subtitle">限时模拟考试，题号导航，倒计时交卷，考后查看详细报告</p>
    </div>

    <!-- ===== 配置卡片 ===== -->
    <el-card class="config-card" shadow="never">
      <template #header>
        <span class="card-header-text">新建模考</span>
      </template>

      <el-form :model="form" label-position="top" :rules="rules" ref="formRef">
        <el-row :gutter="24">
          <!-- 科目 -->
          <el-col :xs="24" :sm="12" :md="8">
            <el-form-item label="考试科目" prop="subjectId">
              <el-select v-model="form.subjectId" placeholder="请选择科目" class="full-width">
                <el-option
                  v-for="s in subjects"
                  :key="s.id"
                  :label="s.name"
                  :value="s.id"
                />
              </el-select>
            </el-form-item>
          </el-col>

          <!-- 难度 -->
          <el-col :xs="24" :sm="12" :md="8">
            <el-form-item label="难度">
              <div class="tag-selector">
                <el-tag
                  v-for="d in difficultyOptions"
                  :key="d.value"
                  :type="form.difficulty === d.value ? '' : 'info'"
                  class="tag-btn"
                  :class="{ active: form.difficulty === d.value }"
                  @click="form.difficulty = d.value"
                >
                  {{ d.label }}
                </el-tag>
              </div>
            </el-form-item>
          </el-col>

          <!-- 题目数量 -->
          <el-col :xs="24" :sm="12" :md="8">
            <el-form-item label="题目数量" prop="count">
              <div class="tag-selector">
                <el-tag
                  v-for="n in countOptions"
                  :key="n"
                  :type="form.count === n ? '' : 'info'"
                  class="tag-btn"
                  :class="{ active: form.count === n }"
                  @click="form.count = n"
                >
                  {{ n }} 题
                </el-tag>
              </div>
            </el-form-item>
          </el-col>

          <!-- 考试时长 -->
          <el-col :xs="24" :sm="12" :md="8">
            <el-form-item label="考试时长" prop="timeLimitMinutes">
              <div class="tag-selector">
                <el-tag
                  v-for="t in timeOptions"
                  :key="t"
                  :type="form.timeLimitMinutes === t ? '' : 'info'"
                  class="tag-btn"
                  :class="{ active: form.timeLimitMinutes === t }"
                  @click="form.timeLimitMinutes = t"
                >
                  {{ t }} 分钟
                </el-tag>
              </div>
            </el-form-item>
          </el-col>
        </el-row>

        <div class="start-action">
          <el-button
            type="primary"
            size="large"
            :loading="starting"
            :disabled="!form.subjectId"
            @click="handleStart"
            class="start-btn"
          >
            <el-icon><VideoPlay /></el-icon>
            开始模考
          </el-button>
          <p v-if="!form.subjectId" class="hint-text">请先选择科目</p>
        </div>
      </el-form>
    </el-card>

    <!-- ===== 历史记录 ===== -->
    <el-card class="history-card" shadow="never" v-loading="historyLoading">
      <template #header>
        <div class="card-header-row">
          <span class="card-header-text">历史记录</span>
          <el-button text @click="loadHistory">
            <el-icon><Refresh /></el-icon>
            刷新
          </el-button>
        </div>
      </template>

      <el-empty v-if="!historyLoading && history.length === 0" description="暂无历史记录" />

      <div v-else class="history-list">
        <div
          v-for="item in history"
          :key="item.examId"
          class="history-item"
          @click="viewReport(item.examId)"
        >
          <div class="history-main">
            <span class="history-title">{{ item.title }}</span>
            <el-tag :type="statusType(item.status)" size="small" class="history-status">
              {{ statusLabel(item.status) }}
            </el-tag>
          </div>
          <div class="history-meta">
            <span class="meta-item">
              <el-icon><DocumentChecked /></el-icon>
              {{ item.totalCorrect }}/{{ item.totalQuestions }} 题
            </span>
            <span class="meta-item accuracy" :class="accuracyClass(item.accuracy)">
              {{ formatPercent(item.accuracy) }}
            </span>
            <span class="meta-item">
              <el-icon><Clock /></el-icon>
              限时 {{ item.timeLimitMin }} 分钟
            </span>
            <span class="meta-item date">{{ formatDate(item.startedAt) }}</span>
          </div>
        </div>
      </div>

      <!-- 加载更多 -->
      <div v-if="hasMore" class="load-more">
        <el-button text @click="loadMore" :loading="loadingMore">加载更多</el-button>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Timer, VideoPlay, Refresh, DocumentChecked, Clock } from '@element-plus/icons-vue'
import { getSubjects } from '@/api/subject'
import {
  startMock,
  getMockHistory,
  type MockHistoryVO,
} from '@/api/mock'

const router = useRouter()
const formRef = ref()
const starting = ref(false)
const historyLoading = ref(false)
const loadingMore = ref(false)
const subjects = ref<{ id: number; name: string }[]>([])
const history = ref<MockHistoryVO[]>([])
const page = ref(1)
const hasMore = ref(false)

const form = reactive({
  subjectId: null as number | null,
  difficulty: 2,
  count: 10,
  timeLimitMinutes: 30,
})

const rules = {
  subjectId: [{ required: true, message: '请选择科目', trigger: 'change' }],
}

const difficultyOptions = [
  { label: '简单', value: 1 },
  { label: '中等', value: 2 },
  { label: '困难', value: 3 },
]

const countOptions = [5, 10, 15, 20]
const timeOptions = [15, 30, 45, 60]

// ===== 加载科目 =====

async function loadSubjects() {
  try {
    const res = await getSubjects()
    subjects.value = res.data || []
  } catch {
    /* 静默处理 */
  }
}

// ===== 开始模考 =====

async function handleStart() {
  await formRef.value?.validate()
  starting.value = true
  try {
    const res = await startMock({
      subjectId: form.subjectId!,
      count: form.count,
      timeLimitMinutes: form.timeLimitMinutes,
      difficulty: form.difficulty,
    })
    router.push(`/mock/answer/${res.data.examId}`)
  } catch {
    /* ElMessage 已在拦截器统一处理 */
  } finally {
    starting.value = false
  }
}

// ===== 历史记录 =====

async function loadHistory() {
  historyLoading.value = true
  page.value = 1
  try {
    const res = await getMockHistory(1, 10)
    history.value = res.data || []
    hasMore.value = history.value.length === 10
  } finally {
    historyLoading.value = false
  }
}

async function loadMore() {
  loadingMore.value = true
  try {
    page.value++
    const res = await getMockHistory(page.value, 10)
    const more = res.data || []
    history.value.push(...more)
    hasMore.value = more.length === 10
  } finally {
    loadingMore.value = false
  }
}

function viewReport(examId: number) {
  router.push(`/mock/report/${examId}`)
}

// ===== 工具方法 =====

function statusLabel(status: number) {
  return status === 1 ? '已完成' : status === 2 ? '超时' : '进行中'
}

function statusType(status: number): '' | 'success' | 'warning' | 'info' {
  return status === 1 ? 'success' : status === 2 ? 'warning' : 'info'
}

function formatPercent(val: number) {
  return (val * 100).toFixed(1) + '%'
}

function accuracyClass(val: number) {
  if (val >= 0.8) return 'text-success'
  if (val >= 0.6) return 'text-warning'
  return 'text-danger'
}

function formatDate(dateStr: string) {
  if (!dateStr) return ''
  return dateStr.replace('T', ' ').substring(0, 16)
}

onMounted(() => {
  loadSubjects()
  loadHistory()
})
</script>

<style scoped>
.mock-home {
  max-width: 960px;
  margin: 0 auto;
  padding: 24px 16px;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.page-header {
  margin-bottom: 4px;
}

.page-title {
  font-size: 22px;
  font-weight: 700;
  color: var(--el-text-color-primary);
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 0 0 6px;
}

.title-icon {
  color: var(--el-color-primary);
}

.page-subtitle {
  color: var(--el-text-color-secondary);
  font-size: 14px;
  margin: 0;
}

.config-card,
.history-card {
  border-radius: 12px;
}

.card-header-text {
  font-weight: 600;
  font-size: 15px;
}

.card-header-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.full-width {
  width: 100%;
}

.tag-selector {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.tag-btn {
  cursor: pointer;
  user-select: none;
  transition: all 0.2s;
  border-radius: 6px;
  padding: 4px 14px;
  height: 32px;
  line-height: 22px;
}

.tag-btn.active {
  font-weight: 600;
}

.start-action {
  margin-top: 8px;
  display: flex;
  align-items: center;
  gap: 16px;
}

.start-btn {
  padding: 12px 36px;
  font-size: 15px;
  border-radius: 8px;
}

.hint-text {
  color: var(--el-text-color-secondary);
  font-size: 13px;
  margin: 0;
}

/* 历史记录 */
.history-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.history-item {
  border: 1px solid var(--el-border-color-light);
  border-radius: 8px;
  padding: 14px 16px;
  cursor: pointer;
  transition: all 0.2s;
}

.history-item:hover {
  border-color: var(--el-color-primary);
  background: var(--el-color-primary-light-9);
}

.history-main {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 8px;
}

.history-title {
  font-weight: 600;
  font-size: 15px;
  color: var(--el-text-color-primary);
}

.history-meta {
  display: flex;
  align-items: center;
  gap: 20px;
  flex-wrap: wrap;
}

.meta-item {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 13px;
  color: var(--el-text-color-secondary);
}

.meta-item.accuracy {
  font-weight: 600;
  font-size: 14px;
}

.meta-item.date {
  margin-left: auto;
}

.text-success { color: var(--el-color-success); }
.text-warning { color: var(--el-color-warning); }
.text-danger { color: var(--el-color-danger); }

.load-more {
  text-align: center;
  margin-top: 12px;
}
</style>
