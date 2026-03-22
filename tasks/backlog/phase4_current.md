---
# Phase 4：智能笔记模块与 OCR 集成

## 目标
用户可创建文字笔记和上传图片笔记（自动 OCR 提取文字），并支持 AI 一键整理笔记（结构化分点、标重点、格式化为 Markdown），笔记支持分类管理和标签。

## 前置条件
- Phase 2 全部验收通过（用户认证、MinIO 文件存储可用）
- PaddleOCR Flask 服务可正常启动（`python ocr-service/app.py`）
- DeepSeek API 可调用（Phase 3 已验证）

## 任务清单

### 后端 - 笔记模块
- [ ] 任务1：创建实体 `entity/Note.java`（对应 t_note 表）
- [ ] 任务2：创建 `module/note/NoteController.java`：
  - `GET /api/notes`：获取当前用户笔记列表，支持 `?subjectId=&tag=&keyword=&page=&size=` 筛选，按 updated_at 倒序
  - `POST /api/notes`：新建笔记 `{title, content, subjectId, tags:[], type}`
  - `GET /api/notes/{id}`：笔记详情（校验归属当前用户）
  - `PUT /api/notes/{id}`：更新笔记内容/标题/标签
  - `DELETE /api/notes/{id}`：软删除（添加 is_deleted 字段）
  - `POST /api/notes/ocr`：上传图片 OCR，流程：图片 → MinIO 存储（得到 URL） → 调用 OCR 微服务 → 返回 `{imageUrl, text}`
  - `POST /api/notes/{id}/organize`：AI 一键整理，调用 AI 整理 content，更新 note 的 content 并标记 is_organized=1
- [ ] 任务3：创建 `module/note/OcrService.java`：用 OkHttp 调用 `OCR_SERVICE_URL/ocr`，传入图片 Base64，返回识别文字
- [ ] 任务4：创建 `module/note/NoteAiService.java`：调用 DeepSeek 整理笔记，Prompt 要求返回 Markdown 格式（分点、加粗重点、生成标题层级）

### OCR 微服务完善
- [ ] 任务5：完善 `ocr-service/app.py`：
  - `POST /ocr`：接收 `multipart/form-data` 图片文件 **或** `{"imageBase64":"..."}` JSON
  - 支持 JPG/PNG/WEBP 格式
  - 识别结果按行拼接，过滤空行
  - 添加 `/health` 接口返回 `{"status":"ok"}`
- [ ] 任务6：在 `ocr-service/Dockerfile` 中预下载 PaddleOCR 中文模型（避免运行时下载超时）

### 前端 - 笔记模块
- [ ] 任务7：创建 `src/api/note.ts`：封装所有笔记接口
- [ ] 任务8：创建 `views/notes/NoteListView.vue`（笔记列表页）：
  - 左侧：科目/标签筛选栏
  - 右侧：笔记卡片网格（显示标题/更新时间/标签/首行内容摘要）
  - 顶部：搜索框、"新建笔记"和"上传图片"按钮
- [ ] 任务9：创建 `views/notes/NoteEditorView.vue`（笔记编辑页）：
  - 标题输入框
  - 富文本编辑器（使用 `@vueuse/core` + 原生 `contenteditable` 或引入 `md-editor-v3` Markdown 编辑器）
  - 右侧面板：科目选择、标签输入（支持多个标签）
  - 顶部工具栏：保存按钮、"AI 整理"按钮（点击调用 organize 接口，loading 动画，整理完刷新内容）
- [ ] 任务10：创建 `views/notes/NoteOcrView.vue`（图片转笔记页）：
  - 拖拽/点击上传图片区域（`el-upload`）
  - 图片预览
  - OCR 识别结果文本框（可编辑）
  - "保存为笔记"按钮（跳转到编辑页，预填 OCR 文字）
- [ ] 任务11：创建 `components/MarkdownViewer.vue`：Markdown 内容渲染组件（用 `marked` + `highlight.js`），在笔记详情页、AI 解析等处复用

## 验收标准
- [ ] `POST /api/notes/ocr` 上传一张含文字的图片，返回识别文字，准确率合理
- [ ] `POST /api/notes/{id}/organize` 调用后，笔记内容被 AI 整理为带标题层级和加粗重点的 Markdown 格式
- [ ] 前端笔记列表页可正常展示笔记，支持科目筛选和关键词搜索
- [ ] 前端笔记编辑页可创建/编辑笔记，AI 整理按钮有 loading 状态，整理完毕内容自动更新
- [ ] OCR 微服务 `GET /health` 返回正常
- [ ] 图片转笔记流程：上传图片 → OCR 识别 → 内容填入编辑器 → 保存笔记，全程可走通

## 注意事项
- **图片大小限制**：前端限制上传 ≤ 5MB，OCR 服务处理前压缩到 ≤ 2MB（Pillow 压缩）
- **OCR 并发**：PaddleOCR 模型不是线程安全的，Flask 服务 `threaded=False` 或用进程锁保护
- **AI 整理幂等性**：多次点击 AI 整理会覆盖已整理内容，前端需弹确认框提示"再次整理将覆盖当前内容"
- **Markdown 编辑器选择**：推荐 `md-editor-v3`（Vue3 支持好），安装：`npm i md-editor-v3`
- **笔记搜索**：使用 MySQL LIKE 查询（`WHERE title LIKE ? OR content LIKE ?`），数据量大时可加全文索引
- **标签存储**：JSON 字段存储标签数组，前端展示时解析，筛选时用 `JSON_CONTAINS`
---
