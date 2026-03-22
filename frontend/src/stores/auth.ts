import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { login as apiLogin, logout as apiLogout } from '@/api/auth'
import { getProfile } from '@/api/user'

/** 用户信息结构 */
export interface UserInfo {
  id: number
  username: string
  nickname: string
  avatarUrl?: string
  role: 0 | 1
  studyStreak?: number
}

const TOKEN_KEY = 'token'
const USER_INFO_KEY = 'userInfo'

export const useAuthStore = defineStore('auth', () => {
  // ===== 状态 =====
  const token = ref<string | null>(localStorage.getItem(TOKEN_KEY))
  const userInfo = ref<UserInfo | null>(
    (() => {
      try {
        const raw = localStorage.getItem(USER_INFO_KEY)
        return raw ? (JSON.parse(raw) as UserInfo) : null
      } catch {
        return null
      }
    })(),
  )

  // ===== Getters =====
  const isLoggedIn = computed(() => !!token.value)
  const isAdmin = computed(() => userInfo.value?.role === 1)
  const displayName = computed(
    () => userInfo.value?.nickname || userInfo.value?.username || '用户',
  )

  // ===== Actions =====

  /** 登录（调用 API，成功后保存 token 和用户信息） */
  async function login(username: string, password: string) {
    const res = await apiLogin({ username, password })
    setAuth(res.data.token, res.data.userInfo)
    return res.data
  }

  /** 注销（调用 API，清理本地状态） */
  async function logout() {
    try {
      await apiLogout()
    } catch {
      // 忽略 API 失败，本地清理即可
    } finally {
      clearAuth()
    }
  }

  /** 拉取最新用户信息（从服务端刷新） */
  async function fetchProfile() {
    const res = await getProfile()
    const info: UserInfo = {
      id: res.data.id,
      username: res.data.username,
      nickname: res.data.nickname,
      avatarUrl: res.data.avatarUrl,
      role: res.data.role,
      studyStreak: res.data.studyStreak,
    }
    userInfo.value = info
    localStorage.setItem(USER_INFO_KEY, JSON.stringify(info))
    return info
  }

  /** 登录成功后调用（同步 token 和用户信息到本地） */
  function setAuth(newToken: string, info: UserInfo) {
    token.value = newToken
    userInfo.value = info
    localStorage.setItem(TOKEN_KEY, newToken)
    localStorage.setItem(USER_INFO_KEY, JSON.stringify(info))
  }

  /** 清理本地认证状态 */
  function clearAuth() {
    token.value = null
    userInfo.value = null
    localStorage.removeItem(TOKEN_KEY)
    localStorage.removeItem(USER_INFO_KEY)
  }

  /** 更新用户信息（头像、昵称等局部更新） */
  function updateUserInfo(partial: Partial<UserInfo>) {
    if (userInfo.value) {
      userInfo.value = { ...userInfo.value, ...partial }
      localStorage.setItem(USER_INFO_KEY, JSON.stringify(userInfo.value))
    }
  }

  return {
    token,
    userInfo,
    isLoggedIn,
    isAdmin,
    displayName,
    login,
    logout,
    fetchProfile,
    setAuth,
    clearAuth,
    updateUserInfo,
  }
})
