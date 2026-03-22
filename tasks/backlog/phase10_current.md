---
# Phase 10：题目纠错反馈（质量闭环）

## 目标
用户在做题/错题详情页可提交题目问题反馈（含截图），管理员在后台处理并回复；用户可在"我的反馈"追踪处理进度，形成完整的题目质量闭环。

## 前置条件
- Phase 3（刷题模块）验收通过：做题页面已存在
- Phase 2（管理员权限）验收通过：管理员角色判断已实现
- Phase 7（管理员后台页面框架）验收通过：AdminLayout.vue 已存在

## 任务清单

### 后端 - 数据表
- [ ] 任务1：在 `schema.sql` 追加 `t_question_feedback` 表（见 CLAUDE.md 数据表定义）
- [ ] 任务2：创建实体 `entity/QuestionFeedback.java`

### 后端 - 反馈接口（用户侧）
- [ ] 任务3：创建 `module/feedback/FeedbackController.java`，实现：

  **`POST /api/feedback/question`**
  - 请求：`{questionId, type(1-4), description(可选,max 500字), screenshotUrl(可选)}`
  - 校验：questionId 对应题目存在；同一用户同一题目未处理的反馈已存在时返回 409（提示"已有待处理的反馈"）
  - 写入 t_question_feedback，status=0（待处理）
  - 返回：`{feedbackId}`

  **`POST /api/feedback/screenshot`**
  - 接收截图文件（multipart/form-data）
  - 上传到 MinIO，返回 `{screenshotUrl}`
  - 限制：图片类型（JPG/PNG），大小 ≤ 3MB

  **`GET /api/feedback/my`**
  - 返回当前用户所有反馈记录，按 created_at 倒序
  - 每条含：feedbackId/questionId/questionTitle(截取前50字)/type/status/adminReply/createdAt/repliedAt

  **`GET /api/feedback/my/{id}`**
  - 反馈详情：含完整题目内容 + 反馈信息 + 管理员回复

### 后端 - 反馈接口（管理员侧）
- [ ] 任务4：在 `module/admin/` 下创建 `AdminFeedbackController.java`（需要 ADMIN 角色）：

  **`GET /api/admin/feedback`**
  - 支持筛选：`?status=&type=&page=&size=`
  - 返回反馈列表，含：用户名/题目摘要/反馈类型/描述/截图URL/状态/提交时间
  - 待处理（status=0）排最前，其次按时间倒序

  **`GET /api/admin/feedback/{id}`**
  - 返回完整反馈详情：题目完整内容+选项+答案/反馈描述/截图/历史处理记录

  **`PUT /api/admin/feedback/{id}`**
  - 请求：`{status(1-3), adminReply(必填)}`
  - 更新 status + admin_reply + admin_id + replied_at
  - 触发"站内通知"：在 Redis 写入一条通知（key: `notice:{userId}`, list 类型，TTL 7天）
    通知内容：`"您反馈的题目「XXX」已处理：{adminReply}"`

### 后端 - 站内通知（轻量实现）
- [ ] 任务5：新增 `GET /api/user/notices`：读取 Redis 中当前用户的通知列表，最多返回 20 条（读后不删除，另设"已读"标记）
- [ ] 任务6：新增 `PUT /api/user/notices/read-all`：标记所有通知已读（Redis 中写入 `notice:read:{userId}` = 当前时间戳）
  未读数计算：notices 列表中 created_at > read_all_at 的数量

### 前端 - 题目反馈入口
- [ ] 任务7：创建 `components/FeedbackDialog.vue`（反馈弹框组件，可在多处复用）：
  - 触发方式：`<FeedbackDialog :questionId="id" />` 插入到 QuizAnswerView / ErrorDetailView / MockAnswerView
  - 弹框内容：
    - 单选反馈类型（4个选项：答案错误/题干歧义/解析不清/排版问题）
    - 可选填写描述（el-input textarea，500 字限制）
    - 可选上传截图（el-upload，单文件，调用 `POST /api/feedback/screenshot`）
    - 截图预览缩略图
    - 提交 / 取消 按钮
  - 提交成功后 Toast 提示"感谢反馈，我们将尽快处理"

- [ ] 任务8：在以下页面右下角/更多菜单中加入"反馈此题"按钮：
  - `views/quiz/QuizAnswerView.vue`：每题右下角加小按钮（图标风格，不占空间）
  - `views/errorbook/ErrorDetailView.vue`：操作栏加"反馈此题"
  - `views/mock/MockAnswerView.vue`：答题区底部加"反馈"链接

### 前端 - 我的反馈页面
- [ ] 任务9：创建 `views/feedback/MyFeedbackView.vue`（我的反馈列表）：
  - Tab 切换：全部 / 待处理 / 已处理
  - 每条反馈卡片：
    - 反馈类型标签（彩色）+ 题目摘要（灰色字）
    - 提交时间
    - 状态标签：🕐待处理（灰）/ ✅已采纳（绿）/ ❌已驳回（红）/ 🔧已修复（蓝）
    - 如有管理员回复：显示回复内容（灰色背景引用样式）
  - 在个人中心 / 侧边栏菜单中加入"我的反馈"入口

### 前端 - 管理员后台反馈管理
- [ ] 任务10：创建 `views/admin/FeedbackManageView.vue`（管理员反馈列表）：
  - 顶部筛选栏：状态筛选（el-radio-group）+ 类型筛选
  - 待处理数量红色 Badge 提示（在侧边菜单"题目反馈"旁显示）
  - 表格列：提交用户/题目摘要/反馈类型/描述摘要/截图（点击预览）/状态/提交时间/操作
  - 操作：点击"处理"打开侧边抽屉（el-drawer）

- [ ] 任务11：创建 `views/admin/FeedbackDetailDrawer.vue`（处理抽屉）：
  - 上半部分：题目完整内容（含选项答案解析）
  - 下半部分：用户反馈详情（类型/描述/截图）
  - 最底部：处理区
    - 状态单选（已采纳/已驳回/已修复）
    - 管理员回复输入框（必填，最少 10 字）
    - "提交处理"按钮
  - 提交成功后关闭抽屉，列表自动刷新

### 前端 - 顶栏通知
- [ ] 任务12：在 `AppLayout.vue` 顶栏用户头像旁添加铃铛图标（el-badge 显示未读数）：
  - 点击下拉展示最近 5 条通知（简短文本 + 时间）
  - 底部"查看全部"跳转 `views/feedback/MyFeedbackView.vue`
  - 展开时调用 read-all 接口清零未读数
  - 每 60 秒轮询 `GET /api/user/notices` 更新未读数（或进入页面时刷新一次即可）

## 验收标准
- [ ] 在做题页提交反馈后，`GET /api/feedback/my` 中出现该条记录，status=0
- [ ] 同一用户对同一题目重复提交（已有待处理反馈时）返回 409 含友好提示
- [ ] 管理员在后台修改状态并填写回复后提交，用户侧 `GET /api/feedback/my/{id}` 中看到回复内容
- [ ] 顶栏铃铛在管理员处理后出现未读 Badge（轮询或刷新页面触发）
- [ ] 截图上传成功后弹框中显示缩略图预览，提交后 `GET /api/admin/feedback/{id}` 中截图 URL 可访问
- [ ] 管理员后台待处理 Tab 的数量与数据库 status=0 的记录数一致

## 注意事项
- **站内通知用 Redis List 而非数据库**：反馈处理通知量少、时效短，Redis 足够；若以后需要更复杂的通知系统再迁移数据库
- **截图 URL 安全**：MinIO 截图使用预签名 URL（有效期 24h），不要直接暴露 bucket 公开访问地址
- **管理员回复必填**：后端 Validation 校验 adminReply 长度 ≥ 10，防止管理员随意"已处理"
- **FeedbackDialog 解耦**：组件通过 Props 传入 questionId，不依赖全局 store，可在任何页面插入
- **反馈类型枚举**：后端定义 `FeedbackType` 枚举（WRONG_ANSWER/AMBIGUOUS/UNCLEAR_ANALYSIS/TYPO），不要用魔法数字
- **轮询频率**：通知轮询建议在用户活跃时（有鼠标事件）才触发，避免后台标签页持续轮询消耗资源（用 `document.visibilitychange` 事件控制）
---
