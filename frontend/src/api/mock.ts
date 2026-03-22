import { get, post, put } from '@/utils/request'

// ===== 类型定义 =====

export interface MockStartRequest {
  subjectId: number
  count: number
  timeLimitMinutes: number
  difficulty?: number
}

export interface MockOptionItem {
  key: string
  value: string
}

export interface MockQuestionItem {
  questionId: number
  sortOrder: number
  flagged: boolean
  userAnswer: string | null
  content: string
  type: number
  difficulty: number
  options: MockOptionItem[]
}

export interface MockExamVO {
  examId: number
  title: string
  totalQuestions: number
  remainingSeconds: number
  status: number // 0:进行中 1:已完成 2:超时
  questions: MockQuestionItem[]
}

export interface MockReportQuestionItem {
  questionId: number
  sortOrder: number
  content: string
  type: number
  options: MockOptionItem[]
  userAnswer: string | null
  correctAnswer: string
  correct: boolean
  explanation: string
}

export interface MockReportVO {
  examId: number
  title: string
  totalQuestions: number
  totalCorrect: number
  accuracy: number
  durationSeconds: number
  status: number
  questions: MockReportQuestionItem[]
}

export interface MockHistoryVO {
  examId: number
  title: string
  totalQuestions: number
  totalCorrect: number
  accuracy: number
  timeLimitMin: number
  status: number
  startedAt: string
  submittedAt: string | null
}

// ===== API 函数 =====

/** 开始模考，返回 examId */
export function startMock(data: MockStartRequest) {
  return post<{ examId: number }>('/mock/start', data)
}

/** 获取进行中模考（题目不含答案） */
export function getMockExam(examId: number) {
  return get<MockExamVO>(`/mock/${examId}`)
}

/** 标记/取消标记题目 */
export function flagQuestion(examId: number, questionId: number, flagged: boolean) {
  return put<void>(`/mock/${examId}/flag`, { questionId, flagged })
}

/** 交卷 */
export function submitMock(
  examId: number,
  answers: Array<{ questionId: number; answer: string | null }>,
) {
  return post<MockReportVO>(`/mock/${examId}/submit`, { answers })
}

/** 获取模考报告 */
export function getMockReport(examId: number) {
  return get<MockReportVO>(`/mock/${examId}/report`)
}

/** 历史模考列表 */
export function getMockHistory(page = 1, size = 10) {
  return get<MockHistoryVO[]>('/mock/history', { page, size })
}
