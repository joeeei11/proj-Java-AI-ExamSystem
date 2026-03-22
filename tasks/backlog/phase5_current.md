---
# Phase 5：智能错题本与 AI 复习中心

## 目标
错题本具备 AI 深度解析和重做功能，复习中心可基于用户笔记 AI 生成复习提纲和高频考点预测，薄弱知识点可视化展示。

## 前置条件
- Phase 3 全部验收通过（错题已可自动录入）
- Phase 4 全部验收通过（笔记数据已存在）
- DeepSeek API 稳定可调用

## 任务清单

### 后端 - 错题本模块
- [ ] 任务1：创建实体 `entity/ErrorBook.java`（对应 t_error_book）
- [ ] 任务2：创建 `module/errorbook/ErrorBookController.java`：
  - `GET /api/errors`：错题列表，支持 `?subjectId=&isMastered=&page=&size=`，JOIN t_question 返回题目内容
  - `GET /api/errors/{id}`：错题详情；若 ai_analysis 为空，触发 AI 异步分析并返回（首次需等待）
  - `POST /api/errors/{id}/redo`：重做接口，返回题目信息（不含答案），用户提交后判分，正确则 error_count 不变但标记 last_redo_correct=1
  - `PUT /api/errors/{id}/master`：标记已掌握（is_mastered=1）
  - `DELETE /api/errors/{id}`：从错题本移除
  - `GET /api/errors/stats`：错题统计，按科目/知识点分组返回错题数量分布
- [ ] 任务3：创建 `module/errorbook/ErrorAiService.java`：
  - AI 深度解析 Prompt：输入题目内容+正确答案+用户错误答案 → 输出 `{errorReason, keyPoints, furtherExample, tips}`
  - 解析结果写回 t_error_book.ai_analysis（JSON 存储）
- [ ] 任务4：实现薄弱知识点统计 `GET /api/errors/weak-points`：查询错误次数 ≥ 2 且未掌握的知识点，按错误次数降序

### 后端 - 复习中心模块
- [ ] 任务5：创建 `entity/ReviewOutline.java`
- [ ] 任务6：创建 `module/review/ReviewController.java`：
  - `POST /api/review/outline`：接收 `{noteIds:[], title?}`
    - 查出所有笔记内容，拼接后调用 AI 生成提纲（Markdown 格式）
    - 存入 t_review_outline，返回提纲 ID 和内容
  - `POST /api/review/predict`：接收 `{subjectId, noteIds?[]}`
    - AI 基于科目特点（和可选笔记内容）预测高频考点，返回 `{hotPoints:[], mustMemory:[], focusTopics:[]}`
  - `GET /api/review/outlines`：当前用户历史提纲列表
  - `GET /api/review/outlines/{id}`：提纲详情
  - `DELETE /api/review/outlines/{id}`：删除提纲
  - `GET /api/review/weak-points`：同错题本薄弱点（复用 ErrorBook 统计，从 Review 模块也可访问）
- [ ] 任务7：在 `resources/prompts/` 创建：
  - `review_outline.txt`：复习提纲 Prompt（要求二级标题结构，每节含核心概念+例题提示）
  - `review_predict.txt`：考点预测 Prompt（基于公考出题规律，分必背/高频/了解三个优先级）

### 前端 - 错题本模块
- [ ] 任务8：创建 `src/api/errorbook.ts`
- [ ] 任务9：创建 `views/errorbook/ErrorListView.vue`（错题列表）：
  - 分科目 Tab 切换或下拉筛选
  - 错题卡片：题目摘要/错误次数/知识点标签/掌握状态
  - 顶部统计：总错题数/已掌握数/待攻克数
  - 每题操作：查看解析/重做/标记已掌握/删除
- [ ] 任务10：创建 `views/errorbook/ErrorDetailView.vue`（错题详情）：
  - 完整题目内容 + 选项
  - 我的答案（标红）vs 正确答案（标绿）
  - AI 深度解析卡片（分 4 区块：错误原因/核心考点/举一反三/记忆技巧）
  - "重做此题"按钮
- [ ] 任务11：创建 `views/errorbook/ErrorRedoView.vue`（错题重做）：复用 QuizAnswerView 逻辑，单题作答

### 前端 - 复习中心模块
- [ ] 任务12：创建 `src/api/review.ts`
- [ ] 任务13：创建 `views/review/ReviewCenterView.vue`（复习中心首页）：
  - 顶部两个功能卡：生成复习提纲 / AI 考点预测
  - 薄弱知识点列表（红色警告样式）
  - 历史提纲列表
- [ ] 任务14：创建 `views/review/OutlineGenerateView.vue`（生成提纲）：
  - 多选框选择要基于哪些笔记（展示笔记列表）
  - 可选填写提纲标题
  - 点击生成，loading 动画，完成后展示提纲（Markdown 渲染）
  - 支持复制提纲全文
- [ ] 任务15：创建 `views/review/PredictView.vue`（考点预测）：
  - 选择科目、可选关联笔记
  - 结果分 3 列展示：必背内容/高频考点/了解即可

## 验收标准
- [ ] 错题本列表正确展示 Phase 3 答错的题目，科目筛选功能正常
- [ ] `GET /api/errors/{id}` 首次调用触发 AI 分析，二次调用直接返回缓存结果，响应 < 300ms
- [ ] "标记已掌握"后，该题从默认列表消失（加 `isMastered=false` 筛选），切换"已掌握"Tab 可见
- [ ] 选择 2 篇以上笔记，调用生成提纲，30s 内返回有意义的 Markdown 提纲
- [ ] 考点预测返回结构化的三分类考点，不为空
- [ ] 薄弱知识点接口正确统计错误次数 ≥ 2 的知识点

## 注意事项
- **AI 分析缓存**：错题 AI 解析生成后存库，不重复调用（`ai_analysis IS NOT NULL` 直接返回）
- **提纲内容过长**：如果用户选了 10+ 篇笔记，拼接内容可能超过 DeepSeek 上下文限制（32K tokens）；需在 NoteAiService 中裁剪，每篇笔记取前 500 字，总共不超过 6000 字
- **重做答案不入错题本**：重做正确只更新 `last_redo_correct`，不会再次写入错题本，避免数据混乱
- **提纲生成是重操作**：前端需禁用重复点击（v-loading），后端加防抖（同一用户同一秒只能触发一次）
---
