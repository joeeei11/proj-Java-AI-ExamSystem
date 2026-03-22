import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const routes: RouteRecordRaw[] = [
  // ===== 公开路由 =====
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/auth/Login.vue'),
    meta: { public: true, title: '登录' },
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('@/views/auth/Register.vue'),
    meta: { public: true, title: '注册' },
  },

  // ===== 需要认证的路由（使用主布局） =====
  {
    path: '/',
    component: () => import('@/components/layout/AppLayout.vue'),
    redirect: '/dashboard',
    children: [
      {
        path: 'profile',
        name: 'Profile',
        component: () => import('@/views/user/ProfileView.vue'),
        meta: { title: '个人中心' },
      },
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/dashboard/Dashboard.vue'),
        meta: { title: '学习看板' },
      },
      {
        path: 'quiz',
        name: 'Quiz',
        component: () => import('@/views/quiz/QuizHome.vue'),
        meta: { title: '智能刷题' },
      },
      {
        path: 'notes',
        name: 'Notes',
        component: () => import('@/views/notes/NoteList.vue'),
        meta: { title: '我的笔记' },
      },
      {
        path: 'notes/:id',
        name: 'NoteDetail',
        component: () => import('@/views/notes/NoteDetail.vue'),
        meta: { title: '笔记详情' },
      },
      {
        path: 'review',
        name: 'Review',
        component: () => import('@/views/review/ReviewCenter.vue'),
        meta: { title: '复习中心' },
      },
      {
        path: 'errorbook',
        name: 'ErrorBook',
        component: () => import('@/views/errorbook/ErrorBook.vue'),
        meta: { title: '错题本' },
      },
      {
        path: 'mock',
        name: 'MockHome',
        component: () => import('@/views/mock/MockHome.vue'),
        meta: { title: '模考模式' },
      },
      {
        path: 'mock/answer/:examId',
        name: 'MockAnswer',
        component: () => import('@/views/mock/MockAnswer.vue'),
        meta: { title: '模考答题' },
      },
      {
        path: 'mock/report/:examId',
        name: 'MockReport',
        component: () => import('@/views/mock/MockReport.vue'),
        meta: { title: '模考报告' },
      },
      {
        path: 'flashcards',
        name: 'FlashCards',
        component: () => import('@/views/flashcards/DeckList.vue'),
        meta: { title: '抽认卡' },
      },
      {
        path: 'flashcards/decks/:id',
        name: 'CardStudy',
        component: () => import('@/views/flashcards/CardStudy.vue'),
        meta: { title: '卡组学习' },
      },
      {
        path: 'flashcards/daily-review',
        name: 'DailyReview',
        component: () => import('@/views/flashcards/DailyReview.vue'),
        meta: { title: '今日复习' },
      },
      {
        path: 'feedback',
        name: 'Feedback',
        component: () => import('@/views/feedback/MyFeedback.vue'),
        meta: { title: '我的反馈' },
      },
      {
        path: 'stats',
        name: 'Stats',
        component: () => import('@/views/stats/StatsDashboard.vue'),
        meta: { title: '学习统计' },
      },
      // 管理员路由
      {
        path: 'admin',
        name: 'Admin',
        component: () => import('@/views/admin/AdminDashboard.vue'),
        meta: { title: '管理后台', requiresAdmin: true },
      },
      {
        path: 'admin/feedback',
        name: 'AdminFeedback',
        component: () => import('@/views/admin/AdminFeedback.vue'),
        meta: { title: '反馈管理', requiresAdmin: true },
      },
    ],
  },

  // 404
  {
    path: '/:pathMatch(.*)*',
    redirect: '/dashboard',
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior: () => ({ top: 0 }),
})

// ===== 全局路由守卫 =====
router.beforeEach((to, _from, next) => {
  // 更新页面标题
  document.title = `${to.meta.title || '公考AI刷题'} - hnust-exam-ai`

  const authStore = useAuthStore()

  // 公开路由：已登录则跳转首页
  if (to.meta.public) {
    if (authStore.isLoggedIn) {
      next('/dashboard')
    } else {
      next()
    }
    return
  }

  // 未登录：跳转登录页
  if (!authStore.isLoggedIn) {
    next({ path: '/login', query: { redirect: to.fullPath } })
    return
  }

  // 管理员路由：非管理员拒绝
  if (to.meta.requiresAdmin && !authStore.isAdmin) {
    next('/dashboard')
    return
  }

  next()
})

export default router
