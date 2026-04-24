# PR13 MQ 契约

## 目标

这份文档用于收口 `PR13` 当前已经真实落地的 RabbitMQ 契约，方便后续继续扩展消息消费者、补幂等、补重试和排查死信。

这份文档记录的是“当前系统实际运行状态”，不是未来消息平台的理想设计稿。

## 一、交换机与路由键

### `jasmine.appointment.event`

用途：

- 预约相关业务事件

当前路由键：

- `appointment.created`

### `jasmine.audit.event`

用途：

- 审计与访问日志相关事件

当前路由键：

- `audit.access-log`

### `jasmine.trade.event`

用途：

- 销售与库存主业务事件

当前路由键：

- `sales.created`
- `inventory.changed`

### `jasmine.dlx`

用途：

- 统一死信交换机

当前路由键模式：

- `dead-letter.#`

## 二、队列与消费者

### `jasmine.appointment.notification`

绑定：

- 交换机：`jasmine.appointment.event`
- 路由键：`appointment.created`

消费者：

- `AppointmentNotificationListener`

当前行为：

- 校验 `appointmentId`、`vipId`、`vipPhone`
- 按 `appointmentId` 做幂等
- 当前只记录“模拟发送预约通知”日志，不直接接短信或企业微信

### `jasmine.audit.access-log`

绑定：

- 交换机：`jasmine.audit.event`
- 路由键：`audit.access-log`

消费者：

- `AccessLogAuditListener`

当前行为：

- 校验 `requestId`
- 按 `requestId` 做幂等
- 消费后把访问日志写回 `ACCESS_LOG`

### `jasmine.sales.event-log`

绑定：

- 交换机：`jasmine.trade.event`
- 路由键：`sales.created`

消费者：

- `SalesEventListener`

当前行为：

- 校验 `salesId`、`orderNo`
- 按 `salesId` 做幂等
- 当前只记录“模拟消费销售事件”日志，暂未写入独立统计表

### `jasmine.inventory.event-log`

绑定：

- 交换机：`jasmine.trade.event`
- 路由键：`inventory.changed`

消费者：

- `InventoryEventListener`

当前行为：

- 校验 `inventoryId`、`bizNo`
- 按 `inventoryId` 做幂等
- 当前只记录“模拟消费库存事件”日志，暂未写入独立预警表

### `jasmine.dlq`

绑定：

- 交换机：`jasmine.dlx`
- 路由键：`dead-letter.#`

用途：

- 收集被拒绝且不再重回原队列的失败消息

## 三、消息体

### `appointment.created`

消息类：

- `src/main/java/com/nfu/jasmine/infra/mq/message/AppointmentCreatedMessage.java`

字段：

- `appointmentId`
- `vipId`
- `vipName`
- `vipPhone`
- `appointmentTime`
- `content`
- `occurredAt`

### `audit.access-log`

消息类：

- `src/main/java/com/nfu/jasmine/infra/mq/message/AccessLogMessage.java`

字段：

- `traceId`
- `requestId`
- `userId`
- `username`
- `method`
- `uri`
- `status`
- `durationMs`
- `clientIp`
- `occurredAt`

### `sales.created`

消息类：

- `src/main/java/com/nfu/jasmine/infra/mq/message/SalesCreatedMessage.java`

字段：

- `salesId`
- `orderNo`
- `vipId`
- `operatorId`
- `itemCount`
- `totalAmount`
- `salesTime`
- `occurredAt`

### `inventory.changed`

消息类：

- `src/main/java/com/nfu/jasmine/infra/mq/message/InventoryChangedMessage.java`

字段：

- `inventoryId`
- `bizNo`
- `flowerId`
- `bizType`
- `quantity`
- `beforeStock`
- `afterStock`
- `operatorId`
- `bizTime`
- `occurredAt`

## 四、发布入口与发布时机

### 当前发布入口

- `appointment.created`
  - 发布入口：`AppointmentServiceImpl.createAppointment`
- `audit.access-log`
  - 发布入口：`RequestTraceFilter.doFilterInternal`
- `sales.created`
  - 发布入口：`SalesServiceImpl.saveSales`
- `inventory.changed`
  - 发布入口：
    - `InventoryServiceImpl.saveInventory`
    - `SalesServiceImpl.rebuildSalesItems`

### 当前发布时机约定

- 业务事件统一优先在事务提交后发布

当前已经遵守该约定的事件：

- `appointment.created`
- `sales.created`
- `inventory.changed`

原因：

- 避免下游收到一条最终没有真正落库成功的业务消息

### 当前边界说明

当前事件仍以“创建后事件”为主，尚未覆盖销售和库存的全部修改/删除语义。

因此当前这些事件更适合承载：

- 审计
- 通知
- 日志型消费

暂不直接用于维护强依赖一致性的统计表或预警表。

## 五、幂等规则

### 幂等 key 命名

定义位置：

- `src/main/java/com/nfu/jasmine/infra/mq/MqKeyNames.java`

当前规则：

- `jasmine:mq:idempotent:appointment-notification:{appointmentId}`
- `jasmine:mq:idempotent:access-log:{requestId}`
- `jasmine:mq:idempotent:sales-created:{salesId}`
- `jasmine:mq:idempotent:inventory-changed:{inventoryId}`

### 幂等实现

实现位置：

- `src/main/java/com/nfu/jasmine/infra/mq/support/MqIdempotencyService.java`

当前策略：

- 优先写 Redis 幂等键
- Redis 不可用时退回本地内存兜底
- 默认幂等键 TTL 为 24 小时

### 当前直接跳过的情况

- 被判定为重复消费的消息

这类消息不会进入死信，而是记日志后直接返回。

## 六、重试规则

配置位置：

- `src/main/java/com/nfu/jasmine/infra/mq/config/RabbitMqTopologyConfig.java`
- `src/main/resources/application.yml`

当前统一策略：

- 最大消费次数：`3`
- 退避策略：指数退避
- 初始间隔：`1000ms`
- 倍数：`2.0`
- 最大间隔：`10000ms`
- 重试耗尽后通过 `RejectAndDontRequeueRecoverer` 拒绝并进入死信

当前直接判定为不重试的异常：

- `AmqpRejectAndDontRequeueException`
- `MessageConversionException`

补充说明：

- 日志里出现 `Retry: count=0` 只表示消息第一次进入重试模板
- 只有继续出现更高计数，才表示异常后的真正重复尝试

## 七、失败分类规则

### 直接进入死信的情况

当前这类情况会直接拒绝并不重回原队列：

- 消息对象本身为 `null`
- 缺少关键业务字段，例如：
  - `appointmentId`
  - `requestId`
  - `salesId`
  - `inventoryId`
  - `orderNo`
  - `bizNo`
- JSON 与消费者类型不匹配导致的反序列化失败

实现位置：

- `src/main/java/com/nfu/jasmine/infra/mq/support/MqMessageSupport.java`

### 先本地重试，再决定是否死信的情况

当前这类情况会先进入统一重试策略：

- 数据库连接抖动
- Redis 临时不可用
- 未来下游扩展消费者时的瞬时依赖故障

如果重试仍失败，则进入 `jasmine.dlq`。

## 八、死信处理约定

- 所有业务队列统一进入 `jasmine.dlq`
- 死信排查手册：`docs/upgrade/pr13-dead-letter-runbook.md`
- 坏消息默认不直接重放
- 瞬时异常导致的死信，先修依赖再决定是否补发

## 九、测试与联调状态

### 自动化测试

- MQ 行为测试：`src/test/java/com/nfu/jasmine/infra/mq/RabbitMqBehaviorIT.java`
- 集成测试容器基线：`src/test/java/com/nfu/jasmine/config/AbstractIntegrationTest.java`
- 集成测试配置：`src/test/resources/application-integration.yml`

当前已覆盖的行为包括：

- 重试成功
- 重试耗尽后死信
- 幂等跳过重复消费
- 坏消息死信
- 事务提交后发布
- 事务回滚不发布

补充说明：

- 在无 Docker 环境中，这类 Testcontainers 集成测试会按预期跳过

### 手工联调

本轮手工联调已经确认：

- `ACCESS_LOG` 异步链正常
- `inventory.changed` 正常发布并消费
- `sales.created` 正常发布并消费
- RabbitMQ 管理台中 `Ready` / `Unacked` / `Total` 最终归零
- `jasmine.dlq` 未出现异常积压

## 十、当前状态说明

当前 `PR13` 的 MQ 能力已经具备：

- 跑通链路
- 固化命名
- 事务后发布
- Redis 幂等
- 本地重试
- 死信观察与排障
- 专门的行为测试

后续如果继续演进，更适合在这套稳定骨架上新增真实下游消费者，而不是回头再补基础设施兜底。
