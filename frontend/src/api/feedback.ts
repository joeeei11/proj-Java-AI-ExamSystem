import { get, post, put } from '@/utils/request'

// ===== 类型定义 =====

export interface FeedbackVO {
  id: number
  questionId: number
  /** 题目摘要（前 60 字） */
  questionSummary: string
  /** 反馈类型：1 答案错误 / 2 题干歧义 / 3 解析不清 / 4 排版问题 */
  type: number
  typeLabel: string
  /** 处理状态：0 待处理 / 1 已采纳 / 2 已驳回 / 3 已修复 */
  status: number
  statusLabel: string
  description: string | null
  createdAt: string
  repliedAt: string | null
}

export interface FeedbackDetailVO {
  id: number
  questionId: number
  questionContent: string
  questionAnswer: string
  type: number
  typeLabel: string
  status: number
  statusLabel: string
  description: string | null
  screenshotUrl: string | null
  adminReply: string | null
  createdAt: string
  repliedAt: string | null
}

export interface PageResult<T> {
  records: T[]
  total: number
  size: number
  current: number
  pages: number
}

export interface SubmitFeedbackRequest {
  questionId: number
  /** 反馈类型：1 答案错误 / 2 题干歧义 / 3 解析不清 / 4 排版问题 */
  type: number
  description?: string
  screenshotUrl?: string
}

export interface HandleFeedbackRequest {
  /** 处理状态：1 已采纳 / 2 已驳回 / 3 已修复 */
  status: number
  adminReply?: string
}

// ===== 常量映射 =====

export const FEEDBACK_TYPES = [
  { value: 1, label: '答案错误' },
  { value: 2, label: '题干歧义' },
  { value: 3, label: '解析不清' },
  { value: 4, label: '排版问题' },
]

export const FEEDBACK_STATUS_MAP: Record<
  number,
  { label: string; type: 'info' | 'warning' | 'success' | 'danger' | '' }
> = {
  0: { label: '待处理', type: 'warning' },
  1: { label: '已采纳', type: 'success' },
  2: { label: '已驳回', type: 'danger' },
  3: { label: '已修复', type: 'info' },
}

// ===== API 函数 =====

/** 提交题目纠错反馈 */
export function submitFeedback(data: SubmitFeedbackRequest) {
  return post<void>('/feedback/question', data)
}

/** 我的反馈列表（分页） */
export function getMyFeedback(params: { page?: number; size?: number }) {
  return get<PageResult<FeedbackVO>>('/feedback/my', params)
}

/** 我的反馈详情 */
export function getMyFeedbackDetail(id: number) {
  return get<FeedbackDetailVO>(`/feedback/my/${id}`)
}

/** 管理员：查看全部反馈 */
export function getAdminFeedback(params: { page?: number; size?: number; status?: number | null }) {
  return get<PageResult<FeedbackVO>>('/admin/feedback', params)
}

/** 管理员：处理反馈 */
export function handleFeedback(id: number, data: HandleFeedbackRequest) {
  return put<void>(`/admin/feedback/${id}`, data)
}
