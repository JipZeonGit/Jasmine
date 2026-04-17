# PR13.5 Redis 与 MQ 完善方案（校正版）

## 背景

`PR12.5` 已经完成 Redis 缓存收口，`PR13` 已经完成 RabbitMQ 三阶段接入、事件链打通、重试、死信、行为测试与运行文档收口。

当前项目已经不是“Redis / MQ 没接上”的阶段，而是进入了：

- 基础设施已接入
- 主链已经可跑
- 但高并发正确性、缓存稳定性、消息发布可靠性还没有闭环

这份文档用于回答两个问题：

1. 前述对话里的判断整体对不对
2. `PR13.5` 真正应该补哪些东西

## 一、先给总判断

前述对话**大方向是对的**，而且抓住了当前项目最重要的三个问题：

1. 库存主链在并发下还不够安全
2. HTTP 写接口还没有请求级幂等
3. MQ 发布侧还没有做到“主业务成功但消息绝不悄悄丢失”的可靠性闭环

如果把当前阶段概括成一句话，就是：

> Redis 和 MQ 已经形成能力骨架，但真正的“库存正确性 + 接口防重 + 消息不丢”还没补齐。

## 二、对话里判断正确的部分

### 1. Redis 当前主要承担的是“缓存 + MQ 消费幂等”

这一点和当前代码是一致的。

缓存相关实现：

- `src/main/java/com/nfu/jasmine/config/MyCacheConfig.java`
- `src/main/java/com/nfu/jasmine/config/MyRedisConfig.java`
- `src/main/java/com/nfu/jasmine/infra/cache/CacheNames.java`

当前缓存对象范围确实主要是：

- `user`
- `menuList`
- `roleList`
- `flowerList`
- `flowerDetail`

Redis 缓存 TTL 也确实已经拆开：

- `user` / `menuList` / `roleList`：30 分钟
- `flowerList` / `flowerDetail`：10 分钟

MQ 幂等相关实现：

- `src/main/java/com/nfu/jasmine/infra/mq/MqKeyNames.java`
- `src/main/java/com/nfu/jasmine/infra/mq/support/MqIdempotencyService.java`

当前消费端幂等确实使用了：

- Redis `SETNX + TTL`
- Redis 不可用时回退到本地 `ConcurrentHashMap`

### 2. RabbitMQ 当前已经不仅仅是“部署好了”

这一点也是对的。

拓扑、发布、消费、重试、死信、行为测试都已经接入：

- `src/main/java/com/nfu/jasmine/infra/mq/JasmineMqConstants.java`
- `src/main/java/com/nfu/jasmine/infra/mq/config/RabbitMqTopologyConfig.java`
- `src/main/java/com/nfu/jasmine/infra/mq/publisher/MqMessagePublisher.java`
- `src/main/java/com/nfu/jasmine/infra/mq/listener/`
- `src/test/java/com/nfu/jasmine/infra/mq/RabbitMqBehaviorIT.java`

当前已经具备：

- 事务提交后发布
- 消费端幂等
- 本地重试
- 统一死信
- 行为测试

### 3. 当前库存并发风险确实存在

这一点是当前最关键的问题，而且判断是准确的。

对应代码：

- `src/main/java/com/nfu/jasmine/sales/application/impl/SalesServiceImpl.java`
- `src/main/java/com/nfu/jasmine/inventory/application/impl/InventoryServiceImpl.java`

当前库存处理模式本质上都是：

1. 先查 `flower.currentStock`
2. 在 Java 中计算 `afterStock`
3. 校验不能小于 0
4. 再 `flowerMapper.updateById(flower)`

而当前并没有：

- 原子扣减 SQL
- 乐观锁 `version`
- `select ... for update`

这意味着在并发下，确实可能出现：

- 丢失更新
- 超卖
- 库存主表与库存流水口径不一致

### 4. HTTP 写接口还没有请求级幂等

这一点也是对的。

当前项目的幂等能力只覆盖到了 MQ 消费端，没有覆盖 HTTP 写请求入口。

也就是说，销售创建、库存创建、预约创建这些写接口，如果遇到：

- 前端连点
- 浏览器重试
- 网关或代理重复投递

就仍然可能重复落库。

### 5. MQ 发布可靠性确实还没闭环

这一点也是对的。

对应代码：

- `src/main/java/com/nfu/jasmine/infra/mq/publisher/MqMessagePublisher.java`

当前 `publishNow()` 的行为是：

- 发送成功返回 `true`
- 发送失败只记 `warn` 日志并返回 `false`

当前还没有看到：

- Publisher Confirm
- Return 回调
- 本地消息表 / Outbox
- 发布失败补偿重发

所以当前确实存在这种风险：

- 主事务成功
- 但业务事件没有发出去
- 下游消费者永远收不到这条消息

## 三、对话里需要修正或补充的部分

### 1. “消息丢失”不能一概而论，访问日志有同步兜底

前述对话里把 MQ 发布失败统一概括成“消息丢失”，方向没错，但要补一个边界：

- `audit.access-log` 这条链路在 `RequestTraceFilter` 里有同步日志兜底

对应代码：

- `src/main/java/com/nfu/jasmine/infra/web/filter/RequestTraceFilter.java`

也就是说：

- 访问日志 MQ 发不出去时，不会完全丢
- 会回退到本地 `ACCESS_LOG`

真正更需要担心的是：

- `appointment.created`
- `sales.created`
- `inventory.changed`

这些业务事件当前没有同等级的补偿闭环。

### 2. 开发环境默认是 memory 缓存，Redis 缓存问题不会自然暴露

前述对话里对 Redis 缓存问题的判断基本正确，但要补一个现实条件：

- `application-dev.yml` 默认 `app.cache.type=memory`
- `application-prod.yml` 默认 `app.cache.type=redis`

这意味着：

- 本地普通 `dev` 启动时，很多 Redis 缓存问题不一定能直接暴露
- 如果要验证缓存击穿、穿透、雪崩类问题，需要切到 Redis 模式或者用专门环境验证

### 3. 当前不只“创建后事件”缺下游，事件语义本身也还不完整

前述对话重点说了统计表和预警表先别上，这个判断是对的。

但还应再补一层：

- 当前销售和库存消息主要覆盖创建/写入链路
- 修改与删除路径并没有同步发布等价的事件

这意味着当前消息更适合承担：

- 日志型消费
- 通知型消费
- 审计型消费

而不适合直接承担：

- 强依赖一致性的统计读模型
- 强依赖一致性的库存预警表

### 4. 当前缓存问题不只是花卉，用户详情也有真实空值风险

前述对话里提到 `getUserById()` 的潜在问题，这一点是对的，而且应该直接纳入 `PR13.5`。

对应代码：

- `src/main/java/com/nfu/jasmine/iam/application/impl/UserServiceImpl.java`

当前实现中：

- `selectById(id)` 可能返回 `null`
- 后面仍然会执行 `user.setRoleIdList(roleIdList)`

这不仅是缓存穿透问题，也是明确的空指针风险。

## 四、当前 Redis 与缓存侧的主要缺陷

### 1. 缓存穿透

当前表现：

- `MyRedisConfig` 明确 `disableCachingNullValues()`
- 详情查询没有做空值占位缓存
- `getUserById()` 对空值没有安全保护

直接影响：

- 不存在的 `flowerDetail`
- 不存在的 `user`

会持续回源数据库。

### 2. 缓存击穿

当前热点读接口如：

- `FlowerServiceImpl.getFlowerDetailById`
- `FlowerServiceImpl.listAllFlowers`
- `UserServiceImpl.getUserById`
- `MenuServiceImpl.getMenuListByUserId`

都没有使用：

- `@Cacheable(sync = true)`

这意味着热点 key 过期时，并发请求可能一起回源。

### 3. 缓存雪崩 / 类雪崩风险

当前并没有 TTL 抖动。

同时，库存和销售写路径存在大量：

- `@CacheEvict(... allEntries = true)`

尤其是：

- `flowerList`
- `flowerDetail`

会被频繁整批清空。

这不一定是教科书式的 TTL 雪崩，但很容易形成一波集中回源，效果上接近“类雪崩”。

### 4. 还没有请求级幂等

当前 Redis 还没有承担：

- 销售创建接口防重
- 库存创建接口防重
- 预约创建接口防重

这一块是 `PR13.5` 应该新增的职责。

## 五、当前 MQ 侧的主要缺陷

### 1. 发布可靠性不闭环

当前 `MqMessagePublisher` 已经做到了：

- 事务提交后发布

但还没做到：

- 发布确认
- 路由失败确认
- 本地消息表
- 补偿重发

因此当前最大的问题不是“消费端不稳”，而是“发布端失败时业务方感知不够”。

### 2. 延迟消息能力还没落地

当前没有看到：

- 延迟交换机
- TTL 重试队列链
- 预约提醒 / 自动取消的延迟消息模型

这说明当前 MQ 还处在“实时事件链”阶段，尚未进入“延迟任务编排”阶段。

### 3. 真正的下游业务消费者还没落地

当前：

- `AccessLogAuditListener` 已有真实副作用
- 预约、销售、库存消费者仍以日志模拟为主

这没有错，但意味着：

- 当前 MQ 仍主要是基础设施骨架
- 真正的统计、预警、提醒、对账类下游还没接上

### 4. 事件模型还不完整

当前消息模型更多是：

- `appointment.created`
- `sales.created`
- `inventory.changed`

而不是完整的业务状态事件集。

如果后续要扩真实读模型，还需要先补：

- 修改 / 删除语义
- 更细的业务状态边界
- 消息补偿口径

## 六、PR13.5 真正应该解决什么

### P0：必须先补“正确性”

#### 1. 库存扣减先定死为“原子库存 SQL”方案

这是第一优先级，优先于 Redis 锁，优先于继续加 MQ 花活。

`PR13.5` 当前直接定死：

- 以数据库原子库存 SQL 作为主方案

落地方式不是只改一条扣减 SQL，而是把所有库存变动入口统一收口到底层原子库存增减动作：

- 增库存：原子加
- 减库存：原子减并校验库存不能为负
- 更新类场景：先回滚旧影响，再施加新影响，但底层库存动作仍然走原子 SQL

当前阶段不建议优先把库存正确性的第一责任交给 Redis 分布式锁。

#### 2. 给写接口补请求级幂等

建议覆盖：

- 销售创建
- 库存创建
- 预约创建

落地方式可以先采用：

- 请求头或请求体中的幂等键
- Redis `SETNX + TTL`

#### 3. 修复 `getUserById()` 空值安全

这是当前一个实际 bug 风险，不应只当优化项处理。

### P1：补缓存治理

#### 本轮必做

- `getUserById()` 空值安全
- `flowerDetail` / `user` 空值保护
- 热点详情接口 `sync = true`

#### 4. 补空值缓存或空值保护

至少先对：

- `flowerDetail`
- `user`

补上：

- 参数校验
- 空值短 TTL 占位
- 明确的 `null` 安全处理

#### 5. 热点 key 先加 `sync = true`

这是当前成本最低、收益最直接的击穿缓解手段。

#### 本轮可顺手做，但不阻塞

- 减少大范围 `allEntries = true`
- TTL 加随机抖动

#### 6. 减少大范围 `allEntries = true`

优先收缩花卉缓存失效粒度：

- 列表缓存必要时全清
- 详情缓存尽量按 `flowerId` 精确清理

#### 7. TTL 加随机抖动

避免同类 key 在接近时间集中失效。

### P1：补 MQ 发布可靠性

#### 8. 先补 Publisher Confirm / Return

这是最低成本的一步，可以先建立发布端可观测性。

#### 9. 下一步再评估 Outbox

Outbox 很值得做，但它的重量比 Confirm / Return 更高。

当前建议是：

- `PR13.5` 先把发布确认接上
- 再决定是否在后续一轮单独做 `Outbox`

### P2：补真正的业务下游能力

#### 10. 真实下游消费者

下一轮再考虑：

- 销售日报统计
- 库存预警
- 预约通知渠道

#### 11. 延迟消息

等业务真的需要：

- 预约提醒
- 超时取消
- 延迟补偿

再补延迟消息能力。

## 七、推荐推进顺序

最推荐的顺序如下：

1. 库存扣减原子化或乐观锁
2. HTTP 写接口请求级幂等
3. 修复 `getUserById()` 空值安全
4. 热点缓存 `sync = true` + 空值保护
5. 收缩缓存失效粒度 + 增加 TTL 抖动
6. 补 Publisher Confirm / Return
7. 再决定是否引入 Outbox
8. 最后再扩真实下游消费者与延迟消息

## 八、最终结论

前述对话的**主判断是成立的**，尤其是下面这句最值得保留：

> 当前 Jasmine 的第一优先级，不是继续堆 Redis / MQ 技术点，而是先把“库存主链正确性 + 写接口幂等 + 消息不丢”这三件事补齐。

但如果要贴着当前 `next` 分支真实实现来写，建议再加上三点修正：

1. 访问日志在 MQ 发布失败时有本地同步日志兜底，不能和业务事件一概而论
2. 开发环境默认是 `memory` 缓存，Redis 缓存问题需要在 Redis 模式下专门验证
3. 当前事件模型本身还不完整，因此统计表 / 预警表不应在这一轮直接硬上

因此，`PR13.5` 最合理的定位不是“再接更多 Redis / MQ 技术点”，而是：

- 先补库存正确性
- 再补接口幂等
- 再补缓存稳态
- 再补 MQ 发布可靠性

这样后面的统计、预警、提醒和更复杂异步链路，才有稳定基础可接。

## 九、本轮已完成内容

当前 `PR13.5` 第一批代码已经实际落地：

### 1. 库存正确性

- 新增 `FlowerStockService`
- 在 `FlowerMapper` / `FlowerMapper.xml` 中补齐 compare-and-set 原子库存更新 SQL
- 销售创建、销售回滚、库存新增、库存修改、库存删除全部收口到底层原子库存变更动作

### 2. 创建接口请求级幂等

- 新增 `RequestIdempotencyService`
- 为以下接口接入创建防重：
  - `POST /sales`
  - `POST /inventory`
  - `POST /appointment`
- 前端 `admin` 侧新增 `X-Idempotency-Key` 自动生成逻辑

### 3. 缓存稳态

- 修复 `UserServiceImpl.getUserById()` 空值安全
- `user` / `menuList` / `flowerDetail` 增加 `sync = true`
- Redis 缓存不再禁用空值缓存
- 销售与库存写路径对 `flowerDetail` 改为按花卉精确失效，不再总是全量清空
- Redis 缓存 TTL 新增随机抖动：
  - `user` / `menuList` / `roleList`：30 分钟 + 0~5 分钟
  - `flowerList` / `flowerDetail`：10 分钟 + 0~2 分钟

### 4. MQ 发布侧可观测性

- `application.yml` 中启用 Publisher Confirm / Return / mandatory
- `RabbitTemplate` 增加 confirm / return 回调日志
- 发布消息增加 `CorrelationData`

## 十、本轮实际测试结果

### 1. 自动化验证

已通过：

- `./mvnw.cmd -DskipTests compile`
- `./mvnw.cmd "-Dtest=RequestIdempotencyServiceTest,RequestTraceFilterTest,AppointmentControllerTest" -DskipITs=true -DskipUTs=false test`

### 2. 手工联调验证

#### 幂等测试

已实际验证：

- `POST /sales`
- `POST /inventory`
- `POST /appointment`

结果：

- 第一次请求成功
- 第二次使用相同 `X-Idempotency-Key` 重放时返回 `20007`
- 当前语义符合“请求重复提交，请稍后再试！”

#### 空值安全测试

已实际验证：

- `GET /user/999999`
- `GET /flower/999999`

结果：

- 接口返回 `20000`
- `data=null`
- 不再出现 500 或空指针

#### 并发库存测试

已实际验证：

- 并发发起两笔销售请求，针对同一花卉抢占库存

本轮实际结果：

- 测试前库存：`43`
- 每笔销售数量：`22`
- 一笔成功，一笔失败
- 测试后库存：`21`

这说明：

- 当前原子库存更新已经挡住并发超卖
- 不会再出现两笔都成功导致库存变负的情况

#### MQ 队列状态测试

已通过 RabbitMQ 管理台 API 实际验证：

- `jasmine.appointment.notification`
- `jasmine.audit.access-log`
- `jasmine.sales.event-log`
- `jasmine.inventory.event-log`
- `jasmine.dlq`

结果：

- `messages=0`
- `messages_ready=0`
- `messages_unacknowledged=0`

说明当前请求触发后的消息都已经被正常消费，没有产生积压，也没有死信堆积。

### 3. 本轮测试中的边界说明

本轮在 `APP_CACHE_TYPE=redis` 模式下继续做了缓存与 TTL 验证。

#### Redis 缓存键验证

实际在 Redis 中读到的 Spring Cache key 包括：

- `user::1`
- `user::2`
- `flowerDetail::1`
- `flowerDetail::2`
- `flowerList::all`
- `menuList::1`

这说明当前这些缓存已经真实写入 Redis，而不是继续留在本地内存里。

#### TTL 抖动验证

本轮实际读到的 TTL 样例：

- `user::1`：`2004s`
- `user::2`：`2003s`
- `flowerDetail::1`：`580s`
- `flowerDetail::2`：`659s`
- `flowerList::all`：`650s`
- `menuList::1`：`1945s`

结合当前配置可判断：

- `user` / `menuList` / `roleList` 的基础 TTL 为 30 分钟，并额外增加 0~5 分钟抖动
- `flowerList` / `flowerDetail` 的基础 TTL 为 10 分钟，并额外增加 0~2 分钟抖动

虽然因为“写入时间不同 + 读取时已经过了一段时间”，TTL 不会刚好等于配置值，但当前同类 key 的 TTL 已经不是完全一致的固定值，说明随机抖动策略已经真实生效。
