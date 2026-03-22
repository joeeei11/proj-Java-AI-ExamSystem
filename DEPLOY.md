# 公考 AI 智能刷题笔记系统 — 环境与部署指南

## 一、环境要求

### 1.1 必备软件清单

| 软件 | 版本要求 | 用途 | 下载地址 |
|------|---------|------|---------|
| Docker Desktop | 24.x+ | 容器化运行所有服务 | https://www.docker.com/products/docker-desktop |
| Docker Compose | v2.x+（Docker Desktop 自带） | 多容器编排 | 同上 |
| Git | 2.40+ | 代码版本管理 | https://git-scm.com/downloads |

> **说明：** 采用 Docker 一键部署时，**无需**单独安装 JDK、Node.js、Python、MySQL、Redis 等，所有依赖均由容器提供。

### 1.2 本地开发额外需要（仅开发模式）

| 软件 | 版本要求 | 用途 |
|------|---------|------|
| JDK | 17 (LTS) | 后端编译运行 |
| Maven | 3.9+ | Java 依赖管理与构建 |
| Node.js | 20.x (LTS) | 前端构建与开发服务器 |
| Python | 3.10+ | OCR 微服务 |
| MySQL | 8.0 | 关系型数据库 |
| Redis | 7.x | 缓存与会话管理 |

### 1.3 硬件建议

| 项目 | 最低配置 | 推荐配置 |
|------|---------|---------|
| CPU | 2 核 | 4 核+ |
| 内存 | 4 GB | 8 GB+ |
| 磁盘 | 10 GB 可用空间 | 20 GB+（含 Docker 镜像） |
| 网络 | 可访问外网（DeepSeek API） | — |

### 1.4 外部服务账号

| 服务 | 必需 | 说明 |
|------|------|------|
| DeepSeek API Key | 是 | AI 出题/笔记整理/复习提纲/抽认卡的核心引擎 |

申请地址：https://platform.deepseek.com/

---

## 二、项目结构

```
hnust-exam-ai/
├── backend/                 # Spring Boot 3.2 后端
│   ├── Dockerfile
│   ├── pom.xml
│   └── src/
├── frontend/                # Vue 3.4 + Vite 5 前端
│   ├── Dockerfile
│   ├── package.json
│   └── src/
├── ocr-service/             # PaddleOCR Flask 微服务
│   ├── Dockerfile
│   ├── app.py
│   └── requirements.txt
├── docker-compose.yml       # 生产环境一键启动
├── docker-compose.dev.yml   # 开发环境（仅基础设施）
├── nginx.conf               # Nginx 反向代理配置
├── .env.example             # 环境变量模板
└── CLAUDE.md                # 项目规范文档
```

---

## 三、部署方式

### 方式一：Docker Compose 一键部署（推荐）

适用于**生产环境**和**快速体验**，一条命令启动全部 6 个服务。

#### 步骤 1：克隆代码

```bash
git clone https://github.com/joeeei11/TASS.git
cd TASS
```

#### 步骤 2：配置环境变量

```bash
cp .env.example .env
```

编辑 `.env` 文件，填入真实配置：

```env
# ===== 数据库 =====
DB_HOST=localhost
DB_PORT=3306
DB_NAME=exam_ai
DB_USERNAME=root
DB_PASSWORD=你的MySQL密码          # ← 必填

# ===== Redis =====
REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_PASSWORD=

# ===== JWT =====
JWT_SECRET=至少32位的随机字符串      # ← 必填
JWT_EXPIRE_HOURS=72

# ===== DeepSeek AI =====
DEEPSEEK_API_KEY=sk-xxxxxxxxxx    # ← 必填，从 platform.deepseek.com 获取
DEEPSEEK_BASE_URL=https://api.deepseek.com
DEEPSEEK_MODEL=deepseek-chat
DEEPSEEK_MAX_TOKENS=4096

# ===== MinIO 文件存储 =====
MINIO_ENDPOINT=http://localhost:9000
MINIO_ACCESS_KEY=minioadmin
MINIO_SECRET_KEY=minioadmin123    # ← 生产环境请修改
MINIO_BUCKET=exam-ai

# ===== OCR 微服务 =====
OCR_SERVICE_URL=http://localhost:5001

# ===== 应用配置 =====
APP_PORT=8080
FRONTEND_URL=http://localhost:80
```

#### 步骤 3：启动所有服务

```bash
docker compose up -d
```

首次启动会自动：
- 拉取 MySQL 8.0、Redis 7.2、MinIO 镜像
- 构建后端（Maven 编译）、前端（Vite 构建）、OCR（PaddleOCR 安装）镜像
- 初始化数据库表结构和基础数据（科目、管理员账号）

> ⏱ 首次构建约需 5-15 分钟（取决于网络速度）

#### 步骤 4：验证服务

```bash
# 查看容器状态（全部应为 Up / healthy）
docker compose ps

# 验证后端健康
curl http://localhost/api/health

# 查看后端日志
docker compose logs -f backend
```

#### 步骤 5：访问系统

| 服务 | 地址 |
|------|------|
| 前端页面 | http://localhost |
| API 文档（Knife4j） | http://localhost/doc.html |
| MinIO 控制台 | http://localhost:9001 |

#### 默认管理员账号

| 用户名 | 密码 | 角色 |
|--------|------|------|
| admin | admin123 | 管理员 |

---

### 方式二：本地开发分步启动

适用于**二次开发**，支持热重载。

#### 步骤 1：启动基础设施

```bash
docker compose -f docker-compose.dev.yml up -d
```

这将启动 MySQL + Redis + MinIO 三个容器。

#### 步骤 2：启动 OCR 微服务

```bash
cd ocr-service
pip install -r requirements.txt
python app.py
# 监听 http://localhost:5001
```

#### 步骤 3：启动后端

```bash
cd backend
mvn spring-boot:run -Dspring-boot.run.profiles=dev
# 监听 http://localhost:8080
```

#### 步骤 4：启动前端

```bash
cd frontend
npm install
npm run dev
# 访问 http://localhost:5173
```

前端开发服务器自动将 `/api` 请求代理到后端 8080 端口。

---

## 四、服务架构

```
┌─────────────────────────────────────────────────┐
│                   用户浏览器                      │
│              http://localhost:80                  │
└──────────────────┬──────────────────────────────┘
                   │
           ┌───────▼───────┐
           │  Nginx (前端)  │  ← 静态资源 + 反向代理
           │  Port: 80     │
           └───┬───────┬───┘
               │       │
    静态资源    │       │  /api/*
               │       │
    ┌──────────▼─┐  ┌──▼──────────────┐
    │ Vue3 SPA   │  │ Spring Boot 后端 │
    │ dist/      │  │ Port: 8080      │
    └────────────┘  └──┬──┬──┬────────┘
                       │  │  │
            ┌──────────┘  │  └──────────┐
            │             │             │
     ┌──────▼──┐  ┌──────▼──┐  ┌───────▼────┐
     │ MySQL   │  │  Redis  │  │   MinIO    │
     │ 3306    │  │  6379   │  │ 9000/9001  │
     └─────────┘  └─────────┘  └────────────┘
                       │
                ┌──────▼──────┐
                │ OCR 微服务   │
                │ Flask: 5001 │
                │ PaddleOCR   │
                └─────────────┘
                       │
                ┌──────▼──────┐
                │ DeepSeek API│
                │ (外部服务)   │
                └─────────────┘
```

---

## 五、常用运维命令

```bash
# 查看所有容器状态
docker compose ps

# 查看后端日志（实时）
docker compose logs -f backend

# 重启单个服务
docker compose restart backend

# 停止所有服务
docker compose down

# 停止并删除数据卷（清空数据库，谨慎操作）
docker compose down -v

# 重新构建并启动（代码更新后）
docker compose up -d --build

# 仅重建后端
docker compose build --no-cache backend && docker compose up -d backend

# 进入 MySQL 命令行
docker compose exec mysql mysql -u root -p exam_ai

# 进入后端容器 Shell
docker compose exec backend sh
```

---

## 六、常见问题

### Q1: 首次启动后端报错 `Invalid value type for attribute 'factoryBeanObjectType'`
**原因：** MyBatis-Plus starter 与 Spring Boot 3 不兼容。
**解决：** 确认 `pom.xml` 中使用的是 `mybatis-plus-spring-boot3-starter`（非 `mybatis-plus-boot-starter`）。

### Q2: 科目名称显示乱码
**原因：** MySQL 初始化时客户端字符集为 latin1。
**解决：** 确认 `data.sql` 头部有 `SET NAMES utf8mb4;`，且 `docker-compose.yml` MySQL 命令包含 `--character-set-client-handshake=FALSE`。如已乱码，需删除数据卷重建：
```bash
docker compose down -v
docker compose up -d
```

### Q3: AI 出题超时
**原因：** DeepSeek API 响应时间较长（通常 10-30 秒），默认超时可能不够。
**解决：** 确认 `application.yml` 中 `app.deepseek.timeout-seconds` 设为 60 或更高。

### Q4: OCR 服务构建失败
**原因：** PaddlePaddle 包较大（~126MB），网络不稳定可能导致下载中断。
**解决：** 重试构建 `docker compose build --no-cache ocr-service`，或在 Dockerfile 中增加 `--retries 5 --timeout 120` 参数。

### Q5: 端口冲突（80 端口被占用）
**解决：** 修改 `docker-compose.yml` 中 frontend 的端口映射，例如改为 `"8000:80"`，然后通过 `http://localhost:8000` 访问。

---

## 七、技术栈版本清单

| 技术 | 版本 | 类别 |
|------|------|------|
| Spring Boot | 3.2.3 | 后端框架 |
| MyBatis-Plus | 3.5.5 | ORM |
| Spring Security 6 | 6.2.x | 认证鉴权 |
| JWT (jjwt) | 0.12.3 | Token |
| Knife4j | 4.4.0 | API 文档 |
| MySQL | 8.0 | 数据库 |
| Redis | 7.2 | 缓存 |
| MinIO | latest | 对象存储 |
| Vue | 3.4.x | 前端框架 |
| Vite | 5.x | 构建工具 |
| Element Plus | 2.x | UI 组件库 |
| Pinia | 2.x | 状态管理 |
| ECharts | 5.x | 数据可视化 |
| Axios | 1.x | HTTP 客户端 |
| PaddleOCR | 2.10 | OCR 引擎 |
| Flask | 3.x | OCR 微服务 |
| Nginx | 1.25 | 反向代理 |
| Docker | 24.x+ | 容器化 |
| DeepSeek API | v3 | AI 引擎 |
