# syntax=docker/dockerfile:1

# ---- Stage 1: Maven build for one module ----
FROM --platform=$BUILDPLATFORM eclipse-temurin:21-jdk AS builder

WORKDIR /app

ARG MODULE
ARG MAVEN_ARGS="-DskipTests"

COPY mvnw mvnw.cmd ./
COPY .mvn .mvn
COPY pom.xml ./
COPY jasmine-common-core/pom.xml jasmine-common-core/pom.xml
COPY jasmine-common/pom.xml jasmine-common/pom.xml
COPY jasmine-schema/pom.xml jasmine-schema/pom.xml
COPY jasmine-gateway/pom.xml jasmine-gateway/pom.xml
COPY jasmine-iam/pom.xml jasmine-iam/pom.xml
COPY jasmine-product/pom.xml jasmine-product/pom.xml
COPY jasmine-trade/pom.xml jasmine-trade/pom.xml
COPY jasmine-crm/pom.xml jasmine-crm/pom.xml

# 修正 mvnw 行尾（兼容 Windows 上 CRLF 检出），并使用 BuildKit cache 复用 ~/.m2 加速依赖解析
RUN --mount=type=cache,target=/root/.m2 \
    test -n "$MODULE" && sed -i 's/\r$//' ./mvnw && chmod +x ./mvnw && \
    ./mvnw -pl "$MODULE" -am dependency:go-offline -B

COPY jasmine-common-core jasmine-common-core
COPY jasmine-common jasmine-common
COPY jasmine-schema jasmine-schema
COPY jasmine-gateway jasmine-gateway
COPY jasmine-iam jasmine-iam
COPY jasmine-product jasmine-product
COPY jasmine-trade jasmine-trade
COPY jasmine-crm jasmine-crm

RUN --mount=type=cache,target=/root/.m2 \
    ./mvnw -pl "$MODULE" -am $MAVEN_ARGS package -B && \
    cp ${MODULE}/target/${MODULE}-0.0.1-SNAPSHOT-exec.jar /app/app.jar

# ---- Stage 2: runtime image ----
FROM eclipse-temurin:21-jre

WORKDIR /app

ARG MODULE
ARG APP_PORT=8080

RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*

COPY --from=builder /app/app.jar /app/app.jar

EXPOSE ${APP_PORT}

ENTRYPOINT ["java", \
  "-XX:+UseZGC", \
  "-Xms256m", \
  "-Xmx384m", \
  "-XX:+UseCompressedOops", \
  "-XX:+UseStringDeduplication", \
  "-XX:+AlwaysPreTouch", \
  "-jar", "/app/app.jar"]
