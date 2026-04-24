# PR18 高并发与一致性增强计划（第一版）

## 背景

`Jasmine` 当前已经完成：

- `PR12` 模块化单体重组
- `PR12.5` Redis 缓存收口
- `PR13` RabbitMQ 基础接入、重试、死信、消费幂等
- `PR13.5` 库存原子更新、请求级幂等、缓存稳态、发布确认
- `PR14` Docker / Ops / 部署基线
- `PR15` 新前端全量迁移

因此，`PR18` 不再是“把 Redis、MQ 再接一遍”，而是要解决当前已经接上的能力还缺哪几条闭环。

同时，路线图里的 `PR18` 原始范围偏大，并不需要在当前阶段全部落地。
本计划书会基于现在真实架构与真实使用场景，把 `PR18` 收口成一版可执行方案。

## 当前架构基线

### 1. 后端已经是模块化单体

当前后端目录按业务域组织，主结构已经稳定：

- `src/main/java/com/nfu/jasmine/iam`
- `src/main/java/com/nfu/jasmine/flower`
- `src/main/java/com/nfu/jasmine/inventory`
- `src/main/java/com/nfu/jasmine/sales`
- `src/main/java/com/nfu/jasmine/vip`
- `src/main/java/com/nfu/jasmine/appointment`
- `src/main/java/com/nfu/jasmine/infra`

这意味着 `PR18` 不需要再回头做结构级重组，重点应该放在业务正确性和基础设施闭环上。

### 2. Redis 当前已经承担两类职责

- 缓存
- 幂等

已落地能力包括：

- Spring Cache + Redis / memory 双模式
- 缓存 TTL 抖动
- MQ 消费幂等：`src/main/java/com/nfu/jasmine/infra/mq/support/MqIdempotencyService.java`
- 创建接口请求级幂等：`src/main/java/com/nfu/jasmine/infra/idempotency/RequestIdempotencyService.java`

### 3. RabbitMQ 当前已经有稳定骨架

已落地能力包括：

- 交换机 / 队列 / 路由键声明：`src/main/java/com/nfu/jasmine/infra/mq/config/RabbitMqTopologyConfig.java`
- 事务提交后发布：`src/main/java/com/nfu/jasmine/infra/mq/publisher/MqMessagePublisher.java`
- 重试、死信、消费幂等
- 预约 / 访问日志 / 销售 / 库存事件链

但当前业务事件仍然主要是：

- `appointment.created`
- `sales.created`
- `inventory.changed`

并且消费者侧仍以日志模拟为主：

- `src/main/java/com/nfu/jasmine/infra/mq/listener/AppointmentNotificationListener.java`
- `src/main/java/com/nfu/jasmine/infra/mq/listener/SalesEventListener.java`
- `src/main/java/com/nfu/jasmine/infra/mq/listener/InventoryEventListener.java`

### 4. 库存正确性主链已经从“普通 update”升级为原子库存变更

当前所有库存增减已经收口到：

- `src/main/java/com/nfu/jasmine/flower/application/support/FlowerStockService.java`
- `src/main/resources/mapper/cus/FlowerMapper.xml`

实现方式是 compare-and-set 原子库存更新。
这意味着当前库存主链已经不是 `PR18` 的第一阻塞点，`PR18` 不应再把重点放回“是否要先上 Redis 分布式锁”。

### 5. 前端与部署基线已经稳定

- 新前端唯一主线：`web/`
- NAS 部署主入口：`ops/prod/up.sh` / `ops/prod/down.sh`

因此，`PR18` 当前也不需要把主要精力放在前端迁移或基础部署整理上。

## 已完成但不需要在 PR18 重复做的内容

以下内容已经有第一版实现，不应在 `PR18` 再重复一轮：

- 创建接口请求级幂等
- MQ 消费幂等
- MQ 重试与死信第一版
- Redis 缓存稳态
- 花卉库存原子更新
- `RabbitTemplate` 发布确认 / Return 回调

`PR18` 应该直接承接“这些能力之上仍然缺的闭环”，而不是回头重做基础设施。

## 当前仍然存在的真实问题

### 1. 业务事件仍然可能“主业务成功，但消息悄悄没发出去”

当前 `MqMessagePublisher` 的行为仍然是：

- 事务提交后尝试发送
- 发送失败只记日志
- 不影响主业务事务

这能保证主业务链不被 MQ 可用性拖死，但也带来了一个明确风险：

> 主业务已经提交成功，但下游业务事件没有可靠落地，也没有后续补偿重发闭环。

这正是路线图里 `Outbox / 本地消息表` 需要承接的部分。

### 2. 事件语义还不完整，无法直接支撑真实下游读模型

当前消息虽然已经能跑，但事件覆盖面还不完整：

- `sales.created` 有
- `sales` 的修改 / 删除等价事件没有
- `inventory.changed` 并没有覆盖所有库存变更入口
  - 当前手工库存新增会发
  - 但库存修改、库存删除没有同步发
  - 销售删除导致的库存回补，也没有独立事件补回

这意味着：

- 当前消息链更适合“日志型 / 通知型 / 审计型”消费
- 还不适合直接支撑严格依赖全量事件的下游读模型

### 3. 库存预警有真实业务落点，但当前还没有落成真实消费者

当前花卉主数据已经有：

- `Flower.safeStock`
- `Flower.currentStock`

也就是说，“库存预警”并不是空想场景，而是项目里已经存在真实业务字段支撑的能力。
但现在还缺：

- 独立预警读模型
- 独立预警消费者
- 预警状态更新与查询入口

当前 `InventoryEventListener` 仍然只是日志模拟，这块是 `PR18` 很适合落地的第一批真实下游。

### 4. 路线图里有些项现在没有真实使用场景，不应该硬上

当前不适合直接纳入 `PR18` 主范围的包括：

#### 4.1 Redis 分布式锁 / Redisson

当前库存主链已经有数据库原子库存更新，且项目没有真实的高热点抢购场景。
在这个前提下，优先级高于数据库原子更新的 Redis 锁并不成立。

#### 4.2 秒杀 / 预扣减 / 热点库存专项方案

当前系统仍然是花店门店管理场景，并没有：

- 秒杀
- 抢购
- 高并发瞬时库存争抢

因此不应为了“会用 Redis”而提前引入热点库存方案。

#### 4.3 延迟消息体系

延迟消息适合真实存在如下场景时再补：

- 预约提醒
- 超时取消
- 延迟补偿

但当前预约实体 `src/main/java/com/nfu/jasmine/appointment/model/entity/Appointment.java` 里还没有状态机字段，预约通知也仍是日志模拟。
在这个阶段硬做延迟编排，会先把模型复杂化。

#### 4.4 单据状态机进一步完善

当前销售、库存、预约都不是围绕复杂状态机建模的。
如果没有真实的“草稿 / 已确认 / 已取消 / 已结算 / 已补偿”业务口径，这一轮不宜强推状态机。

#### 4.5 销售日报异步读模型

当前销售页已经有同步聚合能力：

- `src/main/java/com/nfu/jasmine/sales/application/impl/SalesServiceImpl.java`
- `getTodayBusinessSummary()`

在当前数据规模和页面使用方式下，它还没有显示出必须立刻改成异步读模型的压力。
因此它不是 `PR18` 的第一优先级。

## PR18 建议纳入的实际范围

结合当前架构和当前场景，`PR18` 建议只做三块：

### 一、补齐业务事件发布可靠性闭环

### 目标

解决“主业务成功但消息可能没发出去”的问题。

### 方案

新增本地消息表 / Outbox 机制，只覆盖真正重要的业务事件：

- `appointment.created`
- `sales.created`
- `inventory.changed`

不纳入 Outbox 的消息：

- `audit.access-log`

因为访问日志当前已有同步日志兜底，不值得把整套 Outbox 重量压到这条链上。

### 具体落地建议

#### 1. 新增数据库表

建议新增迁移，例如：

- `src/main/resources/db/migration/V6__event_outbox_and_inventory_alert.sql`

其中新增 `event_outbox` 表，最少包含：

- `id`
- `event_type`
- `exchange`
- `routing_key`
- `payload`
- `status`
- `retry_count`
- `next_retry_time`
- `last_error`
- `created_at`
- `sent_at`

### 2. 新增 Outbox 写入服务

建议新增：

- `src/main/java/com/nfu/jasmine/infra/outbox/`

把当前“事务提交后直接发 MQ”的业务事件发布改造成：

1. 主事务内写业务数据
2. 同事务写入 `event_outbox`
3. 由独立 Relay 任务异步扫描并发 MQ

### 3. 新增 Outbox Relay

可用 Spring 定时任务先做第一版，不必一上来就做复杂调度器。

建议能力包括：

- 扫描 `PENDING` / `RETRYING` 记录
- 发送成功后置为 `SENT`
- 发送失败后增加 `retry_count`
- 按退避策略更新 `next_retry_time`
- 超过阈值后置为 `FAILED`

### 4. 保留现有 MQ Confirm / Return 日志

当前的确认回调和 Return 回调仍然保留，用于：

- 观测发送情况
- 记录更完整的失败原因

Outbox 不是替代 Confirm / Return，而是把“日志可见”升级为“业务可补偿”。

## 二、补齐库存相关事件语义，先让事件覆盖真实库存变更入口

### 目标

让后续任何库存相关下游，都能建立在“库存变动事件是完整的”这个前提上。

### 当前缺口

当前 `inventory.changed` 并没有覆盖：

- 库存修改
- 库存删除
- 销售删除导致的库存回补

### 方案

`PR18` 先不急着把路由键炸成很多种，而是优先保证：

- 所有真正会改 `current_stock` 的入口，都要发库存变更事件

建议检查并补齐这些路径：

- `InventoryServiceImpl.saveInventory`
- `InventoryServiceImpl.updateInventory`
- `InventoryServiceImpl.deleteInventory`
- `SalesServiceImpl.saveSales`
- `SalesServiceImpl.updateSales`
- `SalesServiceImpl.deleteSales`

### 建议消息体增强

可在 `InventoryChangedMessage` 中补充必要字段，例如：

- `changeSource`
- `changeAction`

示例口径：

- `MANUAL_INVENTORY`
- `SALES_ORDER`
- `CREATE`
- `UPDATE`
- `DELETE`
- `ROLLBACK`

这样后面做库存预警或审计时，就不需要靠 `bizNo` 猜来源。

## 三、落地第一批真实下游消费者：库存预警读模型

### 目标

把当前已经有真实字段支撑的“库存预警”落成第一批真正有业务价值的消费者。

### 为什么选库存预警

因为它同时满足三点：

1. 当前已有真实字段
   - `safeStock`
   - `currentStock`

2. 当前已有库存事件骨架
   - `inventory.changed`

3. 不依赖复杂状态机

相比之下：

- 预约提醒还缺真实通知渠道
- 延迟取消还缺预约状态机
- 销售日报异步读模型当前收益没有库存预警直接

### 方案

新增库存预警读模型表，例如：

- `inventory_alert`

建议至少包含：

- `id`
- `flower_id`
- `flower_name_snapshot`
- `safe_stock`
- `current_stock`
- `alert_status`
- `last_trigger_time`
- `last_recover_time`
- `remark`

### 消费逻辑

由库存事件消费者驱动：

1. 收到 `inventory.changed`
2. 读取当前花卉主数据
3. 比较 `currentStock` 与 `safeStock`
4. upsert 预警记录

建议状态口径：

- `NORMAL`
- `LOW_STOCK`

### 查询入口

`PR18` 至少先补后端查询接口即可：

- 低库存预警列表
- 当前低库存数量

前端页面可以放到后续小轮次再补，不要求和 `PR18` 同一轮强绑定。

## PR18 明确不纳入本轮的内容

以下内容保留在路线图里，但当前不纳入 `PR18` 实施范围：

- Redis 分布式锁 / Redisson
- 秒杀 / 预扣减 / 热点库存专项方案
- 延迟消息体系
- 预约超时取消
- 预约提醒真实渠道接入
- 销售日报异步读模型
- 复杂单据状态机

## 推荐实施顺序

建议按下面顺序推进：

1. 新增 `event_outbox` 表与 Outbox 基础设施
2. 把预约 / 销售 / 库存业务事件改为写 Outbox
3. 实现 Outbox Relay 与失败重试
4. 补齐所有库存变更入口的 `inventory.changed`
5. 增强 `InventoryChangedMessage` 的来源与动作语义
6. 新增 `inventory_alert` 表
7. 把 `InventoryEventListener` 从“日志模拟”升级为“预警读模型更新”
8. 补低库存查询接口与最小化测试

## 验证方案

### 自动化验证

建议覆盖：

- Outbox 写入测试
- 主事务成功但 Relay 可重发测试
- MQ 瞬时失败后 Outbox 重试测试
- 库存新增 / 修改 / 删除 / 销售创建 / 销售删除后的库存事件测试
- 低库存预警生成 / 恢复测试

### 集成测试

继续沿用当前 Testcontainers 基线，至少补：

- MySQL + Redis + RabbitMQ 下的 Outbox Relay 行为测试
- `inventory_alert` 消费链测试

### 手工联调

建议联调路径：

1. 新增库存流水，观察是否生成 / 恢复低库存预警
2. 创建销售单，观察库存预警是否变化
3. 删除销售单，观察库存回补后预警是否恢复
4. 人工制造 MQ 发布失败，确认 Outbox 能补发

## 最终建议

当前 `PR18` 最值得做的，不是把路线图里的所有“高并发名词”一次性堆满，而是先完成这三个真正还缺的闭环：

1. 业务事件不丢
2. 库存事件语义完整
3. 库存预警落成第一批真实下游

这三件事一旦补齐，后面的：

- 延迟消息
- 预约提醒真实渠道
- 销售日报异步读模型
- 更复杂的状态机
- 更重的高热点治理

才会有稳定基线可继续推进。
