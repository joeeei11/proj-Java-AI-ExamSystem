<template>
  <div class="note-detail-page" v-loading="loading">
    <!-- 顶部导航 -->
    <div class="detail-header">
      <el-button :icon="ArrowLeft" text @click="router.back()">返回笔记列表</el-button>
      <div class="header-actions">
        <el-button v-if="!editMode" :icon="Edit" @click="enterEdit">编辑</el-button>
        <el-button v-if="editMode" :icon="Close" @click="cancelEdit">取消</el-button>
        <el-button v-if="editMode" type="primary" :icon="Check" :loading="saving" @click="saveNote">保存</el-button>
        <el-button
          type="success"
          :icon="MagicStick"
          :loading="organizing"
          :disabled="editMode"
          @click="handleOrganize"
        >
          AI 整理
        </el-button>
        <el-popconfirm title="确认删除此笔记？删除后不可恢复" @confirm="handleDelete">
          <template #reference>
            <el-button type="danger" :icon="Delete" plain :disabled="editMode">删除</el-button>
          </template>
        </el-popconfirm>
      </div>
    </div>

    <template v-if="note">
      <!-- 笔记信息卡片 -->
      <div class="note-info-bar">
        <div class="info-left">
          <!-- 标题：预览/编辑 -->
          <template v-if="editMode">
            <el-input
              v-model="editForm.title"
              size="large"
              placeholder="笔记标题"
              class="title-input"
            />
          </template>
          <h1 v-else class="note-title">{{ note.title }}</h1>
        </div>
        <div class="info-meta">
          <el-tag :type="sourceTagType(note.source)" effect="light" size="small">
            {{ sourceLabel(note.source) }}
          </el-tag>
          <el-tag v-if="note.subjectName" type="success" effect="plain" size="small">
            {{ note.subjectName }}
          </el-tag>
          <span class="meta-time">更新于 {{ formatTime(note.updatedAt) }}</span>
        </div>
      </div>

      <!-- 编辑模式：科目选择 -->
      <div v-if="editMode" class="edit-subject-row">
        <span class="label">关联科目：</span>
        <el-select v-model="editForm.subjectId" placeholder="选择科目（可选）" clearable size="small" style="width: 200px">
          <el-option v-for="s in subjects" :key="s.id" :label="s.name" :value="s.id" />
        </el-select>
      </div>

      <!-- 主体内容区域 -->
      <div class="content-area">
        <!-- 左：OCR 图片（若有） + OCR 上传 -->
        <div class="side-panel">
          <!-- 原始图片 -->
          <div v-if="note.imageUrl" class="panel-section">
            <p class="panel-label">原始图片</p>
            <el-image
              :src="note.imageUrl"
              fit="contain"
              class="ocr-image"
              :preview-src-list="[note.imageUrl]"
            />
          </div>

          <!-- OCR 上传区 -->
          <div class="panel-section">
            <p class="panel-label">上传图片 OCR</p>
            <el-upload
              drag
              action=""
              :auto-upload="false"
              accept="image/*"
              :show-file-list="false"
              :on-change="handleOcrUpload"
            >
              <el-icon class="el-icon--upload"><upload-filled /></el-icon>
              <div class="el-upload__text">拖拽或<em>点击上传</em>图片</div>
              <template #tip>
                <div class="el-upload__tip">支持 JPG / PNG / WEBP，自动识别文字并创建笔记</div>
              </template>
            </el-upload>
            <div v-if="ocrUploading" class="ocr-loading">
              <el-icon class="is-loading"><loading /></el-icon>
              <span>识别中...</span>
            </div>
          </div>
        </div>

        <!-- 右：内容编辑/预览 -->
        <div class="main-content">
          <!-- 编辑模式 -->
          <el-input
            v-if="editMode"
            v-model="editForm.content"
            type="textarea"
            :rows="20"
            placeholder="输入 Markdown 内容..."
            class="content-textarea"
          />
          <!-- 预览模式 -->
          <div v-else class="preview-wrapper">
            <MarkdownViewer :content="note.content" placeholder="暂无内容，点击「编辑」添加，或上传图片 OCR 识别" />
          </div>
        </div>
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowLeft, Edit, Close, Check, Delete, MagicStick, UploadFilled, Loading } from '@element-plus/icons-vue'
import type { UploadFile } from 'element-plus'
import { getNoteById, updateNote, deleteNote, ocrNote, organizeNote, type Note } from '@/api/note'
import { getSubjects } from '@/api/subject'
import MarkdownViewer from '@/components/MarkdownViewer.vue'

const route = useRoute()
const router = useRouter()

// ===== 状态 =====
const loading = ref(false)
const note = ref<Note | null>(null)
const subjects = ref<{ id: number; name: string }[]>([])

const editMode = ref(false)
const saving = ref(false)
const organizing = ref(false)
const ocrUploading = ref(false)

const editForm = reactive({ title: '', content: '', subjectId: undefined as number | undefined })

// ===== 初始化 =====
onMounted(async () => {
  const id = Number(route.params.id)
  await Promise.all([loadNote(id), loadSubjects()])
})

async function loadNote(id: number) {
  loading.value = true
  try {
    const res = await getNoteById(id)
    note.value = res.data
  } finally {
    loading.value = false
  }
}

async function loadSubjects() {
  try {
    const res = await getSubjects()
    subjects.value = res.data
  } catch { /* ignore */ }
}

// ===== 编辑操作 =====
function enterEdit() {
  if (!note.value) return
  Object.assign(editForm, {
    title: note.value.title,
    content: note.value.content ?? '',
    subjectId: note.value.subjectId,
  })
  editMode.value = true
}

function cancelEdit() {
  editMode.value = false
}

async function saveNote() {
  if (!note.value) return
  saving.value = true
  try {
    const res = await updateNote(note.value.id, {
      title: editForm.title,
      content: editForm.content,
      subjectId: editForm.subjectId,
    })
    note.value = res.data
    editMode.value = false
    ElMessage.success('保存成功')
  } finally {
    saving.value = false
  }
}

async function handleDelete() {
  if (!note.value) return
  await deleteNote(note.value.id)
  ElMessage.success('已删除')
  router.push('/notes')
}

// ===== AI 整理 =====
async function handleOrganize() {
  if (!note.value) return
  organizing.value = true
  try {
    ElMessage.info('正在调用 AI 整理，请稍候...')
    const res = await organizeNote(note.value.id)
    note.value = res.data
    ElMessage.success('AI 整理完成')
  } finally {
    organizing.value = false
  }
}

// ===== OCR 上传 =====
async function handleOcrUpload(uploadFile: UploadFile) {
  if (!uploadFile.raw) return
  ocrUploading.value = true
  try {
    const subjectId = note.value?.subjectId
    const res = await ocrNote(uploadFile.raw, subjectId)
    // OCR 创建了新笔记，跳转到新笔记详情
    ElMessage.success('OCR 识别成功，已创建新笔记')
    router.push(`/notes/${res.data.id}`)
  } finally {
    ocrUploading.value = false
  }
}

// ===== 工具函数 =====
function sourceLabel(source: number) {
  return source === 2 ? 'OCR' : source === 3 ? 'AI整理' : '手动'
}

function sourceTagType(source: number): 'primary' | 'success' | 'warning' {
  return source === 2 ? 'warning' : source === 3 ? 'success' : 'primary'
}

function formatTime(timeStr?: string) {
  if (!timeStr) return ''
  return new Date(timeStr).toLocaleString('zh-CN', {
    month: 'short',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  })
}
</script>

<style scoped>
.note-detail-page {
  padding: 24px;
  max-width: 1200px;
  margin: 0 auto;
}

.detail-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
  flex-wrap: wrap;
  gap: 10px;
}

.header-actions {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.note-info-bar {
  margin-bottom: 12px;
}

.note-title {
  font-size: 1.5rem;
  font-weight: 700;
  color: var(--el-text-color-primary);
  margin: 0 0 8px;
  line-height: 1.3;
}

.title-input {
  font-size: 1.2rem;
  margin-bottom: 8px;
}

.info-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.meta-time {
  font-size: 0.82rem;
  color: var(--el-text-color-placeholder);
}

.edit-subject-row {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 16px;
}

.label {
  font-size: 0.88rem;
  color: var(--el-text-color-secondary);
  white-space: nowrap;
}

.content-area {
  display: flex;
  gap: 20px;
  align-items: flex-start;
}

.side-panel {
  width: 260px;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.panel-section {
  background: var(--el-bg-color);
  border: 1px solid var(--el-border-color-light);
  border-radius: 8px;
  padding: 12px;
}

.panel-label {
  font-size: 0.82rem;
  color: var(--el-text-color-secondary);
  margin: 0 0 8px;
  font-weight: 500;
}

.ocr-image {
  width: 100%;
  max-height: 200px;
  border-radius: 4px;
}

.ocr-loading {
  display: flex;
  align-items: center;
  gap: 6px;
  color: var(--el-color-primary);
  font-size: 0.88rem;
  margin-top: 8px;
}

.main-content {
  flex: 1;
  min-width: 0;
}

.content-textarea :deep(.el-textarea__inner) {
  font-family: 'Consolas', 'Monaco', monospace;
  font-size: 0.9rem;
  line-height: 1.6;
  border-radius: 8px;
}

.preview-wrapper {
  background: var(--el-bg-color);
  border: 1px solid var(--el-border-color-light);
  border-radius: 8px;
  padding: 20px 24px;
  min-height: 300px;
}

/* 响应式：小屏幕竖向排列 */
@media (max-width: 768px) {
  .content-area {
    flex-direction: column-reverse;
  }

  .side-panel {
    width: 100%;
  }
}
</style>
