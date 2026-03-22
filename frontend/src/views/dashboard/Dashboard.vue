<template>
  <div class="dashboard">
    <el-row :gutter="20" class="stat-row">
      <el-col :span="6" v-for="stat in statCards" :key="stat.label">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-inner">
            <div class="stat-icon" :style="{ background: stat.color }">
              <el-icon size="24"><component :is="stat.icon" /></el-icon>
            </div>
            <div class="stat-content">
              <div class="stat-value">{{ stat.value }}</div>
              <div class="stat-label">{{ stat.label }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" class="chart-row">
      <el-col :span="16">
        <el-card shadow="never">
          <template #header>近 7 天答题趋势</template>
          <div ref="chartRef" style="height: 280px" />
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="never">
          <template #header>今日任务</template>
          <div class="today-tasks">
            <el-empty v-if="!todayTasks.length" description="今日任务已完成" :image-size="80" />
            <div v-for="task in todayTasks" :key="task.id" class="task-item">
              <el-tag :type="task.type">{{ task.label }}</el-tag>
              <span class="task-count">{{ task.count }} 题待复习</span>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, nextTick } from 'vue'
import * as echarts from 'echarts'
import { getStatsOverview, getDailyStats } from '@/api/stats'

const chartRef = ref<HTMLElement>()

const statCards = ref([
  { label: '累计答题', value: '0', icon: 'EditPen', color: 'linear-gradient(135deg,#409eff,#2d8cf0)' },
  { label: '正确率', value: '0%', icon: 'TrendCharts', color: 'linear-gradient(135deg,#67c23a,#4caf50)' },
  { label: '错题数', value: '0', icon: 'WarnTriangleFilled', color: 'linear-gradient(135deg,#e6a23c,#ff9800)' },
  { label: '连续打卡', value: '0 天', icon: 'Calendar', color: 'linear-gradient(135deg,#f56c6c,#ff5722)' },
])

const todayTasks = ref<{ id: number; label: string; type: string; count: number }[]>([])

let chartInstance: echarts.ECharts | null = null

async function fetchOverview() {
  try {
    const res = await getStatsOverview()
    const d = res.data
    statCards.value[0].value = String(d.totalAnswered)
    statCards.value[1].value = d.accuracy + '%'
    statCards.value[2].value = String(d.totalErrors)
    statCards.value[3].value = d.studyStreak + ' 天'
  } catch {
    // 静默处理
  }
}

async function fetchDailyChart() {
  try {
    const res = await getDailyStats(7)
    const dailyData = res.data || []

    await nextTick()
    if (!chartRef.value) return

    if (!chartInstance) {
      chartInstance = echarts.init(chartRef.value)
    }

    const dates = dailyData.map((d) => d.date.slice(5)) // MM-DD
    const counts = dailyData.map((d) => d.totalAnswered)

    chartInstance.setOption({
      tooltip: { trigger: 'axis' },
      xAxis: {
        type: 'category',
        data: dates.length ? dates : ['暂无数据'],
      },
      yAxis: { type: 'value', minInterval: 1 },
      series: [
        {
          name: '答题数',
          type: 'bar',
          data: counts.length ? counts : [0],
          itemStyle: { color: '#409eff', borderRadius: [4, 4, 0, 0] },
        },
      ],
    })
  } catch {
    // 静默处理
  }
}

onMounted(() => {
  fetchOverview()
  fetchDailyChart()
})
</script>

<style scoped>
.dashboard {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.stat-card {
  border-radius: 10px;
}

.stat-inner {
  display: flex;
  align-items: center;
  gap: 16px;
}

.stat-icon {
  width: 56px;
  height: 56px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  flex-shrink: 0;
}

.stat-value {
  font-size: 28px;
  font-weight: 700;
  color: #303133;
  line-height: 1;
}

.stat-label {
  font-size: 13px;
  color: #909399;
  margin-top: 6px;
}

.today-tasks {
  min-height: 200px;
}

.task-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 0;
  border-bottom: 1px solid #ebeef5;
}

.task-count {
  font-size: 13px;
  color: #606266;
}
</style>
