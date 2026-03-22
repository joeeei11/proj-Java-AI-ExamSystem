<template>
  <div class="deck-list-page">
    <!-- 顶部操作栏 -->
    <div class="page-header">
      <div class="header-left">
        <h2 class="page-title">抽认卡</h2>
        <el-badge v-if="totalTodayReview > 0" :value="totalTodayReview" class="today-badge">
          <el-button type="warning" @click="goToDailyReview">
            今日复习
          </el-button>
        </el-badge>
        <el-button v-else type="warning" disabled>今日已完成</el-button>
      </div>
      <el-button type="primary" :icon="Plus" @click="showGenerateDialog = true">
        从笔记生成
      </el-button>
    </div>

    <!-- 卡组列表 -->
    <div v-if="decks.length > 0" class="deck-grid">
      <div
        v-for="deck in decks"
        :key="deck.id"
        class="deck-card"
        @click="goToStudy(deck.id)"
      >
        <div class="deck-card-header">
          <div class="deck-title">{{ deck.title }}</div>
          <el-popconfirm
            title="确认删除该卡组及所有卡片？"
            confirm-button-type="danger"
            @confirm.stop="handleDeleteDeck(deck.id)"
          >
            <template #reference>
              <el-button
                text
                type="danger"
                :icon="Delete"
                size="small"
                @click.stop
              />
            </template>
          </el-popconfirm>
        </div>

        <div class="deck-stats">
          <div class="stat-item">
            <span class="stat-num">{{ deck.cardCount }}</span>
            <span class="stat-label">卡片</span>
          </div>
          <div class="stat-divider" />
          <div class="stat-item">
            <span class="stat-num" :class="{ 'has-review': deck.todayReviewCount > 0 }">
              {{ deck.todayReviewCount }}
            </span>
            <span class="stat-label">待复习</span>
          </div>
        </div>

        <div class="deck-footer">
          <span class="deck-date">{{ formatDate(deck.createdAt) }}</span>
          <el-button type="primary" size="small" plain @click.stop="goToStudy(deck.id)">
            学习
          </el-button>
        </div>
      </div>
    </div>

    <!-- 空状态 -->
    <el-empty
      v-else
      description="还没有抽认卡，从笔记生成第一个卡组吧"
      :image-size="120"
    >
      <el-button type="primary" @click="showGenerateDialog = true">从笔记生成</el-button>
    </el-empty>

    <!-- 生成卡组对话框 -->
    <el-dialog
      v-model="showGenerateDialog"
      title="从笔记生成抽认卡"
      width="480px"
      :close-on-click-modal="false"
    >
      <div class="generate-form">
        <el-form label-width="80px">
          <el-form-item label="选择笔记">
            <el-select
              v-model="selectedNoteId"
              filterable
              placeholder="请选择笔记"
              style="width: 100%"
            >
              <el-option
                v-for="note in notes"
                :key="note.id"
                :label="note.title"
                :value="note.id"
              />
            </el-select>
          </el-form-item>
        </el-form>
        <div class="generate-tip">
          <el-icon><InfoFilled /></el-icon>
          AI 将自动分析笔记内容，提取核心知识点，生成 6-12 张抽认卡。
        </div>
      </div>
      <template #footer>
        <el-button @click="showGenerateDialog = false">取消</el-button>
        <el-button
          type="primary"
          :loading="generating"
          :disabled="!selectedNoteId"
          @click="handleGenerate"
        >
          {{ generating ? 'AI 生成中...' : '开始生成' }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Plus, Delete, InfoFilled } from '@element-plus/icons-vue'
import { listDecks, generateFlashcards, deleteDeck, type FlashcardDeckVO } from '@/api/flashcard'
import { getNotes } from '@/api/note'

const router = useRouter()

const decks = ref<FlashcardDeckVO[]>([])
const notes = ref<{ id: number; title: string }[]>([])
const showGenerateDialog = ref(false)
const selectedNoteId = ref<number | null>(null)
const generating = ref(false)

const totalTodayReview = computed(() =>
  decks.value.reduce((sum, d) => sum + d.todayReviewCount, 0)
)

async function loadDecks() {
  try {
    const res = await listDecks()
    decks.value = res.data || []
  } catch {
    ElMessage.error('加载卡组失败')
  }
}

async function loadNotes() {
  try {
    const res = await getNotes()
    notes.value = (res.data || []).map((n: any) => ({ id: n.id, title: n.title }))
  } catch {
    // 静默处理
  }
}

async function handleGenerate() {
  if (!selectedNoteId.value) return
  generating.value = true
  try {
    await generateFlashcards({ noteId: selectedNoteId.value })
    ElMessage.success('抽认卡生成成功！')
    showGenerateDialog.value = false
    selectedNoteId.value = null
    await loadDecks()
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || 'AI 生成失败，请稍后重试')
  } finally {
    generating.value = false
  }
}

async function handleDeleteDeck(id: number) {
  try {
    await deleteDeck(id)
    ElMessage.success('已删除')
    await loadDecks()
  } catch {
    ElMessage.error('删除失败')
  }
}

function goToStudy(deckId: number) {
  router.push(`/flashcards/decks/${deckId}`)
}

function goToDailyReview() {
  router.push('/flashcards/daily-review')
}

function formatDate(dateStr: string) {
  if (!dateStr) return ''
  return dateStr.slice(0, 10)
}

onMounted(() => {
  loadDecks()
  loadNotes()
})
</script>

<style scoped>
.deck-list-page {
  max-width: 960px;
  margin: 0 auto;
  padding: 24px 16px;
}

.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 24px;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.page-title {
  margin: 0;
  font-size: 22px;
  font-weight: 600;
  color: var(--el-text-color-primary);
}

.deck-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
  gap: 16px;
}

.deck-card {
  background: var(--el-bg-color);
  border: 1px solid var(--el-border-color-light);
  border-radius: 10px;
  padding: 18px;
  cursor: pointer;
  transition: box-shadow 0.2s, border-color 0.2s;
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.deck-card:hover {
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.08);
  border-color: var(--el-color-primary-light-5);
}

.deck-card-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 8px;
}

.deck-title {
  font-size: 15px;
  font-weight: 600;
  color: var(--el-text-color-primary);
  line-height: 1.4;
  flex: 1;
  overflow: hidden;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.deck-stats {
  display: flex;
  align-items: center;
  gap: 16px;
}

.stat-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2px;
}

.stat-num {
  font-size: 22px;
  font-weight: 700;
  color: var(--el-text-color-primary);
  line-height: 1;
}

.stat-num.has-review {
  color: var(--el-color-warning);
}

.stat-label {
  font-size: 11px;
  color: var(--el-text-color-secondary);
}

.stat-divider {
  width: 1px;
  height: 32px;
  background: var(--el-border-color-light);
}

.deck-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.deck-date {
  font-size: 12px;
  color: var(--el-text-color-placeholder);
}

.generate-form {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.generate-tip {
  display: flex;
  align-items: flex-start;
  gap: 6px;
  padding: 10px 14px;
  background: var(--el-color-info-light-9);
  border-radius: 6px;
  font-size: 13px;
  color: var(--el-text-color-secondary);
  line-height: 1.5;
}

.generate-tip .el-icon {
  margin-top: 1px;
  flex-shrink: 0;
  color: var(--el-color-info);
}
</style>
