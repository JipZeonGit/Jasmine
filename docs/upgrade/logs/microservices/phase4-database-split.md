# Phase4：数据库按服务拆分

日期：2026-05-20

## 目标

每个服务拥有独立数据库，不再共享单一 `jasmine` 库。各服务自带 Flyway 迁移脚本，启动时自动建表。

## 数据库拆分方案

| 服务 | 独立数据库 | 包含表 |
|:---|:---|:---|
| iam-service | `jasmine_iam` | user, role, user_role, menu, role_menu, auth_refresh_token |
| product-service | `jasmine_product` | flower |
| trade-service | `jasmine_trade` | sales, sales_item, inventory, inventory_alert, event_outbox |
| crm-service | `jasmine_crm` | vip, appointment, site_message, event_outbox |

`event_outbox` 表在 trade 和 crm 两个库中各有一份（结构相同），各服务独立管理自己的 Outbox Relay。

## 本轮改动

### 各服务 Flyway 迁移脚本

每个服务新增 `src/main/resources/db/migration/V1__init.sql`，从原 `jasmine-schema` 的 V1~V8 中提取该服务所属表的完整建表语句 + 索引 + 默认数据：

- `jasmine-iam/V1__init.sql`：user / role / user_role / menu / role_menu / auth_refresh_token + 默认 admin 用户 + 角色 + 菜单
- `jasmine-product/V1__init.sql`：flower + 4 条默认花卉数据
- `jasmine-trade/V1__init.sql`：sales / sales_item / inventory / inventory_alert / event_outbox + 初始化库存流水
- `jasmine-crm/V1__init.sql`：vip / appointment / site_message / event_outbox + 默认会员 + 预约数据

### 各服务 application.yml

所有服务开启 Flyway：

```yaml
spring:
  flyway:
    enabled: ${APP_FLYWAY_ENABLED:true}
    locations: classpath:db/migration
    baseline-on-migrate: true
    baseline-version: 0
    clean-disabled: true
```

### Nacos 配置

- `jasmine-db-common.yml`：数据源 URL 改为 `${MYSQL_DB_NAME:jasmine}` 变量化
- 各服务 Nacos 配置（`jasmine-iam.yml` / `jasmine-product.yml` / `jasmine-trade.yml` / `jasmine-crm.yml`）覆盖数据源 URL 为各自独立库名

### application-dev.yml

各服务 dev profile 数据源 URL 指向各自独立库：
- iam → `jasmine_iam`
- product → `jasmine_product`
- trade → `jasmine_trade`
- crm → `jasmine_crm`

### dev compose

- 新增 `ops/dev/init-databases.sql`：MySQL 容器首次启动时自动创建 4 个库并授权 `jasmine_app` 用户
- `ops/dev/docker-compose.yml` 挂载 init 脚本到 `/docker-entrypoint-initdb.d/01-init-databases.sql`

### jasmine-schema 模块

保留为遗留参考，不再承担运行时迁移职责。各服务自带迁移后，schema 模块的 V1~V8 仅作为历史记录。

## 设计决策

- **每服务一份 V1__init.sql 而非拆分 V1~V8**：新环境只需跑一个脚本即可建好全部表，避免多版本迁移的复杂度。
- **`event_outbox` 表在 trade 和 crm 各一份**：各服务独立管理自己的 Outbox Relay，不跨库操作。
- **`baseline-version: 0`**：Flyway 从 V1 开始执行，不跳过任何迁移。
- **保留 `jasmine-schema` 模块**：不删除，作为历史参考和可能的全量初始化工具。

## 验证

- 全模块 `compile` 通过（9 模块 BUILD SUCCESS）
- 各服务 Flyway 迁移脚本包含完整建表 + 索引 + 默认数据
- dev compose MySQL 容器首次启动时自动创建 4 个独立库

## 后续注意

- 跨服务数据一致性采用事件驱动最终一致性（trade-service 的 `inventory.changed` 通过 MQ 通知 crm-service）
- 共享数据查询通过 Phase3 的内部接口完成，不跨库 JOIN
- 如需全量重建环境，先起 dev compose（自动建库），再逐个启动服务（Flyway 自动建表）
