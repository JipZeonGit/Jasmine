package com.nfu.jasmine.infra.mq.publisher;

import com.nfu.jasmine.infra.mq.JasmineMqConstants;
import com.nfu.jasmine.infra.mq.message.AccessLogMessage;
import com.nfu.jasmine.infra.mq.message.AppointmentCreatedMessage;
import com.nfu.jasmine.infra.mq.message.InventoryChangedMessage;
import com.nfu.jasmine.infra.mq.message.SalesCreatedMessage;
import com.nfu.jasmine.infra.outbox.OutboxService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * 统一的 MQ 消息发布服务。
 * <p>
 * 业务事件（预约/销售/库存）通过 Outbox 写入 event_outbox 表，
 * 由 OutboxRelay 异步扫描并发送到 MQ，保证"主业务成功则事件不丢"。
 * 非事务场景（如访问日志）仍然使用 {@code publishNow} 直接发送。
 * <p>
 * MQ 发送失败时仅记录日志不抛异常，保证主业务链路不受 MQ 瞬时不可用影响。
 */
@Service
public class MqMessagePublisher {
    private static final Logger log = LoggerFactory.getLogger(MqMessagePublisher.class);

    private final RabbitTemplate rabbitTemplate;
    private final OutboxService outboxService;

    @Value("${app.mq.enabled:false}")
    private boolean enabled;

    public MqMessagePublisher(RabbitTemplate rabbitTemplate, OutboxService outboxService) {
        this.rabbitTemplate = rabbitTemplate;
        this.outboxService = outboxService;
    }

    // 访问日志不走 Outbox，仍走直接发送，因为已有同步日志兜底。
    public boolean publishAccessLog(AccessLogMessage message) {
        return publishNow(JasmineMqConstants.AUDIT_EVENT_EXCHANGE, JasmineMqConstants.ACCESS_LOG_ROUTING_KEY, message);
    }

    // 预约创建事件写 Outbox，由 Relay 异步发送
    public void publishAppointmentCreatedAfterCommit(AppointmentCreatedMessage message) {
        if (!enabled) {
            return;
        }
        outboxService.save(
                JasmineMqConstants.APPOINTMENT_CREATED_ROUTING_KEY,
                JasmineMqConstants.APPOINTMENT_EVENT_EXCHANGE,
                JasmineMqConstants.APPOINTMENT_CREATED_ROUTING_KEY,
                message
        );
    }

    /**
     * 发布预约提醒延时消息。消息先投入死信驻留队列，等 TTL 过期后自动弹射至提醒消费队列。
     * delayMs <= 0 时退化为即时投递（用于"距离预约已不足提醒时间"的紧急场景）。
     */
    public void publishAppointmentReminderDelayed(AppointmentCreatedMessage message, long delayMs) {
        if (!enabled) {
            return;
        }
        if (delayMs > 0) {
            // 延时消息投向 delay 队列的路由键，Outbox Relay 会将 delayMs 设为 AMQP expiration
            outboxService.save(
                    "appointment.reminder.delayed",
                    JasmineMqConstants.APPOINTMENT_EVENT_EXCHANGE,
                    JasmineMqConstants.APPOINTMENT_DELAY_ROUTING_KEY,
                    message,
                    delayMs
            );
        } else {
            // 已不足 1 小时（delayMs <= 0），退化为即时投递，直接发给最终唤醒队列让消费者立刻处理
            outboxService.save(
                    "appointment.reminder.immediate",
                    JasmineMqConstants.APPOINTMENT_EVENT_EXCHANGE,
                    JasmineMqConstants.APPOINTMENT_REMINDER_ROUTING_KEY,
                    message,
                    null
            );
        }
    }

    // 销售创建事件写 Outbox，由 Relay 异步发送
    public void publishSalesCreatedAfterCommit(SalesCreatedMessage message) {
        if (!enabled) {
            return;
        }
        outboxService.save(
                JasmineMqConstants.SALES_CREATED_ROUTING_KEY,
                JasmineMqConstants.TRADE_EVENT_EXCHANGE,
                JasmineMqConstants.SALES_CREATED_ROUTING_KEY,
                message
        );
    }

    // 库存变更事件写 Outbox，由 Relay 异步发送
    public void publishInventoryChangedAfterCommit(InventoryChangedMessage message) {
        if (!enabled) {
            return;
        }
        outboxService.save(
                JasmineMqConstants.INVENTORY_CHANGED_ROUTING_KEY,
                JasmineMqConstants.TRADE_EVENT_EXCHANGE,
                JasmineMqConstants.INVENTORY_CHANGED_ROUTING_KEY,
                message
        );
    }

    private boolean publishNow(String exchange, String routingKey, Object payload) {
        if (!enabled) {
            return false;
        }
        try {
            rabbitTemplate.convertAndSend(exchange, routingKey, payload, new CorrelationData(UUID.randomUUID().toString()));
            return true;
        } catch (Exception ex) {
            log.warn("MQ 发布失败 exchange={} routingKey={} message={}", exchange, routingKey, ex.getMessage());
            return false;
        }
    }
}
