# Jasmine

> **分支说明**：
> 当前 `microservices` 分支处于微服务迁移 Phase0/Phase1：已完成 Maven 多模块拆分、Nacos 配置/注册接入、Gateway 独立模块、参数化 Dockerfile 与 Compose 微服务拓扑基线。
> 业务远程调用、网关路由与外部入口治理将在后续 Phase2/Phase3 继续推进。

一个面向花店门店场景的管理系统，当前包含：

- 后端：Spring Boot + Spring Cloud 微服务骨架
- 前端：基于 `web/` 的 Vue 3 + Element Plus 管理端
- 中间件：MySQL、Redis、RabbitMQ、Nacos

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

## 微服务模块

| 模块 | 职责 |
|:---|:---|
| `jasmine-common-core` | Result、通用异常、DTO、JWT claims、通用工具 |
| `jasmine-common` | Servlet 服务侧基础设施：MyBatis、Redis、MQ、Outbox、过滤器、Swagger MVC |
| `jasmine-schema` | 独立 Flyway schema bootstrap，不再由 IAM 服务隐式承担迁移 |
| `jasmine-gateway` | WebFlux Gateway 入口，后续 Phase2 接入路由与鉴权 |
| `jasmine-iam` | 用户、角色、菜单、JWT 签发与 RBAC |
| `jasmine-product` | 花卉主数据与库存原子变更门面 |
| `jasmine-trade` | 销售、库存流水、库存预警与交易事件 |
| `jasmine-crm` | 会员、预约、延时提醒与站内通知 |

## 应用架构与详细接口说明书

- `docs\architecture-and-api-spec.md`

## 项目阶段

当前详细阶段状态已经单独整理到：

- `docs/project-status.md`

如果想快速了解升级路线与后续计划，建议优先阅读：

- `docs/upgrade/roadmap/pr11-after-roadmap.md`

后续微服务架构的迁移计划：
- `docs\upgrade\plan\microservices\microservice-migration-plan.md`

## 仓库结构

当前值得优先关注的目录：

- `jasmine-*`：后端 Maven 多模块
- `web/`：当前新前端管理端
- `ops/`：Docker / Compose / 部署基线
- `docs/upgrade/`：升级路线、阶段记录、实施文档（已按 plan / logs / roadmap 等目录分类归档）

## 本地启动

### 1. 先用 dev compose 起中间件

`ops/dev/docker-compose.yml` 仅启动 MySQL / Redis / RabbitMQ / Nacos 四个中间件，stateful 数据走 Docker named volume，不污染工程目录。

```bash
cp ops/.env.example ops/dev/.env   # 第一次启动需复制并修改密码
cd ops/dev
docker compose up -d
```

容器全部 healthy 之后，端口经 WSL 反向映射到 Windows 主机：

| 端口 | 服务 |
|:---:|:---|
| 13306 | MySQL |
| 6379 | Redis |
| 5673 / 15673 | RabbitMQ AMQP / 管理台 |
| 8848 / 9848 | Nacos HTTP / gRPC |

### 2. 导入 Nacos 配置

```bash
cd ops/nacos-config
bash import.sh 127.0.0.1:8848 dev
```

脚本会自动判断 Nacos 是否开启鉴权（dev 默认关闭），dev 环境可直接匿名导入。

### 3. 在本机以 dev profile 启动后端服务

仓库根目录提供 `start-backend.ps1` 用于本机调试，参数化指定模块：

```powershell
./start-backend.ps1 -Module gateway     # 启动 jasmine-gateway，默认选项
./start-backend.ps1 -Module iam         # 简写自动补 jasmine- 前缀
./start-backend.ps1 -Module product
./start-backend.ps1 -Module trade
./start-backend.ps1 -Module crm
```

脚本内部会先 `mvnw -pl <module> -am install -DskipTests` 安装依赖，再 `mvnw -pl <module> spring-boot:run` 单模块启动。环境变量与 `ops/dev/.env` 一致。

如果你不想用脚本，等价的手工命令：

```powershell
./mvnw.cmd -pl jasmine-iam -am install -DskipTests
./mvnw.cmd -pl jasmine-iam spring-boot:run
```

> 注意：直接 `./mvnw -pl jasmine-iam -am spring-boot:run` 会让 `spring-boot:run` 作用到父 POM 上，触发 "Unable to find a suitable main class"，必须分两步执行。

### 4. 启动前端

```bash
cd web
bun install
bun run dev
```

也可以使用根目录的 `start-frontend-bun.ps1` 一键启动，效果一致。

> 如果环境没有 Bun，可以使用 pnpm 或 npm 替代：`pnpm install && pnpm dev` 或 `npm install && npm run dev`。

### 微服务镜像构建

平时不需要在本机自己构建镜像；推 `microservices` 分支或 `v*` tag 即可由 GitHub Actions 矩阵化构建并推到 GHCR。如确需本机构建：

```bash
docker build -f Dockerfile --build-arg MODULE=jasmine-gateway -t jasmine-gateway:dev .
docker build -f Dockerfile --build-arg MODULE=jasmine-iam -t jasmine-iam:dev .
docker build -f Dockerfile --build-arg MODULE=jasmine-product -t jasmine-product:dev .
docker build -f Dockerfile --build-arg MODULE=jasmine-trade -t jasmine-trade:dev .
docker build -f Dockerfile --build-arg MODULE=jasmine-crm -t jasmine-crm:dev .
docker build -f Dockerfile --build-arg MODULE=jasmine-schema -t jasmine-schema:dev .
```

根目录只保留一个参数化 `Dockerfile`，通过 `ARG MODULE` 构建指定 Maven 模块，避免多 Dockerfile 漂移。Dockerfile 内已开启 BuildKit cache mount 与阿里云 Maven 镜像，重复构建依赖解析会复用 `~/.m2`。

#### 容器资源限制

生产环境 `docker-compose.yml` 已配置 `deploy.resources` 限制，配合 `MaxRAMPercentage` 自动计算堆大小：

```yaml
deploy:
  resources:
    limits:
      memory: ${SERVICE_MEMORY_LIMIT:-512m}
    reservations:
      memory: ${SERVICE_MEMORY_RESERVATION:-256m}
```

通过 `.env` 中的 `SERVICE_MEMORY_LIMIT` 和 `SERVICE_MEMORY_RESERVATION` 按服务统一调整即可。

## API 与调试入口

后端启动后，常用入口如下：

- Gateway 健康检查：`http://localhost:8080/actuator/health`
- IAM Swagger：`http://localhost:9101/swagger-ui/index.html`

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

- `docs/upgrade/logs/monolith/pr14-docker-ops-deploy.md`

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
- Gateway 健康检查：`http://<host>:${GATEWAY_PORT}/actuator/health`
- 各业务服务健康检查：容器内 `9101` / `9102` / `9103` / `9104`
- RabbitMQ 管理台：`http://<host>:${RABBITMQ_MANAGEMENT_PORT}`

## GitHub Actions 镜像构建

当前镜像构建工作流：

- `.github/workflows/docker-publish.yml`

当前支持：

- `microservices` 在通过后端基础检查后自动构建并推送微服务镜像
- 手动 `workflow_dispatch`

镜像仓库：

- `ghcr.io/jipzeongit/jasmine-gateway`
- `ghcr.io/jipzeongit/jasmine-iam`
- `ghcr.io/jipzeongit/jasmine-product`
- `ghcr.io/jipzeongit/jasmine-trade`
- `ghcr.io/jipzeongit/jasmine-crm`
- `ghcr.io/jipzeongit/jasmine-schema`
- `ghcr.io/jipzeongit/jasmine-frontend`

构建顺序：Schema → Gateway → IAM/Product/Trade/CRM → Frontend

标签策略：

- 默认以分支名为前缀：`<branch>-latest`、`<branch>-<short_sha>`

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
- `docs/upgrade/plan/microservices/microservice-migration-plan.md`
- `docs/upgrade/logs/microservices/phase0-maven-restructure.md`
- `docs/upgrade/logs/microservices/phase1-nacos-integration.md`
- `docs/upgrade/logs/microservices/phase0-phase1-code-quality-fixes.md`
- `docs/upgrade/review/microservices/phase0-phase1-code-review-2026-05-19.md`
- `docs/upgrade/logs/monolith/pr20-security-hardening-and-message-reliability.md`

## 说明

- 当前 `web/` 已作为正式前端迁移主线
- 当前 `PR18` 高并发增强与 `PR19` 延时提醒架构已平稳落地。
- 当前 `PR20` 进行一轮代码审查，发布代码审查说明 `docs\upgrade\review\pr20-code-review.md` ，代码审查、安全加固与消息隔离修复已完成。
- 后续主线将继续推进：
  - 分支 `microservices` 微服务前置评估（Nacos / Gateway 边界摸底）
  - 预约超时自动取消
  - 销售数据仓库与异步读模型（CQRS）
  - 复杂单据状态机

