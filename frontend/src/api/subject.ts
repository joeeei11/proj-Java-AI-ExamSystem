import { get } from '@/utils/request'

export interface Subject {
  id: number
  name: string
  description?: string
  sortOrder: number
  isActive: number
}

export interface KnowledgePoint {
  id: number
  subjectId: number
  name: string
  description?: string
  sortOrder: number
}

/**
 * 获取所有启用科目
 */
export function getSubjects() {
  return get<Subject[]>('/subjects')
}

/**
 * 获取科目下的知识点
 */
export function getKnowledgePoints(subjectId: number) {
  return get<KnowledgePoint[]>(`/subjects/${subjectId}/knowledge-points`)
}
