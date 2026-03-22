---
# Phase 6：数据统计与可视化仪表盘

## 目标
首页仪表盘展示用户学习全貌，包括做题正确率趋势、各科目掌握情况雷达图、知识点错误热力图、连续打卡日历，让用户直观了解自己的学习状态。

## 前置条件
- Phase 3、4、5 全部验收通过（有足够测试数据：做题记录/笔记/错题）
- ECharts 已安装到前端

## 任务清单

### 后端 - 统计模块
- [ ] 任务1：创建 `module/stats/StatsController.java`：

  **`GET /api/stats/overview`**（首页总览卡片数据）
  返回：
  ```json
  {
    "totalQuestions": 128,     // 累计答题数
    "correctRate": 0.73,       // 总正确率
    "totalNotes": 15,          // 笔记总数
    "errorCount": 22,          // 未掌握错题数
    "studyStreak": 7,          // 连续打卡天数
    "studyDays": 20            // 累计学习天数
  }
  ```

  **`GET /api/stats/daily?days=30`**（近 N 天每日统计，折线图用）
  返回：`[{date:"2026-03-01", questionCount:20, correctCount:15, studyMinutes:45}]`

  **`GET /api/stats/subject`**（各科目做题情况，雷达图用）
  返回：`[{subjectName:"行测", total:80, correctRate:0.75}, ...]`

  **`GET /api/stats/error-heatmap`**（错题知识点热力图）
  返回：按知识点分组的错题数量 `[{knowledgePoint:"数量关系", errorCount:12}, ...]`

  **`GET /api/stats/calendar?year=2026`**（打卡日历数据）
  返回：`[{date:"2026-03-01", hasCheckedIn:true, questionCount:20}]`

- [ ] 任务2：在答题提交接口（Phase 3 任务4）中补充异步统计更新：
  - 使用 `@Async` 异步执行 `t_study_stat` 的 `INSERT ... ON DUPLICATE KEY UPDATE` 操作
  - 统计内容：当天 total_questions + correct_count + study_minutes（提交时记录耗时）
- [ ] 任务3：打卡后更新统计：`POST /api/user/check-in` 时，在 t_study_stat 对应日期打卡标记

### 前端 - 仪表盘首页
- [ ] 任务4：创建 `src/api/stats.ts`：封装所有统计接口
- [ ] 任务5：创建 `views/dashboard/DashboardView.vue`（首页仪表盘）：

  **顶部数据卡片行（6个）：**
  - 累计答题数（图标：📝）
  - 总正确率（环形进度条，绿色）
  - 笔记总数（图标：📒）
  - 未掌握错题（图标：❌，红色提醒）
  - 连续打卡天数（图标：🔥）
  - 累计学习天数（图标：📅）

  **中部图表行（两列）：**
  - 左：近 30 天答题数折线图（ECharts Line，双折线：答题数+正确数）
  - 右：各科目正确率雷达图（ECharts Radar）

  **底部行（两列）：**
  - 左：错题知识点分布柱状图（ECharts Bar，横向，显示前 10 个知识点）
  - 右：学习打卡日历（使用 ECharts heatmap 或自定义 CSS Grid 日历）

- [ ] 任务6：创建 `views/stats/StatsDetailView.vue`（详细统计页）：
  - 日期范围选择器（切换 7/30/90 天）
  - 可交互折线图（鼠标悬停显示详情）
  - 各科目饼图（答题量占比）
  - 导出学习报告按钮（调用浏览器打印为 PDF）

- [ ] 任务7：创建可复用的 `components/charts/` 目录：
  - `LineChart.vue`：折线图组件（接收 data/xAxis 等 props）
  - `RadarChart.vue`：雷达图组件
  - `BarChart.vue`：柱状图组件
  - 统一封装 ECharts，支持 resize 自适应（ResizeObserver）

### 优化功能：学习推荐（加分项）
- [ ] 任务8：创建 `GET /api/stats/recommend`（智能推荐，基于弱点出题）：
  - 取薄弱知识点（错误率 > 50% 或错误次数 > 3）
  - 随机选 1-2 个知识点，返回"推荐今日练习：XXX 知识点"
  - 前端首页展示推荐条目，点击直接跳转出题页（预填知识点参数）

## 验收标准
- [ ] `GET /api/stats/overview` 返回数据与数据库实际数据一致（人工核对）
- [ ] 首页仪表盘所有图表正常渲染，无白屏、无报错
- [ ] 折线图 X 轴日期正确，Y 轴数值与实际答题记录匹配
- [ ] 雷达图至少显示 3 个科目的数据，各维度数值在 0-1 之间（正确率）
- [ ] 窗口缩放时图表自适应宽度，不溢出或变形
- [ ] 打卡日历正确标注已打卡日期（深色）和未打卡日期（浅色）

## 注意事项
- **ECharts 按需引入**：不要 `import * from 'echarts'`，使用按需引入避免打包体积过大
  ```js
  import * as echarts from 'echarts/core'
  import { LineChart, RadarChart, BarChart } from 'echarts/charts'
  ```
- **统计查询性能**：`t_study_stat` 按 `(user_id, stat_date)` 有唯一索引，范围查询很快；`t_user_answer` 查询时必须带 `user_id` 条件
- **雷达图维度对齐**：后端必须返回所有科目（没有做题记录的科目 correctRate 返回 0），否则前端雷达图维度数量变化会报错
- **首页加载性能**：使用 Promise.all 并行请求所有统计接口，而非串行调用
- **日历组件**：推荐自行用 CSS Grid 实现月份日历，避免引入重型日历组件库
---
