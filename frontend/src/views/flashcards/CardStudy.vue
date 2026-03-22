<template>
  <div class="card-study-page">
    <!-- 顶部导航 -->
    <div class="study-header">
      <el-button text :icon="ArrowLeft" @click="router.back()">返回</el-button>
      <span class="deck-name">{{ deckTitle }}</span>
      <span class="progress-text">{{ current + 1 }} / {{ cards.length }}</span>
    </div>

    <!-- 进度条 -->
    <el-progress
      :percentage="progressPercent"
      :stroke-width="4"
      :show-text="false"
      class="study-progress"
    />

    <!-- 卡片区域 -->
    <div v-if="cards.length > 0 && current < cards.length" class="card-area">
      <!-- 3D 翻转卡片 -->
      <div
        class="flip-card"
        :class="{ flipped: isFlipped }"
        @click="flip"
      >
        <div class="flip-card-inner">
          <!-- 正面（问题） -->
          <div class="flip-card-front">
            <div class="card-label">问题</div>
            <div class="card-content">{{ cards[current].front }}</div>
            <div class="card-hint">点击翻转查看答案</div>
          </div>
          <!-- 背面（答案） -->
          <div class="flip-card-back">
            <div class="card-label">答案</div>
            <div class="card-content">{{ cards[current].back }}</div>
          </div>
        </div>
      </div>

      <!-- 操作按钮（翻转后显示） -->
      <transition name="fade">
        <div v-if="isFlipped" class="action-buttons">
          <el-button
            type="danger"
            size="large"
            :icon="Close"
            @click="handleReview(false)"
          >
            不会
          </el-button>
          <el-button
            type="success"
            size="large"
            :icon="Check"
            @click="handleReview(true)"
          >
            会了
          </el-button>
        </div>
      </transition>
    </div>

    <!-- 完成状态 -->
    <div v-else-if="cards.length > 0 && current >= cards.length" class="finish-area">
      <div class="finish-icon">🎉</div>
      <h3>本轮学习完成！</h3>
      <p>共学习 {{ cards.length }} 张卡片</p>
      <div class="finish-stats">
        <div class="fstat">
          <span class="fstat-num correct">{{ correctCount }}</span>
          <span class="fstat-label">会了</span>
        </div>
        <div class="fstat">
          <span class="fstat-num wrong">{{ wrongCount }}</span>
          <span class="fstat-label">不会</span>
        </div>
      </div>
      <div class="finish-actions">
        <el-button @click="restart">再来一遍</el-button>
        <el-button type="primary" @click="router.back()">返回卡组</el-button>
      </div>
    </div>

    <!-- 空状态 -->
    <el-empty v-else description="该卡组暂无卡片" />

    <!-- 编辑卡片抽屉 -->
    <el-drawer v-model="showEditDrawer" title="编辑卡片" size="40%" direction="rtl">
      <el-form label-width="60px">
        <el-form-item label="问题">
          <el-input v-model="editForm.front" type="textarea" :rows="4" />
        </el-form-item>
        <el-form-item label="答案">
          <el-input v-model="editForm.back" type="textarea" :rows="6" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showEditDrawer = false">取消</el-button>
        <el-button type="primary" @click="handleSaveEdit">保存</el-button>
      </template>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowLeft, Check, Close } from '@element-plus/icons-vue'
import { getDeck, updateCard, submitReview, type FlashcardVO } from '@/api/flashcard'

const route = useRoute()
const router = useRouter()

const deckId = Number(route.params.id)
const deckTitle = ref('')
const cards = ref<FlashcardVO[]>([])
const current = ref(0)
const isFlipped = ref(false)
const correctCount = ref(0)
const wrongCount = ref(0)

const showEditDrawer = ref(false)
const editForm = ref({ front: '', back: '' })

const progressPercent = computed(() =>
  cards.value.length > 0
    ? Math.round((current.value / cards.value.length) * 100)
    : 0
)

async function loadDeck() {
  try {
    const res = await getDeck(deckId)
    const detail = res.data
    deckTitle.value = detail.deck.title
    cards.value = detail.cards
  } catch {
    ElMessage.error('加载卡组失败')
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
    // 短暂延迟，等待翻转动画归位
    setTimeout(() => {
      current.value++
    }, 300)
  } catch {
    ElMessage.error('提交失败')
  }
}

function restart() {
  current.value = 0
  correctCount.value = 0
  wrongCount.value = 0
  isFlipped.value = false
}

async function handleSaveEdit() {
  const card = cards.value[current.value]
  try {
    await updateCard(card.id, editForm.value)
    card.front = editForm.value.front
    card.back = editForm.value.back
    ElMessage.success('已保存')
    showEditDrawer.value = false
  } catch {
    ElMessage.error('保存失败')
  }
}

onMounted(() => {
  loadDeck()
})
</script>

<style scoped>
.card-study-page {
  max-width: 700px;
  margin: 0 auto;
  padding: 20px 16px;
  min-height: calc(100vh - 80px);
  display: flex;
  flex-direction: column;
}

.study-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}

.deck-name {
  flex: 1;
  font-size: 16px;
  font-weight: 600;
  color: var(--el-text-color-primary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.progress-text {
  font-size: 13px;
  color: var(--el-text-color-secondary);
  white-space: nowrap;
}

.study-progress {
  margin-bottom: 32px;
}

.card-area {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 32px;
}

/* 3D 翻转卡片 */
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
  gap: 16px;
  text-align: center;
}

.finish-icon {
  font-size: 64px;
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
  gap: 40px;
  margin: 8px 0;
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

.finish-actions {
  display: flex;
  gap: 12px;
  margin-top: 8px;
}

/* 过渡动画 */
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>
