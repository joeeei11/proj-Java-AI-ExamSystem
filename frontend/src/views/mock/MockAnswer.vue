<template>
  <div class="mock-answer" v-loading="loading">
    <!-- ===== 顶部状态栏 ===== -->
    <div class="top-bar" v-if="exam">
      <div class="exam-title">{{ exam.title }}</div>
      <div class="countdown" :class="countdownClass">
        <el-icon><Timer /></el-icon>
        <span class="time-text">{{ formatCountdown(remainingSeconds) }}</span>
      </div>
      <el-button
        type="primary"
        size="small"
        :loading="submitting"
        @click="handleSubmit"
        class="submit-btn"
      >
        交卷
      </el-button>
    </div>

    <div class="answer-body" v-if="exam">
      <!-- ===== 左侧题目区 ===== -->
      <div class="question-area" ref="questionAreaRef">
        <div
          v-for="(q, index) in exam.questions"
          :key="q.questionId"
          :id="`q-${q.questionId}`"
          class="question-block"
          :class="{ flagged: q.flagged }"
        >
          <!-- 题目头 -->
          <div class="q-header">
            <div class="q-number">
              <span class="num">{{ q.sortOrder }}</span>
              <el-tag v-if="q.flagged" type="warning" size="small" class="flag-tag">
                <el-icon><Flag /></el-icon>
                已标记
              </el-tag>
            </div>
            <div class="q-meta">
              <el-tag size="small" :type="typeTagType(q.type)">{{ typeLabel(q.type) }}</el-tag>
              <el-button
                text
                size="small"
                :type="q.flagged ? 'warning' : 'default'"
                @click="toggleFlag(q, index)"
              >
                <el-icon><Flag /></el-icon>
                {{ q.flagged ? '取消标记' : '标记' }}
              </el-button>
            </div>
          </div>

          <!-- 题干 -->
          <div class="q-content">{{ q.content }}</div>

          <!-- 选项（单选 / 判断） -->
          <el-radio-group
            v-if="q.type === 1 || q.type === 3"
            v-model="answers[q.questionId]"
            class="option-group"
          >
            <el-radio
              v-for="opt in q.options"
              :key="opt.key"
              :value="opt.key"
              class="option-item"
            >
              <span class="opt-key">{{ opt.key }}.</span>
              <span class="opt-value">{{ opt.value }}</span>
            </el-radio>
            <!-- 判断题无选项时兜底 -->
            <template v-if="q.type === 3 && q.options.length === 0">
              <el-radio value="正确" class="option-item">正确</el-radio>
              <el-radio value="错误" class="option-item">错误</el-radio>
            </template>
          </el-radio-group>

          <!-- 多选 -->
          <el-checkbox-group
            v-if="q.type === 2"
            v-model="multiAnswers[q.questionId]"
            class="option-group"
            @change="syncMultiAnswer(q.questionId)"
          >
            <el-checkbox
              v-for="opt in q.options"
              :key="opt.key"
              :label="opt.key"
              class="option-item"
            >
              <span class="opt-key">{{ opt.key }}.</span>
              <span class="opt-value">{{ opt.value }}</span>
            </el-checkbox>
          </el-checkbox-group>

          <!-- 填空 -->
          <el-input
            v-if="q.type === 4"
            v-model="answers[q.questionId]"
            placeholder="请输入答案"
            class="fill-input"
          />
        </div>
      </div>

      <!-- ===== 右侧导航面板 ===== -->
      <div class="nav-panel">
        <div class="nav-header">答题进度</div>
        <div class="progress-info">
          <span class="answered-count">
            {{ answeredCount }}/{{ exam.totalQuestions }}
          </span>
          <span class="progress-label">已作答</span>
        </div>
        <el-progress
          :percentage="Math.round((answeredCount / exam.totalQuestions) * 100)"
          :stroke-width="6"
          class="progress-bar"
        />

        <div class="q-grid">
          <div
            v-for="q in exam.questions"
            :key="q.questionId"
            class="q-dot"
            :class="{
              answered: !!answers[q.questionId],
              flagged: q.flagged,
              current: currentVisible === q.questionId,
            }"
            :title="`第${q.sortOrder}题`"
            @click="scrollToQuestion(q.questionId)"
          >
            {{ q.sortOrder }}
          </div>
        </div>

        <div class="nav-legend">
          <span class="legend-item"><span class="dot answered-dot"></span> 已答</span>
          <span class="legend-item"><span class="dot flagged-dot"></span> 标记</span>
          <span class="legend-item"><span class="dot empty-dot"></span> 未答</span>
        </div>

        <el-button
          type="primary"
          class="nav-submit-btn"
          :loading="submitting"
          @click="handleSubmit"
        >
          <el-icon><Check /></el-icon>
          交卷
        </el-button>
      </div>
    </div>

    <!-- 超时提示 -->
    <el-dialog v-model="timeoutDialogVisible" title="考试已超时" :close-on-click-modal="false" width="360px">
      <p>考试时间已到，系统将自动提交你的答卷。</p>
      <template #footer>
        <el-button type="primary" @click="forceSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, onUnmounted, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Timer, Flag, Check } from '@element-plus/icons-vue'
import { getMockExam, flagQuestion, submitMock, type MockExamVO } from '@/api/mock'

const route = useRoute()
const router = useRouter()
const examId = Number(route.params.examId)

const loading = ref(true)
const submitting = ref(false)
const exam = ref<MockExamVO | null>(null)
const remainingSeconds = ref(0)
const answers = reactive<Record<number, string>>({})
const multiAnswers = reactive<Record<number, string[]>>({})
const currentVisible = ref<number | null>(null)
const timeoutDialogVisible = ref(false)
const questionAreaRef = ref<HTMLElement | null>(null)

let countdownTimer: ReturnType<typeof setInterval> | null = null
let syncTimer: ReturnType<typeof setInterval> | null = null

// ===== 计算属性 =====

const answeredCount = computed(() => {
  if (!exam.value) return 0
  return exam.value.questions.filter(q => !!answers[q.questionId]).length
})

const countdownClass = computed(() => {
  if (remainingSeconds.value <= 60) return 'countdown-danger'
  if (remainingSeconds.value <= 300) return 'countdown-warning'
  return ''
})

// ===== 初始化 =====

onMounted(async () => {
  await loadExam()
  startCountdown()
  startSyncTimer()
  setupScrollObserver()
})

onUnmounted(() => {
  if (countdownTimer) clearInterval(countdownTimer)
  if (syncTimer) clearInterval(syncTimer)
})

async function loadExam() {
  try {
    const res = await getMockExam(examId)
    exam.value = res.data

    // 超时或已完成则跳到报告
    if (res.data.status !== 0) {
      router.replace(`/mock/report/${examId}`)
      return
    }

    remainingSeconds.value = res.data.remainingSeconds

    // 恢复已有作答
    for (const q of res.data.questions) {
      if (q.userAnswer) {
        if (q.type === 2) {
          // 多选：答案格式 "A,B,C"
          multiAnswers[q.questionId] = q.userAnswer.split(',').filter(Boolean)
          answers[q.questionId] = q.userAnswer
        } else {
          answers[q.questionId] = q.userAnswer
        }
      }
    }
  } finally {
    loading.value = false
  }
}

// ===== 倒计时 =====

function startCountdown() {
  countdownTimer = setInterval(() => {
    if (remainingSeconds.value > 0) {
      remainingSeconds.value--
    } else {
      clearInterval(countdownTimer!)
      timeoutDialogVisible.value = true
    }
  }, 1000)
}

// 每 30 秒同步一次服务端剩余时间
function startSyncTimer() {
  syncTimer = setInterval(async () => {
    try {
      const res = await getMockExam(examId)
      if (res.data.status !== 0) {
        clearInterval(countdownTimer!)
        clearInterval(syncTimer!)
        router.replace(`/mock/report/${examId}`)
        return
      }
      // 以服务端为准（误差 > 5s 才更新，减少跳动）
      if (Math.abs(res.data.remainingSeconds - remainingSeconds.value) > 5) {
        remainingSeconds.value = res.data.remainingSeconds
      }
    } catch {
      /* 网络错误不影响倒计时 */
    }
  }, 30000)
}

// ===== 标记 =====

async function toggleFlag(q: MockExamVO['questions'][0], index: number) {
  const newFlagged = !q.flagged
  try {
    await flagQuestion(examId, q.questionId, newFlagged)
    exam.value!.questions[index].flagged = newFlagged
  } catch {
    /* 静默 */
  }
}

// ===== 多选同步 =====

function syncMultiAnswer(questionId: number) {
  const selected = multiAnswers[questionId] || []
  answers[questionId] = selected.sort().join(',')
}

// ===== 滚动导航 =====

function scrollToQuestion(questionId: number) {
  const el = document.getElementById(`q-${questionId}`)
  if (el) {
    el.scrollIntoView({ behavior: 'smooth', block: 'start' })
  }
}

function setupScrollObserver() {
  nextTick(() => {
    if (!questionAreaRef.value) return
    const observer = new IntersectionObserver(
      (entries) => {
        const visible = entries.filter(e => e.isIntersecting)
        if (visible.length > 0) {
          const id = Number(visible[0].target.id.replace('q-', ''))
          currentVisible.value = id
        }
      },
      { threshold: 0.4 },
    )
    document.querySelectorAll('.question-block').forEach(el => observer.observe(el))
  })
}

// ===== 交卷 =====

async function handleSubmit() {
  const unanswered = exam.value!.questions.filter(q => !answers[q.questionId]).length
  if (unanswered > 0) {
    try {
      await ElMessageBox.confirm(
        `还有 ${unanswered} 道题未作答，确认交卷？`,
        '确认交卷',
        { confirmButtonText: '确认交卷', cancelButtonText: '继续作答', type: 'warning' },
      )
    } catch {
      return
    }
  }
  await doSubmit()
}

async function forceSubmit() {
  timeoutDialogVisible.value = false
  await doSubmit()
}

async function doSubmit() {
  submitting.value = true
  if (countdownTimer) clearInterval(countdownTimer)
  if (syncTimer) clearInterval(syncTimer)

  try {
    const answerList = exam.value!.questions.map(q => ({
      questionId: q.questionId,
      answer: answers[q.questionId] || null,
    }))
    await submitMock(examId, answerList)
    ElMessage.success('交卷成功')
    router.replace(`/mock/report/${examId}`)
  } finally {
    submitting.value = false
  }
}

// ===== 工具方法 =====

function formatCountdown(seconds: number) {
  const h = Math.floor(seconds / 3600)
  const m = Math.floor((seconds % 3600) / 60)
  const s = seconds % 60
  if (h > 0) return `${h}:${pad(m)}:${pad(s)}`
  return `${pad(m)}:${pad(s)}`
}

function pad(n: number) {
  return String(n).padStart(2, '0')
}

function typeLabel(type: number) {
  const map: Record<number, string> = { 1: '单选', 2: '多选', 3: '判断', 4: '填空' }
  return map[type] || '单选'
}

function typeTagType(type: number): '' | 'success' | 'warning' | 'info' {
  const map: Record<number, '' | 'success' | 'warning' | 'info'> = {
    1: '',
    2: 'success',
    3: 'warning',
    4: 'info',
  }
  return map[type] || ''
}
</script>

<style scoped>
.mock-answer {
  display: flex;
  flex-direction: column;
  height: 100%;
  background: var(--el-fill-color-light);
}

/* ===== 顶部状态栏 ===== */
.top-bar {
  position: sticky;
  top: 0;
  z-index: 100;
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 12px 24px;
  background: var(--el-bg-color);
  border-bottom: 1px solid var(--el-border-color-light);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.exam-title {
  font-weight: 700;
  font-size: 16px;
  color: var(--el-text-color-primary);
  flex: 1;
}

.countdown {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 20px;
  font-weight: 700;
  font-variant-numeric: tabular-nums;
  color: var(--el-color-primary);
  background: var(--el-color-primary-light-9);
  padding: 6px 16px;
  border-radius: 8px;
}

.countdown.countdown-warning {
  color: var(--el-color-warning);
  background: var(--el-color-warning-light-9);
}

.countdown.countdown-danger {
  color: var(--el-color-danger);
  background: var(--el-color-danger-light-9);
  animation: blink 1s infinite;
}

@keyframes blink {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.6; }
}

.submit-btn {
  padding: 8px 20px;
}

/* ===== 答题区布局 ===== */
.answer-body {
  flex: 1;
  display: flex;
  gap: 20px;
  padding: 20px 24px;
  align-items: flex-start;
  overflow: hidden;
}

/* ===== 左侧题目区 ===== */
.question-area {
  flex: 1;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 16px;
  max-height: calc(100vh - 80px);
  padding-right: 8px;
}

.question-block {
  background: var(--el-bg-color);
  border-radius: 10px;
  padding: 20px;
  border: 2px solid transparent;
  transition: border-color 0.2s;
}

.question-block.flagged {
  border-color: var(--el-color-warning);
}

.q-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

.q-number {
  display: flex;
  align-items: center;
  gap: 8px;
}

.num {
  width: 28px;
  height: 28px;
  background: var(--el-color-primary);
  color: #fff;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 700;
  font-size: 13px;
}

.q-meta {
  display: flex;
  align-items: center;
  gap: 8px;
}

.q-content {
  font-size: 15px;
  line-height: 1.7;
  color: var(--el-text-color-primary);
  margin-bottom: 16px;
}

.option-group {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.option-item {
  display: flex;
  align-items: flex-start;
  height: auto;
  padding: 8px 12px;
  border: 1px solid var(--el-border-color-light);
  border-radius: 6px;
  transition: all 0.2s;
  width: 100%;
}

.option-item:hover {
  background: var(--el-fill-color-light);
}

.opt-key {
  font-weight: 600;
  margin-right: 6px;
  color: var(--el-color-primary);
}

.fill-input {
  width: 100%;
  max-width: 400px;
}

/* ===== 右侧导航面板 ===== */
.nav-panel {
  width: 220px;
  flex-shrink: 0;
  background: var(--el-bg-color);
  border-radius: 10px;
  padding: 16px;
  position: sticky;
  top: 72px;
  max-height: calc(100vh - 100px);
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.nav-header {
  font-weight: 600;
  font-size: 14px;
  color: var(--el-text-color-primary);
}

.progress-info {
  display: flex;
  align-items: baseline;
  gap: 6px;
}

.answered-count {
  font-size: 22px;
  font-weight: 700;
  color: var(--el-color-primary);
}

.progress-label {
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.progress-bar {
  margin: -4px 0;
}

.q-grid {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 6px;
}

.q-dot {
  width: 32px;
  height: 32px;
  border-radius: 6px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 11px;
  font-weight: 600;
  cursor: pointer;
  border: 1.5px solid var(--el-border-color);
  color: var(--el-text-color-secondary);
  transition: all 0.15s;
  background: var(--el-fill-color-blank);
}

.q-dot:hover {
  border-color: var(--el-color-primary);
  color: var(--el-color-primary);
}

.q-dot.answered {
  background: var(--el-color-primary);
  border-color: var(--el-color-primary);
  color: #fff;
}

.q-dot.flagged {
  background: var(--el-color-warning-light-7);
  border-color: var(--el-color-warning);
  color: var(--el-color-warning-dark-2);
}

.q-dot.current {
  box-shadow: 0 0 0 2px var(--el-color-primary-light-5);
}

.nav-legend {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.legend-item {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 11px;
  color: var(--el-text-color-secondary);
}

.dot {
  width: 10px;
  height: 10px;
  border-radius: 3px;
  display: inline-block;
}

.answered-dot { background: var(--el-color-primary); }
.flagged-dot { background: var(--el-color-warning-light-7); border: 1px solid var(--el-color-warning); }
.empty-dot { background: var(--el-fill-color-blank); border: 1px solid var(--el-border-color); }

.nav-submit-btn {
  width: 100%;
  margin-top: auto;
}
</style>
