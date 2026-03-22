---
# Phase 10：题目纠错反馈

## 目标
实现题目纠错反馈功能，包含：
1. 用户提交反馈（答案错误/题干歧义/解析不清/排版问题）
2. 用户查看自己的反馈列表与详情
3. 管理员查看全部反馈、处理（采纳/驳回/已修复）并回复
4. 管理员处理后通过 Redis List 发送站内通知给用户

## 前置条件
- Phase 1-9 全部完成
- `t_question_feedback` 表已在 schema.sql 中定义
- Redis 已集成（TDR-007：通知用 Redis List，key: notice:{userId}，TTL 7天）

## 数据表（schema.sql 已包含）
- `t_question_feedback`（Phase 10，已在 schema.sql 定义）

## 任务清单

### 后端

- [ ] 任务1：创建 `entity/QuestionFeedback.java` 实体
- [ ] 任务2：创建 `module/feedback/FeedbackMapper.java`（含 selectByUserId、selectAllForAdmin 分页查询）
- [ ] 任务3：创建 DTO：
  - `SubmitFeedbackRequest`（questionId、type、description、screenshotUrl?）
  - `FeedbackVO`（含题目简要信息）
  - `FeedbackDetailVO`（完整详情含管理员回复）
  - `HandleFeedbackRequest`（status、adminReply）
- [ ] 任务4：创建 `module/feedback/FeedbackService.java`
  - `submit`：提交反馈
  - `listMy`：我的反馈列表（分页）
  - `getMy`：我的反馈详情
  - `listAll`（管理员）：全部反馈（可按 status 过滤，分页）
  - `handle`（管理员）：处理反馈，写 Redis 通知
- [ ] 任务5：创建 `module/feedback/FeedbackController.java`
  - 用户端：POST /api/feedback/question、GET /api/feedback/my、GET /api/feedback/my/{id}
  - 管理员端：GET /api/admin/feedback、PUT /api/admin/feedback/{id}
- [ ] 任务6：在 `resources/mapper/` 新增 FeedbackMapper.xml（分页查询 SQL）
- [ ] 任务7：（可选）创建 `module/common/NoticeService.java`，封装 Redis 通知写入/读取

### 前端

- [ ] 任务8：创建 `src/api/feedback.ts`（含完整 TS 类型）
- [ ] 任务9：创建/替换 `src/views/feedback/MyFeedback.vue`
  - 我的反馈列表（状态 Tag / 类型 / 创建时间）
  - 查看详情（管理员回复）
  - 提交反馈入口（对话框，选类型+填写描述）
- [ ] 任务10：更新 `src/views/admin/AdminDashboard.vue` 或新建 `AdminFeedback.vue`
  - 反馈列表（按状态过滤/分页）
  - 处理对话框（选状态 + 填写回复）
- [ ] 任务11：检查 `src/views/quiz/QuizHome.vue` 是否有纠错反馈入口（答题结果页面），若无则添加
- [ ] 任务12：更新 `src/router/index.ts`（若需新增管理员反馈路由）

## 验收标准
- [ ] 用户可在答题结果页对题目提交反馈
- [ ] 我的反馈列表正确展示状态（待处理/已采纳/已驳回/已修复）
- [ ] 管理员可分页查看所有反馈并处理
- [ ] 管理员处理后，Redis 写入通知（key: notice:{userId}，TTL 7天）
- [ ] 用户查看详情可看到管理员回复

## 注意事项
- 反馈类型：1 答案错误 / 2 题干歧义 / 3 解析不清 / 4 排版问题
- 反馈状态：0 待处理 / 1 已采纳 / 2 已驳回 / 3 已修复
- Redis 通知（TDR-007）：管理员处理后写 Redis List，不建数据库表，TTL 7天
- 截图上传可复用 MinioUtil（可选功能）
---
