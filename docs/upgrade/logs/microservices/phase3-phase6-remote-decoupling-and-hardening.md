# Phase 3 + Phase 6：服务间远程化与架构加固

## 时间

2026-05-21

## 概述

本次改动完成了路线图中的 Phase 3（服务间编译期耦合解除）和 Phase 6（架构加固），彻底消除了微服务之间的临时 Maven 依赖，使每个服务真正独立可部署。

---

## Phase 3.1 — trade → product 远程依赖清理

### 改动内容

1. **迁移 `ProductStockFacade` 接口到 `jasmine-common-core`**
   - 新增 `jasmine-common-core/.../dto/internal/ProductStockFacade.java`
   - `StockChangeResult` record 使用 `FlowerDTO` 替代 `Flower` 实体，解耦编译期依赖
   - 删除 `jasmine-product/.../support/ProductStockFacade.java`（旧位置）

2. **更新 `FlowerStockService`（product-service）**
   - 实现从 common-core 导入的 `ProductStockFacade` 接口
   - `adjustStock()` 返回值从 `Flower` 实体改为 `FlowerDTO`

3. **新增内部接口（product-service）**
   - `FlowerInternalController` 新增 `POST /internal/flower/batch`（批量查询花卉）
   - `FlowerInternalController` 新增 `GET /internal/flower/ids-by-name`（按名称模糊查询花卉 ID）

4. **更新 `FlowerClient`（trade-service）**
   - 新增 `getFlowersByIds()` 和 `getFlowerIdsByName()` 远程方法

5. **更新 `RemoteProductStockFacade`（trade-service）**
   - 移除 `Flower` 实体引用，直接使用 `FlowerDTO`

6. **重写 `SalesServiceImpl`（trade-service）**
   - 移除 `FlowerMapper` 注入，改用 `FlowerClient` 远程调用
   - 移除 `VipReadFacade` 注入，改用 `VipClient` 远程调用
   - 移除花卉缓存驱逐逻辑（trade-service 不再有本地花卉缓存）
   - `buildSalesVOs()` 使用 `FlowerClient.getFlowersByIds()` 批量加载花卉名称

7. **重写 `InventoryServiceImpl`（trade-service）**
   - 移除 `FlowerMapper` 注入，改用 `FlowerClient` 远程调用
   - `buildInventoryVOs()` 使用 `FlowerClient` 批量加载
   - `pageInventory()` 的花卉名称搜索改用 `FlowerClient.getFlowerIdsByName()`

8. **更新 `InventoryEventListener`（trade-service）**
   - 移除 `FlowerMapper` 注入，改用 `FlowerClient` 远程调用

9. **清理 `TradeApplication`**
   - 移除 `com.nfu.jasmine.flower`、`com.nfu.jasmine.vip`、`com.nfu.jasmine.appointment` 包扫描
   - 移除 `flower.persistence.mapper`、`vip.persistence.mapper`、`appointment.persistence.mapper`、`iam.persistence.mapper` Mapper 扫描

10. **移除 Maven 依赖**
    - `jasmine-trade/pom.xml` 移除 `jasmine-product` 和 `jasmine-crm` 依赖

11. **禁用受影响的集成测试**
    - `BusinessModelWorkflowIT` — 原本依赖 product/crm 的 Mapper 和 Entity，待重构
    - `OutboxAndAlertIntegrationIT` — 原本依赖 FlowerMapper，待重构

---

## Phase 3.2 — trade → crm 远程化

### 改动内容

1. **新增 `VipBasicDTO`（common-core）**
   - `jasmine-common-core/.../dto/internal/VipBasicDTO.java`

2. **新增 `VipInternalController`（crm-service）**
   - `GET /internal/vip/{id}` — 根据 ID 查询会员
   - `POST /internal/vip/batch` — 批量查询会员
   - `GET /internal/vip/exists/{id}` — 判断会员是否存在

3. **新增 `VipClient`（trade-service）**
   - `@HttpExchange` 声明式远程客户端，调用 crm-service 内部接口

4. **更新 `ClientConfig`（trade-service）**
   - 新增 `VipClient` Bean，指向 `http://crm-service`

5. **更新 `SalesServiceImpl`（trade-service）**
   - 会员查询从 `VipReadFacade` 改为 `VipClient`

---

## Phase 3.3 — crm → iam 远程化

### 改动内容

1. **新增 `IUserService.getActiveUserIdsByRoleNames()` 方法**
   - `IUserService` 接口新增方法声明
   - `UserServiceImpl` 新增实现，委托给 `UserMapper`

2. **新增 `UserInternalController` 端点（iam-service）**
   - `GET /internal/user/active-ids-by-roles?roleNames=...` — 按角色名查询活跃用户 ID 列表

3. **新增 `CrmUserClient`（crm-service）**
   - `@HttpExchange` 声明式远程客户端，调用 iam-service 内部接口

4. **新增 `CrmClientConfig`（crm-service）**
   - 配置 `CrmUserClient` Bean，指向 `http://iam-service`

5. **更新 `AppointmentReminderListener`（crm-service）**
   - 移除 `UserMapper` 注入，改用 `CrmUserClient.getActiveUserIdsByRoles()`

6. **清理 `CrmApplication`**
   - 移除 `com.nfu.jasmine.iam.persistence.mapper` Mapper 扫描

7. **移除 Maven 依赖**
    - `jasmine-crm/pom.xml` 移除 `jasmine-iam` 依赖

---

## Phase 6.1 — 优雅关闭

### 改动内容

所有服务的 `application.yml` 新增：
```yaml
server:
  shutdown: graceful
spring:
  lifecycle:
    timeout-per-shutdown-phase: 30s
```

涉及文件：
- `jasmine-gateway/src/main/resources/application.yml`
- `jasmine-iam/src/main/resources/application.yml`
- `jasmine-product/src/main/resources/application.yml`
- `jasmine-trade/src/main/resources/application.yml`
- `jasmine-crm/src/main/resources/application.yml`

---

## Phase 6.2 — 网关限流

### 改动内容

1. **新增 `spring-boot-starter-data-redis-reactive` 依赖（gateway pom.xml）**
   - `RequestRateLimiter` 过滤器需要 Redis Reactive 支持

2. **新增 `GatewayRateLimiterConfig`（gateway 模块）**
   - 定义 `ipKeyResolver` Bean，按客户端 IP 限流
   - 优先取 `X-Forwarded-For`，否则取 `RemoteAddress`

3. **更新 `ops/nacos-config/jasmine-gateway.yml`**
   - 所有路由新增 `RequestRateLimiter` 过滤器
   - 配置：replenishRate=50，burstCapacity=100

---

## Phase 6.3 — 前端幂等键

### 改动内容

`web/src/utils/request.ts` 请求拦截器新增：
```typescript
if (config.method === 'post' || config.method === 'put') {
  config.headers['X-Idempotency-Key'] = crypto.randomUUID()
}
```

每次 POST/PUT 请求自动生成 UUID 幂等键，配合后端 `RequestIdempotencyService` 防止重复提交。

---

## Phase 6.4 — 异常处理器补充

### 改动内容

`GlobalExceptionHandler` 新增：
- `TimeoutException` 处理 — 远程调用超时返回"服务调用超时，请稍后重试"
- `CallNotPermittedException` 处理 — Resilience4j 断路器熔断返回"下游服务暂时不可用"
  - 通过类名字符串匹配，避免 common 模块直接依赖 resilience4j

---

## Phase 6.5 — RestClient 重试策略

### 说明

RestClient 层暂未配置 Spring Retry 重试。当前已通过 Resilience4j 断路器（`RemoteProductStockFacade`）保护远程调用，断路器开启后快速失败，半开状态自动探测恢复。超时配置（3s 连接 / 5s 读取）已足够覆盖瞬时不可用场景。如需进一步重试，可在 `ClientConfig` 中添加 `RetryInterceptor`。

---

## 验证要点

1. trade-service 编译不再依赖 jasmine-product 和 jasmine-crm
2. crm-service 编译不再依赖 jasmine-iam
3. trade-service 通过 `FlowerClient` 远程调用 product-service 查询花卉信息
4. trade-service 通过 `VipClient` 远程调用 crm-service 查询会员信息
5. crm-service 通过 `CrmUserClient` 远程调用 iam-service 查询活跃用户
6. 所有 `/internal/**` 接口通过 `X-Gateway-Token` 共享密钥保护
7. 网关限流按 IP 生效（需 Redis 可用）
8. 前端 POST/PUT 请求自动携带 `X-Idempotency-Key`

## 遗留事项

1. `BusinessModelWorkflowIT` 和 `OutboxAndAlertIntegrationIT` 已禁用，需要重构为跨服务端到端测试
2. RestClient 重试策略可根据实际运行情况补充

---

## 代码审查修复（2026-05-21）

基于 `docs/upgrade/review/microservices/phase3-phase6-code-review.md` 审查报告，修复以下问题：

### P0 — 编译错误 / 启动阻塞

| 问题 | 修复 |
|:---|:---|
| `SalesServiceImpl` 调用 `vip.getRealName()` 但 `VipBasicDTO` 只有 `name` 字段，编译失败 | 改为 `vip.getName()` |
| Gateway 引入 `RequestRateLimiter` + `spring-boot-starter-data-redis-reactive` 但未导入 `jasmine-redis-common.yml`，限流无法初始化 | `application.yml` 的 `config.import` 补充 `jasmine-redis-common.yml` |

### P1 — 数据丢失 / 数据质量

| 问题 | 修复 |
|:---|:---|
| `SalesServiceImpl` 硬编码 `setVipPhone(null)` 但 `VipBasicDTO` 有 `phone` 字段 | 改为 `vip.getPhone()` |
| `InventoryEventListener` 中 `upsertAlert` 的 `safeStock` 硬编码为 0，库存预警阈值失效 | `FlowerDTO` 补充 `safeStock` 和 `currentStock` 字段，`FlowerInternalController.toFlowerDTO()` 赋值，`InventoryEventListener` 使用 `flower.getSafeStock()` |

### P2 — 代码质量

| 问题 | 修复 |
|:---|:---|
| `RemoteProductStockFacade` 的 `@Primary` 已无 Bean 竞争对象，Javadoc 过时 | 移除 `@Primary`，更新 Javadoc |
| `FlowerInternalController.getFlowerIdsByName()` 空字符串会匹配全表 | 补充 `StringUtils.hasText()` 防护，空字符串返回空列表 |
| `VipInternalController.getVipById()` 返回 null 而非抛异常，与其他内部 Controller 风格不一致 | 改为抛出 `BusinessException(NOT_FOUND)`，`SalesServiceImpl.validateVipIfPresent()` 补充异常捕获 |
| `CrmClientConfig` 与 `ClientConfig` 代码高度重复 | 提取 `InternalClientFactory` 到 `jasmine-common`，trade 和 crm 的 ClientConfig 委托给工厂方法 |
| `AppointmentReminderListener` 中角色名硬编码 | 提取为 `REMINDER_RECEIVER_ROLES` 常量 |

### 更新的文件清单

| 文件 | 改动 |
|:---|:---|
| `jasmine-trade/.../SalesServiceImpl.java` | 修复 `getName()`/`getPhone()` + `validateVipIfPresent()` 异常捕获 |
| `jasmine-gateway/src/main/resources/application.yml` | 补充 `jasmine-redis-common.yml` 导入 |
| `jasmine-common-core/.../FlowerDTO.java` | 新增 `safeStock`、`currentStock` 字段 |
| `jasmine-product/.../FlowerInternalController.java` | `toFlowerDTO()` 赋值新字段 + `getFlowerIdsByName()` 空字符串防护 |
| `jasmine-trade/.../RemoteProductStockFacade.java` | 移除 `@Primary`，更新 Javadoc |
| `jasmine-trade/.../InventoryEventListener.java` | 使用 `flower.getSafeStock()` 替代硬编码 0 |
| `jasmine-crm/.../VipInternalController.java` | `getVipById()` 返回 null 改为抛异常 |
| `jasmine-common/.../infra/client/InternalClientFactory.java` | 新增：公共 HTTP 客户端工厂 |
| `jasmine-trade/.../infra/client/ClientConfig.java` | 委托给 `InternalClientFactory` |
| `jasmine-crm/.../infra/client/CrmClientConfig.java` | 委托给 `InternalClientFactory` |
| `jasmine-crm/.../AppointmentReminderListener.java` | 角色名提取为 `REMINDER_RECEIVER_ROLES` 常量 |

---

## 补写单元测试（2026-05-21）

基于代码审查中识别的测试缺口，补写 7 个单元测试文件，覆盖各模块核心业务逻辑和安全链路。

### 测试文件清单

| 模块 | 测试文件 | 用例数 | 覆盖场景 |
|:---|:---|:---|:---|
| **product** | `FlowerStockServiceTest` | 8 | CAS 库存扣减成功、库存不足拒绝、花卉不存在、已删除花卉、CAS 并发重试、最大重试次数、零库存边界、更新成本价 |
| **gateway** | `JwtAuthGlobalFilterTest` | 8 | 内部路径 403 拦截、白名单放行、无 token 返回 401、无效 token 返回 401、有效 token 注入请求头、客户端伪造头被剥离、refresh token 拒绝、过滤器顺序 |
| **trade** | `RemoteProductStockFacadeTest` | 4 | 远程调用成功、业务失败透传、422 响应处理、null 结果兜底 |
| **trade** | `SalesServiceImplTest` | 4 | 创建销售单全链路、空明细拒绝、会员不存在拒绝、无会员创建、今日经营统计 |
| **crm** | `AppointmentReminderListenerTest` | 5 | 正常生成站内信、幂等跳过、已删除预约跳过、预约不存在跳过、改期幽灵消息跳过 |
| **common** | `GlobalExceptionHandlerTest` | 9 | 业务异常、超时异常、约束违反、参数缺失、类型不匹配、数据库约束、通用异常、绑定异常、null FieldError |
| **common** | `InternalEndpointGuardFilterTest` | 7 | 有效 token 放行、无 token 拒绝、错误 token 拒绝、actuator 豁免、swagger 豁免、业务路径过滤、JSON 错误响应格式 |

### 测试策略说明

- 所有单元测试使用 `@ExtendWith(MockitoExtension.class)` + `@Mock` / `@InjectMocks`，不启动 Spring 上下文，执行速度快
- Gateway 测试使用 `MockServerWebExchange` 模拟 WebFlux 请求
- 远程客户端相关测试通过 mock `FlowerClient`、`VipClient`、`CrmUserClient` 隔离网络依赖
- 断言使用 AssertJ（`assertThat`）保持与现有测试风格一致
