import { post } from '@/utils/request'

// ===== 类型定义 =====

export interface QuestionOption {
  key: string
  value: string
}

export interface Question {
  id: number
  subjectId: number
  knowledgePointId?: number
  type: number // 1单选 2多选 3判断 4填空
  difficulty: number // 1-3
  content: string
  options: QuestionOption[]
}

export interface GenerateRequest {
  subjectId: number
  difficulty?: number
  count?: number
  knowledgePointIds?: number[]
}

export interface AnswerItem {
  questionId: number
  answer: string
  timeSpentS?: number
}

export interface SubmitRequest {
  sessionId: string
  answers: AnswerItem[]
}

export interface QuestionResult {
  questionId: number
  userAnswer: string
  correctAnswer: string
  correct: boolean
  explanation: string
  type: number
  content: string
  options: QuestionOption[]
}

export interface SubmitResult {
  totalCount: number
  correctCount: number
  accuracy: number
  results: QuestionResult[]
}

// ===== API 函数 =====

/**
 * AI 出题
 */
export function generateQuestions(data: GenerateRequest) {
  return post<Question[]>('/quiz/generate', data)
}

/**
 * 提交答案并获取判分结果
 */
export function submitAnswers(data: SubmitRequest) {
  return post<SubmitResult>('/quiz/submit', data)
}
