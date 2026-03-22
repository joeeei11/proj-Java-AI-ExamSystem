<template>
  <div class="question-card" :class="[`type-${question.type}`, { answered, correct: answered && isCorrect, wrong: answered && !isCorrect }]">
    <!-- 题目头部：题号 + 题型标签 + 难度 -->
    <div class="card-header">
      <div class="question-meta">
        <span class="question-index">{{ index + 1 }}</span>
        <el-tag :type="typeTagType" size="small" effect="plain">{{ typeName }}</el-tag>
        <div class="difficulty-dots">
          <span
            v-for="d in 3"
            :key="d"
            class="dot"
            :class="{ active: d <= question.difficulty }"
          />
        </div>
      </div>
      <!-- 答题结果标识（提交后展示） -->
      <div v-if="answered" class="result-badge" :class="isCorrect ? 'correct' : 'wrong'">
        <el-icon v-if="isCorrect"><CircleCheckFilled /></el-icon>
        <el-icon v-else><CircleCloseFilled /></el-icon>
        <span>{{ isCorrect ? '正确' : '错误' }}</span>
      </div>
    </div>

    <!-- 题干 -->
    <div class="question-content">{{ question.content }}</div>

    <!-- 选项区（单选/多选/判断） -->
    <div v-if="question.type !== 4" class="options-area">
      <!-- 单选/判断 -->
      <template v-if="question.type === 1 || question.type === 3">
        <div
          v-for="opt in displayOptions"
          :key="opt.key"
          class="option-item"
          :class="getOptionClass(opt.key)"
          @click="!answered && selectSingle(opt.key)"
        >
          <span class="option-key">{{ opt.key }}</span>
          <span class="option-value">{{ opt.value }}</span>
          <!-- 提交后标记正确/错误 -->
          <el-icon v-if="answered && opt.key === correctAnswer" class="opt-icon correct"><CircleCheckFilled /></el-icon>
          <el-icon v-else-if="answered && opt.key === modelValue && opt.key !== correctAnswer" class="opt-icon wrong"><CircleCloseFilled /></el-icon>
        </div>
      </template>

      <!-- 多选 -->
      <template v-else-if="question.type === 2">
        <div
          v-for="opt in displayOptions"
          :key="opt.key"
          class="option-item multi"
          :class="getOptionClassMulti(opt.key)"
          @click="!answered && toggleMulti(opt.key)"
        >
          <el-checkbox
            :model-value="selectedMulti.includes(opt.key)"
            :disabled="answered"
            @change="() => !answered && toggleMulti(opt.key)"
          />
          <span class="option-key">{{ opt.key }}</span>
          <span class="option-value">{{ opt.value }}</span>
        </div>
      </template>
    </div>

    <!-- 填空题输入 -->
    <div v-else class="fill-area">
      <el-input
        v-model="fillAnswer"
        :disabled="answered"
        placeholder="请输入答案"
        @input="$emit('update:modelValue', fillAnswer)"
      />
    </div>

    <!-- 解析区（提交后展示） -->
    <transition name="slide-down">
      <div v-if="answered && result?.explanation" class="explanation">
        <div class="explanation-header">
          <el-icon><InfoFilled /></el-icon>
          <span>题目解析</span>
        </div>
        <div class="explanation-body">
          <div class="correct-answer-tip">
            正确答案：<strong>{{ correctAnswer }}</strong>
            <span v-if="!isCorrect" class="your-answer">　你的作答：<em>{{ modelValue || '未作答' }}</em></span>
          </div>
          <p class="explanation-text">{{ result.explanation }}</p>
        </div>
      </div>
    </transition>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import type { Question, QuestionResult } from '@/api/quiz'

interface Props {
  question: Question
  index: number
  modelValue?: string          // 单选/判断/填空的当前答案
  multiValue?: string          // 多选答案（如 "AC"）
  answered?: boolean           // 是否已提交
  result?: QuestionResult      // 提交后的结果（含解析）
}

const props = withDefaults(defineProps<Props>(), {
  modelValue: '',
  multiValue: '',
  answered: false,
})

const emit = defineEmits<{
  'update:modelValue': [value: string]
  'update:multiValue': [value: string]
}>()

// ===== 本地状态 =====
const fillAnswer = ref(props.modelValue)
const selectedMulti = ref<string[]>(props.multiValue ? props.multiValue.split('') : [])

watch(() => props.modelValue, (v) => { fillAnswer.value = v })
watch(() => props.multiValue, (v) => {
  selectedMulti.value = v ? v.split('') : []
})

// ===== 计算属性 =====

const typeName = computed(() => {
  const map: Record<number, string> = { 1: '单选', 2: '多选', 3: '判断', 4: '填空' }
  return map[props.question.type] || '单选'
})

const typeTagType = computed(() => {
  const map: Record<number, string> = { 1: 'primary', 2: 'success', 3: 'warning', 4: 'info' }
  return map[props.question.type] as any || 'primary'
})

/** 判断题自动生成选项 */
const displayOptions = computed(() => {
  if (props.question.type === 3) {
    return [
      { key: '正确', value: '正确' },
      { key: '错误', value: '错误' },
    ]
  }
  return props.question.options || []
})

const correctAnswer = computed(() => props.result?.correctAnswer || '')

const isCorrect = computed(() => props.result?.correct ?? false)

// ===== 交互方法 =====

function selectSingle(key: string) {
  emit('update:modelValue', key)
}

function toggleMulti(key: string) {
  const idx = selectedMulti.value.indexOf(key)
  if (idx >= 0) {
    selectedMulti.value.splice(idx, 1)
  } else {
    selectedMulti.value.push(key)
  }
  // 排序后拼接（如 A、C 选 → "AC"）
  const sorted = [...selectedMulti.value].sort()
  emit('update:multiValue', sorted.join(''))
}

/** 单选选项样式 */
function getOptionClass(key: string) {
  if (!props.answered) {
    return { selected: props.modelValue === key }
  }
  if (key === correctAnswer.value) return 'opt-correct'
  if (key === props.modelValue) return 'opt-wrong'
  return ''
}

/** 多选选项样式 */
function getOptionClassMulti(key: string) {
  if (!props.answered) {
    return { selected: selectedMulti.value.includes(key) }
  }
  const isInCorrect = correctAnswer.value.split('').includes(key)
  const isSelected = selectedMulti.value.includes(key)
  if (isInCorrect) return 'opt-correct'
  if (isSelected && !isInCorrect) return 'opt-wrong'
  return ''
}
</script>

<style scoped>
.question-card {
  background: #fff;
  border: 1.5px solid #ebeef5;
  border-radius: 12px;
  padding: 20px 22px;
  transition: border-color 0.2s, box-shadow 0.2s;
}

.question-card.correct {
  border-color: #67c23a;
  background: #f0f9eb;
}

.question-card.wrong {
  border-color: #f56c6c;
  background: #fef0f0;
}

/* ===== 头部 ===== */
.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 14px;
}

.question-meta {
  display: flex;
  align-items: center;
  gap: 8px;
}

.question-index {
  width: 26px;
  height: 26px;
  background: #409eff;
  color: #fff;
  border-radius: 50%;
  font-size: 12px;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.difficulty-dots {
  display: flex;
  gap: 3px;
  align-items: center;
}

.dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #dcdfe6;
  transition: background 0.2s;
}

.dot.active {
  background: #e6a23c;
}

.result-badge {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 13px;
  font-weight: 600;
  padding: 3px 10px;
  border-radius: 20px;
}

.result-badge.correct {
  color: #67c23a;
  background: #e1f3d8;
}

.result-badge.wrong {
  color: #f56c6c;
  background: #fde2e2;
}

/* ===== 题干 ===== */
.question-content {
  font-size: 15px;
  line-height: 1.7;
  color: #303133;
  margin-bottom: 16px;
  white-space: pre-wrap;
  word-break: break-word;
}

/* ===== 选项 ===== */
.options-area {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.option-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 14px;
  border: 1.5px solid #e4e7ed;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.15s;
  position: relative;
}

.option-item:hover:not([class*='opt-']) {
  border-color: #409eff;
  background: #ecf5ff;
}

.option-item.selected {
  border-color: #409eff;
  background: #ecf5ff;
}

.option-item.opt-correct {
  border-color: #67c23a;
  background: #f0f9eb;
  cursor: default;
}

.option-item.opt-wrong {
  border-color: #f56c6c;
  background: #fef0f0;
  cursor: default;
}

.option-key {
  width: 22px;
  height: 22px;
  border: 1.5px solid currentColor;
  border-radius: 4px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: 700;
  flex-shrink: 0;
  color: #606266;
}

.option-item.selected .option-key {
  border-color: #409eff;
  background: #409eff;
  color: #fff;
}

.option-value {
  flex: 1;
  font-size: 14px;
  color: #303133;
  line-height: 1.5;
}

.opt-icon {
  font-size: 16px;
  margin-left: auto;
}

.opt-icon.correct { color: #67c23a; }
.opt-icon.wrong   { color: #f56c6c; }

/* 多选复选框对齐 */
.option-item.multi .el-checkbox {
  margin-right: 0;
}

/* ===== 填空 ===== */
.fill-area {
  margin-top: 4px;
}

/* ===== 解析 ===== */
.explanation {
  margin-top: 16px;
  border-top: 1px dashed #e4e7ed;
  padding-top: 14px;
}

.explanation-header {
  display: flex;
  align-items: center;
  gap: 6px;
  color: #409eff;
  font-size: 13px;
  font-weight: 600;
  margin-bottom: 8px;
}

.explanation-body {
  background: #f4f8ff;
  border-radius: 8px;
  padding: 12px 14px;
}

.correct-answer-tip {
  font-size: 13px;
  color: #606266;
  margin-bottom: 8px;
}

.correct-answer-tip strong {
  color: #67c23a;
}

.your-answer em {
  color: #f56c6c;
  font-style: normal;
}

.explanation-text {
  font-size: 13px;
  color: #303133;
  line-height: 1.7;
  margin: 0;
  white-space: pre-wrap;
}

/* ===== 动画 ===== */
.slide-down-enter-active {
  transition: all 0.3s ease-out;
}

.slide-down-enter-from {
  opacity: 0;
  transform: translateY(-8px);
}
</style>
