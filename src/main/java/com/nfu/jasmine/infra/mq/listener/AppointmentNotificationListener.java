package com.nfu.jasmine.infra.mq.listener;

import com.nfu.jasmine.infra.mq.JasmineMqConstants;
import com.nfu.jasmine.infra.mq.MqKeyNames;
import com.nfu.jasmine.infra.mq.message.AppointmentCreatedMessage;
import com.nfu.jasmine.infra.mq.support.MqIdempotencyService;
import com.nfu.jasmine.infra.mq.support.MqMessageSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 预约通知消费者。
 * <p>
 * 当前先用日志模拟通知发送，确认消息链跑通后再接短信或企业微信等真实通知渠道。
 */
@Component
@ConditionalOnProperty(name = "app.mq.enabled", havingValue = "true")
public class AppointmentNotificationListener {
    private static final Logger log = LoggerFactory.getLogger(AppointmentNotificationListener.class);

    private final MqIdempotencyService mqIdempotencyService;

    public AppointmentNotificationListener(MqIdempotencyService mqIdempotencyService) {
        this.mqIdempotencyService = mqIdempotencyService;
    }

    @RabbitListener(queues = JasmineMqConstants.APPOINTMENT_NOTIFICATION_QUEUE)
    public void handle(AppointmentCreatedMessage message) {
        MqMessageSupport.rejectIfNull(message, "message");
        MqMessageSupport.rejectIfNull(message.getAppointmentId(), "appointmentId");
        MqMessageSupport.rejectIfNull(message.getVipId(), "vipId");
        MqMessageSupport.rejectIfBlank(message.getVipPhone(), "vipPhone");

        String key = MqKeyNames.appointmentNotification(message.getAppointmentId());
        if (!mqIdempotencyService.markIfFirstConsume(key)) {
            log.info("跳过重复预约通知消息 appointmentId={}", message.getAppointmentId());
            return;
        }
        // 当前先用日志模拟通知发送，确认消息链跑通后，再接短信或企业微信之类的真实通知渠道。
        log.info("模拟发送预约通知 appointmentId={} vipId={} vipName={} phone={} appointmentTime={} content={}",
                message.getAppointmentId(),
                message.getVipId(),
                message.getVipName(),
                message.getVipPhone(),
                message.getAppointmentTime(),
                message.getContent());
    }
}
