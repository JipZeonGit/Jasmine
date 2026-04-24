# PR13 死信排查手册

## 目标

这份手册用于处理 `PR13` 当前 RabbitMQ 接入后的死信问题，帮助快速判断：

- 消息为什么进了 `jasmine.dlq`
- 属于坏消息、瞬时故障还是重试耗尽
- 后续应该丢弃、修复后补发，还是继续观察

## 当前死信入口

当前统一死信交换机与队列：

- dead letter exchange: `jasmine.dlx`
- dead letter queue: `jasmine.dlq`

当前会进入这条链路的来源队列包括：

- `jasmine.appointment.notification`
- `jasmine.audit.access-log`
- `jasmine.sales.event-log`
- `jasmine.inventory.event-log`

## 如何查看死信消息

### 方式 1：RabbitMQ 管理台

进入：

- `Queues and Streams`
- 选择 `jasmine.dlq`
- 在页面底部使用 `Get messages`

重点观察：

- payload
- headers
- `x-death`
- `routing keys`
- `content_type`

### 方式 2：应用侧日志

优先搜索这些关键词：

- `MQ 消息缺少必要字段`
- `ListenerExecutionFailedException`
- `RejectAndDontRequeueRecoverer`
- `MessageConversionException`

如果是重试耗尽，通常还能在最后一轮异常前看到同一条消息被重复处理的日志痕迹。

## 如何读 `x-death`

`x-death` 是 RabbitMQ 自动补的死亡记录，最有用的字段通常是：

- `queue`: 原始队列
- `reason`: 死亡原因
- `count`: 死亡次数
- `exchange`: 最初进入的交换机
- `routing-keys`: 原始路由键

当前项目里最常见的判断方式：

- `reason=rejected`
  说明消息被消费者拒绝了，通常是字段校验失败，或重试耗尽后被 recoverer 拒绝
- `count=1`
  通常说明第一次进入死信；如果前面配置了本地重试，这个次数不等于应用内重试次数

## 当前失败分类

### 1. 坏消息

典型表现：

- 消息体为 `null`
- 缺少关键业务字段
- JSON 结构和消费者消息类不匹配

当前处理：

- 不重试
- 直接拒绝
- 进入 `jasmine.dlq`

典型排查方向：

- 发布端是否漏填字段
- 消息类是否改了字段名但没有同步契约文档
- 旧消息是否还在投递到新消费者

### 2. 瞬时可恢复异常

典型表现：

- 数据库连接抖动
- Redis 短暂不可达
- 短时资源竞争

当前处理：

- 本地重试 3 次
- 指数退避：1s -> 2s -> 最多 10s
- 如果最终仍失败，拒绝并进入 `jasmine.dlq`

典型排查方向：

- 同时查看应用异常堆栈和中间件健康状态
- 先确认下游资源是否恢复
- 再判断是否需要补发

### 3. 重试耗尽

典型表现：

- 同一条消息在短时间内重复触发消费
- 最终仍失败并进入 `jasmine.dlq`

当前处理：

- 统一通过 `RejectAndDontRequeueRecoverer` 放弃重试
- 进入统一死信队列等待人工排查

## 当前常见问题与建议动作

### 场景 1：关键字段缺失

表现：

- 应用日志包含 `MQ 消息缺少必要字段`
- `x-death.reason=rejected`

处理建议：

1. 先修发布端
2. 确认契约文档字段定义
3. 这类坏消息通常不要原样回放

### 场景 2：反序列化失败

表现：

- 日志里出现 `MessageConversionException`
- 死信消息 body 看起来与当前消费者 POJO 对不上

处理建议：

1. 对照 `pr13-mq-contract.md` 检查 schema
2. 判断是否存在历史消息与新版本消费者不兼容
3. 不要直接重放未知结构消息

### 场景 3：下游短时异常

表现：

- 日志堆栈更像基础设施异常，而不是字段错误
- 最终消息进入死信

处理建议：

1. 先确认下游依赖已恢复
2. 明确这条消息是否已经产生部分副作用
3. 再决定是否手工补发

## 是否可以手工补发

可以，但要先判断消息类型。

### 适合补发的情况

- 基础设施瞬时异常导致的重试耗尽
- 消息内容本身正确
- 下游副作用具备幂等保护

### 不适合直接补发的情况

- 缺字段坏消息
- 反序列化失败消息
- 无法确认消息是否已经部分执行

## 日常巡检建议

建议至少关注：

- `jasmine.dlq` 的 `Ready` 是否大于 0
- 同时间段内是否出现 MQ 消费失败日志
- 重试后仍失败的消息是否集中在某一类消费者

如果 `jasmine.dlq` 长时间积压，优先确认：

1. 是坏消息持续产生，还是单次依赖故障
2. 是否只集中在一个 routing key
3. 是否需要暂停某个新增消费者的继续放量

## 配套文档

- MQ 契约：`docs/upgrade/pr13-mq-contract.md`
- PR13 阶段记录：`docs/upgrade/pr13-rabbitmq-bootstrap.md`
