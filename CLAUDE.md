# 公考 AI 智能刷题笔记系统 (hnust-exam-ai)

## 项目概述

面向公务员考试备考人群的 AI 全链路辅助学习平台，集 AI 智能出题、自动解析、图片笔记 OCR 整理、错题深度分析、复习提纲生成、学习进度可视化于一体。基于 SpringBoot 3 + Vue3 全栈架构，通过 DeepSeek API 驱动核心 AI 功能，解决传统刷题软件无智能分析、笔记散乱无序、复习无针对性三大痛点。

## 技术栈

### 后端
- Java 17 (LTS)
- Spring Boot 3.2.x
- Spring Security 6.x + JWT (jjwt 0.12.x)
- MyBatis-Plus 3.5.x
- MySQL 8.0
- Redis 7.2
- MinIO (最新稳定版，文件存储)
- Knife4j 4.x (API 文档)
- Lombok、MapStruct 1.5.x
- Maven 3.9.x

### 前端
- Node 20.x (LTS)
- Vue 3.4.x
- Vite 5.x
- Element Plus 2.x
- Pinia 2.x (状态管理)
- Vue Router 4.x
- ECharts 5.x (数据可视化)
- Axios 1.x
- VueUse (组合式工具库)

### AI / OCR 服务
- DeepSeek API v3 (主力 AI)
- PaddleOCR 2.x (Python 3.10 + Flask 3.x 微服务)
- OkHttp 4.x (SpringBoot 调用 OCR 服务)

### 部署
- Docker 24.x
- Docker Compose 2.x
- Nginx 1.25 (前端静态托管 + 反向代理)

## 目录结构

```
hnust-exam-ai/
├── backend/                          # SpringBoot 后端
│   ├── pom.xml
│   └── src/main/
│       ├── java/com/hnust/examai/
│       │   ├── ExamAiApplication.java
│       │   ├── common/
│       │   │   ├── result/           # R<T> 统一响应体
│       │   │   ├── exception/        # 全局异常处理
│       │   │   ├── ai/               # AI 调用封装 (DeepSeekClient)
│       │   │   └── utils/            # JwtUtil、RedisUtil、FileUtil
│       │   ├── config/
│       │   │   ├── SecurityConfig.java
│       │   │   ├── RedisConfig.java
│       │   │   ├── MinioConfig.java
│       │   │   └── SwaggerConfig.java
│       │   ├── module/
│       │   │   ├── auth/             # 注册/登录/注销
│       │   │   ├── user/             # 个人信息/打卡
│       │   │   ├── quiz/             # 刷题/AI出题/判分
│       │   │   ├── note/             # 笔记/OCR/AI整理
│       │   │   ├── review/           # 复习提纲/考点预测
│       │   │   ├── errorbook/        # 错题本（含错因标签、间隔复习）
│       │   │   ├── mock/             # 模考模式
│       │   │   ├── flashcard/        # 抽认卡
│       │   │   ├── feedback/         # 题目纠错反馈
│       │   │   ├── stats/            # 学习统计
│       │   │   └── admin/            # 管理员
│       │   │   └── common/           # SrsService（间隔复习算法，错题+抽认卡共用）
│       │   └── entity/               # 所有 JPA/MyBatis 实体
│       └── resources/
│           ├── application.yml
│           ├── application-dev.yml
│           ├── application-prod.yml
│           ├── prompts/              # AI Prompt 模板（*.txt）
│           └── mapper/               # MyBatis XML
├── frontend/                         # Vue3 前端
│   ├── package.json
│   ├── vite.config.ts
│   ├── index.html
│   └── src/
│       ├── main.ts
│       ├── App.vue
│       ├── api/                      # 所有 Axios 接口封装，按模块分文件
│       ├── components/               # 公共组件 (MarkdownViewer、QuestionCard 等)
│       ├── views/
│       │   ├── auth/                 # Login.vue、Register.vue
│       │   ├── dashboard/            # 首页看板
│       │   ├── quiz/                 # 刷题相关页面
│       │   ├── notes/                # 笔记页面
│       │   ├── review/               # 复习中心
│       │   ├── errorbook/            # 错题本
│       │   ├── mock/                 # 模考模式（MockSetup、MockAnswer、MockReport）
│       │   ├── flashcards/           # 抽认卡（DeckList、CardStudy、DailyReview）
│       │   ├── feedback/             # 我的反馈
│       │   ├── stats/                # 统计图表
│       │   └── admin/                # 管理后台
│       ├── router/
│       │   └── index.ts
│       ├── stores/                   # Pinia stores (auth、quiz、note)
│       └── utils/                    # request.ts(Axios封装)、format.ts
├── ocr-service/                      # PaddleOCR Python 微服务
│   ├── app.py                        # Flask 服务入口，POST /ocr
│   ├── requirements.txt
│   └── Dockerfile
├── docker-compose.yml
├── docker-compose.dev.yml
├── nginx.conf
├── .env.example                      # 环境变量示例
├── tasks/
│   ├── current.md                    # 当前执行任务
│   ├── progress.md                   # 进度快照
│   └── decisions.md                  # 技术决策记录
└── CLAUDE.md
```

## 环境变量

项目根目录创建 `.env` 文件（勿提交 Git）：

```env
# ===== 数据库 =====
DB_HOST=localhost
DB_PORT=3306
DB_NAME=exam_ai
DB_USERNAME=root
DB_PASSWORD=your_mysql_password

# ===== Redis =====
REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_PASSWORD=

# ===== JWT =====
JWT_SECRET=your_jwt_secret_at_least_32_chars_long
JWT_EXPIRE_HOURS=72

# ===== DeepSeek AI =====
DEEPSEEK_API_KEY=sk-xxxxxxxxxxxxxxxxxxxxxxxx
DEEPSEEK_BASE_URL=https://api.deepseek.com
DEEPSEEK_MODEL=deepseek-chat
DEEPSEEK_MAX_TOKENS=4096

# ===== MinIO 文件存储 =====
MINIO_ENDPOINT=http://localhost:9000
MINIO_ACCESS_KEY=minioadmin
MINIO_SECRET_KEY=minioadmin
MINIO_BUCKET=exam-ai

# ===== OCR 微服务 =====
OCR_SERVICE_URL=http://localhost:5001

# ===== 应用配置 =====
APP_PORT=8080
FRONTEND_URL=http://localhost:5173
```

## 启动方式

### 方式一：Docker Compose 一键启动（推荐）
```bash
# 复制环境变量文件
cp .env.example .env
# 编辑 .env 填入真实配置

# 启动所有服务（MySQL、Redis、MinIO、OCR服务、后端、前端）
docker compose up -d

# 查看日志
docker compose logs -f backend
```

### 方式二：本地开发分步启动
```bash
# 1. 启动基础设施（MySQL + Redis + MinIO）
docker compose -f docker-compose.dev.yml up -d

# 2. 启动 OCR 微服务
cd ocr-service
pip install -r requirements.txt
python app.py

# 3. 启动后端
cd backend
mvn spring-boot:run -Dspring-boot.run.profiles=dev

# 4. 启动前端
cd frontend
npm install
npm run dev
# 访问 http://localhost:5173
```

### 初始化数据库
```bash
# SpringBoot 启动时自动执行 resources/sql/schema.sql 和 data.sql
# 或手动执行：
mysql -u root -p exam_ai < backend/src/main/resources/sql/schema.sql
mysql -u root -p exam_ai < backend/src/main/resources/sql/data.sql
```

## API 规范

**Base URL：** `http://localhost:8080/api`

**认证：** 所有非 `/api/auth/**` 接口需携带 Header `Authorization: Bearer <token>`

**统一响应：**
```json
{ "code": 200, "message": "success", "data": {}, "timestamp": 1710000000000 }
```

**核心接口：**
```
# 认证
POST /api/auth/register         # 注册
POST /api/auth/login            # 登录
POST /api/auth/logout           # 注销

# 刷题
POST /api/quiz/generate         # AI出题 { subjectId, difficulty, count, knowledgePointIds[] }
POST /api/quiz/submit           # 提交答案 { sessionId, answers: [{questionId, answer}] }

# 笔记
POST /api/notes/ocr             # 图片OCR { imageBase64 }
POST /api/notes/{id}/organize   # AI整理笔记

# 复习
POST /api/review/outline        # 生成提纲 { noteIds[] }

# 错题本（含错因标签+间隔复习）
GET  /api/errors                    # 错题列表 ?subjectId=&isMastered=
PUT  /api/errors/{id}/reason        # 设置错因标签 { reason: 1-6 }
GET  /api/errors/today-review       # 今日待复做列表（SRS到期）
POST /api/errors/{id}/review        # 提交复做结果 { known: true/false }

# 模考模式
POST /api/mock/start                # 开始模考 { subjectId, count, timeLimitMinutes }
GET  /api/mock/{examId}             # 获取进行中模考（题目+剩余时间，无答案）
PUT  /api/mock/{examId}/flag        # 标记/取消标记 { questionId, flagged }
POST /api/mock/{examId}/submit      # 交卷 { answers: [{questionId, answer}] }
GET  /api/mock/{examId}/report      # 模考报告
GET  /api/mock/history              # 历史模考列表

# 抽认卡
POST /api/flashcards/generate       # AI生成卡片 { noteId }（返回 deckId）
GET  /api/flashcards/decks          # 卡组列表
GET  /api/flashcards/decks/{id}     # 卡组详情+所有卡片
DELETE /api/flashcards/decks/{id}   # 删除卡组
PUT  /api/flashcards/{cardId}       # 编辑单张卡片 { front, back }
GET  /api/flashcards/today-review   # 今日待复习卡片（SRS到期）
POST /api/flashcards/{cardId}/review  # 提交复习 { known: true/false }

# 题目纠错反馈
POST /api/feedback/question         # 提交反馈 { questionId, type, description, screenshotUrl? }
GET  /api/feedback/my               # 我的反馈列表
GET  /api/feedback/my/{id}          # 我的反馈详情
# 管理员
GET  /api/admin/feedback            # 全部反馈列表 ?status=&page=
PUT  /api/admin/feedback/{id}       # 处理反馈 { status, adminReply }

# 统计
GET  /api/stats/overview        # 总览数据
GET  /api/stats/daily?days=30   # 日度统计

# 管理员
GET  /api/admin/users?page=&size=
```

## 新增数据表（Phase 8-10）

```sql
-- 模考记录表
CREATE TABLE t_mock_exam (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id         BIGINT NOT NULL,
    subject_id      BIGINT,
    title           VARCHAR(100),           -- 自动生成，如"行测模考 #3"
    time_limit_min  INT NOT NULL,           -- 考试时长（分钟）
    total_questions INT NOT NULL,
    total_correct   INT DEFAULT 0,
    status          TINYINT DEFAULT 0,      -- 0:进行中 1:已完成 2:超时
    started_at      DATETIME DEFAULT NOW(),
    submitted_at    DATETIME
);

-- 模考题目明细
CREATE TABLE t_mock_exam_question (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    exam_id     BIGINT NOT NULL,
    question_id BIGINT NOT NULL,
    sort_order  INT NOT NULL,
    is_flagged  TINYINT DEFAULT 0,          -- 标记"未确定"
    user_answer TEXT,
    is_correct  TINYINT
);

-- 抽认卡组（一篇笔记可生成一个卡组）
CREATE TABLE t_flashcard_deck (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id     BIGINT NOT NULL,
    note_id     BIGINT,                     -- 来源笔记
    title       VARCHAR(200) NOT NULL,
    card_count  INT DEFAULT 0,
    created_at  DATETIME DEFAULT NOW()
);

-- 抽认卡（含 SRS 调度字段）
CREATE TABLE t_flashcard (
    id                  BIGINT PRIMARY KEY AUTO_INCREMENT,
    deck_id             BIGINT NOT NULL,
    user_id             BIGINT NOT NULL,
    front               TEXT NOT NULL,       -- 问题面
    back                TEXT NOT NULL,       -- 答案面
    sort_order          INT DEFAULT 0,
    -- SRS 字段
    next_review_at      DATETIME,            -- NULL 表示从未复习
    review_interval_days INT DEFAULT 1,      -- 当前间隔天数
    review_count        INT DEFAULT 0,       -- 总复习次数
    consecutive_correct INT DEFAULT 0,       -- 连续正确次数
    created_at          DATETIME DEFAULT NOW()
);

-- t_error_book 新增字段（ALTER TABLE）
-- error_reason TINYINT DEFAULT NULL  (1概念不清/2审题失误/3计算失误/4方法不会/5时间不足/6粗心)
-- next_review_at DATETIME DEFAULT NULL
-- review_interval_days INT DEFAULT 1
-- review_count INT DEFAULT 0
-- consecutive_correct INT DEFAULT 0

-- 题目纠错反馈
CREATE TABLE t_question_feedback (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id         BIGINT NOT NULL,
    question_id     BIGINT NOT NULL,
    type            TINYINT NOT NULL,        -- 1答案错误/2题干歧义/3解析不清/4排版问题
    description     TEXT,
    screenshot_url  VARCHAR(500),
    status          TINYINT DEFAULT 0,       -- 0待处理/1已采纳/2已驳回/3已修复
    admin_id        BIGINT,
    admin_reply     TEXT,
    replied_at      DATETIME,
    created_at      DATETIME DEFAULT NOW()
);
```

## SRS（间隔复习）算法规范

错题间隔复习和抽认卡共用同一套逻辑，统一封装在 `module/common/SrsService.java`：

```
// 用户点"会了"
nextInterval = min(ceil(currentInterval * 2.5), 60)  // 最长 60 天
consecutiveCorrect += 1

// 用户点"不会"
nextInterval = 1                                       // 重置为 1 天
consecutiveCorrect = 0

nextReviewAt = now() + nextInterval days
```

## 开发规范

### 命名规范
- **Java**：包名全小写，类名大驼峰，方法/变量小驼峰，常量全大写下划线
- **数据库**：表名 `t_` 前缀，字段全小写下划线，索引名 `idx_字段名`
- **Vue**：组件文件大驼峰 `QuestionCard.vue`，Composable `use` 前缀
- **API 接口**：REST 风格，动词用 HTTP 方法，资源用复数名词

### 代码风格
- 后端：Google Java Style，单行不超 120 字符，统一用 Lombok @Data
- 前端：ESLint + Prettier，2 空格缩进，单引号
- 所有 Service 方法写 Javadoc/JSDoc 注释
- 敏感操作（删除/AI调用）必须记录日志

### 提交规范（Conventional Commits）
```
feat: 新增AI出题功能
fix: 修复错题本重复录入问题
refactor: 重构AI服务调用层
docs: 更新 API 文档
test: 添加出题单元测试
```

### 前端 UI 规范
- **组件库（Element Plus）**：表单、表格、弹框、分页、导航等通用交互组件统一使用 Element Plus，不要绕过它自己实现
- **Impeccable skill**：已安装，用于自定义程度高的页面与组件（答题卡、抽认卡翻转、模考布局、仪表盘看板、错题分析卡等），目标是摆脱通用 AI 模板风格
- **边界原则**：两者不冲突——Element Plus 负责"结构与交互"，Impeccable 负责"视觉与氛围"；Impeccable 的配色和排版变量必须与 Element Plus CSS 变量（`--el-color-primary` 等）保持一致，不得各自为政
- **从 Phase 2 前端部分起统一执行**，不允许在后期阶段对早期页面补丁式修改风格

### AI 调用规范
- 所有 AI 调用统一走 `common/ai/DeepSeekClient.java`
- Prompt 模板统一存放在 `resources/prompts/` 目录下的 `.txt` 文件
- AI 响应必须要求 JSON 格式，并做 Schema 校验 + 最多 2 次重试

## 任务管理
- tasks/current.md    # 当前任务（每次 session 开始执行）
- tasks/progress.md   # 进度快照（每次 session 结束更新）
- tasks/decisions.md  # 技术决策（有新决策时追加）

**每次新 session 开头：**
读 CLAUDE.md、tasks/progress.md、tasks/decisions.md 和 tasks/current.md，执行当前任务。

**每次结束前：**
更新 tasks/progress.md（已完成/进行中/待做），如有新技术决策更新 tasks/decisions.md，然后结束。

## 禁止事项

- **禁止**直接操作生产数据库，所有 DDL 变更必须通过迁移脚本
- **禁止**在代码中硬编码 API Key、密码等敏感信息，必须走环境变量
- **禁止**删除 `t_user_answer`、`t_error_book`、`t_study_stat`、`t_mock_exam`、`t_flashcard`、`t_question_feedback` 表及其数据（用户学习历史与反馈记录）
- **禁止**未经确认修改 `.env` 文件内容
- **禁止**绕过 Spring Security 鉴权，不允许对 `/api/admin/**` 接口开放匿名访问
- **禁止**在前端 Store 中存储明文密码
- **禁止**直接 `git push --force` 到 main 分支
- **禁止** AI 调用超时时间超过 30 秒不做处理，必须有超时兜底响应
