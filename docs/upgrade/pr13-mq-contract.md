# PR13 MQ 契约草稿

## 目标

这份文档用于收口 `PR13` 当前已经落地的 RabbitMQ 契约，方便后续继续扩展消息消费者、补幂等、补重试和排查死信。

## 当前交换机

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

## 当前队列

### `jasmine.appointment.notification`

绑定：

- 交换机：`jasmine.appointment.event`
- 路由键：`appointment.created`

消费者：

- `AppointmentNotificationListener`

### `jasmine.audit.access-log`

绑定：

- 交换机：`jasmine.audit.event`
- 路由键：`audit.access-log`

消费者：

- `AccessLogAuditListener`

### `jasmine.sales.event-log`

绑定：

- 交换机：`jasmine.trade.event`
- 路由键：`sales.created`

消费者：

- `SalesEventListener`

### `jasmine.inventory.event-log`

绑定：

- 交换机：`jasmine.trade.event`
- 路由键：`inventory.changed`

消费者：

- `InventoryEventListener`

### `jasmine.dlq`

绑定：

- 交换机：`jasmine.dlx`
- 路由键：`dead-letter.#`

用途：

- 收集被拒绝且不再重回原队列的失败消息

## 当前消息体

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

## 当前幂等规则

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

## 当前失败分类规则

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

实现位置：

- `src/main/java/com/nfu/jasmine/infra/mq/support/MqMessageSupport.java`

### 直接跳过的情况

当前这类情况不会进入死信，而是记日志后跳过：

- 被判定为重复消费的消息

### 还未细化的情况

这些情况目前还没有做更细的分类重试策略：

- 下游服务临时不可用
- 外部通知渠道偶发失败
- 未来统计或预警消费者出现瞬时异常

后续会在 `PR13` 继续细化：

- 是否立即重试
- 是否延迟重试
- 是否直接死信

## 当前发布时机约定

当前业务事件统一约定：

- 优先在事务提交后发布

原因：

- 避免下游收到一条最终没有真正落库成功的业务消息

当前已经遵守该约定的事件：

- `appointment.created`
- `sales.created`
- `inventory.changed`

## 当前状态说明

这份契约文档对应的是 `PR13` 当前阶段的真实落地状态，不是最终版消息平台设计稿。

当前重点仍然是：

- 跑通链路
- 固化命名
- 控制范围
- 为后续幂等、死信、统计、库存联动继续演进打基础
