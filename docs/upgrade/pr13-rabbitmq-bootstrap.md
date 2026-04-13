# PR13 RabbitMQ 第一阶段记录

## 本阶段目标

`PR13` 第一阶段的目标不是把系统一下子改成全异步，而是先把 RabbitMQ 作为正式基础设施接进来，并跑通最适合当前项目阶段的两条链路：

- 预约创建后的异步通知
- 访问日志的异步审计

这一阶段更强调：

- RabbitMQ 基础设施接入成功
- 交换机、队列、路由键命名收口
- 生产者 / 消费者链路真实可跑
- 本地开发环境与 NAS RabbitMQ 联调打通

## 第一阶段完成了什么

### 基础设施接入

已经完成：

- 在 `pom.xml` 中接入 `spring-boot-starter-amqp`
- 在 `src/main/resources/application.yml` 中增加 `app.mq` 开关
- 在 `src/main/resources/application-dev.yml` 与 `src/main/resources/application-prod.yml` 中补齐 RabbitMQ 连接配置
- 在 `src/main/java/com/nfu/jasmine/infra/mq/` 下建立 MQ 基础设施目录

当前开发环境联调参数：

- `RABBITMQ_HOST=192.168.31.26`
- `RABBITMQ_PORT=5673`
- `RABBITMQ_USERNAME=jasmine`
- `RABBITMQ_PASSWORD=123456`
- `RABBITMQ_VHOST=/jasmine`

### RabbitMQ 拓扑声明

已经完成：

- 交换机常量、队列常量、路由键常量统一收口到 `src/main/java/com/nfu/jasmine/infra/mq/JasmineMqConstants.java`
- 由 `src/main/java/com/nfu/jasmine/infra/mq/config/RabbitMqTopologyConfig.java` 统一声明拓扑

当前已声明的交换机：

- `jasmine.appointment.event`
- `jasmine.audit.event`
- `jasmine.dlx`

当前已声明的队列：

- `jasmine.appointment.notification`
- `jasmine.audit.access-log`
- `jasmine.dlq`

### 业务落点

已经完成两个真实落点：

1. 预约创建后发布 `appointment.created` 事件
2. 访问日志通过 MQ 异步消费后再落回 `ACCESS_LOG`

对应代码位置：

- `src/main/java/com/nfu/jasmine/appointment/application/impl/AppointmentServiceImpl.java`
- `src/main/java/com/nfu/jasmine/infra/mq/publisher/MqMessagePublisher.java`
- `src/main/java/com/nfu/jasmine/infra/mq/listener/AppointmentNotificationListener.java`
- `src/main/java/com/nfu/jasmine/infra/web/filter/RequestTraceFilter.java`
- `src/main/java/com/nfu/jasmine/infra/mq/listener/AccessLogAuditListener.java`

### Docker / NAS 部署文件

已经补齐 RabbitMQ 独立部署文件：

- `ops/rabbitmq/docker-compose.yml`
- `ops/rabbitmq/.env.example`

当前 NAS 实际开发环境使用：

- 镜像：`rabbitmq:4.2-management`
- 宿主机 AMQP 端口：`5673`
- 宿主机管理台端口：`15673`
- 默认用户：`jasmine`
- 默认密码：`123456`
- 默认虚拟主机：`/jasmine`

## 第一阶段测试方法

### 1. 启动 RabbitMQ

在 NAS 上准备：

- `ops/rabbitmq/docker-compose.yml`
- `ops/rabbitmq/.env`

然后执行：

```powershell
docker compose up -d
```

成功标志：

- 管理台可访问
- `Queues and Streams` 能看到 `jasmine.appointment.notification`、`jasmine.audit.access-log`、`jasmine.dlq`
- `Exchanges` 能看到 `jasmine.appointment.event`、`jasmine.audit.event`、`jasmine.dlx`

### 2. 用 MQ 模式启动后端

本地后端使用以下方式启动：

```powershell
cd "D:\Software Engineering\Code Library\IdeaProjects\Jasmine"

$OutputEncoding = [Console]::OutputEncoding = [System.Text.UTF8Encoding]::new($false)
chcp 65001 > $null

$env:JAVA_HOME="D:\Software Engineering\Dependence\JDK\jdk-21.0.10"
$env:Path="$env:JAVA_HOME\bin;$env:Path"

$env:APP_MQ_ENABLED="true"
$env:RABBITMQ_HOST="192.168.31.26"
$env:RABBITMQ_PORT="5673"
$env:RABBITMQ_USERNAME="jasmine"
$env:RABBITMQ_PASSWORD="123456"
$env:RABBITMQ_VHOST="/jasmine"

.\mvnw.cmd spring-boot:run
```

成功标志：

- 后端日志出现 RabbitMQ 连接成功信息
- 应用启动后不报 AMQP 连接异常

### 3. 测试访问日志异步链

操作方法：

- 登录前端
- 随便访问几个业务页面，或触发几个常规接口请求

验证点：

- 后端日志中出现由 `RabbitListenerEndpointContainer` 打印的 `ACCESS_LOG`

### 4. 测试预约通知异步链

操作方法：

- 前端进入预约页面
- 新增一条带明显标识内容的预约，例如 `PR13 MQ 测试预约`

验证点：

- 后端日志出现 `AppointmentNotificationListener` 的消费日志

实际测试结果示例：

```text
模拟发送预约通知 appointmentId=5 vipId=3 vipName=叶小姐 phone=13799990001 appointmentTime=Mon Apr 13 18:34:36 HKT 2026 content=PR13 MQ 测试预约
```

### 5. 测试管理台流量表现

在 RabbitMQ 管理台中观察：

- `Overview`
- `Queues and Streams`

实际现象：

- 会出现短时间消息速率波动
- 队列中的 `Ready`、`Unacked`、`Total` 最终很快归零

这是正常表现，说明消息已经及时消费完成。

## 第一阶段遇到的问题与解决方案

### 问题 1：宿主机端口冲突

现象：

- RabbitMQ 容器启动时报 `address already in use`

原因：

- 宿主机默认 `5672` 端口已被占用

解决方案：

- 不改容器内部默认端口
- 只改宿主机映射端口

最终采用：

- `5673:5672`
- `15673:15672`

### 问题 2：一度把端口映射改反

现象：

- 仍然报 `5672` 端口占用，看起来像换端口无效

原因：

- 把容器端口改成了 `5673`，而不是改宿主机端口

解决方案：

- 保持 RabbitMQ 容器内部还是：
  - `5672`
  - `15672`
- 只修改左侧宿主机端口

### 问题 3：前端动态菜单一度只剩首页

现象：

- 左侧菜单看起来异常收缩，只剩首页

根因：

- 这不是 RabbitMQ 本身的问题
- 前端动态菜单链和后端菜单 SQL 旧逻辑叠在一起导致判断混乱
- 最终确认后端 `/user/info` 在某一时刻返回了空的 `menuList`

进一步定位后发现：

- `src/main/resources/mapper/sys/MenuMapper.xml` 一度回到了旧写法
- 旧 SQL 只查直接挂在 `role_menu` 上的菜单
- 但当前角色菜单历史数据主要存的是叶子菜单
- 导致一级菜单查询为空，`menuList` 变成空数组

解决方案：

- 修正 `src/main/resources/mapper/sys/MenuMapper.xml`
- 从用户已有授权菜单出发
- 递归补回父菜单
- 再按 `parentId` 返回当前层级

修复后验证结果：

- `/user/info` 再次返回了正常的顶层菜单
- 前端侧边栏恢复正常

### 问题 4：旧前端残留模板动态路由 warning

现象：

- 前端开发时出现：
  - `export 'asyncRoutes' was not found in '@/router'`

原因：

- 前端还残留了旧模板的 `asyncRoutes` 逻辑
- 而项目现在已经改成“后端返回 `menuList` 驱动动态路由”

解决方案：

- 不再依赖旧模板的 `asyncRoutes`
- 统一由 `permission` 模块把后端菜单树转换成动态路由
- 侧边栏直接从 Vuex 中读取路由结果

### 问题 5：Bun / 旧 Vue CLI 工具链 warning

现象：

- 使用 Bun 或当前老 Vue CLI 依赖链时，会出现：
  - `Can't resolve 'async_hooks'`

判断：

- 这类 warning 更偏工具链兼容噪音
- 不影响 RabbitMQ 主链验证结果
- 也不是本轮 MQ 接入失败原因

当前处理策略：

- 先不把它当作 `PR13` 第一阶段 blocker
- 等前端现代化或工具链升级时再系统处理

## 第一阶段结论

`PR13` 第一阶段已经完成并经过真实联调验证，当前可明确确认：

- RabbitMQ 已成功接入项目
- NAS 开发环境 RabbitMQ 可稳定连接
- 交换机、队列、死信队列已经声明成功
- 访问日志异步链已经生效
- 预约创建事件已经能成功发布并消费

也就是说，RabbitMQ 已经不再只是“容器部署好了”，而是：

- **正式接进后端**
- **真实跑通了业务链路**

## 第二阶段完成了什么

第二阶段继续沿着“先收口基础设施，再补可验证业务链路”的原则推进，没有把 MQ 一次性膨胀成复杂消息平台，而是补齐了当前项目最需要的三块能力：

- 销售 / 库存事件发布第一版
- 消费幂等第一版
- 死信与消息契约第一版

### 销售 / 库存事件发布第一版

已经完成：

- 在 `src/main/java/com/nfu/jasmine/infra/mq/JasmineMqConstants.java` 中补齐交易交换机、队列和路由键常量
- 在 `src/main/java/com/nfu/jasmine/infra/mq/config/RabbitMqTopologyConfig.java` 中声明：
  - `jasmine.trade.event`
  - `jasmine.sales.event-log`
  - `jasmine.inventory.event-log`
- 新增消息体：
  - `src/main/java/com/nfu/jasmine/infra/mq/message/SalesCreatedMessage.java`
  - `src/main/java/com/nfu/jasmine/infra/mq/message/InventoryChangedMessage.java`
- 新增消费者：
  - `src/main/java/com/nfu/jasmine/infra/mq/listener/SalesEventListener.java`
  - `src/main/java/com/nfu/jasmine/infra/mq/listener/InventoryEventListener.java`
- 真实业务写路径已接入事件发布：
  - 销售单创建后发布 `sales.created`
  - 手工库存新增后发布 `inventory.changed`
  - 销售出库写库存流水时同步发布 `inventory.changed`

这一版仍然保持克制：

- 先发布真实事件
- 先消费并落日志
- 暂不直接接入统计、预警或更复杂下游

### 消费幂等第一版

已经完成：

- 幂等 key 命名统一收口到 `src/main/java/com/nfu/jasmine/infra/mq/MqKeyNames.java`
- 幂等实现统一收口到 `src/main/java/com/nfu/jasmine/infra/mq/support/MqIdempotencyService.java`
- 默认幂等 TTL 通过 `app.mq.idempotency-ttl-hours` 配置，当前默认 24 小时

当前幂等 key 规则：

- `jasmine:mq:idempotent:appointment-notification:{appointmentId}`
- `jasmine:mq:idempotent:access-log:{requestId}`
- `jasmine:mq:idempotent:sales-created:{salesId}`
- `jasmine:mq:idempotent:inventory-changed:{inventoryId}`

### 死信与失败处理第一版

已经完成：

- 死信交换机和死信队列继续沿用 `jasmine.dlx` / `jasmine.dlq`
- 监听器默认不 requeue
- 关键字段缺失的坏消息直接拒绝并进入死信
- 重复消费的消息直接跳过，不再执行副作用

对应支撑工具：

- `src/main/java/com/nfu/jasmine/infra/mq/support/MqMessageSupport.java`

### MQ 契约文档

已经补齐独立契约草稿：

- `docs/upgrade/pr13-mq-contract.md`

这份文档已经收口：

- exchange
- queue
- routing key
- message schema
- 幂等 key 规则
- 当前失败分类策略

## 第二阶段测试方法

### 1. 继续使用 MQ 模式启动后端

开发环境测试仍然通过：

```powershell
cd "D:\Software Engineering\Code Library\IdeaProjects\Jasmine"

$OutputEncoding = [Console]::OutputEncoding = [System.Text.UTF8Encoding]::new($false)
chcp 65001 > $null

$env:JAVA_HOME="D:\Software Engineering\Dependence\JDK\jdk-21.0.10"
$env:Path="$env:JAVA_HOME\bin;$env:Path"

$env:APP_MQ_ENABLED="true"
$env:RABBITMQ_HOST="192.168.31.26"
$env:RABBITMQ_PORT="5673"
$env:RABBITMQ_USERNAME="jasmine"
$env:RABBITMQ_PASSWORD="123456"
$env:RABBITMQ_VHOST="/jasmine"

.\mvnw.cmd spring-boot:run
```

### 2. 测试预约事件链

操作方法：

- 新建一条预约
- 备注写明显一点，例如 `PR13 appointment test`

验证点：

- 后端日志出现 `AppointmentNotificationListener`
- 能看到“模拟发送预约通知”

### 3. 测试销售事件链

操作方法：

- 新建一张销售单

验证点：

- 后端日志出现：
  - `模拟消费销售事件`
  - `模拟消费库存事件`

实际日志示例：

```text
模拟消费库存事件 inventoryId=14 bizNo=SO202604131901066251 flowerId=2 bizType=SALE_OUT quantity=20 beforeStock=80 afterStock=60
模拟消费销售事件 salesId=4 orderNo=SO202604131901066251 vipId=3 operatorId=1 itemCount=1 totalAmount=150.0
```

### 4. 测试手工库存事件链

操作方法：

- 新增一条库存流水
- 例如采购入库

验证点：

- 后端日志出现：
  - `模拟消费库存事件`

实际日志示例：

```text
模拟消费库存事件 inventoryId=15 bizNo=INV202604131901482448 flowerId=3 bizType=PURCHASE_IN quantity=100 beforeStock=43 afterStock=143
```

### 5. 观察 RabbitMQ 管理台速率图

第二阶段测试期间，RabbitMQ `Overview` 中会出现短时锯齿波。

这类波峰与日志时间点基本对得上：

- `19:00:02` 左右：预约创建事件
- `19:01:07` 左右：销售事件 + 销售出库库存事件
- `19:01:49` 左右：手工库存事件
- `19:02:54` 左右：再次手工库存事件

这属于正常现象，因为一次业务操作通常会同时带出：

- 业务事件消息
- 访问日志消息

所以图上表现为短时波段，而不是单个孤立点。

## 第二阶段遇到的问题与解决方案

### 问题 6：MQ 幂等一度持续回退到本地内存兜底

现象：

- 后端日志持续出现：

```text
MQ 幂等键写入 Redis 失败，使用本地兜底
```

最初容易误判成：

- NAS 上 Redis 没开
- Redis 端口不对
- 或者 MQ 幂等没有真正接 Redis

### 问题 6 的根因

最终定位发现，不是 NAS Redis 容器本身的问题，而是配置前缀写错了。

之前在以下文件中使用了：

- `src/main/resources/application-dev.yml`
- `src/main/resources/application-prod.yml`

错误写法：

```yaml
spring:
  redis:
```

但当前 Spring Boot 实际生效的是：

```yaml
spring:
  data:
    redis:
```

于是导致：

- MySQL 正常读取环境变量
- RabbitMQ 正常读取环境变量
- Redis 却退回默认 `localhost:6379`

这也是为什么之前 `prod` 单独验证时，会直接看到：

- `Unable to connect to localhost:6379`

### 问题 6 的解决方案

把两份环境配置中的 Redis 前缀修正为：

```yaml
spring:
  data:
    redis:
```

修复后再次验证：

- `prod` 临时实例中 `redis` 健康检查恢复为 `UP`
- `admin` 登录成功
- `/user/info` 正常返回完整菜单
- `dev` 环境下也不再出现 MQ 幂等写 Redis 失败告警

这说明：

- MQ 幂等已经不再依赖本地内存兜底
- Redis 幂等键现在可以真正写入 NAS Redis

## 第二阶段结论

第二阶段完成后，`PR13` 已经从“RabbitMQ 能接入和跑通预约/审计链”进一步推进到：

- 销售 / 库存业务事件已经发布并消费
- 幂等 key 规范已经落地
- MQ 消费幂等已经接通 Redis
- 坏消息与重复消息已经具备第一版分类处理
- MQ 契约文档已经补齐

也就是说，当前 `PR13` 已经不只是一个“RabbitMQ 接入演示”，而是形成了真正可持续扩展的消息基础骨架。

## 下一阶段该做什么

在当前两阶段已经完成后，后续更适合继续收口“更稳的消费行为”，而不是盲目加更多交换机和队列。

### 建议优先做的内容

1. 重试策略第一版

- 明确哪些异常适合立即重试
- 明确哪些异常适合延迟重试
- 明确哪些异常仍应直接进入死信

2. 下游业务消费者第一版

- 销售事件对接基础统计
- 库存事件对接基础预警或审计记录
- 继续保持“先有真实落点，再扩展平台能力”

3. 死信消息观察与排查手册

- 把常见失败模式整理成排查清单
- 方便以后快速判断是坏消息、重复消费还是外部依赖故障

4. 文档收口

- 继续维护 `docs/upgrade/pr13-mq-contract.md`
- 在后续增加消息体或队列时同步更新契约文档

### 这一阶段暂时不做

- 分布式锁
- Redis 热点库存预扣减
- 秒杀型防超卖
- 完整事件补偿平台
- 全量最终一致性框架

这些仍然属于后面更重的阶段，而不是当前 `PR13` 继续膨胀进来的内容。

## CI 集成测试 RabbitMQ 接入

### 问题 7：CI 集成测试 `/actuator/health` 返回 503

现象：

- GitHub Actions 运行 `./mvnw -B verify -DskipUTs=true` 时，`UserControllerSecurityIT.actuatorHealthShouldBePublic` 报错：

```text
java.lang.AssertionError: Status expected:<200> but was:<503>
```

- 从 health 响应体可以看到：

```json
{
  "status": "DOWN",
  "components": {
    "db": { "status": "UP" },
    "redis": { "status": "UP" },
    "rabbit": {
      "status": "DOWN",
      "details": {
        "error": "org.springframework.amqp.AmqpConnectException: java.net.ConnectException: Connection refused"
      }
    }
  }
}
```

### 问题 7 的根因

`AbstractIntegrationTest` 只启动了 MySQL 和 Redis 的 Testcontainer，没有启动 RabbitMQ。

但 `spring-boot-starter-amqp` 在 classpath 上会自动注册 `RabbitHealthIndicator`，health check 请求时尝试连接 `localhost:5672` 被拒绝，导致 rabbit 组件 DOWN，整体 health status 变成 DOWN，HTTP 响应码变成 503。

### 问题 7 的解决方案

把 RabbitMQ 也纳入 Testcontainers，让集成测试环境拥有完整的 MySQL + Redis + RabbitMQ 三件套。

#### 1. `pom.xml` 新增 Testcontainers RabbitMQ 模块

```xml
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>rabbitmq</artifactId>
    <scope>test</scope>
</dependency>
```

#### 2. `AbstractIntegrationTest.java` 新增 RabbitMQ 容器

```java
@Container
private static final RabbitMQContainer RABBITMQ_CONTAINER =
        new RabbitMQContainer(DockerImageName.parse("rabbitmq:4.2-management"));
```

并在 `registerContainerProperties` 中注册动态属性：

```java
// RabbitMQ
registry.add("spring.rabbitmq.host", RABBITMQ_CONTAINER::getHost);
registry.add("spring.rabbitmq.port", RABBITMQ_CONTAINER::getAmqpPort);
registry.add("spring.rabbitmq.username", RABBITMQ_CONTAINER::getAdminUsername);
registry.add("spring.rabbitmq.password", RABBITMQ_CONTAINER::getAdminPassword);
registry.add("app.mq.enabled", () -> true);
```

#### 3. `application-integration.yml` 启用 rabbit 健康检查

```yaml
management:
  health:
    redis:
      enabled: true
    rabbit:
      enabled: true
```

### 修复后的预期效果

- CI 中每个集成测试类启动时会自动拉取 `rabbitmq:4.2-management` 镜像并启动容器
- Spring Boot 连接真实 RabbitMQ，topology（exchange / queue / binding）自动声明
- `@RabbitListener` 消费者正常启动
- `/actuator/health` 中 rabbit 组件变为 UP，整体 health 返回 200
- `RabbitMqTopologyConfig` 和所有 listener 因 `app.mq.enabled=true` 被激活，测试覆盖范围更完整
