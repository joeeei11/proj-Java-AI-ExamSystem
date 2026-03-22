<template>
  <div class="quiz-home">
    <!-- ===== 配置面板（生成题目前） ===== -->
    <div v-if="phase === 'config'" class="config-panel">
      <div class="panel-header">
        <h2 class="panel-title">
          <el-icon class="title-icon"><EditPen /></el-icon>
          智能刷题
        </h2>
        <p class="panel-subtitle">AI 根据你的选择实时生成题目，刷完即时判分，错题自动收录</p>
      </div>

      <el-card class="config-card" shadow="never">
        <el-form :model="configForm" label-position="top" class="config-form">
          <!-- 科目 -->
          <el-form-item label="选择科目" required>
            <el-select
              v-model="configForm.subjectId"
              placeholder="请选择科目"
              class="full-width"
              @change="onSubjectChange"
            >
              <el-option
                v-for="s in subjects"
                :key="s.id"
                :label="s.name"
                :value="s.id"
              />
            </el-select>
          </el-form-item>

          <!-- 难度 -->
          <el-form-item label="题目难度">
            <div class="difficulty-selector">
              <div
                v-for="d in difficultyOptions"
                :key="d.value"
                class="diff-btn"
                :class="{ active: configForm.difficulty === d.value }"
                @click="configForm.difficulty = d.value"
              >
                <span class="diff-label">{{ d.label }}</span>
                <span class="diff-dots">
                  <span v-for="i in 3" :key="i" class="dot" :class="{ active: i <= d.value }" />
                </span>
              </div>
            </div>
          </el-form-item>

          <!-- 题目数量 -->
          <el-form-item label="题目数量">
            <div class="count-selector">
              <span
                v-for="n in countOptions"
                :key="n"
                class="count-btn"
                :class="{ active: configForm.count === n }"
                @click="configForm.count = n"
              >{{ n }} 题</span>
            </div>
          </el-form-item>

          <!-- 知识点（可选） -->
          <el-form-item v-if="knowledgePoints.length > 0" label="指定知识点（可选）">
            <el-select
              v-model="configForm.knowledgePointIds"
              multiple
              clearable
              collapse-tags
              collapse-tags-tooltip
              placeholder="不选则全面覆盖"
              class="full-width"
            >
              <el-option
                v-for="kp in knowledgePoints"
                :key="kp.id"
                :label="kp.name"
                :value="kp.id"
              />
            </el-select>
          </el-form-item>
        </el-form>

        <!-- 生成按钮 -->
        <div class="generate-btn-wrap">
          <el-button
            type="primary"
            size="large"
            :loading="generating"
            :disabled="!configForm.subjectId"
            class="generate-btn"
            @click="handleGenerate"
          >
            <el-icon v-if="!generating"><MagicStick /></el-icon>
            {{ generating ? 'AI 正在出题...' : '开始出题' }}
          </el-button>
          <p class="generate-hint">首次生成约需 10-20 秒，已生成的题目将缓存 24 小时</p>
        </div>
      </el-card>
    </div>

    <!-- ===== 答题面板 ===== -->
    <div v-else-if="phase === 'answering'" class="answering-panel">
      <!-- 顶部进度条 -->
      <div class="quiz-topbar">
        <div class="quiz-info">
          <span class="quiz-subject">{{ currentSubjectName }}</span>
          <el-tag type="info" size="small">{{ difficultyLabel(configForm.difficulty) }}</el-tag>
        </div>
        <div class="quiz-progress">
          <span class="progress-text">{{ answeredCount }}/{{ questions.length }}</span>
          <el-progress
            :percentage="Math.round((answeredCount / questions.length) * 100)"
            :show-text="false"
            :stroke-width="6"
            style="width: 120px;"
          />
        </div>
        <el-button size="small" plain @click="phase = 'config'">重新配置</el-button>
      </div>

      <!-- 题目列表 -->
      <div class="questions-list">
        <QuestionCard
          v-for="(q, idx) in questions"
          :key="q.id"
          :question="q"
          :index="idx"
          :model-value="answers[q.id] || ''"
          :multi-value="answers[q.id] || ''"
          :answered="false"
          @update:model-value="(v) => answers[q.id] = v"
          @update:multi-value="(v) => answers[q.id] = v"
        />
      </div>

      <!-- 提交区 -->
      <div class="submit-bar">
        <div class="unanswered-tip" v-if="unansweredCount > 0">
          <el-icon><Warning /></el-icon>
          还有 <strong>{{ unansweredCount }}</strong> 题未作答
        </div>
        <div class="unanswered-tip all-done" v-else>
          <el-icon><CircleCheckFilled /></el-icon>
          全部作答完毕，可提交了
        </div>
        <el-button
          type="primary"
          size="large"
          :loading="submitting"
          class="submit-btn"
          @click="handleSubmit"
        >
          {{ submitting ? '正在判分...' : '提交答案' }}
        </el-button>
      </div>
    </div>

    <!-- ===== 结果面板 ===== -->
    <div v-else-if="phase === 'result'" class="result-panel">
      <!-- 成绩摘要卡 -->
      <div class="score-summary">
        <div class="score-circle-wrap">
          <el-progress
            type="circle"
            :percentage="Math.round((submitResult!.accuracy) * 100)"
            :width="120"
            :stroke-width="10"
            :color="scoreColor"
          >
            <template #default>
              <span class="score-text">{{ Math.round(submitResult!.accuracy * 100) }}<small>%</small></span>
            </template>
          </el-progress>
        </div>
        <div class="score-detail">
          <h3 class="score-title">{{ scoreTitle }}</h3>
          <div class="score-stats">
            <div class="stat-item">
              <span class="stat-val correct-val">{{ submitResult!.correctCount }}</span>
              <span class="stat-label">答对</span>
            </div>
            <div class="stat-divider" />
            <div class="stat-item">
              <span class="stat-val wrong-val">{{ submitResult!.totalCount - submitResult!.correctCount }}</span>
              <span class="stat-label">答错</span>
            </div>
            <div class="stat-divider" />
            <div class="stat-item">
              <span class="stat-val">{{ submitResult!.totalCount }}</span>
              <span class="stat-label">总题数</span>
            </div>
          </div>
          <p v-if="submitResult!.correctCount < submitResult!.totalCount" class="error-hint">
            错题已自动收录至错题本
          </p>
        </div>
        <div class="score-actions">
          <el-button type="primary" @click="resetToConfig">再刷一组</el-button>
          <el-button plain @click="$router.push('/errorbook')">查看错题本</el-button>
        </div>
      </div>

      <!-- 纠错反馈入口 -->
      <div class="feedback-hint">
        <el-icon><WarningFilled /></el-icon>
        发现题目有误？
        <el-button text type="primary" size="small" @click="feedbackDialogVisible = true">
          点此反馈
        </el-button>
      </div>

      <!-- 逐题解析 -->
      <div class="results-title">
        <el-icon><List /></el-icon> 逐题解析
      </div>
      <div class="questions-list">
        <QuestionCard
          v-for="(q, idx) in questions"
          :key="q.id"
          :question="q"
          :index="idx"
          :model-value="answers[q.id] || ''"
          :multi-value="answers[q.id] || ''"
          :answered="true"
          :result="getResult(q.id)"
        />
      </div>
    </div>

    <!-- ===== 纳错反馈对话框 ===== -->
    <el-dialog
      v-model="feedbackDialogVisible"
      title="提交题目纠错反馈"
      width="480px"
      :close-on-click-modal="false"
    >
      <el-form
        ref="feedbackFormRef"
        :model="feedbackForm"
        :rules="feedbackRules"
        label-position="top"
      >
        <el-form-item label="选择题目" prop="questionId">
          <el-select
            v-model="feedbackForm.questionId"
            placeholder="选择要反馈的题目"
            style="width: 100%"
          >
            <el-option
              v-for="q in questions"
              :key="q.id"
              :label="`第 ${questions.indexOf(q) + 1} 题：${q.content.slice(0, 30)}…`"
              :value="q.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="反馈类型" prop="type">
          <el-radio-group v-model="feedbackForm.type" style="flex-wrap: wrap; gap: 8px">
            <el-radio-button :value="1">答案错误</el-radio-button>
            <el-radio-button :value="2">题干歧义</el-radio-button>
            <el-radio-button :value="3">解析不清</el-radio-button>
            <el-radio-button :value="4">排版问题</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="详细描述（可选）">
          <el-input
            v-model="feedbackForm.description"
            type="textarea"
            :rows="3"
            placeholder="请描述具体问题..."
            maxlength="500"
            show-word-limit
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="feedbackDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="feedbackSubmitting" @click="submitFeedbackForm">
          提交反馈
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { WarningFilled } from '@element-plus/icons-vue'
import QuestionCard from '@/components/QuestionCard.vue'
import { getSubjects, getKnowledgePoints, type Subject, type KnowledgePoint } from '@/api/subject'
import { generateQuestions, submitAnswers, type Question, type SubmitResult, type QuestionResult } from '@/api/quiz'
import { submitFeedback, FEEDBACK_TYPES } from '@/api/feedback'

// ===== 状态 =====
type Phase = 'config' | 'answering' | 'result'
const phase = ref<Phase>('config')

const configForm = ref({
  subjectId: null as number | null,
  difficulty: 2,
  count: 5,
  knowledgePointIds: [] as number[],
})

const subjects = ref<Subject[]>([])
const knowledgePoints = ref<KnowledgePoint[]>([])
const questions = ref<Question[]>([])
const answers = ref<Record<number, string>>({})
const generating = ref(false)
const submitting = ref(false)
const submitResult = ref<SubmitResult | null>(null)
const sessionId = ref('')

// ===== 配置选项 =====
const difficultyOptions = [
  { value: 1, label: '简单' },
  { value: 2, label: '中等' },
  { value: 3, label: '困难' },
]
const countOptions = [5, 10, 15, 20]

// ===== 计算属性 =====
const currentSubjectName = computed(() =>
  subjects.value.find((s) => s.id === configForm.value.subjectId)?.name || ''
)

const answeredCount = computed(() =>
  questions.value.filter((q) => answers.value[q.id]).length
)

const unansweredCount = computed(() => questions.value.length - answeredCount.value)

const scoreColor = computed(() => {
  const pct = (submitResult.value?.accuracy ?? 0) * 100
  if (pct >= 80) return '#67c23a'
  if (pct >= 60) return '#e6a23c'
  return '#f56c6c'
})

const scoreTitle = computed(() => {
  const pct = (submitResult.value?.accuracy ?? 0) * 100
  if (pct >= 90) return '优秀！继续保持'
  if (pct >= 70) return '良好，还有提升空间'
  if (pct >= 60) return '及格，要加强练习'
  return '需要重点复习'
})

/** 生成简单的 UUID v4 */
function uuidv4(): string {
  return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, (c) => {
    const r = (Math.random() * 16) | 0
    const v = c === 'x' ? r : (r & 0x3) | 0x8
    return v.toString(16)
  })
}

// ===== 生命周期 =====
onMounted(async () => {
  try {
    const res = await getSubjects()
    subjects.value = res.data
  } catch {
    ElMessage.error('加载科目失败')
  }
})

// ===== 方法 =====

async function onSubjectChange(id: number) {
  configForm.value.knowledgePointIds = []
  knowledgePoints.value = []
  if (!id) return
  try {
    const res = await getKnowledgePoints(id)
    knowledgePoints.value = res.data
  } catch { /* 知识点加载失败不阻断流程 */ }
}

async function handleGenerate() {
  if (!configForm.value.subjectId) {
    ElMessage.warning('请先选择科目')
    return
  }

  generating.value = true
  try {
    const res = await generateQuestions({
      subjectId: configForm.value.subjectId,
      difficulty: configForm.value.difficulty,
      count: configForm.value.count,
      knowledgePointIds: configForm.value.knowledgePointIds.length
        ? configForm.value.knowledgePointIds
        : undefined,
    })

    questions.value = res.data
    answers.value = {}
    sessionId.value = uuidv4()
    phase.value = 'answering'

  } catch (e: any) {
    ElMessage.error(e.message || 'AI 出题失败，请稍后重试')
  } finally {
    generating.value = false
  }
}

async function handleSubmit() {
  if (unansweredCount.value > 0) {
    try {
      await ElMessageBox.confirm(
        `还有 ${unansweredCount.value} 题未作答，确定提交？`,
        '提交确认',
        { confirmButtonText: '确认提交', cancelButtonText: '继续答题', type: 'warning' }
      )
    } catch {
      return
    }
  }

  submitting.value = true
  try {
    const answerList = questions.value.map((q) => ({
      questionId: q.id,
      answer: answers.value[q.id] || '',
    }))

    const res = await submitAnswers({
      sessionId: sessionId.value,
      answers: answerList,
    })

    submitResult.value = res.data
    phase.value = 'result'
    // 滚动到顶部
    window.scrollTo({ top: 0, behavior: 'smooth' })

  } catch (e: any) {
    ElMessage.error(e.message || '提交失败，请重试')
  } finally {
    submitting.value = false
  }
}

function getResult(questionId: number): QuestionResult | undefined {
  return submitResult.value?.results.find((r) => r.questionId === questionId)
}

function difficultyLabel(d: number) {
  return ['', '简单', '中等', '困难'][d] || '中等'
}

function resetToConfig() {
  phase.value = 'config'
  submitResult.value = null
  questions.value = []
  answers.value = {}
}

// ===== 纠错反馈 =====
const feedbackDialogVisible = ref(false)
const feedbackSubmitting = ref(false)
const feedbackFormRef = ref<FormInstance>()
const feedbackForm = ref({
  questionId: null as number | null,
  type: null as number | null,
  description: '',
})

const feedbackRules: FormRules = {
  questionId: [{ required: true, message: '请选择要反馈的题目', trigger: 'change' }],
  type: [{ required: true, message: '请选择反馈类型', trigger: 'change' }],
}

async function submitFeedbackForm() {
  if (!feedbackFormRef.value) return
  const valid = await feedbackFormRef.value.validate().catch(() => false)
  if (!valid) return

  feedbackSubmitting.value = true
  try {
    await submitFeedback({
      questionId: feedbackForm.value.questionId!,
      type: feedbackForm.value.type!,
      description: feedbackForm.value.description || undefined,
    })
    ElMessage.success('反馈已提交，感谢您的贡献！')
    feedbackDialogVisible.value = false
    feedbackForm.value = { questionId: null, type: null, description: '' }
  } catch {
    // 已由拦截器处理
  } finally {
    feedbackSubmitting.value = false
  }
}
</script>

<style scoped>
.quiz-home {
  max-width: 800px;
  margin: 0 auto;
}

/* ===== 配置面板 ===== */
.panel-header {
  margin-bottom: 20px;
}

.panel-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 22px;
  font-weight: 700;
  color: #1d2b3a;
  margin: 0 0 8px;
}

.title-icon {
  color: #409eff;
  font-size: 22px;
}

.panel-subtitle {
  font-size: 14px;
  color: #909399;
  margin: 0;
}

.config-card {
  border-radius: 12px;
  border: 1.5px solid #ebeef5;
}

.config-form {
  margin-bottom: 8px;
}

.full-width { width: 100%; }

/* 难度选择器 */
.difficulty-selector {
  display: flex;
  gap: 10px;
}

.diff-btn {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  padding: 12px 8px;
  border: 1.5px solid #e4e7ed;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.15s;
}

.diff-btn:hover,
.diff-btn.active {
  border-color: #409eff;
  background: #ecf5ff;
}

.diff-label {
  font-size: 13px;
  font-weight: 600;
  color: #303133;
}

.diff-dots {
  display: flex;
  gap: 4px;
}

.dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #dcdfe6;
}

.dot.active {
  background: #e6a23c;
}

/* 数量选择器 */
.count-selector {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.count-btn {
  padding: 8px 20px;
  border: 1.5px solid #e4e7ed;
  border-radius: 20px;
  cursor: pointer;
  font-size: 14px;
  color: #606266;
  transition: all 0.15s;
}

.count-btn:hover,
.count-btn.active {
  border-color: #409eff;
  color: #409eff;
  background: #ecf5ff;
}

/* 生成按钮 */
.generate-btn-wrap {
  text-align: center;
  padding-top: 8px;
}

.generate-btn {
  width: 200px;
  height: 46px;
  font-size: 15px;
  border-radius: 23px;
}

.generate-hint {
  margin: 10px 0 0;
  font-size: 12px;
  color: #c0c4cc;
}

/* ===== 答题面板 ===== */
.quiz-topbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #fff;
  border: 1.5px solid #ebeef5;
  border-radius: 10px;
  padding: 12px 20px;
  margin-bottom: 16px;
}

.quiz-info {
  display: flex;
  align-items: center;
  gap: 8px;
}

.quiz-subject {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
}

.quiz-progress {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 13px;
  color: #606266;
}

.questions-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
  margin-bottom: 80px;
}

/* 提交栏 */
.submit-bar {
  position: sticky;
  bottom: 20px;
  background: #fff;
  border: 1.5px solid #ebeef5;
  border-radius: 12px;
  padding: 14px 20px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  box-shadow: 0 -4px 16px rgba(0, 0, 0, 0.08);
}

.unanswered-tip {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 14px;
  color: #e6a23c;
}

.unanswered-tip.all-done {
  color: #67c23a;
}

.unanswered-tip strong {
  font-weight: 700;
}

.submit-btn {
  width: 140px;
  border-radius: 20px;
}

/* ===== 结果面板 ===== */
.score-summary {
  background: #fff;
  border: 1.5px solid #ebeef5;
  border-radius: 12px;
  padding: 28px 24px;
  display: flex;
  align-items: center;
  gap: 32px;
  margin-bottom: 20px;
  flex-wrap: wrap;
}

.score-circle-wrap {
  flex-shrink: 0;
}

.score-text {
  font-size: 26px;
  font-weight: 700;
  color: #303133;
}

.score-text small {
  font-size: 14px;
}

.score-detail {
  flex: 1;
  min-width: 200px;
}

.score-title {
  font-size: 18px;
  font-weight: 700;
  color: #303133;
  margin: 0 0 14px;
}

.score-stats {
  display: flex;
  align-items: center;
  gap: 20px;
  margin-bottom: 10px;
}

.stat-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2px;
}

.stat-val {
  font-size: 24px;
  font-weight: 700;
  color: #303133;
}

.correct-val { color: #67c23a; }
.wrong-val   { color: #f56c6c; }

.stat-label {
  font-size: 12px;
  color: #909399;
}

.stat-divider {
  width: 1px;
  height: 32px;
  background: #e4e7ed;
}

.error-hint {
  font-size: 12px;
  color: #909399;
  margin: 0;
}

.score-actions {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  margin-left: auto;
}

.results-title {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 16px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 14px;
}

/* 纠错反馈提示条 */
.feedback-hint {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: var(--el-text-color-secondary);
  background: var(--el-fill-color-light);
  border-radius: 8px;
  padding: 10px 16px;
  margin-bottom: 16px;
}
</style>
