import { get, post } from '@/utils/request'

export interface RegisterParams {
  username: string
  password: string
  nickname?: string
}

export interface LoginParams {
  username: string
  password: string
}

export interface LoginResult {
  token: string
  userInfo: {
    id: number
    username: string
    nickname: string
    avatarUrl?: string
    role: 0 | 1
    studyStreak: number
  }
}

/**
 * 注册
 */
export function register(params: RegisterParams) {
  return post<void>('/auth/register', params)
}

/**
 * 登录
 */
export function login(params: LoginParams) {
  return post<LoginResult>('/auth/login', params)
}

/**
 * 注销
 */
export function logout() {
  return post<void>('/auth/logout')
}
