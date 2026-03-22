---
# Phase 8：模考模式

## 目标
用户可发起一场限时模拟考试，答题过程中显示倒计时、支持题号跳转和标记存疑题，到时或手动交卷后生成详细模考报告（含各知识点/题型的正确率分析）。

## 前置条件
- Phase 3（AI 刷题核心）验收通过：`t_question` 表有足够题目，AI 出题接口可用
- Phase 2（科目/知识点管理）验收通过

## 任务清单

### 后端 - 数据表
- [ ] 任务1：在 `schema.sql` 追加两张新表：
  - `t_mock_exam`（模考记录：user_id/subject_id/title/time_limit_min/total_questions/total_correct/status/started_at/submitted_at）
  - `t_mock_exam_question`（模考题明细：exam_id/question_id/sort_order/is_flagged/user_answer/is_correct）
- [ ] 任务2：创建实体 `entity/MockExam.java`、`entity/MockExamQuestion.java`

### 后端 - 模考接口
- [ ] 任务3：创建 `module/mock/MockController.java`，实现以下接口：

  **`POST /api/mock/start`**
  - 请求：`{subjectId, count(5-100), timeLimitMinutes(10-180)}`
  - 逻辑：调用 AI 出题（或从题库随机抽题），写入 t_mock_exam + t_mock_exam_question
  - 返回：`{examId, title, timeLimitMinutes, totalQuestions, startedAt}`
  - 题库不足时友好提示"当前科目题目不足，请先多做练习题"

  **`GET /api/mock/{examId}`**
  - 校验 exam 归属当前用户且 status=0（进行中）
  - 计算剩余秒数：`timeLimit * 60 - TIMESTAMPDIFF(SECOND, started_at, NOW())`
  - 剩余时间 ≤ 0 时自动触发交卷逻辑（返回 409，前端收到后跳转结果页）
  - 返回：`{remainingSeconds, questions:[{id,sort_order,content,options,type,isAnswered,isFlagged,userAnswer}]}`

  **`PUT /api/mock/{examId}/flag`**
  - 请求：`{questionId, flagged: true/false}`
  - 更新 t_mock_exam_question.is_flagged

  **`POST /api/mock/{examId}/submit`**
  - 请求：`{answers:[{questionId, answer}]}`（允许部分未答题，未答按错误处理）
  - 逻辑：
    1. 批量判分，更新 t_mock_exam_question
    2. 汇总 total_correct，更新 t_mock_exam.status=1, submitted_at=now()
    3. 错误题目写入 t_error_book（复用 Phase 3 逻辑）
    4. 异步更新 t_study_stat
  - 返回：`{examId}` 前端凭此 ID 跳转报告页

  **`GET /api/mock/{examId}/report`**
  - 返回完整报告：
    ```json
    {
      "title": "行测模考 #3",
      "totalQuestions": 50,
      "totalCorrect": 37,
      "correctRate": 0.74,
      "timeUsedSeconds": 2800,
      "timeLimitSeconds": 3600,
      "knowledgeStats": [
        {"knowledgePoint":"数量关系","total":10,"correct":6,"correctRate":0.6},
        ...
      ],
      "typeStats": [
        {"type":"单选题","total":40,"correct":31,"correctRate":0.775},
        {"type":"判断题","total":10,"correct":6,"correctRate":0.6}
      ],
      "weakPoints": ["数量关系","言语理解"],
      "questions": [{...每题答题详情及AI解析...}]
    }
    ```

  **`GET /api/mock/history`**
  - 返回当前用户历史模考列表，按 started_at 倒序
  - 包含：title/totalQuestions/totalCorrect/correctRate/status/startedAt/submittedAt

- [ ] 任务4：实现超时自动交卷：
  - `GET /api/mock/{examId}` 时检测超时，自动调用提交逻辑（以已作答内容判分）
  - 额外加一个 Spring Scheduled 任务（每 5 分钟扫描），补偿处理没有主动轮询而超时的进行中模考

### 前端 - 模考模式
- [ ] 任务5：创建 `src/api/mock.ts`：封装所有模考接口
- [ ] 任务6：创建 `views/mock/MockSetupView.vue`（模考配置页）：
  - 科目选择（el-select）
  - 题量滑块（5-100，步长 5，默认 50）
  - 考试时长滑块（10-180 分钟，步长 5，默认 90 分钟）
  - 预计时间提示（"平均每题约 X 秒"）
  - "开始模考"按钮（点击弹确认框，确认后跳转答题页，同时锁定路由离开）

- [ ] 任务7：创建 `views/mock/MockAnswerView.vue`（答题页 - 核心页面）：

  **顶部 Header（固定不滚动）：**
  - 左：考试标题（如"行测模考 #3"）
  - 中：**倒计时**（红色，格式 `mm:ss`；剩余 5 分钟时倒计时变红色闪烁）
  - 右："交卷"按钮（弹二次确认框，显示未作答题数和标记题数）

  **左侧题号导航面板（固定）：**
  - 网格显示所有题号按钮
  - 颜色区分：已答（绿）/ 标记（黄）/ 未答（灰）/ 当前题（蓝边框）
  - 点击任意题号直接跳转

  **右侧答题区（可滚动）：**
  - 当前题目内容 + 选项（el-radio / el-checkbox）
  - 简答题用 el-input textarea
  - 底部：上一题 / 标记此题（切换黄色） / 下一题 三个按钮
  - 作答后自动标记为已答（绿），不强制跳下一题

  **离开保护：**
  - 使用 `onBeforeRouteLeave` 拦截，模考进行中离开弹警告框

  **轮询剩余时间：**
  - 每 30 秒向 `GET /api/mock/{examId}` 轮询同步服务端剩余时间（防客户端时钟漂移）
  - 收到 409 时提示"考试时间已到，正在自动交卷..."并跳转报告页

- [ ] 任务8：创建 `views/mock/MockReportView.vue`（模考报告页）：
  - 顶部总结卡：得分/正确率/用时/总题数 四格展示
  - 正确率 ECharts 环形图（大圆，中间显示百分比）
  - 知识点正确率横向柱状图（从低到高排序，红色高亮弱点）
  - 题型统计表格（单选/多选/判断正确率）
  - 答题详情列表（可折叠，每题显示：题目/我的答案/正确答案/是否正确）
  - 底部：再来一场 / 查看错题本 两个按钮

- [ ] 任务9：创建 `views/mock/MockHistoryView.vue`（历史模考列表）：
  - 卡片列表：标题/日期/正确率进度条/状态标签
  - 点击"查看报告"跳转对应报告页

## 验收标准
- [ ] 开始模考后，答题页倒计时从设定时间开始倒数，与服务端时间误差 ≤ 3 秒
- [ ] 点击题号导航可直接跳转到对应题，标记题目后题号变黄
- [ ] 手动交卷弹出确认框，确认后提交成功，跳转报告页
- [ ] 倒计时归零后，前端自动提交（不需要用户操作），跳转报告页，报告中 status 显示"超时交卷"
- [ ] 模考报告中知识点正确率与 t_mock_exam_question 数据一致（人工核对 2-3 个知识点）
- [ ] 模考中的错题正确出现在错题本，不会与普通刷题记录混淆（source 字段标记"模考"）

## 注意事项
- **题目来源策略**：模考优先从 `t_question` 题库随机抽取（按 `subject_id` + `difficulty` 抽），题库不足时再 AI 补充生成；抽取时需排除该用户最近 3 天已做过的题（防重复）
- **并发交卷防重**：同一 examId 可能被前端和定时任务同时触发交卷，用 `UPDATE t_mock_exam SET status=1 WHERE id=? AND status=0` 的行级锁保证幂等
- **报告中 AI 解析**：模考报告题目解析按需加载（点击展开时再请求），不要在交卷时批量 AI 生成，避免长时间等待
- **倒计时精度**：前端 `setInterval(1000)` 做倒计时展示，每 30 秒一次服务端同步校准；不要依赖纯前端计时判断超时，以服务端为准
- **路由跳转保护**：答题页注册 `beforeunload` 事件（浏览器刷新/关闭提示）+ Vue Router 守卫（页内跳转提示）
---
