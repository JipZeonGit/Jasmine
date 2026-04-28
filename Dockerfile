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

ENTRYPOINT ["java", "-jar", "/app/jasmine.jar"]
