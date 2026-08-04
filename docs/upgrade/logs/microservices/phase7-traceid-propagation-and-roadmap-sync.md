# traceId 跨服务传播修复与路线图文档对齐

日期：2026-08-04

## 背景

架构核对时发现一个已实现但未真正生效的可观测性断点：`RequestTraceFilter`
在入口生成 traceId/requestId 并写入 MDC、回写响应头，但**跨服务 RestClient 调用
并不传播这两个头**——`InternalClientFactory` 创建 RestClient 时只注入
`X-Gateway-Token`，没有从 MDC 取 traceId 注入出站请求；网关侧也无任何生成/透传
traceId 的过滤器。

后果：trade→product、trade→crm、crm→iam 每一跳都会在下游重新生成新 traceId，
带 traceId 的 `AccessLogMessage` 发到 MQ 后**跨服务无法关联**，traceId 实际只在
单服务内有效。

同时发现路线图文档与代码状态漂移：`microservices-future-roadmap.md` 仍把
Phase 3（解耦）、Phase 6（优雅关闭/限流/幂等键/异常处理）列为待办，而
这些已全部落地；`future-microservice-memo.md` 仍以 PR11 为基线判断"微服务是
远期学习目标"，与已完成的微服务迁移事实矛盾。

## 改动内容

### 1. traceId 跨服务传播修复（核心）

**新增 `TraceContextPropagatingInterceptor`**（`jasmine-common/.../infra/client/`，
package-private 顶层类，便于单测）：

- 实现 `ClientHttpRequestInterceptor`，从 `MDC` 取 `traceId`/`requestId`，
  注入到出站请求的 `X-Trace-Id`/`X-Request-Id` 头。
- 头名与 `RequestTraceFilter`、网关 `TraceIdGlobalFilter` 三方一致。
- MDC 无值时（如定时任务上下文）不注入，留给下游自行生成，避免引入孤立 trace。
- 已存在的同名头不覆盖，保留上层显式设置的值。

**更新 `InternalClientFactory`**：`createClient()` 注册上述拦截器
（`.requestInterceptor(new TraceContextPropagatingInterceptor())`）。
所有 trade/crm 的远程客户端（`FlowerClient`/`VipClient`/`UserClient`/`CrmUserClient`）
经此工厂创建，自动获得传播能力，无需逐个改动。

**新增网关 `TraceIdGlobalFilter`**（`jasmine-gateway/.../filter/`）：

- `GlobalFilter`，`order = Ordered.HIGHEST_PRECEDENCE`，先于 `JwtAuthGlobalFilter`
  （-100）执行，保证鉴权日志也能带上 traceId。
- 入站 `X-Trace-Id`/`X-Request-Id` 有值时复用（与反代 Nginx 链路一致），
  缺失时生成无连字符 UUID。
- 回写到网关响应头，便于客户端凭 traceId 关联后端日志。
- 注入到下游请求头：仅缺失时注入，避免多值头污染（traceId 非安全敏感头，
  不像 `X-User-Id` 那样强制剥离客户端值）。

### 2. 配套单测（8 个用例，全过）

| 测试文件 | 用例数 | 覆盖场景 |
|:---|:---|:---|
| `jasmine-common/.../TraceContextPropagatingInterceptorTest` | 4 | MDC 有值时注入两个头；MDC 空时不注入；已存在头不覆盖；MDC 仅 traceId 时只注入 trace 头 |
| `jasmine-gateway/.../TraceIdGlobalFilterTest` | 4 | 缺失时生成并写入请求/响应头；入站有值时复用；入站有值时不重复注入；order 为 HIGHEST_PRECEDENCE |

### 3. 路线图与备忘录文档对齐

- `docs/upgrade/roadmap/microservices-future-roadmap.md`：
  - 顶部新增「状态更新（2026-08-04）」横幅，逐条列出已落地的严重/高优先级项。
  - 第六节路线图全量重排，引入 ✅/⏳/⏸ 进度标记：Phase 3.1/3.2/3.3、
    Phase 6.1~6.4 标 ✅；新增 Phase 7.1（本次 traceId 修复）标 ✅；
    Phase 6.5（Spring Retry）、7.5（Zipkin）、7.7（Loki）、Phase 8/9
    明确标 ⏸ 暂缓并给出触发条件；新增 Phase 7.2（iam 死配置清理）、
    Phase 7.3（iam 单测）、Phase 7.4（契约测试）作为真实待办。
- `docs/upgrade/memo/future-microservice-memo.md`：
  - 顶部新增「状态」标注，说明本文档多数判断已过时、不再作为决策依据，
    指向当前架构 spec、迁移规划、路线图三份有效文档。下文原文保留不动作为历史记录。

## 设计决策

### 为什么不直接上 Zipkin

单门店花店 4 服务的业务量，引入 Zipkin 容器 + 全链路追踪属过度工程。本次修复
让 traceId 在服务边界不丢，已有的 `RequestTraceFilter` + AccessLog MQ 链路
即可实现跨服务日志关联——拿到这个 80% 价值后，真出现排障瓶颈再评估 Zipkin。

### 为什么 traceId 头不强制剥离客户端值

`JwtAuthGlobalFilter` 剥离 `X-User-Id`/`X-User-Name`/`X-Gateway-Token` 是因为
它们是安全敏感头（身份伪造）。traceId 仅用于日志关联，且反代 Nginx 合理注入
traceId 是常见实践，复用入站值能保持上下游链路一致。故采用「有则复用、无则生成」
而非强制覆盖。

## 验证

- `./mvnw -pl jasmine-common,jasmine-gateway -am test -Dtest='TraceContextPropagatingInterceptorTest,TraceIdGlobalFilterTest'`：
  8 个用例全过。
- `./mvnw -pl jasmine-common,jasmine-gateway -am test -DskipITs=true`：
  两个模块单测合计 40 个全过（含原有 32 个 + 新增 8 个），无回归。

## 涉及文件

| 文件 | 改动 |
|:---|:---|
| `jasmine-common/.../infra/client/TraceContextPropagatingInterceptor.java` | 新增：出站 traceId 传播拦截器 |
| `jasmine-common/.../infra/client/InternalClientFactory.java` | 注册拦截器；移除原嵌套实现 |
| `jasmine-gateway/.../filter/TraceIdGlobalFilter.java` | 新增：网关入口 traceId 生成/透传过滤器 |
| `jasmine-common/src/test/.../TraceContextPropagatingInterceptorTest.java` | 新增：4 个单测 |
| `jasmine-gateway/src/test/.../TraceIdGlobalFilterTest.java` | 新增：4 个单测 |
| `docs/upgrade/roadmap/microservices-future-roadmap.md` | 顶部状态横幅 + 第六节路线图重排 |
| `docs/upgrade/memo/future-microservice-memo.md` | 顶部过时标注 |

## 后续注意

- traceId 现在能跨服务串联，但**跨 MQ 链路**（Outbox 消费端）仍依赖消费服务
  自己的 traceId。如需 MQ 消费也串联上游 trace，可在 `AccessLogMessage` 等
  消息体已有的 traceId 字段基础上，让消费端 `RequestTraceFilter` 之外的
  MQ 监听器入口写入 MDC——属可选增强，当前不必要。
- 真实待办集中在 Phase 7.2（iam 死配置清理）、Phase 7.3（iam 单测）、
  Phase 7.4（契约测试），详见 `docs/upgrade/plan/microservices/post-phase6-cleanup-plan.md`。
