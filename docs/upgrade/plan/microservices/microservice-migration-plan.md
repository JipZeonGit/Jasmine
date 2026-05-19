# Jasmine 微服务架构迁移规划（Spring Cloud Alibaba 2025.0.0.0）

> 本文档基于 `next` 分支 PR20 完成后的模块化单体代码基线，重新审视 `docs/upgrade/memo/future-microservice-memo.md` 中的微服务设想，结合当前实际成果，规划适配 Spring Cloud Alibaba 2025.0.0.0（Boot 3.5.x）的微服务架构演进方案。

---

## 一、PR11 微服务设想文档审查与过时内容识别

### 1.1 审查范围

本文档审查的核心对象为：

- `docs/upgrade/memo/future-microservice-memo.md`（微服务前瞻备忘录，以下简称"备忘录"）
- `docs/upgrade/roadmap/pr11-after-roadmap.md`（PR11 之后路线图，以下简称"路线图"）

### 1.2 PR20 已完成成果对照

以下能力在备忘录编写时仅存在于设想中，但截至 PR20 完成后已在单体中真实落地：

| 备忘录设想 | PR20 实际落地状态 | 对备忘录的影响 |
|:---|:---|:---|
| "sales 与 inventory 之间仍有明显一致性耦合" | 已通过 `FlowerStockService` CAS 原子更新 + `Outbox` 可靠投递 + `inventory.changed` 事件链实现销售→库存的事务内一致性闭环 | 耦合问题已缓解，但仍是强一致性关系，拆分时仍需审慎 |
| "vip 与 appointment 之间业务关系紧密" | 已通过外键约束 `fk_appointment_vip` + `AppointmentServiceImpl` 中跨域调用 `VipService` 实现 | 关系已规范化，但跨域引用仍然存在 |
| "先把 MQ 用深" | RabbitMQ 已深度使用：4 个交换机、7 条队列、5 个消费者、Outbox 可靠投递、死信延时架构、消息幂等 | **MQ 已不是前置条件，而是现有能力** |
| "先把 Redis 用准" | Redis 已承担：缓存（5 个缓存名+TTL抖动）、请求级幂等、MQ 消费幂等 | **Redis 已不是前置条件，而是现有能力** |
| "补齐 Outbox 可靠投递闭环" | Outbox 已完整：写入→Relay→Confirm/Return→PENDING/SENT/FAILED 状态机→不可路由失败回写 | **Outbox 是微服务拆分后保证事件不丢的核心基础设施，已就绪** |
| "补齐授权链路" | PR20 已完成：路径级 RBAC + `CurrentUserProvider` + `ROLE_xxx` authority 装配 | **授权模型已具备，是微服务网关统一鉴权的前置基础** |
| "站内信按用户隔离" | PR20 已完成：`receiver_user_id` 维度、按用户查询/已读 | **多服务共享消息中心的数据模型基础已就绪** |

### 1.3 过时内容识别

以下备忘录中的判断或建议已过时，需要更新：

| 过时内容 | 原始表述 | 更新判断 |
|:---|:---|:---|
| **"PR11 才刚完成业务模型重构"** | 备忘录以 PR11 为时间基线判断 | ❌ 过时。当前已完成到 PR20，业务模型已历经 V4~V8 共 5 轮 Flyway 迁移，模型已稳定 |
| **"PR12 还要继续收口域边界"** | 域边界尚未收口 | ❌ 过时。PR12 已完成模块化单体重组，域边界已按 iam/flower/inventory/sales/vip/appointment/infra 清晰划分 |
| **"建议顺序：PR12→12.5→13→14→15→18/19 再评估"** | 漫长的前置链路 | ❌ 过时。前置链路已全部完成，当前已站在评估起点上 |
| **"微服务是后续学习目标，不是当前最优落地形态"** | 将微服务定位为远期学习目标 | ⚠️ 需修正。当前前置条件已全部满足，微服务已从"远期学习目标"升级为"下一阶段可正式启动的演进方向" |
| **"第一批建议接入的微服务组件"中的 `OpenFeign`** | 建议用 OpenFeign 做服务间调用 | ⚠️ 需修正。Spring Cloud 2025.x 中 OpenFeign 已不再积极维护，建议改为 Spring 6+ 原生的 `HttpClient` + 声明式接口或 `Spring Cloud OpenFeign` 的替代方案 `RestClient` |
| **"Spring Cloud 2025.0.x"** | 版本表述模糊 | ⚠️ 需精确。当前确认的组合为 Spring Cloud 2025.0.0 + Spring Cloud Alibaba 2025.0.0.0 + Spring Boot 3.5.x |
| **方案 B（6服务）被标记为"更适合学习"** | 学习型拆法，不是首选 | ⚠️ 需重新评估。鉴于当前模块化单体的域划分已经非常清晰（6域+infra），方案B反而与现有代码结构一一对应，迁移成本更低 |

### 1.4 仍然有效的判断

| 有效内容 | 说明 |
|:---|:---|
| "sales + inventory 高度耦合，先放在同一个交易域更稳" | ✅ 仍然成立。`SalesServiceImpl` 中直接调用 `FlowerStockService#adjustStock()` 实现库存原子扣减，拆分后需引入分布式事务或事件驱动补偿 |
| "不建议保留职责模糊的 jasmine-core" | ✅ 仍然成立。当前 `common` + `config` + `infra` 的划分比单一 core 更清晰 |
| "Nacos + Gateway 第一批接入" | ✅ 仍然成立，且必要性更强 |
| "Sentinel / Seata / Dubbo 更适合更后期" | ✅ 仍然成立。当前业务量不需要限流和分布式事务 |
| "RabbitMQ 继续承担异步通知与事件驱动" | ✅ 仍然成立。现有 MQ 拓扑可直接复用 |

---

## 二、微服务架构规划

### 2.1 技术栈确认

| 组件 | 版本 | 用途 | 必要性 |
|:---|:---|:---|:---|
| Spring Boot | 3.5.x | 应用框架 | ✅ 已在用 |
| Spring Cloud | 2025.0.0 | 微服务基础框架 | ✅ 必须 |
| Spring Cloud Alibaba | 2025.0.0.0 | 国内企业级微服务组件 | ✅ 必须 |
| Nacos | 2.4.x（Alibaba 2025.0.0.0 适配版） | 注册中心 + 配置中心 | ✅ 第一批接入 |
| Spring Cloud Gateway | 跟随 Spring Cloud 2025.0.0 | 统一网关 + JWT 鉴权 + 路由转发 | ✅ 第一批接入 |
| RabbitMQ | 4.2 | 异步消息、事件驱动、死信延时 | ✅ 已在用 |
| MySQL | 8.4 | 主数据库 | ✅ 已在用 |
| Redis | 7.2 | 缓存、幂等 | ✅ 已在用 |
| MyBatis-Plus | 3.5.14 | 持久层 | ✅ 已在用 |
| **不引入** | Sentinel / Seata / Dubbo | 限流 / 分布式事务 / RPC | ❌ 当前业务不需要 |

### 2.2 推荐服务拆分方案

基于对当前单体实际结构的分析，推荐采用**方案 A+（4 服务 + 1 网关）**，在方案 A 稳定性基础上，将 `product-service` 与 `trade-service` 的边界做更精细的调整：

```
┌─────────────────────────────────────────────────────────────┐
│                  Spring Cloud Gateway                        │
│            统一入口 / JWT 鉴权 / 路由转发                      │
└──────┬──────────┬──────────┬──────────┬─────────────────────┘
       │          │          │          │
┌──────▼──┐ ┌─────▼───┐ ┌───▼────┐ ┌──▼──────────┐
│iam-service│ │product- │ │ trade- │ │  crm-service │
│认证/用户/ │ │service  │ │service │ │  会员/预约    │
│角色/菜单  │ │花卉主数据│ │销售/库存│ │  站内通知     │
└──────────┘ └─────────┘ └────────┘ └──────────────┘
       │          │          │          │
       └──────────┴──────────┴──────────┘
                     │
              ┌──────┼──────┐
              │      │      │
         ┌────▼─┐ ┌─▼───┐ ┌▼─────┐
         │MySQL │ │Redis│ │RabbitMQ│
         └──────┘ └─────┘ └──────┘
```

#### 服务职责明细

| 服务名 | 对应当前域 | 核心职责 | 数据库表 | MQ 角色 |
|:---|:---|:---|:---|:---|
| `iam-service` | iam | 登录认证、用户管理、角色管理、菜单管理、JWT 签发/刷新/吊销、RBAC 授权 | user, role, user_role, menu, role_menu, auth_refresh_token | 审计事件生产者 |
| `product-service` | flower | 花卉主数据 CRUD、花卉状态管理 | flower | 无直接 MQ 职责，但库存变更会间接触发 |
| `trade-service` | sales + inventory | 销售单管理、库存流水管理、库存原子扣减（CAS）、库存预警读模型、今日经营统计 | sales, sales_item, inventory, inventory_alert | 交易事件生产者（sales.created, inventory.changed）、库存预警消费者 |
| `crm-service` | vip + appointment + notification | 会员管理、预约管理（含延时提醒）、站内通知消息中心 | vip, appointment, site_message | 预约事件生产者/消费者、延时提醒消费者 |

#### 为什么选择方案 A+

1. **trade-service 不拆 sales + inventory 的理由**：
   - `SalesServiceImpl#saveSales()` 在同一事务内调用 `FlowerStockService#adjustStock()` 完成库存原子扣减
   - 如果拆成两个服务，必须引入分布式事务（Seata）或事件驱动补偿（Saga），复杂度远超当前业务量所需
   - `inventory.changed` 事件由 `trade-service` 内部 Outbox 触发，拆分后事件链路会跨服务，可靠性大幅降低

2. **crm-service 合并 vip + appointment 的理由**：
   - 预约创建时需要查询会员信息（`AppointmentServiceImpl` 调用 `VipMapper`），拆分后需远程调用
   - 延时提醒消费者最终写站内信，站内信也在同一域内，聚合更自然

3. **product-service 独立的理由**：
   - 花卉主数据是典型的"读多写少"共享数据，被 sales、inventory、前端等多方引用
   - 独立为服务后，其他服务通过 Feign/RestClient 查询，数据一致性由缓存策略保证

4. **iam-service 独立的理由**：
   - 认证鉴权是横切关注点，必须独立
   - 网关层 JWT 验证需要调用 iam-service 的公钥或用户状态查询
   - RBAC 数据变更频率极低，适合独立部署和缓存

### 2.3 不引入的组件及理由

| 组件 | 不引入理由 |
|:---|:---|
| **Sentinel** | 当前业务量极小（单门店），无流量洪峰和限流需求。可在未来真实出现高并发场景时再引入 |
| **Seata** | trade-service 内 sales+inventory 保持同一服务，不需要分布式事务。如果未来 trade-service 内部再拆，再评估 |
| **Dubbo** | 服务间调用量极小（4 个服务），HTTP/REST 调用足够。Dubbo 的 RPC 性能优势在当前规模下无法体现 |
| **服务网格（Istio/Linkerd）** | 过度工程化，当前 4 个服务不需要 sidecar 代理 |

---

## 三、迁移分阶段实施计划

### 阶段 0：前置准备（`microservices` 分支）✅ 已完成

**目标**：建立多模块 Maven 项目骨架，不改变业务逻辑

| 序号 | 工作项 | 说明 | 状态 |
|:---|:---|:---|:---|
| 0.1 | 创建 `microservices` 分支 | 从 `next` 分支切出 | ✅ |
| 0.2 | Maven 多模块重组 | 父 POM + 7 个子模块：`jasmine-common-core`、`jasmine-common`、`jasmine-schema`、`jasmine-gateway`、`jasmine-iam`、`jasmine-product`、`jasmine-trade`、`jasmine-crm` | ✅ |
| 0.3 | 抽取公共模块 | 拆分为 `jasmine-common-core`（Result/异常/DTO/JWT claims）和 `jasmine-common`（Servlet 服务侧基础设施），Gateway 仅依赖 core，避免 MVC 污染 | ✅ |
| 0.4 | 引入 Spring Cloud Alibaba BOM | 父 POM 中声明 `spring-cloud-alibaba-dependencies` 2025.0.0.0 + `spring-cloud-dependencies` 2025.0.0 | ✅ |
| 0.5 | 各服务独立配置文件 | 每个服务独立的 `application.yml` + `application-{dev,prod}.yml`，通过 `spring.config.import` 拉 Nacos 配置 | ✅ |
| 0.6 | 参数化 Dockerfile | 单一 `Dockerfile` + `ARG MODULE` 构建任一模块，废弃 `Dockerfile.openj9`，配合 BuildKit cache 与阿里云 mirror 加速 | ✅ |
| 0.7 | dev / prod compose 拓扑 | dev 收敛为中间件 only（业务服务由本机 Maven 起）；prod 拉 GHCR 上 7 个独立微服务镜像 | ✅ |

### 阶段 1：Nacos 接入 ✅ 已完成

**目标**：所有服务注册到 Nacos，配置中心化

| 序号 | 工作项 | 说明 | 状态 |
|:---|:---|:---|:---|
| 1.1 | 部署 Nacos Server | `nacos/nacos-server:v2.4.3`，单机模式，MySQL 持久化，已添加到 dev/prod docker-compose | ✅ |
| 1.2 | 各服务引入 `spring-cloud-starter-alibaba-nacos-discovery` | iam/product/trade/crm 四个业务服务 + gateway 均已引入 | ✅ |
| 1.3 | 各服务引入 `spring-cloud-starter-alibaba-nacos-config` | 通过 `spring.config.import=nacos:` 方式从 Nacos 拉取配置 | ✅ |
| 1.4 | 配置 `spring.application.name` | iam-service / product-service / trade-service / crm-service / jasmine-gateway | ✅ |
| 1.5 | 配置中心化 | 公共配置 `jasmine-common.yml`（MySQL/Redis）+ 各服务专属配置（RabbitMQ 等），预设文件在 `ops/nacos-config/`，通过 `import.sh` 导入 | ✅ |
| 1.6 | 验证 | 各服务启动后可在 Nacos 控制台看到注册信息，且能从 Nacos 命名空间拉取配置 | ✅ |

**Phase 1 实施细节**：

- **Nacos 连接配置**：各服务 `application.yml` 中通过 `spring.config.import` 从 Nacos 拉取共享配置，Nacos 地址通过环境变量 `NACOS_ADDR` 覆盖
- **命名空间策略**：dev 环境使用 `dev` 命名空间，prod 使用 `prod` 命名空间，配置 Group 统一为 `JASMINE`
- **配置分层**：
  - `jasmine-common.yml`：MySQL/Redis 连接（所有服务共享）
  - `jasmine-iam.yml` / `jasmine-trade.yml` / `jasmine-crm.yml`：RabbitMQ 连接（需要 MQ 的服务）
  - `jasmine-product.yml` / `jasmine-gateway.yml`：目前为空占位
- **本地开发兼容**：`application-dev.yml` 仅保留 Nacos 地址覆盖和开发特有配置，数据库/Redis/MQ 连接信息已从本地文件迁移到 Nacos
- **根 POM 修复**：移除重复的 `jasmine-iam` 声明，补充缺失的 `jasmine-trade` 和 `jasmine-gateway` 声明
- **本机实跑验证**：在 GraalVM 21 + WSL Mint Docker 中间件栈下，`start-backend.ps1 -Module gateway` 完整跑通：
  - `[Nacos Config] Load config[dataId=jasmine-gateway.yml, group=JASMINE] success`
  - `[Nacos Config] Load config[dataId=jasmine-observability.yml, group=JASMINE] success`
  - `nacos registry, JASMINE jasmine-gateway 169.254.213.167:8080 register finished`
  - 配置监听 `cnt=1`，gRPC 长连接到 9848
  - 启动耗时 2.6 秒，详见 `docs/upgrade/logs/microservices/phase0-phase1-code-quality-fixes.md`

### 阶段 2：Gateway 接入

**目标**：统一入口，JWT 网关鉴权，路由转发

| 序号 | 工作项 | 说明 |
|:---|:---|:---|
| 2.1 | 创建 `jasmine-gateway` 模块 | 引入 `spring-cloud-starter-gateway` |
| 2.2 | 配置路由规则 | `/api/iam/**` → iam-service、`/api/product/**` → product-service 等 |
| 2.3 | JWT 网关鉴权过滤器 | 在 Gateway 层验证 JWT 签名和有效性，解析 userId/username/roles 写入请求头传递给下游服务 |
| 2.4 | 下游服务信任网关传头 | 各服务从请求头中读取 `X-User-Id`、`X-User-Name`、`X-User-Roles`，替代当前 `CurrentUserProvider` 的 JWT 解析逻辑 |
| 2.5 | CORS 配置迁移 | 从后端各服务迁移到 Gateway 统一处理 |
| 2.6 | 前端 API 基路径调整 | 前端 Axios baseURL 改为 Gateway 地址 |

### 阶段 3：服务间通信

**目标**：跨域调用从本地方法调用改为远程 HTTP 调用

| 序号 | 工作项 | 说明 |
|:---|:---|:---|
| 3.1 | 选择服务间调用方式 | 推荐使用 Spring 6+ `RestClient` + 声明式接口，而非 OpenFeign（OpenFeign 在 Spring Cloud 2025.x 中已不积极维护） |
| 3.2 | 定义跨域调用接口 | 以下为需远程化的关键跨域调用 |

**跨域调用清单**：

| 调用方 | 当前本地调用 | 远程化方案 |
|:---|:---|:---|
| `trade-service` | `FlowerStockService#adjustStock()` | 调用 `product-service` 的 `/internal/flower/stock/adjust` 接口 |
| `trade-service` | `SalesServiceImpl` 中查询 `Flower` 售价/成本 | 调用 `product-service` 的 `/internal/flower/{id}` 接口 |
| `crm-service` | `AppointmentServiceImpl` 中查询 `Vip` | 调用 `crm-service` 内部方法（同一服务，无需远程） |
| `crm-service` | 预约提醒消费者写站内信 | 调用 `crm-service` 内部方法（同一服务，无需远程） |
| `trade-service` | `SalesServiceImpl` 中查询 `User` 操作员名 | 调用 `iam-service` 的 `/internal/user/{id}` 接口 |
| `iam-service` | `UserServiceImpl` 中查询 `Menu` 权限树 | `iam-service` 内部方法（同一服务，无需远程） |
| 所有服务 | `CurrentUserProvider` 获取当前用户 | 改为从网关传递的请求头中读取 |

| 序号 | 工作项 | 说明 |
|:---|:---|:---|
| 3.3 | 内部接口设计 | 所有仅供服务间调用的接口统一使用 `/internal/` 前缀，Gateway 不对外暴露 |
| 3.4 | 超时与重试策略 | RestClient 配置合理超时（连接 3s / 读取 5s），非核心调用允许降级 |
| 3.5 | 熔断保护 | 引入 `spring-cloud-circuitbreaker-resilience4j`，对远程调用做熔断降级（不引入 Sentinel） |

### 阶段 4：数据库拆分

**目标**：每个服务拥有独立数据库

| 序号 | 工作项 | 说明 |
|:---|:---|:---|
| 4.1 | 数据库拆分方案 | 如下表 |

| 服务 | 独立数据库 | 包含表 |
|:---|:---|:---|
| iam-service | `jasmine_iam` | user, role, user_role, menu, role_menu, auth_refresh_token |
| product-service | `jasmine_product` | flower |
| trade-service | `jasmine_trade` | sales, sales_item, inventory, inventory_alert, event_outbox（trade 相关） |
| crm-service | `jasmine_crm` | vip, appointment, site_message, event_outbox（crm 相关） |

| 4.2 | Flyway 迁移拆分 | 每个服务维护独立的 `db/migration/` 目录 |
| 4.3 | 跨服务数据一致性 | 采用事件驱动最终一致性：trade-service 的 `inventory.changed` 通过 MQ 通知 crm-service 更新站内信 |
| 4.4 | 共享数据查询 | 通过内部接口查询，不跨库 JOIN |

### 阶段 5：前端适配

| 序号 | 工作项 | 说明 |
|:---|:---|:---|
| 5.1 | API 基路径改为 Gateway | Axios `baseURL` 指向 Gateway 地址 |
| 5.2 | 路由前缀调整 | 后端路由在 Gateway 层统一添加 `/api/iam`、`/api/product` 等前缀，或保持原有路径由 Gateway 路由 |
| 5.3 | 跨域配置移至 Gateway | 前端只与 Gateway 通信 |

---

## 四、关键设计决策

### 4.1 网关鉴权 vs 服务鉴权

**决策**：网关统一 JWT 验证 + 下游服务信任网关头

- Gateway 负责：JWT 签名验证、过期检查、解析 claims
- Gateway 将 `X-User-Id`、`X-User-Name`、`X-User-Roles` 写入请求头
- 下游服务：从请求头读取用户信息，不再自行解析 JWT
- 下游服务之间内部调用：通过服务间信任机制直接调用，无需再走 JWT 验证

### 4.2 trade-service 内 sales + inventory 是否继续合并

**决策**：保持合并，不引入 Seata

理由：
- 当前 `SalesServiceImpl#saveSales()` 在同一事务内完成销售单创建 + 库存扣减 + Outbox 事件写入
- 如果拆分，必须引入分布式事务（Seata AT 模式）或事件驱动补偿（Saga）
- 当前业务量（单门店）完全不需要这个复杂度
- 如果未来业务量增长到需要拆分，可以先将 `FlowerStockService` 的库存扣减改为异步事件驱动（库存扣减由 `inventory.changed` 事件的消费者执行），再拆服务

### 4.3 配置中心策略

**决策**：Nacos 配置中心管理所有服务的配置

- 公共配置（MySQL/Redis/RabbitMQ 连接）放在 Nacos 共享配置 `jasmine-common.yml`
- 各服务特有配置放在 `jasmine-iam.yml`、`jasmine-product.yml` 等
- 敏感配置（JWT 密钥、数据库密码）使用 Nacos 加密配置或环境变量覆盖

### 4.4 服务间调用风格

**决策**：使用 Spring 6+ `RestClient` + 声明式接口，不使用 OpenFeign

理由：
- OpenFeign 在 Spring Cloud 2025.x 中已进入维护模式，不再积极开发
- Spring 6 原生的 `RestClient` 配合 `HttpInterface` 可实现类似 Feign 的声明式调用
- 依赖更少，学习成本更低，与 Spring 生态更契合

示例：

```java
// product-service 提供的内部接口
@RestController
@RequestMapping("/internal/flower")
public class FlowerInternalController {
    @GetMapping("/{id}")
    public FlowerDTO getFlowerById(@PathVariable Integer id) { ... }
    
    @PostMapping("/stock/adjust")
    public StockAdjustResult adjustStock(@RequestBody StockAdjustRequest request) { ... }
}

// trade-service 中的声明式客户端
@HttpExchange(url = "/internal/flower", contentType = "application/json")
public interface FlowerClient {
    @GetExchange("/{id}")
    FlowerDTO getFlowerById(@PathVariable Integer id);
    
    @PostExchange("/stock/adjust")
    StockAdjustResult adjustStock(@RequestBody StockAdjustRequest request);
}
```

### 4.5 Outbox 机制在微服务中的演进

**决策**：每个服务维护独立的 Outbox 表 + Relay

- trade-service 的 `event_outbox` 存储交易域事件（sales.created, inventory.changed）
- crm-service 的 `event_outbox` 存储客户域事件（appointment.created, appointment.delay）
- 各服务 Relay 独立扫描并发 MQ
- 消费者跨服务消费消息时，幂等键需包含来源服务标识

### 4.6 缓存策略

**决策**：各服务独立管理 Redis 缓存

- 缓存 key 加服务前缀：`iam:user:{id}`、`product:flower:list`、`trade:inventory:low-stock-count`
- 缓存失效仍由业务操作触发（如更新花卉后清除 `product:flower:*`）
- 共享数据（如花卉主数据）在调用方服务中缓存短 TTL 副本

---

## 五、风险与缓解

| 风险 | 影响 | 缓解措施 |
|:---|:---|:---|
| trade-service 内 sales+inventory 远期需再拆 | 届时需引入分布式事务或 Saga | 先将库存扣减改为事件驱动异步模式，降低拆分复杂度 |
| 服务间调用增加延迟 | 整体请求延迟上升 10-50ms | 对读多写少的共享数据（花卉、用户）做短 TTL 缓存 |
| 网关单点故障 | 所有请求无法到达后端 | Gateway 多实例部署 + Nacos 健康检查 + 自动摘除 |
| 数据一致性从强一致退化为最终一致 | 部分场景下用户看到的数据可能有延迟 | 关键链路（销售→库存扣减）仍保持同服务事务内强一致 |
| 运维复杂度上升 | 5 个服务 + 4 个中间件的监控和排障 | 引入 Micrometer + Prometheus + Grafana 统一监控，链路追踪留到后续 |

---

## 六、与备忘录的关键差异总结

| 维度 | 备忘录判断（PR11 时期） | 本文档规划（PR20 之后） |
|:---|:---|:---|
| 微服务定位 | "后续学习目标" | "下一阶段正式演进方向" |
| 前置条件 | 需完成 PR12~PR19 | 全部前置条件已满足 |
| 服务拆分方案 | 推荐方案 A（4服务） | 方案 A+（4服务+1网关），product-service 独立 |
| 服务间调用 | OpenFeign | RestClient + HttpInterface |
| 是否引入 Seata | "更适合更后期" | 明确不引入，trade-service 保持 sales+inventory 合并 |
| 是否引入 Sentinel | "更适合更后期" | 明确不引入，使用 Resilience4j 做简单熔断 |
| MQ 状态 | "先掌握 RabbitMQ" | RabbitMQ 已深度使用，拓扑可直接复用 |
| Redis 状态 | "先把 Redis 用准" | Redis 已承担缓存+幂等，策略可复用 |
| Outbox | 未提及 | 已落地，微服务中每服务独立 Outbox |
| 授权模型 | 未提及 | RBAC 已落地，网关统一鉴权有基础 |

---

## 七、实施优先级建议

```
阶段 0（骨架重组）  ──→  阶段 1（Nacos 接入）  ──→  阶段 2（Gateway 接入）
                                                       │
                                                       ▼
                                              阶段 3（服务间通信）
                                                       │
                                                       ▼
                                              阶段 4（数据库拆分）
                                                       │
                                                       ▼
                                              阶段 5（前端适配）
```

每个阶段建议在独立 PR 中完成，确保可回滚、可验证。
