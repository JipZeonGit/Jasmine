# Code Review Report

## 1. Executive Summary
- 总体评价：存在风险
- 核心问题概述（最关键的 3 点）
  - 权限模型目前只有“已登录”校验，没有“已授权”校验，任意登录账号都可以直接调用用户、角色、库存、销售等管理接口，属于系统级越权风险。
  - 站内信模型没有接收人维度，未读数、列表、已读操作都是全局共享，存在跨账号消息泄露和已读状态串扰。
  - MQ Outbox 在“消息不可路由”场景下仍可能被标记为 `SENT`，会把实际未送达的业务事件静默记成成功，破坏可靠投递闭环。

## 2. Critical Issues（P0 - 必须修复）
> 会导致系统错误 / 安全风险

### [P0] 只有认证，没有授权，任意登录用户都能操作后台管理接口
- 影响范围：
  - `src/main/java/com/nfu/jasmine/config/MySecurityConfig.java`
  - `src/main/java/com/nfu/jasmine/infra/security/filter/JwtAuthenticationFilter.java`
  - `src/main/java/com/nfu/jasmine/iam/web/UserController.java`
- 问题说明：
  - 安全链只要求 `.anyRequest().authenticated()`，没有任何按角色/权限的接口约束。
  - `JwtAuthenticationFilter` 创建认证对象时把权限集合写成了 `Collections.emptyList()`，意味着 Spring Security 根本没有可用于授权决策的 authority。
  - 代码库里也没有 `@PreAuthorize` / `@Secured` 之类的方法级授权，因此只要拿到一个合法 JWT，就可以调用 `/user`、`/role`、`/inventory`、`/sales` 等管理接口。
- 代码片段：
```java
// src/main/java/com/nfu/jasmine/config/MySecurityConfig.java
.authorizeHttpRequests(auth -> auth
        // ...
        .anyRequest().authenticated()
)
```

```java
// src/main/java/com/nfu/jasmine/infra/security/filter/JwtAuthenticationFilter.java
UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
        loginUser,
        null,
        Collections.emptyList()
);
```

```java
// src/main/java/com/nfu/jasmine/iam/web/UserController.java
@PostMapping("")
public Result<?> addUser(@Valid @RequestBody UserCreateDTO userDTO) { ... }

@DeleteMapping("/{id}")
public Result<?> deleteUserById(@PathVariable("id") Integer id) { ... }
```
- 风险：
  - 权限绕过
  - 越权增删改查
  - 审计失真
- 修改建议：
  - 先把“认证用户”升级成“带权限的认证用户”。
  - 敏感接口必须加方法级授权，至少先按角色挡住用户管理、角色管理、库存/销售写操作。
- 改后代码示例：
```java
// JwtAuthenticationFilter.java
User currentUser = userService.getUserById(claims.getUserId());
if (currentUser == null || !Integer.valueOf(1).equals(currentUser.getStatus())) {
    throw new JwtException("账号已禁用或不存在");
}

List<GrantedAuthority> authorities = roleService.getRoleNamesByUserId(currentUser.getId()).stream()
        .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
        .toList();

UsernamePasswordAuthenticationToken authentication =
        new UsernamePasswordAuthenticationToken(currentUser, null, authorities);
```

```java
// UserController.java
@PreAuthorize("hasRole('admin')")
@PostMapping("")
public Result<?> addUser(@Valid @RequestBody UserCreateDTO userDTO) { ... }

@PreAuthorize("hasRole('admin')")
@DeleteMapping("/{id}")
public Result<?> deleteUserById(@PathVariable("id") Integer id) { ... }
```

## 3. Major Issues（P1 - 高优先级）
> 影响性能或可维护性

### [P1] 站内信没有接收人维度，导致消息泄露和已读状态串扰
- 影响范围：
  - `src/main/resources/db/migration/V7__site_message_and_outbox_delay.sql`
  - `src/main/java/com/nfu/jasmine/infra/notification/model/entity/SiteMessage.java`
  - `src/main/java/com/nfu/jasmine/infra/notification/service/SiteMessageService.java`
  - `src/main/java/com/nfu/jasmine/infra/notification/web/SiteMessageController.java`
- 问题说明：
  - `site_message` 表只有业务字段和 `is_read`，没有 `receiver_user_id`、`receiver_role` 或单独的已读关系表。
  - 现在 `getUnreadCount()` / `pageMessages()` / `markAsRead()` / `markAllAsRead()` 都是全局操作。一个店员查看或已读消息，会直接影响其他店员的消息中心。
- 代码片段：
```sql
-- src/main/resources/db/migration/V7__site_message_and_outbox_delay.sql
CREATE TABLE `site_message` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `biz_type` varchar(64) NOT NULL,
  `biz_id` varchar(64) DEFAULT NULL,
  `title` varchar(128) NOT NULL,
  `content` varchar(512) NOT NULL,
  `is_read` tinyint(1) NOT NULL DEFAULT 0,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP
)
```

```java
// src/main/java/com/nfu/jasmine/infra/notification/service/SiteMessageService.java
public long getUnreadCount() {
    return siteMessageMapper.selectCount(
            new LambdaQueryWrapper<SiteMessage>().eq(SiteMessage::getIsRead, 0)
    );
}

public void markAllAsRead() {
    List<SiteMessage> unreads = siteMessageMapper.selectList(
            new LambdaQueryWrapper<SiteMessage>().eq(SiteMessage::getIsRead, 0)
    );
    for (SiteMessage msg : unreads) {
        msg.setIsRead(1);
        siteMessageMapper.updateById(msg);
    }
}
```
- 风险：
  - 敏感信息泄露
  - 多用户状态互相污染
  - 后续无法扩展“按门店 / 按岗位 / 按个人”投递
- 修改建议：
  - 如果消息是“发给某个人”，加 `receiver_user_id`。
  - 如果消息是“广播给多人”，拆成 `site_message` + `site_message_read` 两张表，已读状态按用户记录。
  - 控制器所有查询/已读动作都要带上当前用户维度。
- 改后代码示例：
```sql
ALTER TABLE `site_message`
ADD COLUMN `receiver_user_id` int NOT NULL,
ADD KEY `idx_site_message_receiver_read` (`receiver_user_id`, `is_read`, `created_at`);
```

```java
public long getUnreadCount(Integer currentUserId) {
    return siteMessageMapper.selectCount(
            new LambdaQueryWrapper<SiteMessage>()
                    .eq(SiteMessage::getReceiverUserId, currentUserId)
                    .eq(SiteMessage::getIsRead, 0)
    );
}
```

```java
public void markAllAsRead(Integer currentUserId) {
    siteMessageMapper.update(
            null,
            new LambdaUpdateWrapper<SiteMessage>()
                    .eq(SiteMessage::getReceiverUserId, currentUserId)
                    .eq(SiteMessage::getIsRead, 0)
                    .set(SiteMessage::getIsRead, 1)
    );
}
```

### [P1] Outbox 对“不可路由消息”没有失败闭环，可能误标 `SENT`
- 影响范围：
  - `src/main/java/com/nfu/jasmine/infra/mq/config/RabbitMqTopologyConfig.java`
- 问题说明：
  - 现在 `ConfirmCallback` 只要收到 broker `ack=true` 就把 Outbox 置为 `SENT`。
  - 但 `mandatory=true` 下，消息如果成功到达 exchange、却没有任何 queue 绑定，也会先收到 return，再收到 confirm ack。当前 `ReturnsCallback` 只记日志，不反写 Outbox。
  - 结果就是“消息没有真正进入业务队列，但数据库已经显示 SENT”，这是典型的静默丢事件。
- 代码片段：
```java
// src/main/java/com/nfu/jasmine/infra/mq/config/RabbitMqTopologyConfig.java
rabbitTemplate.setConfirmCallback((correlationData, ack, cause) -> {
    if (ack) {
        EventOutbox outbox = new EventOutbox();
        outbox.setId(outboxId);
        outbox.setStatus(OutboxStatus.SENT.name());
        outbox.setSentAt(new java.util.Date());
        outboxMapper.updateById(outbox);
    }
});

rabbitTemplate.setReturnsCallback(returned -> log.error(
        "MQ 消息路由失败 exchange={} routingKey={} replyCode={} replyText={}",
        returned.getExchange(),
        returned.getRoutingKey(),
        returned.getReplyCode(),
        returned.getReplyText()
));
```
- 风险：
  - 业务事件静默丢失
  - 站内信、库存预警、审计日志与主业务状态脱节
- 修改建议：
  - 把 `outboxId` 放进 message header。
  - `ReturnsCallback` 必须把不可路由事件标记为 `FAILED` 或 `PENDING_RETRY`。
  - `ConfirmCallback` 不能无条件 `SENT`，要确认没有 return 失败标记后再落成功状态。
- 改后代码示例：
```java
// OutboxRelay.java 发送前补 header
Message message = MessageBuilder
        .withBody(outbox.getPayload().getBytes(StandardCharsets.UTF_8))
        .setContentType(MessageProperties.CONTENT_TYPE_JSON)
        .setHeader("x-outbox-id", outbox.getId())
        .build();
```

```java
// RabbitMqTopologyConfig.java
rabbitTemplate.setReturnsCallback(returned -> {
    Object rawId = returned.getMessage().getMessageProperties().getHeaders().get("x-outbox-id");
    if (rawId != null) {
        Long outboxId = Long.valueOf(rawId.toString());
        EventOutbox failed = new EventOutbox();
        failed.setId(outboxId);
        failed.setStatus(OutboxStatus.FAILED.name());
        failed.setLastError("UNROUTABLE: " + returned.getReplyText());
        eventOutboxMapper.updateById(failed);
    }
});
```

### [P1] 禁用用户仍可登录，已禁用账号也能继续刷新令牌
- 影响范围：
  - `src/main/java/com/nfu/jasmine/iam/application/impl/UserServiceImpl.java`
- 问题说明：
  - `login()` 只校验用户名和密码，没有校验 `status`。
  - `refreshToken()` 只校验用户是否删除，没有校验 `status`。
  - 这意味着后台把账号状态改成“禁用”后，用户仍然可以继续登录，已有 refresh token 也还能续期。
- 代码片段：
```java
// src/main/java/com/nfu/jasmine/iam/application/impl/UserServiceImpl.java
User loginUser = this.baseMapper.selectOne(wrapper);
if (loginUser == null || !passwordEncoder.matches(loginDTO.getPassword(), loginUser.getPassword())) {
    return null;
}
```

```java
// src/main/java/com/nfu/jasmine/iam/application/impl/UserServiceImpl.java
User user = this.baseMapper.selectById(claims.getUserId());
if (user == null || Integer.valueOf(1).equals(user.getDeleted())) {
    return null;
}
```
- 风险：
  - 封禁策略失效
  - 离职账号、临时禁用账号仍可访问系统
- 修改建议：
  - 登录、刷新、JWT 解析后都要检查 `status == 1`。
  - 用户被禁用时应同步废弃其 refresh token。
- 改后代码示例：
```java
private boolean isUserActive(User user) {
    return user != null
            && !Integer.valueOf(1).equals(user.getDeleted())
            && Integer.valueOf(1).equals(user.getStatus());
}

public LoginVO login(LoginDTO loginDTO) {
    User loginUser = this.baseMapper.selectOne(wrapper);
    if (!isUserActive(loginUser) || !passwordEncoder.matches(loginDTO.getPassword(), loginUser.getPassword())) {
        log.warn("用户登录失败或账号已禁用 username={}", loginDTO.getUsername());
        return null;
    }
    revokeActiveRefreshTokens(loginUser.getId());
    return issueTokenPair(loginUser);
}
```

### [P1] 菜单装配存在递归 N+1 查询，菜单树一大就会明显拖慢登录和鉴权
- 影响范围：
  - `src/main/java/com/nfu/jasmine/iam/application/impl/MenuServiceImpl.java`
  - `src/main/resources/mapper/sys/MenuMapper.xml`
- 问题说明：
  - `getMenuListByUserId()` 先查一级菜单，再对每个节点递归调用 `getMenuListByUserId(userId, parentId)`。
  - 这个 mapper 本身已经用了递归 CTE，每次再递归调用一次，相当于“递归 SQL + Java 递归”叠加，查询次数随节点数线性放大。
- 代码片段：
```java
// src/main/java/com/nfu/jasmine/iam/application/impl/MenuServiceImpl.java
private void setMenuChildrenByUserId(Integer userId, List<Menu> menuList) {
    if(menuList != null){
        for(Menu menu : menuList){
            List<Menu> subMenuList = this.baseMapper.getMenuListByUserId(userId,menu.getMenuId());
            menu.setChildren(subMenuList);
            setMenuChildrenByUserId(userId,subMenuList);
        }
    }
}
```
- 风险：
  - 登录后首次拉菜单慢
  - 菜单节点增多后数据库压力持续放大
- 修改建议：
  - 一次性把用户可见菜单全量查出，再在内存里组树。
- 改后代码示例：
```java
List<Menu> grantedMenus = this.baseMapper.getAllMenusByUserId(userId);
Map<Integer, List<Menu>> childrenMap = grantedMenus.stream()
        .collect(Collectors.groupingBy(menu -> Optional.ofNullable(menu.getParentId()).orElse(0)));

List<Menu> roots = childrenMap.getOrDefault(0, List.of());
buildTree(roots, childrenMap);
return roots;
```

## 4. Minor Issues（P2 - 建议优化）
> 代码风格或小问题

### [P2] JWT 认证失败时没有设置真实 HTTP 401 状态码
- 影响范围：
  - `src/main/java/com/nfu/jasmine/infra/security/filter/JwtAuthenticationFilter.java`
- 问题说明：
  - 注释写的是“返回 401”，但实际只写了 JSON body，没有 `response.setStatus(HttpServletResponse.SC_UNAUTHORIZED)`。
  - 前端虽然靠业务码能兜住，但网关、日志平台、APM、CDN、反向代理都会把这类请求当成 HTTP 200。
- 代码片段：
```java
response.setContentType("application/json;charset=utf-8");
Result<Object> fail = Result.fail(ResultCode.UNAUTHORIZED, "JWT无效，请重新登录！");
response.getWriter().write(JSON.toJSONString(fail));
```
- 改后代码示例：
```java
response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
response.setContentType("application/json;charset=utf-8");
response.getWriter().write(JSON.toJSONString(
        Result.fail(ResultCode.UNAUTHORIZED, "JWT无效，请重新登录！")
));
```

### [P2] 前端把 access token 和 refresh token 都放在 `localStorage`，XSS 代价被放大
- 影响范围：
  - `web/src/utils/auth.ts`
  - `web/src/utils/request.ts`
- 问题说明：
  - 当前代码把两类令牌都持久化到 `localStorage`，只要未来任何页面引入 XSS，攻击者就能直接读出长期有效凭证。
  - 目前代码里没有直接看到 `v-html` 之类的危险输出点，但这个存储策略会把“一个前端缺陷”放大成“账户接管”。
- 代码片段：
```ts
// web/src/utils/auth.ts
export function getToken() {
  return localStorage.getItem(TOKEN_KEY) || ''
}

export function getRefreshToken() {
  return localStorage.getItem(REFRESH_TOKEN_KEY) || ''
}
```
- 修改建议：
  - 更稳妥的方案是 `refresh token` 放 `HttpOnly + Secure + SameSite` Cookie，`access token` 放内存。
  - 如果短期不改协议，至少先把 refresh token 从 `localStorage` 移出。

### [P2] `InventoryAlertQueryDTO` 的 `@Builder` 默认值当前不会生效
- 影响范围：
  - `src/main/java/com/nfu/jasmine/inventory/alert/web/dto/InventoryAlertQueryDTO.java`
- 问题说明：
  - 编译阶段已经给出 Lombok 警告：`@Builder` 会忽略字段初始化表达式。
  - 也就是说，一旦后续代码通过 `InventoryAlertQueryDTO.builder().build()` 创建对象，`pageNo/pageSize` 不会自动拿到 `1/10`，而会变成 `null`，容易把分页逻辑带成隐藏空指针或默认值漂移问题。
- 代码片段：
```java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryAlertQueryDTO {
    private Integer pageNo = 1;
    private Integer pageSize = 10;
    private String flowerName;
    private String alertStatus;
}
```
- 改后代码示例：
```java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryAlertQueryDTO {
    @Builder.Default
    private Integer pageNo = 1;

    @Builder.Default
    private Integer pageSize = 10;

    private String flowerName;
    private String alertStatus;
}
```

## 5. Security Analysis
- SQL Injection：
  - 目前未发现直接字符串拼接 SQL 的实现，主流查询都走 MyBatis-Plus Wrapper 或 mapper 参数绑定，风险较低。
- XSS：
  - 当前前端未发现明显的 `v-html` / `innerHTML` 直出点。
  - 但令牌持久化在 `localStorage`，一旦未来出现 XSS，攻击影响会被显著放大。
- CSRF：
  - 当前主要使用 `Authorization: Bearer` 头，不依赖 Cookie，会比 Cookie Session 模式更安全一些。
  - 但后端 CORS 已开启 `allowCredentials(true)`，若后续切到 Cookie 登录，需要重新补 CSRF 设计。
- 权限绕过：
  - 存在，且是当前最严重问题。表现为“已登录即可全后台读写”。
- 敏感信息泄露：
  - 站内信目前是全局共享模型，存在跨账号可见和跨账号改读状态的问题。
- 缓存雪崩、穿透、击穿：
  - Redis 缓存侧已经有 TTL 抖动，说明项目有意识在做防雪崩。
  - 但站内信、菜单树、用户权限这些链路仍主要依赖数据库直查，没有额外防热点读放大设计。
- 修复建议：
  - 第一优先级先补授权模型。
  - 第二优先级重构站内信接收人与已读模型。
  - 第三优先级收紧令牌存储方式与账户状态校验。

## 6. Performance Analysis
- 时间复杂度 / 空间复杂度
  - `MenuServiceImpl.getMenuListByUserId()` 当前是“递归 CTE + Java 递归查询”，总体复杂度接近 `O(N * Q)`，其中 `Q` 是每层额外 SQL 次数。
  - `SiteMessageService.markAllAsRead()` 当前是 `selectList + N 次 updateById`，属于典型 `O(N)` 次数据库往返。
  - `AppointmentServiceImpl.pageAppointments()` 在按姓名/手机号筛选时会先把匹配会员全部查出，再拼一个 `IN (...)`，会员规模大时会出现大列表传输和 SQL 变长问题。
- IO / DB 问题
  - 菜单树、站内信已读、预约筛选都存在额外 DB 往返或批量 ID 搬运。
  - Outbox 在不可路由场景下的状态闭环不完整，会造成“数据库显示成功、消息实际未到”的排障成本。
- 是否有缓存优化空间
  - 菜单树可以继续缓存，但前提是先把查询模型改成“一次查全 + 内存组树”。
  - 站内信未读数可以做短 TTL 缓存，但前提是先补用户维度，否则缓存只会把错误模型放大。

## 7. Architecture & Design
- 是否符合 SOLID 原则
  - 业务模块拆分整体方向是对的，但“认证”和“授权”职责没有真正分层，导致安全边界失效。
  - 站内信把“消息实体”和“用户已读状态”揉在一张表里，不符合后续扩展需要。
- 模块耦合情况
  - 控制器层普遍直接拼装当前用户获取逻辑，存在重复代码。
  - 菜单、角色、JWT、控制器之间没有形成统一的授权抽象。
- 是否易扩展
  - 当前模型适合单门店、低账号数、弱权限要求的阶段。
  - 如果继续往多角色、多店员、更多消息场景演进，站内信模型和授权模型都会成为硬阻塞点。

## 8. Refactoring Suggestions
- 具体优化方案
  - 把权限模型升级为“JWT 认证 + authority 授权 + 方法级注解”。
  - 引入 `CurrentUserProvider`，统一获取当前登录用户，移除各控制器重复的 `getCurrentUserId()` / `getLoginUser()`。
  - 把站内信拆成“消息定义”和“用户已读状态”两个维度，支持个人消息与广播消息。
  - 把菜单查询改成“一次全量查 + 内存组树”，不要继续走递归数据库查询。
  - 为 Outbox 增加不可路由失败回写和明确的发送中间状态。
- 必要时提供重构代码示例
```java
// 建议抽一个统一的当前用户提供器
@Component
public class CurrentUserProvider {
    public User requireCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof User user)) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "用户未登录");
        }
        return user;
    }
}
```

```java
// 控制器中统一使用
@PutMapping("/read-all")
public Result<Void> markAllAsRead() {
    User currentUser = currentUserProvider.requireCurrentUser();
    siteMessageService.markAllAsRead(currentUser.getId());
    return Result.success("全部已读");
}
```

## 9. Testability
- 是否易于测试
  - 现有项目已经有较完整的 Spring Boot + Testcontainers 基线，适合继续补安全与消息链路测试。
  - 但当前授权逻辑分散在控制器、过滤器、service 之间，测试需要覆盖的入口较多。
- 建议补充哪些测试
  - 增加“普通店员不能调用 `/user/**`、`/role/**` 写接口”的鉴权集成测试。
  - 增加“禁用用户不能登录、不能刷新 token、已发 access token 也会被拒绝”的安全测试。
  - 增加“用户 A 的站内信不会被用户 B 看见，也不会被用户 B 标记已读”的隔离测试。
  - 增加“mandatory publish 下消息不可路由时，Outbox 不会被标记为 `SENT`”的 MQ 集成测试。
  - 增加“菜单树查询只触发一次主查询”的性能回归测试或 SQL 次数断言。
- 本次验证说明
  - 已使用 Java 21 运行 `./mvnw test -DskipITs=true`，结果 `Tests run: 14, Failures: 0, Errors: 0, Skipped: 0`。
  - 编译阶段额外出现两类构建告警：
    - `InventoryAlertQueryDTO` 的 `@Builder` 默认值不会生效。
    - Mockito 目前仍依赖动态自附加 agent，后续 JDK 收紧后建议按官方文档改成显式 agent 配置。
