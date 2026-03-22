import { del, get, post } from '@/utils/request'

// ===== 类型定义 =====

export interface ReviewOutline {
  id: number
  title: string
  content: string
  noteIds: number[]
  createdAt: string
  updatedAt: string
}

export interface GenerateOutlineRequest {
  noteIds: number[]
}

// ===== API 函数 =====

/** 生成复习提纲（基于笔记调用 AI） */
export function generateOutline(data: GenerateOutlineRequest) {
  return post<ReviewOutline>('/review/outline', data)
}

/** 提纲列表 */
export function getOutlines() {
  return get<ReviewOutline[]>('/review/outlines')
}

/** 删除提纲 */
export function deleteOutline(id: number) {
  return del<void>(`/review/outlines/${id}`)
}
