# Jasmine 微服务架构与详细接口说明书

> 本文档基于 `microservices` 分支 Phase 0~5 全部完成后的代码基线编写，用于全面记录当前微服务架构的形态、服务拆分、接口清单、数据模型、事件驱动机制与基础设施配置。

---

## 一、项目概览

| 属性 | 值 |
|:---|:---|
| 项目名称 | Jasmine（花店门店管理系统） |
| 基础包名 | `com.nfu.jasmine` |
| Java 版本 | 21 |
| Spring Boot | 3.5.13 |
| Spring Cloud | 2025.0.0 |
| Spring Cloud Alibaba | 2025.0.0.0 |
| 持久层 | MyBatis-Plus 3.5.14 |
| 安全框架 | Spring Security + JWT (0.12.7) + Spring Cloud Gateway |
| 数据库 | MySQL 8.4（每服务独立库） |
| 缓存 | Redis 7.2（生产）/ Caffeine 内存（开发） |
| 消息队列 | RabbitMQ 4.2-management |
| 注册与配置中心 | Nacos 2.4.x |
| API 文档 | springdoc-openapi 2.8.16 |
| 指标监控 | Micrometer + Prometheus |
| 服务间调用 | Spring 6 RestClient + @HttpExchange |
| 熔断器 | Resilience4j |
| 前端 | Vue 3.5 + Element Plus 2.13 + Vite 8.0 |
| 网关端口 | 8080 |
| 前端端口 | 5173（开发）/ 80（生产） |

---

## 二、架构形态

当前 Jasmine 为 **Spring Cloud Alibaba 微服务架构**，采用 **方案 A+（4 业务服务 + 1 网关）** 拆分策略，每个服务拥有独立数据库，通过 Nacos 实现服务发现与配置中心化，通过 Spring Cloud Gateway 统一入口。

### 2.1 整体架构图

```
┌──────────────────────────────────────────────────────────────┐
│                        前端 (web/)                           │
│   Vue 3 + Element Plus + Pinia + Vue Router + Axios          │
└────────────────────────┬─────────────────────────────────────┘
                         │ HTTP / Bearer JWT
┌────────────────────────▼─────────────────────────────────────┐
│              Spring Cloud Gateway (:8080)                     │
│         统一入口 / JWT 鉴权 / 路由转发 / CORS                  │
└──────┬──────────┬──────────┬──────────┬─────────────────────┘
       │          │          │          │
┌──────▼──┐ ┌────▼────┐ ┌───▼────┐ ┌──▼──────────┐
│  iam-   │ │product- │ │trade-  │ │  crm-       │
│ service │ │service  │ │service │ │  service    │
│  :9101  │ │  :9102  │ │ :9103  │ │   :9104     │
│认证/用户│ │花卉主数据│ │销售/库存│ │会员/预约/通知│
└────┬────┘ └────┬────┘ └───┬────┘ └──────┬─────┘
     │           │          │             │
     └───────────┴──────────┴─────────────┘
                         │
          ┌──────────────┼──────────────┐
          │              │              │
    ┌─────▼─────┐ ┌─────▼────┐ ┌──────▼──────┐
    │ MySQL 8.4 │ │Redis 7.2 │ │RabbitMQ 4.2 │
    │ 4 个独立库 │ │  缓存/幂等│ │ 事件驱动/MQ │
    └───────────┘ └──────────┘ └─────────────┘
                         │
                   ┌─────▼─────┐
                   │Nacos 2.4.x│
                   │注册中心+配置│
                   └───────────┘
```

### 2.2 Maven 多模块结构

```
jasmine (父 POM, packaging=pom)
├── jasmine-common-core     // 纯核心库（Result/异常/DTO/JWT 工具）
├── jasmine-common          // Servlet 侧基础设施（安全/MQ/Outbox/缓存/幂等）
├── jasmine-schema          // 独立 Flyway 迁移引导模块
├── jasmine-gateway         // Spring Cloud Gateway（WebFlux, :8080）
├── jasmine-iam             // IAM 服务（:9101）
├── jasmine-product         // 花卉服务（:9102）
├── jasmine-trade           // 交易服务（:9103）
└── jasmine-crm             // CRM 服务（:9104）
```

### 2.3 模块依赖关系

```
jasmine-common-core  （无内部依赖）
       ▲
       │
jasmine-common  （依赖 common-core）
       ▲
       ├─── jasmine-iam     （依赖 common）
       ├─── jasmine-product （依赖 common）
       ├─── jasmine-trade   （依赖 common + product + crm）
       └─── jasmine-crm     （依赖 common + iam）

jasmine-gateway  （仅依赖 common-core，避免 MVC/DB/MQ 污染）

jasmine-schema   （独立，仅 Flyway + MySQL）
```

### 2.4 服务职责划分

| 服务名 | 对应域 | 核心职责 | 端口 | 数据库 |
|:---|:---|:---|:---|:---|
| `jasmine-gateway` | — | 统一入口、JWT 鉴权、路由转发、CORS | 8080 | 无 |
| `iam-service` | iam | 登录认证、用户管理、角色管理、菜单管理、JWT 签发/刷新/吊销、RBAC 授权 | 9101 | `jasmine_iam` |
| `product-service` | flower | 花卉主数据 CRUD、花卉状态管理、库存 CAS 原子扣减 | 9102 | `jasmine_product` |
| `trade-service` | sales + inventory | 销售单管理、库存流水管理、库存预警读模型、今日经营统计 | 9103 | `jasmine_trade` |
| `crm-service` | vip + appointment + notification | 会员管理、预约管理（含延时提醒）、站内通知消息中心 | 9104 | `jasmine_crm` |

### 2.5 业务域内部分层约定

每个业务域统一遵循以下分层：

| 层 | 包路径 | 职责 |
|:---|:---|:---|
| web | `{domain}.web` | Controller + DTO + VO，负责请求接收和响应返回 |
| application | `{domain}.application` | Service 接口与实现，承载业务逻辑 |
| application.support | `{domain}.application.support` | Facade 接口，用于跨域数据访问抽象 |
| model | `{domain}.model` | Entity + 枚举，对应数据库表 |
| persistence | `{domain}.persistence` | Mapper 接口 + XML，数据库访问 |
| infra.client | `infra.client` | 服务间 HTTP 调用客户端（@HttpExchange） |
| infra.mq.listener | `infra.mq.listener` | MQ 消费者监听器 |

---

## 三、网关层 — Spring Cloud Gateway

### 3.1 路由配置

路由规则定义在 Nacos 配置 `jasmine-gateway.yml` 中，支持运行时刷新：

| 路由 ID | URI | 路径谓词 | 目标服务 |
|:---|:---|:---|:---|
| `iam-service` | `lb://iam-service` | `/user/**`, `/role/**`, `/menu/**`, `/sys/**` | IAM 服务 |
| `product-service` | `lb://product-service` | `/flower/**` | 花卉服务 |
| `trade-service` | `lb://trade-service` | `/sales/**`, `/inventory/**`, `/inventory-alert/**` | 交易服务 |
| `crm-service` | `lb://crm-service` | `/vip/**`, `/appointment/**`, `/site-message/**` | CRM 服务 |

### 3.2 JWT 鉴权全局过滤器

`JwtAuthGlobalFilter`（`order = -100`）执行以下处理流程：

1. **请求头清洗**：无条件移除客户端伪造的 `X-User-Id`、`X-User-Name`、`X-Gateway-Token`
2. **内部路径拦截**：`/internal/**` 直接返回 403 Forbidden
3. **白名单放行**：`/user/login`、`/user/refresh`、`/actuator/**`、`/swagger-ui/**`、`/v3/api-docs/**` 仅注入 `X-Gateway-Token`
4. **JWT 验证**：从 `Authorization: Bearer <token>` 提取令牌，调用 `JwtUtil.parseAccessToken()` 验证签名、过期、类型
5. **请求头注入**：验证通过后注入 `X-User-Id`（JWT `uid`）、`X-User-Name`（JWT `sub`）、`X-Gateway-Token`（共享密钥）

**请求头传递矩阵**：

| 场景 | X-User-Id | X-User-Name | X-Gateway-Token |
|:---|:---|:---|:---|
| `/internal/**` | N/A（403） | N/A | N/A |
| 白名单路径 | 不设置 | 不设置 | 设置 |
| 已认证路径 | 设置（来自 JWT） | 设置（来自 JWT） | 设置 |
| 受保护路径无令牌 | N/A（401） | N/A | N/A |

### 3.3 CORS 配置

`GatewayCorsConfig` 使用响应式 `CorsWebFilter` 统一处理跨域：

- 允许来源：`http://localhost:8888`、`http://localhost:5173`、`http://127.0.0.1:5173`、`http://localhost`
- 允许凭证：是
- 允许方法：全部
- 预检缓存：3600 秒

### 3.4 下游服务安全防线

- **`InternalEndpointGuardFilter`**（jasmine-common 模块）：所有业务端点必须携带有效的 `X-Gateway-Token`，否则返回 403。仅 `/actuator/**`、`/swagger-ui/**`、`/v3/api-docs/**`、`/error` 豁免
- **`CurrentUserProvider`**（jasmine-common 模块）：从网关传递的 `X-User-Id`/`X-User-Name` 请求头中提取当前用户信息

---

## 四、业务服务详细接口清单

### 4.1 IAM 服务 — 身份与访问管理

服务名：`iam-service`，端口：9101，数据库：`jasmine_iam`

#### UserController (`/user`)

| HTTP 方法 | 路径 | 说明 | 认证要求 |
|:---|:---|:---|:---|
| GET | `/user/all` | 获取全部用户 | admin |
| POST | `/user/login` | 用户登录（签发 JWT + Refresh Token Cookie） | 公开 |
| POST | `/user/refresh` | 刷新登录状态（一次性令牌轮换） | 公开（Cookie） |
| GET | `/user/info` | 获取当前用户信息 + 角色 + 菜单树 | 已登录 |
| POST | `/user/logout` | 注销登录（吊销所有 Refresh Token） | 已登录 |
| GET | `/user/list` | 分页查询用户 | admin |
| POST | `/user` | 新增用户 | admin |
| PUT | `/user` | 修改用户 | admin |
| GET | `/user/{id}` | 根据 ID 查询用户 | admin |
| DELETE | `/user/{id}` | 逻辑删除用户 | admin |
| PUT | `/user/changePassword` | 修改密码 | 已登录 |

**关键 DTO/VO**：
- `LoginDTO`：username, password
- `RefreshTokenDTO`：refreshToken（可选，优先读 Cookie）
- `UserCreateDTO`：username, password, email, phone, status, roleIdList
- `UserUpdateDTO`：id, username, email, phone, status, roleIdList
- `UserQueryDTO`：username, phone, pageNo, pageSize
- `ChangePasswordDTO`：oldPassword, newPassword
- `LoginVO`：token, refreshToken
- `UserInfoVO`：name, avatar, phone, email, status, roles, menuList
- `UserVO`：id, username, email, phone, status, avatar, roleIdList

#### RoleController (`/role`)

| HTTP 方法 | 路径 | 说明 | 认证要求 |
|:---|:---|:---|:---|
| GET | `/role/list` | 分页查询角色 | admin |
| POST | `/role` | 新增角色 | admin |
| PUT | `/role` | 修改角色 | admin |
| GET | `/role/{id}` | 根据 ID 查询角色 | admin |
| DELETE | `/role/{id}` | 逻辑删除角色 | admin |
| GET | `/role/all` | 获取所有角色 | admin |

**关键 DTO/VO**：
- `RoleSaveDTO`：roleId, roleName, roleDesc, menuIdList
- `RoleQueryDTO`：roleName, pageNo, pageSize
- `RoleVO`：roleId, roleName, roleDesc, menuIdList

#### MenuController (`/menu`)

| HTTP 方法 | 路径 | 说明 | 认证要求 |
|:---|:---|:---|:---|
| GET | `/menu` | 获取全部权限菜单树 | admin |

**关键 VO**：
- `MenuVO`：menuId, component, path, redirect, name, title, icon, parentId, isLeaf, hidden, children, meta

#### UserInternalController (`/internal/user`)

| HTTP 方法 | 路径 | 说明 | 认证要求 |
|:---|:---|:---|:---|
| GET | `/internal/user/{id}` | 根据 ID 查询用户基本信息 | 网关令牌（服务间调用） |

**响应**：`UserBasicDTO`（id, username, realName），无 `Result` 包装

---

### 4.2 花卉服务 — 花卉主数据

服务名：`product-service`，端口：9102，数据库：`jasmine_product`

#### FlowerController (`/flower`)

| HTTP 方法 | 路径 | 说明 | 认证要求 |
|:---|:---|:---|:---|
| GET | `/flower/all` | 获取全部花卉 | admin/Boss/clerk |
| POST | `/flower` | 新增花卉（初始库存为 0） | admin/Boss/clerk |
| PUT | `/flower` | 修改花卉（保留现有库存） | admin/Boss/clerk |
| GET | `/flower/{id}` | 根据 ID 查询花卉 | admin/Boss/clerk |
| DELETE | `/flower/{id}` | 逻辑删除花卉（库存 > 0 时拒绝） | admin/Boss/clerk |
| GET | `/flower/list` | 分页查询花卉 | admin/Boss/clerk |

**关键 DTO/VO**：
- `FlowerSaveDTO`：id, name, unit, salePrice, costPrice, safeStock, status
- `FlowerQueryDTO`：name, pageNo, pageSize
- `FlowerVO`：id, name, unit, salePrice, costPrice, safeStock, currentStock, status

**核心内部服务**：
- `FlowerStockService#adjustStock()`：基于 CAS 的库存原子变更（compare-and-set SQL），最多重试 8 次

#### FlowerInternalController (`/internal/flower`)

| HTTP 方法 | 路径 | 说明 | 认证要求 |
|:---|:---|:---|:---|
| GET | `/internal/flower/{id}` | 查询花卉基本信息 | 网关令牌（服务间调用） |
| POST | `/internal/flower/stock/adjust` | CAS 原子库存变更 | 网关令牌（服务间调用） |

**请求/响应**：
- `FlowerDTO`：id, name, price, cost, status（无 `Result` 包装）
- `StockAdjustRequest`：flowerId, quantity, costPrice, updateCostPrice, operatorId, reason
- `StockAdjustResult`：success, beforeStock, currentStock, flower, message

**HTTP 状态码语义**：成功返回 200，业务失败（如库存不足）返回 422，避免熔断器误判

---

### 4.3 交易服务 — 销售与库存

服务名：`trade-service`，端口：9103，数据库：`jasmine_trade`

#### SalesController (`/sales`)

| HTTP 方法 | 路径 | 说明 | 认证要求 |
|:---|:---|:---|:---|
| GET | `/sales/all` | 获取全部销售单 | admin/Boss/clerk |
| GET | `/sales/today-summary` | 获取今日经营统计 | admin/Boss/clerk |
| POST | `/sales` | 新增销售单（含幂等防重 + 库存扣减 + Outbox 事件） | admin/Boss/clerk |
| PUT | `/sales` | 修改销售单（回滚旧库存 + 重建新库存） | admin/Boss/clerk |
| GET | `/sales/{id}` | 根据 ID 查询销售单 | admin/Boss/clerk |
| DELETE | `/sales/{id}` | 逻辑删除销售单（回滚库存） | admin/Boss/clerk |
| GET | `/sales/list` | 分页查询销售单 | admin/Boss/clerk |

**关键 DTO/VO**：
- `SalesSaveDTO`：id, vipId, date, remark, items（`List<SalesItemSaveDTO>`）
- `SalesItemSaveDTO`：id, flowerId, quantity, unitPrice
- `SalesQueryDTO`：orderNo, startTime, endTime, pageNo, pageSize
- `SalesVO`：id, orderNo, vipId, vipName, vipPhone, totalAmount, remark, itemCount, date, items
- `SalesItemVO`：id, flowerId, flowerName, quantity, unitPrice, unitCost, amount, costAmount
- `TodayBusinessSummaryVO`：todaySalesAmount, todayPurchaseCost, todayGrossProfit, todayNetCashflow, todaySalesOrderCount, todayPurchaseCount

**核心业务流程**：
- 创建销售单：验证会员 → 扣减库存（远程调用 product-service）→ 快照成本价 → 写入销售单/明细/库存流水 → Outbox 发布 `SalesCreatedMessage` + `InventoryChangedMessage`
- 修改/删除：回滚旧库存（远程调用 product-service 恢复）→ 重建新库存

#### InventoryController (`/inventory`)

| HTTP 方法 | 路径 | 说明 | 认证要求 |
|:---|:---|:---|:---|
| GET | `/inventory/all` | 获取全部库存流水 | admin/Boss/clerk |
| POST | `/inventory` | 新增库存动作（含幂等防重） | admin/Boss/clerk |
| PUT | `/inventory` | 修改库存动作 | admin/Boss/clerk |
| GET | `/inventory/{id}` | 根据 ID 查询库存流水 | admin/Boss/clerk |
| DELETE | `/inventory/{id}` | 逻辑删除库存流水 | admin/Boss/clerk |
| GET | `/inventory/list` | 分页查询库存流水 | admin/Boss/clerk |
| GET | `/inventory/low-stock-count` | 获取低库存预警数量 | admin/Boss/clerk |

**关键 DTO/VO**：
- `InventorySaveDTO`：id, flowerId, bizType, quantity, unitCost, date, remark
- `InventoryQueryDTO`：name, num, bizType, startTime, endTime, pageNo, pageSize
- `InventoryVO`：id, bizNo, flowerId, flowerName, bizType, bizTypeLabel, quantity, beforeStock, afterStock, unitCost, totalCost, remark, date

**库存业务类型枚举** (`InventoryBizType`)：

| 枚举值 | 标签 | 方向 |
|:---|:---|:---|
| `PURCHASE_IN` | 采购入库 | +1 |
| `SALE_OUT` | 销售出库 | -1 |
| `LOSS_OUT` | 损耗出库 | -1 |
| `RETURN_IN` | 退货入库 | +1 |
| `CHECK_IN` | 盘点调增 | +1 |
| `CHECK_OUT` | 盘点调减 | -1 |

#### InventoryAlertController (`/inventory-alert`)

| HTTP 方法 | 路径 | 说明 | 认证要求 |
|:---|:---|:---|:---|
| GET | `/inventory-alert/low-stock-count` | 获取低库存预警数量 | admin/Boss/clerk |
| GET | `/inventory-alert/list` | 分页查询库存预警 | admin/Boss/clerk |

**关键 DTO/VO**：
- `InventoryAlertQueryDTO`：flowerName, alertStatus, pageNo, pageSize
- `InventoryAlertVO`：id, flowerId, flowerNameSnapshot, safeStock, currentStock, alertStatus, lastTriggerTime, lastRecoverTime, remark

---

### 4.4 CRM 服务 — 客户关系管理

服务名：`crm-service`，端口：9104，数据库：`jasmine_crm`

#### VipController (`/vip`)

| HTTP 方法 | 路径 | 说明 | 认证要求 |
|:---|:---|:---|:---|
| GET | `/vip/all` | 获取全部会员 | admin/Boss |
| POST | `/vip` | 新增会员（自动生成会员卡号 + 手机号唯一校验） | admin/Boss |
| PUT | `/vip` | 修改会员（手机号唯一校验） | admin/Boss |
| GET | `/vip/{id}` | 根据 ID 查询会员 | admin/Boss |
| DELETE | `/vip/{id}` | 逻辑删除会员 | admin/Boss |
| GET | `/vip/list` | 分页查询会员（支持姓名/卡号/手机号模糊搜索） | admin/Boss |

**关键 DTO/VO**：
- `VipSaveDTO`：id, vid, name, sex, phone
- `VipQueryDTO`：name, vid, phone, pageNo, pageSize
- `VipVO`：id, vid, name, sex, phone

#### AppointmentController (`/appointment`)

| HTTP 方法 | 路径 | 说明 | 认证要求 |
|:---|:---|:---|:---|
| GET | `/appointment/all` | 获取全部预约 | admin/Boss |
| POST | `/appointment` | 新增预约（含幂等防重 + 延时提醒事件） | admin/Boss |
| PUT | `/appointment` | 修改预约（重新发布延时提醒） | admin/Boss |
| GET | `/appointment/{id}` | 根据 ID 查询预约 | admin/Boss |
| DELETE | `/appointment/{id}` | 逻辑删除预约 | admin/Boss |
| GET | `/appointment/list` | 分页查询预约（支持会员姓名/手机号筛选） | admin/Boss |

**关键 DTO/VO**：
- `AppointmentCreateDTO`：vipId, vid, phone, date, content（vipId/vid/phone 三选一）
- `AppointmentUpdateDTO`：id, date, content
- `AppointmentQueryDTO`：name, phone, startTime, endTime, pageNo, pageSize
- `AppointmentVO`：id, vipId, vid, name, sex, phone, content, date

#### SiteMessageController (`/site-message`)

| HTTP 方法 | 路径 | 说明 | 认证要求 |
|:---|:---|:---|:---|
| GET | `/site-message/unread-count` | 获取当前用户未读站内信数量 | 已登录 |
| GET | `/site-message/list` | 分页查询当前用户未读站内信列表 | 已登录 |
| PUT | `/site-message/read/{id}` | 标记单条站内信为已读（验证归属） | 已登录 |
| PUT | `/site-message/read-all` | 全部标记为已读 | 已登录 |

---

### 4.5 观测接口（所有服务共享）

| HTTP 方法 | 路径 | 说明 | 认证要求 |
|:---|:---|:---|:---|
| GET | `/actuator/health` | 健康检查 | 公开 |
| GET | `/actuator/info` | 应用信息 | 公开 |
| GET | `/actuator/prometheus` | Prometheus 指标 | 公开 |
| GET | `/swagger-ui/index.html` | Swagger UI | 公开 |
| GET | `/v3/api-docs` | OpenAPI 文档 | 公开 |

---

## 五、数据模型

### 5.1 数据库总览

| 服务 | 数据库 | 包含表 |
|:---|:---|:---|
| iam-service | `jasmine_iam` | user, role, user_role, menu, role_menu, auth_refresh_token |
| product-service | `jasmine_product` | flower |
| trade-service | `jasmine_trade` | sales, sales_item, inventory, inventory_alert, event_outbox |
| crm-service | `jasmine_crm` | vip, appointment, site_message, event_outbox |

### 5.2 核心表结构

#### user（用户表）

| 字段 | 类型 | 说明 |
|:---|:---|:---|
| id | int AUTO_INCREMENT | 主键 |
| username | varchar(50) NOT NULL UNIQUE | 用户名 |
| password | varchar(255) NOT NULL | BCrypt 加密密码 |
| email | varchar(100) | 邮箱 |
| phone | varchar(20) | 手机号（索引） |
| status | int DEFAULT 1 | 状态（1=启用） |
| avatar | varchar(255) | 头像 URL |
| deleted | int DEFAULT 0 | 逻辑删除 |

#### role（角色表）

| 字段 | 类型 | 说明 |
|:---|:---|:---|
| role_id | int AUTO_INCREMENT | 主键 |
| role_name | varchar(50) NOT NULL | 角色名 |
| role_desc | varchar(100) | 角色描述 |

#### user_role（用户角色关联表）

| 字段 | 类型 | 说明 |
|:---|:---|:---|
| id | int AUTO_INCREMENT | 主键 |
| user_id | int NOT NULL | 用户 ID（FK → user） |
| role_id | int NOT NULL | 角色 ID（FK → role） |

#### menu（菜单权限表）

| 字段 | 类型 | 说明 |
|:---|:---|:---|
| menu_id | int AUTO_INCREMENT | 主键 |
| component | varchar(100) | Vue 组件路径 |
| path | varchar(100) | 路由路径 |
| redirect | varchar(100) | 重定向 |
| name | varchar(50) | 路由名称 |
| title | varchar(50) | 显示标题 |
| icon | varchar(50) | 图标 |
| parent_id | int DEFAULT 0 | 父菜单 ID（0=根） |
| is_leaf | char(1) DEFAULT 'Y' | 是否叶子节点 |
| hidden | tinyint(1) DEFAULT 0 | 是否隐藏 |

#### role_menu（角色菜单关联表）

| 字段 | 类型 | 说明 |
|:---|:---|:---|
| id | int AUTO_INCREMENT | 主键 |
| role_id | int NOT NULL | 角色 ID（FK → role） |
| menu_id | int NOT NULL | 菜单 ID（FK → menu） |

#### auth_refresh_token（刷新令牌表）

| 字段 | 类型 | 说明 |
|:---|:---|:---|
| id | int AUTO_INCREMENT | 主键 |
| user_id | int NOT NULL | 用户 ID（FK → user） |
| token_id | varchar(64) NOT NULL UNIQUE | UUID 令牌标识 |
| expires_at | datetime NOT NULL | 过期时间 |
| revoked | int DEFAULT 0 | 吊销标记 |
| created_at | datetime | 创建时间 |
| updated_at | datetime | 更新时间 |

#### flower（花卉主数据）

| 字段 | 类型 | 说明 |
|:---|:---|:---|
| id | int AUTO_INCREMENT | 主键 |
| name | varchar(50) NOT NULL UNIQUE | 花卉名称 |
| unit | varchar(10) DEFAULT '枝' | 单位 |
| sale_price | decimal(10,2) NOT NULL | 售价 |
| cost_price | decimal(10,2) NOT NULL | 成本价 |
| safe_stock | int DEFAULT 0 | 安全库存阈值 |
| current_stock | int DEFAULT 0 | 当前库存 |
| status | tinyint DEFAULT 1 | 状态 |
| deleted | tinyint DEFAULT 0 | 逻辑删除 |

#### sales（销售单头）

| 字段 | 类型 | 说明 |
|:---|:---|:---|
| id | int AUTO_INCREMENT | 主键 |
| order_no | varchar(32) NOT NULL UNIQUE | 订单编号 |
| vip_id | int | 会员 ID（FK → vip，跨库） |
| date | datetime NOT NULL | 销售日期 |
| total_amount | decimal(12,2) DEFAULT 0 | 总金额 |
| remark | varchar(200) | 备注 |
| operator_id | int | 操作员 ID（FK → user，跨库） |
| deleted | tinyint DEFAULT 0 | 逻辑删除 |

#### sales_item（销售明细）

| 字段 | 类型 | 说明 |
|:---|:---|:---|
| id | int AUTO_INCREMENT | 主键 |
| sales_id | int NOT NULL | 销售单 ID（FK → sales） |
| flower_id | int NOT NULL | 花卉 ID（FK → flower，跨库） |
| quantity | int NOT NULL | 数量 |
| unit_price | decimal(10,2) NOT NULL | 售价 |
| unit_cost | decimal(10,2) | 成本价快照 |
| amount | decimal(12,2) NOT NULL | 金额 |
| cost_amount | decimal(12,2) | 成本金额 |
| deleted | tinyint DEFAULT 0 | 逻辑删除 |

#### inventory（库存流水）

| 字段 | 类型 | 说明 |
|:---|:---|:---|
| id | int AUTO_INCREMENT | 主键 |
| biz_no | varchar(32) NOT NULL | 业务编号 |
| flower_id | int NOT NULL | 花卉 ID（FK → flower，跨库） |
| biz_type | varchar(32) NOT NULL | 业务类型（枚举名） |
| quantity | int NOT NULL | 变动数量（正数，方向由 bizType 决定） |
| before_stock | int NOT NULL | 变动前库存 |
| after_stock | int NOT NULL | 变动后库存 |
| unit_cost | decimal(10,2) | 单位成本（采购入库时填写） |
| total_cost | decimal(12,2) | 总成本 |
| remark | varchar(200) | 备注 |
| operator_id | int | 操作员 ID |
| date | datetime NOT NULL | 操作时间 |
| deleted | tinyint DEFAULT 0 | 逻辑删除 |

#### inventory_alert（库存预警读模型）

| 字段 | 类型 | 说明 |
|:---|:---|:---|
| id | int AUTO_INCREMENT | 主键 |
| flower_id | int NOT NULL UNIQUE | 花卉 ID |
| flower_name_snapshot | varchar(64) NOT NULL | 花卉名称快照 |
| safe_stock | int NOT NULL | 安全库存阈值 |
| current_stock | int NOT NULL | 当前库存 |
| alert_status | varchar(16) DEFAULT 'NORMAL' | NORMAL/LOW_STOCK |
| last_trigger_time | datetime | 最近触发低库存时间 |
| last_recover_time | datetime | 最近恢复时间 |
| remark | varchar(256) | 备注 |

#### vip（会员表）

| 字段 | 类型 | 说明 |
|:---|:---|:---|
| id | int AUTO_INCREMENT | 主键 |
| vid | varchar(20) NOT NULL UNIQUE | 会员卡号（自动生成） |
| name | varchar(50) NOT NULL | 姓名 |
| sex | varchar(2) | 性别 |
| phone | varchar(20) NOT NULL UNIQUE | 手机号 |
| deleted | int DEFAULT 0 | 逻辑删除 |

#### appointment（预约表）

| 字段 | 类型 | 说明 |
|:---|:---|:---|
| id | int AUTO_INCREMENT | 主键 |
| vip_id | int NOT NULL | 会员 ID（FK → vip） |
| date | datetime NOT NULL | 预约时间 |
| content | varchar(100) NOT NULL | 预约内容 |
| deleted | int DEFAULT 0 | 逻辑删除 |

#### site_message（站内通知消息表）

| 字段 | 类型 | 说明 |
|:---|:---|:---|
| id | bigint AUTO_INCREMENT | 主键 |
| biz_type | varchar(64) NOT NULL | 业务类型（如 APPOINTMENT_REMINDER） |
| biz_id | varchar(64) | 业务主键 |
| receiver_user_id | int | 接收人用户 ID |
| title | varchar(128) NOT NULL | 标题 |
| content | varchar(512) NOT NULL | 正文 |
| is_read | tinyint(1) DEFAULT 0 | 0=未读 1=已读 |
| created_at | datetime NOT NULL | 消息生成时间 |

#### event_outbox（本地消息表，trade 和 crm 各一份）

| 字段 | 类型 | 说明 |
|:---|:---|:---|
| id | bigint AUTO_INCREMENT | 主键 |
| event_type | varchar(64) NOT NULL | 事件类型 |
| exchange | varchar(128) NOT NULL | 目标交换机 |
| routing_key | varchar(128) NOT NULL | 目标路由键 |
| payload | text NOT NULL | 消息体 JSON |
| delay_ms | bigint | 延迟投递毫秒数 |
| status | varchar(16) DEFAULT 'PENDING' | PENDING/SENT/FAILED |
| retry_count | int DEFAULT 0 | 已重试次数 |
| next_retry_time | datetime | 下次允许重试时间 |
| last_error | varchar(512) | 最近一次失败原因 |
| created_at | datetime NOT NULL | 写入时间 |
| sent_at | datetime | 发送成功时间 |

---

## 六、MQ 拓扑与事件契约

### 6.1 交换机与队列

| 交换机 | 类型 | 说明 |
|:---|:---|:---|
| `jasmine.appointment.event` | Topic | 预约域事件交换机 |
| `jasmine.audit.event` | Topic | 审计域事件交换机 |
| `jasmine.trade.event` | Topic | 交易域事件交换机 |
| `jasmine.dlx` | Topic | 统一死信交换机 |

| 队列 | 绑定交换机 | 路由键 | 说明 |
|:---|:---|:---|:---|
| `jasmine.appointment.notification` | appointment.event | `appointment.created` | 预约创建通知 |
| `jasmine.appointment.delay` | appointment.event | `appointment.delay` | 延时驻留队列（无消费者，靠 TTL 过期后弹射） |
| `jasmine.appointment.reminder` | appointment.event | `appointment.reminder` | 延时唤醒队列（消费者监听生成站内信） |
| `jasmine.audit.access-log` | audit.event | `audit.access-log` | 访问日志审计 |
| `jasmine.sales.event-log` | trade.event | `sales.created` | 销售事件 |
| `jasmine.inventory.event-log` | trade.event | `inventory.changed` | 库存变更事件 |
| `jasmine.dlq` | jasmine.dlx | `dead-letter.#` | 统一死信队列 |

### 6.2 消息体定义

| 事件类型 | 路由键 | 消息体 | 说明 |
|:---|:---|:---|:---|
| `appointment.created` | appointment.created | `AppointmentCreatedMessage` | appointmentId, vipId, vipName, vipPhone, appointmentTime, content, occurredAt |
| `appointment.delay` | appointment.delay | `AppointmentCreatedMessage` + delay_ms | 延时投递，预约到店前 1 小时提醒 |
| `appointment.reminder` | appointment.reminder | `AppointmentCreatedMessage` | 死信弹射后的唤醒消息 |
| `audit.access-log` | audit.access-log | `AccessLogMessage` | traceId, requestId, userId, username, method, uri, status, durationMs, clientIp, occurredAt |
| `sales.created` | sales.created | `SalesCreatedMessage` | salesId, orderNo, vipId, operatorId, itemCount, totalAmount, salesTime, occurredAt |
| `inventory.changed` | inventory.changed | `InventoryChangedMessage` | inventoryId, bizNo, flowerId, bizType, quantity, beforeStock, afterStock, operatorId, bizTime, occurredAt, changeSource, changeAction |

### 6.3 消费者与下游

| 消费者 | 所属服务 | 监听队列 | 下游动作 |
|:---|:---|:---|:---|
| `AppointmentNotificationListener` | crm-service | appointment.notification | 验证消息字段 + 幂等检查 + 日志记录（预留短信/微信集成） |
| `AppointmentReminderListener` | crm-service | appointment.reminder | 验证预约仍存在 + 时间差检查（处理改期）+ 查询 admin/Boss/clerk 角色活跃用户 + 批量写入站内信 |
| `AccessLogAuditListener` | common（共享） | audit.access-log | 幂等检查 + 写入访问日志 |
| `SalesEventListener` | trade-service | sales.event-log | 幂等检查 + 日志记录（预留报表/审计） |
| `InventoryEventListener` | trade-service | inventory.event-log | 幂等检查 + 更新 `inventory_alert` 预警读模型 |

### 6.4 Outbox 可靠投递流程

1. 主事务内调用 `OutboxService#save()` 写入 `event_outbox` 表（status=PENDING）
2. `OutboxRelay` 每 5 秒扫描 PENDING 记录（`next_retry_time <= now`），每批 50 条
3. 使用乐观锁（CAS 更新 status + next_retry_time）防止集群重复发送
4. 通过 `RabbitTemplate.send()` 发送，附带 `CorrelationData(outboxId)`
5. `ConfirmCallback`：broker 确认后更新 `PENDING → SENT`
6. `ReturnsCallback`：消息不可路由时标记 `FAILED`
7. 指数退避重试：基础 1 秒，最大 5 次，超过后标记 `FAILED`
8. 延时消息通过 per-message `expiration` 头实现（预约提醒场景）

### 6.5 延时提醒机制（per-message TTL）

```
预约创建
    │
    ▼
OutboxService.save() with delayMs = (预约时间 - 当前时间 - 3600s)
    │
    ▼ (OutboxRelay 每 5 秒扫描)
RabbitTemplate.send() → exchange=jasmine.appointment.event, key=appointment.delay
    │ (消息驻留在 delay 队列，无消费者)
    ▼ (TTL 过期 → 死信弹射到 jasmine.appointment.event, key=appointment.reminder)
AppointmentReminderListener 消费
    → 验证预约仍存在且未删除
    → 时间差检查（60 秒容差，处理改期场景）
    → 查询 admin/Boss/clerk 角色活跃用户
    → 批量写入站内信
```

---

## 七、服务间通信

### 7.1 调用方式

使用 Spring 6+ `RestClient` + `@HttpExchange` 声明式接口（不使用 OpenFeign）：

```java
// trade-service 中的声明式客户端
@HttpExchange(url = "/internal/flower", contentType = "application/json")
public interface FlowerClient {
    @GetExchange("/{id}")
    FlowerDTO getFlowerById(@PathVariable Integer id);

    @PostExchange("/stock/adjust")
    StockAdjustResult adjustStock(@RequestBody StockAdjustRequest request);
}
```

### 7.2 跨域调用清单

| 调用方 | 被调用方 | 接口 | 说明 |
|:---|:---|:---|:---|
| trade-service | product-service | `GET /internal/flower/{id}` | 查询花卉售价/成本 |
| trade-service | product-service | `POST /internal/flower/stock/adjust` | CAS 原子库存扣减/恢复 |
| trade-service | iam-service | `GET /internal/user/{id}` | 查询操作员姓名 |
| crm-service | crm-service 内部 | — | 同一服务，无需远程调用 |
| trade-service | crm-service 内部 | — | 通过 VipReadFacade 本地调用（临时依赖） |

### 7.3 内部接口设计规范

- 所有仅供服务间调用的接口统一使用 `/internal/` 前缀
- Gateway 对 `/internal/**` 返回 403 Forbidden
- 响应不使用 `Result` 包装，直接返回 DTO
- 服务间调用自动附带 `X-Gateway-Token` 请求头

### 7.4 超时与熔断策略

| 配置项 | 值 | 说明 |
|:---|:---|:---|
| 连接超时 | 3 秒 | RestClient 连接超时 |
| 读取超时 | 5 秒 | RestClient 读取超时 |
| 滑动窗口大小 | 10 次调用 | Resilience4j 熔断器 |
| 失败率阈值 | 50% | 触发熔断 |
| 开启状态持续 | 10 秒 | 熔断后等待时间 |
| 半开探测次数 | 3 次 | 半开状态试探调用数 |

**业务异常处理**：`BusinessException`（如库存不足）不触发熔断，直接传播；服务不可用异常触发熔断。

---

## 八、安全架构

### 8.1 认证流程

1. 客户端 `POST /user/login` 提交 username + password（经 Gateway 白名单放行）
2. IAM 服务校验账号状态 + BCrypt 密码匹配
3. 签发 Access Token（30 分钟）+ Refresh Token（7 天）
4. Access Token 放入响应 JSON，Refresh Token 写入 HttpOnly Cookie

### 8.2 JWT 验证链（微服务架构）

```
Request → Gateway: JwtAuthGlobalFilter
                    │
                    ├─ 清洗伪造请求头
                    ├─ /internal/** → 403
                    ├─ 白名单 → 注入 X-Gateway-Token → 放行
                    └─ 提取 Bearer Token → JwtUtil.parseAccessToken()
                            │
                            ├─ 验证签名 + 过期 + 类型
                            ├─ 注入 X-User-Id, X-User-Name, X-Gateway-Token
                            └─ 转发到下游服务
                                    │
                                    ▼
                          InternalEndpointGuardFilter（验证 X-Gateway-Token）
                                    │
                                    ▼
                          CurrentUserProvider（从请求头提取用户信息）
                                    │
                                    ▼
                          JwtAuthenticationFilter（IAM 服务专用，回查用户状态）
                                    │
                                    ▼
                          装配 ROLE_xxx authority → SecurityContext
```

### 8.3 授权策略

| 路径模式 | 允许角色 |
|:---|:---|
| `/user/login`, `/user/refresh`, `/actuator/**`, `/swagger-ui/**` | 公开 |
| `/user/info`, `/user/logout`, `/user/changePassword`, `/site-message/**` | 已登录 |
| `/user/**`, `/role/**`, `/menu/**`, `/sys/**` | admin |
| `/vip/**`, `/appointment/**` | admin, Boss |
| `/flower/**`, `/sales/**`, `/inventory/**`, `/inventory-alert/**` | admin, Boss, clerk |
| 其他 | 拒绝 |

### 8.4 令牌存储策略

| 令牌类型 | 存储位置 | 安全属性 |
|:---|:---|:---|
| Access Token | 前端 `sessionStorage` | 短生命周期，关闭标签页即清除 |
| Refresh Token | HttpOnly Cookie | Secure + SameSite=Lax，后端写入 |

### 8.5 JWT 令牌结构

```
Header:  { "alg": "HS256" }
Payload: {
  "jti": "<random-UUID>",
  "sub": "<username>",
  "iss": "system",
  "uid": <userId>,
  "type": "access" | "refresh",
  "iat": <issued-at>,
  "exp": <expiration>
}
```

---

## 九、缓存架构

### 9.1 缓存名称与策略

| 缓存名 | 对象 | TTL | 抖动 | 说明 |
|:---|:---|:---|:---|:---|
| `user` | User | 30 分钟 | 0-5 分钟 | 用户主数据，按 ID 缓存 |
| `menuList` | List\<Menu\> | 30 分钟 | 0-5 分钟 | 用户菜单树，按 userId 缓存 |
| `roleList` | List\<Role\> | 30 分钟 | 0-5 分钟 | 角色列表 |
| `flowerList` | List\<Flower\> | 10 分钟 | 0-2 分钟 | 花卉主数据列表 |
| `flowerDetail` | Flower | 10 分钟 | 0-2 分钟 | 花卉详情，按 ID 缓存 |

### 9.2 缓存环境切换

- 开发环境（`app.cache.type=memory`）：`ConcurrentMapCacheManager`
- 生产环境（`app.cache.type=redis`）：`RedisCacheManager` + `GenericJackson2JsonRedisSerializer`
- TTL 抖动机制：随机追加秒数，防止同批 key 集中过期（缓存雪崩）
- 缓存 key 前缀：`${spring.application.name}:`（如 `iam-service:`、`product-service:`），防止跨服务 key 冲突

### 9.3 缓存注解使用

| 服务 | 方法 | 注解 | 缓存 | Key |
|:---|:---|:---|:---|:---|
| iam | `getUserById()` | `@Cacheable` | user | `#id` |
| iam | `updateUser()` | `@CacheEvict` | user + menuList | `#user.id` |
| iam | `deleteUserById()` | `@CacheEvict` | user + menuList | `#id` |
| iam | `changePassword()` | `@CacheEvict` | user（全部） | — |
| iam | `getMenuListByUserId()` | `@Cacheable` | menuList | `#userId` |
| iam | `listAllRoles()` | `@Cacheable` | roleList | `'all'` |
| iam | `addRole/updateRole/deleteRoleById()` | `@CacheEvict` | roleList + menuList（全部） | — |
| product | `listAllFlowers()` | `@Cacheable` | flowerList | `'all'` |
| product | `getFlowerDetailById()` | `@Cacheable` | flowerDetail | `#id` |
| product | `addFlower()` | `@CacheEvict` | flowerList + flowerDetail（全部） | — |
| product | `updateFlower()` | `@CacheEvict` | flowerList（全部）+ flowerDetail | `#flower.id` |
| product | `deleteFlowerById()` | `@CacheEvict` | flowerList（全部）+ flowerDetail | `#id` |
| trade | `saveSales/updateSales/deleteSales()` | `@CacheEvict` | flowerList + flowerDetail | 每个受影响的 flowerId |
| trade | `saveInventory/updateInventory/deleteInventory()` | `@CacheEvict` | flowerList + flowerDetail | 每个受影响的 flowerId |

---

## 十、幂等与防重

### 10.1 请求级幂等

- 覆盖接口：`POST /sales`、`POST /inventory`、`POST /appointment`
- 机制：前端传 `X-Idempotency-Key` 请求头，后端 `RequestIdempotencyService` 基于 Redis SETNX 去重
- 默认防重窗口：30 秒
- 回退机制：Redis 不可用时降级为本地 `ConcurrentHashMap`
- 失败释放：业务操作失败时释放幂等键，允许客户端重试

### 10.2 MQ 消费幂等

- `MqIdempotencyService`：基于 Redis SETNX 的消费幂等键，默认保留 24 小时
- 键格式：`jasmine:mq:idempotent:{event-type}:{business-id}`
- 回退机制：Redis 不可用时降级为本地 `ConcurrentHashMap`（含过期追踪）

---

## 十一、前端架构

### 11.1 技术栈

| 技术 | 版本 | 说明 |
|:---|:---|:---|
| Vue | 3.5.32 | 前端框架 |
| Vue Router | 4.6.3 | 路由（Hash 模式） |
| Pinia | 3.0.3 | 状态管理 |
| Element Plus | 2.11.5 | 组件库 |
| Axios | 1.13.1 | HTTP 请求 |
| Vite | 8.0.4 | 构建工具 |
| Bun | 1.3.12 | 本地开发与依赖管理 |
| TypeScript | 6.0.2 | 类型安全 |

### 11.2 页面与路由

| 页面 | 组件路径 | 对应后端接口域 |
|:---|:---|:---|
| 登录 | `views/login/LoginView.vue` | IAM |
| 首页仪表盘 | `views/dashboard/DashboardView.vue` | Sales（今日统计） |
| 个人信息 | `views/profile/ProfileView.vue` | IAM |
| 用户管理 | `views/system/UserManageView.vue` | IAM |
| 角色管理 | `views/system/RoleManageView.vue` | IAM |
| 花卉管理 | `views/flower/FlowerManageView.vue` | Flower + Inventory |
| 库存管理 | `views/inventory/InventoryManageView.vue` | Inventory + InventoryAlert |
| 销售管理 | `views/sales/SalesManageView.vue` | Sales |
| 会员管理 | `views/vip/VipManageView.vue` | Vip |
| 预约管理 | `views/appointment/AppointmentManageView.vue` | Appointment |

### 11.3 路由守卫与动态菜单

- 登录后从 `/user/info` 获取用户信息与菜单树
- 前端根据菜单数据动态注册路由（`buildDynamicRoutes`）
- 刷新页面时通过 Refresh Token Cookie 自动恢复会话
- 全局消息中心悬浮窗：`AppLayout.vue`，聚合库存预警与预约待办

### 11.4 API 基路径

- 开发环境：`/prod-api` → Vite 代理 → `http://localhost:8080`（Gateway）
- 生产环境：Nginx 反向代理 `/prod-api/` → `jasmine-gateway:8080`

---

## 十二、部署架构

### 12.1 容器化

- 后端：参数化 `Dockerfile`，基于 Eclipse Temurin JDK 21，`ARG MODULE` 构建任一模块
- 前端：`web/Dockerfile`，Bun 构建 + Nginx Alpine 托管
- 中间件：MySQL 8.4 + Redis 7.2 + RabbitMQ 4.2 + Nacos 2.4.x

### 12.2 环境编排

#### 开发环境（`ops/dev/docker-compose.yml`）

仅启动中间件，业务服务由本机 Maven 启动：

| 服务 | 镜像 | 端口映射 | 说明 |
|:---|:---|:---|:---|
| MySQL | mysql:8.0 | 13306:3306 | tmpfs 数据目录（WSL2 兼容） |
| Redis | redis:7.2-alpine | 6379:6379 | 持久化：AOF |
| RabbitMQ | rabbitmq:4.2-management | 5673:5672, 15673:15672 | vhost=/jasmine |
| Nacos | nacos/nacos-server:v2.4.3 | 8848:8848, 9848:9848 | 单机模式，鉴权关闭 |

MySQL 启动时通过 `init-databases.sql` 自动创建 5 个数据库（jasmine_iam, jasmine_product, jasmine_trade, jasmine_crm, nacos）。

#### 生产环境（`ops/prod/docker-compose.yml`）

全栈部署，包含 7 个服务 + 前端：

| 服务 | 容器名 | 端口 | 依赖 |
|:---|:---|:---|:---|
| MySQL | jasmine-prod-mysql | 3306 | — |
| Redis | jasmine-prod-redis | 6379 | — |
| RabbitMQ | jasmine-prod-rabbitmq | 5672/15672 | — |
| Nacos | jasmine-prod-nacos | 8848/9848 | MySQL 健康 |
| jasmine-schema | jasmine-prod-schema | — | MySQL 健康，执行完退出 |
| jasmine-gateway | jasmine-prod-gateway | 8080 | Nacos 健康 |
| iam-service | jasmine-prod-iam | 9101（内部） | MySQL/Redis/RabbitMQ/Nacos 健康 + schema 完成 |
| product-service | jasmine-prod-product | 9102（内部） | 同上 |
| trade-service | jasmine-prod-trade | 9103（内部） | 同上 |
| crm-service | jasmine-prod-crm | 9104（内部） | 同上 |
| frontend | jasmine-prod-frontend | 80 | Gateway 健康 |

**生产特性**：
- 所有服务配置健康检查（`/actuator/health`）
- 内存限制：每服务 512MB 上限，256MB 预留
- Nacos 鉴权启用（需 `NACOS_AUTH_TOKEN` ≥ 32 字节）
- Nacos 使用独立 `nacos` 数据库
- 启动顺序：MySQL → (Redis, RabbitMQ, Nacos) → schema → 业务服务 → 前端
- 仅 Gateway（8080）和前端（80）对外暴露

### 12.3 Dockerfile 配置

后端运行时 JVM 调优：
- 垃圾回收器：ZGC
- 堆内存：256-384MB
- 压缩指针：启用
- 字符串去重：启用
- 内存预触摸：启用

### 12.4 CI/CD

- GitHub Actions：`.github/workflows/docker-publish.yml`
- 触发条件：push 到 `main`/`microservices`、`v*` 标签、手动触发
- 构建策略：矩阵并行构建 6 个后端模块 + 1 个前端模块
- 镜像仓库：`ghcr.io/jipzeongit/jasmine-*`
- 标签策略：`<branch>-latest`、`sha-<7字符>`、版本标签
- 缓存：BuildKit GHA cache

---

## 十三、Nacos 配置中心

### 13.1 配置分层

| Data ID | 用途 | 关键内容 |
|:---|:---|:---|
| `jasmine-common.yml` | 非敏感共享配置 | 缓存类型、网关共享令牌 |
| `jasmine-db-common.yml` | 共享数据库配置 | MySQL 驱动、凭证（环境变量）、URL 模板 |
| `jasmine-redis-common.yml` | 共享 Redis 配置 | 主机、端口、密码（环境变量） |
| `jasmine-mq-common.yml` | 共享 RabbitMQ 配置 | 主机、端口、凭证、vhost=/jasmine |
| `jasmine-observability.yml` | 可观测性配置 | Actuator 端点、Prometheus 指标、JVM/HikariCP/HTTP 指标 |
| `jasmine-gateway.yml` | 网关专属 | 路由表、CORS、JWT 密钥、网关共享令牌 |
| `jasmine-iam.yml` | IAM 专属 | 数据源 URL（jasmine_iam） |
| `jasmine-product.yml` | 花卉专属 | 数据源 URL（jasmine_product） |
| `jasmine-trade.yml` | 交易专属 | 数据源 URL（jasmine_trade） |
| `jasmine-crm.yml` | CRM 专属 | 数据源 URL（jasmine_crm） |

### 13.2 配置导入

- 导入脚本：`ops/nacos-config/import.sh <nacos-addr> [namespace]`
- 自动创建命名空间（如不存在）
- 支持 Nacos 2.x 鉴权（通过 `/v1/auth/users/login` 获取 accessToken）
- 使用 `curl --data-urlencode` 安全上传 YAML 内容

### 13.3 命名空间策略

| 环境 | 命名空间 | 配置 Group |
|:---|:---|:---|
| 开发 | `dev` | `JASMINE` |
| 生产 | `prod` | `JASMINE` |

---

## 十四、配置项索引

| 配置项 | 默认值 | 说明 |
|:---|:---|:---|
| `server.port` | 8080(gateway)/9101(iam)/9102(product)/9103(trade)/9104(crm) | 各服务端口 |
| `spring.application.name` | jasmine-gateway/iam-service/product-service/trade-service/crm-service | 服务名（Nacos 注册） |
| `app.cache.type` | memory(dev) / redis(prod) | 缓存类型 |
| `app.security.jwt-secret` | 开发默认值 | JWT 签名密钥 |
| `app.security.jwt-expire-millis` | 1800000 | Access Token 有效期（30 分钟） |
| `app.security.jwt-refresh-expire-millis` | 604800000 | Refresh Token 有效期（7 天） |
| `app.security.gateway-shared-token` | 开发默认值 | 网关共享令牌（服务间信任） |
| `app.cors.allowed-origins` | localhost:8888,localhost:5173,... | CORS 允许来源 |
| `app.mq.enabled` | false(dev) / true(prod) | MQ 启用开关 |
| `app.mq.dead-letter-enabled` | true | 死信交换机启用 |
| `app.mq.idempotency-ttl-hours` | 24 | 消费幂等键保留时间 |
| `app.mq.retry.max-attempts` | 3 | 消费重试最大次数 |
| `app.mq.retry.initial-interval-ms` | 1000 | 重试初始退避间隔 |
| `app.mq.retry.multiplier` | 2.0 | 重试退避乘数 |
| `app.mq.retry.max-interval-ms` | 10000 | 重试最大退避间隔 |
| `app.idempotency.create-ttl-seconds` | 30 | 创建类接口防重窗口 |
| `app.outbox.relay.interval-ms` | 5000 | Outbox Relay 扫描间隔 |
| `app.outbox.relay.batch-size` | 50 | Outbox Relay 每批处理量 |
| `spring.cloud.nacos.server-addr` | 127.0.0.1:8848 | Nacos 地址 |
| `spring.cloud.nacos.discovery.namespace` | dev | Nacos 命名空间 |
| `spring.cloud.nacos.discovery.group` | JASMINE | Nacos 服务分组 |

---

## 十五、关键设计决策摘要

| 决策 | 选择 | 理由 |
|:---|:---|:---|
| 服务拆分方案 | 方案 A+（4 服务 + 1 网关） | 与现有模块化单体域划分一一对应，迁移成本最低 |
| trade-service 内 sales+inventory | 保持合并，不引入 Seata | 同一事务内完成库存扣减 + Outbox 写入，当前业务量不需要分布式事务 |
| crm-service 内 vip+appointment | 保持合并 | 预约创建需查询会员，站内信在同一域内 |
| 服务间调用 | RestClient + @HttpExchange | OpenFeign 在 Spring Cloud 2025.x 中不再积极维护 |
| 网关鉴权 | 网关统一 JWT 验证 + 下游信任请求头 | RBAC 下沉到各服务，网关保持轻量 |
| 内部接口保护 | X-Gateway-Token 共享密钥 + InternalEndpointGuardFilter | 纵深防御，防止绕过网关直接访问 |
| 限流/熔断 | Resilience4j（不引入 Sentinel） | 当前业务量极小，简单熔断足够 |
| MQ | RabbitMQ（不引入 Kafka） | 已深度使用，拓扑可直接复用 |
| 配置中心 | Nacos | Spring Cloud Alibaba 生态，注册中心 + 配置中心一体 |
| Outbox | 每服务独立 Outbox 表 + Relay | 保证事件不丢失，支持延时消息 |
