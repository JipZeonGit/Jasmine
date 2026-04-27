# Jasmine 单体应用架构与详细接口说明书

> 本文档基于 `next` 分支 PR20 完成后的代码基线编写，用于全面记录当前模块化单体的架构形态、业务域划分、接口清单、数据模型与基础设施配置。

---

## 一、项目概览

| 属性 | 值 |
|:---|:---|
| 项目名称 | Jasmine（花店门店管理系统） |
| 基础包名 | `com.nfu.jasmine` |
| Java 版本 | 21 |
| Spring Boot | 3.5.13 |
| 持久层 | MyBatis-Plus 3.5.14 |
| 安全框架 | Spring Security + JWT (0.12.7) |
| 数据库 | MySQL 8.4 |
| 缓存 | Redis 7.2（生产）/ Caffeine 内存（开发） |
| 消息队列 | RabbitMQ 4.2-management |
| API 文档 | springdoc-openapi 2.8.16 |
| 指标监控 | Micrometer + Prometheus |
| 前端 | Vue 3.5 + Element Plus 2.13 + Vite 8.0 |
| 后端端口 | 9999 |
| 前端端口 | 5173 |

---

## 二、架构形态

当前 Jasmine 为**按业务域分包的模块化单体**，每个业务域内部遵循统一分层约定。

### 2.1 整体架构图

```
┌──────────────────────────────────────────────────────────────┐
│                        前端 (web/)                           │
│   Vue 3 + Element Plus + Pinia + Vue Router + Axios          │
└────────────────────────┬─────────────────────────────────────┘
                         │ HTTP / Bearer JWT
┌────────────────────────▼─────────────────────────────────────┐
│                    Spring Boot 单体应用                       │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐        │
│  │  iam 域  │ │ flower域 │ │inventory │ │  sales域 │        │
│  │ 认证鉴权 │ │ 花卉主数据│ │  库存域   │ │  销售域  │        │
│  └──────────┘ └──────────┘ └──────────┘ └──────────┘        │
│  ┌──────────┐ ┌──────────┐ ┌────────────────────────┐       │
│  │  vip 域  │ │appointment│ │       infra 基础设施   │       │
│  │  会员域  │ │  预约域   │ │ MQ/Outbox/缓存/幂等/通知│       │
│  └──────────┘ └──────────┘ └────────────────────────┘       │
│  ┌──────────────────────────────────────────────────┐       │
│  │            common + config 公共层                 │       │
│  └──────────────────────────────────────────────────┘       │
└────────────────────────┬─────────────────────────────────────┘
                         │
          ┌──────────────┼──────────────┐
          │              │              │
   ┌──────▼──────┐ ┌────▼─────┐ ┌─────▼──────┐
   │   MySQL 8.4 │ │ Redis 7.2│ │ RabbitMQ 4.2│
   └─────────────┘ └──────────┘ └────────────┘
```

### 2.2 后端包结构

```
com.nfu.jasmine
├── JasmineApplication.java           // 启动类
├── common/                           // 通用层
│   ├── dto/PageQueryDTO.java         // 分页查询基类
│   ├── enums/ResultCode.java         // 统一结果码
│   ├── exception/BusinessException.java
│   ├── handler/GlobalExceptionHandler.java
│   ├── utils/
│   │   ├── JwtUtil.java              // JWT 工具
│   │   ├── JwtTokenClaims.java       // JWT Claims 模型
│   │   ├── BusinessNoUtil.java       // 业务编号生成
│   │   ├── MembershipIdUtil.java     // 会员卡号生成
│   │   └── SerialNumberUtil.java     // 序列号生成
│   └── vo/
│       ├── Result.java               // 统一返回包装
│       └── TableData.java            // 分页数据包装
├── config/                           // 配置层
│   ├── MyBatisPlusConfig.java        // MyBatis-Plus 分页插件
│   ├── MyCacheConfig.java            // 内存缓存配置(开发)
│   ├── MyCorsConfig.java             // 跨域配置
│   ├── MyRedisConfig.java            // Redis 缓存配置(生产)
│   ├── MySecurityConfig.java         // Spring Security 配置
│   └── MySwaggerConfig.java          // Swagger 文档配置
├── iam/                              // 【IAM 域】身份与访问管理
│   ├── application/                  //   Service 层
│   ├── model/entity/                 //   实体
│   ├── persistence/mapper/           //   Mapper
│   └── web/                          //   Controller + DTO/VO
├── flower/                           // 【花卉域】花卉主数据
├── vip/                              // 【会员域】VIP 会员
├── sales/                            // 【销售域】销售订单
├── inventory/                        // 【库存域】库存流水 + 预警
│   ├── application/
│   ├── model/
│   ├── persistence/
│   ├── web/
│   └── alert/                        //   库存预警子模块
│       ├── service/
│       ├── model/
│       ├── persistence/
│       └── web/
├── appointment/                      // 【预约域】预约管理
└── infra/                            // 【基础设施域】
    ├── cache/CacheNames.java         //   缓存名称常量
    ├── idempotency/                  //   请求级幂等服务
    ├── mq/                           //   MQ 基础设施
    │   ├── config/RabbitMqTopologyConfig.java
    │   ├── message/                  //     消息体定义
    │   ├── listener/                 //     消费者监听器
    │   ├── publisher/MqMessagePublisher.java
    │   └── support/                  //     幂等与消息辅助
    ├── notification/                 //   站内通知
    │   ├── service/SiteMessageService.java
    │   ├── model/entity/SiteMessage.java
    │   ├── persistence/
    │   └── web/SiteMessageController.java
    ├── outbox/                       //   本地消息表(Outbox)
    │   ├── OutboxService.java
    │   ├── relay/OutboxRelay.java
    │   ├── model/entity/EventOutbox.java
    │   └── persistence/
    ├── security/                     //   安全基础设施
    │   ├── filter/JwtAuthenticationFilter.java
    │   ├── handler/                  //     401/403 处理
    │   └── CurrentUserProvider.java  //     当前用户提供器
    └── web/filter/RequestTraceFilter.java  // 请求追踪
```

### 2.3 业务域内部分层约定

每个业务域统一遵循以下分层：

| 层 | 包路径 | 职责 |
|:---|:---|:---|
| web | `{domain}.web` | Controller + DTO + VO，负责请求接收和响应返回 |
| application | `{domain}.application` | Service 接口与实现，承载业务逻辑 |
| model | `{domain}.model` | Entity + 枚举，对应数据库表 |
| persistence | `{domain}.persistence` | Mapper 接口 + XML，数据库访问 |

**跨域调用原则**：控制器只做请求接收和响应返回；持久层只服务本域；跨域调用走对方 `application` 层。

---

## 三、业务域详细接口清单

### 3.1 IAM 域 — 身份与访问管理

基础路径：`/user`、`/role`、`/menu`

**权限要求**：`/user/**`、`/role/**`、`/menu/**`、`/sys/**` 仅 `ROLE_admin`

#### UserController (`/user`)

| HTTP 方法 | 路径 | 说明 | 认证要求 |
|:---|:---|:---|:---|
| GET | `/user/all` | 获取全部用户 | admin |
| POST | `/user/login` | 用户登录 | 公开 |
| POST | `/user/refresh` | 刷新登录状态 | 公开（Cookie） |
| GET | `/user/info` | 获取当前用户信息与权限路由 | 已登录 |
| POST | `/user/logout` | 注销登录 | 已登录 |
| GET | `/user/list` | 分页查询用户 | admin |
| POST | `/user` | 新增用户 | admin |
| PUT | `/user` | 修改用户 | admin |
| GET | `/user/{id}` | 根据ID查询用户 | admin |
| DELETE | `/user/{id}` | 逻辑删除用户 | admin |
| PUT | `/user/changePassword` | 修改密码 | 已登录 |

**关键 DTO/VO**：
- `LoginDTO`：username, password
- `RefreshTokenDTO`：refreshToken（可选，优先读 Cookie）
- `UserCreateDTO`：username, password, email, phone, status, roleIdList
- `UserUpdateDTO`：id, username, email, phone, status, roleIdList
- `UserQueryDTO`：username, phone, pageNo, pageSize
- `ChangePasswordDTO`：oldPassword, newPassword
- `LoginVO`：accessToken, refreshToken
- `UserInfoVO`：name, avatar, phone, email, status, roles, menuList
- `UserVO`：id, username, email, phone, status, avatar

#### RoleController (`/role`)

| HTTP 方法 | 路径 | 说明 | 认证要求 |
|:---|:---|:---|:---|
| GET | `/role/list` | 分页查询角色 | admin |
| POST | `/role` | 新增角色 | admin |
| PUT | `/role` | 修改角色 | admin |
| GET | `/role/{id}` | 根据ID查询角色 | admin |
| DELETE | `/role/{id}` | 逻辑删除角色 | admin |
| GET | `/role/all` | 获取所有角色 | admin |

**关键 DTO/VO**：
- `RoleSaveDTO`：roleId, roleName, roleDesc, menuIdList
- `RoleQueryDTO`：roleName, pageNo, pageSize
- `RoleVO`：roleId, roleName, roleDesc

#### MenuController (`/menu`)

| HTTP 方法 | 路径 | 说明 | 认证要求 |
|:---|:---|:---|:---|
| GET | `/menu` | 获取全部权限菜单树 | admin |

**关键 VO**：
- `MenuVO`：menuId, component, path, redirect, name, title, icon, parentId, isLeaf, hidden, children

---

### 3.2 花卉域 — 花卉主数据

基础路径：`/flower`

**权限要求**：`/flower/**` 允许 `ROLE_admin`、`ROLE_Boss`、`ROLE_clerk`

#### FlowerController (`/flower`)

| HTTP 方法 | 路径 | 说明 | 认证要求 |
|:---|:---|:---|:---|
| GET | `/flower/all` | 获取全部花卉 | admin/Boss/clerk |
| POST | `/flower` | 新增花卉 | admin/Boss/clerk |
| PUT | `/flower` | 修改花卉 | admin/Boss/clerk |
| GET | `/flower/{id}` | 根据ID查询花卉 | admin/Boss/clerk |
| DELETE | `/flower/{id}` | 逻辑删除花卉（库存为0时允许） | admin/Boss/clerk |
| GET | `/flower/list` | 分页查询花卉 | admin/Boss/clerk |

**关键 DTO/VO**：
- `FlowerSaveDTO`：id, name, unit, salePrice, costPrice, safeStock, status
- `FlowerQueryDTO`：name, pageNo, pageSize
- `FlowerVO`：id, name, unit, salePrice, costPrice, safeStock, currentStock, status

**核心内部服务**：
- `FlowerStockService#adjustStock()`：基于 CAS 的库存原子变更（compare-and-set SQL），最多重试 8 次

---

### 3.3 库存域 — 库存流水与预警

基础路径：`/inventory`、`/inventory-alert`

**权限要求**：`/inventory/**`、`/inventory-alert/**` 允许 `ROLE_admin`、`ROLE_Boss`、`ROLE_clerk`

#### InventoryController (`/inventory`)

| HTTP 方法 | 路径 | 说明 | 认证要求 |
|:---|:---|:---|:---|
| GET | `/inventory/all` | 获取全部库存流水 | admin/Boss/clerk |
| POST | `/inventory` | 新增库存动作（含幂等防重） | admin/Boss/clerk |
| PUT | `/inventory` | 修改库存动作 | admin/Boss/clerk |
| GET | `/inventory/{id}` | 根据ID查询库存流水 | admin/Boss/clerk |
| DELETE | `/inventory/{id}` | 逻辑删除库存流水 | admin/Boss/clerk |
| GET | `/inventory/list` | 分页查询库存流水 | admin/Boss/clerk |
| GET | `/inventory/low-stock-count` | 获取低库存预警数量 | admin/Boss/clerk |

**关键 DTO/VO**：
- `InventorySaveDTO`：flowerId, bizType, quantity, remark
- `InventoryQueryDTO`：flowerName, bizNo, pageNo, pageSize
- `InventoryVO`：id, bizNo, flowerId, bizType, quantity, beforeStock, afterStock, unitCost, totalCost, remark, operatorId, date

**库存业务类型枚举** (`InventoryBizType`)：
- `PURCHASE_IN`：采购入库
- `SALE_OUT`：销售出库
- `MANUAL_ADJUST`：手工调整

#### InventoryAlertController (`/inventory-alert`)

| HTTP 方法 | 路径 | 说明 | 认证要求 |
|:---|:---|:---|:---|
| GET | `/inventory-alert/low-stock-count` | 获取低库存预警数量 | admin/Boss/clerk |
| GET | `/inventory-alert/list` | 分页查询库存预警 | admin/Boss/clerk |

**关键 DTO/VO**：
- `InventoryAlertQueryDTO`：flowerName, alertStatus, pageNo, pageSize
- `InventoryAlertVO`：id, flowerId, flowerNameSnapshot, safeStock, currentStock, alertStatus, lastTriggerTime, lastRecoverTime, remark

---

### 3.4 销售域 — 销售订单

基础路径：`/sales`

**权限要求**：`/sales/**` 允许 `ROLE_admin`、`ROLE_Boss`、`ROLE_clerk`

#### SalesController (`/sales`)

| HTTP 方法 | 路径 | 说明 | 认证要求 |
|:---|:---|:---|:---|
| GET | `/sales/all` | 获取全部销售单 | admin/Boss/clerk |
| GET | `/sales/today-summary` | 获取今日经营统计 | admin/Boss/clerk |
| POST | `/sales` | 新增销售单（含幂等防重） | admin/Boss/clerk |
| PUT | `/sales` | 修改销售单 | admin/Boss/clerk |
| GET | `/sales/{id}` | 根据ID查询销售单 | admin/Boss/clerk |
| DELETE | `/sales/{id}` | 逻辑删除销售单 | admin/Boss/clerk |
| GET | `/sales/list` | 分页查询销售单 | admin/Boss/clerk |

**关键 DTO/VO**：
- `SalesSaveDTO`：vipId, salesItemDTOList, remark
- `SalesItemSaveDTO`：flowerId, quantity
- `SalesQueryDTO`：startDate, endDate, pageNo, pageSize
- `SalesVO`：id, orderNo, vipId, date, totalAmount, remark, operatorId, items
- `SalesItemVO`：id, flowerId, quantity, unitPrice, amount
- `TodayBusinessSummaryVO`：totalSalesAmount, totalSalesCount, totalCostAmount, grossProfit

---

### 3.5 会员域 — VIP 会员

基础路径：`/vip`

**权限要求**：`/vip/**` 允许 `ROLE_admin`、`ROLE_Boss`

#### VipController (`/vip`)

| HTTP 方法 | 路径 | 说明 | 认证要求 |
|:---|:---|:---|:---|
| GET | `/vip/all` | 获取全部会员 | admin/Boss |
| POST | `/vip` | 新增会员（含手机号唯一校验） | admin/Boss |
| PUT | `/vip` | 修改会员 | admin/Boss |
| GET | `/vip/{id}` | 根据ID查询会员 | admin/Boss |
| DELETE | `/vip/{id}` | 逻辑删除会员 | admin/Boss |
| GET | `/vip/list` | 分页查询会员 | admin/Boss |

**关键 DTO/VO**：
- `VipSaveDTO`：id, name, sex, phone
- `VipQueryDTO`：name, vid, phone, pageNo, pageSize
- `VipVO`：id, vid, name, sex, phone

---

### 3.6 预约域 — 预约管理

基础路径：`/appointment`

**权限要求**：`/appointment/**` 允许 `ROLE_admin`、`ROLE_Boss`

#### AppointmentController (`/appointment`)

| HTTP 方法 | 路径 | 说明 | 认证要求 |
|:---|:---|:---|:---|
| GET | `/appointment/all` | 获取全部预约 | admin/Boss |
| POST | `/appointment` | 新增预约（含幂等防重 + 延时提醒） | admin/Boss |
| PUT | `/appointment` | 修改预约 | admin/Boss |
| GET | `/appointment/{id}` | 根据ID查询预约 | admin/Boss |
| DELETE | `/appointment/{id}` | 逻辑删除预约 | admin/Boss |
| GET | `/appointment/list` | 分页查询预约 | admin/Boss |

**关键 DTO/VO**：
- `AppointmentCreateDTO`：vipId, date, content
- `AppointmentUpdateDTO`：id, date, content
- `AppointmentQueryDTO`：name, phone, startDate, endDate, pageNo, pageSize
- `AppointmentVO`：id, vipId, date, content

---

### 3.7 站内通知 — 消息中心

基础路径：`/site-message`

**权限要求**：`/site-message/**` 已登录即可访问

#### SiteMessageController (`/site-message`)

| HTTP 方法 | 路径 | 说明 | 认证要求 |
|:---|:---|:---|:---|
| GET | `/site-message/unread-count` | 获取当前用户未读站内信数量 | 已登录 |
| GET | `/site-message/list` | 分页查询当前用户站内信列表 | 已登录 |
| PUT | `/site-message/read/{id}` | 标记单条站内信为已读 | 已登录 |
| PUT | `/site-message/read-all` | 全部标记为已读 | 已登录 |

---

### 3.8 观测接口

| HTTP 方法 | 路径 | 说明 | 认证要求 |
|:---|:---|:---|:---|
| GET | `/actuator/health` | 健康检查 | 公开 |
| GET | `/actuator/info` | 应用信息 | 公开 |
| GET | `/actuator/prometheus` | Prometheus 指标 | 公开 |
| GET | `/swagger-ui/index.html` | Swagger UI | 公开 |
| GET | `/v3/api-docs` | OpenAPI 文档 | 公开 |

---

## 四、数据模型

### 4.1 数据库表总览

| 域 | 表名 | 说明 | Flyway 版本 |
|:---|:---|:---|:---|
| IAM | `user` | 用户表 | V1 |
| IAM | `role` | 角色表 | V1 |
| IAM | `user_role` | 用户角色关联表 | V1 |
| IAM | `menu` | 菜单权限表 | V1 |
| IAM | `role_menu` | 角色菜单关联表 | V1 |
| IAM | `auth_refresh_token` | 刷新令牌表 | V2 |
| 花卉 | `flower` | 花卉主数据表 | V4（重建） |
| 会员 | `vip` | 会员表 | V4（重建） |
| 预约 | `appointment` | 预约表 | V4（重建） |
| 销售 | `sales` | 销售单头表 | V4（重建） |
| 销售 | `sales_item` | 销售明细表 | V4（重建） |
| 库存 | `inventory` | 库存流水表 | V4（重建） |
| 基础设施 | `event_outbox` | 本地消息表(Outbox) | V6 |
| 库存 | `inventory_alert` | 库存预警读模型 | V6 |
| 基础设施 | `site_message` | 站内通知消息表 | V7 |

### 4.2 核心表结构

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
| vip_id | int | 会员ID（FK → vip） |
| date | datetime NOT NULL | 销售日期 |
| total_amount | decimal(12,2) DEFAULT 0 | 总金额 |
| remark | varchar(200) | 备注 |
| operator_id | int | 操作员ID（FK → user） |
| deleted | tinyint DEFAULT 0 | 逻辑删除 |

#### sales_item（销售明细）

| 字段 | 类型 | 说明 |
|:---|:---|:---|
| id | int AUTO_INCREMENT | 主键 |
| sales_id | int NOT NULL | 销售单ID（FK → sales） |
| flower_id | int NOT NULL | 花卉ID（FK → flower） |
| quantity | int NOT NULL | 数量 |
| unit_price | decimal(10,2) NOT NULL | 售价 |
| unit_cost | decimal(10,2) | 成本价 |
| amount | decimal(12,2) NOT NULL | 金额 |
| cost_amount | decimal(12,2) | 成本金额 |
| deleted | tinyint DEFAULT 0 | 逻辑删除 |

#### inventory（库存流水）

| 字段 | 类型 | 说明 |
|:---|:---|:---|
| id | int AUTO_INCREMENT | 主键 |
| biz_no | varchar(32) NOT NULL | 业务编号 |
| flower_id | int NOT NULL | 花卉ID（FK → flower） |
| biz_type | varchar(32) NOT NULL | 业务类型（PURCHASE_IN/SALE_OUT/MANUAL_ADJUST） |
| quantity | int NOT NULL | 变动数量 |
| before_stock | int NOT NULL | 变动前库存 |
| after_stock | int NOT NULL | 变动后库存 |
| unit_cost | decimal(10,2) | 单位成本 |
| total_cost | decimal(12,2) | 总成本 |
| remark | varchar(200) | 备注 |
| operator_id | int | 操作员ID（FK → user） |
| date | datetime NOT NULL | 操作时间 |
| deleted | tinyint DEFAULT 0 | 逻辑删除 |

#### event_outbox（本地消息表）

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

#### site_message（站内通知消息）

| 字段 | 类型 | 说明 |
|:---|:---|:---|
| id | bigint AUTO_INCREMENT | 主键 |
| biz_type | varchar(64) NOT NULL | 业务类型（如 APPOINTMENT_REMINDER） |
| biz_id | varchar(64) | 业务主键 |
| receiver_user_id | int | 接收人用户ID |
| title | varchar(128) NOT NULL | 标题 |
| content | varchar(512) NOT NULL | 正文 |
| is_read | tinyint(1) DEFAULT 0 | 0=未读 1=已读 |
| created_at | datetime NOT NULL | 消息生成时间 |

#### inventory_alert（库存预警读模型）

| 字段 | 类型 | 说明 |
|:---|:---|:---|
| id | int AUTO_INCREMENT | 主键 |
| flower_id | int NOT NULL UNIQUE | 花卉ID |
| flower_name_snapshot | varchar(64) NOT NULL | 花卉名称快照 |
| safe_stock | int NOT NULL | 安全库存阈值 |
| current_stock | int NOT NULL | 当前库存 |
| alert_status | varchar(16) DEFAULT 'NORMAL' | NORMAL/LOW_STOCK |
| last_trigger_time | datetime | 最近触发低库存时间 |
| last_recover_time | datetime | 最近恢复时间 |
| remark | varchar(256) | 备注 |

---

## 五、MQ 拓扑与事件契约

### 5.1 交换机与队列

| 交换机 | 类型 | 说明 |
|:---|:---|:---|
| `jasmine.appointment.event` | Topic | 预约域事件交换机 |
| `jasmine.audit.event` | Topic | 审计域事件交换机 |
| `jasmine.trade.event` | Topic | 交易域事件交换机 |
| `jasmine.dlx` | Topic | 统一死信交换机 |

| 队列 | 绑定交换机 | 路由键 | 说明 |
|:---|:---|:---|:---|
| `jasmine.appointment.notification` | appointment.event | `appointment.created` | 预约创建通知 |
| `jasmine.appointment.delay` | appointment.event | `appointment.delay` | 延时驻留队列（无消费者，靠TTL过期后弹射） |
| `jasmine.appointment.reminder` | appointment.event | `appointment.reminder` | 延时唤醒队列（消费者监听生成站内信） |
| `jasmine.audit.access-log` | audit.event | `audit.access-log` | 访问日志审计 |
| `jasmine.sales.event-log` | trade.event | `sales.created` | 销售事件 |
| `jasmine.inventory.event-log` | trade.event | `inventory.changed` | 库存变更事件 |
| `jasmine.dlq` | jasmine.dlx | `dead-letter.#` | 统一死信队列 |

### 5.2 消息体定义

| 事件类型 | 路由键 | 消息体 | 说明 |
|:---|:---|:---|:---|
| `appointment.created` | appointment.created | `AppointmentCreatedMessage` | id, vipId, date, content |
| `appointment.delay` | appointment.delay | `AppointmentCreatedMessage` + delay_ms | 延时投递，预约到店前1小时提醒 |
| `appointment.reminder` | appointment.reminder | `AppointmentCreatedMessage` | 死信弹射后的唤醒消息 |
| `audit.access-log` | audit.access-log | `AccessLogMessage` | userId, username, method, path, ip, timestamp |
| `sales.created` | sales.created | `SalesCreatedMessage` | salesId, orderNo, vipId, totalAmount, operatorId |
| `inventory.changed` | inventory.changed | `InventoryChangedMessage` | inventoryId, flowerId, bizType, quantity, beforeStock, afterStock, source |

### 5.3 消费者与下游

| 消费者 | 监听队列 | 下游动作 |
|:---|:---|:---|
| `AppointmentNotificationListener` | appointment.notification | 记录预约通知日志 |
| `AppointmentReminderListener` | appointment.reminder | 写入 `site_message` 站内信，按接收人批量落库 |
| `AccessLogAuditListener` | audit.access-log | 记录访问日志审计 |
| `SalesEventListener` | sales.event-log | 记录销售事件日志 |
| `InventoryEventListener` | inventory.event-log | 更新 `inventory_alert` 预警读模型 + 写站内信 |

### 5.4 Outbox 可靠投递流程

1. 主事务内调用 `OutboxService#save()` 写入 `event_outbox` 表
2. `OutboxRelay` 定时扫描 PENDING 记录，发送到 RabbitMQ
3. `ConfirmCallback`：broker 确认后条件更新 `PENDING → SENT`
4. `ReturnsCallback`：消息不可路由时标记 `FAILED`，写入 `lastError`
5. 失败记录按指数退避重试

---

## 六、安全架构

### 6.1 认证流程

1. 客户端 `POST /user/login` 提交 username + password
2. 后端校验账号状态（status=1 且 deleted=0）+ 密码匹配
3. 签发 Access Token（30分钟）+ Refresh Token（7天）
4. Access Token 放入响应 JSON，Refresh Token 写入 HttpOnly Cookie

### 6.2 JWT 验证链

```
Request → RequestTraceFilter（traceId） → JwtAuthenticationFilter → SecurityFilterChain
                                                    ↓
                                          解析 JWT Claims
                                                    ↓
                                          回查 User 状态（禁用则拒绝）
                                                    ↓
                                          装配 ROLE_xxx authority
                                                    ↓
                                          写入 SecurityContext
```

### 6.3 授权策略

| 路径模式 | 允许角色 |
|:---|:---|
| `/user/login`, `/user/refresh`, `/actuator/**`, `/swagger-ui/**` | 公开 |
| `/user/info`, `/user/logout`, `/user/changePassword`, `/site-message/**` | 已登录 |
| `/user/**`, `/role/**`, `/menu/**`, `/sys/**` | admin |
| `/vip/**`, `/appointment/**` | admin, Boss |
| `/flower/**`, `/sales/**`, `/inventory/**`, `/inventory-alert/**` | admin, Boss, clerk |
| 其他 | 拒绝 |

### 6.4 令牌存储策略（PR20 后）

| 令牌类型 | 存储位置 | 安全属性 |
|:---|:---|:---|
| Access Token | 前端 `sessionStorage` | 短生命周期，关闭标签页即清除 |
| Refresh Token | HttpOnly Cookie | Secure + SameSite=Lax，后端写入 |

---

## 七、缓存架构

### 7.1 缓存名称与策略

| 缓存名 | 对象 | TTL | 抖动 | 说明 |
|:---|:---|:---|:---|:---|
| `user` | User | 30 分钟 | 0-5 分钟 | 用户主数据，按 ID 缓存 |
| `menuList` | List\<Menu\> | 30 分钟 | 0-5 分钟 | 用户菜单树，按 userId 缓存 |
| `roleList` | List\<Role\> | 30 分钟 | 0-5 分钟 | 角色列表 |
| `flowerList` | List\<Flower\> | 10 分钟 | 0-2 分钟 | 花卉主数据列表 |
| `flowerDetail` | Flower | 10 分钟 | 0-2 分钟 | 花卉详情，按 ID 缓存 |

### 7.2 缓存环境切换

- 开发环境（`app.cache.type=memory`）：`ConcurrentMapCacheManager`
- 生产环境（`app.cache.type=redis`）：`RedisCacheManager` + `GenericJackson2JsonRedisSerializer`
- TTL 抖动机制：随机追加秒数，防止同批 key 集中过期

---

## 八、幂等与防重

### 8.1 请求级幂等

- 覆盖接口：`POST /sales`、`POST /inventory`、`POST /appointment`
- 机制：前端传 `X-Idempotency-Key` 请求头，后端 `RequestIdempotencyService` 基于 Redis 去重
- 默认防重窗口：30 秒

### 8.2 MQ 消费幂等

- `MqIdempotencyService`：基于 Redis 的消费幂等键，默认保留 24 小时
- 消费者端根据消息体中的业务主键构建幂等键，防止重复消费

---

## 九、前端架构

### 9.1 技术栈

| 技术 | 版本 | 说明 |
|:---|:---|:---|
| Vue | 3.5.32 | 前端框架 |
| Vue Router | 4.6.4 | 路由（Hash 模式） |
| Pinia | 3.0.4 | 状态管理 |
| Element Plus | 2.13.7 | 组件库 |
| Axios | 1.15.0 | HTTP 请求 |
| Vite | 8.0.8 | 构建工具 |
| Bun | 1.3.12 | 本地开发与依赖管理 |

### 9.2 页面与路由

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

### 9.3 路由守卫与动态菜单

- 登录后从 `/user/info` 获取用户信息与菜单树
- 前端根据菜单数据动态注册路由（`buildDynamicRoutes`）
- 刷新页面时通过 Refresh Token Cookie 自动恢复会话
- 全局消息中心悬浮窗：`AppLayout.vue`，聚合库存预警与预约待办

---

## 十、部署架构

### 10.1 容器化

- 后端：`Dockerfile`，基于 Eclipse Temurin JDK 21
- 前端：Nginx 托管 `dist/` 产物
- 中间件：MySQL 8.4 + Redis 7.2 + RabbitMQ 4.2

### 10.2 环境编排

- 开发环境：`ops/dev/docker-compose.yml`
- 生产环境：`ops/prod/docker-compose.yml`
- 环境变量：`ops/.env.example`

### 10.3 CI/CD

- GitHub Actions：`.github/workflows/docker-publish.yml`
- 镜像仓库：`ghcr.io/jipzeongit/jasmine-backend` / `jasmine-frontend`
- 标签策略：`<branch>-latest` + `<branch>-<short_sha>`

---

## 十一、配置项索引

| 配置项 | 默认值 | 说明 |
|:---|:---|:---|
| `server.port` | 9999 | 后端端口 |
| `spring.application.name` | jasmine | 应用名称 |
| `app.cache.type` | memory(dev) / redis(prod) | 缓存类型 |
| `app.security.jwt-secret` | 开发默认值 | JWT 签名密钥 |
| `app.security.jwt-expire-millis` | 1800000 | Access Token 有效期 |
| `app.security.jwt-refresh-expire-millis` | 604800000 | Refresh Token 有效期 |
| `app.mq.enabled` | false(dev) / true(prod) | MQ 启用开关 |
| `app.mq.dead-letter-enabled` | true | 死信交换机启用 |
| `app.mq.idempotency-ttl-hours` | 24 | 消费幂等键保留时间 |
| `app.mq.retry.max-attempts` | 3 | 消费重试最大次数 |
| `app.mq.retry.initial-interval-ms` | 1000 | 重试初始退避间隔 |
| `app.idempotency.create-ttl-seconds` | 30 | 创建类接口防重窗口 |
| `app.outbox.relay.interval-ms` | 5000 | Outbox Relay 扫描间隔 |
| `app.outbox.relay.batch-size` | 50 | Outbox Relay 每批处理量 |
