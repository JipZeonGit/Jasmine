# PR3 鉴权链路重构

这个 PR 聚焦在安全链路本身，把项目从 MVC 拦截器校验切换到 Spring Security 6 过滤器链。

## 本次范围

- `spring-security-core` 切换为 `spring-boot-starter-security`
- 引入 `SecurityFilterChain + JWT 过滤器`
- 后端兼容 `Authorization: Bearer <token>` 与旧的 `X-Token`
- 前端本轮继续保留 `X-Token`，避免一次性改动过大
- Springdoc 安全方案切换为 Bearer Token
- 测试环境的 Redis 缓存切到内存实现，避免依赖真实 Redis

## 说明

- 这一轮仍然保留项目当前的 JWT 单体模式
- `logout` 接口暂时继续保留原有实现
- `/user/info` 优先使用 Security 上下文中的登录用户，旧 token 解析逻辑作为兼容兜底