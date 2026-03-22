<template>
  <div class="stats-dashboard">
    <!-- 页头 -->
    <div class="page-header">
      <h2 class="page-title">学习统计</h2>
      <span class="page-subtitle">全面掌握你的学习轨迹</span>
    </div>

    <!-- 概览卡片组 -->
    <el-row :gutter="16" class="overview-cards" v-loading="overviewLoading">
      <el-col
        v-for="card in overviewCards"
        :key="card.key"
        :xs="12" :sm="8" :md="8" :lg="4" :xl="4"
      >
        <div class="stat-card" :style="{ '--accent': card.color, '--accent-bg': card.bg }">
          <div class="stat-icon-wrap">
            <el-icon :size="22"><component :is="card.icon" /></el-icon>
          </div>
          <div class="stat-body">
            <div class="stat-value">{{ card.value }}</div>
            <div class="stat-label">{{ card.label }}</div>
          </div>
        </div>
      </el-col>
    </el-row>

    <!-- 答题趋势折线图（全宽） -->
    <el-card shadow="never" class="chart-card">
      <template #header>
        <div class="chart-card-header">
          <span class="chart-title">答题趋势</span>
          <el-radio-group v-model="trendDays" size="small" @change="loadDailyStats">
            <el-radio-button :value="7">7天</el-radio-button>
            <el-radio-button :value="30">30天</el-radio-button>
          </el-radio-group>
        </div>
      </template>
      <div ref="trendChartRef" class="chart-box" v-loading="trendLoading" />
    </el-card>

    <!-- 科目柱状图 + 错因饼图 -->
    <el-row :gutter="16" class="bottom-charts">
      <el-col :xs="24" :md="14">
        <el-card shadow="never" class="chart-card">
          <template #header>
            <span class="chart-title">各科目正确率</span>
          </template>
          <div
            ref="subjectChartRef"
            class="chart-box"
            :style="{ height: subjectChartHeight }"
            v-loading="subjectLoading"
          />
        </el-card>
      </el-col>

      <el-col :xs="24" :md="10">
        <el-card shadow="never" class="chart-card">
          <template #header>
            <span class="chart-title">错题错因分布</span>
          </template>
          <div ref="reasonChartRef" class="chart-box" v-loading="errorLoading" />
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import * as echarts from 'echarts'
import type { ECharts } from 'echarts'
import {
  Calendar,
  DataLine,
  Finished,
  Medal,
  TrendCharts,
} from '@element-plus/icons-vue'
import { getDailyStats, getStatsOverview, getSubjectStats } from '@/api/stats'
import { getErrors } from '@/api/errorbook'
import { ERROR_REASON_MAP } from '@/api/errorbook'
import type { StatsOverview, DailyStat, SubjectStat } from '@/api/stats'
import type { ErrorBookItem } from '@/api/errorbook'

// ===== 数据状态 =====
const overviewLoading = ref(false)
const trendLoading    = ref(false)
const subjectLoading  = ref(false)
const errorLoading    = ref(false)

const overview   = ref<StatsOverview | null>(null)
const dailyStats = ref<DailyStat[]>([])
const subjectStats = ref<SubjectStat[]>([])
const errors     = ref<ErrorBookItem[]>([])

const trendDays = ref<7 | 30>(30)

// ===== ECharts DOM 引用 =====
const trendChartRef   = ref<HTMLElement>()
const subjectChartRef = ref<HTMLElement>()
const reasonChartRef  = ref<HTMLElement>()

let trendChart:   ECharts | null = null
let subjectChart: ECharts | null = null
let reasonChart:  ECharts | null = null

// ===== 概览卡片配置 =====
const overviewCards = computed(() => {
  const ov = overview.value
  return [
    {
      key: 'total',
      label: '累计答题',
      value: ov ? ov.totalAnswered.toLocaleString() : '--',
      icon: DataLine,
      color: '#409eff',
      bg: '#ecf5ff',
    },
    {
      key: 'accuracy',
      label: '正确率',
      value: ov ? `${ov.accuracy}%` : '--',
      icon: TrendCharts,
      color: '#67c23a',
      bg: '#f0f9eb',
    },
    {
      key: 'days',
      label: '学习天数',
      value: ov ? `${ov.studyDays} 天` : '--',
      icon: Calendar,
      color: '#e6a23c',
      bg: '#fdf6ec',
    },
    {
      key: 'streak',
      label: '连续打卡',
      value: ov ? `${ov.studyStreak} 天` : '--',
      icon: Medal,
      color: '#f56c6c',
      bg: '#fef0f0',
    },
    {
      key: 'mastered',
      label: '错题掌握率',
      value: ov && ov.totalErrors > 0
        ? `${Math.round(ov.masteredErrors / ov.totalErrors * 100)}%`
        : '--',
      icon: Finished,
      color: '#909399',
      bg: '#f4f4f5',
    },
  ]
})

// 科目图表高度随数据量动态调整（最少 200px，每条 40px）
const subjectChartHeight = computed(() => {
  const min = 200
  const h = Math.max(min, subjectStats.value.length * 40 + 60)
  return `${h}px`
})

// ===== 数据加载 =====
async function loadOverview() {
  overviewLoading.value = true
  try {
    const res = await getStatsOverview()
    overview.value = res.data
  } finally {
    overviewLoading.value = false
  }
}

async function loadDailyStats() {
  trendLoading.value = true
  try {
    const res = await getDailyStats(trendDays.value)
    dailyStats.value = res.data
    renderTrendChart()
  } finally {
    trendLoading.value = false
  }
}

async function loadSubjectStats() {
  subjectLoading.value = true
  try {
    const res = await getSubjectStats()
    subjectStats.value = res.data
    renderSubjectChart()
  } finally {
    subjectLoading.value = false
  }
}

async function loadErrors() {
  errorLoading.value = true
  try {
    const res = await getErrors()
    errors.value = res.data
    renderReasonChart()
  } finally {
    errorLoading.value = false
  }
}

// ===== ECharts 渲染 =====

/** 答题趋势折线图 */
function renderTrendChart() {
  if (!trendChartRef.value) return
  if (!trendChart) {
    trendChart = echarts.init(trendChartRef.value)
  }
  const dates   = dailyStats.value.map(d => d.date)
  const answers = dailyStats.value.map(d => d.totalAnswered)
  const correct = dailyStats.value.map(d => d.correctCount)

  trendChart.setOption({
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'cross' },
    },
    legend: {
      data: ['答题数', '正确数'],
      bottom: 0,
      textStyle: { fontSize: 12 },
    },
    grid: { left: 40, right: 20, top: 20, bottom: 40 },
    xAxis: {
      type: 'category',
      data: dates,
      axisLabel: {
        rotate: dates.length > 14 ? 30 : 0,
        formatter: (val: string) => val.slice(5),  // 只显示 MM-DD
      },
    },
    yAxis: { type: 'value', minInterval: 1 },
    series: [
      {
        name: '答题数',
        type: 'line',
        data: answers,
        smooth: true,
        symbol: 'circle',
        symbolSize: 4,
        itemStyle: { color: '#409eff' },
        areaStyle: { color: 'rgba(64,158,255,0.08)' },
      },
      {
        name: '正确数',
        type: 'line',
        data: correct,
        smooth: true,
        symbol: 'circle',
        symbolSize: 4,
        itemStyle: { color: '#67c23a' },
        areaStyle: { color: 'rgba(103,194,58,0.08)' },
      },
    ],
  }, true)
}

/** 科目正确率横向柱状图 */
function renderSubjectChart() {
  if (!subjectChartRef.value) return
  if (!subjectChart) {
    subjectChart = echarts.init(subjectChartRef.value)
  }

  if (subjectStats.value.length === 0) {
    subjectChart.setOption({ title: { text: '暂无数据', left: 'center', top: 'middle', textStyle: { color: '#c0c4cc', fontSize: 14 } } }, true)
    return
  }

  const names     = subjectStats.value.map(s => s.subjectName)
  const accuracies = subjectStats.value.map(s => s.accuracy)
  const totals    = subjectStats.value.map(s => s.totalAnswered)

  subjectChart.setOption({
    tooltip: {
      trigger: 'axis',
      formatter: (params: any[]) => {
        const idx = params[0].dataIndex
        const s = subjectStats.value[idx]
        return `${s.subjectName}<br/>答题数：${s.totalAnswered}<br/>正确率：${s.accuracy}%`
      },
    },
    grid: { left: 80, right: 60, top: 10, bottom: 30 },
    xAxis: {
      type: 'value',
      max: 100,
      axisLabel: { formatter: '{value}%' },
    },
    yAxis: {
      type: 'category',
      data: names,
      axisLabel: { fontSize: 12 },
    },
    series: [
      {
        type: 'bar',
        data: accuracies,
        barMaxWidth: 28,
        label: {
          show: true,
          position: 'right',
          formatter: (p: any) => `${p.value}%`,
          fontSize: 12,
        },
        itemStyle: {
          color: (params: any) => {
            const v = params.value as number
            if (v >= 80) return '#67c23a'
            if (v >= 60) return '#e6a23c'
            return '#f56c6c'
          },
          borderRadius: [0, 4, 4, 0],
        },
      },
    ],
  }, true)
}

/** 错因分布饼图 */
function renderReasonChart() {
  if (!reasonChartRef.value) return
  if (!reasonChart) {
    reasonChart = echarts.init(reasonChartRef.value)
  }

  // 前端聚合错因统计（只统计有错因标签的错题）
  const reasonCount: Record<number, number> = {}
  for (const e of errors.value) {
    if (e.errorReason) {
      reasonCount[e.errorReason] = (reasonCount[e.errorReason] ?? 0) + 1
    }
  }

  const pieData = Object.entries(reasonCount)
    .map(([key, count]) => ({
      name: ERROR_REASON_MAP[Number(key) as keyof typeof ERROR_REASON_MAP] ?? `原因${key}`,
      value: count,
    }))
    .sort((a, b) => b.value - a.value)

  if (pieData.length === 0) {
    reasonChart.setOption({
      title: {
        text: '暂无错题数据',
        left: 'center',
        top: 'middle',
        textStyle: { color: '#c0c4cc', fontSize: 14 },
      },
    }, true)
    return
  }

  const COLORS = ['#f56c6c', '#e6a23c', '#409eff', '#67c23a', '#909399', '#b37feb']

  reasonChart.setOption({
    tooltip: {
      trigger: 'item',
      formatter: '{b}：{c} 题 ({d}%)',
    },
    legend: {
      orient: 'horizontal',
      bottom: 0,
      textStyle: { fontSize: 11 },
    },
    series: [
      {
        type: 'pie',
        radius: ['38%', '65%'],
        center: ['50%', '44%'],
        data: pieData,
        label: { fontSize: 12 },
        itemStyle: {
          color: (params: any) => COLORS[params.dataIndex % COLORS.length],
          borderWidth: 2,
          borderColor: '#fff',
        },
        emphasis: {
          itemStyle: { shadowBlur: 8, shadowColor: 'rgba(0,0,0,0.12)' },
        },
      },
    ],
  }, true)
}

// ===== 响应式 resize =====
let resizeObserver: ResizeObserver | null = null

function initResizeObserver() {
  resizeObserver = new ResizeObserver(() => {
    trendChart?.resize()
    subjectChart?.resize()
    reasonChart?.resize()
  })
  // 监听 dashboard 容器即可
  const container = document.querySelector('.stats-dashboard')
  if (container) resizeObserver.observe(container)
}

// ===== 生命周期 =====
onMounted(async () => {
  await Promise.all([
    loadOverview(),
    loadDailyStats(),
    loadSubjectStats(),
    loadErrors(),
  ])
  initResizeObserver()
})

// 科目数量变化时重新调整 subject 图表高度
watch(subjectChartHeight, () => {
  subjectChart?.resize()
})

onBeforeUnmount(() => {
  resizeObserver?.disconnect()
  trendChart?.dispose()
  subjectChart?.dispose()
  reasonChart?.dispose()
})
</script>

<style scoped>
.stats-dashboard {
  padding: 20px;
  max-width: 1400px;
  margin: 0 auto;
}

/* 页头 */
.page-header {
  display: flex;
  align-items: baseline;
  gap: 12px;
  margin-bottom: 20px;
}
.page-title {
  font-size: 22px;
  font-weight: 700;
  color: var(--el-text-color-primary);
  margin: 0;
}
.page-subtitle {
  font-size: 13px;
  color: var(--el-text-color-secondary);
}

/* 概览卡片 */
.overview-cards {
  margin-bottom: 16px;
}
.overview-cards .el-col {
  margin-bottom: 16px;
}

.stat-card {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 16px 18px;
  background: #fff;
  border-radius: 10px;
  border: 1px solid var(--el-border-color-lighter);
  transition: box-shadow 0.2s;
}
.stat-card:hover {
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.08);
}

.stat-icon-wrap {
  flex-shrink: 0;
  width: 44px;
  height: 44px;
  border-radius: 10px;
  background: var(--accent-bg);
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--accent);
}

.stat-body {
  min-width: 0;
}
.stat-value {
  font-size: 22px;
  font-weight: 700;
  color: var(--el-text-color-primary);
  line-height: 1.2;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.stat-label {
  font-size: 12px;
  color: var(--el-text-color-secondary);
  margin-top: 2px;
}

/* 图表卡片 */
.chart-card {
  margin-bottom: 16px;
}

.chart-card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.chart-title {
  font-size: 15px;
  font-weight: 600;
  color: var(--el-text-color-primary);
}

.chart-box {
  height: 280px;
  width: 100%;
}

/* 底部两列图表 */
.bottom-charts .el-col {
  margin-bottom: 16px;
}
</style>
