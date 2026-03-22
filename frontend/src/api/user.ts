import { get, put, upload, post } from '@/utils/request'

export interface UserProfile {
  id: number
  username: string
  nickname: string
  email?: string
  avatarUrl?: string
  role: 0 | 1
  studyStreak: number
  lastLoginAt?: string
  createdAt?: string
}

export interface UpdateProfileParams {
  nickname?: string
  email?: string
}

export interface ChangePasswordParams {
  oldPassword: string
  newPassword: string
}

/**
 * 获取个人资料
 */
export function getProfile() {
  return get<UserProfile>('/user/profile')
}

/**
 * 更新昵称/邮箱
 */
export function updateProfile(params: UpdateProfileParams) {
  return put<UserProfile>('/user/profile', params)
}

/**
 * 修改密码
 */
export function changePassword(params: ChangePasswordParams) {
  return put<void>('/auth/password', params)
}

/**
 * 上传头像
 */
export function uploadAvatar(file: File) {
  const form = new FormData()
  form.append('file', file)
  return upload<{ avatarUrl: string }>('/user/avatar', form)
}

/**
 * 每日打卡
 */
export function checkIn() {
  return post<{ studyStreak: number }>('/user/check-in')
}
