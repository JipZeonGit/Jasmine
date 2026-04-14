package com.nfu.jasmine.infra.mq.listener;

import com.nfu.jasmine.infra.mq.JasmineMqConstants;
import com.nfu.jasmine.infra.mq.MqKeyNames;
import com.nfu.jasmine.infra.mq.message.InventoryChangedMessage;
import com.nfu.jasmine.infra.mq.support.MqIdempotencyService;
import com.nfu.jasmine.infra.mq.support.MqMessageSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 库存变动事件消费者。
 * <p>
 * 消费模式：前置校验 -> 幂等去重 -> 业务处理（当前为日志模拟，后续接预警/统计）。
 */
@Component
@ConditionalOnProperty(name = "app.mq.enabled", havingValue = "true")
public class InventoryEventListener {
    private static final Logger log = LoggerFactory.getLogger(InventoryEventListener.class);

    private final MqIdempotencyService mqIdempotencyService;

    public InventoryEventListener(MqIdempotencyService mqIdempotencyService) {
        this.mqIdempotencyService = mqIdempotencyService;
    }

    @RabbitListener(queues = JasmineMqConstants.INVENTORY_EVENT_QUEUE)
    public void onInventoryChanged(InventoryChangedMessage message) {
        MqMessageSupport.rejectIfNull(message, "message");
        MqMessageSupport.rejectIfNull(message.getInventoryId(), "inventoryId");
        MqMessageSupport.rejectIfBlank(message.getBizNo(), "bizNo");

        String key = MqKeyNames.inventoryChanged(message.getInventoryId());
        if (!mqIdempotencyService.markIfFirstConsume(key)) {
            log.info("跳过重复库存事件 inventoryId={}", message.getInventoryId());
            return;
        }
        // 库存事件先落成一个稳定的“出入口”，这样后面要接预警、审计或统计时不用再回头改主业务事务。
        log.info("模拟消费库存事件 inventoryId={} bizNo={} flowerId={} bizType={} quantity={} beforeStock={} afterStock={} bizTime={}",
                message.getInventoryId(),
                message.getBizNo(),
                message.getFlowerId(),
                message.getBizType(),
                message.getQuantity(),
                message.getBeforeStock(),
                message.getAfterStock(),
                message.getBizTime());
    }
}
