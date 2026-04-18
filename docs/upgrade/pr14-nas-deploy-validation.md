# PR14 NAS 部署验证记录

## 目标

这份文档用于记录 `PR14` 在 NAS 上基于 Docker / Compose / GHCR 镜像的真实部署验证结果，包括：

- 首次部署时遇到的问题
- 问题的根因与处理方式
- 当前 NAS 联调通过的验证项
- 仍然存在但不阻塞本轮的提示项

## 一、首次登录失败问题记录

### 现象

容器全部启动成功后：

- 前端页面可正常打开
- 后端、MySQL、Redis、RabbitMQ 都已启动完成
- 但前端登录时报错

后端日志中对应异常为：

```text
io.jsonwebtoken.security.WeakKeyException: The specified key byte array is 48 bits which is not secure enough for any JWT HMAC-SHA algorithm.
```

### 根因

NAS 部署时在 `ops/.env` 中把：

```env
JWT_SECRET=123456
```

直接写成了一个过短的测试值。

当前项目使用的是新版 `jjwt`，其 HMAC-SHA 签名要求密钥长度至少达到 `256 bits`。

因此虽然：

- 用户名密码校验通过
- 登录业务逻辑进入了发 token 阶段

但在真正生成 JWT 时抛出了 `WeakKeyException`，导致前端登录失败。

### 解决方式

把 `ops/.env` 中的 `JWT_SECRET` 改成足够长的字符串，例如：

```env
JWT_SECRET=JasmineNasJwtSecret2026_for_test_only_32bytes_minimum
```

然后重新启动后端：

```bash
./ops/prod/down.sh
./ops/prod/up.sh
```

或至少重启后端服务。

### 结论

这不是前端问题、不是 RabbitMQ 问题、也不是 MySQL / Redis 问题，而是部署环境变量中的 JWT 密钥长度不满足当前 JWT 库要求。

## 二、NAS 联调结果

### 1. 后端健康检查

实际返回：

```json
{"status":"UP","components":{"db":{"status":"UP","details":{"database":"MySQL","validationQuery":"isValid()"}},"diskSpace":{"status":"UP","details":{"total":1000064679936,"free":611031715840,"threshold":10485760,"path":"/app/.","exists":true}},"ping":{"status":"UP"},"rabbit":{"status":"UP","details":{"version":"4.2.5"}},"redis":{"status":"UP","details":{"version":"7.2.13"}},"ssl":{"status":"UP","details":{"validChains":[],"invalidChains":[]}}}}
```

结论：

- 后端应用正常启动
- MySQL 正常
- Redis 正常
- RabbitMQ 正常
- 健康检查链路正常

### 2. Swagger 页面

Swagger 页面可正常打开，接口分组与主接口列表显示正常。

关于页面下方的 `Schemas`：

- `Schemas` 不是“没有纳入 Swagger 的接口”
- 它表示当前 OpenAPI 文档里涉及到的请求体 / 响应体模型定义
- 例如：
  - `SalesSaveDTO`
  - `InventorySaveDTO`
  - `UserCreateDTO`
  - `ResultObject`
  - `LoginVO`

也就是说，`Schemas` 是接口所引用的数据模型清单，不是漏掉的接口。

### 3. RabbitMQ

RabbitMQ 管理台访问正常，当前状态表现为：

- `Connections: 1`
- `Channels: 6`
- `Exchanges: 11`
- `Queues: 5`
- `Consumers: 4`

队列页验证结果：

- `jasmine.appointment.notification` 正常
- `jasmine.audit.access-log` 正常
- `jasmine.sales.event-log` 正常
- `jasmine.inventory.event-log` 正常
- `jasmine.dlq` 正常

并且：

- `Ready=0`
- `Unacked=0`
- `Total=0`

说明当前消息链路正常，联调后没有出现消息积压或死信堆积。

### 4. 前端页面

前端页面可正常访问，登录后：

- 花卉管理正常
- 销售管理正常
- 库存管理正常
- 用户预约正常

页面截图验证显示：

- 菜单正常加载
- 列表数据正常渲染
- 今日经营统计正常回显

### 5. 主链路验证

本轮已实际验证以下主链路：

- 登录
- 查看花卉列表
- 查看销售列表与今日经营统计
- 新增销售单
- 查看库存流水
- 新增库存流水
- 新增预约

结果：

- 销售创建后，销售列表与今日经营统计更新正常
- 销售出库后，库存流水正常增加一条 `SALE_OUT`
- 手工库存新增后，库存流水正常增加一条 `PURCHASE_IN`
- 预约创建后，预约列表正常出现新记录

### 6. 后端日志验证

从后端日志可以确认：

- 应用以 `prod` profile 启动成功
- Flyway 校验并确认数据库已在 `v5`
- RabbitMQ 连接建立成功
- 登录成功日志正常
- 访问日志异步链正常
- `GET /v3/api-docs` 正常
- 销售事件消费正常：
  - `模拟消费销售事件`
- 库存事件消费正常：
  - `模拟消费库存事件`
- 预约通知消费正常：
  - `模拟发送预约通知`

说明：

- `PR13` 与 `PR13.5` 打下的 MQ 主链在 NAS 部署环境中也能正常工作

## 三、当前存在但不阻塞本轮的提示项

### 1. Flyway 对 MySQL 8.4 的提示

日志里会出现：

```text
Flyway upgrade recommended: MySQL 8.4 is newer than this version of Flyway and support has not been tested.
```

这是一条版本支持范围提示，不代表当前迁移失败。

本轮实际结果是：

- 校验成功
- 当前 schema 已到 `v5`
- 启动正常

因此当前先记为已知提示项，不阻塞 `PR14`。

### 2. Spring Security 生成默认密码提示

日志里会出现：

```text
Using generated security password: ...
```

这说明 Boot 默认的 `inMemoryUserDetailsManager` 仍然存在。

但从实际登录结果看：

- 当前项目自己的 `/user/login` 主链可正常使用
- 不影响本轮联调结果

因此这条先记为后续可继续收口的提示项，不阻塞当前部署基线。

### 3. SpringDoc 默认启用提示

日志里会出现：

```text
SpringDoc /v3/api-docs endpoint is enabled by default.
SpringDoc /swagger-ui.html endpoint is enabled by default.
```

这属于生产环境暴露 OpenAPI 文档的提醒，不影响当前部署验证。

后续如果需要更收敛的生产安全策略，可以再单独决定是否关闭。

### 4. Redis overcommit 提示

Redis 日志里有：

```text
WARNING Memory overcommit must be enabled!
```

这是宿主机内核参数提醒，表示 NAS 上后续最好设置：

```bash
sysctl vm.overcommit_memory=1
```

当前并未影响本轮 Redis 启动和联调，因此同样记为后续优化项。

## 四、结论

本轮 `PR14` 在 NAS 上已经完成一轮真实部署验证，当前可以明确确认：

- GHCR 镜像可正常拉取
- `ops/prod/docker-compose.yml` 可正常启动完整环境
- 后端健康检查正常
- Swagger 正常
- RabbitMQ 正常
- 前端页面正常
- 登录、销售、库存、预约主链正常
- MQ 消费链路正常

这说明 `PR14` 的 Docker / Ops / 部署基线已经具备可用性。

当前唯一实际踩到的问题是：

- `JWT_SECRET` 过短导致登录阶段 JWT 生成失败

该问题已经通过调整 `ops/.env` 中的密钥长度解决。
