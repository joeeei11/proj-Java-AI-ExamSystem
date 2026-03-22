import { del, get, post, put } from '@/utils/request'

// ===== 类型定义 =====

/** 错因标签枚举 */
export type ErrorReason = 1 | 2 | 3 | 4 | 5 | 6

/** 错因标签文本映射 */
export const ERROR_REASON_MAP: Record<ErrorReason, string> = {
  1: '概念不清',
  2: '审题失误',
  3: '计算失误',
  4: '方法不会',
  5: '时间不足',
  6: '粗心',
}

/** 错因标签颜色映射（Element Plus tag type） */
export const ERROR_REASON_COLOR: Record<ErrorReason, string> = {
  1: 'danger',
  2: 'warning',
  3: 'warning',
  4: 'danger',
  5: 'info',
  6: 'info',
}

export interface ErrorBookItem {
  id: number
  questionId: number
  /** 题型：1 单选 2 多选 3 判断 4 填空 */
  questionType: 1 | 2 | 3 | 4
  difficulty: 1 | 2 | 3
  questionContent: string
  options?: string        // JSON 字符串
  answer: string
  explanation?: string
  subjectId?: number
  subjectName?: string
  errorReason?: ErrorReason
  /** 0 未掌握 1 已掌握 */
  isMastered: 0 | 1
  reviewCount: number
  consecutiveCorrect: number
  reviewIntervalDays: number
  nextReviewAt?: string
  createdAt: string
  updatedAt: string
}

export interface SetReasonRequest {
  reason: ErrorReason
}

export interface ReviewRequest {
  known: boolean
}

// ===== API 函数 =====

/** 错题列表（可按科目/掌握状态过滤） */
export function getErrors(params?: { subjectId?: number; isMastered?: 0 | 1 }) {
  return get<ErrorBookItem[]>('/errors', params)
}

/** 设置错因标签 */
export function setErrorReason(id: number, data: SetReasonRequest) {
  return put<void>(`/errors/${id}/reason`, data)
}

/** 今日待复做列表（SRS 到期） */
export function getTodayReview() {
  return get<ErrorBookItem[]>('/errors/today-review')
}

/** 提交复做结果 */
export function submitReview(id: number, data: ReviewRequest) {
  return post<void>(`/errors/${id}/review`, data)
}
