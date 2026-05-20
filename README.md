# Jasmine

> **分支说明**：
> 当前 `microservices` 分支已完成微服务迁移 Phase0 ~ Phase5：Maven 多模块拆分、Nacos 配置/注册接入、Gateway 路由与鉴权、服务间远程调用（RestClient + 共享密钥）、数据库按服务独立拆分、前端适配与安全加固、Docker 开发/生产环境全链路就绪。

一个面向花店门店场景的管理系统，当前包含：

- 后端：Spring Boot + Spring Cloud 微服务架构（5 业务服务 + 1 网关）
- 前端：基于 `web/` 的 Vue 3 + Element Plus 管理端
- 中间件：MySQL、Redis、RabbitMQ、Nacos

当前主线已经完成：

- 核心业务模型重建
- RabbitMQ 三阶段接入与稳定性收口
- Redis 与 MQ 的第一轮一致性 / 缓存稳态增强
- 高并发本地消息表（Outbox）及全链路强一致性预警机制构建
- 基于延时死信架构的业务级消息定时提醒及前端闭环
- 微服务 Phase0~Phase5 全阶段完成（模块拆分 → Nacos → Gateway → 服务通信 → 数据库拆分 → 前端适配与安全加固）

## 当前技术栈

### 后端

| 技术 | 说明 | 当前版本 |
|:---|:---|:---|
| Java | 运行时 | 21 |
| Spring Boot | 应用框架 | 3.5.13 |
| Spring Cloud Gateway | API 网关（WebFlux） | 跟随 Spring Cloud |
| Nacos | 配置中心 + 服务注册发现 | v2.4.3 |
| Spring Security | 认证鉴权 | 跟随 Spring Boot |
| Spring Actuator | 健康检查 / 运行指标 | 跟随 Spring Boot |
| Spring AMQP | RabbitMQ 集成 | 跟随 Spring Boot |
| Spring Retry | MQ 消费重试 | 跟随 Spring Boot |
| MyBatis-Plus | 持久层框架 | 3.5.14 |
| Flyway | 数据库迁移（各服务独立） | 跟随 Spring Boot |
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
| MySQL | 主数据库 | 8.4（生产）/ 8.0（WSL2 开发） |
| Redis | 缓存 / 幂等 | 7.2 |
| RabbitMQ | 消息队列 | 4.2-management |
| Nacos | 配置中心 / 服务注册 | v2.4.3 |
| Docker | 镜像与容器运行 | 当前基线已接入 |
| Docker Compose | 多服务编排 | 当前基线已接入 |
| GitHub Actions | CI / 镜像构建 | 当前基线已接入 |

## 微服务模块

| 模块 | 职责 | 端口 |
|:---|:---|:---|
| `jasmine-common-core` | Result、通用异常、DTO、JWT claims、通用工具 | — |
| `jasmine-common` | Servlet 服务侧基础设施：MyBatis、Redis、MQ、Outbox、Gateway Token 防护过滤器、Swagger MVC | — |
| `jasmine-schema` | 独立 Flyway schema bootstrap（遗留参考，各服务已自带迁移） | — |
| `jasmine-gateway` | WebFlux Gateway 入口，路由转发、JWT 鉴权、CORS 统一处理 | 8080 |
| `jasmine-iam` | 用户、角色、菜单、JWT 签发与 RBAC | 9101 |
| `jasmine-product` | 花卉主数据与库存原子变更门面 | 9102 |
| `jasmine-trade` | 销售、库存流水、库存预警与交易事件 | 9103 |
| `jasmine-crm` | 会员、预约、延时提醒与站内通知 | 9104 |

### 数据库拆分

每个业务服务拥有独立数据库，不再共享单一 `jasmine` 库：

| 服务 | 数据库 |
|:---|:---|
| jasmine-iam | `jasmine_iam` |
| jasmine-product | `jasmine_product` |
| jasmine-trade | `jasmine_trade` |
| jasmine-crm | `jasmine_crm` |

各服务启动时 Flyway 自动建表，dev compose 首次启动时 `init-databases.sql` 自动创建 4 个库并授权。

### 安全架构

- **网关层**：Gateway 统一进行 JWT 鉴权，向下游透传 `X-User-Id` / `X-User-Role` / `X-Gateway-Token`
- **下游服务层**：`InternalEndpointGuardFilter` 拦截所有业务请求，强制校验 `X-Gateway-Token` 共享密钥，阻止绕过网关直连微服务端口的越权攻击
- **服务间通信**：RestClient 调用内部接口时自动附加 `X-Gateway-Token`，下游服务校验通过后放行
- **放行白名单**：`/actuator/**`、`/swagger-ui/**`、`/v3/api-docs/**`、`/error`

## 应用架构与详细接口说明书

- `docs/architecture-and-api-spec.md`

## 项目阶段

当前详细阶段状态已经单独整理到：

- `docs/project-status.md`

如果想快速了解升级路线与后续计划，建议优先阅读：

- `docs/upgrade/roadmap/pr11-after-roadmap.md`

微服务架构的迁移计划与各阶段实施记录：

- `docs/upgrade/plan/microservices/microservice-migration-plan.md`
- `docs/upgrade/logs/microservices/`（Phase0 ~ Phase5 全记录）

## 仓库结构

当前值得优先关注的目录：

- `jasmine-*`：后端 Maven 多模块
- `web/`：当前新前端管理端
- `ops/`：Docker / Compose / 部署基线
- `ops/nacos-config/`：Nacos 配置文件与导入脚本
- `docs/upgrade/`：升级路线、阶段记录、实施文档（已按 plan / logs / roadmap 等目录分类归档）

## 本地启动

### 1. 先用 dev compose 起中间件

`ops/dev/docker-compose.yml` 仅启动 MySQL / Redis / RabbitMQ / Nacos 四个中间件。

```bash
cp ops/.env.example ops/dev/.env   # 第一次启动需复制并修改密码
cd ops/dev
docker compose up -d
```

> **WSL2 注意**：dev compose 中 MySQL 使用 `tmpfs` 挂载数据目录以规避 WSL2 + Docker overlay2 的 InnoDB redo log 兼容性问题。数据不持久化，每次容器重启会自动重新初始化。请保持一个 WSL 终端窗口打开以防止 WSL 自动关闭。

容器启动后，端口映射如下：

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

#### 方式一：一键启动全部后端

仓库根目录提供 `start-all-backends.ps1`，一次性编译并弹出 5 个独立窗口分别运行 Gateway / IAM / Product / Trade / CRM：

```powershell
.\start-all-backends.ps1
```

#### 方式二：单独启动指定模块

仓库根目录提供 `start-backend.ps1` 用于单模块调试：

```powershell
./start-backend.ps1 -Module gateway     # 启动 jasmine-gateway
./start-backend.ps1 -Module iam         # 简写自动补 jasmine- 前缀
./start-backend.ps1 -Module product
./start-backend.ps1 -Module trade
./start-backend.ps1 -Module crm
```

#### 方式三：手工命令

```powershell
./mvnw.cmd clean install -DskipTests           # 先全局编译
./mvnw.cmd -pl jasmine-iam spring-boot:run      # 再单模块启动
```

> 注意：直接 `./mvnw -pl jasmine-iam -am spring-boot:run` 会让 `spring-boot:run` 作用到父 POM 上，触发 "Unable to find a suitable main class"，必须分两步执行。

### 4. 启动前端

```bash
cd web
bun install
bun run dev
```

前端 Vite dev proxy 已配置为 `http://localhost:8080`（Gateway 端口），所有 API 请求经 Gateway 路由到各下游服务。

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
- Product Swagger：`http://localhost:9102/swagger-ui/index.html`
- Trade Swagger：`http://localhost:9103/swagger-ui/index.html`
- CRM Swagger：`http://localhost:9104/swagger-ui/index.html`
- Nacos 控制台：`http://localhost:8848/nacos`（默认 nacos/nacos）
- RabbitMQ 管理台：`http://localhost:15673`

如果开启了前端：

- 前端开发地址：`http://localhost:5173`

## Docker / Ops 基线

当前 Docker 与部署基线已经统一收口到 `ops/`：

- `ops/.env.example`
- `ops/dev/docker-compose.yml`（仅中间件，MySQL tmpfs 适配 WSL2）
- `ops/dev/init-databases.sql`（自动创建 4 个独立库）
- `ops/dev/up.sh` / `ops/dev/down.sh`
- `ops/prod/docker-compose.yml`（全栈：中间件 + 6 业务服务 + 前端）
- `ops/prod/up.sh` / `ops/prod/down.sh`
- `ops/nacos-config/`（Nacos 配置文件与导入脚本）

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
- Nacos 控制台：`http://<host>:${NACOS_PORT}/nacos`

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
- `docs/upgrade/logs/microservices/phase2-gateway-routing.md`
- `docs/upgrade/logs/microservices/phase3-service-communication.md`
- `docs/upgrade/logs/microservices/phase4-database-split.md`
- `docs/upgrade/logs/microservices/phase5-frontend-adaptation.md`
- `docs/upgrade/logs/microservices/phase5-post-adaptation-fixes.md`
- `docs/upgrade/logs/microservices/phase0-phase1-code-quality-fixes.md`
- `docs/upgrade/logs/microservices/phase2-5-review-fixes.md`
- `docs/upgrade/logs/microservices/phase3-5-timeout-circuitbreaker-cache-prefix.md`
- `docs/upgrade/review/microservices/phase0-phase1-code-review.md`
- `docs/upgrade/logs/monolith/pr20-security-hardening-and-message-reliability.md`

## 说明

- 当前 `web/` 已作为正式前端迁移主线
- 当前微服务迁移 Phase0 ~ Phase5 已全部完成，全链路可用
- 当前 `PR18` 高并发增强与 `PR19` 延时提醒架构已平稳落地
- 当前 `PR20` 代码审查、安全加固与消息隔离修复已完成
- 后续主线将继续推进：
  - 预约超时自动取消
  - 销售数据仓库与异步读模型（CQRS）
  - 复杂单据状态机
