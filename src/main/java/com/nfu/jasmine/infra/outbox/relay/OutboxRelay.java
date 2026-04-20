package com.nfu.jasmine.infra.outbox.relay;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.nfu.jasmine.infra.outbox.model.entity.EventOutbox;
import com.nfu.jasmine.infra.outbox.model.enums.OutboxStatus;
import com.nfu.jasmine.infra.outbox.persistence.mapper.EventOutboxMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;


/**
 * Outbox Relay 定时任务。
 * <p>
 * 定期扫描 event_outbox 表中 PENDING 和需要重试的记录，
 * 尝试发送到 MQ，成功置为 SENT，失败按退避策略更新 next_retry_time，
 * 超过重试阈值后置为 FAILED。
 */
@Component
@ConditionalOnProperty(name = "app.mq.enabled", havingValue = "true")
public class OutboxRelay {
    private static final Logger log = LoggerFactory.getLogger(OutboxRelay.class);

    private static final int MAX_RETRY_COUNT = 5;
    // 指数退避基数：1s, 2s, 4s, 8s, 16s
    private static final long RETRY_BASE_DELAY_SECONDS = 1L;

    private final EventOutboxMapper eventOutboxMapper;
    private final RabbitTemplate rabbitTemplate;

    @Value("${app.outbox.relay.batch-size:50}")
    private int batchSize;

    public OutboxRelay(EventOutboxMapper eventOutboxMapper, RabbitTemplate rabbitTemplate) {
        this.eventOutboxMapper = eventOutboxMapper;
        this.rabbitTemplate = rabbitTemplate;
    }

    // 每 5 秒扫描一次待发送记录
    @Scheduled(fixedDelayString = "${app.outbox.relay.interval-ms:5000}")
    public void relayPendingMessages() {
        List<EventOutbox> pendingList = fetchPending();
        if (pendingList.isEmpty()) {
            return;
        }

        for (EventOutbox outbox : pendingList) {
            sendOne(outbox);
        }
    }

    private List<EventOutbox> fetchPending() {
        Date now = new Date();
        LambdaQueryWrapper<EventOutbox> wrapper = new LambdaQueryWrapper<>();
        // PENDING 且 retry_count=0（首次发送）或 next_retry_time 已到（重试）
        wrapper.eq(EventOutbox::getStatus, OutboxStatus.PENDING.name())
                .le(EventOutbox::getNextRetryTime, now)
                .orderByAsc(EventOutbox::getId)
                .last("LIMIT " + batchSize);
        return eventOutboxMapper.selectList(wrapper);
    }

    private void sendOne(EventOutbox outbox) {
        // 第一步：利用乐观并发抢占执行权，并将下一次重试时间向前推移起保护期
        long delaySeconds = (long) Math.pow(2, outbox.getRetryCount()) * RETRY_BASE_DELAY_SECONDS;
        Date nextRetry = Date.from(Instant.now().plus(Duration.ofSeconds(delaySeconds)));

        int rows = eventOutboxMapper.update(null,
                new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<EventOutbox>()
                        .eq(EventOutbox::getId, outbox.getId())
                        .eq(EventOutbox::getStatus, OutboxStatus.PENDING.name())
                        // 确保没被别的节点抢走
                        .le(EventOutbox::getNextRetryTime, new Date())
                        .set(EventOutbox::getNextRetryTime, nextRetry)
                        .set(EventOutbox::getRetryCount, outbox.getRetryCount() + 1)
        );

        if (rows == 0) {
            return; // 已经被同类进程抢占处理
        }

        try {
            org.springframework.amqp.core.Message message = org.springframework.amqp.core.MessageBuilder
                    .withBody(outbox.getPayload().getBytes(java.nio.charset.StandardCharsets.UTF_8))
                    .setContentType(org.springframework.amqp.core.MessageProperties.CONTENT_TYPE_JSON)
                    .build();
            rabbitTemplate.send(
                    outbox.getExchange(),
                    outbox.getRoutingKey(),
                    message,
                    new CorrelationData(outbox.getId().toString())
            );
            log.debug("Outbox 消息投至网关待复，已脱离本进程死锁 id={} eventType={}", outbox.getId(), outbox.getEventType());
        } catch (Exception ex) {
            handleSendFailure(outbox, ex);
        }
    }

    private void handleSendFailure(EventOutbox outbox, Exception ex) {
        int newRetryCount = outbox.getRetryCount() + 1;
        String errorMsg = ex.getMessage() == null ? "Unknown Exception" : ex.getMessage();
        if (errorMsg.length() > 500) {
            errorMsg = errorMsg.substring(0, 500);
        }

        if (newRetryCount >= MAX_RETRY_COUNT) {
            // 超过重试阈值，置为 FAILED
            outbox.setStatus(OutboxStatus.FAILED.name());
            outbox.setRetryCount(newRetryCount);
            outbox.setLastError(errorMsg);
            outbox.setNextRetryTime(null);
            eventOutboxMapper.updateById(outbox);
            log.error("Outbox 消息重试耗尽 id={} eventType={} retryCount={}",
                    outbox.getId(), outbox.getEventType(), newRetryCount);
        } else {
            // 按指数退避计算下次重试时间
            long delaySeconds = (long) Math.pow(2, newRetryCount - 1) * RETRY_BASE_DELAY_SECONDS;
            Date nextRetry = Date.from(Instant.now().plus(Duration.ofSeconds(delaySeconds)));
            outbox.setRetryCount(newRetryCount);
            outbox.setLastError(errorMsg);
            outbox.setNextRetryTime(nextRetry);
            eventOutboxMapper.updateById(outbox);
            log.warn("Outbox 消息发送失败，等待重试 id={} eventType={} retryCount={} nextRetry={} error={}",
                    outbox.getId(), outbox.getEventType(), newRetryCount, nextRetry, errorMsg);
        }
    }
}
