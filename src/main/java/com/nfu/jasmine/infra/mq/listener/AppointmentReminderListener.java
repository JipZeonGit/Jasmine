package com.nfu.jasmine.infra.mq.listener;

import com.nfu.jasmine.appointment.model.entity.Appointment;
import com.nfu.jasmine.appointment.persistence.mapper.AppointmentMapper;
import com.nfu.jasmine.infra.mq.JasmineMqConstants;
import com.nfu.jasmine.infra.mq.MqKeyNames;
import com.nfu.jasmine.infra.mq.message.AppointmentCreatedMessage;
import com.nfu.jasmine.infra.mq.support.MqIdempotencyService;
import com.nfu.jasmine.infra.mq.support.MqMessageSupport;
import com.nfu.jasmine.infra.notification.service.SiteMessageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.text.SimpleDateFormat;

/**
 * 预约提醒消费者。
 * <p>
 * 监听延时驻留队列过期后弹射到的唤醒队列，在消费时先回查预约主库确认预约仍然有效，
 * 然后生成站内信通知店员备货。
 */
@Component
@ConditionalOnProperty(name = "app.mq.enabled", havingValue = "true")
public class AppointmentReminderListener {
    private static final Logger log = LoggerFactory.getLogger(AppointmentReminderListener.class);

    private final SiteMessageService siteMessageService;
    private final AppointmentMapper appointmentMapper;
    private final MqIdempotencyService mqIdempotencyService;

    public AppointmentReminderListener(SiteMessageService siteMessageService,
                                       AppointmentMapper appointmentMapper,
                                       MqIdempotencyService mqIdempotencyService) {
        this.siteMessageService = siteMessageService;
        this.appointmentMapper = appointmentMapper;
        this.mqIdempotencyService = mqIdempotencyService;
    }

    @RabbitListener(queues = JasmineMqConstants.APPOINTMENT_REMINDER_QUEUE)
    public void handle(AppointmentCreatedMessage message) {
        MqMessageSupport.rejectIfNull(message, "message");
        MqMessageSupport.rejectIfNull(message.getAppointmentId(), "appointmentId");

        // 幂等校验：避免重复生成同一条预约的提醒站内信
        String key = MqKeyNames.appointmentReminder(message.getAppointmentId());
        if (!mqIdempotencyService.markIfFirstConsume(key)) {
            log.info("跳过重复预约提醒 appointmentId={}", message.getAppointmentId());
            return;
        }

        // 消费端回查预约主库，过滤掉已删除或已修改时间的过期提醒
        Appointment appointment = appointmentMapper.selectById(message.getAppointmentId());
        if (appointment == null || appointment.getDeleted() == 1) {
            log.info("预约已删除，跳过提醒 appointmentId={}", message.getAppointmentId());
            return;
        }

        // 构造站内信标题和正文
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
        String timeStr = message.getAppointmentTime() != null ? sdf.format(message.getAppointmentTime()) : "未知";
        String title = message.getVipName() + " 预约 " + timeStr + " 到店";

        // 备注内容截断至 50 字符
        String content = message.getContent();
        if (content != null && content.length() > 50) {
            content = content.substring(0, 50) + "...";
        }
        if (content == null || content.isBlank()) {
            content = "无备注";
        }

        siteMessageService.create(
                "APPOINTMENT_REMINDER",
                String.valueOf(message.getAppointmentId()),
                title,
                content
        );
        log.info("已生成预约提醒站内信 appointmentId={} vipName={} time={}",
                message.getAppointmentId(), message.getVipName(), timeStr);
    }
}
