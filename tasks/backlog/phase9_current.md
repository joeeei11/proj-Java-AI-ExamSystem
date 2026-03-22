---
# Phase 9：间隔复习引擎（错因标签 + 错题 SRS + 抽认卡 SRS）

## 目标
实现一套统一的间隔复习（SRS）引擎：错题本支持错因标签和按算法安排复习时间，笔记可 AI 生成抽认卡并按相同算法每日调度复习任务，两个入口在首页汇聚为"今日待复习"模块。

## 前置条件
- Phase 3（错题本基础）验收通过：t_error_book 已有数据
- Phase 4（笔记模块）验收通过：t_note 已有数据
- DeepSeek API 可调用（抽认卡 AI 生成）

## 任务清单

### 后端 - SRS 核心引擎
- [ ] 任务1：在 `t_error_book` 表执行 ALTER TABLE，添加字段：
  ```sql
  ALTER TABLE t_error_book
    ADD COLUMN error_reason       TINYINT DEFAULT NULL COMMENT '错因:1概念不清/2审题失误/3计算失误/4方法不会/5时间不足/6粗心',
    ADD COLUMN next_review_at     DATETIME DEFAULT NULL COMMENT 'SRS下次复习时间',
    ADD COLUMN review_interval_days INT DEFAULT 1 COMMENT '当前间隔天数',
    ADD COLUMN review_count       INT DEFAULT 0 COMMENT '总复习次数',
    ADD COLUMN consecutive_correct INT DEFAULT 0 COMMENT '连续正确次数';
  ```
- [ ] 任务2：创建数据表（已在 CLAUDE.md 中定义）：
  - `t_flashcard_deck`（卡组）
  - `t_flashcard`（单张卡，含 SRS 字段）
- [ ] 任务3：创建 `module/common/SrsService.java`（统一 SRS 计算服务）：
  ```java
  /**
   * 计算下次复习时间
   * @param known      用户是否掌握（true=会了，false=不会）
   * @param currentInterval  当前间隔天数
   * @param consecutiveCorrect 连续正确次数
   * @return SrsResult {nextReviewAt, newInterval, newConsecutiveCorrect}
   */
  SrsResult calculate(boolean known, int currentInterval, int consecutiveCorrect);
  // 规则：会了 → interval = min(ceil(interval*2.5), 60)；不会 → interval = 1
  ```
- [ ] 任务4：创建实体 `entity/FlashcardDeck.java`、`entity/Flashcard.java`

### 后端 - 错因标签 + 错题间隔复习
- [ ] 任务5：在 `module/errorbook/ErrorBookController.java` 新增接口：

  **`PUT /api/errors/{id}/reason`**
  - 请求：`{reason: 1-6}`
  - 更新 t_error_book.error_reason
  - 同时若 next_review_at 为空，设置首次复习时间为明天（新入错题本即安排复习）

  **`GET /api/errors/today-review`**
  - 查询条件：`next_review_at <= NOW() AND is_mastered = 0`，JOIN t_question 返回完整题目
  - 返回按 next_review_at 升序排列（最紧迫的先出）

  **`POST /api/errors/{id}/review`**
  - 请求：`{known: true/false}`
  - 调用 SrsService.calculate() 计算新的 next_review_at 和 interval
  - 更新 t_error_book 的 SRS 字段
  - 若 known=true 且 consecutive_correct >= 3，自动设置 is_mastered=1

### 后端 - 抽认卡
- [ ] 任务6：创建 `module/flashcard/FlashcardController.java`：

  **`POST /api/flashcards/generate`**
  - 请求：`{noteId}`
  - 读取笔记内容，调用 AI 生成卡片（Prompt 要求返回 JSON 数组 `[{front:"问题",back:"答案"}]`，每篇笔记生成 5-15 张）
  - 创建 t_flashcard_deck + 批量写入 t_flashcard
  - 设置所有卡片 next_review_at = 当天（立即可复习）
  - 返回：`{deckId, cardCount, deck 基本信息}`

  **`GET /api/flashcards/decks`**
  - 返回当前用户所有卡组，含：title/noteTitle/cardCount/masteredCount/nextReviewCount
  - masteredCount = consecutive_correct >= 3 的卡片数
  - nextReviewCount = next_review_at <= NOW() 的卡片数（今日到期）

  **`GET /api/flashcards/decks/{deckId}`**
  - 返回卡组详情 + 所有卡片列表（含 SRS 状态）

  **`DELETE /api/flashcards/decks/{deckId}`**
  - 删除卡组及其所有卡片（物理删除，需二次确认）

  **`PUT /api/flashcards/{cardId}`**
  - 编辑单张卡片的 front/back（AI 生成后用户可手动修正）

  **`GET /api/flashcards/today-review`**
  - 查询：`next_review_at <= NOW() AND consecutive_correct < 3`（未完全掌握）
  - 随机打散顺序返回（不按 deck 分组，混合更好）
  - 返回：`{totalCount, cards:[{id, deckId, deckTitle, front, back}]}`

  **`POST /api/flashcards/{cardId}/review`**
  - 请求：`{known: true/false}`
  - 调用 SrsService.calculate()，更新卡片 SRS 字段
  - 返回：`{nextReviewAt, newInterval}`（前端可展示"下次复习在 X 天后"）

- [ ] 任务7：在 `resources/prompts/flashcard_generate.txt` 写入 Prompt 模板：
  - System：指定返回 JSON 数组格式，要求问题面简洁（≤50字），答案面精准（≤200字）
  - 示例：行测数量关系笔记 → `[{"front":"等差数列求和公式","back":"Sn = n(a1+an)/2 = na1 + n(n-1)d/2"}]`

### 前端 - 首页"今日待复习"模块
- [ ] 任务8：在 `views/dashboard/DashboardView.vue` 新增"今日待复习"卡片区域（Phase 6 基础上追加）：
  - 显示两个数字：今日待复做错题数 + 今日待复习抽认卡数
  - 点击对应数字跳转到对应复习页面
  - 两者之和为 0 时展示"今日任务已完成 🎉"

### 前端 - 错因标签
- [ ] 任务9：在 `views/errorbook/ErrorDetailView.vue` 和 `ErrorListView.vue` 中新增错因标签组件：
  - 创建 `components/ErrorReasonTag.vue`：6 个标签（概念不清/审题失误/计算失误/方法不会/时间不足/粗心），不同颜色区分，单选，可取消
  - 点击后立即调用 `PUT /api/errors/{id}/reason` 保存（无需提交按钮）
  - 错题列表中每张卡片显示已设置的错因标签

### 前端 - 错题今日待复做
- [ ] 任务10：创建 `views/errorbook/TodayReviewView.vue`（今日待复做页）：
  - 顶部：待复做题目总数 + "今日任务"标题
  - 进度条：已完成 X / 共 Y 题
  - 每次展示一题（全屏答题卡样式），用户输入答案后点"确认"显示答案
  - 显示正确答案后，出现两个大按钮：**"我会了 ✓"（绿色）** / **"还不会 ✗"（红色）**
  - 点击后显示下次复习时间提示（"会了！下次复习在 X 天后"），自动跳下一题
  - 全部完成后展示完成动画 + 统计（本次会了 X 题，还需复习 Y 题）

### 前端 - 抽认卡
- [ ] 任务11：创建 `src/api/flashcard.ts`：封装所有抽认卡接口
- [ ] 任务12：创建 `views/flashcards/DeckListView.vue`（卡组列表页）：
  - 卡片网格：每格显示卡组标题/来源笔记名/总卡数/已掌握数/今日到期数
  - 顶部"今日需复习 X 张"醒目提示
  - 每个卡组右上角 ⋮ 菜单：查看详情/编辑卡片/删除卡组
  - 浮动"开始今日复习"按钮（跳转到 TodayFlashcardView）

- [ ] 任务13：创建 `views/flashcards/CardStudyView.vue`（卡片学习/浏览页）：
  - 用于浏览某个卡组所有卡片
  - 显示进度（第 X / 共 Y 张）
  - 卡片翻转动画（CSS `transform: rotateY(180deg)`，正面→背面）
  - 下方：**翻面** 按钮（未翻前）/ **我会了** + **不会** 按钮（翻面后）
  - 右上角铅笔图标 → 打开编辑弹框（直接编辑 front/back）

- [ ] 任务14：创建 `views/flashcards/TodayFlashcardView.vue`（今日待复习抽认卡页）：
  - 同 TodayReviewView 交互逻辑，但展示的是抽认卡（先显示问题面）
  - 顶部显示"今日待复习 X 张"
  - 每张卡片可查看来源卡组名（副标题）
  - 完成后统计展示

- [ ] 任务15：在笔记详情/编辑页 `views/notes/NoteEditorView.vue` 的工具栏新增"生成抽认卡"按钮：
  - 点击调用 `/api/flashcards/generate`，成功后 Toast 提示"已生成 X 张抽认卡"并显示跳转链接

## 验收标准
- [ ] 错题详情页可以设置/修改错因标签，刷新后保持选中状态
- [ ] 新录入错题后，第二天 `GET /api/errors/today-review` 会返回该题
- [ ] 点"会了"后，该错题从今日复做列表消失，再次调用接口返回新的 next_review_at（≥ 2 天后）
- [ ] 连续 3 次"会了"后，该错题 is_mastered 自动变为 1
- [ ] `POST /api/flashcards/generate` 成功生成抽认卡，卡片 front/back 内容合理（非空）
- [ ] 抽认卡翻面动画流畅，点"不会"后该卡明天再次出现在今日复习列表
- [ ] 首页"今日待复习"数字与 `/api/errors/today-review` 和 `/api/flashcards/today-review` 数据一致

## 注意事项
- **SRS 不影响现有"标记已掌握"功能**：手动标记已掌握 = 直接设 is_mastered=1，不经过 SRS 流程；SRS 的自动掌握是补充逻辑
- **抽认卡 Prompt 中文公考场景优化**：Prompt 中说明"这是公考备考笔记，请生成适合背诵的问答卡片，问题面要考查核心知识点，答案面要精简准确"
- **首次生成的卡片 next_review_at**：设置为当天（立即可复习），而非 1 天后；让用户当天就能体验到复习功能
- **卡片翻转 CSS**：需要给卡片容器设置 `perspective`，给正反两面分别设置 `backface-visibility: hidden`，避免翻转时看到背面透过来
- **今日复习混合顺序**：`today-review` 接口需打乱顺序（`ORDER BY RAND()` 或后端 Collections.shuffle），防止每次都是同样顺序导致依赖顺序记忆
- **数据一致性**：SRS 字段更新必须在数据库事务中完成，不能先更新 interval 再更新 next_review_at（两次更新）
---
