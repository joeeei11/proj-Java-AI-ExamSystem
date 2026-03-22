import { get, put } from '@/utils/request'

// ===== 类型定义 =====

export interface UserAdminVO {
  id: number
  username: string
  nickname: string
  email: string
  /** 0 普通用户，1 管理员 */
  role: number
  /** 0 禁用，1 正常 */
  isActive: number
  studyStreak: number
  lastLoginAt: string | null
  createdAt: string
}

export interface PageResult<T> {
  records: T[]
  total: number
  size: number
  current: number
  pages: number
}

export interface SystemStatsVO {
  totalUsers: number
  totalQuestions: number
  totalAnswers: number
  totalNotes: number
  todayActiveUsers: number
}

// ===== API 函数 =====

/** 分页查询用户列表 */
export function getAdminUsers(params: { page?: number; size?: number; keyword?: string }) {
  return get<PageResult<UserAdminVO>>('/admin/users', params)
}

/** 启用 / 禁用用户 */
export function updateUserStatus(userId: number, isActive: boolean) {
  return put<void>(`/admin/users/${userId}/status`, { isActive })
}

/** 获取系统级统计总览 */
export function getSystemStats() {
  return get<SystemStatsVO>('/admin/stats')
}
