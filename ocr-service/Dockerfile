# OCR 微服务镜像（PaddleOCR + Flask）
FROM python:3.10-slim

WORKDIR /app

# 安装系统依赖
RUN apt-get update && apt-get install -y --no-install-recommends \
    libgl1 \
    libglib2.0-0 \
    libgomp1 \
    && rm -rf /var/lib/apt/lists/*

# 先复制依赖文件（利用 Docker 缓存层）
COPY requirements.txt .

# 安装 Python 依赖（PaddleOCR 模型会在首次运行时下载）
RUN pip install --no-cache-dir --retries 5 --timeout 120 -r requirements.txt

# 复制应用代码
COPY . .

# 暴露端口
EXPOSE 5001

# 环境变量
ENV OCR_PORT=5001

# 使用 gunicorn 生产启动
CMD ["gunicorn", "--bind", "0.0.0.0:5001", "--workers", "1", "--timeout", "120", "app:app"]
