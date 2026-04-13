package com.nfu.jasmine.infra.mq.publisher;

import com.nfu.jasmine.infra.mq.JasmineMqConstants;
import com.nfu.jasmine.infra.mq.message.AccessLogMessage;
import com.nfu.jasmine.infra.mq.message.AppointmentCreatedMessage;
import com.nfu.jasmine.infra.mq.message.InventoryChangedMessage;
import com.nfu.jasmine.infra.mq.message.SalesCreatedMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
public class MqMessagePublisher {
    private static final Logger log = LoggerFactory.getLogger(MqMessagePublisher.class);

    private final RabbitTemplate rabbitTemplate;

    @Value("${app.mq.enabled:false}")
    private boolean enabled;

    public MqMessagePublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public boolean publishAccessLog(AccessLogMessage message) {
        return publishNow(JasmineMqConstants.AUDIT_EVENT_EXCHANGE, JasmineMqConstants.ACCESS_LOG_ROUTING_KEY, message);
    }

    public void publishAppointmentCreatedAfterCommit(AppointmentCreatedMessage message) {
        publishAfterCommit(JasmineMqConstants.APPOINTMENT_EVENT_EXCHANGE, JasmineMqConstants.APPOINTMENT_CREATED_ROUTING_KEY, message);
    }

    public void publishSalesCreatedAfterCommit(SalesCreatedMessage message) {
        publishAfterCommit(JasmineMqConstants.TRADE_EVENT_EXCHANGE, JasmineMqConstants.SALES_CREATED_ROUTING_KEY, message);
    }

    public void publishInventoryChangedAfterCommit(InventoryChangedMessage message) {
        publishAfterCommit(JasmineMqConstants.TRADE_EVENT_EXCHANGE, JasmineMqConstants.INVENTORY_CHANGED_ROUTING_KEY, message);
    }

    private void publishAfterCommit(String exchange, String routingKey, Object payload) {
        if (!enabled) {
            return;
        }
        // 业务事件统一在事务提交后发布，避免下游拿到一条最终没有真正写入数据库的“幽灵消息”。
        if (TransactionSynchronizationManager.isActualTransactionActive() && TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    publishNow(exchange, routingKey, payload);
                }
            });
            return;
        }
        publishNow(exchange, routingKey, payload);
    }

    private boolean publishNow(String exchange, String routingKey, Object payload) {
        if (!enabled) {
            return false;
        }
        try {
            rabbitTemplate.convertAndSend(exchange, routingKey, payload);
            return true;
        } catch (Exception ex) {
            // 第一版先确保主业务链不因为 MQ 暂时不可用而整体失败，失败信息留在日志里继续追。
            log.warn("MQ 发布失败 exchange={} routingKey={} message={}", exchange, routingKey, ex.getMessage());
            return false;
        }
    }
}
