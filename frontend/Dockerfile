# 前端镜像：多阶段构建
# Stage 1: 构建 Vue 应用
FROM node:20-alpine AS builder

WORKDIR /app

# 先复制依赖清单（利用 Docker 缓存层）
COPY frontend/package.json frontend/package-lock.json ./
RUN npm ci

# 复制前端源码并构建
COPY frontend/ .
RUN npm run build

# Stage 2: Nginx 静态托管
FROM nginx:1.25-alpine

# 复制构建产物
COPY --from=builder /app/dist /usr/share/nginx/html

# 复制 Nginx 配置（context 是项目根目录，可以直接引用）
COPY nginx.conf /etc/nginx/nginx.conf

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
