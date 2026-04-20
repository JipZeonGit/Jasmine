# Code Review Report

## 1. Executive Summary
- **总体评价**：存在风险（存在多处底层设计缺陷和致命 P0 级 Bug）
- **核心问题概述**：
  1. **主键查询严重错误**：库存预警读模型中，将业务外键当做 MyBatis Plus 的 `selectById` 物理主键检索，将导致无法正确更新记录并引发唯一约束引发业务崩溃。
  2. **违反本地发件箱的原子性并出现失控**：Outbox 自身掩盖了 DB 落盘异常导致事件可能静默丢失，且发信调度无脑同步修改 `SENT` 彻底破坏异步触达的可靠性。
  3. **轮询机制无视分布式与数据库索引**：Outbox 定时抓取会导致索引失效触发全表扫描，并且在多实例集群下完全没有防重机制，势必导致“脑裂”双投。

## 2. Critical Issues（P0 - 必须修复）
> 会导致系统错误 / 安全风险

### [P0] 预警读模型主键查询错误将导致更新崩溃（隐藏逻辑 Bug）
- **问题说明**：在 `InventoryAlertService.upsertAlert` 中，业务尝试根据业务键 `flowerId` 来查找花卉预警状态，但是调用了  `inventoryAlertMapper.selectById(flowerId)`。在 MyBatis Plus 中该方法操作的是物理主键（即自增 `id` 列），导致这不仅找不到预期的行，还会在接下来的 `insert` 步骤中违背 `uk_inventory_alert_flower_id` 唯一索引导致报错和整体业务回滚。
- **修复建议**：必须声名外键映射关系获取实体：
```java
// 改后代码：
InventoryAlert existing = inventoryAlertMapper.selectOne(
    new LambdaQueryWrapper<InventoryAlert>().eq(InventoryAlert::getFlowerId, flowerId)
);
```

### [P0] Outbox 吞并写入异常破坏业务与消息的原子性（架构核心问题）
- **问题说明**：代码在 `OutboxService.save` 内 `try-catch` 后吞掉了事件实体保存报错动作。由于 Outbox 本地发件箱最核心的要义是**令发事件操作同主业务落进同样的本地事务边界**。如果你截断了由于字符超宽或 JSON 格式引发的持久异常，那就代表你向数据库骗得了主业务保存成功而丢弃了后续任何重试分发的可能性。
- **修复方案**：
```java
// 改后代码：拿掉掩盖错误的封装
public void save(String eventType, String exchange, String routingKey, Object payload) {
    try {
        String json = objectMapper.writeValueAsString(payload);
        EventOutbox outbox = EventOutbox.builder() // ...
                .build();
        eventOutboxMapper.insert(outbox);
    } catch (Exception ex) {
        log.error("Outbox 写入失败，将跟随引爆同事务回滚避免幽灵事件...", ex);
        // 此类关键情况必须以异常冒泡形式推起本地主事务同步回滚！
        throw new RuntimeException("Outbox Persistence Failed", ex);
    }
}
```

## 3. Major Issues（P1 - 高优先级）
> 影响性能或可维护性

### [P1] MQ 发送后的盲目“欺骗式”确认（架构设计问题）
- **问题说明**：我们在 `OutboxRelay.sendOne` 看到它把消息塞给 `rabbitTemplate.convertAndSend` 之后立刻自以为是地标记改行数据为 `SENT`。事实上此调用只是代表已经放进底层 JVM 或网络发送管道，并不代表目标 RabbitMQ Exchange 成功接管。一旦服务异常关闭或者 RabbitMQ 短暂脑裂将出现未达丢信现象。
- **修复方案**：引入 `rabbitTemplate.invoke` 实施阻塞级别的强制同步推流保障一致（偏损耗），或者通过实现 `RabbitTemplate.ConfirmCallback` 在接收到异步 `ACK=true` 的时候凭所带的 `CorrelationData` 反写完成通知。

### [P1] Outbox 扫描存在查询索引失效问题（性能瓶颈）
- **问题说明**：在 `OutboxRelay.fetchPending` 里 `wrapper.and(w -> w.eq(retryCount, 0).or().le(nextRetryTime, now))`。在这个表中存在建立的联合索引 `idx_event_outbox_status_next_retry` ，由于使用了基于 Or 的合并查询且 `retryCount` 并无建树这会导致索引全免及非常暴力的全表扫描拖垮数据库。
- **修复方案**：已知第一回 `next_retry_time` 就已默认塞进新时间节点，可以直接省掉第一部分的 Or 控制：
```java
// 改后代码：
wrapper.eq(EventOutbox::getStatus, OutboxStatus.PENDING.name())
       .le(EventOutbox::getNextRetryTime, now)
       .orderByAsc(EventOutbox::getId)
       .last("LIMIT " + batchSize);
```

### [P1] 缺少多实例防脏读及防双写的防范机制
- **问题说明**：在真实的集群或 Kubernetes 中，多个副本可能同时运行定时的 Relay 代码并查出并提取等同列表向中间件压入复发件，尽管下游有防重体系，依旧无端极大加重系统交互网络压力和争建负载。
- **修复建议**：必须做防脑裂锁定。可用 Redisson 给方法体下发 `@SchedulerLock` 获取独立时片；如果不行最基本也要加一个发件预置确认环节，执行 `UPDATE event_outbox SET status = 'SENDING' WHERE id = ? AND status = 'PENDING'` 再进入转换流中。

## 4. Minor Issues（P2 - 建议优化）
> 代码风格或小问题

### [P2] 失败长文本简单切分带来代码隐患（异常越界风险）
- **问题说明**：在 `OutboxRelay.handleSendFailure` 中切片手段采用硬切 `errorMsg.substring(0, 500)`。如果捕获的 Exception 为空对象、Null 等则会引入 `NullPointerException` 或其它索引溢出。
- **修复建议**：请引入 `StringUtils.left(errorMsg, 500)` 防护工具保障系统的基本强韧。

## 5. Security Analysis
此段代码并未使用直接开放的接口控制而主要为底层架构流配置调用，因此并未存在主要面向外部黑客漏洞攻击的暴露点：
- **SQL Injection**：通过 MybatisPlus 与实体及 Wrapper 的有效隔离，不存在任何裸拼接攻击场景。
- **XSS & CSRF**：未向浏览器及模板系统输出任何相关内容。
- **权限与越权处理**：在调用库存更改及写回之前均有正常且严密的主业务模块作为校验关口拦截。
- **建议**：整体并未带来新型安全架构缺失问题，基本合格。

## 6. Performance Analysis
- 主要的问题就在**查询扫描优化环节（见 P1 描述）**。对于含有持续积累和状态扭转的时间节点表一定要时刻遵守高命中率的左侧查询，处理掉 OR 后该扫描调度将被稳定维持常态 $O(\log n)$ 级别响应。
- 另，发件环节为规避过多长链接也可启用批量打包发件，或直接依托现有代码采用配置好线程池控制即可。

## 7. Architecture & Design
- **SOLID 评价**：从总体解耦效果看，非常不错（脱离了原来对 MQ 原系统的绝对信任并加入削峰自处理）。
- **模块耦合性**：中度耦合状态，后续对 Outbox 可以形成一套类似基座的组件包而不与现有业务层紧相连。
- **扩展性**：基于现在的表架构未来可进一步做成多状态、长生命力回查体系或者做单独的 DeadLetter 转储扩展设计。

## 8. Refactoring Suggestions
结合 P1 提出的投递方案缺陷和并发问题进行混合整改，强化可靠闭环并增加发送队列并发防抢和同步处理（若接受高消耗也可以强留异步），演示安全改后发送逻辑：
```java
// OutboxRelay.java 发件阶段防抢重构演示
private void sendOne(EventOutbox outbox) {
     // 第一步，增加乐观行锁保护多实例并发现象
     int updated = eventOutboxMapper.update(null, 
            new LambdaQueryWrapper<EventOutbox>()
               .eq(EventOutbox::getId, outbox.getId())
               .eq(EventOutbox::getStatus, OutboxStatus.PENDING.name())
               .set(EventOutbox::getStatus, "SENDING") 
     );
     if (updated == 0) return; // 已被其他节点认领走

     // 第二步，利用 ID 进行 MQ 请求匹配与投递
     try {
         rabbitTemplate.convertAndSend(
                 outbox.getExchange(),
                 outbox.getRoutingKey(),
                 outbox.getPayload(),
                 new CorrelationData(outbox.getId().toString())
         );
         // 注意：真正的 setStatus("SENT") 和入库请务必通过 RabbitMQ 的 ConfirmCallback 监听上述 ID 来安全完成写库回调！
     } catch(Exception e) {
         handleSendFailure(outbox, e);
     }
}
```

## 9. Testability
- 依托现有项目 TestContainers 以及组件，此部分不难构建相关链路测试。
- 重点强烈建议排查包含补充以下测试范畴：
  1. 出具由异常模拟（Mock `ObjectMapper`）发起的强硬 `OutboxService` 断连测试，断言发件写入失败是否会正确逼退主记录；
  2. 断开 MQ 容器模拟网络崩溃，查看其指数退避是否按照预期工作产生推移 `nextRetryTime` 回滚并且没有丢失 `PENDING` 的行存。
  3. `inventoryAlertMapper.selectOne` 若发生 `flowerId` 插入或更新逻辑合并的自动化断言验证。
