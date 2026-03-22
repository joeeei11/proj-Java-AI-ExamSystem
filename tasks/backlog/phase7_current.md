---
# Phase 7：管理员模块、性能优化与系统收尾

## 目标
管理员后台功能完整可用，系统完成全面性能优化、安全加固、前端体验打磨，达到可提交答辩的生产级质量。

## 前置条件
- Phase 1 ~ 6 全部验收通过
- 所有核心功能已联调无报错

## 任务清单

### 后端 - 管理员模块
- [ ] 任务1：创建 `module/admin/AdminUserController.java`：
  - `GET /api/admin/users`：用户列表，支持 `?keyword=&role=&page=&size=` 分页
  - `PUT /api/admin/users/{id}/status`：启用/禁用用户（同时将该用户 token 加入 Redis 黑名单强制下线）
  - `GET /api/admin/users/{id}/stats`：查看指定用户的学习统计（答题数/正确率/最后活跃时间）
- [ ] 任务2：创建 `module/admin/AdminStatsController.java`：
  - `GET /api/admin/stats/overview`：系统概览（注册用户数/今日活跃数/总答题数/题库量/笔记总数）
  - `GET /api/admin/stats/daily?days=7`：近 7 天新增用户/答题量趋势
- [ ] 任务3：完善 `module/admin/SubjectController.java`（Phase 2 已建，补充）：
  - 批量导入知识点接口 `POST /api/admin/knowledge-points/batch`（JSON 数组）
  - 知识点排序 `PUT /api/admin/knowledge-points/sort`

### 后端 - 安全与性能优化
- [ ] 任务4：添加接口限流（用 Redis + 令牌桶）：
  - AI 相关接口（出题/整理笔记/生成提纲）：每用户每分钟最多 5 次
  - 登录接口：同 IP 每分钟最多 10 次（防暴力破解）
  - 创建 `common/ratelimit/RateLimitAspect.java`（AOP 切面 + 自定义注解 `@RateLimit`）
- [ ] 任务5：添加参数校验：所有 Controller 方法加 `@Valid`，DTO 用 `@NotBlank/@Size/@Range` 等注解，统一由 GlobalExceptionHandler 返回 400
- [ ] 任务6：敏感日志脱敏：日志中的密码字段、API Key、手机号自动替换为 `***`（自定义 Logback Pattern）
- [ ] 任务7：添加 CORS 配置，仅允许前端域名跨域请求（通过环境变量 `FRONTEND_URL` 控制）
- [ ] 任务8：数据库索引优化，执行 `EXPLAIN` 分析以下高频查询并补充索引：
  - `t_user_answer(user_id, created_at)`
  - `t_note(user_id, updated_at)`
  - `t_error_book(user_id, is_mastered, subject_id)`
- [ ] 任务9：健康检查完善 `GET /api/health`：检查 MySQL/Redis/MinIO/OCR 服务连通性，返回各组件状态

### 前端 - 管理员后台
- [ ] 任务10：创建 `views/admin/AdminLayout.vue`：独立管理后台布局（顶部面包屑 + 左侧菜单）
- [ ] 任务11：创建 `views/admin/UserManageView.vue`：
  - 用户列表表格（头像/用户名/角色/状态/注册时间/最后活跃）
  - 搜索框 + 角色筛选
  - 操作列：启用/禁用按钮（带二次确认弹框）
- [ ] 任务12：创建 `views/admin/SubjectManageView.vue`：
  - 科目列表 + 新增/编辑/删除
  - 点击科目展开知识点树（`el-tree`）
  - 知识点新增/删除/拖拽排序
- [ ] 任务13：创建 `views/admin/SystemStatsView.vue`：
  - 顶部 4 格数据卡片（用户数/今日活跃/题库量/总答题数）
  - 折线图：近 7 天新增用户 vs 新增答题量

### 前端 - 用户体验优化
- [ ] 任务14：全局 loading 优化：路由跳转时显示顶部进度条（`nprogress`）
- [ ] 任务15：空状态优化：所有列表页在无数据时显示引导插画 + 操作按钮（`el-empty`）
- [ ] 任务16：移动端响应式适配（最低适配 768px 平板）：
  - 侧边栏在小屏幕下改为折叠抽屉
  - 图表在小屏幕下改为单列布局
- [ ] 任务17：添加页面 `<title>` 动态设置（路由 meta.title + 项目名拼接）
- [ ] 任务18：前端构建优化：`vite.config.ts` 配置代码分割（按路由懒加载）、gzip 压缩（`vite-plugin-compression`）

### 系统收尾
- [ ] 任务19：编写 `README.md`：项目简介、技术栈、快速启动步骤、截图展示
- [ ] 任务20：编写 `docker-compose.yml` 生产版本：所有服务加健康检查 `healthcheck`、重启策略 `restart: unless-stopped`
- [ ] 任务21：功能完整性自测：按照下方验收标准逐项检查，记录问题并修复
- [ ] 任务22：在答辩材料中准备的演示数据：为测试账号注入 50+ 道题的答题记录、10+ 篇笔记、20+ 条错题

## 验收标准

### 管理员功能
- [ ] 管理员账号登录后，侧边栏出现"管理后台"菜单入口（普通用户不可见）
- [ ] 用户列表分页正常，"禁用"操作后该用户再次请求 API 返回 401
- [ ] 科目管理 CRUD 全部正常，新增科目后在刷题配置页立即可选

### 性能与安全
- [ ] AI 出题接口连续调用第 6 次，返回 429 Too Many Requests，提示"操作太频繁，请稍后再试"
- [ ] 前端 `npm run build` 构建成功，`dist/` 目录总大小 < 5MB（gzip 后）
- [ ] `GET /api/health` 显示所有依赖服务状态正常

### 整体体验
- [ ] 全流程演示：注册 → 刷题 → 错题本 → 上传图片笔记 → OCR → AI整理 → 生成提纲 → 查看仪表盘，无报错
- [ ] 页面切换有顶部进度条，所有列表无数据时有引导提示
- [ ] 前端 Console 无 `error` 级别报错（警告可接受）
- [ ] Knife4j 接口文档所有接口均有描述，可在文档页面直接测试

## 注意事项
- **限流 AOP 顺序**：`@RateLimit` 切面需在 `@PreAuthorize` 之后执行（即先鉴权再限流），注意 `@Order` 设置
- **管理员路由守卫**：前端路由对 `/admin/**` 页面检查 `userInfo.role === 1`，否则跳转 403 页面
- **禁用用户强制下线**：禁用时需查出该用户所有有效 token（Redis 中存 `user:tokens:{userId}` Set），逐一加黑名单
- **生产 Docker 配置**：MySQL 数据目录挂载到宿主机 volume，防止容器重建丢失数据
- **答辩演示数据**：提前用脚本初始化，不要现场实时 AI 生成（防止 API 网络波动）
---
