# PR10 MySQL 8.4 正式收口

## 本轮目标

在项目已经运行于 MySQL 8.4 的基础上，正式完成数据库层的 8.4 收口与治理，避免继续沿用 5.7 时代遗留定义。

## 本轮内容

1. 将 `Jasmine.sql` 与 `V1__baseline.sql` 中的旧表结构统一收口到 MySQL 8.4 语义
2. 将主要业务表默认字符集与排序规则统一为 `utf8mb4` + `utf8mb4_0900_ai_ci`
3. 移除 `int(11)`、`int(1)`、`tinyint(1)` 等旧式 display width 定义
4. 新增 `V3__mysql84_hardening.sql`，通过 Flyway 对现有库执行正式治理迁移
5. 补充当前代码路径真实会用到的基础索引与唯一约束
6. 清理 `Jasmine.sql` 中遗留的 `user_role` 脏数据，使导库脚本与 Flyway baseline 保持一致
7. 明确开发/生产环境 JDBC 连接排序规则为 `utf8mb4_0900_ai_ci`
8. 为数据库唯一约束冲突补充统一异常映射，避免直接落成系统异常

## 重点收口项

- 字符集：`utf8` / `utf8mb3` -> `utf8mb4`
- 排序规则：`utf8_general_ci` / `utf8mb4_general_ci` -> `utf8mb4_0900_ai_ci`
- 逻辑删除字段：统一收口为 `tinyint`
- 关键唯一约束：
  - `user.username`
  - `vip.vid`
  - `vip.phone`
  - `inventory.num`
  - `user_role(user_id, role_id)`
  - `role_menu(role_id, menu_id)`
- 关键查询索引：预约、库存、菜单、销售、会员、用户、刷新令牌等主查询路径

## 本地验证

1. `./mvnw test -DskipITs=true` 通过
2. 本机 MySQL 8.4 开发库（`192.168.31.26:13306/jasmine`）已实际执行并应用 `V3__mysql84_hardening.sql`
3. Flyway 启动日志确认：
   - `Successfully validated 4 migrations`
   - `Successfully applied 1 migration to schema jasmine, now at version v3`
4. 迁移后再次启动应用，日志确认：
   - 当前 schema 版本已为 `3`
   - `Schema jasmine is up to date. No migration necessary.`
   - 应用可正常启动

## 说明

1. 本轮不是把数据库“切换”到 MySQL 8.4，而是把已经运行在 MySQL 8.4 上的现状正式治理完成
2. 这轮保持独立，不与业务模型重构混在一起
3. 当前 Flyway 版本在官方文档里对 MySQL 8.4 仍会提示支持验证不足，但本轮迁移链已经在本机 8.4 开发库上完成验证