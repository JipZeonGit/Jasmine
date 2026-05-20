# Phase3：服务间通信（RestClient + 内部接口 + 远程 Facade）

日期：2026-05-20

## 目标

跨域调用从本地方法调用改为远程 HTTP 调用，使用 Spring 6+ `RestClient` + `@HttpExchange` 声明式客户端，通过 Spring Cloud LoadBalancer 按服务名解析。

## 本轮改动

### 内部接口（仅供服务间调用）

**product-service**：
- `FlowerInternalController`（`/internal/flower`）
  - `GET /{id}`：返回 `FlowerDTO`（id, name, price, cost, status）
  - `POST /stock/adjust`：接收 `StockAdjustRequest`，调用 `ProductStockFacade#adjustStock()`，返回 `StockAdjustResult`

**iam-service**：
- `UserInternalController`（`/internal/user`）
  - `GET /{id}`：返回 `UserBasicDTO`（id, username, realName）

### 内部通信 DTO（jasmine-common-core）

新增 `com.nfu.jasmine.common.dto.internal` 包：
- `FlowerDTO`：花卉基本信息
- `StockAdjustRequest`：库存调整请求（flowerId, quantity, costPrice, updateCostPrice, operatorId, reason）
- `StockAdjustResult`：库存调整结果（success, currentStock, message），含静态工厂方法
- `UserBasicDTO`：用户基本信息

### RestClient 声明式客户端（trade-service）

- `FlowerClient`（`@HttpExchange("/internal/flower")`）：调用 product-service
- `UserClient`（`@HttpExchange("/internal/user")`）：调用 iam-service
- `ClientConfig`：注册 `@LoadBalanced RestClient.Builder`，按服务名 `http://product-service` / `http://iam-service` 解析

### 远程 Facade 实现（trade-service）

- `RemoteProductStockFacade`（`@Primary @Component`）：
  - 实现 `ProductStockFacade` 接口
  - 委托 `FlowerClient#adjustStock()` 完成远程库存调整
  - 调整后再调 `FlowerClient#getFlowerById()` 获取完整花卉信息构造 `StockChangeResult`
  - 标记 `@Primary` 使其在 trade-service 上下文中优先于 product-service 模块内的本地实现

### Gateway 防护

- `JwtAuthGlobalFilter` 增加 `/internal/**` 路径拦截：外部请求到达 `/internal/**` 时直接返回 403

## 设计决策

- **使用 Spring 6 `RestClient` + `@HttpExchange`，不使用 OpenFeign**。OpenFeign 在 Spring Cloud 2025.x 中已进入维护模式。
- **内部接口统一 `/internal/` 前缀**，Gateway 不对外暴露，形成网络边界。
- **`RemoteProductStockFacade` 标记 `@Primary`**，在 trade-service 中覆盖 product-service 模块内的本地 `FlowerStockService` 实现。后续移除 trade → product 的 Maven 依赖后可去掉 `@Primary`。
- **超时与重试暂用 Spring Cloud 默认值**，后续按实际联调情况调整。

## 跨域调用清单

| 调用方 | 被调方 | 接口 | 说明 |
|:---|:---|:---|:---|
| trade-service | product-service | `POST /internal/flower/stock/adjust` | 销售时扣减库存 |
| trade-service | product-service | `GET /internal/flower/{id}` | 查询花卉售价/成本 |
| trade-service | iam-service | `GET /internal/user/{id}` | 查询操作员信息（预留） |
| crm-service | crm-service（本地） | `VipReadFacade` | 会员查询仍在同一服务内，无需远程 |

## 验证

- 全模块 `compile` 通过（9 模块 BUILD SUCCESS）
- 内部接口通过 Gateway 路径拦截保护，不对外暴露
- 远程调用通过 Spring Cloud LoadBalancer 按服务名解析
