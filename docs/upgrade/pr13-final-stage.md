# PR13 最终阶段分析

## 背景

`PR13` 前两个阶段已经完成了 RabbitMQ 基础设施接入、拓扑声明、四条事件链路的发布与消费骨架、消费幂等第一版以及死信与坏消息的初步分类。

在 `pr13-rabbitmq-bootstrap.md` 最后提出了四项"建议优先做的内容"，本文档对这四项逐一进行现状评估、待决设计点梳理以及实施方案建议。

## 一、重试策略第一版

### 当前现状

- `RabbitMqTopologyConfig` 中 `rabbitListenerContainerFactory` 设置了 `setDefaultRequeueRejected(false)`
- 所有消费者通过 `MqMessageSupport.rejectIfNull` / `rejectIfBlank` 校验关键字段，校验失败时抛出 `AmqpRejectAndDontRequeueException`，消息直接进入死信
- 除此以外，没有任何重试机制
- 如果消费者在执行业务逻辑过程中遇到瞬时故障（如数据库连接超时、Redis 暂时不可达），消息同样会直接进入死信，不做任何重试

### 问题

当前策略过于简单粗暴：**一次失败即永久死信**。

在实际运行中，瞬时故障是最常见的消费失败原因（网络抖动、连接池满、下游短暂不可用），直接丢进死信意味着需要人工干预才能恢复，这不合理。

### 需要区分的三类异常

| 异常分类 | 典型场景 | 处理策略 |
|---------|---------|---------|
| **不可恢复异常** | 消息体 null、关键字段缺失、反序列化失败、业务校验不通过 | 直接拒绝，进入死信 |
| **瞬时可恢复异常** | 数据库连接超时、Redis 短暂不可达、HTTP 调用超时 | 本地重试若干次，间隔递增 |
| **长期不可恢复异常** | 本地重试次数耗尽仍未成功 | 放弃重试，进入死信 |

### 实施方案

在 `rabbitListenerContainerFactory` 上配置 Spring Retry 的 `RetryInterceptor`：

```
最大重试次数：3
退避策略：指数退避，初始间隔 1 秒，倍数 2，上限 10 秒
不可恢复异常白名单：AmqpRejectAndDontRequeueException、MessageConversionException
```

具体实现方式：

1. 在 `RabbitMqTopologyConfig.rabbitListenerContainerFactory` 中通过 `factory.setAdviceChain(retryInterceptor)` 注入重试拦截器
2. 可恢复异常由 Spring Retry 自动重试，超过最大次数后由 `RejectAndDontRequeueRecoverer` 最终拒绝入死信
3. 不可恢复异常（如 `AmqpRejectAndDontRequeueException`）应在分类器中标记为不重试，直接进死信

### 待决设计点

- 最大重试次数取 3 还是 5？建议先取 3，观察死信量后再调整
- 是否需要把重试参数外化为配置项（`app.mq.retry.max-attempts`、`app.mq.retry.initial-interval-ms`）？建议第一版先写死常量，后续确定需要动态调整时再外化
- 是否需要对不同队列配置不同的重试策略？当前四条链路性质相近，建议第一版统一策略

---

## 二、下游业务消费者第一版

### 当前现状

四个消费者的消费逻辑全部停留在打日志阶段：

| 消费者 | 当前行为 | 代码中的注释原文 |
|-------|---------|---------------|
| `SalesEventListener` | `log.info("模拟消费销售事件")` | "后面再让统计或审计真正接这个事件做下游处理" |
| `InventoryEventListener` | `log.info("模拟消费库存事件")` | "后面要接预警、审计或统计时不用再回头改主业务事务" |
| `AppointmentNotificationListener` | `log.info("模拟发送预约通知")` | "再接短信或企业微信之类的真实通知渠道" |
| `AccessLogAuditListener` | 写结构化日志到 logback | 已有真实副作用，当前不需要改动 |

### 需要决策的问题

#### 2.1 销售事件消费者

销售事件消费后最自然的落点是**日维度销售汇总**。

当前数据库中已有 `sales` 和 `sales_item` 表，但没有独立的汇总统计表。

**选项 A**：新建 `daily_sales_summary` 表

```sql
CREATE TABLE `daily_sales_summary` (
  `id`            int NOT NULL AUTO_INCREMENT,
  `summary_date`  date NOT NULL,
  `order_count`   int NOT NULL DEFAULT 0,
  `total_amount`  decimal(12,2) NOT NULL DEFAULT 0,
  `total_cost`    decimal(12,2) NOT NULL DEFAULT 0,
  `gross_profit`  decimal(12,2) NOT NULL DEFAULT 0,
  `updated_at`    datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_summary_date` (`summary_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
```

消费者在收到 `sales.created` 事件后，按 `salesTime` 提取日期，对该日汇总行做 upsert：订单数 +1、总金额累加、毛利润累加。

优势：

- 前端可以直接查询汇总表展示当日/历史销售数据，不再需要每次实时聚合 `sales` + `sales_item`
- 统计逻辑与主业务事务解耦

风险：

- 如果消费者异常或消息丢失，汇总数据会与 `sales` 表不一致
- 需要考虑后续补数据或对账机制

**选项 B**：暂不建表，先把消费者改为查询聚合并写日志

把 `log.info("模拟消费")` 改为真正查询 `sales` + `sales_item` 做一次实时聚合，将结果写入业务日志。

优势：

- 不引入新表，风险最小
- 验证消费链路的真实性

劣势：

- 没有持久化的统计结果，前端仍然需要实时聚合

#### 2.2 库存事件消费者

当前 `flower` 表已有 `safe_stock`（安全库存）和 `current_stock`（当前库存）字段。

**选项 A**：消费者检查库存阈值，触发预警

消费者在收到 `inventory.changed` 事件后，查询对应花材的 `current_stock` 和 `safe_stock`，如果 `current_stock < safe_stock`，则：

- 写一张 `inventory_alert` 表记录预警事件
- 或者直接写一条 WARN 级别日志

```sql
CREATE TABLE `inventory_alert` (
  `id`            int NOT NULL AUTO_INCREMENT,
  `flower_id`     int NOT NULL,
  `flower_name`   varchar(50) NOT NULL,
  `current_stock` int NOT NULL,
  `safe_stock`    int NOT NULL,
  `alerted_at`    datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_alert_flower_id` (`flower_id`),
  KEY `idx_alert_alerted_at` (`alerted_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
```

**选项 B**：不建新表，消费者只做日志预警

收到事件后检查阈值，如果触发预警只写 WARN 日志，不建表。

优势：改动最小。劣势：预警信息只在日志中，不可查询不可展示。

#### 2.3 预约通知消费者

**选项 A**：接入真实通知渠道

需要确定具体要接什么渠道：短信 SDK、企业微信 Webhook、还是其他。这取决于项目当前的外部集成能力。

**选项 B**：写通知发送记录表

消费者收到预约事件后，写一张 `notification_log` 表，记录"应该发送的通知"。真实发送渠道后续接入时直接查这张表补发。

**选项 C**：维持现状

预约通知的真实渠道尚未确定，继续保持日志模拟，不做改动。

### 建议

| 消费者 | 建议选项 | 理由 |
|-------|---------|------|
| `SalesEventListener` | 选项 A（建 `daily_sales_summary` 表） | 前端已经有销售管理页面，汇总表能直接支撑后续的仪表盘或报表需求 |
| `InventoryEventListener` | 选项 A（建 `inventory_alert` 表） | `flower.safe_stock` 已经存在，检查逻辑简单，预警记录持久化后可在前端展示或做批量查询 |
| `AppointmentNotificationListener` | 选项 C（维持现状） | 真实通知渠道未确定，过早建表反而增加维护负担 |

---

## 三、死信消息观察与排查手册

### 当前现状

- 死信队列 `jasmine.dlq` 已声明
- 所有业务队列均绑定了死信交换机 `jasmine.dlx`
- 消费者中所有校验失败和不可恢复异常都会通过 `AmqpRejectAndDontRequeueException` 将消息路由到死信队列
- 但是没有任何文档说明如何观察和排查死信消息

### 排查手册应覆盖的内容

1. **如何查看死信消息**
   - RabbitMQ 管理台 → Queues → `jasmine.dlq` → Get messages
   - 通过消息头的 `x-death` 字段判断原始队列、死亡原因、死亡次数

2. **常见死信原因分类**

   | 死信原因 | 典型表现 | 排查方向 |
   |---------|---------|---------|
   | 消息体为 null 或缺少关键字段 | `x-death.reason=rejected`，应用日志有 `MQ 消息缺少必要字段` | 检查发布端是否正常构建消息体 |
   | 反序列化失败 | `MessageConversionException`，消息体内容与消费者期望的 POJO 不匹配 | 检查消息体 JSON 结构是否与消息类字段一致 |
   | 消费者业务异常且重试耗尽 | `x-death.count` 等于最大重试次数 | 检查消费者日志中最后一轮异常堆栈 |
   | 队列 TTL 过期 | `x-death.reason=expired` | 检查队列是否配置了 `x-message-ttl` |

3. **死信消息的处理策略**
   - 坏消息（反序列化失败、字段缺失）：分析原因后丢弃或归档
   - 瞬时故障导致的死信：修复下游问题后，从 `jasmine.dlq` 手工重新发布到原始交换机
   - 重复消息导致的死信：幂等机制正常工作的情况下不应出现在死信中（重复消息会被跳过而非拒绝）

4. **日常巡检建议**
   - 定期检查 `jasmine.dlq` 的 `Ready` 数值
   - 如果 `Ready > 0`，说明存在未处理的死信消息
   - 配合后端 WARN 日志关键字 `MQ 消息缺少必要字段` 进行交叉定位

### 实施方案

将上述内容整理为独立文档 `docs/upgrade/pr13-dead-letter-runbook.md`。

---

## 四、文档收口

### 当前现状

`docs/upgrade/pr13-mq-contract.md` 已经覆盖：

- 全部交换机和队列声明
- 全部消息体字段定义
- 幂等 key 命名规则和实现位置
- 失败分类规则（直接死信、直接跳过、还未细化）
- 发布时机约定

### 待完善的内容

| 章节 | 现状 | 需要更新的时机 |
|-----|------|-------------|
| 失败分类规则 → "还未细化的情况" | 明确标注了"后续会继续细化" | 重试策略第一版实施后，需更新为具体的重试参数和异常分类规则 |
| 消费者行为描述 | 只写了消费者类名 | 下游业务消费者实施后，需补充每个消费者的实际副作用描述 |
| 死信处理策略 | 未记录 | 排查手册完成后，在契约文档中引用排查手册链接 |

### 实施方案

不需要独立启动任务，在上述三项完成后同步更新 `pr13-mq-contract.md` 即可。

---

## 建议实施顺序

```
第一步：重试策略
  ↓ 不依赖任何业务决策，纯基础设施改动
第二步：死信排查手册
  ↓ 纯文档工作，重试策略上线后有更完整的场景可以收录
第三步：下游业务消费者
  ↓ 需要确认建表方案后再实施
第四步：文档收口
  ↓ 跟随前三步的产出同步更新
```

其中第一步和第二步可以立即启动，不需要额外的业务决策。

第三步需要确认：

1. 是否新建 `daily_sales_summary` 表？
2. 是否新建 `inventory_alert` 表？
3. 预约通知是否维持现状？

确认后即可实施。
