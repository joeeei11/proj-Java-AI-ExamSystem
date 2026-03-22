---
# Phase 3：AI 刷题核心模块

## 目标
用户可选择科目/难度/知识点，由 AI 自动出题，在线答题提交后立即获得判分和 AI 解析，做错的题目自动进入错题本。

## 前置条件
- Phase 2 全部验收通过
- DeepSeek API Key 已配置到 `.env`
- 科目和知识点数据已存在

## 任务清单

### 后端 - AI 调用基础层
- [ ] 任务1：创建 `common/ai/DeepSeekClient.java`：
  - 封装 OkHttp 调用 DeepSeek Chat Completion API
  - 支持普通调用 `chat(systemPrompt, userPrompt)` 返回 String
  - 超时设置：connectTimeout=10s, readTimeout=30s
  - 失败自动重试 2 次（指数退避）
- [ ] 任务2：在 `resources/prompts/` 目录下创建 Prompt 模板文件：
  - `quiz_generate.txt`：出题提示词（要求返回 JSON 数组，含题目/选项/答案/解析/知识点）
  - `quiz_analyze.txt`：题目解析提示词（用户答案+正确答案 → 生成易错分析+考点说明）

### 后端 - 刷题模块
- [ ] 任务3：创建实体 `entity/Question.java`、`entity/UserAnswer.java`
- [ ] 任务4：创建 `module/quiz/QuizController.java`：
  - `POST /api/quiz/generate`：接收 `{subjectId, difficulty, count(1-10), knowledgePointIds[]}`
    - 先查 Redis 缓存（key: `quiz:cache:{subjectId}:{difficulty}:{kpIds_hash}`）
    - 缓存命中直接返回，未命中调用 AI，结果存库（t_question）并缓存 24h
    - 返回 `{sessionId, questions:[{id,content,options,type}]}`（不含答案）
  - `POST /api/quiz/submit`：接收 `{sessionId, answers:[{questionId, answer}]}`
    - 判分（选择题/判断题对比答案，简答题 AI 评分）
    - 批量写入 `t_user_answer`
    - 错误题目自动写入 `t_error_book`（INSERT ... ON DUPLICATE KEY UPDATE）
    - 异步更新 `t_study_stat` 当天统计
    - 返回每题是否正确、正确答案、AI 解析
  - `GET /api/quiz/history`：当前用户历史做题列表（按 session_id 分组，返回日期/正确率/科目）
  - `GET /api/quiz/session/{sessionId}`：某次做题全部题目+用户答案+解析
  - `POST /api/quiz/question/{questionId}/collect`：收藏/取消收藏题目（t_question_collect 表）
- [ ] 任务5：实现 `module/quiz/QuizCacheService.java`：Redis 缓存读写、缓存 key 生成（MD5 知识点 ID 组合）
- [ ] 任务6：实现 `module/quiz/QuizAiService.java`：AI 出题调用、Prompt 渲染、JSON 响应解析（用 Jackson），校验每道题格式

### 前端 - 刷题模块
- [ ] 任务7：创建 `src/api/quiz.ts`：封装出题/提交/历史接口
- [ ] 任务8：创建 `views/quiz/QuizSetupView.vue`（出题配置页）：
  - 科目下拉选择、难度单选（易/中/难）、题量滑块（1-10）
  - 知识点多选（根据科目动态加载）
  - 点击"开始答题"调用 `/api/quiz/generate`，loading 状态展示
- [ ] 任务9：创建 `views/quiz/QuizAnswerView.vue`（答题页）：
  - 每题展示题目内容 + 选项（单选/多选/判断用 el-radio/el-checkbox）
  - 顶部进度条（第 X / 共 Y 题）
  - 底部"上一题/下一题/提交"按钮
  - 简答题用 `el-input` 文本域
- [ ] 任务10：创建 `views/quiz/QuizResultView.vue`（结果页）：
  - 展示本次得分/正确率
  - 每题卡片：用户答案 vs 正确答案，对/错标识，AI 解析（支持 Markdown 渲染）
  - "再做一次"和"查看错题本"按钮
- [ ] 任务11：创建 `views/quiz/QuizHistoryView.vue`（历史记录页）：
  - 列表展示每次做题记录（日期/科目/正确率/题数），点击查看详情

## 验收标准
- [ ] `POST /api/quiz/generate {subjectId:1, difficulty:2, count:5}` 返回 5 道题，格式正确，AI 解析不为空
- [ ] 相同参数第二次调用，响应时间 < 100ms（命中 Redis 缓存）
- [ ] `POST /api/quiz/submit` 提交答案后，错题自动出现在 `GET /api/errors` 返回列表中
- [ ] 前端从配置页到答题页到结果页全流程可正常走通
- [ ] 简答题 AI 评分有合理反馈（说明答题是否切题）
- [ ] AI 调用失败时，前端显示友好错误提示，不白屏

## 注意事项
- **Prompt 设计**：出题时必须在 Prompt 中明确要求返回 JSON 数组格式，并给出 few-shot 示例（1 道示例题），否则 AI 输出极不稳定
- **AI 评分简答题**：不要让 AI 给百分制分数，改为返回 `{isCorrect: boolean, feedback: "...", keyPoints: []}` 结构，更易解析
- **题目去重**：相同 session 内 AI 可能生成重复题目，生成后需按 `content` 去重，不足数量时补充调用
- **sessionId 生成**：后端用 UUID，存 Redis（key: `quiz:session:{uuid}`, value: `{userId, questionIds}`, TTL 2h），提交时验证 sessionId 归属该用户
- **收藏表**：`t_question_collect(user_id, question_id, created_at)`，需唯一索引
---
