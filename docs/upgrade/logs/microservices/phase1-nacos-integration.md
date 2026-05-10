# Phase 1：Nacos 接入

> **分支**：`microservices`
> **提交**：`30a47ae` — 25 files changed, +414 / -80
> **日期**：2026-05-08

## 目标

所有服务注册到 Nacos，配置中心化，为后续 Gateway 路由和服务间远程调用奠定基础。

## 本次改动

### 1.1 部署 Nacos Server

- 镜像：`nacos/nacos-server:v2.4.3`，单机模式
- 数据持久化：MySQL（复用已有的 MySQL 容器）
- 已添加到 `ops/dev/docker-compose.yml` 和 `ops/prod/docker-compose.yml`
- 端口映射：8848（HTTP）/ 9848（gRPC）
- 开启鉴权：`NACOS_AUTH_ENABLE=true`

### 1.2 各服务引入 Nacos 依赖

四个业务服务 + Gateway 均引入：

```xml
<dependency>
    <groupId>com.alibaba.cloud</groupId>
    <artifactId>spring-cloud-starter-alibaba-nacos-discovery</artifactId>
</dependency>
<dependency>
    <groupId>com.alibaba.cloud</groupId>
    <artifactId>spring-cloud-starter-alibaba-nacos-config</artifactId>
</dependency>
```

### 1.3 配置中心化 — `spring.config.import=nacos:`

各服务 `application.yml` 中通过 `spring.config.import` 从 Nacos 拉取共享配置：

```yaml
spring:
  config:
    import:
      - nacos:jasmine-common.yml?refresh=true
      - nacos:jasmine-iam.yml?refresh=true   # 各服务专属
  cloud:
    nacos:
      server-addr: ${NACOS_ADDR:127.0.0.1:8848}
      username: ${NACOS_USERNAME:nacos}
      password: ${NACOS_PASSWORD:nacos}
      discovery:
        namespace: ${NACOS_NAMESPACE:dev}
        group: JASMINE
      config:
        namespace: ${NACOS_NAMESPACE:dev}
        group: JASMINE
```

### 1.4 配置分层

| Data ID | 内容 | 说明 |
|:---|:---|:---|
| `jasmine-common.yml` | MySQL/Redis 连接 | 所有服务共享 |
| `jasmine-iam.yml` | RabbitMQ 连接 | IAM 服务额外需要 MQ |
| `jasmine-product.yml` | （空占位） | 商品服务无额外中间件 |
| `jasmine-trade.yml` | RabbitMQ 连接 | 交易服务需要 MQ |
| `jasmine-crm.yml` | RabbitMQ 连接 | 客户关系服务需要 MQ |
| `jasmine-gateway.yml` | （空占位） | 阶段 2 添加路由规则 |

### 1.5 配置预设与导入脚本

- 预设文件目录：`ops/nacos-config/`
- 导入脚本：`ops/nacos-config/import.sh <nacos-addr> [namespace]`
- 自动创建命名空间并导入所有配置

### 1.6 本地开发配置瘦身

`application-dev.yml` 不再包含数据库/Redis/MQ 连接信息，仅保留：

- Nacos 地址覆盖（`NACOS_ADDR` 环境变量）
- 开发环境特有配置（内存缓存、MQ 开关、JWT 密钥等）

### 1.7 根 POM 修复

- 移除重复的 `jasmine-iam` 声明
- 补充缺失的 `jasmine-trade` 和 `jasmine-gateway` 声明

### 1.8 环境变量

`ops/.env.example` 新增 Nacos 相关环境变量：

- `NACOS_PORT` / `NACOS_GRPC_PORT`
- `NACOS_NAMESPACE`
- `NACOS_USERNAME` / `NACOS_PASSWORD`
- `NACOS_AUTH_TOKEN`

## 验证

- 需在 NAS 环境启动 Nacos Server
- 运行 `ops/nacos-config/import.sh` 导入配置
- 启动各服务后在 Nacos 控制台确认注册信息
- 命名空间：`dev`，Group：`JASMINE`

## 说明

- Nacos 连接地址通过 `NACOS_ADDR` 环境变量覆盖，本地默认 `127.0.0.1:8848`，Docker 容器内使用 `nacos:8848`
- 使用 `spring.config.import` 而非 `bootstrap.yml`，无需额外引入 `spring-cloud-starter-bootstrap`
- 阶段 2 将在 `jasmine-gateway.yml` 中添加路由规则和 JWT 鉴权过滤器配置
