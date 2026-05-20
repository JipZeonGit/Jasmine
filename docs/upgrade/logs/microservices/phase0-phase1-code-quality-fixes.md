# Phase0 / Phase1 Code Quality Fix Log

日期：2026-05-19

## 背景

本轮修复针对 `docs/upgrade/review/microservices/phase0-phase1-code-review-2026-05-19.md` 中列出的 Phase0 / Phase1 代码审查问题，目标是把“模块拆分 + Nacos 接入”从代码层接线补齐到可构建、可编排、可继续演进的微服务基线。

## 修复范围

### 1. Docker 与 Compose 运行链路

- 根目录 `Dockerfile` 改为单一参数化 Dockerfile，通过 `ARG MODULE` 构建指定 Maven 模块。
- 移除单体时期的 `Dockerfile.openj9`，避免双 Dockerfile 长期漂移。
- `ops/dev/docker-compose.yml` 和 `ops/prod/docker-compose.yml` 从单 `backend` 容器改为真实微服务拓扑：
  - `jasmine-schema`
  - `jasmine-gateway`
  - `iam-service`
  - `product-service`
  - `trade-service`
  - `crm-service`
- Compose 中新增 `jasmine-schema` 作为 Flyway bootstrap，业务服务依赖其成功完成后再启动。
- Dev/Prod 均移除可直接使用的默认 `NACOS_AUTH_TOKEN`，改为 `.env` 显式注入。
- 外部入口收敛到 Gateway 与 Frontend；业务服务只保留容器内 healthcheck。

### 2. Gateway 依赖边界

- 新增 `jasmine-common-core`，承载 `Result`、通用异常、DTO、JWT claims 与核心工具。
- `jasmine-gateway` 改为只依赖 `jasmine-common-core`，不再继承 Servlet MVC、MySQL、Flyway、Redis、RabbitMQ 等服务侧基础设施。
- Gateway OpenAPI 依赖切换为 `springdoc-openapi-starter-webflux-ui`。
- Gateway Spring Cloud Gateway 依赖切换为 WebFlux server starter，保持 WebFlux 运行模型一致。

### 3. 服务扫描与装配

- `iam-service`、`product-service`、`trade-service`、`crm-service` 补齐服务侧组件扫描边界。
- 各服务补充 `infra.outbox` mapper 扫描，避免公共 Outbox 基础设施无法装配。
- `trade-service` 与 `crm-service` 对跨模块依赖使用显式 scan package，避免把其他服务的 `Application` 配置类整体扫入当前服务上下文。

### 4. 临时跨域依赖收敛

- 商品库存新增 `ProductStockFacade`，交易侧改为依赖库存门面，不再直接注入 `FlowerStockService` 实现类。
- CRM 会员读取新增 `VipReadFacade`，预约与交易侧改为依赖会员读取门面，不再直接散落 `VipMapper` 读操作。
- 这些 facade 仍是 Phase0/1 的进程内临时实现，后续 Phase3 可以替换为远程调用实现。

### 5. Nacos 配置拆分

- `jasmine-common.yml` 改为非敏感公共配置。
- 新增：
  - `jasmine-db-common.yml`
  - `jasmine-redis-common.yml`
  - `jasmine-mq-common.yml`
  - `jasmine-observability.yml`
- Gateway 只导入观测配置与自身配置，不再持有数据库、Redis、MQ 敏感配置。
- 各服务 `spring.config.import` 改为 `optional:nacos:`，本地无 Nacos 时不再在配置导入阶段直接失败。
- Dev profile 增加本地兜底数据库、Redis、RabbitMQ 配置，便于开发机不接 Nacos 时调试。

### 6. Nacos 导入脚本

- `ops/nacos-config/import.sh` 改用 `curl --data-urlencode` 导入配置，避免 YAML 中的 `&`、`+`、`%`、换行等内容被表单语义破坏。
- 支持 `NACOS_SCHEME=https`，保留默认内网 HTTP 使用方式。
- 命名空间创建也改为 URL 编码参数。

### 7. 测试与文档

- 新增 Gateway WebFlux 上下文烟雾测试。
- 新增 IAM / Product / Trade / CRM 服务级 context smoke IT。
- `README.md` 更新为当前 `microservices` 分支语义，修正模块目录、镜像构建方式、Compose 拓扑与镜像命名。

## 验证记录

- 已尝试执行全模块 Maven package。
- 第一次执行被沙箱网络权限拦截。
- 使用 JDK 21 重新执行后，构建推进到 `jasmine-crm` 并暴露一个缺失 import，已修复。
- 再次执行全模块 package 时，提权请求被当前环境的审批/配额机制拒绝，因此本轮无法在当前会话内完成最终 Maven 复验。

后续建议在本机可用 JDK 21 环境执行：

```bash
./mvnw -DskipUTs=true -DskipITs=true package
./mvnw -pl jasmine-gateway test
./mvnw verify -DskipUTs=true
```

## 后续注意

- Phase2 开始前，应先用 `ops/dev/docker-compose.yml` 拉起完整拓扑，验证 5 个服务注册到 Nacos。
- Gateway 路由接入后，应补充“外部只经 Gateway 访问”的网络与端口策略验证。
- `ProductStockFacade` / `VipReadFacade` 是远程化前的止血门面，Phase3 应替换为 OpenFeign / RestClient / MQ 查询模型等正式实现。


## 补充：开发链路调整与发布工作流（同日）

为了让 Phase2 之前的开发体验更顺畅，本轮在审查修复之上又做了以下调整：

### dev compose 收敛为「中间件 only」

- `ops/dev/docker-compose.yml` 仅启动 MySQL / Redis / RabbitMQ / Nacos 四个中间件，业务服务和前端不再在 dev compose 中构建。
- 数据卷统一改为 named volume，避免在 Windows + WSL 场景下 DrvFs 文件权限问题（典型表现：MySQL 8.4 启动时无法 chmod TLS 证书）。
- Nacos dev profile 默认关闭鉴权，配合 `import.sh` 的匿名分支可以一键导入配置。
- 新增 `ops/dev/README.md`，记录本机以 dev profile 启动各服务、连 WSL/Linux 中间件的标准用法。

### Dockerfile 与 Maven 构建

- 顶层 `Dockerfile` 引入 BuildKit `--mount=type=cache,target=/root/.m2`，多模块串行/并行构建可共享 Maven 仓库缓存。
- 新增 `.mvn/settings.xml`，内置阿里云镜像，供容器构建使用；本机 `~/.m2/settings.xml` 不受影响。
- mvnw 行尾在容器内主动 `sed -i 's/\r$//'`，兼容 Windows 上 CRLF 检出的工作树。

### 发布工作流改为微服务矩阵

- `.github/workflows/docker-publish.yml` 重写为：
  - `prepare` 计算镜像元数据
  - `build-backend` 用 matrix 并行构建 6 个后端模块（schema / gateway / iam / product / trade / crm）
  - `build-frontend` 单独构建前端
- 触发条件：`push` 到 `main` / `microservices`、`v*` tag、或 `workflow_dispatch`（支持 `modules` 输入用于按需构建）。
- `ops/.env.example` 镜像变量拆分为 `SCHEMA_IMAGE` / `GATEWAY_IMAGE` / ... / `FRONTEND_IMAGE`，与 `ops/prod/docker-compose.yml` 保持一致。
- `backend-ci.yml` 的 PR 触发分支由 `next` 改为 `main` / `microservices`。

### import.sh 兼容 Nacos 2.x

- Nacos 2.x 已不支持 HTTP Basic，本轮把 `import.sh` 改为先调 `/v1/auth/users/login` 换 `accessToken`，再用 token 写配置；同时对未开启鉴权的 dev 环境保留无 token 路径。

### Maven 全模块验证

- 在本机 GraalVM 21 上执行 `mvnw -B -DskipUTs=true -DskipITs=true clean package`，9 个模块全部 BUILD SUCCESS，6 个 exec jar 全部产出。
- `mvnw -pl jasmine-gateway test` 上下文烟雾测试通过，确认 P0-2 网关 WebFlux 装配无 MVC 冲突。
- 4 个中间件已在 WSL Mint Docker 中启动并通过 healthcheck，`import.sh` 已成功向 dev 命名空间写入 10 份配置。

业务服务镜像不再要求在本地拉起；Phase2 接入路由后，由 GitHub Actions 推到 GHCR，需要联调时再 `pull` 回来即可。


## 补充：本机实跑验证与 Nacos 客户端登录噪声修复（同日）

本节记录这一轮代码质量修复后，在本机以 GraalVM 21 + WSL Mint Docker（mysql/redis/rabbitmq/nacos）拉起后端的完整验证信号，以及途中发现的一处副作用修复。

### 实跑路径

1. `ops/dev/docker-compose.yml` 起 4 个中间件：均通过 healthcheck，端口经 WSL 反向转发到 Windows `127.0.0.1`（13306 / 6379 / 5673 / 8848）。
2. `ops/nacos-config/import.sh 127.0.0.1:8848 dev`：dev 关闭鉴权时走匿名分支，10 份 YAML 全部写入 `dev` 命名空间的 `JASMINE` 分组。
3. `start-backend.ps1 -Module gateway`：
   - 内部先 `mvnw -pl jasmine-gateway -am install -DskipTests`，jasmine（pom）、jasmine-common-core、jasmine-gateway 三模块 `BUILD SUCCESS`。
   - 中间附带跑 `GatewayContextSmokeTest`，`Tests run: 1, Failures: 0`，再次确认 Gateway WebFlux 上下文无 MVC 冲突。
   - 之后 `mvnw -pl jasmine-gateway spring-boot:run` 单模块启动，关键日志：
     - `[Nacos Config] Load config[dataId=jasmine-gateway.yml, group=JASMINE] success`
     - `[Nacos Config] Load config[dataId=jasmine-observability.yml, group=JASMINE] success`
     - `Netty started on port 8080 (http)`
     - `nacos registry, JASMINE jasmine-gateway 169.254.213.167:8080 register finished`
     - `Started GatewayApplication in 2.635 seconds`
     - 配置监听：`jasmine-observability.yml`、`jasmine-gateway.yml` 各 `cnt=1`。

至此 Phase0 / Phase1 在本机的验收条件已经齐全：构建可、上下文可、配置可拉、服务可注册、配置可监听。

### Nacos 客户端登录噪声修复

第一次跑 gateway 时观察到周期性 `User nacos not found` 错误日志（每 5 秒一次），定位到根因：

- dev 环境下 `ops/dev/docker-compose.yml` 中 `NACOS_AUTH_ENABLE=false`，Nacos derby 默认数据源里没有任何用户。
- 而所有服务的 `application.yml` 默认值是 `username: ${NACOS_USERNAME:nacos}` / `password: ${NACOS_PASSWORD:nacos}`，只要 yaml 里写了非空账号密码，Nacos 客户端就会去 `/v1/auth/users/login` 换 token，不论 server 是否真的开启鉴权。
- 副作用：服务端无用户，客户端 login 必然 500，每个 worker 线程持续刷错误。功能上不影响（注册和拉配置走 gRPC 已成功），但日志被打爆。

**修复**：把 5 个微服务 `application.yml` 中 `NACOS_USERNAME` / `NACOS_PASSWORD` 的默认值改为空字符串：

```yaml
spring:
  cloud:
    nacos:
      username: ${NACOS_USERNAME:}
      password: ${NACOS_PASSWORD:}
```

效果：

- 未注入凭据时 Nacos 客户端跳过 `/v1/auth/users/login`，dev 启动日志彻底干净。
- 开了鉴权的环境只需通过环境变量注入 `NACOS_USERNAME` / `NACOS_PASSWORD`，行为与原先一致。

### 本地启动脚本调整

- `start-backend-redis.ps1` 删除：缓存类型已下沉到 dev profile / Nacos，无需再开第二份脚本。
- `start-backend.ps1` 改为参数化：`-Module gateway|iam|product|trade|crm|schema`，支持 `iam` 等简写自动补 `jasmine-` 前缀。环境变量与 `ops/dev/.env` 完全对齐。
- 启动方式由"先 package 再 java -jar"改为 `mvnw -pl <module> -am install -DskipTests` 之后再 `mvnw -pl <module> spring-boot:run`。这一步分两次执行的原因：在 reactor 中如果 `-am` 与 `spring-boot:run` 同一步一起跑，Maven 会把 `spring-boot:run` 也作用于父 pom，从而触发 "Unable to find a suitable main class"。
- 脚本中所有提示信息改为英文，规避 Windows PowerShell 5.x 在中文系统上以 GBK 解析脚本时引号配对失败的问题。
- `start-frontend-bun.ps1` 行为不变，仅放宽 Bun 路径检查（找不到内置路径时回退到系统 PATH）。

### 留作 Phase2 起点的事项

- 5 个微服务都按相同方式逐一在本机起起来，确认全部注册到 dev 命名空间。
- Gateway 路由表（按服务名转发）正式接入。
- `ops/prod/docker-compose.yml` 联调用一次 GHCR 上构建好的镜像。


## 补充：Phase0 / Phase1 测试覆盖盘点

为了避免后续审查再次被点同样的问题，这里把本轮修复点的测试覆盖逐条列清，并说明哪些有意暂不补自动化测试。

### 已有自动化覆盖

| 修复点 | 覆盖测试 |
|:---|:---|
| Gateway WebFlux 上下文不冲突 MVC | `jasmine-gateway` `GatewayContextSmokeTest` |
| IAM 服务上下文装配 | `jasmine-iam` `IamContextIT`（Testcontainers） |
| Product 服务上下文装配 | `jasmine-product` `ProductContextIT` |
| Trade 服务上下文装配 | `jasmine-trade` `TradeContextIT` |
| CRM 服务上下文装配 | `jasmine-crm` `CrmContextIT` |
| 公共工具（JWT/会员号生成/请求 trace/请求级幂等）| `jasmine-common` `JwtUtilTest`、`jasmine-crm` `MembershipIdTest`、`RequestTraceFilterTest`、`RequestIdempotencyServiceTest` |
| MQ 行为 / Outbox 链路 | `jasmine-common` `RabbitMqBehaviorIT`、`jasmine-trade` `OutboxAndAlertIntegrationIT`、`BusinessModelWorkflowIT` |

服务级核心装配点全部有自动化保障，迁移过程中不会出现"上下文起不来"的回归。

### 暂不补自动化的项与理由

| 修复点 | 当前验证方式 | 暂不补自动化的理由 |
|:---|:---|:---|
| `ProductStockFacade` 进程内门面 | 由 trade 现有 IT 间接覆盖 | Phase3 会替换为远程 HTTP 实现，独立单测会被 Phase3 实现一并替换 |
| `VipReadFacade` 进程内门面 | 由 crm 现有 IT 间接覆盖 | 同上 |
| `jasmine-schema` 全表 Flyway bootstrap | 手动启 dev compose + 跑过迁移 | Phase4 会把单一 schema 拆成 4 个独立库，每服务自带迁移；现在写一份"V1~V8 整体跑通"的 IT 到 Phase4 立刻作废 |
| `Dockerfile` 参数化构建 | GitHub Actions matrix 构建即验证 | 自动化构建本身就是验证；额外写本地 IT 收益有限 |
| `dev` / `prod` compose 拓扑 | 本机 `docker compose up -d` 实跑 | 拓扑级测试需要 e2e 框架，超出本轮范围 |
| `ops/nacos-config/import.sh`（auth + 无 auth）| 本机对照空 / 启 auth 两个 Nacos 实跑 | 单 bash 脚本，写 bats 自验脚本边际收益低 |
| `application.yml` Nacos 默认账号空字符串 | 本机启动日志确认无 `User nacos not found` | 只影响日志噪声，不影响功能；既有 IT 间接覆盖客户端可用性 |
| `docker-publish.yml` matrix 重写 | GitHub Actions 实跑结果 | 工作流本身就是产物，自验通过即视为有效 |
| `backend-ci.yml` PR 触发分支调整 | PR 实际触发即验证 | 同上 |

### 决策记录

- **不补 `jasmine-schema` 的 Flyway 集成测试**：Phase4 数据库拆分会重写 schema 模块，当前一次性测试会被淘汰。
- **不补 Facade 的单元测试**：Phase3 会把这两个 Facade 替换为远程实现，新增单测属于一次性投入。
- **保留对 5 个服务的 ContextIT**：上下文装配是迁移期最大风险点，必须有自动化兜底，迁移到 Phase3/4 仍然有效。
