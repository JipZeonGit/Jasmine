# PR1 基线整理

这个 PR 的目标是先把 `next` 升级分支的底座整理好，为后续 Boot 3 和 JDK 21 升级提供一个更稳定的出发点。

## 本次范围

- 将运行配置拆分为 `dev`、`test`、`prod` 三套环境
- 共同配置保留在 `application.yml`，环境差异配置分开管理
- 测试改为使用内存 H2，不再依赖当前真实 MySQL
- 新增一条后端基础 CI 流程，用于 PR 校验

## 关键调整

- `src/main/resources/application.yml`
  只保留项目共用配置，并将默认环境设置为 `dev`
- `src/main/resources/application-dev.yml`
  保留当前本地开发所需的 MySQL / Redis 默认值
- `src/main/resources/application-prod.yml`
  改为从环境变量读取生产配置，避免生产误用开发配置
- `src/test/resources/application-test.yml`
  测试启动时使用 H2，并自动初始化测试表结构和测试数据
- `src/test/resources/schema-test.sql` / `data-test.sql`
  提供测试所需的最小表结构和初始化数据

## 为什么先做这一步

在 Boot 3 升级之前，先把配置和测试基线整理好，后面无论是升依赖、改 Security 还是换 springdoc，都会更安全。
