# Phase2：Gateway 路由接入与网关 JWT 鉴权

日期：2026-05-20

## 目标

统一入口，JWT 网关鉴权，路由转发。前端只与 Gateway 通信，各下游服务信任网关透传的用户信息。

## 本轮改动

### Gateway 侧

- 新增 `JwtAuthGlobalFilter`（WebFlux `GlobalFilter`）：
  - 白名单路径（login / refresh / actuator / swagger）直接放行
  - `/internal/**` 路径直接返回 403，禁止外部访问内部接口
  - 其他路径校验 Bearer Token 签名与有效期
  - 验签通过后将 `X-User-Id` / `X-User-Name` 写入请求头传递给下游
- 新增 `GatewayCorsConfig`（`CorsWebFilter`）：
  - 统一 CORS 配置，各下游服务不再需要自己的 `MyCorsConfig`
  - 允许的 origin 通过 `app.cors.allowed-origins` 配置
- `jasmine-gateway.yml`（Nacos 配置）新增路由表：
  - `iam-service`：`/user/**`, `/role/**`, `/menu/**`, `/sys/**`
  - `product-service`：`/flower/**`
  - `trade-service`：`/sales/**`, `/inventory/**`, `/inventory-alert/**`
  - `crm-service`：`/vip/**`, `/appointment/**`, `/site-message/**`
- `application.yml` 补 `app.security.jwt-secret` 与 `app.cors.allowed-origins` 本地兜底

### 下游服务侧

- `CurrentUserProvider` 改为优先从 Gateway 透传的请求头读取用户信息：
  - 优先 `request.getAttribute("loginUser")`（IAM 自身 JWT filter 设置）
  - 回退 `X-User-Id` / `X-User-Name` 请求头（Gateway 透传）
  - 新增内部 `GatewayUserInfo` record 实现 `LoginUserInfo` 接口
- IAM 服务的 `JwtAuthenticationFilter` 和 `MySecurityConfig` 保持不变（IAM 仍自行解 JWT，因为它是签发方）

### 前端侧

- `web/vite.config.ts`：proxy target 从 `http://localhost:9999` 改为 `http://localhost:8080`（Gateway 端口）
- `web/default.conf`：Nginx `proxy_pass` 从 `http://backend:9999/` 改为 `http://jasmine-gateway:8080/`

## 设计决策

- **网关只做签名验证 + claims 提取，不查数据库、不做 RBAC**。RBAC 由各下游服务自行根据 `X-User-Id` / `X-User-Roles` 判断。
- **IAM 服务保留自己的 JWT filter**。因为 IAM 是 JWT 签发方，它需要完整的 `User` 实体（含角色列表）来做 RBAC，不能只靠请求头。
- **路由表通过 Nacos 配置中心下发**，支持动态刷新，不需要重启 Gateway。
- **`/internal/**` 在 Gateway 层直接 403**，作为 Phase3 内部接口的防护屏障。

## 验证

- 全模块 `compile` 通过（9 模块 BUILD SUCCESS）
- Gateway 路由表通过 Nacos 配置中心下发，支持动态刷新
