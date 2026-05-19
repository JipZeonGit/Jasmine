# PR19 延时队列与站内信体系（到店预约提醒）

## 目标

基于 PR18 已完成的本地消息表（Outbox）基础，引入 RabbitMQ 死信延时架构，实现：
1. **精准延时触发**：在会员预约到店的倒数前 1 小时，系统自动生成后台站内信提醒店员准备。
2. **全局通知中心 UI 统筹**：在前端把 PR18 的库存小铃铛升级为“全局消息提醒中心”，采用双 Tab 选项卡（库存告警 / 预约待办），并统一管理红点数量。

## 方案设计

### 1. 数据库改造

**1.1 EventOutbox 支持自定义延迟头保护**
由于 RabbitMQ 通过基于 TTL 的死信实现队列延时需要向 Message 参数里注入 `expiration`（生存时间），且我们在 PR18 中确立了所有核心事件必须走 `event_outbox` 以防丢失的铁律，所以我们需要对 Outbox 机制提供拓展头（Headers）的支持：
- 在 `event_outbox` 表新增 `headers` 字段 (JSON 类型，允许为空)，用于保存投递属性（比如动态 TTL `expiration` 等参数）。

**1.2 引入统一前台站内信机制**
新建表 `site_message`，专门用于长期保存业务派发的瞬态或者持久态通知（不同于 `inventory_alert` 取决于实时库存快照的读状态模型，`site_message` 更多像是一个实体邮箱箱）：
```sql
CREATE TABLE `site_message` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `biz_type` varchar(64) NOT NULL COMMENT '业务类型，如 APPOINTMENT_REMINDER',
  `biz_id` varchar(64) NULL COMMENT '业务携带主键，用于跳转',
  `title` varchar(128) NOT NULL COMMENT '站内信标题',
  `content` text NOT NULL COMMENT '站内信正文',
  `is_read` tinyint(1) NOT NULL DEFAULT 0 COMMENT '0:未读 1:已读',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
);
```

### 2. RabbitMQ 拓扑定义与生产者流转

**2.1 死信延时架构定义（不用强求延时特装插件）**
利用 `RabbitMqTopologyConfig` 配置两组队列：
1. **死信驻留队列 (Delay Queue)**：`appointment.delay.queue`
   - 无任何消费者监听
   - 配置属性 `x-dead-letter-exchange = trade.event.exchange`
   - 配置属性 `x-dead-letter-routing-key = appointment.reminder.actual`
2. **终点唤醒队列 (Target Queue)**：`appointment.reminder.queue`
   - 绑定至前面的 Exchange，RoutingKey 为 `appointment.reminder.actual`

**2.2 Outbox 投递与 TTL 计算**
在 `AppointmentServiceImpl#createAppointment` 中：
- 计算 `DelayMs = 预约倒计时 = 预约时间Date.getTime() - System.currentTimeMillis() - 1小时（3600000ms）`。
- 构建带 `expiration` = `DelayMs` 参数的 Outbox 行，由 `OutboxRelay` 读取 Headers 原封不动赋予底层 AMQP Message 发送至驻留队列！

### 3. 后端消费与 API 打通

1. **预约提醒消费者 `AppointmentReminderListener`**：
   - 监听终点唤醒队列 `appointment.reminder.queue`
   - 解析预约快照体，存入 `site_message` 并设为`未读`状态。
2. **前台站内信网关接口 `SiteMessageController`**：
   - `GET /site-message/unread-count` —— 获取未读数字
   - `GET /site-message/list` —— 分页获取未读/全体提醒清单
   - `PUT /site-message/read/{id}` —— 将消息标记为已读

### 4. 前端小铃铛（消息中心）升级展现

在现存的 `AppLayout.vue` 的 Hover 浮窗内：
1. **数字角标合体**：全局 `Badge` 数值 = `低库存数量 + 站内未读信件数量`。
2. **选项卡分类**：气泡面框不再只平铺库存，而是使用 `el-tabs` 把明细包裹住。分为 `<el-tab-pane label="库存告警 (2)">` 和 `<el-tab-pane label="预约待办 (1)">`。
3. **视觉截断处理**：待办列表中展示内容体时，强制增加 `ellipsises` 约束备注字数上限（截断至 15 字符），附带 `标为已读` 按钮来消除红点。

## 测试验证边界

- 当新创一个 `距离现在少于 1小时` 的紧急预约单时，预期延时计算直接设为 `0` 或退化为即时直达。
- 在页面将消息 `标为已读` 时，前台红点数据可联动减少。
- 模拟关闭消费后观察 Delay 队列积压能否随时间自动“引爆”越渡至 Reminder 队列。

## 本轮不涉及

- 接入真实手机号短信通道或者微信模版消息订阅通道（依旧限于网页内部）
- 向外衍生复杂的聊天消息对话模型
