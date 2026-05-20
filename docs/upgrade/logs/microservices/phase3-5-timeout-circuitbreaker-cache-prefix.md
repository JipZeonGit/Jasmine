# Phase3/4 补齐：超时重试、断路器、缓存 key 前缀

日期：2026-05-20

## 背景

Phase3 / Phase4 主体实现时遗留了三项设计决策未落地：
- 3.4 超时与重试策略
- 3.5 熔断保护（Resilience4j）
- 4.6 缓存 key 按服务前缀隔离

本轮一次性补齐。

## 本轮改动

### 3.4 RestClient 超时与重试

**超时配置**（`jasmine-trade/.../client/ClientConfig.java`）：
- 使用 `JdkClientHttpRequestFactory`（Java 21 原生 HttpClient）
- 读取超时：5 秒
- 连接超时：由 JDK HttpClient 默认管理（约 30 秒，实际受 LoadBalancer 健康检查约束）

**重试配置**（`jasmine-trade/src/main/resources/application.yml`）：
```yaml
spring:
  cloud:
    loadbalancer:
      retry:
        enabled: true
        max-retries-on-same-service-instance: 0
        max-retries-on-next-service-instance: 1
        retryable-status-codes: 500,502,503
```

策略说明：
- 同一实例不重试（避免对已知故障实例重复请求）
- 切换到下一个实例重试 1 次（利用 Nacos 多实例负载均衡）
- 仅对 5xx 重试，4xx 不重试（业务错误不应重试）

### 3.5 Resilience4j 断路器

**依赖**（`jasmine-trade/pom.xml`）：
```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-circuitbreaker-resilience4j</artifactId>
</dependency>
```

**使用方式**（`RemoteProductStockFacade`）：
- 注入 `CircuitBreakerFactory`，创建名为 `productStock` 的断路器
- 远程调用包裹在 `cb.run(supplier, fallback)` 中
- fallback 抛出 `BusinessException("商品服务暂时不可用，请稍后重试")`

**断路器参数**（`jasmine-trade/src/main/resources/application.yml`）：
```yaml
resilience4j:
  circuitbreaker:
    configs:
      default:
        sliding-window-size: 10
        failure-rate-threshold: 50
        wait-duration-in-open-state: 10s
        permitted-number-of-calls-in-half-open-state: 3
  timelimiter:
    configs:
      default:
        timeout-duration: 5s
```

参数说明：
- 滑动窗口 10 次调用，失败率超过 50% 时断路器打开
- 打开状态持续 10 秒后进入半开，允许 3 次试探
- 超时限制 5 秒（与 RestClient 读取超时对齐）

### 4.6 缓存 key 按服务前缀隔离

**改动位置**（`jasmine-common/.../config/MyRedisConfig.java`）：
- 注入 `${spring.application.name:jasmine}` 作为前缀
- `RedisCacheConfiguration` 加 `.prefixCacheNameWith(appName + ":")`

**效果**：
- product-service 的 `@Cacheable("flowerList")` → Redis key 前缀 `product-service:flowerList::`
- iam-service 的 `@Cacheable("user")` → Redis key 前缀 `iam-service:user::`
- 多服务共享同一 Redis 实例时不会互相覆盖

**无需修改业务代码**：所有 `@Cacheable` / `@CacheEvict` 注解的 `cacheNames` 保持不变，前缀由基础设施层自动追加。

## 验证

- 全模块 `compile` 通过（9 模块 BUILD SUCCESS）
- Resilience4j 断路器通过 Spring Cloud CircuitBreaker 抽象层接入，不侵入业务代码
- 缓存前缀通过 `RedisCacheConfiguration.prefixCacheNameWith()` 实现，对业务透明
