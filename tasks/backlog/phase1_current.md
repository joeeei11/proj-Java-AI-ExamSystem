---
# Phase 1：项目脚手架与基础设施

## 目标
项目骨架搭建完毕，前后端能分别独立启动，数据库表全部创建，Docker Compose 可一键拉起所有基础服务。

## 前置条件
- 已安装：Java 17、Maven 3.9、Node 20、Docker Desktop、Python 3.10
- 已申请：DeepSeek API Key
- `.env` 文件已按 `.env.example` 配置

## 任务清单

### 后端初始化
- [ ] 任务1：使用 Spring Initializr 创建 `backend/` 项目，选依赖：Web、Security、MyBatis-Plus、MySQL、Redis、Validation、Lombok
- [ ] 任务2：配置 `application.yml` / `application-dev.yml`，读取 `.env` 环境变量（用 `@Value` 注入）
- [ ] 任务3：编写 `src/main/resources/sql/schema.sql`，包含全部 8 张表（t_user、t_subject、t_knowledge_point、t_question、t_user_answer、t_error_book、t_note、t_review_outline、t_study_stat）
- [ ] 任务4：编写 `src/main/resources/sql/data.sql`，插入科目初始数据（行测/申论/公基/常识等 5 个科目）
- [ ] 任务5：创建 `common/result/R.java` 统一响应体
- [ ] 任务6：创建 `common/exception/GlobalExceptionHandler.java` 全局异常处理
- [ ] 任务7：配置 `SwaggerConfig.java`（Knife4j），验证 `/doc.html` 可访问
- [ ] 任务8：编写健康检查接口 `GET /api/health`，返回 `{"status":"UP","timestamp":"..."}`

### 前端初始化
- [ ] 任务9：`npm create vue@latest frontend` 创建项目（选 TypeScript、Router、Pinia）
- [ ] 任务10：安装依赖：`element-plus axios echarts @vueuse/core`
- [ ] 任务11：配置 `vite.config.ts` API 代理（`/api` 转发到 `http://localhost:8080`）
- [ ] 任务12：创建 `src/utils/request.ts`（Axios 实例 + 请求拦截器加 JWT Header + 响应拦截器统一错误提示）
- [ ] 任务13：创建基础布局框架 `src/components/layout/AppLayout.vue`（侧边栏导航 + 顶栏）
- [ ] 任务14：配置路由 `src/router/index.ts`，添加 beforeEach 路由守卫（未登录跳转 `/login`）
- [ ] 任务15：创建 `src/stores/auth.ts`（存 token 和 userInfo，持久化到 localStorage）

### OCR 微服务初始化
- [ ] 任务16：在 `ocr-service/` 创建 `app.py`：Flask 应用，`POST /ocr` 接口接收 `{"imageBase64":"..."}` 返回 `{"text":"..."}`
- [ ] 任务17：创建 `ocr-service/requirements.txt`：`flask paddlepaddle paddleocr`
- [ ] 任务18：创建 `ocr-service/Dockerfile`

### Docker 编排
- [ ] 任务19：编写 `docker-compose.yml`，包含：mysql8、redis7、minio、ocr-service
- [ ] 任务20：编写 `docker-compose.dev.yml`，仅启动 mysql8、redis7、minio（本地开发用）
- [ ] 任务21：创建 `.env.example` 文件

## 验收标准
- [ ] `docker compose -f docker-compose.dev.yml up -d` 启动无报错，MySQL/Redis/MinIO 健康
- [ ] `mvn spring-boot:run` 后端启动成功，`GET http://localhost:8080/api/health` 返回 200
- [ ] `npm run dev` 前端启动成功，浏览器访问 `http://localhost:5173` 显示应用框架
- [ ] `python ocr-service/app.py` OCR 服务启动成功，`POST http://localhost:5001/ocr` 返回正常
- [ ] 访问 `http://localhost:8080/doc.html` 显示 Knife4j 接口文档

## 注意事项
- Schema.sql 中 `t_question.options` 字段类型为 MySQL JSON，确保 MySQL 8.0+
- MinIO 初始化时需手动创建 Bucket `exam-ai`，或在 SpringBoot 启动时自动创建（建议 `@PostConstruct` 实现）
- OCR 服务首次启动会下载 PaddleOCR 模型文件（约 1GB），需要等待，建议提前下载
- Windows 开发时注意文件路径分隔符，MinIO SDK 路径统一用 `/`
---
