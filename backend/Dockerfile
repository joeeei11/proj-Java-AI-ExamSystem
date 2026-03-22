# 后端镜像：多阶段构建
# Stage 1: Maven 打包
FROM maven:3.9-eclipse-temurin-17-alpine AS builder

WORKDIR /app

# 复制阿里云 Maven 镜像配置
COPY settings.xml /root/.m2/settings.xml

# 先复制 pom.xml 下载依赖（利用缓存层）
COPY pom.xml .
RUN mvn dependency:go-offline -q

# 复制源码并打包
COPY src ./src
RUN mvn package -DskipTests -q

# Stage 2: 运行时镜像
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# 从构建阶段复制 jar
COPY --from=builder /app/target/exam-ai-*.jar app.jar

# 创建日志目录
RUN mkdir -p /app/logs

EXPOSE 8080

# JVM 优化参数
ENTRYPOINT ["java", \
    "-Xms256m", \
    "-Xmx512m", \
    "-XX:+UseG1GC", \
    "-XX:MaxGCPauseMillis=200", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-jar", "app.jar"]
