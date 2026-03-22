<template>
  <div class="daily-review-page">
    <!-- 顶部导航 -->
    <div class="review-header">
      <el-button text :icon="ArrowLeft" @click="router.back()">返回</el-button>
      <span class="header-title">今日复习</span>
      <span v-if="!finished" class="progress-text">{{ current + 1 }} / {{ cards.length }}</span>
    </div>

    <!-- 进度条 -->
    <el-progress
      v-if="!finished"
      :percentage="progressPercent"
      :stroke-width="4"
      :show-text="false"
      class="review-progress"
    />

    <!-- 加载中 -->
    <div v-if="loading" class="loading-area">
      <el-skeleton :rows="6" animated />
    </div>

    <!-- 没有待复习 -->
    <div v-else-if="cards.length === 0" class="empty-area">
      <el-empty description="今日暂无待复习卡片">
        <el-button type="primary" @click="router.push('/flashcards')">去学习新卡片</el-button>
      </el-empty>
    </div>

    <!-- 复习中 -->
    <div v-else-if="!finished" class="card-area">
      <!-- 3D 翻转卡片 -->
      <div
        class="flip-card"
        :class="{ flipped: isFlipped }"
        @click="flip"
      >
        <div class="flip-card-inner">
          <div class="flip-card-front">
            <div class="card-label">问题</div>
            <div class="card-content">{{ cards[current].front }}</div>
            <div class="card-hint">点击翻转查看答案</div>
          </div>
          <div class="flip-card-back">
            <div class="card-label">答案</div>
            <div class="card-content">{{ cards[current].back }}</div>
          </div>
        </div>
      </div>

      <!-- 操作按钮（翻转后显示） -->
      <transition name="fade">
        <div v-if="isFlipped" class="action-buttons">
          <el-button type="danger" size="large" :icon="Close" @click="handleReview(false)">
            不会
          </el-button>
          <el-button type="success" size="large" :icon="Check" @click="handleReview(true)">
            会了
          </el-button>
        </div>
      </transition>
    </div>

    <!-- 复习完成 -->
    <div v-else class="finish-area">
      <div class="finish-icon">✅</div>
      <h3>今日复习完成！</h3>
      <p>共复习 {{ cards.length }} 张卡片</p>

      <div class="finish-stats">
        <div class="stat-ring">
          <svg viewBox="0 0 80 80" class="ring-svg">
            <circle cx="40" cy="40" r="32" class="ring-bg" />
            <circle
              cx="40" cy="40" r="32"
              class="ring-fill"
              :stroke-dasharray="`${accuracyDashArray} 201`"
            />
          </svg>
          <div class="ring-label">
            <span class="ring-num">{{ Math.round(accuracy * 100) }}%</span>
            <span class="ring-text">掌握率</span>
          </div>
        </div>

        <div class="stat-detail">
          <div class="fstat">
            <span class="fstat-num correct">{{ correctCount }}</span>
            <span class="fstat-label">会了</span>
          </div>
          <div class="fstat">
            <span class="fstat-num wrong">{{ wrongCount }}</span>
            <span class="fstat-label">不会</span>
          </div>
        </div>
      </div>

      <div class="finish-tip" v-if="wrongCount > 0">
        {{ wrongCount }} 张卡片将在明天再次出现复习
      </div>

      <el-button type="primary" @click="router.push('/flashcards')">返回卡组列表</el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowLeft, Check, Close } from '@element-plus/icons-vue'
import { getTodayReview, submitReview, type FlashcardVO } from '@/api/flashcard'

const router = useRouter()

const loading = ref(true)
const cards = ref<FlashcardVO[]>([])
const current = ref(0)
const isFlipped = ref(false)
const correctCount = ref(0)
const wrongCount = ref(0)
const finished = ref(false)

const progressPercent = computed(() =>
  cards.value.length > 0
    ? Math.round((current.value / cards.value.length) * 100)
    : 0
)

const accuracy = computed(() =>
  cards.value.length > 0 ? correctCount.value / cards.value.length : 0
)

const accuracyDashArray = computed(() =>
  Math.round(accuracy.value * 201)
)

async function loadCards() {
  loading.value = true
  try {
    const res = await getTodayReview()
    cards.value = res.data || []
  } catch {
    ElMessage.error('加载待复习卡片失败')
  } finally {
    loading.value = false
  }
}

function flip() {
  isFlipped.value = !isFlipped.value
}

async function handleReview(known: boolean) {
  const card = cards.value[current.value]
  try {
    await submitReview(card.id, { known })
    if (known) {
      correctCount.value++
    } else {
      wrongCount.value++
    }
    isFlipped.value = false
    setTimeout(() => {
      if (current.value + 1 >= cards.value.length) {
        finished.value = true
      } else {
        current.value++
      }
    }, 300)
  } catch {
    ElMessage.error('提交失败')
  }
}

onMounted(() => {
  loadCards()
})
</script>

<style scoped>
.daily-review-page {
  max-width: 700px;
  margin: 0 auto;
  padding: 20px 16px;
  min-height: calc(100vh - 80px);
  display: flex;
  flex-direction: column;
}

.review-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}

.header-title {
  flex: 1;
  font-size: 16px;
  font-weight: 600;
  color: var(--el-text-color-primary);
}

.progress-text {
  font-size: 13px;
  color: var(--el-text-color-secondary);
}

.review-progress {
  margin-bottom: 32px;
}

.loading-area {
  padding: 24px;
}

.empty-area {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
}

.card-area {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 32px;
}

/* 翻转卡片（与 CardStudy 相同样式） */
.flip-card {
  width: 100%;
  max-width: 560px;
  height: 320px;
  perspective: 1200px;
  cursor: pointer;
}

.flip-card-inner {
  position: relative;
  width: 100%;
  height: 100%;
  transition: transform 0.5s cubic-bezier(0.4, 0, 0.2, 1);
  transform-style: preserve-3d;
}

.flip-card.flipped .flip-card-inner {
  transform: rotateY(180deg);
}

.flip-card-front,
.flip-card-back {
  position: absolute;
  inset: 0;
  backface-visibility: hidden;
  border-radius: 14px;
  padding: 32px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 16px;
  box-shadow: 0 6px 24px rgba(0, 0, 0, 0.08);
}

.flip-card-front {
  background: var(--el-bg-color);
  border: 1px solid var(--el-border-color-light);
}

.flip-card-back {
  background: linear-gradient(135deg, var(--el-color-primary-light-8), var(--el-color-primary-light-9));
  border: 1px solid var(--el-color-primary-light-5);
  transform: rotateY(180deg);
}

.card-label {
  font-size: 11px;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 1.5px;
  color: var(--el-text-color-placeholder);
}

.flip-card-back .card-label {
  color: var(--el-color-primary);
}

.card-content {
  font-size: 18px;
  line-height: 1.6;
  color: var(--el-text-color-primary);
  text-align: center;
  white-space: pre-wrap;
  word-break: break-word;
}

.card-hint {
  font-size: 12px;
  color: var(--el-text-color-placeholder);
}

.action-buttons {
  display: flex;
  gap: 24px;
}

.action-buttons .el-button {
  min-width: 120px;
  font-size: 16px;
}

/* 完成页 */
.finish-area {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 20px;
  text-align: center;
  padding: 20px 0;
}

.finish-icon {
  font-size: 60px;
  line-height: 1;
}

.finish-area h3 {
  margin: 0;
  font-size: 22px;
  color: var(--el-text-color-primary);
}

.finish-area p {
  margin: 0;
  color: var(--el-text-color-secondary);
}

.finish-stats {
  display: flex;
  align-items: center;
  gap: 40px;
}

/* SVG 圆环 */
.stat-ring {
  position: relative;
  width: 80px;
  height: 80px;
}

.ring-svg {
  width: 80px;
  height: 80px;
  transform: rotate(-90deg);
}

.ring-bg {
  fill: none;
  stroke: var(--el-border-color-light);
  stroke-width: 8;
}

.ring-fill {
  fill: none;
  stroke: var(--el-color-success);
  stroke-width: 8;
  stroke-linecap: round;
  stroke-dashoffset: 0;
  transition: stroke-dasharray 0.6s ease;
}

.ring-label {
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}

.ring-num {
  font-size: 14px;
  font-weight: 700;
  color: var(--el-text-color-primary);
}

.ring-text {
  font-size: 10px;
  color: var(--el-text-color-secondary);
}

.stat-detail {
  display: flex;
  gap: 32px;
}

.fstat {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
}

.fstat-num {
  font-size: 32px;
  font-weight: 700;
}

.fstat-num.correct {
  color: var(--el-color-success);
}

.fstat-num.wrong {
  color: var(--el-color-danger);
}

.fstat-label {
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.finish-tip {
  font-size: 13px;
  color: var(--el-text-color-secondary);
  padding: 8px 16px;
  background: var(--el-color-warning-light-9);
  border-radius: 6px;
}

/* 过渡 */
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>
