package com.nfu.jasmine.infra.mq.listener;

import com.nfu.jasmine.infra.mq.JasmineMqConstants;
import com.nfu.jasmine.infra.mq.MqKeyNames;
import com.nfu.jasmine.infra.mq.message.SalesCreatedMessage;
import com.nfu.jasmine.infra.mq.support.MqIdempotencyService;
import com.nfu.jasmine.infra.mq.support.MqMessageSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 销售事件消费者。
 * <p>
 * 消费模式：前置校验 -> 幂等去重 -> 业务处理（当前为日志模拟，后续接报表/审计）。
 */
@Component
@ConditionalOnProperty(name = "app.mq.enabled", havingValue = "true")
public class SalesEventListener {
    private static final Logger log = LoggerFactory.getLogger(SalesEventListener.class);

    private final MqIdempotencyService mqIdempotencyService;

    public SalesEventListener(MqIdempotencyService mqIdempotencyService) {
        this.mqIdempotencyService = mqIdempotencyService;
    }

    @RabbitListener(queues = JasmineMqConstants.SALES_EVENT_QUEUE)
    public void onSalesCreated(SalesCreatedMessage message) {
        MqMessageSupport.rejectIfNull(message, "message");
        MqMessageSupport.rejectIfNull(message.getSalesId(), "salesId");
        MqMessageSupport.rejectIfBlank(message.getOrderNo(), "orderNo");

        String key = MqKeyNames.salesCreated(message.getSalesId());
        if (!mqIdempotencyService.markIfFirstConsume(key)) {
            log.info("跳过重复销售事件 salesId={}", message.getSalesId());
            return;
        }
        // 第一版先把销售事件稳定发布和消费起来，后面再让统计或审计真正接这个事件做下游处理。
        log.info("模拟消费销售事件 salesId={} orderNo={} vipId={} operatorId={} itemCount={} totalAmount={} salesTime={}",
                message.getSalesId(),
                message.getOrderNo(),
                message.getVipId(),
                message.getOperatorId(),
                message.getItemCount(),
                message.getTotalAmount(),
                message.getSalesTime());
    }
}
