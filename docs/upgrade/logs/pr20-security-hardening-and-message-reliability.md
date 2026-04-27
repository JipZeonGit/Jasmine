# PR20 安全加固与消息隔离修复记录

## 目标

本轮根据 `docs/upgrade/review/next-code-review-2026-04-27.md` 中的审查结论，集中修复以下问题：

- 登录后无细粒度授权，任意账号可访问后台管理接口
- 站内信没有接收人维度，未读状态在多账号之间串扰
- Outbox 在消息不可路由时可能被误标为已发送
- 禁用账号仍可登录、仍可刷新令牌
- 菜单装配存在递归 N+1 查询
- JWT 失败响应未设置真实 HTTP 状态码
- 前端将长生命周期令牌放在 `localStorage`
- `InventoryAlertQueryDTO` 的 `@Builder` 默认值失效

## 本次改动

### 1. 补齐认证后的授权链路

涉及文件：

- `src/main/java/com/nfu/jasmine/config/MySecurityConfig.java`
- `src/main/java/com/nfu/jasmine/infra/security/filter/JwtAuthenticationFilter.java`
- `src/main/java/com/nfu/jasmine/iam/application/impl/UserServiceImpl.java`
- `src/main/java/com/nfu/jasmine/infra/security/CurrentUserProvider.java`

本轮处理：

- 在 `MySecurityConfig` 中按接口分组补充路径级授权：
  - `/user/**`、`/role/**`、`/menu/**`、`/sys/**` 仅 `admin`
  - `/vip/**`、`/appointment/**` 允许 `admin` / `Boss`
  - `/flower/**`、`/sales/**`、`/inventory/**`、`/inventory-alert/**` 允许 `admin` / `Boss` / `clerk`
  - `/site-message/**`、`/user/info`、`/user/logout`、`/user/changePassword` 保持已登录可用
- `JwtAuthenticationFilter` 不再写入空 authority，而是实时装配 `ROLE_xxx`
- 新增 `CurrentUserProvider`，统一获取当前登录用户，去掉控制器里重复的 `request + SecurityContext` 解析逻辑
- JWT 校验失败时显式返回 `401`
- 访问受限资源时 `JwtAccessDeniedHandler` 显式返回 `403`

### 2. 禁用账号立即失效

涉及文件：

- `src/main/java/com/nfu/jasmine/iam/application/impl/UserServiceImpl.java`

本轮处理：

- 登录时新增账号状态校验，禁用账号无法登录
- 刷新令牌时新增账号状态校验，禁用账号无法续期
- JWT 每次解析后都会回查当前账号状态，避免“旧 access token 持续可用”
- 用户状态被改成非启用时，主动撤销该账号所有 refresh token

### 3. 改造 refresh token 存储方式

涉及文件：

- `src/main/java/com/nfu/jasmine/iam/web/UserController.java`
- `src/main/resources/application.yml`
- `src/main/resources/application-prod.yml`
- `web/src/utils/auth.ts`
- `web/src/utils/request.ts`
- `web/src/api/auth.ts`
- `web/src/stores/auth.ts`
- `web/src/router/index.ts`
- `web/src/views/profile/ProfileView.vue`
- `web/src/types/index.ts`

本轮处理：

- 后端登录/刷新成功后把 refresh token 写入 `HttpOnly Cookie`
- 前端不再把 refresh token 放进浏览器可读存储
- access token 从 `localStorage` 迁移到 `sessionStorage`
- 路由守卫新增“无 access token 时尝试用 refresh cookie 恢复会话”
- Axios 统一开启 `withCredentials`
- 个人中心改为只提交旧密码和新密码，后端从当前登录态识别用户，不再信任前端传来的用户名

### 4. 站内信改为按用户隔离

涉及文件：

- `src/main/resources/db/migration/V8__site_message_receiver_scope.sql`
- `src/main/java/com/nfu/jasmine/infra/notification/model/entity/SiteMessage.java`
- `src/main/java/com/nfu/jasmine/infra/notification/service/SiteMessageService.java`
- `src/main/java/com/nfu/jasmine/infra/notification/web/SiteMessageController.java`
- `src/main/java/com/nfu/jasmine/infra/mq/listener/AppointmentReminderListener.java`
- `src/main/java/com/nfu/jasmine/iam/persistence/mapper/UserMapper.java`
- `src/main/resources/mapper/sys/UserMapper.xml`

本轮处理：

- `site_message` 新增 `receiver_user_id`
- 未读数、分页列表、单条已读、全部已读全部按当前用户隔离
- “全部已读”改成单 SQL 批量更新，不再逐条循环写库
- 预约提醒消费端改为按接收人批量落库，当前默认投递给激活状态的 `admin` / `Boss` / `clerk`

### 5. 修复 Outbox 不可路由误判成功

涉及文件：

- `src/main/java/com/nfu/jasmine/infra/mq/config/RabbitMqTopologyConfig.java`
- `src/main/java/com/nfu/jasmine/infra/outbox/relay/OutboxRelay.java`

本轮处理：

- Relay 发送消息时额外写入 `x-outbox-id` header
- `ConfirmCallback` 改成条件更新，只允许 `PENDING -> SENT`
- `ReturnsCallback` 捕获不可路由消息后，按 `outboxId` 直接反写 `FAILED`
- 对 `confirm` / `return` 回调先后顺序做幂等兼容，确保“不可路由优先失败，不会再被覆盖成成功”

### 6. 消除菜单递归 N+1

涉及文件：

- `src/main/java/com/nfu/jasmine/iam/application/impl/MenuServiceImpl.java`
- `src/main/java/com/nfu/jasmine/iam/persistence/mapper/MenuMapper.java`
- `src/main/resources/mapper/sys/MenuMapper.xml`

本轮处理：

- 新增 `getAllMenusByUserId`
- 菜单改成“一次查全 + 内存组树”
- 删除按节点递归二次查询数据库的实现

### 7. 补齐低优先级问题

涉及文件：

- `src/main/java/com/nfu/jasmine/inventory/alert/web/dto/InventoryAlertQueryDTO.java`
- `src/test/java/com/nfu/jasmine/appointment/web/AppointmentControllerTest.java`
- `src/test/java/com/nfu/jasmine/iam/web/UserControllerSecurityIT.java`

本轮处理：

- `InventoryAlertQueryDTO` 为分页默认值补上 `@Builder.Default`
- 单测适配新的 `CurrentUserProvider`
- 安全集成测试同步适配 cookie 刷新协议与权限收紧后的行为

## 验证

### 后端单元测试

执行：

```powershell
$env:JAVA_HOME='D:\Software Engineering\Dependence\JDK\jdk-21.0.10'
$env:PATH="$env:JAVA_HOME\bin;$env:PATH"
./mvnw test -DskipITs=true
```

结果：

- `Tests run: 14, Failures: 0, Errors: 0, Skipped: 0`

### 前端生产构建

执行：

```powershell
cd web
bun run build
```

结果：

- `vue-tsc` 通过
- `vite build` 通过

### 后端集成测试探测

执行：

```powershell
$env:JAVA_HOME='D:\Software Engineering\Dependence\JDK\jdk-21.0.10'
$env:PATH="$env:JAVA_HOME\bin;$env:PATH"
./mvnw verify -DskipUTs=true
```

结果：

- 构建成功
- Failsafe 启动成功
- 当前环境缺少可用 Docker，Testcontainers 相关集成测试按预期跳过
- 汇总为 `Tests run: 23, Failures: 0, Errors: 0, Skipped: 23`

## 当前状态

- 修复代码已完成
- 已提交
- 已推送

## 说明

- 本轮没有修改 `docs/upgrade/review/next-code-review-2026-04-27.md` 的原始审查结论，只新增实现日志用于记录修复落地情况
- 站内信新增了 `receiver_user_id` 维度，历史旧消息因没有接收人信息，不会再出现在当前用户未读列表中
- refresh token 已迁移到 `HttpOnly Cookie`，如后续接入独立域名前后端部署，需要继续结合真实域名评估 `SameSite` 与 `Secure` 配置

## CI 集成测试修复

### 问题描述

GitHub Actions 集成测试（`./mvnw -B verify -DskipUTs=true`）出现 2 个失败：

1. `UserControllerSecurityIT.refreshTokenShouldReturnNewTokenPair` — 断言 `$.code` 期望 `20000` 实际 `20003`
2. `AppointmentReminderIntegrationIT.shouldRelayDelayedMessageAndCreateSiteMessage` — `TooManyResultsException: selectOne() 返回了 2 条`

### 根因分析

**问题 1：refresh token cookie 在 MockMvc 中无法被后端解析**

测试通过 `header("Cookie", "jasmine_refresh_token=xxx")` 传递 cookie。但 Spring MockMvc 的 `header("Cookie", ...)` **不会** 填充到 `HttpServletRequest.getCookies()` 数组中。后端 `UserController.resolveRefreshToken` 依赖 `request.getCookies()` 遍历获取 refresh token，因此永远读到 null，最终返回 20003（令牌无效）。

**问题 2：站内信按用户隔离后，预约提醒会为每个符合角色的用户各生成一条**

PR20 将站内信改为按用户批量落库（`createForUsers`），预约提醒消费端会查询所有激活状态的 `admin` / `Boss` / `clerk` 用户。V1 baseline 中已有 2 个 admin 角色用户（id=1 和 id=2），导致 `createForUsers` 产生 2 条站内信。测试中使用 `selectOne` 查询 `bizType = APPOINTMENT_REMINDER`，命中 2 条数据，MyBatis 抛出 `TooManyResultsException`。

### 修复方案

**`UserControllerSecurityIT.java`**：

- 将 `header("Cookie", ...)` 改为 `MockMvcRequestBuilders.cookie(new Cookie(...))` 方式传递 cookie，使 `HttpServletRequest.getCookies()` 能正确返回 cookie 数组
- 相应调整 `extractCookiePair` 辅助方法为 `extractCookieNameValue`，返回 `name` 和 `value` 拆分结果

**`AppointmentReminderIntegrationIT.java`**：

- 将 `selectOne` 改为 `selectList`，断言至少有一条即可，取第一条做内容验证

