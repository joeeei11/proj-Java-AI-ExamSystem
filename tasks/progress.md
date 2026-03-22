# 项目进度快照

## 总体进度

| 阶段 | 名称 | 状态 | 完成日期 |
|------|------|------|----------|
| Phase 1  | 项目脚手架与基础设施 | ✅ 已完成 | 2026-03-17 |
| Phase 2  | 用户认证与科目管理 | ✅ 已完成 | 2026-03-17 |
| Phase 3  | AI 刷题核心模块 | ✅ 已完成 | 2026-03-17 |
| Phase 4  | 智能笔记与 OCR | ✅ 已完成 | 2026-03-17 |
| Phase 5  | 错题本与 AI 复习中心 | ✅ 已完成 | 2026-03-17 |
| Phase 6  | 数据统计与可视化 | ✅ 已完成 | 2026-03-17 |
| Phase 7  | 管理员模块与系统优化 | ✅ 已完成 | 2026-03-18 |
| Phase 8  | 模考模式 | ✅ 已完成 | 2026-03-18 |
| Phase 9  | 间隔复习引擎（错因标签+抽认卡） | ✅ 已完成 | 2026-03-18 |
| Phase 10 | 题目纠错反馈 | ✅ 已完成 | 2026-03-18 |

## 当前执行阶段
**全部完成** - Phase 1~10 已全部完成

## Phase 3 完成清单（2026-03-17）

### 后端
- [x] `common/ai/DeepSeekClient.java` - OkHttp 调用 DeepSeek Chat API，30s 超时，最多 2 次重试，要求 JSON 响应格式
- [x] `entity/Question.java` - 题目实体（对应 t_question）
- [x] `entity/UserAnswer.java` - 用户答题记录实体
- [x] `entity/ErrorBook.java` - 错题本实体（含 SRS 字段）
- [x] `module/quiz/QuestionMapper.java` - 含 batchInsert
- [x] `module/quiz/UserAnswerMapper.java`
- [x] `module/errorbook/ErrorBookMapper.java` - 含 upsertError（ON DUPLICATE KEY UPDATE 防重）
- [x] `module/quiz/dto/GenerateRequest.java`
- [x] `module/quiz/dto/QuestionVO.java`
- [x] `module/quiz/dto/SubmitRequest.java`
- [x] `module/quiz/dto/SubmitResult.java`
- [x] `module/quiz/QuizService.java` - generate（缓存+AI出题）、submit（判分+写错题+异步统计）
- [x] `module/quiz/QuizController.java` - POST /api/quiz/generate、/api/quiz/submit
- [x] `resources/prompts/quiz_generate.txt` - 行测出题 Prompt 模板
- [x] `resources/mapper/ErrorBookMapper.xml` - upsertError SQL
- [x] `resources/mapper/QuestionMapper.xml` - batchInsert SQL
- [x] `module/user/StudyStatMapper.java` - 新增 upsertAnswerStat
- [x] `ExamAiApplication.java` - 新增 @EnableAsync

### 前端
- [x] `src/api/quiz.ts` - generateQuestions、submitAnswers，完整类型定义
- [x] `src/components/QuestionCard.vue` - 单/多选/判断/填空题卡片，支持答题态/结果态，含解析展开
- [x] `src/views/quiz/QuizHome.vue` - 三阶段流程（config→answering→result），带成绩环形图

## Phase 2 完成清单（2026-03-17）

### 后端
- [x] `entity/User.java` - 含 email、studyStreak 字段
- [x] `entity/Subject.java` / `entity/KnowledgePoint.java` / `entity/StudyStat.java`
- [x] `common/utils/JwtUtil.java` - JWT 生成/解析/校验（HS256）
- [x] `common/utils/MinioUtil.java` - 文件上传/删除/URL 构建
- [x] `config/PasswordEncoderConfig.java` - BCrypt 独立配置（解决循环依赖）
- [x] `config/MetaObjectHandlerConfig.java` - MyBatis-Plus 自动填充处理器
- [x] `module/auth/UserMapper.java` - 含 updateLastLoginAt、updateStudyStreak
- [x] `module/user/StudyStatMapper.java` - 含 upsertCheckin、countCheckin
- [x] `module/admin/SubjectMapper.java` / `KnowledgePointMapper.java`
- [x] `module/auth/dto/` - RegisterRequest、LoginRequest、LoginResponse
- [x] `module/user/dto/` - ProfileResponse、UpdateProfileRequest、ChangePasswordRequest
- [x] `module/admin/dto/` - SubjectRequest、KnowledgePointRequest
- [x] `module/auth/AuthService.java` - 注册/登录/注销（Redis 黑名单）
- [x] `module/auth/JwtAuthenticationFilter.java` - JWT 解析+黑名单校验+SecurityContext 注入
- [x] `module/auth/AuthController.java` - POST /api/auth/{register,login,logout}
- [x] `module/user/UserService.java` - 个人资料、头像上传、打卡（Redis 分布式锁+SRS连续天数）
- [x] `module/user/UserController.java` - /api/user/{profile,avatar,check-in}、/api/auth/password
- [x] `module/admin/SubjectService.java` / `SubjectAdminController.java` - CRUD（需 ADMIN 角色）
- [x] `module/subject/SubjectController.java` - GET /api/subjects、/api/subjects/{id}/knowledge-points（公开）
- [x] `config/SecurityConfig.java` - 注入 JWT 过滤器，自定义 401/403 JSON 响应
- [x] `sql/schema.sql` - t_user 新增 email、study_streak 字段（含 Phase 1 升级注释）
- [x] `common/result/ResultCode.java` - 新增 CHECKIN_DUPLICATE(409)

### 前端
- [x] `src/api/auth.ts` - register、login、logout API
- [x] `src/api/user.ts` - getProfile、updateProfile、changePassword、uploadAvatar、checkIn
- [x] `src/api/subject.ts` - getSubjects、getKnowledgePoints
- [x] `src/stores/auth.ts` - 新增 login、logout、fetchProfile action
- [x] `src/views/auth/Login.vue` - 更新为调用 authStore.login()
- [x] `src/components/layout/AppLayout.vue` - 更新为调用 authStore.logout()
- [x] `src/views/user/ProfileView.vue` - 头像上传、昵称邮箱编辑、修改密码、打卡
- [x] `src/router/index.ts` - 新增 /profile 路由

## Phase 4 完成清单（2026-03-17）

### 后端
- [x] `entity/Note.java` - 笔记实体（对应 t_note）
- [x] `module/note/NoteMapper.java` - BaseMapper<Note>
- [x] `module/note/dto/NoteCreateRequest.java`
- [x] `module/note/dto/NoteUpdateRequest.java`
- [x] `module/note/dto/NoteVO.java` - 含 subjectName 字段
- [x] `common/utils/OcrClient.java` - OkHttp 调用 PaddleOCR Flask 微服务，30s 超时，失败抛 OCR_FAILED
- [x] `resources/prompts/note_organize.txt` - AI 整理笔记 Prompt 模板
- [x] `module/note/NoteService.java` - list/get/create/update/delete/ocr/organizeByAI
- [x] `module/note/NoteController.java` - REST 接口（含 multipart OCR 上传）

### 前端
- [x] `src/api/note.ts` - 完整类型定义 + 7 个 API 函数
- [x] `src/components/MarkdownViewer.vue` - 轻量 Markdown 渲染组件（零外部依赖）
- [x] `src/views/notes/NoteList.vue` - 卡片列表（科目过滤/搜索/新建对话框）
- [x] `src/views/notes/NoteDetail.vue` - 详情页（编辑/AI整理/OCR上传/Markdown预览）
- [x] `src/router/index.ts` - 新增 /notes/:id 路由

## 最近更新
- 2026-03-17：Phase 1 全部完成，项目骨架搭建完毕
- 2026-03-17：Phase 2 全部完成，认证鉴权体系 + 用户中心 + 科目管理完毕
- 2026-03-17：Phase 3 全部完成，AI 刷题核心模块（DeepSeekClient + 缓存 + 出题 + 判分 + 错题录入）
- 2026-03-17：Phase 4 全部完成，智能笔记与 OCR（手动/OCR/AI整理，OcrClient，MarkdownViewer）
- 2026-03-17：Phase 5 全部完成，错题本（错因标签+SRS复做）与 AI 复习提纲中心

## Phase 5 完成清单（2026-03-17）

### 后端
- [x] `module/errorbook/dto/ErrorBookVO.java` - 含题目信息+SRS 字段的错题本 VO
- [x] `module/errorbook/dto/SetReasonRequest.java` - 错因标签请求（reason: 1-6）
- [x] `module/errorbook/dto/ReviewRequest.java` - 复做结果请求（known: boolean）
- [x] `module/common/SrsService.java` - 共用 SRS 算法（错题本+抄认卡），无状态工具类
- [x] `module/errorbook/ErrorBookMapper.java` - 新增 selectByUserId、selectTodayReview
- [x] `resources/mapper/ErrorBookMapper.xml` - 新增 JOIN 查询 SQL（展示题目+科目）
- [x] `module/errorbook/ErrorBookService.java` - list/setReason/todayReview/submitReview
- [x] `module/errorbook/ErrorBookController.java` - 4 个 REST 接口
- [x] `entity/ReviewOutline.java` - 复习提纲实体
- [x] `module/review/ReviewOutlineMapper.java` - BaseMapper<ReviewOutline>
- [x] `module/review/dto/GenerateOutlineRequest.java`
- [x] `module/review/dto/ReviewOutlineVO.java`
- [x] `resources/prompts/review_outline.txt` - AI 生成提纲 Prompt 模板
- [x] `module/review/ReviewService.java` - generateOutline/listOutlines/deleteOutline
- [x] `module/review/ReviewController.java` - 3 个 REST 接口
- [x] `common/result/ResultCode.java` - 新增错题本和复习提纲相关错误码

### 前端
- [x] `src/api/errorbook.ts` - 错题本接口封装，含错因标签映射表
- [x] `src/api/review.ts` - 复习提纲接口封装
- [x] `src/views/errorbook/ErrorBook.vue` - 错题列表（科目过滤/错因设置/SRS进度）+今日复习入口
- [x] `src/views/errorbook/components/TodayReview.vue` - 今日复做抑屉内组件（逐题展示/揀示/不会/会了）
- [x] `src/views/review/ReviewCenter.vue` - 选笔记生成提纲+提纲列表+Markdown详情

## Phase 6 完成清单（2026-03-17）

### 后端
- [x] `module/stats/dto/StatsOverviewVO.java` - 总览VO（总答题/正确率/学习天数/连续打卡/错题掌握率）
- [x] `module/stats/dto/DailyStatVO.java` - 日度统计VO（日期/答题数/正确数/正确率）
- [x] `module/stats/dto/SubjectStatVO.java` - 科目统计VO
- [x] `module/stats/StatsMapper.java` - 5个查询方法（总览/日度/科目/错题总数/已掌握数）
- [x] `resources/mapper/StatsMapper.xml` - 对应 SQL（t_study_stat聚合、t_user_answer JOIN科目、错题本统计）
- [x] `module/stats/StatsService.java` - getOverview/getDailyStats（补零）/getSubjectStats，calcAccuracy 工具方法
- [x] `module/stats/StatsController.java` - GET /api/stats/{overview,daily,subjects}

### 前端
- [x] `src/api/stats.ts` - 3个接口封装，含完整 TS 类型定义
- [x] `src/views/stats/StatsDashboard.vue` - 概览卡片5张+ECharts折线图/横向柱状图/饼图，ResizeObserver响应式

## 最近更新
- 2026-03-17：Phase 6 全部完成，数据统计可视化（ECharts 趋势/科目/错因图表）
- 2026-03-18：Phase 7 全部完成，管理员模块（用户分页/搜索/启禁用）+ 系统统计总览 + 健康检查（已存在）

## Phase 7 完成清单（2026-03-18）

### 后端
- [x] `module/admin/dto/UserAdminVO.java` - 管理员查询用户 VO
- [x] `module/admin/dto/UpdateUserStatusRequest.java` - 启用/禁用请求 DTO
- [x] `module/admin/dto/SystemStatsVO.java` - 系统统计总览 VO
- [x] `module/admin/AdminStatsMapper.java` - 跨表 COUNT 查询（全平台答题总数/今日活跃用户）
- [x] `module/admin/AdminUserService.java` - 分页查询（LambdaQueryWrapper）+ 状态变更
- [x] `module/admin/AdminStatsService.java` - 复用现有 Mapper 自动计数
- [x] `module/admin/AdminUserController.java` - GET /api/admin/users、PUT /api/admin/users/{id}/status
- [x] `module/admin/AdminStatsController.java` - GET /api/admin/stats
- [x] `module/health/HealthController.java` - 已存在，无需修改

### 前端
- [x] `src/api/admin.ts` - 3 个 API 函数 + 完整 TS 类型定义
- [x] `views/admin/AdminDashboard.vue` - 统计卡片组（5张）+ 用户表格（分页/搜索/启禁用 Popconfirm）

## Phase 8 完成清单（2026-03-18）

### 后端
- [x] `entity/MockExam.java` - 模考记录实体（对应 t_mock_exam）
- [x] `entity/MockExamQuestion.java` - 模考题目明细实体（对应 t_mock_exam_question）
- [x] `module/mock/MockExamMapper.java` - 含 batchMarkTimeout（批量超时）、selectHistoryByUserId
- [x] `module/mock/MockExamQuestionMapper.java` - 含 selectByExamId（按序号排序）、toggleFlag
- [x] `module/mock/dto/MockStartRequest.java` - 开始模考请求（subjectId 必填，count 5-20）
- [x] `module/mock/dto/MockExamVO.java` - 答题视图（含剩余秒数，题目不含答案）
- [x] `module/mock/dto/FlagRequest.java` - 标记/取消标记请求
- [x] `module/mock/dto/MockSubmitRequest.java` - 交卷请求
- [x] `module/mock/dto/MockReportVO.java` - 模考报告（含完整答案/解析）
- [x] `module/mock/dto/MockHistoryVO.java` - 历史列表项 VO
- [x] `module/mock/MockService.java` - start/getExam（服务端超时检测）/flag/submit/getReport/listHistory + @Scheduled 定时超时扫描
- [x] `module/mock/MockController.java` - 6 个 REST 接口
- [x] `resources/mapper/MockExamMapper.xml` - selectHistoryByUserId SQL
- [x] `resources/mapper/MockExamQuestionMapper.xml` - selectByExamId、toggleFlag SQL

### 前端
- [x] `src/api/mock.ts` - 6 个 API 函数 + 完整 TS 类型定义
- [x] `src/views/mock/MockHome.vue` - 配置面板（科目/难度/题数/时长）+ 历史记录列表
- [x] `src/views/mock/MockAnswer.vue` - 答题主界面（左侧题目 + 右侧题号导航面板 + 顶部倒计时 + 30s 轮询同步）
- [x] `src/views/mock/MockReport.vue` - 报告（ECharts 环形图 + 逐题解析含高亮选项）
- [x] `src/router/index.ts` - 新增 /mock/answer/:examId、/mock/report/:examId 路由

## 最近更新
- 2026-03-18：Phase 8 全部完成，模考模式（限时/题号导航/倒计时/服务端超时检测/模考报告）
- 2026-03-18：Phase 9 全部完成，抽认卡全功能（AI生成/卡组管理/3D翻转/SRS每日复习）

## Phase 9 完成清单（2026-03-18）

### 后端
- [x] `entity/FlashcardDeck.java` - 抽认卡组实体（对应 t_flashcard_deck）
- [x] `entity/Flashcard.java` - 抽认卡实体（对应 t_flashcard，含 SRS 字段）
- [x] `module/flashcard/FlashcardDeckMapper.java` - BaseMapper<FlashcardDeck>
- [x] `module/flashcard/FlashcardMapper.java` - 含 selectTodayReview（SRS 到期查询）
- [x] `module/flashcard/dto/GenerateFlashcardRequest.java` - noteId
- [x] `module/flashcard/dto/FlashcardDeckVO.java` - 含 todayReviewCount
- [x] `module/flashcard/dto/FlashcardVO.java` - 含 SRS 字段
- [x] `module/flashcard/dto/FlashcardReviewRequest.java` - known: boolean
- [x] `module/flashcard/dto/UpdateCardRequest.java` - front/back
- [x] `module/flashcard/FlashcardService.java` - generateFromNote/listDecks/getDeckDetail/deleteDeck/updateCard/todayReview/submitReview（复用 SrsService）
- [x] `module/flashcard/FlashcardController.java` - 7 个 REST 接口
- [x] `resources/prompts/flashcard_generate.txt` - AI 生成抽认卡 Prompt 模板（要求 JSON）
- [x] `resources/mapper/FlashcardMapper.xml` - selectTodayReview SQL（SRS 到期 + 新卡优先）

### 前端
- [x] `src/api/flashcard.ts` - 7 个 API 函数 + 完整 TS 类型定义
- [x] `src/views/flashcards/DeckList.vue` - 卡组列表（今日复习 Badge/从笔记生成/删除 Popconfirm/卡片数展示）
- [x] `src/views/flashcards/CardStudy.vue` - 卡组学习（3D flip 动画/会了-不会/进度条/完成汇总）
- [x] `src/views/flashcards/DailyReview.vue` - 今日复习（SRS 到期卡片逐一复习/SVG 掌握率环形图/完成总结）
- [x] `src/router/index.ts` - 新增 /flashcards/decks/:id、/flashcards/daily-review 路由

## Phase 10 完成清单（2026-03-18）

### 后端
- [x] `entity/QuestionFeedback.java` - 纠错反馈实体（对应 t_question_feedback）
- [x] `module/feedback/dto/SubmitFeedbackRequest.java` - 提交反馈请求 DTO
- [x] `module/feedback/dto/FeedbackVO.java` - 反馈列表项 VO、`FeedbackDetailVO.java` - 详情 VO
- [x] `module/feedback/dto/HandleFeedbackRequest.java` - 管理员处理请求 DTO
- [x] `module/feedback/FeedbackMapper.java` - 含 selectByUserId、selectAllForAdmin 分页查询
- [x] `resources/mapper/FeedbackMapper.xml` - 分页查询 SQL（JOIN t_question 获取题目摘要）
- [x] `module/common/NoticeService.java` - Redis List 站内通知封装（push/getAll/clear）
- [x] `module/feedback/FeedbackService.java` - submit/listMy/getMy/listAll/handle（含 Redis 通知）
- [x] `module/feedback/FeedbackController.java` - 用户端 3 接口 + 管理员端 2 接口

### 前端
- [x] `src/api/feedback.ts` - 5 个 API 函数 + 完整 TS 类型 + 常量映射
- [x] `src/views/feedback/MyFeedback.vue` - 用户反馈列表/详情/提交对话框
- [x] `src/views/admin/AdminFeedback.vue` - 管理员反馈列表（Tab 过滤） + 处理对话框
- [x] `src/views/quiz/QuizHome.vue` - 结果面板新增纠错反馈入口＋对话框
- [x] `src/views/admin/AdminDashboard.vue` - 新增快捷功能卡入口
- [x] `src/router/index.ts` - 新增 /admin/feedback 路由
