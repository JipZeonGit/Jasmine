# PR6 测试基线现代化

## 目标
- 把关键集成测试从 H2 迁移到 Testcontainers。
- 使用真实 MySQL 8.4 与 Redis 7.x 验证启动、鉴权和核心数据库链路。
- 保持本机开发轻量：默认 `mvn test` 只跑快速单元测试，完整容器测试交给 GitHub Actions 的 `verify` 阶段。

## 这轮做了什么
- 新增 `AbstractIntegrationTest`，统一托管 MySQL 8.4 与 Redis 7.x 容器。
- 将应用启动、登录服务、用户控制器安全测试迁移为 `*IT`，由 Maven Failsafe 在 `verify` 阶段执行。
- 保留 `JwtUtilTest`、`FlywayBaselineScriptTest`、`MembershipIdTest` 为本机可快速执行的轻量测试。
- CI 改为两段式：
  - `test`：快速单元测试
  - `verify`：Testcontainers 集成测试 + 打包

## 本机开发约定
- 日常开发默认运行：`./mvnw test`
- 如果本机没有 Docker 运行时，不强制执行 `*IT`。
- 需要完整验证时，优先查看 GitHub Actions 的 `verify` 结果。

## CI 期望
- GitHub Actions 运行在 `ubuntu-latest`，可直接使用 Docker。
- `verify` 阶段会启动临时 MySQL/Redis 容器，并由 Flyway 初始化数据库结构。
- 集成测试完成后，容器自动销毁，不依赖 NAS 上的长期测试环境。

## 后续衔接
- PR7 继续处理 JWT 生命周期与 refresh/logout 策略。
- PR8 继续把其他控制器的 DTO/VO 与统一返回结构补齐。