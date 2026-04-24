# PR19 延时队列与站内信体系 — 后端实施记录

## 目标

基于 PR18 已完成的本地消息表（Outbox）与库存预警体系，引入 RabbitMQ 死信延时架构，实现预约到店前 1 小时自动生成站内信提醒店员备货，并为前端统一消息中心提供后端接口支撑。

## 架构分析与计划修正

在动手之前，对照现有代码做了以下架构审查：

### 1. PR18 分支未合并问题

PR19 分支最初从 `next` 切出，但 `next` 上尚未合并 PR18（`pr18-concurrency-consistency`）的 Outbox 代码。通过 `git rebase origin/next`（PR18 已合入远端 `next`）将基线对齐，确保 Outbox 全套基础设施可用。

### 2. 延时方案选型：per-message TTL + 死信路由

计划书原定使用 `headers` JSON 字段存储 AMQP 投递属性，实际分析后改为更简洁的 `delay_ms` 列：

- **原方案**：在 `event_outbox` 表加 `headers`（JSON），存放 `expiration` 等 AMQP 属性
- **实际方案**：加 `delay_ms`（bigint），语义更清晰，OutboxRelay 读到后直接设为 AMQP `expiration`

不依赖 `rabbitmq_delayed_message_exchange` 插件，使用原生的 per-message TTL + 死信路由（DLX）模式。

### 3. 过期提醒的容错策略

当用户修改或删除预约后，已投入死信驻留队列的延时消息无法撤销。采用**消费端回查过滤**策略：消费者在生成站内信前先回查预约主库，确认预约仍然有效且未被删除。

## 本次改动

### 数据库迁移

#### `V7__site_message_and_outbox_delay.sql`

- 新建 `site_message` 站内信表，含 `biz_type`、`biz_id`、`title`、`content`、`is_read`、`created_at` 字段
- 在 `event_outbox` 表新增 `delay_ms` 列，支持延迟投递

---

### MQ 拓扑扩展

#### `JasmineMqConstants`

新增 4 个常量：

| 常量 | 值 | 用途 |
|------|------|------|
| `APPOINTMENT_DELAY_QUEUE` | `jasmine.appointment.delay` | 死信驻留队列（无消费者） |
| `APPOINTMENT_DELAY_ROUTING_KEY` | `appointment.delay` | 延时消息路由键 |
| `APPOINTMENT_REMINDER_QUEUE` | `jasmine.appointment.reminder` | 最终唤醒队列 |
| `APPOINTMENT_REMINDER_ROUTING_KEY` | `appointment.reminder` | 唤醒消息路由键 |

#### `RabbitMqTopologyConfig`

在现有拓扑声明中追加：

- **延时驻留队列** `appointment.delay.queue`：配置 `x-dead-letter-exchange` 指向预约交换机，`x-dead-letter-routing-key` 指向 `appointment.reminder`，无消费者监听
- **最终唤醒队列** `appointment.reminder.queue`：消费者在此监听，收到消息后生成站内信
- 对应的两组绑定关系

消息流转路径：

```
创建预约 → Outbox(delay_ms) → OutboxRelay(设置 expiration) → 延时驻留队列
                                                                ↓ (TTL 过期)
                                                           死信路由弹射
                                                                ↓
                                                          唤醒队列 → 消费者 → site_message
```

---

### Outbox 延时能力增强

#### `EventOutbox` 实体

新增 `delayMs` 字段（对应 `delay_ms` 列）。

#### `OutboxService`

新增带 `delayMs` 参数的 `save` 重载方法，原有的四参数方法委托到新方法（`delayMs = null`），不影响已有调用。

#### `OutboxRelay`

发送消息时，如果 `outbox.getDelayMs()` 不为空且大于 0，则将其设为 AMQP 消息的 `expiration` 属性，确保消息在死信驻留队列中按指定时长停留。

---

### 业务发布层

#### `MqMessagePublisher`

新增 `publishAppointmentReminderDelayed` 方法：

- 接收 `AppointmentCreatedMessage` 和 `delayMs`
- 消息投向延时驻留队列的路由键（`appointment.delay`）
- 当 `delayMs <= 0` 时退化为即时投递（不设 `expiration`）

#### `AppointmentServiceImpl`

在 `createAppointment` 方法中：

1. 保留原有的 `publishAppointmentCreatedAfterCommit` 调用
2. 追加延时提醒发布：`delayMs = 预约时间 - 当前时间 - 1小时`

---

### 消费端

#### `AppointmentReminderListener`（新建）

- 监听 `jasmine.appointment.reminder` 唤醒队列
- 幂等校验：通过 `MqKeyNames.appointmentReminder()` 避免重复生成
- 消费端回查：根据 `appointmentId` 查预约主库，已删除则跳过
- 构造站内信标题格式：`{会员名} 预约 {HH:mm} 到店`
- 备注内容截断至 50 字符
- 调用 `SiteMessageService.create()` 写入站内信

#### `MqKeyNames`

新增 `appointmentReminder()` 幂等键生成方法。

---

### 站内信模块（新建）

按项目模块化单体结构，新建 `infra.notification` 包：

| 文件 | 职责 |
|------|------|
| `model/entity/SiteMessage.java` | 站内信实体 |
| `persistence/mapper/SiteMessageMapper.java` | MyBatis-Plus Mapper |
| `service/SiteMessageService.java` | 写入、查询、标记已读 |
| `web/SiteMessageController.java` | REST 接口 |

#### 接口清单

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/site-message/unread-count` | 获取未读站内信数量 |
| GET | `/site-message/list` | 分页查询站内信列表 |
| PUT | `/site-message/read/{id}` | 标记单条为已读 |
| PUT | `/site-message/read-all` | 全部标记为已读 |

## 待完成

- [ ] 前端消息中心 UI 改造（小铃铛升级为双 Tab 选项卡）
- [ ] 集成测试补充
- [ ] 推送远端并更新项目状态文档
