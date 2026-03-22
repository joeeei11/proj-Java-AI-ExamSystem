<template>
  <div class="review-center">
    <!-- 页头 -->
    <div class="page-header">
      <h2 class="page-title">复习中心</h2>
      <el-button type="primary" :icon="MagicStick" @click="openGenerateDialog">
        AI 生成提纲
      </el-button>
    </div>

    <!-- 提纲列表 -->
    <div v-loading="loading" class="outline-list">
      <el-empty v-if="!loading && outlines.length === 0" description="暂无复习提纲，快去生成一份吧！" />

      <el-card
        v-for="outline in outlines"
        :key="outline.id"
        class="outline-card"
        shadow="hover"
      >
        <template #header>
          <div class="card-header">
            <span class="outline-title">{{ outline.title }}</span>
            <div class="card-actions">
              <span class="outline-date">{{ formatDate(outline.createdAt) }}</span>
              <el-button size="small" type="primary" text @click="viewOutline(outline)">
                查看
              </el-button>
              <el-popconfirm
                title="确定删除此提纲？"
                confirm-button-text="删除"
                cancel-button-text="取消"
                @confirm="handleDelete(outline.id)"
              >
                <template #reference>
                  <el-button size="small" type="danger" text>删除</el-button>
                </template>
              </el-popconfirm>
            </div>
          </div>
        </template>

        <div class="outline-preview">{{ previewContent(outline.content) }}</div>
        <div class="outline-meta">来源笔记 {{ outline.noteIds.length }} 篇</div>
      </el-card>
    </div>

    <!-- 生成提纲对话框 -->
    <el-dialog
      v-model="showGenerateDialog"
      title="AI 生成复习提纲"
      width="560px"
      :close-on-click-modal="false"
    >
      <div class="generate-form">
        <p class="form-hint">选择笔记，AI 将自动提炼知识点、生成结构化提纲</p>

        <div v-if="notesLoading" class="notes-loading">
          <el-skeleton :rows="4" animated />
        </div>
        <el-checkbox-group
          v-else-if="noteList.length > 0"
          v-model="selectedNoteIds"
          class="note-checkbox-group"
        >
          <el-checkbox
            v-for="note in noteList"
            :key="note.id"
            :value="note.id"
            class="note-checkbox-item"
          >
            <div class="note-checkbox-content">
              <span class="note-title">{{ note.title }}</span>
              <el-tag v-if="note.subjectName" size="small" type="info">
                {{ note.subjectName }}
              </el-tag>
            </div>
          </el-checkbox>
        </el-checkbox-group>
        <el-empty
          v-else
          description="暂无笔记，请先创建笔记"
          :image-size="80"
        />
      </div>

      <template #footer>
        <el-button @click="showGenerateDialog = false">取消</el-button>
        <el-button
          type="primary"
          :loading="generating"
          :disabled="selectedNoteIds.length === 0"
          @click="handleGenerate"
        >
          {{ generating ? 'AI 生成中…' : '生成提纲' }}
        </el-button>
      </template>
    </el-dialog>

    <!-- 提纲详情对话框 -->
    <el-dialog
      v-model="showDetailDialog"
      :title="currentOutline?.title"
      width="760px"
      top="5vh"
    >
      <div class="outline-detail">
        <MarkdownViewer v-if="currentOutline" :content="currentOutline.content" />
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { MagicStick } from '@element-plus/icons-vue'
import { getOutlines, generateOutline, deleteOutline, type ReviewOutline } from '@/api/review'
import { getNotes, type Note } from '@/api/note'
import MarkdownViewer from '@/components/MarkdownViewer.vue'

// ===== 状态 =====
const loading = ref(false)
const outlines = ref<ReviewOutline[]>([])
const showGenerateDialog = ref(false)
const showDetailDialog = ref(false)
const currentOutline = ref<ReviewOutline | null>(null)
const noteList = ref<Note[]>([])
const notesLoading = ref(false)
const selectedNoteIds = ref<number[]>([])
const generating = ref(false)

// ===== 初始化 =====
onMounted(loadOutlines)

async function loadOutlines() {
  loading.value = true
  try {
    const res = await getOutlines()
    outlines.value = res.data ?? []
  } finally {
    loading.value = false
  }
}

// ===== 打开生成对话框（懒加载笔记列表） =====
async function openGenerateDialog() {
  showGenerateDialog.value = true
  selectedNoteIds.value = []
  if (noteList.value.length > 0) return
  notesLoading.value = true
  try {
    const res = await getNotes()
    noteList.value = res.data ?? []
  } finally {
    notesLoading.value = false
  }
}

// ===== 生成提纲 =====
async function handleGenerate() {
  if (selectedNoteIds.value.length === 0) return
  generating.value = true
  try {
    const res = await generateOutline({ noteIds: selectedNoteIds.value })
    if (res.data) outlines.value.unshift(res.data)
    showGenerateDialog.value = false
    ElMessage.success('提纲生成成功！')
    selectedNoteIds.value = []
  } catch (err: any) {
    ElMessage.error(err?.message || 'AI 生成失败，请稍后重试')
  } finally {
    generating.value = false
  }
}

// ===== 查看详情 =====
function viewOutline(outline: ReviewOutline) {
  currentOutline.value = outline
  showDetailDialog.value = true
}

// ===== 删除提纲 =====
async function handleDelete(id: number) {
  try {
    await deleteOutline(id)
    outlines.value = outlines.value.filter((o) => o.id !== id)
    ElMessage.success('提纲已删除')
  } catch {
    ElMessage.error('删除失败，请重试')
  }
}

// ===== 工具函数 =====
function previewContent(content: string) {
  return content
    .split('\n')
    .filter(Boolean)
    .slice(0, 3)
    .join(' | ')
    .replace(/#+\s*/g, '')
}

function formatDate(dateStr: string) {
  const d = new Date(dateStr)
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
}
</script>

<style scoped>
.review-center {
  max-width: 860px;
  margin: 0 auto;
  padding: 20px;
}

.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 24px;
}

.page-title {
  margin: 0;
  font-size: 22px;
  font-weight: 600;
  color: var(--el-text-color-primary);
}

.outline-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.outline-card {
  transition: box-shadow 0.2s;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.outline-title {
  font-size: 16px;
  font-weight: 600;
  color: var(--el-text-color-primary);
  flex: 1;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.card-actions {
  display: flex;
  align-items: center;
  gap: 4px;
  flex-shrink: 0;
}

.outline-date {
  font-size: 12px;
  color: var(--el-text-color-placeholder);
  margin-right: 8px;
}

.outline-preview {
  font-size: 14px;
  color: var(--el-text-color-secondary);
  line-height: 1.6;
  overflow: hidden;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  margin-bottom: 8px;
}

.outline-meta {
  font-size: 12px;
  color: var(--el-text-color-placeholder);
}

.form-hint {
  color: var(--el-text-color-secondary);
  font-size: 14px;
  margin-bottom: 16px;
}

.notes-loading {
  padding: 8px 0;
}

.note-checkbox-group {
  display: flex;
  flex-direction: column;
  gap: 8px;
  max-height: 320px;
  overflow-y: auto;
}

.note-checkbox-item {
  margin-right: 0 !important;
  padding: 8px 12px;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 8px;
  width: 100%;
  transition: border-color 0.15s;
}

.note-checkbox-item:hover {
  border-color: var(--el-color-primary-light-5);
}

.note-checkbox-content {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  width: 100%;
}

.note-title {
  font-size: 14px;
  color: var(--el-text-color-primary);
}

.outline-detail {
  max-height: 70vh;
  overflow-y: auto;
  padding: 4px 0;
}
</style>
