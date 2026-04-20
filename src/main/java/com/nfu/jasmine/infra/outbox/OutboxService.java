package com.nfu.jasmine.infra.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nfu.jasmine.infra.outbox.model.entity.EventOutbox;
import com.nfu.jasmine.infra.outbox.model.enums.OutboxStatus;
import com.nfu.jasmine.infra.outbox.persistence.mapper.EventOutboxMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Date;

/**
 * Outbox 写入服务。
 * <p>
 * 在主事务内调用，把业务事件写入 event_outbox 表，
 * 后续由 OutboxRelayService 异步扫描并发送到 MQ。
 */
@Service
public class OutboxService {
    private static final Logger log = LoggerFactory.getLogger(OutboxService.class);

    private final EventOutboxMapper eventOutboxMapper;
    private final ObjectMapper objectMapper;

    public OutboxService(EventOutboxMapper eventOutboxMapper, ObjectMapper objectMapper) {
        this.eventOutboxMapper = eventOutboxMapper;
        this.objectMapper = objectMapper;
    }

    /**
     * 写入一条待发送的 Outbox 记录。应在主事务内调用，保证业务数据与 Outbox 原子写入。
     */
    public void save(String eventType, String exchange, String routingKey, Object payload) {
        try {
            String json = objectMapper.writeValueAsString(payload);
            EventOutbox outbox = EventOutbox.builder()
                    .eventType(eventType)
                    .exchange(exchange)
                    .routingKey(routingKey)
                    .payload(json)
                    .status(OutboxStatus.PENDING.name())
                    .retryCount(0)
                    .nextRetryTime(new Date())
                    .createdAt(new Date())
                    .build();
            eventOutboxMapper.insert(outbox);
        } catch (Exception ex) {
            log.error("Outbox 写入失败，将跟随引爆同事务回滚，以避免幽灵事件。eventType={} exchange={} routingKey={} error={}",
                    eventType, exchange, routingKey, ex.getMessage(), ex);
            throw new RuntimeException("Outbox Persistence Failed", ex);
        }
    }
}
