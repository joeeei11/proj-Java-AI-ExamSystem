![Language](https://img.shields.io/badge/language-Java-red) ![License](https://img.shields.io/badge/license-MIT-green)

# proj-Java-AI-ExamSystem

**TASS —— 基于 DeepSeek AI 的智能考试与阅卷系统，支持 OCR 识别与自动评分。**

## 功能特性

- AI 自动出题与智能阅卷（DeepSeek 接入）
- OCR 识别手写答题卡
- MinIO 文件存储与试卷管理
- JWT 鉴权与全局异常处理
- Docker 容器化，支持一键部署

## 快速开始

### 环境要求

- Java >= 17, Maven >= 3.8
- MySQL >= 8.0, MinIO, Redis

### 安装步骤

```bash
git clone https://github.com/joeeei11/proj-Java-AI-ExamSystem.git
cd proj-Java-AI-ExamSystem
cp .env.example .env   # 填写数据库与 AI Key
docker compose up -d
```

### 基础用法

```
接口文档：http://localhost:8080/doc.html
```
