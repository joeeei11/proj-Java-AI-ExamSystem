<template>
  <div class="today-review">
    <!-- 加载中 -->
    <div v-if="loading" class="loading-wrap">
      <el-skeleton :rows="5" animated />
    </div>

    <!-- 完成 -->
    <div v-else-if="done" class="done-wrap">
      <el-result icon="success" title="今日复习完成！">
        <template #sub-title>
          <p>共复做 {{ total }} 题，掌握 {{ knownCount }} 题</p>
        </template>
        <template #extra>
          <el-button type="primary" @click="emit('done')">返回错题本</el-button>
        </template>
      </el-result>
    </div>

    <!-- 无待复习 -->
    <div v-else-if="list.length === 0" class="empty-wrap">
      <el-empty description="今日暂无待复做题目">
        <el-button @click="emit('done')">关闭</el-button>
      </el-empty>
    </div>

    <!-- 复习题卡 -->
    <div v-else class="review-wrap">
      <!-- 进度 -->
      <div class="progress-bar">
        <el-progress
          :percentage="Math.round((currentIndex / total) * 100)"
          :stroke-width="6"
          color="var(--el-color-primary)"
        />
        <span class="progress-text">{{ currentIndex }} / {{ total }}</span>
      </div>

      <!-- 当前题目 -->
      <el-card class="review-card" shadow="never">
        <div class="question-content">{{ current.questionContent }}</div>

        <!-- 选项 -->
        <div v-if="parsedOptions" class="options-list">
          <div
            v-for="opt in parsedOptions"
            :key="opt.key"
            class="option-item"
            :class="{
              correct: revealed && isCorrect(opt.key),
              wrong: revealed && !isCorrect(opt.key) && selectedAnswer === opt.key,
            }"
          >
            <span class="option-key">{{ opt.key }}.</span>
            <span>{{ opt.value }}</span>
          </div>
        </div>

        <!-- 正确答案（揭晓后显示） -->
        <transition name="fade">
          <div v-if="revealed" class="answer-section">
            <div class="answer-row">
              <span class="label">正确答案：</span>
              <span class="answer-text">{{ current.answer }}</span>
            </div>
            <div v-if="current.explanation" class="explanation">
              <span class="label">解析：</span>{{ current.explanation }}
            </div>
          </div>
        </transition>
      </el-card>

      <!-- 操作按钮 -->
      <div class="action-buttons">
        <template v-if="!revealed">
          <el-button size="large" @click="reveal">查看答案</el-button>
        </template>
        <template v-else>
          <el-button
            size="large"
            type="danger"
            plain
            :loading="submitting"
            @click="submit(false)"
          >
            不会
          </el-button>
          <el-button
            size="large"
            type="success"
            :loading="submitting"
            @click="submit(true)"
          >
            会了
          </el-button>
        </template>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getTodayReview, submitReview, type ErrorBookItem } from '@/api/errorbook'

const emit = defineEmits<{ done: [] }>()

// ===== 状态 =====
const loading = ref(true)
const submitting = ref(false)
const done = ref(false)
const list = ref<ErrorBookItem[]>([])
const currentIndex = ref(0)
const revealed = ref(false)
const selectedAnswer = ref('')
const knownCount = ref(0)
const total = ref(0)

// ===== 计算 =====
const current = computed(() => list.value[currentIndex.value])

const parsedOptions = computed(() => {
  if (!current.value?.options) return null
  try {
    return JSON.parse(current.value.options) as { key: string; value: string }[]
  } catch {
    return null
  }
})

function isCorrect(key: string) {
  return current.value?.answer?.includes(key)
}

// ===== 初始化 =====
onMounted(async () => {
  try {
    const res = await getTodayReview()
    list.value = res.data ?? []
    total.value = list.value.length
  } finally {
    loading.value = false
  }
})

// ===== 操作 =====
function reveal() {
  revealed.value = true
}

async function submit(known: boolean) {
  if (!current.value) return
  submitting.value = true
  try {
    await submitReview(current.value.id, { known })
    if (known) knownCount.value++

    // 下一题
    currentIndex.value++
    revealed.value = false
    selectedAnswer.value = ''

    if (currentIndex.value >= list.value.length) {
      done.value = true
    }
  } catch {
    ElMessage.error('提交失败，请重试')
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.today-review {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.loading-wrap,
.done-wrap,
.empty-wrap {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
}

.review-wrap {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.progress-bar {
  display: flex;
  align-items: center;
  gap: 12px;
}

.progress-bar .el-progress {
  flex: 1;
}

.progress-text {
  font-size: 13px;
  color: var(--el-text-color-secondary);
  white-space: nowrap;
}

.review-card {
  border: 1px solid var(--el-border-color-lighter);
}

.question-content {
  font-size: 16px;
  line-height: 1.7;
  margin-bottom: 16px;
}

.options-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-bottom: 16px;
}

.option-item {
  display: flex;
  gap: 8px;
  padding: 8px 12px;
  border-radius: 8px;
  background: var(--el-fill-color-lighter);
  font-size: 14px;
  cursor: pointer;
  transition: background 0.15s;
}

.option-item.correct {
  background: var(--el-color-success-light-9);
  color: var(--el-color-success);
  font-weight: 500;
}

.option-item.wrong {
  background: var(--el-color-danger-light-9);
  color: var(--el-color-danger);
}

.option-key {
  font-weight: 600;
  min-width: 20px;
}

.answer-section {
  padding-top: 12px;
  border-top: 1px dashed var(--el-border-color-lighter);
}

.answer-row {
  margin-bottom: 8px;
  font-size: 15px;
}

.label {
  color: var(--el-text-color-secondary);
  font-size: 14px;
}

.answer-text {
  color: var(--el-color-success);
  font-weight: 600;
}

.explanation {
  font-size: 14px;
  color: var(--el-text-color-secondary);
  line-height: 1.6;
}

.action-buttons {
  display: flex;
  justify-content: center;
  gap: 16px;
  padding-top: 8px;
}

.action-buttons .el-button {
  min-width: 120px;
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>
