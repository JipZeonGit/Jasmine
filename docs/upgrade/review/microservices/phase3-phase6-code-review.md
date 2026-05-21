# Code Review Report — Phase 3 + Phase 6 远程化与架构加固

> 审查日期：2026-05-21
> 审查范围：`microservices` 分支本地未提交变更（30 个文件，+312 / -500 行）
> 审查基线：Phase 0~5 完成后的代码基线 + `microservices-future-roadmap.md` + `phase3-phase6-remote-decoupling-and-hardening.md`

---

## 1. Executive Summary

**总体评价：一般（存在需要修复的问题）**

本轮改动完成了路线图中最关键的两个里程碑——服务间编译期耦合解除（Phase 3）和架构加固（Phase 6），整体方向正确且执行力强。trade-service 成功移除了对 product/crm 的 Maven 依赖，crm-service 移除了对 iam 的依赖，每个服务已真正可独立编译部署。

**核心问题概述：**

1. **🔴 P0 编译错误**：`SalesServiceImpl` 调用了 `VipBasicDTO.getRealName()`，但 `VipBasicDTO` 只有 `name` 字段没有 `realName` — 编译不通过
2. **🔴 P0 启动阻塞**：Gateway 引入了 `RequestRateLimiter` + `spring-boot-starter-data-redis-reactive`，但未导入 `jasmine-redis-common.yml` 配置，缺少 Redis 连接信息，限流功能无法初始化
3. **🟠 P1 数据丢失**：`VipBasicDTO` 有 `phone` 字段但 `SalesServiceImpl` 硬编码 `setVipPhone(null)` 并注释"暂无 phone 字段"，会员电话信息丢失
4. **🟠 P1 数据质量**：`InventoryEventListener` 中 `upsertAlert` 的 `safeStock` 参数硬编码为 `0`，库存预警阈值失效

---

## 2. Critical Issues（P0 — 必须修复）

### 2.1 `VipBasicDTO` 字段名不匹配导致编译失败

**文件**：[SalesServiceImpl.java](jasmine-trade/src/main/java/com/nfu/jasmine/sales/application/impl/SalesServiceImpl.java#L378)

**问题原因**：`VipBasicDTO`（common-core）定义的字段是 `name`（Lombok 生成 `getName()`），但 `SalesServiceImpl` 调用的是 `getRealName()`。

```java
// SalesServiceImpl.java L378 — 编译报错
vo.setVipName(vip == null ? null : vip.getRealName());
```

```java
// VipBasicDTO.java — 只有 name 字段
@Data
public class VipBasicDTO {
    private Integer id;
    private String vid;
    private String name;   // ← 生成的是 getName()，不是 getRealName()
    private String sex;
    private String phone;
}
```

**实际风险**：trade-service 无法编译，整个交易服务瘫痪。

**推荐修复**：

```java
// 方案 A（推荐）：修改 SalesServiceImpl 使用正确的 getter
vo.setVipName(vip == null ? null : vip.getName());
```

```java
// 方案 B：如果业务语义上确实需要 realName，在 VipBasicDTO 新增字段
private String realName;
// 并在 VipInternalController.toVipBasicDTO() 中补充赋值
dto.setRealName(vip.getName()); // Vip 实体的 name 字段在业务上就是真实姓名
```

**是否建议当前阶段立即修复**：✅ 必须立即修复，否则服务无法启动。

---

### 2.2 Gateway 缺少 Redis 连接配置，限流功能无法启动

**文件**：[application.yml (gateway)](jasmine-gateway/src/main/resources/application.yml)

**问题原因**：Gateway 新增了 `spring-boot-starter-data-redis-reactive` 依赖和 `RequestRateLimiter` 过滤器，但 `application.yml` 的 `config.import` 中没有导入 `jasmine-redis-common.yml`。

```yaml
# gateway application.yml — 当前 config.import
config:
  import:
    - optional:nacos:jasmine-observability.yml?refresh=true
    - optional:nacos:jasmine-gateway.yml?refresh=true
    # ❌ 缺少 jasmine-redis-common.yml
```

**实际风险**：

- 由于 `jasmine-redis-common.yml` 使用 `${REDIS_HOST}`（无默认值），Redis 自动配置会连接 `localhost:6379` 且无密码
- 如果 Redis 不在 localhost 或需要密码，连接失败 → `RequestRateLimiter` 初始化失败 → 所有路由请求返回 503
- dev 环境碰巧 Redis 在 localhost:6379 可能暂时不报错，但 **prod 必然崩溃**

**推荐修复**：

```yaml
# gateway application.yml config.import 补充
config:
  import:
    - optional:nacos:jasmine-observability.yml?refresh=true
    - optional:nacos:jasmine-redis-common.yml?refresh=true  # ← 新增
    - optional:nacos:jasmine-gateway.yml?refresh=true
```

**是否建议当前阶段立即修复**：✅ 必须立即修复，否则 prod 环境 Gateway 启动即崩溃。

---

## 3. Major Issues（P1 — 高优先级）

### 3.1 `SalesServiceImpl` 丢弃了 `VipBasicDTO` 已有的 `phone` 字段

**文件**：[SalesServiceImpl.java](jasmine-trade/src/main/java/com/nfu/jasmine/sales/application/impl/SalesServiceImpl.java#L379)

**问题原因**：注释声称 "VipBasicDTO 暂无 phone 字段"，但实际上 `VipBasicDTO` 有 `phone` 字段，且 `VipInternalController.toVipBasicDTO()` 也正确赋值了。

```java
// SalesServiceImpl.java L379
vo.setVipPhone(null); // VipBasicDTO 暂无 phone 字段  ← 注释与事实矛盾
```

**实际风险**：前端销售单列表中会员电话始终显示为空，影响店员操作体验。

**推荐修复**：

```java
vo.setVipPhone(vip == null ? null : vip.getPhone());
```

**是否建议当前阶段立即修复**：✅ 建议与 2.1 一并修复，代价极低。

---

### 3.2 `InventoryEventListener` 库存预警 `safeStock` 硬编码为 0

**文件**：[InventoryEventListener.java](jasmine-trade/src/main/java/com/nfu/jasmine/infra/mq/listener/InventoryEventListener.java#L68-L72)

**问题原因**：远程化后 `FlowerDTO` 不含 `safeStock` / `currentStock` 字段，代码用 `0` 代替。

```java
// InventoryEventListener.java L68-L71
inventoryAlertService.upsertAlert(
        flower.getId(),
        flower.getName(),
        0, // safeStock 需要从 flower 表获取，MQ 消息中未携带 ← 硬编码 0
        message.getAfterStock()
);
```

**实际风险**：

- `safeStock = 0` 意味着只有库存降为负数才会触发预警，等于预警功能形同虚设
- 如果 `upsertAlert` 的逻辑是"当 `currentStock < safeStock` 时标记预警"，那 `safeStock=0` 永远不会触发

**推荐修复**：

方案 A：在 `FlowerDTO` 中补充 `safeStock` 字段，`FlowerInternalController.toFlowerDTO()` 中赋值。

```java
// FlowerDTO 补充
private Integer safeStock;

// FlowerInternalController.toFlowerDTO()
dto.setSafeStock(flower.getSafeStock());

// InventoryEventListener
inventoryAlertService.upsertAlert(
        flower.getId(),
        flower.getName(),
        flower.getSafeStock() != null ? flower.getSafeStock() : 0,
        message.getAfterStock()
);
```

方案 B（最小改动）：在 `FlowerClient` 新增一个返回含 `safeStock` 的方法，或在 `InventoryChangedMessage` 中补充 `safeStock` 字段（推送端在发事件时直接从 product 查并携带）。

**是否建议当前阶段立即修复**：✅ 建议修复，否则库存预警功能失效。

---

### 3.3 `RemoteProductStockFacade` 上的 `@Primary` 已无实际作用，Javadoc 过时

**文件**：[RemoteProductStockFacade.java](jasmine-trade/src/main/java/com/nfu/jasmine/infra/client/RemoteProductStockFacade.java#L20-L27)

**问题原因**：jasmine-product 依赖已移除，trade 上下文中不再有 `FlowerStockService`（product 本地实现），`RemoteProductStockFacade` 是唯一的 `ProductStockFacade` Bean。`@Primary` 不再有 Bean 竞争对象。

```java
/**
 * 标记 @Primary 使其在 trade-service 中优先于本地实现。
 * 后续移除 jasmine-product 依赖后可去掉 @Primary。  ← 就是现在
 */
@Primary  // ← 不再需要
@Component
public class RemoteProductStockFacade implements ProductStockFacade {
```

**实际风险**：无功能风险，但注释与现实不一致会误导维护者。

**推荐修复**：

```java
/**
 * 远程商品库存门面实现 —— 通过 HTTP 调用 product-service 内部接口。
 * <p>
 * 使用 Resilience4j 断路器保护远程调用，当 product-service 持续不可用时快速失败，
 * 避免级联故障拖垮 trade-service。业务异常（库存不足等）直接透传，不触发降级。
 */
@Component
public class RemoteProductStockFacade implements ProductStockFacade {
```

**是否建议当前阶段立即修复**：✅ 建议修复，改动极小。

---

## 4. Minor Issues（P2 — 建议优化）

### 4.1 `CrmClientConfig` 与 `ClientConfig`（trade）代码高度重复

**文件**：
- [CrmClientConfig.java](jasmine-crm/src/main/java/com/nfu/jasmine/infra/client/CrmClientConfig.java)
- [ClientConfig.java](jasmine-trade/src/main/java/com/nfu/jasmine/infra/client/ClientConfig.java)

**问题原因**：两份配置类的 `createRequestFactory()` 方法、超时常量、`gatewaySharedToken` 注入和 `HttpServiceProxyFactory` 创建逻辑完全相同。

**实际风险**：修改超时配置时需要改两处，容易遗漏。

**推荐修复**：在 `jasmine-common` 中提取公共 `InternalClientSupport` 工具类：

```java
// jasmine-common/.../infra/client/InternalClientSupport.java
public class InternalClientSupport {
    public static <T> T createClient(RestClient.Builder builder, String baseUrl,
                                      String gatewayToken, Class<T> clientType) {
        RestClient restClient = builder.baseUrl(baseUrl)
                .requestFactory(createRequestFactory())
                .defaultHeader("X-Gateway-Token", gatewayToken)
                .build();
        return HttpServiceProxyFactory.builderFor(RestClientAdapter.create(restClient))
                .build().createClient(clientType);
    }
    // ...
}
```

**是否建议当前阶段立即修复**：✅ 已修复。提取 `InternalClientFactory` 到 `jasmine-common`，trade 和 crm 的 ClientConfig 委托给工厂方法。

---

### 4.2 `FlowerInternalController.getFlowerIdsByName()` 缺少模糊查询防护

**文件**：[FlowerInternalController.java](jasmine-product/src/main/java/com/nfu/jasmine/flower/web/internal/FlowerInternalController.java#L54-L62)

**问题原因**：`name` 参数直接传入 `LIKE` 查询，空字符串会匹配全表。

```java
@GetMapping("/ids-by-name")
public List<Integer> getFlowerIdsByName(@RequestParam String name) {
    return flowerService.list(new LambdaQueryWrapper<Flower>()
                    .like(Flower::getName, name))  // name="" → LIKE '%%' → 全表扫描
            .stream().map(Flower::getId).toList();
}
```

**实际风险**：如果前端传入空字符串或空格，会返回全部花卉 ID，虽然在内部接口场景下风险可控。

**推荐修复**：

```java
@GetMapping("/ids-by-name")
public List<Integer> getFlowerIdsByName(@RequestParam String name) {
    if (!StringUtils.hasText(name)) {
        return List.of();
    }
    return flowerService.list(new LambdaQueryWrapper<Flower>()
                    .like(Flower::getName, name.trim()))
            .stream().map(Flower::getId).toList();
}
```

**是否建议当前阶段立即修复**：✅ 建议修复，改动极小。

---

### 4.3 `VipInternalController.getVipById()` 返回 `null` 而非抛异常

**文件**：[VipInternalController.java](jasmine-crm/src/main/java/com/nfu/jasmine/vip/web/internal/VipInternalController.java#L28-L35)

**问题原因**：与 `FlowerInternalController.getFlowerById()` 和 `UserInternalController.getUserById()` 风格不一致——后两者对不存在的资源抛出 `BusinessException(NOT_FOUND)`，`VipInternalController` 却返回 `null`。

```java
// VipInternalController — 返回 null
public VipBasicDTO getVipById(@PathVariable Integer id) {
    Vip vip = vipMapper.selectById(id);
    if (vip == null || Integer.valueOf(1).equals(vip.getDeleted())) {
        return null;  // ← 其他内部 Controller 抛异常
    }
    return toVipBasicDTO(vip);
}
```

**实际风险**：`SalesServiceImpl.validateVipIfPresent()` 中检查了 `vip == null`，逻辑上能工作，但 **`VipClient.getVipById()` 的返回值反序列化行为在不同 Content-Type 下可能不一致**：如果 response body 是空的，RestClient 可能抛 `HttpMessageNotReadableException` 而非返回 `null`。

**推荐修复**：统一风格，不存在时返回 HTTP 404 + 业务异常，调用方 catch 404 来判断：

```java
if (vip == null || Integer.valueOf(1).equals(vip.getDeleted())) {
    throw new BusinessException(ResultCode.NOT_FOUND, "会员不存在！");
}
```

或者如果调用方需要 null 语义，明确约定返回 `ResponseEntity<VipBasicDTO>` 并用 HTTP 204。

**是否建议当前阶段立即修复**：⚠️ 建议跟进，需要确认 `VipClient` 调用 null 响应的反序列化行为。

---

### 4.4 `AppointmentReminderListener` 中角色名硬编码

**文件**：[AppointmentReminderListener.java](jasmine-crm/src/main/java/com/nfu/jasmine/infra/mq/listener/AppointmentReminderListener.java#L92)

```java
List<Integer> receiverUserIds = crmUserClient.getActiveUserIdsByRoles(List.of("admin", "Boss", "clerk"));
```

**实际风险**：角色名散落在消费端代码中，角色重命名时需要全文搜索。不是功能性问题，但不利于维护。

**推荐修复**：提取为常量或配置项。

**是否建议当前阶段立即修复**：✅ 已修复。提取为 `REMINDER_RECEIVER_ROLES` 常量。

---

## 5. Security Analysis

| 检查项 | 结果 | 说明 |
|:---|:---|:---|
| SQL Injection | ✅ 安全 | 全部使用 MyBatis-Plus LambdaQueryWrapper 参数化查询 |
| XSS | ✅ 安全 | 后端 JSON API，不渲染 HTML |
| CSRF | ✅ 安全 | JWT + SPA 架构，已全局 disable CSRF |
| 权限绕过 | ✅ 安全 | `/internal/**` 由 `InternalEndpointGuardFilter` + `X-Gateway-Token` 双重保护 |
| 敏感信息泄露 | ⚠️ 低风险 | `gateway-shared-token` 有 dev 默认值 `jasmine-dev-gateway-shared-token-change-me-2026`，prod 通过 Nacos/环境变量覆盖，可接受 |
| 缓存雪崩/穿透/击穿 | ✅ 安全 | 本轮改动未涉及缓存逻辑 |
| X-Forwarded-For 伪造 | ⚠️ 低风险 | `GatewayRateLimiterConfig` 信任 `X-Forwarded-For` 头，在无前置反向代理时攻击者可伪造 IP 绕过限流。当前项目部署在 Docker Compose 内网，外部流量走 Nginx 时由 Nginx 设置可信 `X-Forwarded-For`，风险可控 |

---

## 6. Performance Analysis

### 远程调用 N+1 问题 — 已正确处理

`SalesServiceImpl.buildSalesVOs()` 和 `InventoryServiceImpl.buildInventoryVOs()` 均使用批量查询（`FlowerClient.getFlowersByIds()` / `VipClient.getVipsByIds()`），**没有 N+1 问题**。

### `saveInventory` 中存在冗余远程调用

```java
// InventoryServiceImpl.saveInventory() L112
FlowerDTO flowerInfo = flowerClient.getFlowerById(inventoryDTO.getFlowerId());  // ← 第 1 次远程调用

ProductStockFacade.StockChangeResult stockChange = productStockFacade.adjustStock(  // ← 第 2 次远程调用
        inventoryDTO.getFlowerId(), ...
);
```

`adjustStock` 的返回值 `StockChangeResult` 已经包含了 `FlowerDTO`，第一次 `getFlowerById` 仅为获取花卉名称用于拼接错误信息。可以先调用 `adjustStock`，从返回值取花卉名称，若抛异常则异常信息中已包含名称。

**影响**：每次库存操作多一次 HTTP 往返（约 5~10ms），对当前业务量可忽略。

### Gateway 限流 Redis 查询开销

每个请求经过 `RequestRateLimiter` 需要 1 次 Redis 操作（Lua 脚本），当前 `replenishRate=50` / `burstCapacity=100` 配置合理，不会成为瓶颈。

---

## 7. Architecture & Design

### ✅ 服务边界完整性

- trade → product：Maven 依赖已移除 ✅
- trade → crm：Maven 依赖已移除 ✅
- crm → iam：Maven 依赖已移除 ✅
- 无跨库 JOIN ✅
- 无共享 Entity（使用 DTO 传输）✅
- 所有内部接口走 `/internal/**` + `X-Gateway-Token` ✅

### ✅ Gateway WebFlux 体系未被污染

- `jasmine-gateway/pom.xml` 仅依赖 `jasmine-common-core`（无 Servlet / MVC / MyBatis 依赖）✅
- `GatewayRateLimiterConfig` 使用 `Mono`（Reactive）✅
- Redis 依赖是 `spring-boot-starter-data-redis-reactive`（不是阻塞版）✅
- 无 Servlet Filter 混入 ✅

### ✅ Nacos 配置合理

- 所有 config.import 使用 `optional:nacos:` 前缀 ✅
- 敏感配置（密码、密钥）通过环境变量注入，不硬编码在公共配置中 ✅
- **唯一问题**：Gateway 缺少 `jasmine-redis-common.yml` 导入（见 P0 2.2）

### ✅ 服务通信合理

- 统一使用 RestClient + `@HttpExchange` ✅
- 配置了超时（3s/5s）✅
- trade 的库存调用有 Resilience4j 断路器 fallback ✅
- crm → iam 调用无断路器（低风险：仅用于预约提醒，非核心路径）

### ✅ 数据库边界

- 无跨服务 JOIN ✅
- 各服务独立数据库 ✅
- Flyway 迁移无变更 ✅

### ✅ Phase 演进兼容性

- 本地 facade 已移除（trade 不再有 product 本地实现），完全远程化 ✅
- 各服务可独立部署 ✅
- 无阻碍 K8s 化的硬编码（使用服务名 `http://product-service` 而非 IP）✅

---

## 8. Refactoring Suggestions

### 8.1 `VipBasicDTO` 字段名标准化

当前 `VipBasicDTO` 的 `name` 字段语义不明确（是昵称还是真实姓名？），建议与 `UserBasicDTO` 对齐使用 `realName`：

```java
@Data
public class VipBasicDTO {
    private Integer id;
    private String vid;
    private String realName;  // ← 统一命名
    private String sex;
    private String phone;
}
```

同步修改 `VipInternalController.toVipBasicDTO()` 中的赋值。

### 8.2 `FlowerDTO` 补充 `safeStock` 和 `currentStock`

参见 3.2，这样 `InventoryEventListener` 可以正确更新预警阈值，且其他消费端也能获取库存全貌。

---

## 9. Testability

### 集成测试现状

| 测试 | 状态 | 说明 |
|:---|:---|:---|
| `BusinessModelWorkflowIT` | ⛔ `@Disabled` | Phase 3 远程化后丧失了跨服务本地测试能力 |
| `OutboxAndAlertIntegrationIT` | ⛔ `@Disabled` | 同上 |

**两个 `@Disabled` 测试是合理的临时决策**（远程化后确实无法在单服务内测试跨服务流程），但需要在后续阶段补充替代测试。

### 建议补充的测试

1. **`@RestClientTest` 契约测试**（trade → product）
   - Mock product-service 的 `/internal/flower/batch` 和 `/internal/flower/stock/adjust`
   - 验证 `FlowerClient` 的请求/响应序列化契约
   - 验证 `RemoteProductStockFacade` 对 422 响应的处理

2. **`@RestClientTest` 契约测试**（trade → crm）
   - 验证 `VipClient` 的请求/响应序列化

3. **`@RestClientTest` 契约测试**（crm → iam）
   - 验证 `CrmUserClient.getActiveUserIdsByRoles()` 参数传递

4. **`GatewayRateLimiterConfig` 单元测试**
   - 验证 `X-Forwarded-For` 多 IP 取首个的逻辑
   - 验证无 `X-Forwarded-For` 时 fallback 到 `RemoteAddress`

5. **trade-service 纯本域集成测试**
   - 使用 `@MockBean` mock `FlowerClient`、`VipClient`
   - 验证销售单创建 → 库存扣减 → MQ 事件发布 → 预警更新的本域链路
