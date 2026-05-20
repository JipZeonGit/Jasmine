# Phase2~5 代码审查修复

日期：2026-05-20

## 背景

依据 `docs/upgrade/review/microservices/phase2-5-code-review.md` 的审查结论，本轮把 1 个 P0、3 个 P1 全部修掉，并连带处理 4 个 P2 中的可立即落地项。

## 修复清单

### P0-1：Gateway 清除客户端伪造的内部头

`JwtAuthGlobalFilter` 在过滤器入口 **无条件** 剥离客户端可能携带的 `X-User-Id` / `X-User-Name` / `X-Gateway-Token`，再决定是否补回。包括白名单路径也走这条剥离逻辑。

```java
ServerHttpRequest.Builder sanitized = request.mutate()
        .headers(h -> {
            h.remove(HEADER_USER_ID);
            h.remove(HEADER_USER_NAME);
            h.remove(HEADER_GATEWAY_TOKEN);
        });
```

之后白名单路径仅写入 `X-Gateway-Token`（让下游 InternalEndpointGuardFilter 放行内部接口的 RestClient 调用），鉴权路径才写 `X-User-Id` / `X-User-Name`。

### P1-1：内部接口加纵深防御

新增 `jasmine-common/.../infra/security/filter/InternalEndpointGuardFilter`，所有 `/internal/**` 请求必须携带匹配的 `X-Gateway-Token` 才能放行。

- 网关入口在 Phase2 已经会拦截外部对 `/internal/**` 的访问，本过滤器是绕过网关时的第二道防线
- IAM 服务的 `MySecurityConfig` 把 `/internal/**` 加到 `permitAll()`，让本过滤器接管授权
- product / trade / crm 没有 Spring Security，本过滤器直接通过 `OncePerRequestFilter` 生效
- trade-service 的 RestClient 在 `ClientConfig` 中通过 `defaultHeader("X-Gateway-Token", token)` 自动给所有出站请求附带令牌

共享密钥统一从 Nacos `jasmine-common.yml` 的 `app.security.gateway-shared-token` 下发，prod 必须通过 `GATEWAY_SHARED_TOKEN` 环境变量覆盖。

### P1-2：断路器区分业务异常与服务不可用

`RemoteProductStockFacade` 在 fallback 中先判断异常类型：

```java
throwable -> {
    if (throwable instanceof BusinessException be) {
        throw be;  // 业务异常透传
    }
    log.error("商品服务调用失败，断路器降级 ...", throwable);
    throw new BusinessException("商品服务暂时不可用，请稍后重试");
}
```

库存不足、参数非法等业务错误不再被吞，运维只在真正的网络/服务不可用时看到降级日志。

### P1-3：prod Nacos 用独立 nacos 库

- `ops/prod/docker-compose.yml` 中 Nacos 的 `MYSQL_SERVICE_DB_NAME` 由 `${MYSQL_DATABASE:-jasmine}` 改为 `nacos`
- 新增 `MYSQL_SERVICE_DB_PARAM`，包含 `allowPublicKeyRetrieval=true` 以兼容 MySQL 8.4 默认 `caching_sha2_password`
- 新增 `ops/prod/init-databases.sql`，创建 5 个库（4 个业务库 + nacos）并授权
- MySQL 容器挂载 init 脚本到 `/docker-entrypoint-initdb.d/`
- `ops/dev/init-databases.sql` 同步加上 `nacos` 库与授权

### P2-2：内部接口业务失败返回 422

`FlowerInternalController#adjustStock` 在捕获 `BusinessException` 时返回 `422 Unprocessable Entity` + `StockAdjustResult.fail()`。配合 `FlowerClient` 改为 `ResponseEntity<StockAdjustResult>` 接收，让 LoadBalancer 的 `retryable-status-codes`（500/502/503）和断路器都不会把业务失败当作"服务不可用"重试或熔断。

### P2-3：缩小 Gateway scanBasePackages

```java
@SpringBootApplication(scanBasePackages = {
    "com.nfu.jasmine.gateway",
    "com.nfu.jasmine.common.utils"  // 仅扫 JwtUtil
})
```

避免未来 `jasmine-common-core` 中无关组件被 Gateway 扫到导致启动失败。

### P2-4：jwt-secret / shared-token 单源声明

- 本地 `jasmine-gateway/application.yml` 移除 `app.security` 段
- 新增 `application-dev.yml` 提供本地兜底（仅 dev profile 生效）
- Nacos `jasmine-gateway.yml` 仍声明 `jwt-secret` 和 `gateway-shared-token`，作为运行时唯一来源
- 配置漂移风险消除

### Refactor 1：内部接口响应携带完整花卉信息

`StockAdjustResult` 新增 `beforeStock` + `flower` 字段。trade-service 远程调用 `adjustStock` 一次即可拿到全部信息，省掉原来调用方的第二次 `getFlowerById` 网络往返。

### 代码层面顺手清理

- `ClientConfig` 中 `CONNECT_TIMEOUT` 之前是死代码，现在通过自定义 `HttpClient.newBuilder().connectTimeout()` 落地
- `ClientConfig` 提取 `createClient` 私有方法，复用 `RestClient` 构建逻辑
- 所有出站请求自动带 `X-Gateway-Token` 默认头

## 验证

- `mvnw -B -DskipTests compile`：9 模块 BUILD SUCCESS
- `mvnw -B test -DskipITs=true`：13 个单测全部通过（含 GatewayContextSmokeTest）
- 现有 Testcontainers IT 不受影响

## 留作后续

- **Refactor 2**（移除 trade → product / crm Maven 依赖）：需要把 `ProductStockFacade.StockChangeResult` 中的 `Flower` 实体替换为 `FlowerDTO`，影响面较大（trade 内部业务代码也用 Flower），归入 Phase 远程化收尾阶段
- **Testability** 章节列的 4 项补测建议留在专门的测试补强 PR 中处理，不混在本轮安全/正确性修复里
