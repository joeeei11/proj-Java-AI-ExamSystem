<template>
  <el-container class="app-layout">
    <!-- 侧边栏 -->
    <el-aside :width="isCollapsed ? '64px' : '220px'" class="sidebar">
      <!-- Logo 区域 -->
      <div class="logo-wrap">
        <el-icon class="logo-icon" size="24"><DataBoard /></el-icon>
        <span v-if="!isCollapsed" class="logo-text">公考 AI 刷题</span>
      </div>

      <!-- 导航菜单 -->
      <el-menu
        :default-active="activeMenu"
        :collapse="isCollapsed"
        :collapse-transition="false"
        router
        background-color="#1d2b3a"
        text-color="#b0bec5"
        active-text-color="#ffffff"
        class="sidebar-menu"
      >
        <el-menu-item index="/dashboard">
          <el-icon><House /></el-icon>
          <template #title>学习看板</template>
        </el-menu-item>

        <el-menu-item index="/quiz">
          <el-icon><EditPen /></el-icon>
          <template #title>智能刷题</template>
        </el-menu-item>

        <el-menu-item index="/notes">
          <el-icon><Notebook /></el-icon>
          <template #title>我的笔记</template>
        </el-menu-item>

        <el-menu-item index="/review">
          <el-icon><Reading /></el-icon>
          <template #title>复习中心</template>
        </el-menu-item>

        <el-menu-item index="/errorbook">
          <el-icon><WarnTriangleFilled /></el-icon>
          <template #title>错题本</template>
        </el-menu-item>

        <el-menu-item index="/mock">
          <el-icon><Timer /></el-icon>
          <template #title>模考模式</template>
        </el-menu-item>

        <el-menu-item index="/flashcards">
          <el-icon><Collection /></el-icon>
          <template #title>抽认卡</template>
        </el-menu-item>

        <el-menu-item index="/stats">
          <el-icon><TrendCharts /></el-icon>
          <template #title>学习统计</template>
        </el-menu-item>

        <el-menu-item index="/feedback">
          <el-icon><ChatDotSquare /></el-icon>
          <template #title>题目反馈</template>
        </el-menu-item>

        <!-- 管理员菜单 -->
        <el-menu-item v-if="authStore.isAdmin" index="/admin">
          <el-icon><Setting /></el-icon>
          <template #title>管理后台</template>
        </el-menu-item>
      </el-menu>

      <!-- 折叠按钮 -->
      <div class="collapse-btn" @click="isCollapsed = !isCollapsed">
        <el-icon>
          <component :is="isCollapsed ? 'Expand' : 'Fold'" />
        </el-icon>
      </div>
    </el-aside>

    <!-- 主内容区 -->
    <el-container direction="vertical" class="main-container">
      <!-- 顶栏 -->
      <el-header class="header">
        <div class="header-left">
          <span class="page-title">{{ currentTitle }}</span>
        </div>
        <div class="header-right">
          <!-- 用户下拉菜单 -->
          <el-dropdown trigger="click">
            <div class="user-info">
              <el-avatar :size="32" :src="authStore.userInfo?.avatarUrl">
                {{ authStore.displayName.charAt(0) }}
              </el-avatar>
              <span class="username">{{ authStore.displayName }}</span>
              <el-icon><ArrowDown /></el-icon>
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item @click="router.push('/profile')">
                  <el-icon><User /></el-icon> 个人资料
                </el-dropdown-item>
                <el-dropdown-item divided @click="handleLogout">
                  <el-icon><SwitchButton /></el-icon> 退出登录
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>

      <!-- 页面内容 -->
      <el-main class="main-content">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessageBox, ElMessage } from 'element-plus'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()

const isCollapsed = ref(false)

/** 当前激活的菜单项 */
const activeMenu = computed(() => '/' + route.path.split('/')[1])

/** 当前页面标题 */
const currentTitle = computed(() => (route.meta.title as string) || '公考AI刷题')

/** 退出登录 */
async function handleLogout() {
  await ElMessageBox.confirm('确认退出登录？', '提示', {
    confirmButtonText: '确认退出',
    cancelButtonText: '取消',
    type: 'warning',
  })

  await authStore.logout()
  ElMessage.success('已退出登录')
  router.push('/login')
}
</script>

<style scoped>
.app-layout {
  height: 100vh;
  overflow: hidden;
}

/* ===== 侧边栏 ===== */
.sidebar {
  background-color: #1d2b3a;
  display: flex;
  flex-direction: column;
  transition: width 0.2s;
  overflow: hidden;
}

.logo-wrap {
  height: 60px;
  display: flex;
  align-items: center;
  padding: 0 20px;
  gap: 10px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
  overflow: hidden;
  white-space: nowrap;
}

.logo-icon {
  color: #409eff;
  flex-shrink: 0;
}

.logo-text {
  color: #ffffff;
  font-size: 16px;
  font-weight: 600;
}

.sidebar-menu {
  flex: 1;
  border-right: none;
  overflow-y: auto;
  overflow-x: hidden;
}

.sidebar-menu :deep(.el-menu-item.is-active) {
  background-color: #409eff !important;
  border-radius: 6px;
  margin: 2px 8px;
}

.sidebar-menu :deep(.el-menu-item) {
  height: 46px;
  line-height: 46px;
  border-radius: 6px;
  margin: 2px 8px;
}

.sidebar-menu :deep(.el-menu-item:hover) {
  background-color: rgba(255, 255, 255, 0.08) !important;
}

.collapse-btn {
  height: 48px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #b0bec5;
  cursor: pointer;
  border-top: 1px solid rgba(255, 255, 255, 0.08);
  transition: color 0.2s;
}

.collapse-btn:hover {
  color: #ffffff;
}

/* ===== 顶栏 ===== */
.header {
  height: 60px;
  background: #ffffff;
  border-bottom: 1px solid #ebeef5;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
  box-shadow: 0 1px 4px rgba(0, 21, 41, 0.08);
}

.page-title {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 16px;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  padding: 4px 8px;
  border-radius: 6px;
  transition: background 0.2s;
}

.user-info:hover {
  background: #f5f7fa;
}

.username {
  font-size: 14px;
  color: #303133;
}

/* ===== 主内容 ===== */
.main-container {
  overflow: hidden;
}

.main-content {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
  background: #f5f7fa;
}
</style>
