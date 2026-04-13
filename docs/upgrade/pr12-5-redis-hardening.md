# PR12.5 Redis 收口备忘录

## 本轮目标

这一轮的目标不是把 Redis 铺到全部业务，而是先把当前已经接入的缓存能力收口成：

- 缓存对象边界清楚
- 缓存失效链明确
- `memory` 与 `redis` 两种模式都能稳定工作
- 为后续 `PR13` 的 MQ、以及更后面的高并发治理打基础

## 本轮实际改动

### 1. 明确缓存对象范围

本轮只缓存这些对象：

- `user`
- `menuList`
- `roleList`
- `flowerList`
- `flowerDetail`

没有纳入缓存的对象：

- 销售单
- 销售明细
- 库存流水
- 预约创建结果
- 今日经营统计

这些对象实时性要求更高，或者写路径更复杂，现阶段不适合为了缓存引入额外一致性成本。

### 2. 统一缓存名

新增统一缓存名常量：

- `src/main/java/com/nfu/jasmine/infra/cache/CacheNames.java`

用于收口以下缓存名：

- `user`
- `menuList`
- `roleList`
- `flowerList`
- `flowerDetail`

这样可以避免注解中散落硬编码字符串，后续调整缓存名时也更安全。

### 3. 收口缓存配置

涉及文件：

- `src/main/java/com/nfu/jasmine/config/MyCacheConfig.java`
- `src/main/java/com/nfu/jasmine/config/MyRedisConfig.java`
- `src/main/resources/application-dev.yml`
- `src/main/resources/application-prod.yml`

当前策略：

- 开发环境默认：`memory`
- 生产环境默认：`redis`

Redis TTL 也开始按缓存名拆分：

- `user` / `menuList` / `roleList`：30 分钟
- `flowerList` / `flowerDetail`：10 分钟

### 4. 补齐缓存失效链

涉及文件：

- `src/main/java/com/nfu/jasmine/iam/application/impl/UserServiceImpl.java`
- `src/main/java/com/nfu/jasmine/iam/application/impl/RoleServiceImpl.java`
- `src/main/java/com/nfu/jasmine/iam/application/impl/MenuServiceImpl.java`
- `src/main/java/com/nfu/jasmine/flower/application/impl/FlowerServiceImpl.java`
- `src/main/java/com/nfu/jasmine/inventory/application/impl/InventoryServiceImpl.java`
- `src/main/java/com/nfu/jasmine/sales/application/impl/SalesServiceImpl.java`

当前已覆盖的失效链：

- 用户修改、删除、改密后清理 `user`
- 用户角色变化时同步清理 `menuList`
- 角色新增、修改、删除后清理 `roleList`
- 角色菜单变更时同步清理 `menuList`
- 花卉新增、修改、删除后清理 `flowerList` 与 `flowerDetail`
- 库存和销售写操作会同步清理花卉主数据缓存，因为它们会影响库存和部分成本口径

### 5. 补充主数据缓存读路径

涉及文件：

- `src/main/java/com/nfu/jasmine/flower/application/IFlowerService.java`
- `src/main/java/com/nfu/jasmine/flower/application/impl/FlowerServiceImpl.java`
- `src/main/java/com/nfu/jasmine/flower/web/FlowerController.java`
- `src/main/java/com/nfu/jasmine/iam/application/IRoleService.java`
- `src/main/java/com/nfu/jasmine/iam/web/RoleController.java`

新增并切换到缓存读路径的方法包括：

- `listAllFlowers()`
- `getFlowerDetailById(Integer id)`
- `listAllRoles()`

## 本轮验证经过

### 一、memory 模式验证

默认开发环境本来就是 `memory` 模式，因此先对当前运行中的后端做了后端接口级验证。

#### 1. `user` 缓存失效验证

验证方式：

1. 调用 `GET /user/1`
2. 调用 `PUT /user` 修改手机号
3. 再次调用 `GET /user/1`
4. 确认能立即读到新手机号
5. 将手机号恢复原值

结果：

- 验证通过

#### 2. `flowerDetail / flowerList` 缓存失效验证

验证方式：

1. 调用 `GET /flower/1`
2. 调用 `GET /flower/all`
3. 调用 `PUT /flower` 修改售价
4. 再次读取详情和列表
5. 确认详情与列表都立即反映最新售价
6. 将售价恢复原值

结果：

- 验证通过

#### 3. `menuList` 缓存失效验证

验证方式：

1. 调用 `GET /user/info`
2. 统计菜单树节点数
3. 调用 `PUT /role` 临时减少管理员角色的菜单权限
4. 再次调用 `GET /user/info`
5. 确认菜单树节点数减少
6. 恢复原角色菜单
7. 再次读取，确认菜单树恢复

第一次验证时出现了异常现象：

- 菜单失效后会立即变化
- 但恢复后菜单没有恢复

这说明并不是缓存不生效，而是失效后重新计算菜单时，读出了错误的菜单树结果。

### 二、定位到的真实问题

问题根因不在 `@CacheEvict`，而在旧的权限菜单模型：

涉及文件：

- `src/main/resources/mapper/sys/RoleMenuMapper.xml`
- `src/main/resources/mapper/sys/MenuMapper.xml`

旧逻辑存在口径不一致：

- `RoleMenuMapper.xml` 查询角色菜单时，只返回叶子菜单
- `MenuMapper.xml` 生成导航树时，又要求父菜单本身也直接存在于 `role_menu`

这导致：

- 旧缓存存在时，页面可能看起来正常
- 一旦 `menuList` 真正失效并重算，就会把父菜单链断掉
- 极端情况下菜单树可能直接变空

### 三、修复方案

修复文件：

- `src/main/resources/mapper/sys/MenuMapper.xml`

修复思路：

- 不推翻当前“角色只存叶子菜单”的历史习惯
- 改为从已授权菜单出发
- 通过递归查询把父菜单自动补回来
- 再按 `parentId` 组织成导航树

这样可以兼容：

- 历史角色菜单数据
- 现在的菜单树展示逻辑
- 当前的缓存失效策略

### 四、修复后的验证结果

修复后重新验证：

- `user` 缓存失效：通过
- `menuList` 缓存失效：通过
- `menuList` 恢复：通过
- `flowerDetail` 缓存失效：通过
- `flowerList` 缓存失效：通过

其中菜单树节点数验证结果为：

- 变更前：`9`
- 临时减少权限后：`8`
- 恢复后：`9`

说明 `menuList` 的失效与恢复都已经闭环。

### 五、redis 模式验证

为了不影响当前本地 `9999` 端口的开发实例，单独启动了一份 Redis 模式的临时后端实例到 `10099` 端口，用来做运行态验证。

验证目标：

- 在真实 Redis 模式下，重复 `memory` 模式的同一套验证链路

最终结果：

- `user` 缓存失效：通过
- `menuList` 缓存失效：通过
- `menuList` 恢复：通过
- `flowerDetail` 缓存失效：通过
- `flowerList` 缓存失效：通过

说明当前这批缓存收口改动，已经在：

- `memory`
- `redis`

两种模式下都通过了后端接口级验证。

## 本轮结论

`PR12.5` 当前已经完成的核心目标是：

- 把 Redis 从“已经接入但边界模糊”推进成“缓存对象明确、失效链清楚、运行态已验证”的基础设施

当前可以认为已经跑通的内容：

- `user` 缓存
- `menuList` 缓存
- `roleList` 缓存
- `flowerList / flowerDetail` 缓存
- `memory / redis` 双模式运行验证

## 补充检查：Mapper 查询写法

这轮顺手检查了当前仓库里的 MyBatis XML / Mapper 查询写法，重点排查是否存在明显的 `select *` 低级错误。

检查结果：

- 没有发现裸写的 `select *`
- 但在 `src/main/resources/mapper/sys/MenuMapper.xml` 中，仍存在一处等价的全列查询：`SELECT DISTINCT m.*`

当前判断：

- 这不属于会立刻影响功能的阻塞问题
- 对当前系统的性能影响也很小，因为 `menu` 表数据量不大、字段数量也有限
- 但从代码整洁性和后续维护角度看，它仍然不是最佳写法

为什么暂时没有继续修改：

- 当前 `PR12.5` 的主任务是 Redis 收口与缓存一致性验证
- 这条 SQL 本身不会阻碍缓存逻辑，也不会影响后续微服务演进
- 因此更适合作为后续顺手清理的查询优化项，而不是本轮的核心修复项

后续建议：

- 如果后面继续整理 `iam` 域查询，建议把这处 `m.*` 改成显式字段列表
- 这样可以让导航树查询结果边界更清楚，也能减少未来字段扩展后的无效列读取

## 当前仍然不做的内容

本轮仍然明确不做：

- Redis 分布式锁
- 库存预扣减
- 秒杀型高并发治理
- MQ 幂等
- 多级缓存
- 销售 / 库存 / 预约结果缓存

这些属于后面更靠后的高并发与一致性阶段，不属于当前单体收口范围。