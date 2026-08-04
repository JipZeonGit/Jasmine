# Phase 7.4 — 跨服务契约测试

> **日期**：2026-08-04
> **分支**：microservices
> **状态**：✅ 已完成
> **预估**：2~3 天 → 实际约 0.5 天

## 背景

Phase 3 远程化后，两个跨服务 IT（`BusinessModelWorkflowIT`、`OutboxAndAlertIntegrationIT`）被 `@Disabled`，跨服务 HTTP 调用的契约保障完全缺失。provider 改 `/internal/**` 接口时 consumer 无自动感知，接口漂移只能等生产 404 才发现。

本阶段补上轻量契约测试，让 provider 改接口时 CI 立即报红。

## 改动

### 新增文件（4 个契约测试，共 13 个用例）

| 文件 | 所在模块 | 覆盖契约 | 用例数 |
|:---|:---|:---|:---|
| `FlowerClientContractTest` | jasmine-trade | trade → product 的 4 个内部接口 | 5 |
| `VipClientContractTest` | jasmine-trade | trade → crm 的 3 个内部接口 | 4 |
| `UserClientContractTest` | jasmine-trade | trade → iam 的 1 个内部接口 | 2 |
| `CrmUserClientContractTest` | jasmine-crm | crm → iam 的 1 个内部接口 | 2 |

### 技术方案

手搓 `RestClient.Builder` 绑定到 `MockRestServiceServer`，再用 `HttpServiceProxyFactory` 创建客户端代理——与生产 `InternalClientFactory` 的创建方式一致（都是 `HttpServiceProxyFactory + RestClientAdapter`），保证契约行为相同。

不走 `@RestClientTest` 注解的原因：客户端经 `@LoadBalanced RestClient.Builder` 创建，直接 `@RestClientTest` 无法替换 LoadBalancer 逻辑。契约测试只验"请求路径/方法/序列化/响应解析"，不需要 LoadBalancer 真实路由——后者由已有的 `RemoteProductStockFacadeTest` 覆盖。

```java
RestClient.Builder builder = RestClient.builder();
MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
FlowerClient client = HttpServiceProxyFactory.builderFor(
        RestClientAdapter.create(builder.baseUrl("http://product-service").build()))
        .build().createClient(FlowerClient.class);
server.expect(requestTo("http://product-service/internal/flower/1")).andRespond(...);
```

无需加 pom 依赖——`MockRestServiceServer` 由 `spring-boot-starter-test` 传递提供。

### 每个契约测试文件顶部注释

标注对应的 Provider 控制器与端点，形成显式耦合点：Provider 改接口时必须同步改契约测试，否则 CI 报红即代表契约漂移。

## 发现的契约漂移

写测试过程中发现 `adjustStock` 的 422 契约存在漂移：

- **provider 端设计意图**（`FlowerInternalController`）：业务失败返回 HTTP 422 + `StockAdjustResult(success=false, message=...)` body，让 consumer 读 body 决定处理方式。
- **consumer 端实际行为**（`FlowerClient.adjustStock()`）：注释写"422 不会抛异常，由调用方统一处理"，但 `RestClient` 默认对 4xx 抛 `HttpClientErrorException$UnprocessableEntity`，**根本不会进入 ResponseEntity**。
- **`RemoteProductStockFacade` 的断路器降级**：`throwable instanceof BusinessException` 匹配不到 `HttpClientErrorException`，走降级分支抛 `BusinessException("商品服务暂时不可用，请稍后重试")`——**库存不足被误判为服务故障**。

### Bug 修复（同日完成）

契约测试记录漂移后，**同日修复了该 Bug**。修复策略：在 Facade 层捕获 `HttpClientErrorException`，422 时解析响应体提取业务消息，转回 `BusinessException`。其他 4xx 仍走断路器降级。

**修复后行为**：
- 库存不足：`422 → BusinessException("红玫瑰库存不足")` → 断路器 fallback 透传 → 前端显示"红玫瑰库存不足" ✅
- 服务故障（404/500 等）：仍走断路器降级 → 前端显示"商品服务暂时不可用，请稍后重试" ✅

**修复涉及的细节坑**：
1. `e.getStatusCode() == HttpStatus.UNPROCESSABLE_ENTITY` 不可靠（Spring 6.2 `HttpClientErrorException.create()` 工厂方法可能用 `DefaultResponseClientStatus` 包装 statusCode，引用比较失败），改用 `.value() ==` 比较数值。
2. 手搓 `HttpClientErrorException.create()` 不会自动注入 `bodyConvertFunction`（生产环境由 RestClient 错误处理器注入），导致 `getResponseBodyAs()` 在测试中抛 `IllegalStateException: Function to convert body not set`。测试中需要手动调用 `setBodyConvertFunction()` 注入 Jackson 解析逻辑。
3. 主代码加 `catch (RuntimeException parseEx)` 兜底：body 不可解析时使用兜底文案"库存调整失败！"，不暴露技术异常给前端。

**契约测试保持不变**：`FlowerClientContractTest.adjustStockShouldThrowOn422BusinessFailure` 仍锁定底层"422 抛 HttpClientErrorException"行为，因为修复在 Facade 层，没改 FlowerClient 本身的行为。

**Facade 层单测更新**：原来的 `adjustStockShouldThrowWhen422Response` 测试 mock 成 `ResponseEntity.status(422)` 直接返回，掩盖了真实行为（RestClient 抛异常）。改为用真实 `HttpClientErrorException` 验证，并新增 4 个测试覆盖 422 透传、422 不触发降级、422 body 不可解析、其他 4xx 触发降级场景。

## 验证

- `./mvnw -pl jasmine-trade,jasmine-crm -am test -Dtest='*ContractTest'`：13 个用例全过
- 全量单测无回归：trade 32 + common 19 + crm 9 = 60 个全过
- **反向验证**：故意把 client 的 baseUrl 改为 `product-service-DRIFT`，`getFlowerByIdShouldSerializePathAndParseDto` 立即失败，报错信息清晰：
  ```
  Request URI expected:<http://product-service/internal/flower/1> but was:<http://product-service-DRIFT/internal/flower/1>
  ```

## 涉及文件

- `jasmine-trade/src/test/java/com/nfu/jasmine/infra/client/FlowerClientContractTest.java`（新增）
- `jasmine-trade/src/test/java/com/nfu/jasmine/infra/client/VipClientContractTest.java`（新增）
- `jasmine-trade/src/test/java/com/nfu/jasmine/infra/client/UserClientContractTest.java`（新增）
- `jasmine-crm/src/test/java/com/nfu/jasmine/infra/client/CrmUserClientContractTest.java`（新增）
- `docs/upgrade/roadmap/microservices-future-roadmap.md`（标记 7.4 完成）

### Bug 修复同日追加的文件

- `jasmine-trade/src/main/java/com/nfu/jasmine/infra/client/RemoteProductStockFacade.java`（doAdjustStock 捕获 HttpClientErrorException，422 转 BusinessException）
- `jasmine-trade/src/main/java/com/nfu/jasmine/infra/client/FlowerClient.java`（修正类注释：422 实际抛异常，由 Facade 统一处理）
- `jasmine-trade/src/test/java/com/nfu/jasmine/infra/client/RemoteProductStockFacadeTest.java`（修正错误的 422 测试 + 新增 4 个回归测试）

## 后续注意

1. **provider 改 `/internal/**` 接口时**，必须同步改对应契约测试——这是显式耦合点
2. 契约测试只验序列化/路径，不验 LoadBalancer/熔断/超时——后者由 `RemoteProductStockFacadeTest` 覆盖
3. **422 契约漂移已修复**：Facade 层捕获 `HttpClientErrorException`，422 时解析 body 转回 `BusinessException`，避免业务失败被误判为服务故障
