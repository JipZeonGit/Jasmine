# Jasmine

> **分支说明**：
> 当前 `next` 分支为项目的主干分支，它已经完成了从老旧 `legacy` 分支到新一代现代化单体应用架构的全面迁移与重构。
> 未来项目将继续演进，计划单独开辟 `microservices` 分支，探索并实现从单体应用架构向最新微服务架构的跨越。

一个面向花店门店场景的管理系统，当前包含：

- 后端：Spring Boot 单体应用
- 前端：基于 `web/` 的 Vue 3 + Element Plus 管理端
- 中间件：MySQL、Redis、RabbitMQ

当前主线已经完成：

- 核心业务模型重建
- RabbitMQ 三阶段接入与稳定性收口
- Redis 与 MQ 的第一轮一致性 / 缓存稳态增强
- 高并发本地消息表（Outbox）及全链路强一致性预警机制构建
- 基于延时死信架构的业务级消息定时提醒及前端闭环

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
| Vue | 前端框架 | 3.5.32 |
| Vue Router | 路由 | 4.6.4 |
| Pinia | 状态管理 | 3.0.4 |
| Element Plus | 组件库 | 2.13.7 |
| Axios | HTTP 请求 | 1.15.0 |
| Vite | 构建工具链 | 8.0.8 |
| Bun | 本地开发与依赖管理 | 1.3.12 |

### 中间件与部署

| 技术 | 说明 | 当前版本 |
|:---|:---|:---|
| MySQL | 主数据库 | 8.4（Docker / PR14 基线） |
| Redis | 缓存 / 幂等 | 7.2 |
| RabbitMQ | 消息队列 | 4.2-management |
| Docker | 镜像与容器运行 | 当前基线已接入 |
| Docker Compose | 多服务编排 | 当前基线已接入 |
| GitHub Actions | CI / 镜像构建 | 当前基线已接入 |
| OpenJ9 (Semeru) | 极致省内存 JVM | 21（IBM Semeru Runtimes） |

## 架构演进与组件对比 (Legacy vs Next)

从老旧的 `legacy` 分支迁移到现在的 `next` 分支过程中，系统全栈技术经历了彻底的现代化洗牌。以下是核心组件的变动说明：

| 领域 | 组件名称 | 老架构 (Legacy) | 新架构 (Next) | 演进状态 |
|:---|:---|:---|:---|:---|
| **运行环境** | Java | 1.8.0_472 | 21 | 🚀 **跨代升级**，支持虚拟线程等现代特性 |
| **核心框架** | Spring Boot | 2.7.8 | 3.5.13 | 🚀 **大版本升级**，全面迁移至 Jakarta EE 规范 |
| **持久层** | MyBatis-Plus | 3.5.2 | 3.5.14 | ⬆️ **常规升级** |
| **安全鉴权** | Spring Security | 2.7.8 | 跟随 Spring Boot 3 | 🚀 **架构重构**，采用全新 `SecurityFilterChain` |
| **Token机制** | JWT | 0.9.1 | 0.12.7 | ⬆️ **大版本升级**，重构签名与验证 API |
| **接口文档** | Swagger | 3.0.0 | springdoc-openapi 2.8.16 | 🔄 **平替升级**，完美适配 Boot 3 及 OpenAPI 3 规范 |
| **模板引擎** | FreeMarker | 2.3.32 | *(无)* | ❌ **彻底废弃**，实现纯粹的前后端分离 |
| **JSON解析** | Fast Json | 2.0.7 | Jackson | 🔄 **全面替换**，回归 Spring 原生标准，移除潜在隐患 |
| **前端基座** | 前端集成环境 | vue-admin-template 4.4.0 | Vue 3 + Element Plus | 🚀 **彻底重构**，抛弃 Vue2，全量重写现代响应式页面 |
| **前端工具链**| 运行时与构建 | Node.js 22.22.1 | Bun 1.3.x + Vite | 🚀 **基建升级**，拥抱极速本地开发与毫秒级热更 |
| **主数据库** | MySQL | 5.7.44 | 8.4 | 🚀 **大版本升级**，统一规范字符集与现代 SQL 语法 |
| **缓存方案** | Redis | 7.2 | 7.2 | 🟢 **沿用**，且规范化了边界与幂等防重场景 |
| **消息中间件**| RabbitMQ | *(无)* | 4.2-management | ✨ **全新引入**，承接异步削峰、死信延时与高并发解耦 |

## 应用架构与详细接口说明书

- `docs\architecture-and-api-spec.md`

## 项目阶段

当前详细阶段状态已经单独整理到：

- `docs/project-status.md`

如果想快速了解升级路线与后续计划，建议优先阅读：

- `docs/upgrade/roadmap/pr11-after-roadmap.md`

后续微服务架构的迁移计划:
- `docs\upgrade\plan\microservice-migration-plan.md`

## 仓库结构

当前值得优先关注的目录：

- `src/main/java/`：后端业务与基础设施代码
- `src/main/resources/`：配置、Mapper XML、Flyway 迁移
- `src/test/java/`：单元测试与集成测试
- `web/`：当前新前端管理端
- `ops/`：Docker / Compose / 部署基线
- `docs/upgrade/`：升级路线、阶段记录、实施文档（已按 plan / logs / roadmap 等目录分类归档）

## 本地启动

### Windows 本地调试

后端：

```powershell
./start-backend.ps1
```

前端（首选 Bun）：

```bash
cd web
bun install
bun run dev
```

> **提示**：如果你的环境没有安装 Bun，依然可以使用传统的 Node.js 配合 npm 或 pnpm 来启动，只需替换对应命令即可，效果完全一致：
> 
> 使用 **pnpm**：
> ```bash
> cd web
> pnpm install
> pnpm dev
> ```
> 
> 使用 **npm**：
> ```bash
> cd web
> npm install
> npm run dev
> ```

如果需要在本地直接验证 Redis 缓存模式，可使用：

```powershell
./start-backend-redis.ps1
```

说明：

- `start-backend.ps1`：默认 `dev` + MQ 打开
- `start-backend-redis.ps1`：`dev` + `APP_CACHE_TYPE=redis`
- 新前端默认目录：`web/`

### Docker 镜像变体

项目提供两种后端 Docker 镜像，按场景选择：

| 镜像 | Dockerfile | JVM / 运行时 | 预估内存占用 | 预估镜像大小 | 适用场景 |
|:---|:---|:---|:---|:---|:---|
| `jasmine-backend` | `Dockerfile` | HotSpot (Temurin 21 JRE) + ZGC | ~450 MB | ~280 MB | 通用部署，兼容性最佳 |
| `jasmine-backend-openj9` | `Dockerfile.openj9` | OpenJ9 (Semeru 21 JRE) | ~300 MB | ~260 MB | 内存敏感环境，低成本 VPS |

#### HotSpot 镜像（默认）

```bash
docker build -f Dockerfile -t jasmine-backend:hotspot .
```

JVM 参数已针对容器化优化：ZGC 低延迟收集器、`MaxRAMPercentage=75.0` 按容器内存自动计算堆大小、字符串去重、压缩对象指针。

#### OpenJ9 镜像（省内存）

```bash
docker build -f Dockerfile.openj9 -t jasmine-backend:openj9 .
```

基于 [IBM Semeru Runtimes](https://developer.ibm.com/languages/java/semeru-runtimes/)（OpenJ9 JVM + OpenJDK 类库），内存占用通常比 HotSpot 低 30-60%。关键调优参数：

| 参数 | 说明 |
|:---|:---|
| `-Xgcpolicy:gencon` | 分代并发收集器，OpenJ9 默认策略 |
| `-XX:MaxRAMPercentage=70.0` | OpenJ9 堆外内存占比更高，设 70% 更保守 |
| `-Xtune:virtualized` | 虚拟化/容器环境调优，缩减线程栈和 JIT 缓存默认值 |
| `-Xshareclasses` | 共享类缓存，加速启动并减少运行时内存 |
| `-Xquickstart` | 牺牲少量峰值吞吐换取更快启动 |

**注意事项**：

- OpenJ9 的 Micrometer / Prometheus JVM 指标标签与 HotSpot 有差异（如 `jvm.memory.used` 的 area 标签），Grafana 面板可能需要适配
- OpenJ9 的 JIT 行为与 HotSpot 不同，依赖运行时动态代理的框架（如 MyBatis）建议充分测试后再上生产
- 生产环境使用 OpenJ9 镜像时，建议将 `BACKEND_MEMORY_LIMIT` 调低至 `384m`：

```bash
# .env 中
BACKEND_IMAGE=ghcr.io/jipzeongit/jasmine-backend-openj9
BACKEND_MEMORY_LIMIT=384m
BACKEND_MEMORY_RESERVATION=192m
```

#### 容器资源限制

生产环境 `docker-compose.yml` 已配置 `deploy.resources` 限制，配合 `MaxRAMPercentage` 自动计算堆大小：

```yaml
deploy:
  resources:
    limits:
      memory: ${BACKEND_MEMORY_LIMIT:-512m}
    reservations:
      memory: ${BACKEND_MEMORY_RESERVATION:-256m}
```

通过 `.env` 中的 `BACKEND_MEMORY_LIMIT` 和 `BACKEND_MEMORY_RESERVATION` 按镜像类型调整即可。

## API 与调试入口

后端启动后，常用入口如下：

- Swagger：`http://localhost:9999/swagger-ui/index.html`
- 健康检查：`http://localhost:9999/actuator/health`

如果开启了前端：

- 前端开发地址：`http://localhost:5173`

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

- `next`（当前主干）在通过后端基础检查后自动构建并推送镜像
- 手动 `workflow_dispatch`

镜像仓库：

- `ghcr.io/jipzeongit/jasmine-backend`（HotSpot JVM）
- `ghcr.io/jipzeongit/jasmine-backend-openj9`（OpenJ9）
- `ghcr.io/jipzeongit/jasmine-frontend`

构建顺序：HotSpot → OpenJ9 → Frontend

标签策略：

- 默认以分支名为前缀：`<branch>-latest`、`<branch>-<short_sha>`（目前主干为 `next`，故产物为 `next-latest`）

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

### 项目约束
- `docs/project-constraints.md`

### 项目状态
- `docs/project-status.md`

如果要继续理解当前路线与阶段边界，建议优先看：

- `docs/upgrade/roadmap/pr11-after-roadmap.md`
- `docs/upgrade/logs/pr12-5-redis-hardening.md`
- `docs/upgrade/logs/pr13-rabbitmq-bootstrap.md`
- `docs/upgrade/logs/pr13-mq-contract.md`
- `docs/upgrade/logs/pr13-5-redis-mq-hardening.md`
- `docs/upgrade/logs/pr14-docker-ops-deploy.md`
- `docs/upgrade/logs/pr15-full-frontend-migration.md`
- `docs/upgrade/logs/pr16-vite-chunk-optimization.md`
- `docs/upgrade/logs/pr18-high-concurrency-consistency.md`
- `docs/upgrade/logs/pr19-delayed-message-notification.md`
- `docs/upgrade/logs/pr19-frontend-notification.md`
- `docs/upgrade/logs/pr20-security-hardening-and-message-reliability.md`

## 说明

- 当前 `web/` 已作为正式前端迁移主线
- 当前 `PR18` 高并发增强与 `PR19` 延时提醒架构已平稳落地。
- 当前 `PR20` 进行一轮代码审查，发布代码审查说明 `docs\upgrade\review\pr20-code-review.md` ，代码审查、安全加固与消息隔离修复已完成。
- 后续主线将继续推进：
  - 分支 `microservices` 微服务前置评估（Nacos / Gateway 边界摸底）
  - 预约超时自动取消
  - 销售数据仓库与异步读模型（CQRS）
  - 复杂单据状态机

