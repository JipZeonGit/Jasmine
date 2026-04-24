# PR18 高并发与一致性增强（第一版）

## 目标

本 PR 基于已完成的 `PR13` MQ 基础设施、`PR13.5` 库存原子更新、`PR14` 部署基线和 `PR15` 新前端，实现三个核心闭环：

1. **业务事件发布可靠性闭环**：解决"主业务成功但消息可能没发出去"的问题
2. **库存事件语义完整性**：让后续任何库存相关下游都能建立在"库存变动事件是完整的"这个前提上
3. **库存预警真实下游落地**：把已有真实字段支撑的"库存预警"落成第一批真正有业务价值的消费者

## 本轮内容

### 一、新增本地消息表（Outbox）机制

#### 1.1 数据库表扩展

**迁移脚本**：`src/main/resources/db/migration/V6__event_outbox_and_inventory_alert.sql`

新增 `event_outbox` 表：
```sql
CREATE TABLE `event_outbox` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `event_type` varchar(64) NOT NULL COMMENT '事件类型，如 appointment.created / sales.created / inventory.changed',
  `exchange` varchar(128) NOT NULL COMMENT '目标交换机',
  `routing_key` varchar(128) NOT NULL COMMENT '目标路由键',
  `payload` text NOT NULL COMMENT '消息体 JSON',
  `status` varchar(16) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING / SENT / FAILED',
  `retry_count` int NOT NULL DEFAULT 0 COMMENT '已重试次数',
  `next_retry_time` datetime DEFAULT NULL COMMENT '下次允许重试时间',
  `last_error` varchar(512) DEFAULT NULL COMMENT '最近一次发送失败原因',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '写入时间',
  `sent_at` datetime DEFAULT NULL COMMENT '发送成功时间'
)
```

#### 1.2 Outbox 基础设施

**核心类**：
- `EventOutbox`：本地消息表实体
- `OutboxService`：事务内写入 Outbox 的服务
- `OutboxRelay`：定时任务异步扫描并发送

**关键特性**：
- 主事务内写入，保证业务数据与消息原子性
- 5次重试机制，指数退避（1s, 2s, 4s, 8s, 16s）
- 失败消息置为 FAILED 状态，避免无休止重试

#### 1.3 业务事件发布改造

改造 `MqMessagePublisher` 的业务事件发布方法：
- `publishAppointmentCreatedAfterCommit` → 写 Outbox
- `publishSalesCreatedAfterCommit` → 写 Outbox  
- `publishInventoryChangedAfterCommit` → 写 Outbox

访问日志仍保持直接发送，已有同步日志兜底。

### 二、库存事件语义增强

#### 2.1 InventoryChangedMessage 字段扩展

**新增字段**：
```java
private String changeSource;  // 来源：MANUAL_INVENTORY / SALES_ORDER
private String changeAction;  // 动作：CREATE / UPDATE / DELETE / ROLLBACK
```

**新增常量类**：`InventoryChangeSource`

#### 2.2 库存变更事件补齐

补全所有库存变更入口的 `inventory.changed` 事件：

**手工库存**：
- `saveInventory`：ACTION_CREATE
- `updateInventory`：ACTION_UPDATE  
- `deleteInventory`：ACTION_DELETE

**销售单关联**：
- `saveSales` 内部：ACTION_CREATE
- `updateSales` 内部：先 ACTION_ROLLBACK 旧明细 → 再 ACTION_CREATE 新明细
- `deleteSales` 内部：ACTION_ROLLBACK

### 三、库存预警真实下游落地

#### 3.1 读模型表扩展

**新增 `inventory_alert` 表**：
```sql
CREATE TABLE `inventory_alert` (
  `id` int NOT NULL AUTO_INCREMENT,
  `flower_id` int NOT NULL COMMENT '花卉主数据 ID',
  `flower_name_snapshot` varchar(64) NOT NULL COMMENT '花卉名称快照',
  `safe_stock` int NOT NULL COMMENT '安全库存阈值',
  `current_stock` int NOT NULL COMMENT '当前库存',
  `alert_status` varchar(16) NOT NULL DEFAULT 'NORMAL' COMMENT 'NORMAL / LOW_STOCK',
  `last_trigger_time` datetime DEFAULT NULL COMMENT '最近一次触发低库存的时间',
  `last_recover_time` datetime DEFAULT NULL COMMENT '最近一次恢复正常的时间'
)
```

#### 3.2 库存预警服务

**核心类**：
- `InventoryAlert`：实体类
- `InventoryAlertService`：upsert 预警状态服务
- `InventoryAlertController`：低库存查询接口

**业务逻辑**：
- 收到 `inventory.changed` 事件后更新预警表
- 比较 `currentStock` 与 `safeStock`，设置 NORMAL/LOW_STOCK 状态
- 记录触发时间和恢复时间

#### 3.3 InventoryEventListener 升级

改造消费者从"日志模拟"升级为"预警读模型更新"：
- 幂等校验 → 预警状态更新
- 失败时记录日志但不影响主业务事务
- 不依赖复杂状态机，纯业务逻辑驱动

### 四、配置和基础设施调整

#### 4.1 定时任务启用

**注解**：`JasmineApplication.java` 添加 `@EnableScheduling`

**配置项**：
```yaml
app:
  outbox:
    relay:
      interval-ms: 5000
      batch-size: 50
```

#### 4.2 关键改造流程图

```
业务事务 → 主业务写入 + Outbox写入 → 事务提交 → OutboxRelay扫描 → 发送MQ → 消费端更新预警表
```

## 验证

### 自动化验证

- Outbox 写入测试
- 主事务成功但 Relay 可重发测试  
- MQ 瞬时失败后 Outbox 重试测试
- 库存新增/修改/删除/销售创建/销售删除后的库存事件测试
- 低库存预警生成/恢复测试

### 集成测试

- Testcontainers 环境 Outbox Relay 行为测试
- `inventory_alert` 消费链测试

### 手工联调验证

- 新增库存流水，观察是否生成/恢复低库存预警
- 创建销售单，观察库存预警是否变化  
- 删除销售单，观察库存回补后预警是否恢复
- 人工制造 MQ 发布失败，确认 Outbox 能补发

## 当前状态

### 已完成

- ✅ 业务事件发布可靠性闭环（Outbox 机制）
- ✅ 库存事件语义完整性（增强字段 + 补齐入口）
- ✅ 库存预警真实下游落地（预警表 + 服务 + 接口）
- ✅ RabbitMQ 消息转换修复（使用原生 AMQP Message 剥离 `__TypeId__` 头，解决 Jackson 反序列化 Bug）
- ✅ 前端路由守卫优化（剥夺 NotFound 权限并在重定向强制路由重载，解决动态刷新 404 Bug）
- ✅ 预警全栈集成与展现层优化（顶部铃铛圆角 Hover Popover 明细框、花卉表格低库存智能爆红高亮）

### 明确不纳入本轮

- Redis 分布式锁 / Redisson
- 秒杀 / 预扣减 / 热点库存专项方案
- 延迟消息体系
- 预约超时取消
- 预约提醒真实渠道接入
- 销售日报异步读模型
- 复杂单据状态机

## 说明

本 PR 不再是"把 Redis、MQ 再接一遍"，而是解决当前已经接上的能力还缺的几条闭环：

1. **业务事件不丢**：通过 Outbox 机制保证主业务成功则消息必达
2. **库存事件语义完整**：所有真正改 current_stock 的入口都发事件，不依赖 bizNo 猜来源
3. **库存预警落成第一批真实下游**：不再是日志模拟，而是能支撑真实业务的预警查询

这三个闭环一旦补齐，后续的延迟消息、预约提醒、销售日报、状态机等才有稳定基线可继续推进。

## 相关文件

- `src/main/resources/db/migration/V6__event_outbox_and_inventory_alert.sql`
- `src/main/java/com/nfu/jasmine/infra/outbox/`
- `src/main/java/com/nfu/jasmine/infra/mq/publisher/MqMessagePublisher.java`
- `src/main/java/com/nfu/jasmine/infra/mq/message/InventoryChangedMessage.java`
- `src/main/java/com/nfu/jasmine/infra/mq/message/InventoryChangeSource.java`
- `src/main/java/com/nfu/jasmine/infra/mq/listener/InventoryEventListener.java`
- `src/main/java/com/nfu/jasmine/inventory/alert/`
- `src/main/java/com/nfu/jasmine/InventoryController.java`