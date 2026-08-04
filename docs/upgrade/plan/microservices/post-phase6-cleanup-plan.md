# Phase 7.2 + 7.3 + 7.4 收尾清理规划

> 本规划基于 `microservices` 分支 Phase 0~6 + traceId 传播修复（2026-08-04）完成后的代码基线。
> 目标是把微服务迁移遗留的真实缺口收口：iam 死配置、iam 单测空白、跨服务契约测试缺失。
> **明确不做**：Zipkin / Loki / K8s / Sentinel / Seata / Spring Retry（已在 roadmap 标 ⏸ 暂缓，触发条件出现再评估）。

---

## 一、背景与目标

微服务主干（解耦 + 加固 + traceId）已落地，但审查发现三处遗留技术债：

1. **iam SecurityFilterChain 含 7 条死规则**——针对 `/vip/**`、`/appointment/**`、`/flower/**`、`/sales/**`、`/inventory/**`、`/inventory-alert/**`、`/site-message/**` 的角色约束，这些端点根本不在 iam-service，是单体拆分时未清理的残留。`/sys/**` 下还有两个完全空壳的控制器（`UserRoleController`/`RoleMenuController`）。
2. **iam 模块零纯单元测试**——仅有 4 个 Testcontainers IT，`JwtUtil`/`RoleServiceImpl`/`MenuServiceImpl` 完全无直接测试，`UserServiceImpl` 的 login/refresh/changePassword 失败分支未覆盖。iam 是 RBAC + JWT + 密码的安全核心，反而是测试最薄的模块。
3. **跨服务无契约测试**——Phase 3 远程化后 2 个跨服务 IT 被 `@Disabled`，且无任何 `@RestClientTest`。provider 改 `/internal/**` 接口时 consumer 无自动感知。

目标：以最小成本把这三处补齐，**不引入新中间件、不改业务逻辑、不扩大架构**。

---

## 二、范围边界

### 本规划覆盖

| 阶段 | 内容 | 性质 |
|:---|:---|:---|
| Phase 7.2 | iam 死配置 + 空壳控制器清理 | 做减法，纯删除 |
| Phase 7.3 | iam 单测补齐（UserServiceImpl / RoleServiceImpl / MenuServiceImpl / JwtUtil） | 补安全核心测试 |
| Phase 7.4 | `@RestClientTest` 契约测试（trade→product / trade→crm / crm→iam） | 补跨服务契约 |

### 明确不做（触发条件出现再说）

- Micrometer Tracing + Zipkin：traceId 跨服务传播已修复，日志关联覆盖 80% 价值
- Loki / Promtail 日志聚合：单节点 Compose，`docker logs` 够用
- Grafana dashboard + Prometheus scrape：metrics 端点已暴露，加 scrape 成本低但非必需，有空再做
- K8s / Helm：服务数 < 10、团队 < 5 人不评估
- Sentinel / Seata / Dubbo / Kafka：迁移规划已否决
- RestClient Spring Retry：Resilience4j 熔断 + LoadBalancer 实例切换已覆盖

---

## 三、Phase 7.2 — iam 死配置与空壳清理（先做，1~2 小时）

### 3.1 改动清单

**文件 1：`jasmine-iam/src/main/java/com/nfu/jasmine/config/MySecurityConfig.java`**

删除以下死规则（目标端点不在 iam-service）：

| 行号 | 删除内容 | 理由 |
|:---|:---|:---|
| 57 | `.requestMatchers("/site-message/**").authenticated()` | site-message 属于 crm-service |
| 66 | `.requestMatchers("/vip/**").hasAnyRole("admin","Boss")` | vip 属于 crm-service |
| 67 | `.requestMatchers("/appointment/**").hasAnyRole("admin","Boss")` | appointment 属于 crm-service |
| 70 | `.requestMatchers("/flower/**").hasAnyRole("admin","Boss","clerk")` | flower 属于 product-service |
| 71 | `.requestMatchers("/sales/**").hasAnyRole("admin","Boss","clerk")` | sales 属于 trade-service |
| 72 | `.requestMatchers("/inventory/**").hasAnyRole("admin","Boss","clerk")` | inventory 属于 trade-service |
| 73 | `.requestMatchers("/inventory-alert/**").hasAnyRole("admin","Boss","clerk")` | inventory-alert 属于 trade-service |

**保留** `/sys/**` 的 `hasRole("admin")`（第 63 行）**直至 3.2 删除空壳控制器后再一并清理**——避免删除控制器与删除规则两步之间出现安全缺口。

清理后 `MySecurityConfig` 的有效规则应仅剩：`OPTIONS/**`、`/internal/**`、登录/actuator/swagger 白名单、`/user/info` 等已登录路径、`/user/**`/`/role/**`/`/menu/**` 的 admin 约束、`anyRequest().denyAll()` 兜底。

### 3.2 删除空壳控制器及配套空 Service/Mapper

| 文件 | 删除理由 |
|:---|:---|
| `jasmine-iam/.../iam/web/UserRoleController.java` | 空壳，`@Controller` + `/sys/userRole`，无任何端点/字段 |
| `jasmine-iam/.../iam/web/RoleMenuController.java` | 空壳，`@Controller` + `/sys/roleMenu`，无任何端点/字段 |
| `jasmine-iam/.../iam/application/IUserRoleService.java` + `impl/UserRoleServiceImpl.java` | MyBatis-Plus `IService`/`ServiceImpl` 空继承，无自定义方法 |
| `jasmine-iam/.../iam/application/IRoleMenuService.java` + `impl/RoleMenuServiceImpl.java` | 同上 |
| `jasmine-iam/.../iam/persistence/mapper/UserRoleMapper.java` | `BaseMapper<UserRole>` 空继承 |
| `jasmine-iam/.../iam/persistence/mapper/RoleMenuMapper.java` | `BaseMapper<RoleMenu>` 空继承 |

**删除前必须确认**：
- 全仓 grep `UserRoleService`/`RoleMenuService`/`UserRoleMapper`/`RoleMenuMapper` 的引用，确认无业务代码依赖（预期仅控制器自身引用）
- `UserServiceImpl`/`RoleServiceImpl` 内部若直接用了这些 Mapper（如查角色关联），改为用各自的 `userRoleMapper`/`roleMenuMapper` 字段——但根据 Phase 6 审查，这些 Service 已有自己的 Mapper 注入，空壳的 UserRoleMapper/RoleMenuMapper 应未被引用

**控制器删除后**，回到 `MySecurityConfig` 删除第 63 行的 `/sys/**` → `hasRole("admin")` 规则（此时已无 `/sys/**` 端点）。

**同步**：`jasmine-gateway` 的路由配置 `ops/nacos-config/jasmine-gateway.yml` 若有 `/sys/**` 路由也一并删除（核对后若无该路由则跳过）。

### 3.3 验证

- `./mvnw -pl jasmine-iam -am test -DskipITs=true`：编译 + 单测通过
- `./mvnw -pl jasmine-iam -am verify -DskipUTs=true`：IT 通过（`UserControllerSecurityIT` 验证安全规则未误删有效项）
- 手动核对：`MySecurityConfig` 删除后，iam 内仍存在的端点（`/user/**`、`/role/**`、`/menu/**`、`/internal/user/**`）规则完整

---

## 四、Phase 7.3 — iam 单测补齐（2~3 天）

### 4.1 测试基础设施

- 风格对齐现有 `FlowerStockServiceTest`/`SalesServiceImplTest`：`@ExtendWith(MockitoExtension.class)` + `@Mock`/`@InjectMocks`，不启 Spring 上下文
- `UserServiceImpl` 继承 `ServiceImpl<UserMapper, User>`，`@InjectMocks` 无法注入 baseMapper，参照 `SalesServiceImplTest` 用 `ReflectionTestUtils.setField(service, "baseMapper", userMapper)`
- 缓存注解（`@Cacheable`/`@CacheEvict`）在纯单测不触发（无 Spring 上下文），单测只验证 Service 方法体内的业务逻辑，缓存行为留给 IT
- 断言用 AssertJ

### 4.2 测试文件清单

| 文件 | 模块 | 用例数 | 覆盖场景 |
|:---|:---|:---|:---|
| `JwtUtilTest` | jasmine-common | 6 | 签发+解析 round-trip；过期 token 拒绝；篡改签名拒绝；refresh token 类型识别；access/refresh 类型混淆拒绝；claims 字段完整性 |
| `UserServiceImplTest` | jasmine-iam | ~10 | 见 4.3 |
| `RoleServiceImplTest` | jasmine-iam | 6 | listAllRoles 命中缓存路径（mock）；addRole 事务内插角色+批量插 role_menu；getRoleById 含菜单 ID；updateRole 先删后插 role_menu；deleteRoleById 级联删 role_menu |
| `MenuServiceImplTest` | jasmine-iam | 5 | getAllMenu 建树；getMenuListByUserId 命中缓存路径；buildMenuTree 空列表；buildMenuTree 孤儿节点（parentId 指向不存在）；buildMenuTree 三层嵌套 |

### 4.3 UserServiceImplTest 详细用例

| 用例 | 覆盖方法 | 验证点 |
|:---|:---|:---|
| `loginShouldReturnTokenPairWhenPasswordMatches` | login | BCrypt 校验通过；签发 access+refresh；吊销既有 refresh token |
| `loginShouldThrowWhenUserNotFound` | login | 用户不存在抛业务异常 |
| `loginShouldThrowWhenPasswordWrong` | login | 密码不匹配抛业务异常 |
| `loginShouldThrowWhenUserDisabled` | login | status≠1 抛业务异常 |
| `refreshTokenShouldRotateWhenValid` | refreshToken | 旧 token 吊销 + 重签新对 |
| `refreshTokenShouldReturnNullWhenTokenInvalid` | refreshToken | 解析失败分支 |
| `refreshTokenShouldReturnNullWhenTokenRevoked` | refreshToken | revoked=1 分支 |
| `refreshTokenShouldReturnNullWhenTokenExpired` | refreshToken | 过期分支 |
| `refreshTokenShouldReturnNullWhenUserDisabled` | refreshToken | 用户禁用分支 |
| `changePasswordShouldReturnFalseWhenOldPasswordWrong` | changePassword | 旧密码校验失败 |
| `changePasswordShouldRevokeRefreshTokensWhenChanged` | changePassword | 改密成功后吊销 refresh 的副作用 |
| `updateUserShouldRevokeRefreshTokensWhenUserDisabled` | updateUser | status 置 0 时吊销 refresh |

### 4.4 JwtUtil 跨模块说明

`JwtUtil` 位于 `jasmine-common`（非 iam）。现有 `jasmine-common/.../JwtUtilTest.java` 仅 2 个用例。补 6 个用例直接加到现有文件，无需新建。重点补过期/篡改/类型混淆拒绝分支——这些是安全关键路径，当前零覆盖。

### 4.5 验证

- `./mvnw -pl jasmine-iam,jasmine-common -am test -DskipITs=true`：新增单测全过
- 覆盖率自检：`UserServiceImpl` 关键方法（login/refreshToken/changePassword）分支覆盖应达 80%+

---

## 五、Phase 7.4 — `@RestClientTest` 契约测试（2~3 天）

### 5.1 测试基础设施决策

**挑战**：远程客户端经 `InternalClientFactory`（用 `@LoadBalanced RestClient.Builder` + `HttpServiceProxyFactory`）创建，直接 `@RestClientTest` 无法替换 LoadBalancer 逻辑。

**方案**：不通过工厂，**测试内直接手搓 RestClient-backed HttpInterface 绑定到 `MockRestServiceServer`**。契约测试只验证"请求路径/方法/序列化/响应解析"，不需要 LoadBalancer 真实路由。

```java
// 测试内通用构造（可提取到测试基类或工具方法）
RestClient.Builder builder = RestClient.builder();
MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
FlowerClient client = HttpServiceProxyFactory.builderFor(
        RestClientAdapter.create(builder.baseUrl("http://product-service").build()))
        .build().createClient(FlowerClient.class);
// server.expect(requestTo("http://product-service/internal/flower/1")).andRespond(...);
```

**依赖**：`spring-boot-starter-test` 已传递提供 `MockRestServiceServer`，无需加 pom 依赖。

### 5.2 测试文件清单

| 文件 | 消费方 | 覆盖契约 | 用例数 |
|:---|:---|:---|:---|
| `FlowerClientContractTest` | jasmine-trade | FlowerClient ↔ FlowerInternalController | 5 |
| `VipClientContractTest` | jasmine-trade | VipClient ↔ VipInternalController | 4 |
| `UserClientContractTest` | jasmine-trade | UserClient ↔ UserInternalController | 2 |
| `CrmUserClientContractTest` | jasmine-crm | CrmUserClient ↔ UserInternalController | 2 |

### 5.3 FlowerClientContractTest 详细用例

| 用例 | 验证 |
|:---|:---|
| `getFlowerByIdShouldSerializePathAndParseDto` | GET `/internal/flower/{id}`，path 变量正确，`FlowerDTO` 反序列化字段完整 |
| `getFlowersByIdsShouldPostJsonArrayBody` | POST `/internal/flower/batch`，请求体是 `[1,2,3]` JSON 数组，响应 `List<FlowerDTO>` |
| `getFlowerIdsByNameShouldPassQueryParam` | GET `/internal/flower/ids-by-name?name=rose`，query 参数正确 |
| `adjustStockShouldParseResponseEntity` | POST `/internal/flower/stock/adjust`，`ResponseEntity<StockAdjustResult>` 状态码与 body 解析 |
| `adjustStockShouldHandleBusinessFailureAs422` | 422 响应触发 `BusinessException` 透传（验证非 5xx 不被熔断误判） |

其余三个 Client 契约测试用例模式相同，覆盖各自方法的路径/方法/序列化。

### 5.4 契约一致性保障

每个契约测试文件顶部注释标注对应的 Provider 控制器与端点，**Provider 改接口时必须同步改契约测试**——形成显式耦合点。CI 中契约测试失败即代表 provider/consumer 漂移。

### 5.5 验证

- `./mvnw -pl jasmine-trade,jasmine-crm -am test -Dtest='*ContractTest'`：契约测试全过
- 故意改一个 provider 端点路径，验证对应契约测试立即失败（验证契约有效性）

---

## 六、实施顺序与预估

```
Phase 7.2（死配置清理）  ── 1~2 小时 ── 纯删除，风险最低，先做
    │
    ▼
Phase 7.3（iam 单测）   ── 2~3 天 ── 补安全核心测试
    │
    ▼
Phase 7.4（契约测试）   ── 2~3 天 ── 补跨服务契约
```

每个阶段独立成 PR，可单独回滚。Phase 7.2 改动最小风险最低优先；7.3/7.4 可并行。

---

## 七、风险与缓解

| 风险 | 影响 | 缓解 |
|:---|:---|:---|
| 删除空壳 Service/Mapper 后发现某处隐式依赖 | 编译失败 | 删除前全仓 grep 引用；编译验证兜底 |
| `MySecurityConfig` 误删有效规则 | iam 端点鉴权失效 | `UserControllerSecurityIT` 覆盖关键鉴权路径，IT 通过即安全规则完整 |
| `@RestClientTest` 手搓 RestClient 与生产工厂行为不一致 | 契约测试通过但生产失败 | 契约测试只验序列化/路径，不验 LoadBalancer/熔断/超时——这些由现有 `RemoteProductStockFacadeTest`（已存在）覆盖 |
| iam 单测 Mock 不到位导致假绿 | 测试无意义 | 关键用例验证 `verify()` 调用次数与顺序（如 changePassword 必须先校验旧密码再吊销 refresh） |

---

## 八、与路线图的对应

本规划对应 `microservices-future-roadmap.md` 第六节的：

- ⏳ Phase 7.3 — iam 单测补齐（本规划 7.3）
- ⏳ Phase 7.4 — 跨服务契约测试（本规划 7.4）
- ⏳ Phase 7.2 — iam 死配置清理（本规划 7.2）

完成后这两项在路线图标 ✅，微服务分支的"真实待办"清空，剩余均为 ⏸ 暂缓项（按业务量触发再评估）。

---

## 九、不在本规划但值得记录的后续观察点

- **跨 MQ 链路 traceId 串联**：traceId 跨 HTTP 已修，但 Outbox 消费端的 traceId 是消费服务自己生成的。如需 MQ 消费也串联上游 trace，可让消费端 MQ 监听器入口从消息体的 traceId 字段写入 MDC。属可选增强，当前不必要。
- **`BusinessModelWorkflowIT` / `OutboxAndAlertIntegrationIT` 两个 `@Disabled` 跨服务 IT**：本规划用 `@RestClientTest` 契约测试替代了它们的"契约保障"职责，但端到端业务流（销售→库存→预警）的 IT 仍空缺。如未来需要，可用 Testcontainers 同时拉起 trade+product 两个服务做真正的 e2e——成本较高，按需评估。
