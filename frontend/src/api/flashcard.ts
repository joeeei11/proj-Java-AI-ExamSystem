import { del, get, post, put } from '@/utils/request'

// ===== 类型定义 =====

export interface FlashcardDeckVO {
  id: number
  noteId?: number
  title: string
  cardCount: number
  todayReviewCount: number
  createdAt: string
}

export interface FlashcardVO {
  id: number
  deckId: number
  front: string
  back: string
  sortOrder: number
  reviewCount: number
  consecutiveCorrect: number
  reviewIntervalDays: number
  nextReviewAt?: string
  createdAt: string
}

export interface DeckDetailResult {
  deck: FlashcardDeckVO
  cards: FlashcardVO[]
}

export interface GenerateFlashcardRequest {
  noteId: number
}

export interface UpdateCardRequest {
  front: string
  back: string
}

export interface FlashcardReviewRequest {
  known: boolean
}

// ===== API 函数 =====

/** AI 生成抽认卡（基于笔记），返回 deckId */
export function generateFlashcards(data: GenerateFlashcardRequest) {
  return post<{ deckId: number }>('/flashcards/generate', data)
}

/** 卡组列表（含今日待复习数量） */
export function listDecks() {
  return get<FlashcardDeckVO[]>('/flashcards/decks')
}

/** 卡组详情 + 所有卡片 */
export function getDeck(id: number) {
  return get<DeckDetailResult>(`/flashcards/decks/${id}`)
}

/** 删除卡组 */
export function deleteDeck(id: number) {
  return del<void>(`/flashcards/decks/${id}`)
}

/** 编辑单张卡片 */
export function updateCard(cardId: number, data: UpdateCardRequest) {
  return put<void>(`/flashcards/${cardId}`, data)
}

/** 今日待复习卡片（SRS 到期） */
export function getTodayReview() {
  return get<FlashcardVO[]>('/flashcards/today-review')
}

/** 提交复习结果（known: 会了/不会） */
export function submitReview(cardId: number, data: FlashcardReviewRequest) {
  return post<void>(`/flashcards/${cardId}/review`, data)
}
