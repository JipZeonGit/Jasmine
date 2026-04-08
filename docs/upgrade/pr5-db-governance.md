# PR5 数据库治理与基础观测

## 本轮目标

这一轮的重点不是继续改业务接口，而是先把项目的数据库变更方式和运行观测底座补起来，并顺手把少量基础设施欠账收口，主要包括：

- 引入 Flyway，开始接管后续数据库迁移
- 基于现有 `Jasmine.sql` 整理一份可用于 Flyway 的 baseline 脚本
- 引入 Actuator，开放最基础的健康检查与应用信息端点
- 保持当前测试基线稳定，不因为 MySQL 方言脚本影响 H2 测试
- 把后端 CI 补到 `build + test + package`
- 把 JWT 密钥切到环境变量配置
- 补充项目功能清单与接口清单文档

## 主要改动

### 1. 接入 Flyway

项目新增了 Flyway 依赖，并在 `src/main/resources/application.yml` 中开启了基础配置：

- `spring.flyway.enabled=true`
- `spring.flyway.locations=classpath:db/migration`
- `spring.flyway.baseline-on-migrate=true`
- `spring.flyway.baseline-version=1`
- `spring.flyway.clean-disabled=true`

这意味着后续数据库结构变更应通过版本化 SQL 脚本进入仓库，而不是继续依赖手工改库。

### 2. 产出可用于 Flyway 的 baseline SQL

新增 baseline 脚本：`src/main/resources/db/migration/V1__baseline.sql`

这份脚本基于仓库根目录的 `Jasmine.sql` 清洗而来，做了几件明确的收口：

- 移除了建库与切库语句，避免 Flyway 在迁移阶段切换目标数据库
- 移除了 `DROP TABLE IF EXISTS`，避免 baseline 被误用于已有数据的数据库时造成破坏性删除
- 移除了 `SET FOREIGN_KEY_CHECKS`，让脚本职责更聚焦在结构与初始数据
- 清掉了 `user_role` 中引用不存在用户的两条脏数据

保留下来的 `Jasmine.sql` 仍然是历史快照；真正参与版本迁移的入口改为 `db/migration`。

### 3. 保持测试环境稳定

测试环境仍旧沿用 H2 初始化脚本，因此 `src/test/resources/application-test.yml` 中显式关闭了 Flyway：

- `spring.flyway.enabled=false`

这样做的原因很直接：当前 baseline SQL 仍然是以 MySQL 方言为准，如果强行让 H2 执行，反而会把这轮数据库治理和既有测试基线搅在一起。

### 4. 补基础观测入口

项目新增了 Actuator 依赖，并开放了两个最小但有价值的端点：

- `/actuator/health`
- `/actuator/info`

对应地，`src/main/java/com/nfu/jasmine/config/MySecurityConfig.java` 中已经放行这两个端点，方便本地联调和后续部署检查。

### 5. 补齐后端 CI 流程

`backend-ci.yml` 现在不再只跑测试，而是拆成了三步：

- 编译检查
- 自动化测试
- 打包产物

这样可以更接近后续真实交付链路，也能更早发现“测试通过但打包失败”这类问题。

### 6. JWT 密钥切换到环境变量

`JwtUtil` 不再内置固定密钥，而是改为读取配置：

- `app.security.jwt-secret`
- `app.security.jwt-expire-millis`

默认策略如下：

- `dev`：允许使用开发默认值，方便本地联调
- `test`：使用固定测试密钥，避免依赖外部环境变量
- `prod`：必须显式注入 `JWT_SECRET`，不提供默认值

### 7. 补功能清单与接口清单

本轮新增文档：`docs/upgrade/system-inventory.md`

这份文档主要用于沉淀：

- 当前升级主线与稳定分支关系
- 现有功能模块清单
- 当前后端接口清单
- 运行与配置入口
- 已知待后移的大项重构问题

## baseline 使用约定

### 已有非空开发库

对于已经存在表结构和数据的开发库，项目会通过 `baseline-on-migrate=true` 以版本 `1` 接入 Flyway。也就是说，Flyway 会把这类数据库视为已经处在 baseline 版本上，后续只执行 `V2`、`V3` 这样的增量迁移。

### 新建空库

对于全新的空数据库，Flyway 会从 `src/main/resources/db/migration/V1__baseline.sql` 开始初始化库结构和基础数据。

## 本轮验证

本轮除了保留既有自动化测试外，还新增了一条资源级静态校验测试：`src/test/java/com/nfu/jasmine/config/FlywayBaselineScriptTest.java`

这条测试主要确认 baseline SQL 中没有混入下列不应再出现的内容：

- `CREATE DATABASE`
- `DROP TABLE IF EXISTS`
- `SET FOREIGN_KEY_CHECKS`
- `USE \`jasmine\``
- 已识别出的两条 `user_role` 脏数据

## 本轮刻意未做的内容

为了控制风险，这一轮没有直接把 `Jasmine.sql` 拆成多个迁移版本，也没有顺手对现有库结构做大规模整形，例如：

- 没有直接给 `user.username` 补唯一索引
- 没有统一所有表的字符集
- 没有重做历史表设计
- 没有把 JWT 精简为必要 claims
- 没有补 refresh / logout 的完整失效策略

这些内容更适合作为 baseline 接入完成后的后续增量迁移或认证重构来处理。

## 本地运行提醒

由于这个项目之前出现过 `target` 目录残留旧 class 干扰运行的问题，本地联调后端时建议优先使用：

```powershell
./mvnw.cmd clean spring-boot:run
```

这样可以避免旧编译产物继续参与启动，减少“代码已经删了但运行时还在拦请求”这类问题。