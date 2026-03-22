import { del, get, post, put, upload } from '@/utils/request'

// ===== 类型定义 =====

export interface Note {
  id: number
  subjectId?: number
  subjectName?: string
  title: string
  content?: string
  imageUrl?: string
  ocrText?: string
  /** 来源：1 手动 2 OCR 3 AI整理 */
  source: 1 | 2 | 3
  createdAt: string
  updatedAt: string
}

export interface NoteCreateRequest {
  title: string
  content?: string
  subjectId?: number
}

export interface NoteUpdateRequest {
  title?: string
  content?: string
  subjectId?: number
}

// ===== API 函数 =====

/** 笔记列表（可按科目过滤） */
export function getNotes(subjectId?: number) {
  return get<Note[]>('/notes', subjectId ? { subjectId } : undefined)
}

/** 笔记详情 */
export function getNoteById(id: number) {
  return get<Note>(`/notes/${id}`)
}

/** 手动创建笔记 */
export function createNote(data: NoteCreateRequest) {
  return post<Note>('/notes', data)
}

/** 更新笔记 */
export function updateNote(id: number, data: NoteUpdateRequest) {
  return put<Note>(`/notes/${id}`, data)
}

/** 删除笔记 */
export function deleteNote(id: number) {
  return del<void>(`/notes/${id}`)
}

/**
 * 图片 OCR：上传图片并识别文字创建笔记
 * @param file      图片文件
 * @param subjectId 关联科目（可选）
 */
export function ocrNote(file: File, subjectId?: number) {
  const formData = new FormData()
  formData.append('file', file)
  if (subjectId) formData.append('subjectId', String(subjectId))
  return upload<Note>('/notes/ocr', formData)
}

/** AI 整理笔记内容 */
export function organizeNote(id: number) {
  return post<Note>(`/notes/${id}/organize`)
}
