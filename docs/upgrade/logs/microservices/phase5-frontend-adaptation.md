# Phase5：前端适配

日期：2026-05-20

## 目标

前端只与 Gateway 通信，CORS 配置收敛到 Gateway，各下游服务不再自行处理跨域。

## 本轮改动

### 前端（Phase2 已完成，本轮确认）

- `web/vite.config.ts`：proxy target 已改为 `http://localhost:8080`（Gateway 端口）
- `web/default.conf`：Nginx `proxy_pass` 已改为 `http://jasmine-gateway:8080/`
- 前端 axios `baseURL` 仍为 `/prod-api`，由 Vite dev proxy / Nginx 层剥掉前缀后直接到 Gateway
- 后端路由路径不变（`/user/**`、`/flower/**`、`/sales/**` 等），Gateway 按 Path 谓词转发到对应服务

### 下游服务 CORS 清理（本轮新增）

- 删除 `jasmine-iam/src/main/java/com/nfu/jasmine/config/MyCorsConfig.java`
- `MySecurityConfig` 中 `.cors(Customizer.withDefaults())` 改为 `.cors(AbstractHttpConfigurer::disable)`
- 移除 `Customizer` 无用 import
- IAM `application.yml` 移除 `app.cors.allowed-origins` 配置项（已无消费方）

### 设计说明

微服务架构下，浏览器只与 Gateway 通信：
- Gateway 的 `GatewayCorsConfig`（`CorsWebFilter`）统一处理 CORS 预检和响应头
- 下游服务收到的请求来自 Gateway 内网转发，不是浏览器直连，不需要 CORS
- 下游服务显式 `cors(disable)` 避免意外产生重复 CORS 头

## 验证

- 全模块 `compile` 通过（9 模块 BUILD SUCCESS）
- 前端 Vite dev proxy → Gateway 8080 → 路由到各服务，路径不变
- 生产环境 Nginx → Gateway → 各服务，路径不变
