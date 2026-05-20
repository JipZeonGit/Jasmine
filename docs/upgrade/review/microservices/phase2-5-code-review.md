# Code Review Report — Phase2 ~ Phase5

日期：2026-05-20

## 1. Executive Summary

- **总体评价：一般（偏好方向）**
- 核心架构决策正确：Gateway WebFlux 纯净、内部接口隔离、数据库按服务拆分、缓存 key 前缀隔离。
- 存在 3 个需要修复的安全/正确性问题和若干可维护性改进点。

核心问题概述：

1. **P0：Gateway 未剥离客户端伪造的 `X-User-Id` / `X-User-Name` 请求头**，恶意客户端可以直接注入这两个头绕过鉴权。
2. **P1：内部接口（`/internal/**`）在下游服务侧没有任何访问控制**，如果攻击者绕过 Gateway 直连服务端口，可以无鉴权调用内部接口。
3. **P1：`RemoteProductStockFacade` 在断路器 fallback 中吞掉了原始异常信息**，运维排障时无法区分是 product-service 真的不可用还是业务逻辑异常。

## 2. Critical Issues（P0 - 必须修复）

### P0-1 Gateway 未清除客户端伪造的 X-User-Id / X-User-Name 请求头

**问题说明**

`JwtAuthGlobalFilter` 在验签成功后通过 `request.mutate().header(...)` 写入 `X-User-Id` / `X-User-Name`。但如果客户端在原始请求中**已经携带了这两个头**，`header()` 方法是**追加**而非覆盖。更严重的是，白名单路径（如 `/user/login`）直接放行，不会清除这些头 — 如果下游服务在白名单路径上也读了 `X-User-Id`，就会被伪造。

**代码片段**

```java
// JwtAuthGlobalFilter.java
// 白名单放行 — 没有清除可能已存在的 X-User-Id
if (isWhiteListed(path)) {
    return chain.filter(exchange);
}

// 验签成功后追加头 — 没有先移除旧值
ServerHttpRequest mutatedRequest = request.mutate()
        .header(HEADER_USER_ID, String.valueOf(claims.getUserId()))
        .header(HEADER_USER_NAME, claims.getUsername())
        .build();
```

**影响**

- 攻击者可以在请求头中伪造 `X-User-Id: 1`（admin），如果请求命中白名单路径或 Gateway 验签逻辑有任何旁路，下游服务会信任这个伪造值。
- 即使验签成功，如果客户端同时传了 `X-User-Id: 999`，下游服务可能读到多值头。

**修复建议**

在所有路径上（包括白名单），先移除客户端可能传入的 `X-User-Id` / `X-User-Name`，再决定是否写入新值。

```java
@Override
public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
    ServerHttpRequest request = exchange.getRequest();
    String path = request.getURI().getPath();

    // 无论什么路径，先剥离客户端可能伪造的内部头
    ServerHttpRequest.Builder sanitized = request.mutate()
            .headers(h -> {
                h.remove(HEADER_USER_ID);
                h.remove(HEADER_USER_NAME);
            });

    if (path.startsWith("/internal/")) {
        exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
        return exchange.getResponse().setComplete();
    }

    if (isWhiteListed(path)) {
        return chain.filter(exchange.mutate().request(sanitized.build()).build());
    }

    String token = resolveToken(request);
    if (!StringUtils.hasText(token)) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        return exchange.getResponse().setComplete();
    }

    JwtTokenClaims claims;
    try {
        claims = jwtUtil.parseAccessToken(token);
    } catch (Exception e) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        return exchange.getResponse().setComplete();
    }

    ServerHttpRequest mutatedRequest = sanitized
            .header(HEADER_USER_ID, String.valueOf(claims.getUserId()))
            .header(HEADER_USER_NAME, claims.getUsername())
            .build();

    return chain.filter(exchange.mutate().request(mutatedRequest).build());
}
```

**是否建议当前阶段立即修复**：**是**。这是安全漏洞，修复成本极低（加 3 行代码）。

---

## 3. Major Issues（P1 - 高优先级）

### P1-1 内部接口在下游服务侧无访问控制

**问题说明**

`/internal/**` 路径在 Gateway 层被 403 拦截，但如果攻击者绕过 Gateway 直连业务服务端口（9101~9104），可以无鉴权调用 `FlowerInternalController` / `UserInternalController`。

当前 prod compose 中业务服务没有暴露端口（正确），但 dev 环境本机直连、或未来 Kubernetes 中 Pod 间网络默认互通时，这个风险会暴露。

**代码片段**

```java
// FlowerInternalController.java — 没有任何 @PreAuthorize 或 IP 白名单
@RestController
@RequestMapping("/internal/flower")
public class FlowerInternalController { ... }
```

IAM 的 `MySecurityConfig` 中 `.anyRequest().denyAll()` 理论上会拦截未匹配的路径，但 `/internal/user/**` 没有在 `authorizeHttpRequests` 中显式声明 — 它会被 `denyAll()` 拦住。但 product-service / trade-service / crm-service 如果没有 Spring Security 依赖或配置，`/internal/**` 就是裸露的。

**修复建议**

在 `jasmine-common` 中增加一个通用的内部接口保护过滤器，校验请求头中是否携带 Gateway 注入的标识（如 `X-Gateway-Token`），或者在各服务的 Security 配置中显式 `permitAll()` 内部路径但要求来源 IP 为容器网络。

短期止血方案：在各服务 Security 配置中把 `/internal/**` 设为 `authenticated()`，这样至少需要 JWT 或 Gateway 头才能访问。

**是否建议当前阶段立即修复**：**是**（短期止血即可，不需要完整的 mTLS）。

---

### P1-2 RemoteProductStockFacade 断路器 fallback 吞掉原始异常

**问题说明**

```java
return cb.run(
    () -> doAdjustStock(...),
    throwable -> {
        throw new BusinessException("商品服务暂时不可用，请稍后重试");
    }
);
```

`throwable` 参数被完全忽略。如果 product-service 返回的是 4xx 业务异常（如"库存不足"），也会被断路器 fallback 吞掉，用户看到的是"商品服务暂时不可用"而非真实原因。

**修复建议**

区分可恢复异常（网络超时、5xx）和不可恢复异常（4xx 业务错误）。业务异常不应触发 fallback：

```java
return cb.run(
    () -> doAdjustStock(...),
    throwable -> {
        // 如果是业务异常（如 BusinessException），直接抛出，不走 fallback
        if (throwable instanceof BusinessException be) {
            throw be;
        }
        log.error("商品服务调用失败，断路器降级", throwable);
        throw new BusinessException("商品服务暂时不可用，请稍后重试");
    }
);
```

**是否建议当前阶段立即修复**：**是**。影响用户体验和运维排障。

---

### P1-3 prod compose 中 Nacos 仍使用 `jasmine` 库而非独立 `nacos` 库

**问题说明**

```yaml
# ops/prod/docker-compose.yml
MYSQL_SERVICE_DB_NAME: ${MYSQL_DATABASE:-jasmine}
```

prod 环境 Nacos 会把自己的元数据表（config_info、tenant_info 等）建在业务库 `jasmine` 里，与 Phase4 拆分后的独立库设计冲突。dev compose 已经改了（用 derby），但 prod 还指向 `jasmine`。

**修复建议**

prod compose 中 Nacos 的 `MYSQL_SERVICE_DB_NAME` 改为 `nacos`，并在 prod 初始化脚本中建 `nacos` 库。

**是否建议当前阶段立即修复**：**是**。prod 部署前必须修。

---

## 4. Minor Issues（P2 - 建议优化）

### P2-1 `ClientConfig` 中 `CONNECT_TIMEOUT` 常量声明了但未使用

```java
/** 连接超时 3 秒 */
private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(3);
```

`JdkClientHttpRequestFactory` 只设了 `readTimeout`，没有设 `connectTimeout`。`CONNECT_TIMEOUT` 是死代码。

**建议**：要么通过 `HttpClient.newBuilder().connectTimeout(CONNECT_TIMEOUT)` 传入，要么删掉常量。

---

### P2-2 `FlowerInternalController#adjustStock` 捕获 `BusinessException` 后返回 200 + fail body

```java
} catch (BusinessException e) {
    return StockAdjustResult.fail(e.getMessage());
}
```

HTTP 状态码仍是 200，调用方需要检查 body 里的 `success` 字段才能判断失败。这不符合 RESTful 语义，也让 LoadBalancer 的 `retryable-status-codes` 无法识别业务失败。

**建议**：业务失败时返回 4xx（如 422 Unprocessable Entity），让调用方和断路器能正确区分"服务可用但业务拒绝"和"服务不可用"。

---

### P2-3 `GatewayApplication` 使用 `scanBasePackages = "com.nfu.jasmine"` 范围过宽

Gateway 只需要扫描 `com.nfu.jasmine.gateway` 和 `com.nfu.jasmine.common.utils`（JwtUtil 所在包）。当前扫描整个 `com.nfu.jasmine` 会把 classpath 上所有 `@Component` 都扫进来 — 虽然 Gateway 只依赖 `jasmine-common-core`（没有 Servlet 组件），但如果未来 core 里加了不兼容 WebFlux 的 bean，会直接炸。

**建议**：缩小为 `scanBasePackages = {"com.nfu.jasmine.gateway", "com.nfu.jasmine.common.utils"}`。

---

### P2-4 `jasmine-gateway.yml` 中 `app.security.jwt-secret` 与 `application.yml` 重复声明

Gateway 的 `application.yml` 和 Nacos 的 `jasmine-gateway.yml` 都声明了 `app.security.jwt-secret`。Nacos 配置会覆盖本地，但两处维护同一个 key 容易漂移。

**建议**：本地 `application.yml` 只保留 dev 兜底值，Nacos 配置里不重复声明（或反过来）。

---

## 5. Security Analysis

| 风险类型 | 状态 | 说明 |
|:---|:---|:---|
| SQL Injection | **低** | 使用 MyBatis-Plus LambdaQueryWrapper，内部接口参数为 Integer/BigDecimal，无字符串拼接 |
| XSS | **低** | 后端 API 返回 JSON，无 HTML 渲染 |
| CSRF | **无风险** | 全链路 stateless JWT，CSRF 已禁用 |
| 权限绕过 | **中** | P0-1（伪造请求头）+ P1-1（直连内部接口）|
| 敏感信息泄露 | **低** | JWT secret 通过环境变量注入，dev 默认值仅用于本地调试 |
| 缓存雪崩 | **低** | TTL 抖动已实现（`withJitter`）|
| 缓存穿透 | **低** | 当前业务量极小，暂无风险 |
| 缓存击穿 | **低** | 热点 key 有 TTL 保护 |

**修复建议**：优先修 P0-1（请求头清洗），再修 P1-1（内部接口保护）。

---

## 6. Performance Analysis

| 维度 | 评估 |
|:---|:---|
| Gateway 过滤器 | `isWhiteListed` 每次请求遍历 List + AntPathMatcher，当前 8 条规则无性能问题；如果白名单增长到 50+ 条建议改用 PathPatternParser |
| RemoteProductStockFacade | 每次 `adjustStock` 会发起 **2 次** HTTP 调用（adjust + getFlowerById），可以在 adjust 响应中直接返回完整花卉信息，减少一次网络往返 |
| 断路器 | 每次调用 `circuitBreakerFactory.create("productStock")` 会查找或创建实例，Spring Cloud 内部有缓存，无性能问题 |
| 缓存前缀 | `prefixCacheNameWith` 在 key 序列化时追加字符串，开销可忽略 |
| Flyway | 各服务启动时跑迁移，首次启动会有 1~2 秒延迟，后续无影响 |

**优化建议**：`FlowerInternalController#adjustStock` 的响应中直接返回调整后的花卉完整信息，让 `RemoteProductStockFacade` 省掉第二次 `getFlowerById` 调用。

---

## 7. Architecture & Design

| 维度 | 评估 |
|:---|:---|
| SOLID | 基本符合。`ProductStockFacade` 接口隔离了本地/远程实现（ISP + DIP）。`CurrentUserProvider` 的 fallback 逻辑略违反 SRP，但在迁移期可接受。 |
| 模块耦合 | trade → product / crm 的 Maven 依赖仍存在（已知技术债）。`RemoteProductStockFacade` 标记 `@Primary` 是正确的过渡方案。 |
| 扩展性 | 路由表通过 Nacos 动态下发，新增服务只需加一条路由。内部接口统一 `/internal/` 前缀，Gateway 一条规则拦截所有。 |
| WebFlux 纯净性 | Gateway 模块无 Servlet 依赖，`JwtUtil` 是纯计算类（无 blocking IO），符合 WebFlux 要求。 |
| 数据库边界 | 各服务独立库，无跨库 JOIN。`event_outbox` 按服务各一份，Relay 独立。 |

---

## 8. Refactoring Suggestions

### 建议 1：`FlowerInternalController#adjustStock` 响应中返回完整花卉信息

当前 `StockAdjustResult` 只有 `currentStock`，导致 `RemoteProductStockFacade` 需要额外调一次 `getFlowerById`。

改后：

```java
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StockAdjustResult {
    private Boolean success;
    private Integer currentStock;
    private String message;
    private FlowerDTO flower; // 新增：调整后的花卉完整信息

    public static StockAdjustResult ok(Integer currentStock, FlowerDTO flower) {
        StockAdjustResult r = new StockAdjustResult();
        r.setSuccess(true);
        r.setCurrentStock(currentStock);
        r.setFlower(flower);
        return r;
    }
}
```

这样 `RemoteProductStockFacade` 只需一次 HTTP 调用。

### 建议 2：后续移除 trade → product / crm 的 Maven 依赖

当前 `RemoteProductStockFacade` 已经通过 HTTP 调用 product-service，但 `jasmine-trade/pom.xml` 仍依赖 `jasmine-product`。这是因为 `StockChangeResult` 引用了 `Flower` 实体。

建议：把 `StockChangeResult` 改为只依赖 `FlowerDTO`（已在 common-core 中），然后移除 trade → product 的 Maven 依赖。

---

## 9. Testability

| 维度 | 评估 |
|:---|:---|
| Gateway 过滤器 | `GatewayContextSmokeTest` 验证上下文可启动，但没有测试 JWT 验签逻辑和 `/internal/` 拦截。建议补一个 `WebTestClient` 级别的集成测试。 |
| 内部接口 | `FlowerInternalController` / `UserInternalController` 没有单元测试。建议至少补 MockMvc 测试验证参数校验和异常处理。 |
| RemoteProductStockFacade | 没有测试。建议用 `@MockBean FlowerClient` + `@MockBean CircuitBreakerFactory` 验证正常路径和 fallback 路径。 |
| 断路器行为 | 没有测试断路器打开后的降级行为。建议补一个 IT 模拟 product-service 不可用时 trade-service 的响应。 |

**建议补充的测试**（按优先级）：

1. Gateway JWT 过滤器测试：验证伪造头被清除、白名单放行、无 token 返回 401、`/internal/` 返回 403
2. `RemoteProductStockFacade` 单元测试：正常调用 + 断路器降级 + 业务异常透传
3. 内部接口 MockMvc 测试：参数校验、404 处理
