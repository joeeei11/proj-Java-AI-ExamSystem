<template>
  <div class="my-feedback">
    <!-- 页头 -->
    <div class="page-header">
      <div class="header-left">
        <h2 class="page-title">
          <el-icon class="title-icon"><Comment /></el-icon>
          我的反馈
        </h2>
        <p class="page-subtitle">查看题目纠错反馈进度，追踪管理员处理结果</p>
      </div>
      <el-button type="primary" @click="openSubmitDialog">
        <el-icon><Plus /></el-icon>
        提交反馈
      </el-button>
    </div>

    <!-- 反馈列表 -->
    <el-card class="list-card" shadow="never">
      <el-table
        :data="feedbackList"
        v-loading="loading"
        stripe
        style="width: 100%"
      >
        <el-table-column label="题目摘要" min-width="200" show-overflow-tooltip>
          <template #default="{ row }">
            <span class="question-summary">{{ row.questionSummary || '题目已删除' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="反馈类型" width="110" align="center">
          <template #default="{ row }">
            <el-tag type="info" size="small">{{ row.typeLabel }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag
              :type="FEEDBACK_STATUS_MAP[row.status]?.type"
              size="small"
            >
              {{ row.statusLabel }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="提交时间" width="150" align="center">
          <template #default="{ row }">
            {{ formatDate(row.createdAt) }}
          </template>
        </el-table-column>
        <el-table-column label="回复时间" width="150" align="center">
          <template #default="{ row }">
            {{ row.repliedAt ? formatDate(row.repliedAt) : '--' }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="90" align="center" fixed="right">
          <template #default="{ row }">
            <el-button size="small" text type="primary" @click="viewDetail(row.id)">
              查看详情
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 空状态 -->
      <el-empty
        v-if="!loading && feedbackList.length === 0"
        description="暂无反馈记录，遇到题目问题可点击右上角提交"
        :image-size="80"
      />

      <!-- 分页 -->
      <div class="pagination-wrapper" v-if="total > pageSize">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :total="total"
          layout="total, prev, pager, next"
          background
          @current-change="loadList"
        />
      </div>
    </el-card>

    <!-- ===== 提交反馈对话框 ===== -->
    <el-dialog
      v-model="submitDialogVisible"
      title="提交题目纠错反馈"
      width="520px"
      :close-on-click-modal="false"
    >
      <el-form
        ref="submitFormRef"
        :model="submitForm"
        :rules="submitRules"
        label-position="top"
      >
        <el-form-item label="反馈类型" prop="type">
          <el-select v-model="submitForm.type" placeholder="请选择反馈类型" style="width: 100%">
            <el-option
              v-for="t in FEEDBACK_TYPES"
              :key="t.value"
              :label="t.label"
              :value="t.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="详细描述（可选）" prop="description">
          <el-input
            v-model="submitForm.description"
            type="textarea"
            :rows="4"
            placeholder="请描述具体问题，如：选项 A 的答案应为 XX..."
            maxlength="500"
            show-word-limit
          />
        </el-form-item>
        <el-form-item label="题目 ID" prop="questionId">
          <el-input-number
            v-model="submitForm.questionId"
            :min="1"
            placeholder="题目 ID"
            style="width: 100%"
            controls-position="right"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="submitDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">
          提交反馈
        </el-button>
      </template>
    </el-dialog>

    <!-- ===== 详情对话框 ===== -->
    <el-dialog
      v-model="detailDialogVisible"
      title="反馈详情"
      width="600px"
    >
      <div v-if="detailVO" class="detail-content">
        <!-- 题目信息 -->
        <div class="detail-section">
          <div class="section-title">题目内容</div>
          <div class="question-box">{{ detailVO.questionContent || '题目已删除' }}</div>
        </div>
        <div v-if="detailVO.questionAnswer" class="detail-section">
          <div class="section-title">题目答案</div>
          <div class="answer-box">{{ detailVO.questionAnswer }}</div>
        </div>

        <!-- 反馈信息 -->
        <el-descriptions :column="2" border size="small" class="detail-desc">
          <el-descriptions-item label="反馈类型">
            <el-tag type="info" size="small">{{ detailVO.typeLabel }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="处理状态">
            <el-tag :type="FEEDBACK_STATUS_MAP[detailVO.status]?.type" size="small">
              {{ detailVO.statusLabel }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="提交时间">
            {{ formatDate(detailVO.createdAt) }}
          </el-descriptions-item>
          <el-descriptions-item label="回复时间">
            {{ detailVO.repliedAt ? formatDate(detailVO.repliedAt) : '待处理' }}
          </el-descriptions-item>
          <el-descriptions-item label="详细描述" :span="2">
            {{ detailVO.description || '无' }}
          </el-descriptions-item>
        </el-descriptions>

        <!-- 管理员回复 -->
        <div v-if="detailVO.adminReply" class="admin-reply">
          <div class="reply-title">
            <el-icon><ChatLineRound /></el-icon>
            管理员回复
          </div>
          <div class="reply-content">{{ detailVO.adminReply }}</div>
        </div>
        <el-empty
          v-else-if="detailVO.status === 0"
          description="尚未收到管理员回复，请耐心等待"
          :image-size="60"
        />
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { Comment, Plus, ChatLineRound } from '@element-plus/icons-vue'
import {
  getMyFeedback,
  getMyFeedbackDetail,
  submitFeedback,
  FEEDBACK_TYPES,
  FEEDBACK_STATUS_MAP,
  type FeedbackVO,
  type FeedbackDetailVO,
} from '@/api/feedback'

// ===== 列表状态 =====
const loading = ref(false)
const feedbackList = ref<FeedbackVO[]>([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(10)

// ===== 提交对话框 =====
const submitDialogVisible = ref(false)
const submitting = ref(false)
const submitFormRef = ref<FormInstance>()
const submitForm = ref({
  questionId: null as number | null,
  type: null as number | null,
  description: '',
})

const submitRules: FormRules = {
  questionId: [{ required: true, message: '请输入题目 ID', trigger: 'blur' }],
  type: [{ required: true, message: '请选择反馈类型', trigger: 'change' }],
}

// ===== 详情对话框 =====
const detailDialogVisible = ref(false)
const detailVO = ref<FeedbackDetailVO | null>(null)

// ===== 生命周期 =====
onMounted(loadList)

// ===== 方法 =====

async function loadList() {
  loading.value = true
  try {
    const res = await getMyFeedback({ page: currentPage.value, size: pageSize.value })
    feedbackList.value = res.data.records
    total.value = Number(res.data.total)
  } catch {
    // 已由拦截器处理
  } finally {
    loading.value = false
  }
}

function openSubmitDialog() {
  submitForm.value = { questionId: null, type: null, description: '' }
  submitDialogVisible.value = true
}

async function handleSubmit() {
  if (!submitFormRef.value) return
  const valid = await submitFormRef.value.validate().catch(() => false)
  if (!valid) return

  submitting.value = true
  try {
    await submitFeedback({
      questionId: submitForm.value.questionId!,
      type: submitForm.value.type!,
      description: submitForm.value.description || undefined,
    })
    ElMessage.success('反馈提交成功，感谢您的贡献！')
    submitDialogVisible.value = false
    loadList()
  } catch {
    // 已由拦截器处理
  } finally {
    submitting.value = false
  }
}

async function viewDetail(id: number) {
  detailVO.value = null
  detailDialogVisible.value = true
  try {
    const res = await getMyFeedbackDetail(id)
    detailVO.value = res.data
  } catch {
    detailDialogVisible.value = false
  }
}

function formatDate(dateStr: string) {
  if (!dateStr) return '--'
  const d = new Date(dateStr)
  return d.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  })
}
</script>

<style scoped>
.my-feedback {
  max-width: 1000px;
  margin: 0 auto;
}

.page-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  margin-bottom: 20px;
}

.header-left {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.page-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 22px;
  font-weight: 700;
  color: var(--el-text-color-primary);
  margin: 0;
}

.title-icon {
  color: var(--el-color-primary);
  font-size: 22px;
}

.page-subtitle {
  font-size: 14px;
  color: var(--el-text-color-secondary);
  margin: 0;
}

.list-card {
  border-radius: 10px;
}

.question-summary {
  font-size: 13px;
  color: var(--el-text-color-regular);
}

.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}

/* ===== 详情弹框 ===== */
.detail-content {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.detail-section {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.section-title {
  font-size: 13px;
  font-weight: 600;
  color: var(--el-text-color-secondary);
}

.question-box {
  background: var(--el-fill-color-light);
  border-radius: 6px;
  padding: 12px;
  font-size: 14px;
  line-height: 1.7;
  white-space: pre-wrap;
  max-height: 150px;
  overflow-y: auto;
}

.answer-box {
  background: #f0f9eb;
  border-radius: 6px;
  padding: 10px 12px;
  font-size: 14px;
  color: #529b2e;
  font-weight: 500;
}

.detail-desc {
  margin-top: 4px;
}

.admin-reply {
  background: #ecf5ff;
  border-radius: 8px;
  padding: 14px 16px;
}

.reply-title {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  font-weight: 600;
  color: var(--el-color-primary);
  margin-bottom: 8px;
}

.reply-content {
  font-size: 14px;
  line-height: 1.7;
  color: var(--el-text-color-regular);
  white-space: pre-wrap;
}
</style>
