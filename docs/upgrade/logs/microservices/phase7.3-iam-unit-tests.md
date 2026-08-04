# Phase 7.3 — iam 单测补齐

日期：2026-08-04

## 背景

iam 模块是 RBAC + JWT + 密码的安全核心，但 Phase 7.3 之前**零纯单元测试**——仅有 4 个
Testcontainers 集成测试。`UserServiceImpl`/`RoleServiceImpl`/`MenuServiceImpl` 完全无直接测试，
`JwtUtil` 仅 2 个 smoke 用例，过期/篡改/类型混淆等安全拒绝路径零覆盖。

目标：用 `MockitoExtension` + `@Mock`/`@InjectMocks` 纯单测补齐安全核心分支，不启 Spring 上下文，
缓存注解行为留给 IT。

## 改动内容

### 1. 新增/补齐测试文件（4 个文件，30 个用例）

| 文件 | 模块 | 用例数 | 覆盖场景 |
|:---|:---|:---|:---|
| `JwtUtilTest` | jasmine-common | +4（原 2，合计 6） | 过期 token 拒绝；篡改签名拒绝；refresh token 类型识别 + tokenId 完整性；claims 时间戳完整性 |
| `UserServiceImplTest` | jasmine-iam | 13 | 见下表 |
| `RoleServiceImplTest` | jasmine-iam | 6 | listAllRoles 升序返回；addRole 插角色+批量插 role_menu；addRole menuIdList 为 null 跳过；getRoleById 含菜单 ID；updateRole 先删后插 role_menu；deleteRoleById 级联删 role_menu |
| `MenuServiceImplTest` | jasmine-iam | 5 | getAllMenu 建树；getMenuListByUserId 建授权树；空列表返回空树；孤儿节点丢弃；三层嵌套 |

### 2. UserServiceImplTest 详细用例

| 用例 | 覆盖方法 | 验证点 |
|:---|:---|:---|
| `loginShouldReturnTokenPairWhenPasswordMatches` | login | 密码匹配 → 签发 token + 吊销既有 refresh + 插入新 refresh 记录 |
| `loginShouldReturnNullWhenUserNotFound` | login | 用户不存在 → null，不签发 token |
| `loginShouldReturnNullWhenPasswordWrong` | login | 密码不匹配 → null，不签发 token |
| `loginShouldReturnNullWhenUserDisabled` | login | status=0 → null，不调密码校验 |
| `refreshTokenShouldRotateWhenValid` | refreshToken | 旧 token 吊销 + 重签新对 |
| `refreshTokenShouldReturnNullWhenTokenInvalid` | refreshToken | 解析失败 → null |
| `refreshTokenShouldReturnNullWhenTokenRevoked` | refreshToken | DB 查不到（已吊销）→ null |
| `refreshTokenShouldReturnNullWhenUserDisabled` | refreshToken | 用户禁用 → null，不签发新 token |
| `changePasswordShouldReturnFalseWhenOldPasswordWrong` | changePassword | 旧密码错 → false，不更新 |
| `changePasswordShouldRevokeRefreshTokensWhenChanged` | changePassword | 改密成功 + 吊销 refresh |
| `updateUserShouldRevokeRefreshTokensWhenUserDisabled` | updateUser | status=0 → 吊销 refresh |
| `updateUserShouldNotRevokeRefreshTokensWhenUserActive` | updateUser | status=1 → 不吊销（对照用例） |
| `deleteUserByIdShouldDeleteUserAndClearRolesAndRevokeTokens` | deleteUserById | 删用户 + 清角色关联 + 吊销 refresh |

## 设计决策

### 为什么用 `TableInfoHelper.initTableInfo` 初始化 lambda cache

`UserServiceImpl.revokeActiveRefreshTokens` 内部用 `LambdaUpdateWrapper<AuthRefreshToken>`
引用 `AuthRefreshToken::getUserId` 等方法引用。MyBatis-Plus 解析这些 lambda 需要 `TableInfo`
缓存，而纯单测无 Spring/MyBatis 上下文，缓存为空 → `can not find lambda cache for this entity`。

解决方案：`@BeforeEach` 里用 `TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new Configuration(), ""), AuthRefreshToken.class)` 手动初始化。
这是纯内存操作，不依赖数据库或 Spring，比启 Spring 上下文轻得多。

### 为什么不测缓存行为

`@Cacheable`/`@CacheEvict` 是 Spring AOP 织入的，纯 Mockito 单测无 Spring 上下文，
这些注解不会触发。单测只验证 Service 方法体内的业务逻辑（DB 调用、条件分支、副作用），
缓存命中/失效行为留给 `UserControllerSecurityIT` 等集成测试覆盖。

### BaseMapper 重载方法的 verify 写法

MyBatis-Plus 3.5.x 的 `BaseMapper` 有 `insert(T)` 和 `insert(Collection<T>)` 重载。
`verify(mapper).insert(any())` 因 `any()` 返回 null 导致重载不明确，编译报错。
解法：用 `verify(mapper).insert((T) any())` 或 `verify(mapper, never()).insert(any(T.class))` 显式指定类型。

## 验证

- `./mvnw -pl jasmine-common,jasmine-iam -am test -DskipITs=true`：
  - jasmine-common：32 个单测全过（含 `JwtUtilTest` 6 个）
  - jasmine-iam：24 个单测全过（含新增 `UserServiceImplTest` 13 + `RoleServiceImplTest` 6 + `MenuServiceImplTest` 5）
  - 无回归

## 涉及文件

| 文件 | 改动 |
|:---|:---|
| `jasmine-common/src/test/.../JwtUtilTest.java` | 新增 4 个用例（过期/篡改/refresh 识别/字段完整性） |
| `jasmine-iam/src/test/.../UserServiceImplTest.java` | 新增：13 个用例 |
| `jasmine-iam/src/test/.../RoleServiceImplTest.java` | 新增：6 个用例 |
| `jasmine-iam/src/test/.../MenuServiceImplTest.java` | 新增：5 个用例 |
| `docs/upgrade/roadmap/microservices-future-roadmap.md` | Phase 7.3 标记 ⏳ → ✅ |

## 后续

Phase 7.3 收口完成。剩余真实待办：

- **Phase 7.4** — 跨服务契约测试（`@RestClientTest`，trade→product / trade→crm / crm→iam）

完成后路线图「真实待办」清空，剩余均为 ⏸ 暂缓项。
