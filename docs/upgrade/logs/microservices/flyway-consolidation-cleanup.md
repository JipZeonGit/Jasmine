# Flyway 收口确认与残留脚本清理

日期：2026-08-03

## 背景

架构核对时发现 `jasmine-iam/src/main/resources/db/migration/` 目录下同时存在
`V1__init.sql` 与 `V1__baseline.sql`（两个 V1 版本），若业务服务的 Flyway 被启用，
会直接触发 `Found more than one migration with version 1` 启动失败。
本轮通过文档与 git 历史完成根因调查，并清理残留。

## 历史调查结论（为什么会共存）

迁移执行方式经历过三次演变：

| 阶段 | 提交 | 迁移方式 |
|:---|:---|:---|
| 单体期 | `4bfae01`（pr5）~ pr11 | 单体唯一 `db/migration`，V1__baseline ~ V8，应用启动时 Flyway 自动执行 |
| Phase0 | `1304c2a` | Maven 拆分，单体 V1~V8 **整体搬入 jasmine-iam**（当时代码审查 P1-2 记录：迁移由 iam-service 单点承担）；`jasmine-schema` 作为 Flyway bootstrap 引入 compose |
| Phase4 | `88a43ac` | 数据库按服务拆分，每服务**新增** `V1__init.sql` 自带迁移。product/trade/crm 为新建目录所以干净；iam 的旧 V1~V8 **原地未删**，V1__init.sql 加入后两个 V1 开始共存 |
| 部署期 | `d25d1b3`、`c852720` | `jasmine-schema` 复活为多数据源迁移工具：`SchemaApplication` 用 Flyway 原生 API 对 4 个库分别执行按库分目录脚本。提交信息"旧单体迁移脚本全部删除"只清理了 schema 模块自身，**未触碰 jasmine-iam 内的残留** |

根因总结：

1. Phase4 只做了"新增"，没有删除 iam 里的单体残留脚本，且日志未记录该欠账
2. `d25d1b3`/`c852720` 两次改变迁移架构归属的重构**没有对应的 upgrade log**，文档盲区导致残留无人发现
3. Nacos `jasmine-db-common.yml` 将业务服务 `spring.flyway.enabled` 覆盖为 `false`，
   残留脚本从未真正执行，V1 版本冲突从未暴露

## 本轮清理

删除 `jasmine-iam/src/main/resources/db/migration/` 下的 8 个单体残留脚本：

- `V1__baseline.sql`（与 V1__init.sql 版本冲突的直接来源）
- `V2__auth_refresh_token.sql`
- `V3__mysql84_hardening.sql`
- `V4__business_model_rebuild.sql`
- `V5__cost_tracking_and_daily_summary.sql`
- `V6__event_outbox_and_inventory_alert.sql`
- `V7__site_message_and_outbox_delay.sql`
- `V8__site_message_receiver_scope.sql`

同时删除守护测试 `jasmine-iam/src/test/java/.../FlywayBaselineScriptTest.java`
（其断言对象 V1__baseline.sql 已删除，该校验内容已被 schema 模块按库脚本承接）。

## 清理后的迁移架构（双轨制）

| 环境 | 迁移执行方式 |
|:---|:---|
| 开发/生产运行时 | `jasmine-schema` 在部署阶段用 Flyway 原生 API 对 4 个库执行 `db/migration/{iam,product,trade,crm}` 分目录脚本；业务服务 flyway 由 Nacos `jasmine-db-common.yml` 覆盖为 `false` |
| 集成测试（Testcontainers） | `AbstractIntegrationTest` 动态开启 `spring.flyway.enabled=true`，业务服务 classpath 中的 `V1__init.sql` 自动建表 |

因此本轮**刻意保留**以下内容，不做激进删除：

- `jasmine-common` 的 `flyway-core` / `flyway-mysql` 依赖（IT 基础设施需要）
- 各业务服务 `application.yml` 的 `spring.flyway` 配置块（IT 动态属性覆盖依赖该配置路径存在）
- 各业务服务的 `V1__init.sql`（IT 建表数据源）

## 验证

- `jasmine-iam/db/migration` 仅剩 `V1__init.sql`，与 product/trade/crm 对齐
- 全模块编译通过
- 各模块集成测试（Testcontainers）通过，确认 V1__init.sql 建表链路完整

## 后续注意

- 新增表结构变更时：**生产侧**在 `jasmine-schema/db/migration/{库}/` 追加版本脚本；
  **IT 侧**同步更新对应服务的 `V1__init.sql`（两处需保持一致）
- 禁止再把脚本直接加到业务服务的 `db/migration` 根目录制造多版本链，该目录只允许唯一 `V1__init.sql`
