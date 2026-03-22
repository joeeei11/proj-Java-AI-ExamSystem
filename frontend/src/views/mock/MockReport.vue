<template>
  <div class="mock-report" v-loading="loading">
    <template v-if="report">
      <!-- ===== 报告头部 ===== -->
      <div class="report-header">
        <div class="header-left">
          <h2 class="report-title">{{ report.title }} - 成绩报告</h2>
          <div class="report-meta">
            <el-tag :type="statusType" size="small">{{ statusLabel }}</el-tag>
            <span class="meta-sep">·</span>
            <span>用时 {{ formatDuration(report.durationSeconds) }}</span>
          </div>
        </div>
        <el-button text @click="router.push('/mock')">
          <el-icon><ArrowLeft /></el-icon>
          返回
        </el-button>
      </div>

      <!-- ===== 成绩概览 ===== -->
      <div class="score-section">
        <!-- 环形图 -->
        <div class="score-chart-wrap">
          <div ref="chartRef" class="score-chart"></div>
        </div>

        <!-- 数字统计 -->
        <div class="score-stats">
          <div class="stat-item">
            <span class="stat-value primary">{{ formatPercent(report.accuracy) }}</span>
            <span class="stat-label">正确率</span>
          </div>
          <div class="stat-divider"></div>
          <div class="stat-item">
            <span class="stat-value success">{{ report.totalCorrect }}</span>
            <span class="stat-label">答对</span>
          </div>
          <div class="stat-divider"></div>
          <div class="stat-item">
            <span class="stat-value danger">{{ report.totalQuestions - report.totalCorrect }}</span>
            <span class="stat-label">答错</span>
          </div>
          <div class="stat-divider"></div>
          <div class="stat-item">
            <span class="stat-value">{{ report.totalQuestions }}</span>
            <span class="stat-label">总题数</span>
          </div>
        </div>

        <!-- 等级评价 -->
        <div class="grade-badge" :class="gradeClass">
          <span class="grade-letter">{{ grade }}</span>
          <span class="grade-text">{{ gradeText }}</span>
        </div>
      </div>

      <!-- ===== 逐题解析 ===== -->
      <div class="question-list">
        <div class="section-title">逐题解析</div>

        <div
          v-for="q in report.questions"
          :key="q.questionId"
          class="question-block"
          :class="q.correct ? 'block-correct' : 'block-wrong'"
        >
          <!-- 题目头 -->
          <div class="q-header">
            <div class="q-number-badge" :class="q.correct ? 'badge-correct' : 'badge-wrong'">
              {{ q.sortOrder }}
            </div>
            <el-tag size="small" :type="typeTagType(q.type)">{{ typeLabel(q.type) }}</el-tag>
            <el-icon v-if="q.correct" class="result-icon correct"><CircleCheck /></el-icon>
            <el-icon v-else class="result-icon wrong"><CircleClose /></el-icon>
            <span class="result-text" :class="q.correct ? 'text-success' : 'text-danger'">
              {{ q.correct ? '回答正确' : '回答错误' }}
            </span>
          </div>

          <!-- 题干 -->
          <div class="q-content">{{ q.content }}</div>

          <!-- 选项列表（带高亮） -->
          <div v-if="q.options && q.options.length > 0" class="options-wrap">
            <div
              v-for="opt in q.options"
              :key="opt.key"
              class="option-row"
              :class="optionClass(opt.key, q)"
            >
              <span class="opt-key">{{ opt.key }}.</span>
              <span class="opt-value">{{ opt.value }}</span>
              <span v-if="opt.key === q.correctAnswer" class="opt-marker correct-marker">
                <el-icon><CircleCheck /></el-icon> 正确
              </span>
              <span
                v-else-if="q.userAnswer === opt.key && opt.key !== q.correctAnswer"
                class="opt-marker wrong-marker"
              >
                <el-icon><CircleClose /></el-icon> 你的选择
              </span>
            </div>
          </div>

          <!-- 答案对比 -->
          <div class="answer-compare">
            <div class="answer-item user-answer">
              <span class="answer-label">你的答案：</span>
              <span
                class="answer-value"
                :class="q.correct ? 'text-success' : 'text-danger'"
              >
                {{ q.userAnswer || '未作答' }}
              </span>
            </div>
            <div class="answer-item correct-answer">
              <span class="answer-label">正确答案：</span>
              <span class="answer-value text-success">{{ q.correctAnswer }}</span>
            </div>
          </div>

          <!-- 解析 -->
          <div v-if="q.explanation" class="explanation">
            <div class="explanation-label">
              <el-icon><InfoFilled /></el-icon>
              解析
            </div>
            <div class="explanation-content">{{ q.explanation }}</div>
          </div>
        </div>
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowLeft, CircleCheck, CircleClose, InfoFilled } from '@element-plus/icons-vue'
import { getMockReport, type MockReportVO, type MockReportQuestionItem } from '@/api/mock'
import * as echarts from 'echarts'

const route = useRoute()
const router = useRouter()
const examId = Number(route.params.examId)

const loading = ref(true)
const report = ref<MockReportVO | null>(null)
const chartRef = ref<HTMLElement | null>(null)

// ===== 计算属性 =====

const statusLabel = computed(() => {
  if (!report.value) return ''
  return report.value.status === 1 ? '已完成' : '超时'
})

const statusType = computed((): '' | 'success' | 'warning' => {
  if (!report.value) return ''
  return report.value.status === 1 ? 'success' : 'warning'
})

const grade = computed(() => {
  if (!report.value) return '-'
  const acc = report.value.accuracy
  if (acc >= 0.9) return 'S'
  if (acc >= 0.8) return 'A'
  if (acc >= 0.7) return 'B'
  if (acc >= 0.6) return 'C'
  return 'D'
})

const gradeText = computed(() => {
  const map: Record<string, string> = {
    S: '优秀', A: '良好', B: '中等', C: '及格', D: '需加油',
  }
  return map[grade.value] || ''
})

const gradeClass = computed(() => {
  const map: Record<string, string> = {
    S: 'grade-s', A: 'grade-a', B: 'grade-b', C: 'grade-c', D: 'grade-d',
  }
  return map[grade.value] || ''
})

// ===== 初始化 =====

onMounted(async () => {
  try {
    const res = await getMockReport(examId)
    report.value = res.data
    initChart()
  } finally {
    loading.value = false
  }
})

function initChart() {
  if (!chartRef.value || !report.value) return
  const chart = echarts.init(chartRef.value)
  const correct = report.value.totalCorrect
  const wrong = report.value.totalQuestions - correct

  chart.setOption({
    series: [
      {
        type: 'pie',
        radius: ['55%', '80%'],
        center: ['50%', '50%'],
        avoidLabelOverlap: false,
        label: {
          show: true,
          position: 'center',
          formatter: () => `${formatPercent(report.value!.accuracy)}`,
          fontSize: 22,
          fontWeight: 'bold',
          color: '#333',
        },
        data: [
          { value: correct, name: '正确', itemStyle: { color: '#67c23a' } },
          { value: wrong, name: '错误', itemStyle: { color: '#f56c6c' } },
        ],
      },
    ],
  })
}

// ===== 工具方法 =====

function formatPercent(val: number) {
  return (val * 100).toFixed(1) + '%'
}

function formatDuration(seconds: number) {
  const m = Math.floor(seconds / 60)
  const s = seconds % 60
  return `${m}分${s}秒`
}

function typeLabel(type: number) {
  const map: Record<number, string> = { 1: '单选', 2: '多选', 3: '判断', 4: '填空' }
  return map[type] || '单选'
}

function typeTagType(type: number): '' | 'success' | 'warning' | 'info' {
  const map: Record<number, '' | 'success' | 'warning' | 'info'> = {
    1: '', 2: 'success', 3: 'warning', 4: 'info',
  }
  return map[type] || ''
}

function optionClass(key: string, q: MockReportQuestionItem) {
  if (key === q.correctAnswer) return 'option-correct'
  if (q.userAnswer === key) return 'option-wrong'
  return ''
}
</script>

<style scoped>
.mock-report {
  max-width: 900px;
  margin: 0 auto;
  padding: 24px 16px;
}

/* ===== 报告头部 ===== */
.report-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  margin-bottom: 24px;
}

.report-title {
  font-size: 22px;
  font-weight: 700;
  margin: 0 0 8px;
  color: var(--el-text-color-primary);
}

.report-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  color: var(--el-text-color-secondary);
}

.meta-sep { color: var(--el-border-color); }

/* ===== 成绩概览 ===== */
.score-section {
  display: flex;
  align-items: center;
  gap: 32px;
  background: var(--el-bg-color);
  border-radius: 16px;
  padding: 28px 32px;
  margin-bottom: 24px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
  flex-wrap: wrap;
}

.score-chart-wrap {
  flex-shrink: 0;
}

.score-chart {
  width: 160px;
  height: 160px;
}

.score-stats {
  display: flex;
  align-items: center;
  gap: 0;
  flex: 1;
  flex-wrap: wrap;
}

.stat-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  padding: 0 24px;
}

.stat-divider {
  width: 1px;
  height: 40px;
  background: var(--el-border-color-light);
}

.stat-value {
  font-size: 28px;
  font-weight: 700;
  color: var(--el-text-color-primary);
}

.stat-value.primary { color: var(--el-color-primary); }
.stat-value.success { color: var(--el-color-success); }
.stat-value.danger { color: var(--el-color-danger); }

.stat-label {
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.grade-badge {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  padding: 16px 24px;
  border-radius: 12px;
  min-width: 80px;
}

.grade-letter {
  font-size: 40px;
  font-weight: 900;
  line-height: 1;
}

.grade-text {
  font-size: 12px;
  font-weight: 500;
}

.grade-s { background: #fef3c7; color: #d97706; }
.grade-a { background: #dcfce7; color: #16a34a; }
.grade-b { background: #dbeafe; color: #2563eb; }
.grade-c { background: #fef9c3; color: #ca8a04; }
.grade-d { background: #fee2e2; color: #dc2626; }

/* ===== 逐题解析 ===== */
.section-title {
  font-size: 16px;
  font-weight: 600;
  color: var(--el-text-color-primary);
  margin-bottom: 16px;
  padding-bottom: 10px;
  border-bottom: 2px solid var(--el-border-color-light);
}

.question-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.question-block {
  background: var(--el-bg-color);
  border-radius: 10px;
  padding: 20px;
  border-left: 4px solid transparent;
}

.block-correct { border-left-color: var(--el-color-success); }
.block-wrong { border-left-color: var(--el-color-danger); }

.q-header {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 14px;
}

.q-number-badge {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 700;
  font-size: 13px;
  color: #fff;
}

.badge-correct { background: var(--el-color-success); }
.badge-wrong { background: var(--el-color-danger); }

.result-icon {
  font-size: 18px;
}

.result-icon.correct { color: var(--el-color-success); }
.result-icon.wrong { color: var(--el-color-danger); }

.result-text {
  font-size: 13px;
  font-weight: 600;
}

.text-success { color: var(--el-color-success); }
.text-danger { color: var(--el-color-danger); }

.q-content {
  font-size: 15px;
  line-height: 1.7;
  color: var(--el-text-color-primary);
  margin-bottom: 14px;
}

/* 选项 */
.options-wrap {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-bottom: 14px;
}

.option-row {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  border-radius: 6px;
  border: 1px solid var(--el-border-color-light);
  font-size: 14px;
}

.option-row.option-correct {
  background: var(--el-color-success-light-9);
  border-color: var(--el-color-success);
}

.option-row.option-wrong {
  background: var(--el-color-danger-light-9);
  border-color: var(--el-color-danger);
}

.opt-key {
  font-weight: 600;
  color: var(--el-color-primary);
  min-width: 20px;
}

.opt-value { flex: 1; }

.opt-marker {
  display: flex;
  align-items: center;
  gap: 3px;
  font-size: 12px;
  font-weight: 600;
}

.correct-marker { color: var(--el-color-success); }
.wrong-marker { color: var(--el-color-danger); }

/* 答案对比 */
.answer-compare {
  display: flex;
  gap: 24px;
  margin-bottom: 14px;
  flex-wrap: wrap;
}

.answer-item {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 14px;
}

.answer-label {
  color: var(--el-text-color-secondary);
}

.answer-value {
  font-weight: 600;
}

/* 解析 */
.explanation {
  background: var(--el-fill-color-light);
  border-radius: 8px;
  padding: 12px 16px;
}

.explanation-label {
  display: flex;
  align-items: center;
  gap: 6px;
  font-weight: 600;
  font-size: 13px;
  color: var(--el-color-primary);
  margin-bottom: 8px;
}

.explanation-content {
  font-size: 14px;
  line-height: 1.7;
  color: var(--el-text-color-regular);
}
</style>
