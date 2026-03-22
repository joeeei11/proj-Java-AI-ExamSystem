<template>
  <div class="note-list-page">
    <!-- 顶部操作栏 -->
    <div class="toolbar">
      <div class="toolbar-left">
        <h2 class="page-title">我的笔记</h2>
        <el-tag v-if="notes.length" type="info" size="small" class="count-tag">
          共 {{ notes.length }} 篇
        </el-tag>
      </div>
      <div class="toolbar-right">
        <!-- 科目过滤 -->
        <el-select
          v-model="filterSubjectId"
          placeholder="全部科目"
          clearable
          size="default"
          style="width: 160px"
          @change="loadNotes"
        >
          <el-option
            v-for="s in subjects"
            :key="s.id"
            :label="s.name"
            :value="s.id"
          />
        </el-select>

        <!-- 搜索框 -->
        <el-input
          v-model="searchKeyword"
          placeholder="搜索笔记标题..."
          clearable
          size="default"
          style="width: 220px"
          :prefix-icon="Search"
        />

        <!-- 新建按钮 -->
        <el-button type="primary" :icon="Plus" @click="openCreateDialog">
          新建笔记
        </el-button>
      </div>
    </div>

    <!-- 笔记卡片列表 -->
    <div v-loading="loading" class="notes-grid">
      <template v-if="filteredNotes.length">
        <div
          v-for="note in filteredNotes"
          :key="note.id"
          class="note-card"
          @click="goToDetail(note.id)"
        >
          <!-- 来源标记 -->
          <el-tag
            :type="sourceTagType(note.source)"
            size="small"
            class="source-tag"
            effect="light"
          >
            {{ sourceLabel(note.source) }}
          </el-tag>

          <!-- 标题 -->
          <h3 class="note-title">{{ note.title }}</h3>

          <!-- 内容预览 -->
          <p class="note-preview">{{ previewText(note.content) }}</p>

          <!-- 底部元信息 -->
          <div class="note-meta">
            <el-tag v-if="note.subjectName" size="small" type="success" effect="plain">
              {{ note.subjectName }}
            </el-tag>
            <span class="note-time">{{ formatTime(note.updatedAt) }}</span>
          </div>
        </div>
      </template>

      <el-empty
        v-else-if="!loading"
        description="暂无笔记，点击「新建笔记」开始记录"
        :image-size="120"
      />
    </div>

    <!-- 新建笔记对话框 -->
    <el-dialog
      v-model="createDialogVisible"
      title="新建笔记"
      width="520px"
      :close-on-click-modal="false"
    >
      <el-form :model="createForm" :rules="createRules" ref="createFormRef" label-width="80px">
        <el-form-item label="标题" prop="title">
          <el-input v-model="createForm.title" placeholder="请输入笔记标题" maxlength="200" show-word-limit />
        </el-form-item>
        <el-form-item label="关联科目">
          <el-select v-model="createForm.subjectId" placeholder="选择科目（可选）" clearable style="width: 100%">
            <el-option v-for="s in subjects" :key="s.id" :label="s.name" :value="s.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="初始内容">
          <el-input
            v-model="createForm.content"
            type="textarea"
            :rows="4"
            placeholder="输入笔记内容（可选）"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="creating" @click="submitCreate">创建</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, type FormInstance } from 'element-plus'
import { Plus, Search } from '@element-plus/icons-vue'
import { getNotes, createNote, type Note } from '@/api/note'
import { getSubjects } from '@/api/subject'

const router = useRouter()

// ===== 状态 =====
const loading = ref(false)
const notes = ref<Note[]>([])
const subjects = ref<{ id: number; name: string }[]>([])
const filterSubjectId = ref<number | null>(null)
const searchKeyword = ref('')

// 新建对话框
const createDialogVisible = ref(false)
const creating = ref(false)
const createFormRef = ref<FormInstance>()
const createForm = reactive({ title: '', content: '', subjectId: undefined as number | undefined })
const createRules = { title: [{ required: true, message: '标题不能为空', trigger: 'blur' }] }

// ===== 计算属性 =====
const filteredNotes = computed(() => {
  const keyword = searchKeyword.value.trim().toLowerCase()
  if (!keyword) return notes.value
  return notes.value.filter((n) => n.title.toLowerCase().includes(keyword))
})

// ===== 初始化 =====
onMounted(async () => {
  await Promise.all([loadNotes(), loadSubjects()])
})

async function loadNotes() {
  loading.value = true
  try {
    const res = await getNotes(filterSubjectId.value ?? undefined)
    notes.value = res.data
  } catch {
    // 错误已由 request.ts 拦截器处理
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

// ===== 操作 =====
function goToDetail(id: number) {
  router.push(`/notes/${id}`)
}

function openCreateDialog() {
  Object.assign(createForm, { title: '', content: '', subjectId: undefined })
  createDialogVisible.value = true
}

async function submitCreate() {
  await createFormRef.value?.validate()
  creating.value = true
  try {
    const res = await createNote({
      title: createForm.title,
      content: createForm.content || undefined,
      subjectId: createForm.subjectId,
    })
    notes.value.unshift(res.data)
    createDialogVisible.value = false
    ElMessage.success('笔记创建成功')
    router.push(`/notes/${res.data.id}`)
  } finally {
    creating.value = false
  }
}

// ===== 工具函数 =====
function sourceLabel(source: number) {
  return source === 2 ? 'OCR' : source === 3 ? 'AI整理' : '手动'
}

function sourceTagType(source: number): 'primary' | 'success' | 'warning' {
  return source === 2 ? 'warning' : source === 3 ? 'success' : 'primary'
}

function previewText(content?: string) {
  if (!content) return '暂无内容'
  // 去除 Markdown 符号，取前 80 字符
  return content.replace(/[#*`_>\-\[\]()!]/g, '').slice(0, 80) + (content.length > 80 ? '...' : '')
}

function formatTime(timeStr: string) {
  if (!timeStr) return ''
  const date = new Date(timeStr)
  const now = new Date()
  const diffMs = now.getTime() - date.getTime()
  const diffDays = Math.floor(diffMs / 86400000)
  if (diffDays === 0) return '今天 ' + date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
  if (diffDays === 1) return '昨天'
  if (diffDays < 7) return `${diffDays} 天前`
  return date.toLocaleDateString('zh-CN', { month: 'short', day: 'numeric' })
}
</script>

<style scoped>
.note-list-page {
  padding: 24px;
  max-width: 1200px;
  margin: 0 auto;
}

.toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 24px;
  flex-wrap: wrap;
  gap: 12px;
}

.toolbar-left {
  display: flex;
  align-items: center;
  gap: 10px;
}

.page-title {
  font-size: 1.4rem;
  font-weight: 600;
  color: var(--el-text-color-primary);
  margin: 0;
}

.count-tag {
  font-size: 12px;
}

.toolbar-right {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.notes-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 16px;
}

.note-card {
  background: var(--el-bg-color);
  border: 1px solid var(--el-border-color-light);
  border-radius: 10px;
  padding: 16px 18px;
  cursor: pointer;
  transition: box-shadow 0.2s, transform 0.2s;
  position: relative;
  min-height: 140px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.note-card:hover {
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.1);
  transform: translateY(-2px);
}

.source-tag {
  position: absolute;
  top: 12px;
  right: 12px;
}

.note-title {
  font-size: 1rem;
  font-weight: 600;
  color: var(--el-text-color-primary);
  margin: 0;
  padding-right: 60px;
  line-height: 1.4;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.note-preview {
  font-size: 0.85rem;
  color: var(--el-text-color-secondary);
  line-height: 1.5;
  margin: 0;
  flex: 1;
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.note-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: auto;
}

.note-time {
  font-size: 0.78rem;
  color: var(--el-text-color-placeholder);
}
</style>
