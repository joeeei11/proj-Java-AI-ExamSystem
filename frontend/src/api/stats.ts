import { get } from '@/utils/request'

// ===== 类型定义 =====

export interface StatsOverview {
  /** 累计答题数 */
  totalAnswered: number
  /** 累计正确数 */
  totalCorrect: number
  /** 正确率（如 82.5 表示 82.5%） */
  accuracy: number
  /** 学习总天数 */
  studyDays: number
  /** 当前连续打卡天数 */
  studyStreak: number
  /** 错题总数 */
  totalErrors: number
  /** 已掌握错题数 */
  masteredErrors: number
}

export interface DailyStat {
  /** 日期，yyyy-MM-dd */
  date: string
  totalAnswered: number
  correctCount: number
  accuracy: number
}

export interface SubjectStat {
  subjectId: number
  subjectName: string
  totalAnswered: number
  correctCount: number
  accuracy: number
}

// ===== API 函数 =====

/** 学习总览数据 */
export function getStatsOverview() {
  return get<StatsOverview>('/stats/overview')
}

/** 近 N 天日度答题趋势（默认 30 天） */
export function getDailyStats(days = 30) {
  return get<DailyStat[]>('/stats/daily', { days })
}

/** 科目维度答题统计 */
export function getSubjectStats() {
  return get<SubjectStat[]>('/stats/subjects')
}
