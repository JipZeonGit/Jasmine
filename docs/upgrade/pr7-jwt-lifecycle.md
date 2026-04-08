# PR7 JWT 生命周期补完

## 目标

PR7 聚焦把现有 Bearer Token 登录链路补成可长期维护的单体认证方案，范围收敛在以下三点：

- JWT 只保留最小 claims，不再把完整用户对象写入令牌。
- 引入 refresh token，支持续签 access token。
- 让 logout 真正生效，至少能让 refresh token 失效。

## 本轮改动

### 1. 精简 access token 内容

`JwtUtil` 现在只在 JWT 中写入必要字段：

- `uid`
- `subject(username)`
- `type`
- `jti`
- `iat`
- `exp`

这样可以避免把用户手机号、邮箱、头像等完整资料直接塞进令牌。

### 2. 新增 refresh token 持久化

新增 Flyway 脚本 [`V2__auth_refresh_token.sql`](/D:/Software%20Engineering/Code%20Library/IdeaProjects/Jasmine/src/main/resources/db/migration/V2__auth_refresh_token.sql)，创建 `auth_refresh_token` 表，用于记录：

- 用户 ID
- refresh token 对应的 `token_id`
- 过期时间
- 是否已撤销

当前策略是单体阶段优先求稳：登录会撤销当前用户的其他有效 refresh token，续签时会轮换 refresh token。

### 3. 登录返回双 token

`/user/login` 保持原有 `data.token` 不变，同时新增：

- `data.refreshToken`

这样前端旧逻辑不会被直接打断，但后续已经可以接入刷新机制。

### 4. 新增刷新接口

新增接口：`POST /user/refresh`

请求体：

```json
{
  "refreshToken": "..."
}
```

成功时返回新的 access token 和 refresh token；失败时返回 `20003`，提示重新登录。

### 5. logout 改为真正撤销 refresh token

`POST /user/logout` 现在会根据当前 access token 对应的用户，撤销该用户当前有效的 refresh token。

当前策略下：

- access token 仍然按短有效期自然过期
- refresh token 在 logout 后立即不可再用

### 6. 用户信息改为按用户 ID 回库补全

因为 access token 不再携带完整 `User`，`/user/info` 会根据 JWT 中的用户 ID 回库加载最新资料，再补角色和菜单。

## 测试策略

PR7 延续 PR6 的测试分层：

- 本机优先跑轻量测试
- 完整集成测试由 GitHub Actions 上的 Testcontainers 执行

本轮重点覆盖：

- 登录返回双 token
- refresh 成功续签
- logout 后 refresh 失效
- JWT access/refresh 类型校验

## 当前边界

PR7 只补完单体应用下的 JWT 生命周期，不引入以下能力：

- OAuth2 / 认证中心
- SSO
- 多终端复杂会话管理
- access token 黑名单体系

这些内容不属于当前阶段路线，后续如有需要再单独拆 PR。