# Jasmine

一个面向花店门店场景的管理系统，当前包含：

- 后端：Spring Boot 单体应用
- 前端：基于 `admin/` 的 Vue 2 后台管理端
- 中间件：MySQL、Redis、RabbitMQ

当前主线已经完成：

- 核心业务模型重建
- RabbitMQ 三阶段接入与稳定性收口
- Redis 与 MQ 的第一轮一致性 / 缓存稳态增强

## 当前技术栈

### 后端

| 技术 | 说明 | 当前版本 |
|:---|:---|:---|
| Java | 运行时 | 21 |
| Spring Boot | 应用框架 | 3.5.13 |
| Spring Security | 认证鉴权 | 跟随 Spring Boot |
| Spring Actuator | 健康检查 / 运行指标 | 跟随 Spring Boot |
| Spring AMQP | RabbitMQ 集成 | 跟随 Spring Boot |
| Spring Retry | MQ 消费重试 | 跟随 Spring Boot |
| MyBatis-Plus | 持久层框架 | 3.5.14 |
| Flyway | 数据库迁移 | 跟随 Spring Boot |
| Redis | 缓存与幂等支撑 | Spring Data Redis |
| JWT | 登录令牌 | 0.12.7 |
| springdoc-openapi | Swagger / OpenAPI | 2.8.16 |
| Micrometer Prometheus | 指标导出 | 跟随 Spring Boot |

### 前端

| 技术 | 说明 | 当前版本 |
|:---|:---|:---|
| Vue | 前端框架 | 2.6.10 |
| Vue Router | 路由 | 3.0.6 |
| Vuex | 状态管理 | 3.1.0 |
| Element UI | 组件库 | 2.13.2 |
| Axios | HTTP 请求 | 0.18.1 |
| Vue CLI | 构建工具链 | 4.4.4 |

### 中间件与部署

| 技术 | 说明 | 当前版本 |
|:---|:---|:---|
| MySQL | 主数据库 | 8.4（Docker / PR14 基线） |
| Redis | 缓存 / 幂等 | 7.2 |
| RabbitMQ | 消息队列 | 4.2-management |
| Docker | 镜像与容器运行 | 当前基线已接入 |
| Docker Compose | 多服务编排 | 当前基线已接入 |
| GitHub Actions | CI / 镜像构建 | 当前基线已接入 |

## 项目阶段

当前详细阶段状态已经单独整理到：

- `docs/project-status.md`

如果想快速了解升级路线与后续计划，建议优先阅读：

- `docs/upgrade/pr11-after-roadmap.md`

## 仓库结构

当前值得优先关注的目录：

- `src/main/java/`：后端业务与基础设施代码
- `src/main/resources/`：配置、Mapper XML、Flyway 迁移
- `src/test/java/`：单元测试与集成测试
- `admin/`：当前旧前端管理端
- `ops/`：Docker / Compose / 部署基线
- `docs/upgrade/`：升级路线、阶段记录、实施文档

## 本地启动

### Windows 本地调试

后端：

```powershell
./start-backend.ps1
```

前端：

```powershell
./start-frontend-bun.ps1
```

如果需要在本地直接验证 Redis 缓存模式，可使用：

```powershell
./start-backend-redis.ps1
```

说明：

- `start-backend.ps1`：默认 `dev` + MQ 打开
- `start-backend-redis.ps1`：`dev` + `APP_CACHE_TYPE=redis`
- `start-frontend-bun.ps1`：自动切到 `admin/` 并使用 Bun 启动前端

## API 与调试入口

后端启动后，常用入口如下：

- Swagger：`http://localhost:9999/swagger-ui/index.html`
- 健康检查：`http://localhost:9999/actuator/health`

如果开启了前端：

- 前端开发地址：`http://localhost:8888`

## Docker / Ops 基线

当前 Docker 与部署基线已经统一收口到 `ops/`：

- `ops/.env.example`
- `ops/dev/docker-compose.yml`
- `ops/dev/up.sh`
- `ops/dev/down.sh`
- `ops/prod/docker-compose.yml`
- `ops/prod/up.sh`
- `ops/prod/down.sh`

详细说明请优先阅读：

- `docs/upgrade/pr14-docker-ops-deploy.md`

### 环境变量准备

先复制：

```bash
cp ops/.env.example ops/.env
```

然后按实际部署环境修改：

- MySQL 账号密码
- Redis 密码
- RabbitMQ 账号密码
- JWT 密钥
- GHCR 镜像标签

### 开发环境 Compose

适合：

- 有 Docker 的本地机器
- NAS 联调环境

启动：

```bash
chmod +x ops/dev/up.sh ops/dev/down.sh
./ops/dev/up.sh
```

### 生产环境 Compose

适合：

- NAS 部署
- 服务器部署

默认从 GHCR 拉镜像：

```bash
chmod +x ops/prod/up.sh ops/prod/down.sh
./ops/prod/up.sh
```

### 管理入口

- 前端：`http://<host>:${FRONTEND_PORT}`
- 后端健康检查：`http://<host>:${BACKEND_PORT}/actuator/health`
- Swagger：`http://<host>:${BACKEND_PORT}/swagger-ui/index.html`
- RabbitMQ 管理台：`http://<host>:${RABBITMQ_MANAGEMENT_PORT}`

## GitHub Actions 镜像构建

当前镜像构建工作流：

- `.github/workflows/docker-publish.yml`

当前支持：

- `push` 到 `main` 自动构建并推送正式镜像
- `push` 到 `next` 自动构建并推送 `next` 通道镜像
- 手动 `workflow_dispatch`

镜像仓库：

- `ghcr.io/<owner>/jasmine-backend`
- `ghcr.io/<owner>/jasmine-frontend`

标签策略：

- `main`：`latest`、`sha-<short_sha>`
- `next`：`next-latest`、`next-<short_sha>`

## 测试说明

当前仓库测试分为两类：

### 单元测试

```bash
./mvnw test -DskipITs=true
```

### 集成测试

```bash
./mvnw verify -DskipUTs=true
```

说明：

- 集成测试使用 Testcontainers
- MySQL / Redis / RabbitMQ 都已纳入集成测试基线
- 无 Docker 的本地环境中，相关集成测试会按预期跳过

## 文档入口

如果要继续理解当前路线与阶段边界，建议优先看：

- `docs/upgrade/pr11-after-roadmap.md`
- `docs/upgrade/pr13-rabbitmq-bootstrap.md`
- `docs/upgrade/pr13-mq-contract.md`
- `docs/upgrade/pr13-5-redis-mq-hardening.md`
- `docs/upgrade/pr14-docker-ops-deploy.md`
- `docs/project-constraints.md`

## 说明

- 当前 `admin/` 仍然是正式可运行前端
- 后续 `PR15` 开始才会进入新前端迁移主线
- 当前 `PR18` 会继续承接更重的高并发与一致性能力，例如：
  - Outbox / 本地消息表
  - 延迟消息体系
  - 真实统计 / 预警类下游消费者
  - Redis 分布式锁
  - 热点库存专项方案
