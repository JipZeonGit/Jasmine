# PR13 最终阶段总结

## 背景

`PR13` 前两个阶段已经完成了 RabbitMQ 基础设施接入、拓扑声明、预约/访问日志/销售/库存四条事件链路、消费幂等第一版以及死信与消息契约第一版。

最终阶段的目标不是继续扩交换机和队列，也不是把系统一下子改成复杂消息平台，而是把当前这套消息骨架收口成“可以稳定跑、可以测试、可以排障、可以继续维护”的状态。

本阶段最终完成了四件事：

1. MQ 重试策略第一版
2. 死信排查手册
3. MQ 行为测试
4. 契约文档收口

## 一、重试策略第一版已完成

### 已落地内容

- 在 `RabbitMqTopologyConfig.rabbitListenerContainerFactory` 中挂载统一的 `RetryOperationsInterceptor`
- 保持 `defaultRequeueRejected=false`，避免异常消息无限回原队列
- 对可恢复异常启用本地重试，对不可恢复异常直接拒绝
- 重试参数已外化到 `app.mq.retry.*`

当前默认策略：

- 最大消费次数：`3`
- 退避策略：指数退避
- 初始间隔：`1000ms`
- 倍数：`2.0`
- 最大间隔：`10000ms`
- 重试耗尽后由 `RejectAndDontRequeueRecoverer` 拒绝并进入死信

当前直接判定为不重试的异常：

- `AmqpRejectAndDontRequeueException`
- `MessageConversionException`

### 本阶段结论

`PR13` 当前已经不再是“一次失败即永久死信”的粗放策略，而是具备了第一版可恢复异常重试能力。

## 二、死信排查手册已完成

### 已落地内容

- 新增 `docs/upgrade/pr13-dead-letter-runbook.md`

手册已经覆盖：

- 如何在 RabbitMQ 管理台查看 `jasmine.dlq`
- 如何解读 `x-death`
- 当前常见死信原因分类
- 哪些消息适合补发，哪些不适合直接重放
- 日常巡检建议

### 本阶段结论

`PR13` 当前已经不只是“有 DLQ”，而是具备了实际可用的排障入口和排查说明。

## 三、MQ 行为测试已完成

### 已落地内容

- 新增 `src/test/java/com/nfu/jasmine/infra/mq/RabbitMqBehaviorIT.java`
- 新增测试专用 MQ 探针 listener 和队列，用来验证基础设施行为而不是改业务逻辑本身

当前覆盖的行为包括：

- 可恢复异常重试后成功
- 重试耗尽后进入死信
- 幂等键命中后跳过重复消费
- 坏消息进入统一死信队列
- 事务提交后发布业务事件
- 事务回滚时不发布业务事件

同时，集成测试基线继续保留：

- `AbstractIntegrationTest` 中的 MySQL + Redis + RabbitMQ Testcontainers
- `application-integration.yml` 中的 rabbit 健康检查与测试重试参数

### 验证说明

- 在具备 Docker 的环境中，上述 MQ 行为测试会使用真实 RabbitMQ 跑集成验证
- 在没有 Docker 的本地环境中，`Testcontainers` 会自动跳过这类测试，这属于预期行为，不代表实现无效

### 本阶段结论

`PR13` 当前已经具备了专门的 MQ 行为测试，不再完全依赖人工点点点看日志。

## 四、契约文档收口已完成

### 已落地内容

`docs/upgrade/pr13-mq-contract.md` 已补齐并更新为当前真实状态，覆盖：

- 全部交换机、队列、路由键
- 全部消息体字段
- 发布入口与发布时机
- 当前消费者行为
- 幂等 key 规则与实现策略
- 重试规则
- 失败分类规则
- 死信处理约定
- 测试与联调状态

### 本阶段结论

`PR13` 的 MQ 契约已经从“命名草稿”升级成“可运行、可排障、可验证”的运行文档。

## 五、为什么这轮没有继续做下游建表消费者

本阶段最终明确决定：

- 暂不在 `PR13` 内继续推进 `daily_sales_summary`
- 暂不在 `PR13` 内继续推进 `inventory_alert`
- 预约通知仍保持日志模拟，不提前引入通知记录表

原因不是这些方向永远没价值，而是当前时机不合适：

1. 当前事件主要覆盖 `created`

- `sales.created`
- `inventory.changed`

但销售、库存本身还有修改和删除路径，如果现在直接让下游去维护持久化读模型，很容易出现读模型与主表漂移。

2. 当前系统已经有同步统计能力

- 销售页的“今日经营统计”已经由同步聚合提供
- 这轮更应该优先把 MQ 的可靠性、可测性和可观测性做稳

3. PR13 的定位仍然是“消息基础骨架”

- 先稳消息边界
- 再扩复杂读模型

这也意味着后面如果真要推进统计表或预警表，更适合放在下一轮专门做，而不是混在 `PR13` 最终收尾里一起膨胀。

## 六、本阶段最终验收结果

### 代码与测试层面

- 离线 `verify` 已通过
- 单元测试正常通过
- MQ 行为测试已纳入测试集
- 在无 Docker 环境下，相关集成测试被 `Testcontainers` 按预期跳过

### 手工联调层面

已确认：

- RabbitMQ 交换机和队列全部声明成功
- `ACCESS_LOG` 异步链正常
- 手工库存新增后 `InventoryEventListener` 正常消费
- 销售单创建后 `SalesEventListener` 与 `InventoryEventListener` 都能正常消费
- RabbitMQ 管理台中 `Ready` / `Unacked` / `Total` 最终归零
- `jasmine.dlq` 在本轮联调中没有出现异常积压

### 日志观察层面

本轮手工联调里出现的：

- `Publishing message`
- `Received message`
- `Processing [GenericMessage ...]`
- `Retry: count=0`

都属于当前打开 DEBUG 日志后的正常表现。

其中 `Retry: count=0` 只表示消息进入了重试模板的第一次执行，并不等于已经发生了真正重试；只有后续继续出现更高计数，才代表异常后的重复尝试。

## 七、最终结论

`PR13` 最终已经完成收尾，当前可以明确确认：

- RabbitMQ 基础设施接入完成
- 四条消息链路真实可跑
- Redis 幂等第一版完成
- 死信与失败分类第一版完成
- 本地重试第一版完成
- 死信排查手册完成
- MQ 行为测试完成
- 契约文档收口完成

也就是说，`PR13` 当前已经不是“RabbitMQ 接入演示”，而是形成了一个具备以下特征的消息基础骨架：

- 能发布
- 能消费
- 能幂等
- 能重试
- 能进死信
- 能排障
- 能测试
- 能继续扩展

下一轮如果继续演进，更适合围绕“消息驱动下的真实下游能力”展开，而不是回头再补基础设施兜底。
