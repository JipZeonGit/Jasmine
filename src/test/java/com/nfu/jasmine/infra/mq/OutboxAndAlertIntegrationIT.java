package com.nfu.jasmine.infra.mq;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nfu.jasmine.config.AbstractIntegrationTest;
import com.nfu.jasmine.infra.mq.message.InventoryChangeSource;
import com.nfu.jasmine.infra.mq.message.InventoryChangedMessage;
import com.nfu.jasmine.infra.outbox.model.entity.EventOutbox;
import com.nfu.jasmine.infra.outbox.model.enums.OutboxStatus;
import com.nfu.jasmine.infra.outbox.persistence.mapper.EventOutboxMapper;
import com.nfu.jasmine.infra.outbox.relay.OutboxRelay;
import com.nfu.jasmine.inventory.alert.model.entity.InventoryAlert;
import com.nfu.jasmine.inventory.alert.model.enums.AlertStatus;
import com.nfu.jasmine.inventory.alert.persistence.mapper.InventoryAlertMapper;
import com.nfu.jasmine.flower.model.entity.Flower;
import com.nfu.jasmine.flower.persistence.mapper.FlowerMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class OutboxAndAlertIntegrationIT extends AbstractIntegrationTest {

    @Autowired
    private EventOutboxMapper eventOutboxMapper;

    @Autowired
    private OutboxRelay outboxRelay;

    @Autowired
    private InventoryAlertMapper inventoryAlertMapper;

    @Autowired
    private FlowerMapper flowerMapper;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldRelayOutboxMessageAndConsumeSuccessfully() throws Exception {
        // 1. 准备大前提条件：一盆花，安全库存为10，当前库存跌到5
        Flower flower = new Flower();
        flower.setName("百合花-集成测试");
        flower.setUnit("枝");
        flower.setCostPrice(new BigDecimal("10.00"));
        flower.setSalePrice(new BigDecimal("20.00"));
        flower.setSafeStock(10);
        flower.setCurrentStock(5);
        flower.setStatus(1);
        flowerMapper.insert(flower);

        // 2. 模拟本地事务写 Outbox
        InventoryChangedMessage message = new InventoryChangedMessage();
        message.setInventoryId(9999);
        message.setFlowerId(flower.getId());
        message.setQuantity(-5);
        message.setChangeSource(InventoryChangeSource.MANUAL_INVENTORY);
        message.setChangeAction("UPDATE");
        message.setBizNo(UUID.randomUUID().toString());
        message.setBizTime(new java.util.Date());
        message.setOccurredAt(new java.util.Date());

        EventOutbox outbox = EventOutbox.builder()
                .eventType("inventory.changed")
                .exchange(JasmineMqConstants.TRADE_EVENT_EXCHANGE)
                .routingKey(JasmineMqConstants.INVENTORY_CHANGED_ROUTING_KEY)
                .payload(objectMapper.writeValueAsString(message))
                .status(OutboxStatus.PENDING.name())
                .retryCount(0)
                .nextRetryTime(new java.util.Date())
                .createdAt(new java.util.Date())
                .build();
        eventOutboxMapper.insert(outbox);

        assertThat(outbox.getId()).isNotNull();

        // 3. 触发 OutboxRelay 扫表推送
        outboxRelay.relayPendingMessages();

        // 4. 断言 MQ Confirm Callback 反写了 Outbox 状态并且消费者成功消费写入预警表
        // 采用简单的轮询等待（最长等待 10 秒），等待异步处理完成
        boolean isSent = false;
        boolean isAlerted = false;
        
        for (int i = 0; i < 20; i++) {
            Thread.sleep(500); // 每次等半秒

            if (!isSent) {
                EventOutbox updatedOutbox = eventOutboxMapper.selectById(outbox.getId());
                if (updatedOutbox != null && OutboxStatus.SENT.name().equals(updatedOutbox.getStatus())) {
                    isSent = true;
                }
            }

            if (!isAlerted) {
                InventoryAlert alert = inventoryAlertMapper.selectOne(
                        new LambdaQueryWrapper<InventoryAlert>().eq(InventoryAlert::getFlowerId, flower.getId())
                );
                if (alert != null && AlertStatus.LOW_STOCK.name().equals(alert.getAlertStatus())) {
                    isAlerted = true;
                    assertThat(alert.getCurrentStock()).isEqualTo(5);
                    assertThat(alert.getSafeStock()).isEqualTo(10);
                }
            }

            if (isSent && isAlerted) {
                break;
            }
        }

        assertThat(isSent).as("Outbox 消息应当被正常标记为 SENT").isTrue();
        assertThat(isAlerted).as("消费者应当成功捕获并拉起低库存预警").isTrue();
    }
}
