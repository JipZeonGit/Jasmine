# Phase 7.2 — iam 死配置与空壳清理

日期：2026-08-04

## 背景

`post-phase6-cleanup-plan.md` 在 Phase 7.2 一节指出 iam-service 存在两类单体拆分遗留：

1. **`MySecurityConfig` 含死规则**——针对 `/vip/**`、`/appointment/**`、`/flower/**`、`/sales/**`、`/inventory/**`、`/inventory-alert/**`、`/site-message/**` 的角色约束，这些端点根本不在 iam-service。
2. **`/sys/**` 下两个空壳控制器**——`UserRoleController`/`RoleMenuController` 是 MyBatis-Plus 代码生成器残留，无任何端点/字段，配套的 Service/Mapper 也是空继承。

目标：以纯删除的方式把这些残留收口，**不引入新依赖、不改业务逻辑**。

## 改动内容

### 1. `MySecurityConfig` 死规则清理

文件：`jasmine-iam/src/main/java/com/nfu/jasmine/config/MySecurityConfig.java`

删除以下针对其他服务端点的规则（端点归属已迁移至对应微服务）：

| 已删规则 | 端点实际归属 |
|:---|:---|
| `/site-message/**` → `authenticated()` | crm-service |
| `/vip/**` → `hasAnyRole("admin","Boss")` | crm-service |
| `/appointment/**` → `hasAnyRole("admin","Boss")` | crm-service |
| `/flower/**` → `hasAnyRole("admin","Boss","clerk")` | product-service |
| `/sales/**` → `hasAnyRole("admin","Boss","clerk")` | trade-service |
| `/inventory/**` → `hasAnyRole("admin","Boss","clerk")` | trade-service |
| `/inventory-alert/**` → `hasAnyRole("admin","Boss","clerk")` | trade-service |
| `/sys/**` → `hasRole("admin")` | 空壳控制器已删，无对应端点 |

清理后 `MySecurityConfig` 的有效规则仅剩 iam-service 实际暴露的端点：
- `OPTIONS/**`、`/internal/**`、登录/actuator/swagger 白名单
- `/user/info`、`/user/logout`、`/user/changePassword` → `authenticated()`
- `/user/**`、`/role/**`、`/menu/**` → `hasRole("admin")`
- `anyRequest().denyAll()` 兜底

### 2. 空壳 Controller/Service 删除

删除以下 6 个文件（MyBatis-Plus 代码生成器残留，无业务代码引用）：

| 文件 | 删除理由 |
|:---|:---|
| `jasmine-iam/.../iam/web/UserRoleController.java` | 空壳 `@Controller` + `/sys/userRole`，无端点 |
| `jasmine-iam/.../iam/web/RoleMenuController.java` | 空壳 `@Controller` + `/sys/roleMenu`，无端点 |
| `jasmine-iam/.../iam/application/IUserRoleService.java` | `IService<UserRole>` 空继承 |
| `jasmine-iam/.../iam/application/IRoleMenuService.java` | `IService<RoleMenu>` 空继承 |
| `jasmine-iam/.../iam/application/impl/UserRoleServiceImpl.java` | `ServiceImpl` 空继承 |
| `jasmine-iam/.../iam/application/impl/RoleMenuServiceImpl.java` | `ServiceImpl` 空继承 |

### 3. 空壳 Mapper XML 删除

删除 `jasmine-iam/src/main/resources/mapper/sys/UserRoleMapper.xml`——该文件无任何 SQL 语句，`UserRoleMapper` 仅用 MyBatis-Plus 通用方法。

### 4. 网关路由同步清理

文件：`ops/nacos-config/jasmine-gateway.yml`

`iam-service` 路由谓词从 `Path=/user/**,/role/**,/menu/**,/sys/**` 改为
`Path=/user/**,/role/**,/menu/**`——与删除空壳控制器后 iam-service 不再暴露
`/sys/**` 端点的事实对齐。

## 与规划文档的偏差

`post-phase6-cleanup-plan.md` 第 3.2 节原计划同时删除 `UserRoleMapper`/`RoleMenuMapper`
两个 Mapper 接口，并判断「这些 Service 已有自己的 Mapper 注入，空壳的
UserRoleMapper/RoleMenuMapper 应未被引用」。

**实际 grep 发现该判断有误**：

- `UserRoleMapper` 被 `UserServiceImpl` 直接 `@Autowired` 注入，用于用户增删改时
  维护 `user_role` 关联表；`UserControllerSecurityIT` 也用它插入种子角色数据。
- `RoleMenuMapper` 被 `RoleServiceImpl` 直接 `@Resource` 注入，且 `RoleMenuMapper.xml`
  含自定义 SQL `getMenuIdListByRoleId`（联表查询 `role_menu` + `menu` 取叶子菜单 ID）。

因此本次**保留** `UserRoleMapper.java`、`RoleMenuMapper.java`、`RoleMenuMapper.xml`
三个文件，仅删除真正的空壳（6 个 Service/Controller + 1 个空 XML）。这是对规划文档
「删除前必须全仓 grep 引用」原则的执行结果，符合规划第七节「删除空壳 Service/Mapper
后发现某处隐式依赖 → 编译失败」的风险缓解预期——只是发现依赖后选择保留而非删除。

## 验证

- `./mvnw -pl jasmine-iam -am test -DskipITs=true`：编译 + 单测通过。
- `./mvnw -pl jasmine-iam -am verify -DskipUTs=true`：集成测试通过，
  其中 `UserControllerSecurityIT`（9 个用例）验证安全规则清理后 iam 端点鉴权未误删：
  - 未登录访问 `/user/list` → 401
  - `/actuator/health` 公开可访问
  - 登录校验、`/user/info` 鉴权、refresh token 轮换、logout 吊销、CORS preflight
    等全过
- Lint：`MySecurityConfig.java` 无新增错误。

## 涉及文件

| 文件 | 改动 |
|:---|:---|
| `jasmine-iam/.../config/MySecurityConfig.java` | 删除 8 条死规则（7 条跨服务 + `/sys/**`） |
| `jasmine-iam/.../iam/web/UserRoleController.java` | 删除 |
| `jasmine-iam/.../iam/web/RoleMenuController.java` | 删除 |
| `jasmine-iam/.../iam/application/IUserRoleService.java` | 删除 |
| `jasmine-iam/.../iam/application/IRoleMenuService.java` | 删除 |
| `jasmine-iam/.../iam/application/impl/UserRoleServiceImpl.java` | 删除 |
| `jasmine-iam/.../iam/application/impl/RoleMenuServiceImpl.java` | 删除 |
| `jasmine-iam/.../resources/mapper/sys/UserRoleMapper.xml` | 删除（空文件） |
| `ops/nacos-config/jasmine-gateway.yml` | iam-service 路由谓词移除 `/sys/**` |
| `docs/architecture/microservices/architecture-and-api-spec.md` | 同步：路由表移除 `/sys/**`、删除遗留空壳控制器章节、授权规则表移除死规则说明 |
| `docs/upgrade/roadmap/microservices-future-roadmap.md` | Phase 7.2 标记 ⏳ → ✅ |

## 后续

Phase 7.2 收口完成。剩余真实待办：

- **Phase 7.3** — iam 单测补齐（`UserServiceImpl`/`RoleServiceImpl`/`MenuServiceImpl`/`JwtUtil`，
  当前 iam 模块零纯单元测试，是 RBAC/JWT 安全核心）
- **Phase 7.4** — 跨服务契约测试（`@RestClientTest`，trade→product / trade→crm / crm→iam）

两者可并行，各预计 2~3 天。完成后路线图「真实待办」清空。
