<template>
  <div class="admin-dashboard">
    <!-- 页面标题 -->
    <div class="page-header">
      <h2>管理后台</h2>
      <span class="subtitle">系统运营数据总览</span>
    </div>

    <!-- 统计卡片 -->
    <el-row :gutter="16" class="stats-row">
      <el-col :xs="12" :sm="8" :md="4" :lg="4" v-for="card in statCards" :key="card.label">
        <el-card class="stat-card" shadow="never" :style="{ borderTop: `3px solid ${card.color}` }">
          <div class="stat-inner">
            <el-icon :size="28" :color="card.color" class="stat-icon">
              <component :is="card.icon" />
            </el-icon>
            <div class="stat-text">
              <div class="stat-value">
                <el-skeleton v-if="statsLoading" :rows="1" animated style="width: 60px" />
                <span v-else>{{ card.value?.toLocaleString() ?? '--' }}</span>
              </div>
              <div class="stat-label">{{ card.label }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 快捷入口 -->
    <el-card class="shortcut-card" shadow="never" style="margin-bottom: 20px">
      <template #header>
        <span class="card-title">快捷功能</span>
      </template>
      <div class="shortcut-list">
        <el-button type="primary" plain @click="$router.push('/admin/feedback')">
          <el-icon><Comment /></el-icon>
          题目反馈管理
        </el-button>
      </div>
    </el-card>

    <!-- 用户管理 -->
    <el-card class="table-card" shadow="never">
      <template #header>
        <div class="card-header">
          <span class="card-title">用户管理</span>
          <el-input
            v-model="keyword"
            placeholder="搜索用户名 / 昵称"
            prefix-icon="Search"
            clearable
            style="width: 240px"
            @input="onSearch"
            @clear="onSearch"
          />
        </div>
      </template>

      <el-table
        :data="userList"
        v-loading="tableLoading"
        stripe
        style="width: 100%"
        row-key="id"
      >
        <el-table-column prop="id" label="ID" width="72" align="center" />
        <el-table-column label="用户名" min-width="120">
          <template #default="{ row }">
            <div class="user-cell">
              <el-avatar :size="28" style="flex-shrink: 0">
                {{ row.nickname?.charAt(0) || row.username?.charAt(0) }}
              </el-avatar>
              <div class="user-info">
                <div class="username">{{ row.username }}</div>
                <div class="nickname">{{ row.nickname }}</div>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="email" label="邮箱" min-width="160" show-overflow-tooltip />
        <el-table-column label="角色" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="row.role === 1 ? 'danger' : 'info'" size="small">
              {{ row.role === 1 ? '管理员' : '普通用户' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="row.isActive === 1 ? 'success' : 'warning'" size="small">
              {{ row.isActive === 1 ? '正常' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="studyStreak" label="连续打卡" width="90" align="center">
          <template #default="{ row }">
            <span>{{ row.studyStreak ?? 0 }} 天</span>
          </template>
        </el-table-column>
        <el-table-column label="最后登录" width="160" align="center">
          <template #default="{ row }">
            {{ row.lastLoginAt ? formatDate(row.lastLoginAt) : '从未登录' }}
          </template>
        </el-table-column>
        <el-table-column label="注册时间" width="160" align="center">
          <template #default="{ row }">
            {{ formatDate(row.createdAt) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="110" align="center" fixed="right">
          <template #default="{ row }">
            <el-popconfirm
              :title="`确认${row.isActive === 1 ? '禁用' : '启用'}该用户？`"
              :confirm-button-text="row.isActive === 1 ? '禁用' : '启用'"
              :confirm-button-type="row.isActive === 1 ? 'danger' : 'primary'"
              cancel-button-text="取消"
              @confirm="toggleStatus(row)"
            >
              <template #reference>
                <el-button
                  :type="row.isActive === 1 ? 'danger' : 'success'"
                  size="small"
                  text
                  :disabled="row.role === 1"
                >
                  {{ row.isActive === 1 ? '禁用' : '启用' }}
                </el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :total="total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next"
          background
          @current-change="loadUsers"
          @size-change="onSizeChange"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { User, Document, ChatDotRound, Notebook, Timer, Comment } from '@element-plus/icons-vue'
import {
  getAdminUsers,
  updateUserStatus,
  getSystemStats,
  type UserAdminVO,
  type SystemStatsVO,
} from '@/api/admin'

// ===== 系统统计 =====
const statsLoading = ref(false)
const stats = ref<SystemStatsVO | null>(null)

const statCards = computed(() => [
  { label: '注册用户数', value: stats.value?.totalUsers, icon: User, color: '#409EFF' },
  { label: '题目总数', value: stats.value?.totalQuestions, icon: Document, color: '#67C23A' },
  { label: '答题总数', value: stats.value?.totalAnswers, icon: ChatDotRound, color: '#E6A23C' },
  { label: '笔记总数', value: stats.value?.totalNotes, icon: Notebook, color: '#909399' },
  { label: '今日活跃', value: stats.value?.todayActiveUsers, icon: Timer, color: '#F56C6C' },
])

async function loadStats() {
  statsLoading.value = true
  try {
    const res = await getSystemStats()
    stats.value = res.data
  } catch {
    // 错误已由 request.ts 拦截器统一提示
  } finally {
    statsLoading.value = false
  }
}

// ===== 用户列表 =====
const tableLoading = ref(false)
const userList = ref<UserAdminVO[]>([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(10)
const keyword = ref('')

let searchTimer: ReturnType<typeof setTimeout> | null = null

function onSearch() {
  if (searchTimer) clearTimeout(searchTimer)
  searchTimer = setTimeout(() => {
    currentPage.value = 1
    loadUsers()
  }, 400)
}

function onSizeChange() {
  currentPage.value = 1
  loadUsers()
}

async function loadUsers() {
  tableLoading.value = true
  try {
    const res = await getAdminUsers({
      page: currentPage.value,
      size: pageSize.value,
      keyword: keyword.value || undefined,
    })
    userList.value = res.data.records
    total.value = Number(res.data.total)
  } catch {
    // 已由拦截器处理
  } finally {
    tableLoading.value = false
  }
}

async function toggleStatus(row: UserAdminVO) {
  try {
    await updateUserStatus(row.id, row.isActive !== 1)
    ElMessage.success(`已${row.isActive === 1 ? '禁用' : '启用'}用户 ${row.username}`)
    loadUsers()
    loadStats()
  } catch {
    // 已由拦截器处理
  }
}

// ===== 工具函数 =====
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

onMounted(() => {
  loadStats()
  loadUsers()
})
</script>

<style scoped>
.admin-dashboard {
  padding: 24px;
  max-width: 1400px;
}

.page-header {
  display: flex;
  align-items: baseline;
  gap: 12px;
  margin-bottom: 20px;
}

.page-header h2 {
  margin: 0;
  font-size: 22px;
  font-weight: 600;
}

.subtitle {
  color: var(--el-text-color-secondary);
  font-size: 14px;
}

/* 统计卡片 */
.stats-row {
  margin-bottom: 20px;
}

.stat-card {
  border-radius: 8px;
}

.stat-inner {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 4px 0;
}

.stat-icon {
  flex-shrink: 0;
}

.stat-text {
  flex: 1;
  min-width: 0;
}

.stat-value {
  font-size: 22px;
  font-weight: 700;
  line-height: 1.2;
  color: var(--el-text-color-primary);
}

.stat-label {
  font-size: 12px;
  color: var(--el-text-color-secondary);
  margin-top: 4px;
}

/* 用户表格 */
.table-card {
  border-radius: 8px;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.card-title {
  font-size: 15px;
  font-weight: 600;
}

.user-cell {
  display: flex;
  align-items: center;
  gap: 8px;
}

.user-info {
  min-width: 0;
}

.username {
  font-size: 14px;
  font-weight: 500;
  line-height: 1.3;
}

.nickname {
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}

.shortcut-list {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}
</style>
