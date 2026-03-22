---
# Phase 2：用户认证与基础数据管理

## 目标
用户可完成注册/登录/个人信息修改，管理员可管理科目和知识点，前端登录页面可正常使用。

## 前置条件
- Phase 1 全部验收通过
- 数据库表已创建，基础科目数据已初始化

## 任务清单

### 后端 - 认证模块
- [ ] 任务1：创建实体类 `entity/User.java`（对应 t_user，加 @TableName、@TableId 等 MyBatis-Plus 注解）
- [ ] 任务2：创建 `module/auth/AuthController.java`，实现：
  - `POST /api/auth/register`：用户名/密码注册，BCrypt 加密，校验用户名唯一
  - `POST /api/auth/login`：登录验证，成功返回 JWT token
  - `POST /api/auth/logout`：将 token 加入 Redis 黑名单（key: `blacklist:token`, TTL 与 token 剩余时间一致）
- [ ] 任务3：创建 `common/utils/JwtUtil.java`：生成/解析/校验 JWT
- [ ] 任务4：配置 `SecurityConfig.java`：白名单放行 `/api/auth/**` 和 `/api/health`，其余需要认证
- [ ] 任务5：实现 `JwtAuthenticationFilter.java`：每次请求从 Header 取 token → 解析 → 检查黑名单 → 放入 SecurityContext

### 后端 - 用户模块
- [ ] 任务6：创建 `module/user/UserController.java`：
  - `GET /api/user/profile`：获取当前登录用户信息（不返回 password）
  - `PUT /api/user/profile`：更新昵称/邮箱
  - `PUT /api/auth/password`：修改密码（需校验旧密码）
  - `POST /api/user/avatar`：上传头像到 MinIO，返回访问 URL
  - `POST /api/user/check-in`：每日打卡，当天已打卡返回 409，更新 study_streak
- [ ] 任务7：创建 `common/utils/MinioUtil.java`：封装文件上传/删除/获取预签名 URL 操作

### 后端 - 科目与知识点管理
- [ ] 任务8：创建 `entity/Subject.java`、`entity/KnowledgePoint.java`
- [ ] 任务9：创建 `module/admin/SubjectController.java`（需 ADMIN 角色）：
  - `GET/POST/PUT/DELETE /api/admin/subjects`
  - `GET /api/admin/subjects/{id}/knowledge-points`（层级树结构返回）
  - `POST/PUT/DELETE /api/admin/knowledge-points`
- [ ] 任务10：创建公开接口 `GET /api/subjects`（用于前端出题选择），`GET /api/subjects/{id}/knowledge-points`

### 前端 - 认证页面
- [ ] 任务11：创建 `views/auth/LoginView.vue`：用户名密码登录表单，登录成功跳转首页，表单校验
- [ ] 任务12：创建 `views/auth/RegisterView.vue`：注册表单（用户名/密码/确认密码），提交后跳登录
- [ ] 任务13：在 `stores/auth.ts` 完善 login/logout/fetchProfile action
- [ ] 任务14：创建 `src/api/auth.ts` 和 `src/api/user.ts` 封装所有接口调用

### 前端 - 个人中心页面
- [ ] 任务15：创建 `views/user/ProfileView.vue`：显示头像/昵称/邮箱/连续打卡天数，支持编辑和头像上传
- [ ] 任务16：在 `AppLayout.vue` 顶栏显示当前用户头像和昵称，点击弹出菜单（个人中心/退出登录）

## 验收标准
- [ ] `POST /api/auth/register` 注册成功，重复用户名返回 400 含错误信息
- [ ] `POST /api/auth/login` 登录返回 token，用 token 请求 `GET /api/user/profile` 成功返回用户信息
- [ ] `POST /api/auth/logout` 后，原 token 再次请求 `/api/user/profile` 返回 401
- [ ] 前端登录页面输入正确账号密码可正常登录，跳转到首页
- [ ] 前端未登录访问任何需要鉴权的页面，自动跳转 `/login`
- [ ] `GET /api/subjects` 返回 5 个科目数据
- [ ] 管理员账户可访问 `/api/admin/subjects`，普通账户返回 403

## 注意事项
- JWT 密钥长度必须 ≥ 32 字节（HS256 要求），否则 jjwt 会抛异常
- 头像上传大小限制 2MB，后端和前端都要做校验
- 管理员账户在 `data.sql` 中预置（role=1），密码 BCrypt 加密
- 打卡接口需要加 Redis 分布式锁，防止并发重复打卡
- `GET /api/subjects/{id}/knowledge-points` 需要递归查询，建议限制树深度 ≤ 3 层
---
