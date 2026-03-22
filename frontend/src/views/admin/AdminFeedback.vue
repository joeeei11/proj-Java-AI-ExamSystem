<template>
  <div class="admin-feedback">
    <!-- 页头 -->
    <div class="page-header">
      <div>
        <h2 class="page-title">题目纠错反馈管理</h2>
        <span class="subtitle">查看用户提交的纠错反馈并进行处理</span>
      </div>
    </div>

    <!-- 状态过滤 Tabs -->
    <el-card class="table-card" shadow="never">
      <template #header>
        <div class="card-header">
          <el-tabs v-model="activeStatus" @tab-change="onStatusChange">
            <el-tab-pane label="全部" name="" />
            <el-tab-pane label="待处理" name="0" />
            <el-tab-pane label="已采纳" name="1" />
            <el-tab-pane label="已驳回" name="2" />
            <el-tab-pane label="已修复" name="3" />
          </el-tabs>
        </div>
      </template>

      <el-table
        :data="feedbackList"
        v-loading="loading"
        stripe
        style="width: 100%"
      >
        <el-table-column prop="id" label="ID" width="72" align="center" />
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
        <el-table-column label="描述" min-width="160" show-overflow-tooltip>
          <template #default="{ row }">
            {{ row.description || '--' }}
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
        <el-table-column label="操作" width="120" align="center" fixed="right">
          <template #default="{ row }">
            <el-button
              size="small"
              text
              type="primary"
              @click="openHandleDialog(row)"
            >
              {{ row.status === 0 ? '处理' : '重新处理' }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-empty
        v-if="!loading && feedbackList.length === 0"
        description="暂无反馈记录"
        :image-size="80"
      />

      <!-- 分页 -->
      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :total="total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next"
          background
          @current-change="loadList"
          @size-change="onSizeChange"
        />
      </div>
    </el-card>

    <!-- ===== 处理对话框 ===== -->
    <el-dialog
      v-model="handleDialogVisible"
      title="处理反馈"
      width="500px"
      :close-on-click-modal="false"
    >
      <div v-if="currentFeedback" class="handle-dialog-content">
        <!-- 反馈信息摘要 -->
        <div class="feedback-summary">
          <div class="summary-item">
            <span class="summary-label">反馈类型</span>
            <el-tag type="info" size="small">{{ currentFeedback.typeLabel }}</el-tag>
          </div>
          <div class="summary-item">
            <span class="summary-label">题目摘要</span>
            <span class="summary-value">{{ currentFeedback.questionSummary || '题目已删除' }}</span>
          </div>
          <div class="summary-item" v-if="currentFeedback.description">
            <span class="summary-label">用户描述</span>
            <span class="summary-value">{{ currentFeedback.description }}</span>
          </div>
        </div>

        <el-divider />

        <!-- 处理表单 -->
        <el-form
          ref="handleFormRef"
          :model="handleForm"
          :rules="handleRules"
          label-position="top"
        >
          <el-form-item label="处理结果" prop="status">
            <el-radio-group v-model="handleForm.status" style="flex-wrap: wrap; gap: 8px;">
              <el-radio-button :value="1">已采纳</el-radio-button>
              <el-radio-button :value="2">已驳回</el-radio-button>
              <el-radio-button :value="3">已修复</el-radio-button>
            </el-radio-group>
          </el-form-item>
          <el-form-item label="回复内容（可选）">
            <el-input
              v-model="handleForm.adminReply"
              type="textarea"
              :rows="3"
              placeholder="向用户说明处理结果..."
              maxlength="500"
              show-word-limit
            />
          </el-form-item>
        </el-form>

        <el-alert
          type="info"
          :closable="false"
          show-icon
          style="margin-top: 4px"
        >
          <template #default>
            处理后将通过站内通知告知用户，通知有效期 7 天。
          </template>
        </el-alert>
      </div>

      <template #footer>
        <el-button @click="handleDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="handling" @click="confirmHandle">
          确认处理
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import {
  getAdminFeedback,
  handleFeedback,
  FEEDBACK_STATUS_MAP,
  type FeedbackVO,
} from '@/api/feedback'

// ===== 列表状态 =====
const loading = ref(false)
const feedbackList = ref<FeedbackVO[]>([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(10)
const activeStatus = ref<string>('')

// ===== 处理对话框 =====
const handleDialogVisible = ref(false)
const handling = ref(false)
const handleFormRef = ref<FormInstance>()
const currentFeedback = ref<FeedbackVO | null>(null)
const handleForm = ref({
  status: null as number | null,
  adminReply: '',
})

const handleRules: FormRules = {
  status: [{ required: true, message: '请选择处理结果', trigger: 'change' }],
}

// ===== 生命周期 =====
onMounted(loadList)

// ===== 方法 =====

async function loadList() {
  loading.value = true
  try {
    const statusParam = activeStatus.value !== '' ? Number(activeStatus.value) : undefined
    const res = await getAdminFeedback({
      page: currentPage.value,
      size: pageSize.value,
      status: statusParam,
    })
    feedbackList.value = res.data.records
    total.value = Number(res.data.total)
  } catch {
    // 已由拦截器处理
  } finally {
    loading.value = false
  }
}

function onStatusChange() {
  currentPage.value = 1
  loadList()
}

function onSizeChange() {
  currentPage.value = 1
  loadList()
}

function openHandleDialog(row: FeedbackVO) {
  currentFeedback.value = row
  handleForm.value = { status: null, adminReply: '' }
  handleDialogVisible.value = true
}

async function confirmHandle() {
  if (!handleFormRef.value || !currentFeedback.value) return
  const valid = await handleFormRef.value.validate().catch(() => false)
  if (!valid) return

  handling.value = true
  try {
    await handleFeedback(currentFeedback.value.id, {
      status: handleForm.value.status!,
      adminReply: handleForm.value.adminReply || undefined,
    })
    ElMessage.success('处理成功，已通知用户')
    handleDialogVisible.value = false
    loadList()
  } catch {
    // 已由拦截器处理
  } finally {
    handling.value = false
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
.admin-feedback {
  padding: 0;
}

.page-header {
  display: flex;
  align-items: baseline;
  gap: 12px;
  margin-bottom: 20px;
}

.page-title {
  margin: 0;
  font-size: 20px;
  font-weight: 600;
}

.subtitle {
  color: var(--el-text-color-secondary);
  font-size: 14px;
  margin-left: 8px;
}

.table-card {
  border-radius: 8px;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

:deep(.el-tabs__header) {
  margin-bottom: 0;
}

:deep(.el-tabs__nav-wrap::after) {
  display: none;
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

/* ===== 处理弹框 ===== */
.handle-dialog-content {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.feedback-summary {
  display: flex;
  flex-direction: column;
  gap: 8px;
  background: var(--el-fill-color-light);
  border-radius: 8px;
  padding: 12px 14px;
}

.summary-item {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  font-size: 13px;
}

.summary-label {
  color: var(--el-text-color-secondary);
  min-width: 64px;
  flex-shrink: 0;
}

.summary-value {
  color: var(--el-text-color-regular);
  line-height: 1.5;
}
</style>
