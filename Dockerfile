# ---- Stage 1: Maven 构建 ----
FROM eclipse-temurin:21-jdk AS builder

WORKDIR /app

# 先拷贝 Maven 包装器和 pom.xml，利用 Docker 层缓存加速依赖下载
COPY mvnw mvnw.cmd ./
COPY .mvn .mvn
COPY pom.xml ./

RUN chmod +x ./mvnw && ./mvnw dependency:go-offline -B

# 再拷贝源码并编译
COPY src src
RUN ./mvnw -DskipTests package -B

# ---- Stage 2: 最小化运行镜像 ----
FROM eclipse-temurin:21-jre

WORKDIR /app

RUN apt-get update && apt-get install -y --no-install-recommends curl && rm -rf /var/lib/apt/lists/*

COPY --from=builder /app/target/Jasmine-0.0.1-SNAPSHOT.jar /app/jasmine.jar

EXPOSE 9999

# JVM 内存与 GC 优化：
#   -XX:+UseZGC              : ZGC 低延迟收集器，暂停 <1ms，适合容器化 Web 服务
#   -XX:MaxRAMPercentage     : 按容器内存上限百分比自动计算堆大小，避免硬编码
#   -XX:+UseCompressedOops   : 压缩对象指针，减少 40%+ 堆内存开销
#   -XX:+UseStringDeduplication : 字符串去重，Spring 大量重复字符串场景可省 10-20%
#   -XX:+AlwaysPreTouch      : 启动时预分配所有堆内存，避免运行时缺页抖动
#   -XX:+UseContainerSupport : 确保 JVM 正确识别 cgroup 内存/CPU 限制（JDK 10+ 默认开启，显式声明）
ENTRYPOINT ["java", \
  "-XX:+UseZGC", \
  "-XX:MaxRAMPercentage=75.0", \
  "-XX:InitialRAMPercentage=50.0", \
  "-XX:+UseCompressedOops", \
  "-XX:+UseStringDeduplication", \
  "-XX:+AlwaysPreTouch", \
  "-XX:+UseContainerSupport", \
  "-jar", "/app/jasmine.jar"]
