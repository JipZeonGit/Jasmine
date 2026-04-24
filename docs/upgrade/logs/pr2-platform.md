# PR2 底座升级

这个 PR 聚焦在后端底座升级本身，不调整现有业务接口设计，也不在这一轮强行重写整套鉴权流程。

## 本次范围

- Spring Boot 升级到 3.5.13
- Java 版本升级到 21
- MyBatis-Plus 升级到 Boot 3 对应 starter
- Springfox 替换为 springdoc
- `javax.servlet` / `javax.annotation` 迁移到 `jakarta`
- GitHub Actions 与 Dockerfile 同步切换到 JDK 21

## 说明

- 这一轮保留了项目现有的 JWT 拦截器方案，只做 Boot 3 兼容调整
- 完整的 Security 体系整理会放在后续阶段单独处理
- 文档注解已经切到 springdoc 对应的 OpenAPI 3 写法
