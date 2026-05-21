package com.nfu.jasmine.infra.mq.listener;

import com.nfu.jasmine.common.dto.internal.FlowerDTO;
import com.nfu.jasmine.infra.client.FlowerClient;
import com.nfu.jasmine.infra.mq.JasmineMqConstants;
import com.nfu.jasmine.infra.mq.MqKeyNames;
import com.nfu.jasmine.infra.mq.message.InventoryChangedMessage;
import com.nfu.jasmine.infra.mq.support.MqIdempotencyService;
import com.nfu.jasmine.infra.mq.support.MqMessageSupport;
import com.nfu.jasmine.inventory.alert.service.InventoryAlertService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 库存变动事件消费者。
 * <p>
 * 消费模式：前置校验 -> 幂等去重 -> 更新库存预警读模型（这是第一个真实下游）。
 */
@Component
@ConditionalOnProperty(name = "app.mq.enabled", havingValue = "true")
public class InventoryEventListener {
    private static final Logger log = LoggerFactory.getLogger(InventoryEventListener.class);

    private final MqIdempotencyService mqIdempotencyService;
    private final InventoryAlertService inventoryAlertService;
    private final FlowerClient flowerClient;

    public InventoryEventListener(MqIdempotencyService mqIdempotencyService, 
                                 InventoryAlertService inventoryAlertService,
                                 FlowerClient flowerClient) {
        this.mqIdempotencyService = mqIdempotencyService;
        this.inventoryAlertService = inventoryAlertService;
        this.flowerClient = flowerClient;
    }

    @RabbitListener(queues = JasmineMqConstants.INVENTORY_EVENT_QUEUE)
    public void onInventoryChanged(InventoryChangedMessage message) {
        MqMessageSupport.rejectIfNull(message, "message");
        MqMessageSupport.rejectIfNull(message.getInventoryId(), "inventoryId");
        MqMessageSupport.rejectIfNull(message.getFlowerId(), "flowerId");
        MqMessageSupport.rejectIfBlank(message.getBizNo(), "bizNo");

        String key = MqKeyNames.inventoryChanged(message.getInventoryId());
        if (!mqIdempotencyService.markIfFirstConsume(key)) {
            log.info("跳过重复库存事件 inventoryId={}", message.getInventoryId());
            return;
        }

        // 由事件消费者驱动更新库存预警读模型
        updateInventoryAlert(message);
    }

    private void updateInventoryAlert(InventoryChangedMessage message) {
        try {
            // 通过远程接口查询花卉信息
            FlowerDTO flower = flowerClient.getFlowerById(message.getFlowerId());
            if (flower == null) {
                log.warn("库存事件关联的花卉不存在 flowerId={}", message.getFlowerId());
                return;
            }

            // FlowerDTO 不含 safeStock/currentStock，需要从库存事件消息中推算
            // 消息体中有 beforeStock/afterStock，用 afterStock 作为当前库存
            // safeStock 需要通过产品服务获取，这里用 0 作为默认值（由预警服务自行判断）
            inventoryAlertService.upsertAlert(
                    flower.getId(),
                    flower.getName(),
                    flower.getSafeStock() != null ? flower.getSafeStock() : 0,
                    message.getAfterStock()
            );

            log.info("更新库存预警完成 flowerId={} flowerName={} afterStock={}",
                    flower.getId(),
                    flower.getName(),
                    message.getAfterStock());
        } catch (Exception ex) {
            // 预警更新失败不影响主业务事务，但需要记录日志便于排查
            log.error("库存预警更新失败 inventoryId={} flowerId={} error={}",
                    message.getInventoryId(), message.getFlowerId(), ex.getMessage(), ex);
        }
    }
}
