# 微服务架构改进建议与后续路线图

> 本文档基于 `microservices` 分支 Phase 0~5 完成后的代码基线进行架构审查，识别当前架构中存在的问题、遗留的阶段性工作，并规划后续演进路线。
> 审查范围涵盖：服务边界完整性、横切关注点、基础设施、部署运维、前端适配。

> **状态更新（2026-08-04）**：本文档原为审查时点（Phase 0~5）的快照。其下列出的多数 🔴严重/🟠高优先级问题**已在本轮之前陆续落地**：
> - 🔴 2.1 服务间编译期耦合 → 已解除（Phase 3.1/3.2/3.3 完成，见 `logs/microservices/phase3-phase6-remote-decoupling-and-hardening.md`）
> - 🟠 3.1 优雅关闭 / 3.2 网关限流 / 3.4 前端幂等键 → 已落地（Phase 6.1/6.2/6.3）
> - 🟡 4.1 全局异常处理器补充（Timeout / CallNotPermitted）→ 已落地（Phase 6.4）
> - 🟠 3.3 分布式追踪 → **未引入 Zipkin**（按业务量判断暂不需要），改为先修复 traceId 跨服务断链（2026-08-04 完成，见 `logs/microservices/phase7-traceid-propagation-and-roadmap-sync.md`）
>
> 因此下文各小节的"问题/修复方案"仍作为历史审查记录保留，**实际进度以第六节路线图的 ✅ 标记与上述更新为准**。
> 仍在待办的真实缺口集中在：iam 单测与契约测试（详见 `plan/microservices/post-phase6-cleanup-plan.md`）；Phase 7.2 死配置清理已于 2026-08-04 完成。

---

## 一、审查结果总览

| 严重级别 | 数量 | 说明 |
|:---|:---|:---|
| 🔴 严重 | 2 | 服务间编译期耦合未解除、Phase 3 远程调用未全面落地 |
| 🟠 高 | 4 | 优雅关闭缺失、网关无限流、分布式追踪缺失、前端未用幂等键 |
| 🟡 中 | 5 | 全局异常覆盖不全、RestClient 无重试、API 无版本、Nacos 默认值风险、无合同测试 |
| 🟢 低 | 5 | 无监控大盘、无文件日志、无 K8s 编排、前端构建时变量、服务配置项待补充 |

---

## 二、🔴 严重问题

### 2.1 服务间编译期耦合未解除

**问题描述**：迁移规划中 `jasmine-trade/pom.xml` 和 `jasmine-crm/pom.xml` 标注为"阶段 0 临时依赖"的跨模块 Maven 依赖尚未移除。当前 trade-service 和 crm-service 在编译期直接依赖其他服务的 Mapper 和 Entity，导致：

- 编译期耦合：trade-service 的上下文中包含了来自 product/crm/iam 的 Bean 和 Mapper，依赖服务的表结构变更会引发 trade 的编译失败
- 运行时风险：trade-service 连接 `jasmine_trade` 库，该库无 `flower`/`vip`/`user` 表，被扫描进来的 Mapper 若直接查库会运行时报错；当前依赖 `@Primary` RemoteProductStockFacade 覆盖了本地实现才未触发，但这是脆弱的覆盖而非隔离
- 无法独立部署和演进各服务

**依赖现状**：

| 依赖方向 | Maven 依赖 | 直接导入 | 远程客户端 | 组件扫描 | 实际状态 |
|:---|:---|:---|:---|:---|:---|
| trade → product | 是（临时） | ProductStockFacade, Flower, FlowerMapper | ✅ FlowerClient + @Primary RemoteProductStockFacade | 扫描 flower.* | **双重耦合，远程优先但本地依赖仍在** |
| trade → crm | 是（临时） | VipReadFacade, Vip | ❌ 无远程客户端 | 扫描 vip.*, appointment.* | **纯本地调用，未远程化** |
| trade → iam | 否（通过 crm 间接传递） | 无直接导入 | ✅ UserClient | 扫描 iam.mapper | **仅远程调用，但 Mapper 扫描通过 crm→iam 传递依赖在 classpath 上可用，应清理** |
| crm → iam | 是（临时） | UserMapper | ❌ 无远程客户端 | 扫描 iam.mapper | **纯本地调用，未远程化** |

**修复方案**：

```
Phase 3.1 — trade → product 清理（前置：迁移接口定义）
  ⚠️ 必须先完成第 0 步，否则移除 Maven 依赖后 trade 编译直接报错

  0.【必须第一步】将 ProductStockFacade 接口 + StockChangeResult record
    从 jasmine-product 迁移到 jasmine-common-core（共享 DTO 包），
    因为 trade 的 RemoteProductStockFacade 实现了此接口，移除 product 依赖后编译会失败
  1. 验证 RemoteProductStockFacade 的 @Primary 已生效
  2. 移除 jasmine-trade/pom.xml 中 jasmine-product 依赖
  3. 将 SalesServiceImpl/InventoryServiceImpl 中的 Flower 实体引用替换为 FlowerDTO
  4. 将 FlowerMapper 查询替换为 ProductStockFacade 远程调用
  5. 移除 TradeApplication 中 flower.* 的组件扫描和 Mapper 扫描
```

---

## 三、🟠 高优先级问题

### 3.1 缺乏优雅关闭机制

**问题**：所有服务未配置优雅关闭。当服务被 SIGTERM 时：
- OutboxRelay 可能中断正在投递的消息
- RabbitMQ 通道可能非正常关闭导致消息丢失
- 数据库连接池直接释放

**修复方案**：
```yaml
# 各服务 application.yml 补充
server.shutdown: graceful
spring.lifecycle.timeout-per-shutdown-phase: 30s
```

同时补充 `@EventListener(ContextClosedEvent.class)` 监听，在关闭前等待 OutboxRelay 当前批次完成、关闭 RabbitTemplate、等待进行中的 RestClient 调用完成。

### 3.2 网关缺乏限流能力

**问题**：Gateway 未配置限流，当前业务量小不影响，但架构上存在空白。

**修复方案**：
```yaml
# jasmine-gateway.yml 新增 RequestRateLimiter 过滤器
spring.cloud.gateway.routes:
  - id: iam-service
    uri: lb://iam-service
    predicates:
      - Path=/user/**,/role/**,/menu/**,/**
    filters:
      - name: RequestRateLimiter
        args:
          redis-rate-limiter.replenishRate: 100
          redis-rate-limiter.burstCapacity: 200
          key-resolver: "#{@principalNameKeyResolver}"
```

引入 `spring-boot-starter-data-redis-reactive` 依赖（Gateway 已引入 Redis 则复用），使用 `PrincipalNameKeyResolver` 按用户限流，或自定义 `KeyResolver` 按 IP 限流。

### 3.3 缺乏分布式追踪

**问题**：当前基于 `RequestTraceFilter` 手动传递 traceId/requestId 仅能实现日志关联，无法实现：
- 跨服务调用链路可视化
- 各环节耗时分析
- 服务依赖拓扑图

**修复方案**：引入 Micrometer Tracing + Zipkin：

```xml
<!-- 各服务或 common 模块 pom.xml -->
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-tracing-bridge-brave</artifactId>
</dependency>
<dependency>
    <groupId>io.zipkin.reporter2</groupId>
    <artifactId>zipkin-reporter-brave</artifactId>
</dependency>
```

Gateway 的 `RestClient` 和 `@HttpExchange` 客户端自动继承 Tracing 拦截器。部署 Zipkin Server 到 docker-compose。

### 3.4 前端未使用幂等键

**问题**：后端 `RequestIdempotencyService` 支持 `X-Idempotency-Key` 请求头，但前端 Axios 发起的 POST 创建请求（销售单、库存动作、预约）未携带此头，仅依赖后端 payload 指纹回退。

**修复方案**：在 `web/src/utils/request.ts` 中，对 POST/PUT 请求自动生成 UUID 并注入 `X-Idempotency-Key` 头：

```typescript
// request.ts 请求拦截器补充
if (config.method === 'post' || config.method === 'put') {
  config.headers['X-Idempotency-Key'] = crypto.randomUUID();
}
```

---

## 四、🟡 中优先级改进

### 4.1 全局异常处理器覆盖不全

`GlobalExceptionHandler` 未覆盖以下异常：
- `TimeoutException` — RestClient 调用超时
- `CircuitBreakerException` — Resilience4j 熔断触发
- `RetryExhaustedException` — Spring Retry 重试耗尽

**修复**：在 `GlobalExceptionHandler` 中补充对应 handler 方法。

> **注意**：`AmqpRejectAndDontRequeueException` 是 MQ 消费端专用的拒绝异常，用于告知 Broker "不再重试此消息"，它不走 HTTP 响应通道，不应被 `@RestControllerAdvice` 捕获。该异常由 `RejectAndDontRequeueRecoverer` 在 Listener 级别处理，已在 `RabbitMqTopologyConfig` 中正确配置。

### 4.2 RestClient 调用缺乏重试

当前 trade-service 的 `ClientConfig` 仅配置了超时（3s/5s），未配置重试。当 product-service 瞬时不可用时将直接抛异常。

**修复**：在 `ClientConfig` 中为 `RestClient` 添加 `RetryTemplate`（最多 1 次重试，仅对 5xx 响应）。参考 OutboxRelay 的重试策略模式。

### 4.3 后端 API 缺乏版本管理

所有 Controller 使用无版本路径（如 `/flower/list`），未来接口变更时无法平滑过渡。

**建议**：当前业务量小、无外部客户端，暂不强制版本化。但后续引入新 API 时应规划版本策略：
- 向下兼容的变更（新增字段）不改变路径
- 破坏性变更时使用前缀 `/api/v2/flower/list`

### 4.4 Nacos 配置默认值安全风险

`application-dev.yml` 中存在硬编码默认密码（MySQL `123456`、RabbitMQ `123456`），且 Nacos 配置文件中的 JWT 密钥和网关令牌有 fallback 默认值。

**修复**：
- 生产环境 Nacos 配置移除所有 `${FALLBACK:default}` 表达式，仅使用 `${SECRET}` 无默认值
- Dev 配置的密码默认值仅在本地有效，确保 prod compose 中强制设置环境变量

### 4.5 缺乏服务间合同测试

当前无 `@RestClientTest` 测试验证服务间远程调用契约。当 product-service 修改 `/internal/flower` 接口时，trade-service 无法自动感知。

**建议**：引入 Consumer-Driven Contract 测试模式：
- 各服务 API 提供方维护 OpenAPI 规范
- 消费方使用 `@RestClientTest` + MockWebServer 验证接口兼容性
- 在 CI 中增加 consumer 端契约测试步骤

---

## 五、🟢 低优先级 / 增强项

### 5.1 可观测性体系

| 项目 | 当前状态 | 建议 |
|:---|:---|:---|
| 指标收集 | ✅ Prometheus 端点已暴露 | — |
| 监控大盘 | ❌ 无 Grafana 配置 | 提供社区版 Grafana dashboard JSON（JVM / HikariCP / HTTP / MQ 指标） |
| 告警规则 | ❌ 无 | Prometheus AlertManager 规则（服务宕机、队列堆积、熔断器开启） |
| 日志聚合 | ❌ 仅控制台 | Docker Compose 补充 Loki + Promtail，或 ELK 方案 |
| 日志持久化 | ❌ 仅控制台 | 生产 docker-compose 补充卷挂载 + 日志轮转 |

### 5.2 部署运维

| 项目 | 当前状态 | 建议 |
|:---|:---|:---|
| 容器编排 | Docker Compose | 中后期可演进到 Kubernetes（Helm Chart 或 Kustomize） |
| 蓝绿发布 | 无 | 结合 K8s + 滚动更新策略 |
| 数据库迁移 | Flyway（每服务独立） | ✅ 已就绪 |
| CI/CD | GitHub Actions + GHCR | ✅ 已就绪 |

### 5.3 前端运行时配置

当前所有 `VITE_*` 变量在构建时编译到 JS bundle 中，无法在部署时修改 API 地址。如需要多环境部署同一镜像，可引入运行时配置机制：

```nginx
# nginx entrypoint script: 运行时注入 window.__env__
location /config.js {
    add_header Content-Type application/javascript;
    return 200 'window.__env__ = { apiBaseUrl: "${API_BASE_URL:-/prod-api}" };';
}
```

---

## 六、后续实施路线图

> 进度标记：✅ 已完成 / ⏳ 待办 / ⏸ 暂缓（按业务量判断暂不需要，触发条件出现再评估）

```
当前基线（Phase 0~5 完成）
    │
    ├── ✅ Phase 3.1 — trade→product 远程依赖清理（1~2 天）
    │   ├── 验证 RemoteProductStockFacade 覆盖所有调用点
    │   ├── 移除 Maven 依赖 + 包扫描 + 实体直接引用
    │   └── 共享 DTO 迁移到 common-core
    │
    ├── ✅ Phase 3.2 — trade→crm 远程化（2~3 天）
    │   ├── crm-service 暴露 /internal/vip/** 内部接口
    │   ├── trade-service 新增 VipClient + RemoteVipReadFacade
    │   └── 移除 trade 对 crm 的编译期依赖
    │
    ├── ✅ Phase 3.3 — crm→iam 远程化（1~2 天）
    │   ├── iam-service 补充 /internal/user/active-ids-by-roles
    │   ├── 替换 AppointmentReminderListener 中的 UserMapper 直接调用
    │   └── 移除 crm 对 iam 的编译期依赖
    │
    ├── ✅ Phase 6 — 架构加固（3~5 天）
    │   ├── 6.1 优雅关闭配置（server.shutdown=graceful + 生命周期监听）
    │   ├── 6.2 网关限流（RequestRateLimiter + Redis）
    │   ├── 6.3 前端幂等键（Axios 拦截器注入 UUID）
    │   ├── 6.4 异常处理器补充（Timeout / CallNotPermitted）
    │   └── 6.5 ⏸ RestClient 应用层 Spring Retry —— 暂缓
    │           现有 Resilience4j 熔断 + LoadBalancer 实例切换重试已覆盖；
    │           出现真实瞬时失败再补
    │
    ├── ✅ Phase 7.1 — traceId 跨服务传播修复（2026-08-04）
    │   ├── 网关 TraceIdGlobalFilter：入口生成/复用 traceId，写入下游请求头与响应头
    │   ├── InternalClientFactory 出站拦截器：从 MDC 取 traceId 透传到下游
    │   └── 配套单测 8 个（TraceIdGlobalFilterTest / TraceContextPropagatingInterceptorTest）
    │
    ├── ✅ Phase 7.2 — iam 死配置与空壳清理（2026-08-04）
    │   ├── iam SecurityFilterChain 中针对其他服务端点的死规则（/site-message、/vip、
    │   │   /appointment、/flower、/sales、/inventory、/inventory-alert）已删除
    │   ├── /sys/** 下两个空壳控制器 + 4 个空壳 Service + 空 UserRoleMapper.xml 已删除
    │   └── 网关 jasmine-gateway.yml 的 /sys/** 路由谓词同步清理
    │
    ├── ⏳ Phase 7.3 — iam 单测补齐（真实缺口）
    │   └── UserServiceImpl / RoleServiceImpl / MenuServiceImpl / JwtUtil
    │       （当前 0 单测，是 RBAC/JWT 安全核心）
    │
    ├── ⏳ Phase 7.4 — 跨服务契约测试（真实缺口）
    │   └── @RestClientTest（trade→product / trade→crm / crm→iam）
    │
    ├── ⏸ Phase 7.5~7.8 — 可观测性补强（按需，触发条件出现再评估）
    │   ├── 7.5 ⏸ Micrometer Tracing + Zipkin —— 暂缓
    │   │       先靠 Phase 7.1 的 traceId 日志关联（已覆盖 80% 价值），
    │   │       真出现跨服务排障瓶颈再上 Zipkin
    │   ├── 7.6 ⏸ Grafana dashboard + Prometheus scrape job —— 可选
    │   │       metrics 端点已暴露，加 scrape + 一份社区 dashboard 成本低
    │   ├── 7.7 ⏸ Loki 日志聚合 —— 暂缓
    │   │       单节点 Compose，docker logs 够用
    │   └── 7.8 ✅ 生产日志文件持久化（卷挂载）—— 已落地
    │
    ├── 🟢 Phase 8 — 部署演进（可选，按需）
    │   ├── ⏸ K8s 部署清单（Helm Chart）—— 服务数>10/团队>5人再评估
    │   ├── 前端运行时配置机制
    │   └── 蓝绿发布 / 滚动更新策略
    │
    └── 🟡 Phase 9 — 长期演进（按需评估）
        ├── ⏸ Sentinel 流量控制（如业务量增长）
        ├── ⏸ Seata 分布式事务（如 trade 内部 sales+inventory 拆分）
        ├── 读写分离 / 分库分表（ShardingSphere）
        └── 事件驱动架构深化（Kafka 替代 RabbitMQ，如业务量剧增）
```

---

## 七、关键设计决策记录

| 决策 | 建议 | 理由 |
|:---|:---|:---|
| 服务间调用仍然使用 RestClient | 保持当前方案 | 调用量极小，HTTP 足够，不引入 Dubbo |
| 不引入 Sentinel | 保持当前方案 | 当前业务量不需要，Resilience4j 已满足基础熔断需求 |
| 不引入 Seata | 保持当前方案 | trade-service 内 sales+inventory 保持合并，不需要分布式事务 |
| 分布式追踪方案 | 推荐 Micrometer Tracing + Zipkin | Spring Boot 3.x 原生方案，接入成本低，社区成熟 |
| 日志聚合方案 | 推荐 Loki + Promtail | 与 Prometheus 生态一致，资源消耗低于 ELK |
| K8s 迁移时机 | 推荐服务数 > 10 或团队 > 5 人时 | 当前 5 服务 + Docker Compose 足够管理 |
