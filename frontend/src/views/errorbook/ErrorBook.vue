<template>
  <div class="error-book">
    <!-- 页头 -->
    <div class="page-header">
      <div class="header-left">
        <h2 class="page-title">错题本</h2>
        <el-tag v-if="todayCount > 0" type="danger" effect="dark" class="today-badge">
          今日待复做 {{ todayCount }} 题
        </el-tag>
      </div>
      <div class="header-actions">
        <el-button
          v-if="todayCount > 0"
          type="primary"
          :icon="Clock"
          @click="showTodayReview = true"
        >
          开始今日复习
        </el-button>
      </div>
    </div>

    <!-- 筛选栏 -->
    <el-card class="filter-card" shadow="never">
      <el-row :gutter="12" align="middle">
        <el-col :span="8">
          <el-select
            v-model="filters.subjectId"
            placeholder="全部科目"
            clearable
            @change="loadErrors"
          >
            <el-option
              v-for="s in subjects"
              :key="s.id"
              :label="s.name"
              :value="s.id"
            />
          </el-select>
        </el-col>
        <el-col :span="8">
          <el-select
            v-model="filters.isMastered"
            placeholder="全部状态"
            clearable
            @change="loadErrors"
          >
            <el-option label="未掌握" :value="0" />
            <el-option label="已掌握" :value="1" />
          </el-select>
        </el-col>
        <el-col :span="8">
          <span class="total-count">共 {{ errorList.length }} 条错题</span>
        </el-col>
      </el-row>
    </el-card>

    <!-- 错题列表 -->
    <div v-loading="loading" class="error-list">
      <el-empty v-if="!loading && errorList.length === 0" description="暂无错题，继续加油！" />

      <el-card
        v-for="item in errorList"
        :key="item.id"
        class="error-card"
        shadow="hover"
        :class="{ mastered: item.isMastered === 1 }"
      >
        <!-- 卡片头部 -->
        <template #header>
          <div class="card-header">
            <div class="card-meta">
              <el-tag size="small" :type="difficultyType(item.difficulty)">
                {{ difficultyLabel(item.difficulty) }}
              </el-tag>
              <el-tag size="small" type="info">{{ questionTypeLabel(item.questionType) }}</el-tag>
              <span v-if="item.subjectName" class="subject-name">{{ item.subjectName }}</span>
            </div>
            <div class="card-actions">
              <!-- 错因标签 -->
              <el-select
                :model-value="item.errorReason"
                placeholder="设置错因"
                size="small"
                style="width: 120px"
                clearable
                @change="(val: number) => handleSetReason(item, val)"
              >
                <el-option
                  v-for="(label, key) in ERROR_REASON_MAP"
                  :key="key"
                  :label="label"
                  :value="Number(key)"
                />
              </el-select>
              <!-- 掌握状态 -->
              <el-tag
                :type="item.isMastered === 1 ? 'success' : 'warning'"
                size="small"
              >
                {{ item.isMastered === 1 ? '已掌握' : '未掌握' }}
              </el-tag>
            </div>
          </div>
        </template>

        <!-- 题干 -->
        <div class="question-content">{{ item.questionContent }}</div>

        <!-- 选项（选择题） -->
        <div v-if="parsedOptions(item)" class="options-list">
          <div
            v-for="opt in parsedOptions(item)"
            :key="opt.key"
            class="option-item"
            :class="{ correct: isCorrectOption(item, opt.key) }"
          >
            <span class="option-key">{{ opt.key }}.</span>
            <span class="option-value">{{ opt.value }}</span>
          </div>
        </div>

        <!-- 答案 -->
        <div class="answer-row">
          <span class="label">正确答案：</span>
          <span class="answer-text">{{ item.answer }}</span>
        </div>

        <!-- SRS 进度 -->
        <div class="srs-info">
          <el-icon><Refresh /></el-icon>
          <span>复做 {{ item.reviewCount }} 次</span>
          <el-divider direction="vertical" />
          <span>间隔 {{ item.reviewIntervalDays }} 天</span>
          <template v-if="item.nextReviewAt">
            <el-divider direction="vertical" />
            <span>下次复习：{{ formatDate(item.nextReviewAt) }}</span>
          </template>
        </div>

        <!-- 解析（可折叠） -->
        <el-collapse v-if="item.explanation" class="explanation-collapse">
          <el-collapse-item title="查看解析" name="1">
            <div class="explanation-text">{{ item.explanation }}</div>
          </el-collapse-item>
        </el-collapse>
      </el-card>
    </div>

    <!-- 今日复习抽屉 -->
    <el-drawer
      v-model="showTodayReview"
      title="今日复习计划"
      direction="rtl"
      size="50%"
      destroy-on-close
    >
      <TodayReview @done="handleReviewDone" />
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Clock, Refresh } from '@element-plus/icons-vue'
import {
  getErrors,
  getTodayReview,
  setErrorReason,
  ERROR_REASON_MAP,
  type ErrorBookItem,
  type ErrorReason,
} from '@/api/errorbook'
import { getSubjects } from '@/api/subject'
import TodayReview from './components/TodayReview.vue'

// ===== 状态 =====
const loading = ref(false)
const errorList = ref<ErrorBookItem[]>([])
const todayCount = ref(0)
const showTodayReview = ref(false)
const subjects = ref<{ id: number; name: string }[]>([])
const filters = ref<{ subjectId?: number; isMastered?: 0 | 1 }>({})

// ===== 初始化 =====
onMounted(async () => {
  await Promise.all([loadErrors(), loadTodayCount(), loadSubjects()])
})

async function loadErrors() {
  loading.value = true
  try {
    const res = await getErrors(filters.value)
    errorList.value = res.data ?? []
  } finally {
    loading.value = false
  }
}

async function loadTodayCount() {
  try {
    const res = await getTodayReview()
    todayCount.value = res.data?.length ?? 0
  } catch {
    todayCount.value = 0
  }
}

async function loadSubjects() {
  try {
    const res = await getSubjects()
    subjects.value = res.data ?? []
  } catch {
    // 忽略
  }
}

// ===== 操作 =====
async function handleSetReason(item: ErrorBookItem, reason: number) {
  if (!reason) return
  try {
    await setErrorReason(item.id, { reason: reason as ErrorReason })
    item.errorReason = reason as ErrorReason
    ElMessage.success('错因标签已设置')
  } catch {
    ElMessage.error('设置失败，请重试')
  }
}

function handleReviewDone() {
  showTodayReview.value = false
  loadErrors()
  loadTodayCount()
}

// ===== 工具函数 =====
function difficultyType(d: number) {
  return d === 1 ? 'success' : d === 2 ? 'warning' : 'danger'
}

function difficultyLabel(d: number) {
  return ['', '简单', '中等', '困难'][d] ?? '未知'
}

function questionTypeLabel(t: number) {
  return ['', '单选', '多选', '判断', '填空'][t] ?? '未知'
}

function parsedOptions(item: ErrorBookItem) {
  if (!item.options) return null
  try {
    return JSON.parse(item.options) as { key: string; value: string }[]
  } catch {
    return null
  }
}

function isCorrectOption(item: ErrorBookItem, key: string) {
  return item.answer?.includes(key)
}

function formatDate(dateStr: string) {
  const d = new Date(dateStr)
  return `${d.getMonth() + 1}/${d.getDate()}`
}
</script>

<style scoped>
.error-book {
  max-width: 900px;
  margin: 0 auto;
  padding: 20px;
}

.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.page-title {
  margin: 0;
  font-size: 22px;
  font-weight: 600;
  color: var(--el-text-color-primary);
}

.today-badge {
  font-size: 13px;
}

.filter-card {
  margin-bottom: 20px;
}

.filter-card :deep(.el-card__body) {
  padding: 12px 16px;
}

.total-count {
  color: var(--el-text-color-secondary);
  font-size: 14px;
}

.error-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.error-card {
  transition: box-shadow 0.2s;
}

.error-card.mastered {
  opacity: 0.75;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.card-meta {
  display: flex;
  align-items: center;
  gap: 6px;
}

.subject-name {
  color: var(--el-text-color-secondary);
  font-size: 13px;
  margin-left: 4px;
}

.card-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.question-content {
  font-size: 15px;
  line-height: 1.7;
  color: var(--el-text-color-primary);
  margin-bottom: 12px;
}

.options-list {
  margin-bottom: 12px;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.option-item {
  display: flex;
  gap: 6px;
  padding: 6px 10px;
  border-radius: 6px;
  background: var(--el-fill-color-lighter);
  font-size: 14px;
  color: var(--el-text-color-regular);
}

.option-item.correct {
  background: var(--el-color-success-light-9);
  color: var(--el-color-success);
  font-weight: 500;
}

.option-key {
  font-weight: 600;
  min-width: 18px;
}

.answer-row {
  font-size: 14px;
  margin-bottom: 8px;
}

.answer-row .label {
  color: var(--el-text-color-secondary);
}

.answer-row .answer-text {
  color: var(--el-color-success);
  font-weight: 600;
}

.srs-info {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: var(--el-text-color-placeholder);
  margin-top: 8px;
}

.explanation-collapse {
  margin-top: 10px;
}

.explanation-text {
  font-size: 14px;
  line-height: 1.7;
  color: var(--el-text-color-secondary);
  white-space: pre-wrap;
}
</style>
