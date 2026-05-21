package com.nfu.jasmine.infra.mq.listener;

import com.nfu.jasmine.appointment.model.entity.Appointment;
import com.nfu.jasmine.appointment.persistence.mapper.AppointmentMapper;
import com.nfu.jasmine.infra.client.CrmUserClient;
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
import java.util.List;

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

    /** 预约提醒站内信的接收角色列表 */
    private static final List<String> REMINDER_RECEIVER_ROLES = List.of("admin", "Boss", "clerk");

    private final SiteMessageService siteMessageService;
    private final AppointmentMapper appointmentMapper;
    private final MqIdempotencyService mqIdempotencyService;
    private final CrmUserClient crmUserClient;

    public AppointmentReminderListener(SiteMessageService siteMessageService,
                                       AppointmentMapper appointmentMapper,
                                       MqIdempotencyService mqIdempotencyService,
                                       CrmUserClient crmUserClient) {
        this.siteMessageService = siteMessageService;
        this.appointmentMapper = appointmentMapper;
        this.mqIdempotencyService = mqIdempotencyService;
        this.crmUserClient = crmUserClient;
    }

    @RabbitListener(queues = JasmineMqConstants.APPOINTMENT_REMINDER_QUEUE)
    public void handle(AppointmentCreatedMessage message) {
        MqMessageSupport.rejectIfNull(message, "message");
        MqMessageSupport.rejectIfNull(message.getAppointmentId(), "appointmentId");

        // 幂等校验：加上时间戳，允许改签后生成新的提醒，同时避免同一时间的重复提醒
        long timeMills = message.getAppointmentTime() != null ? message.getAppointmentTime().getTime() : 0;
        String key = MqKeyNames.appointmentReminder(message.getAppointmentId(), timeMills);
        if (!mqIdempotencyService.markIfFirstConsume(key)) {
            log.info("跳过重复预约提醒 appointmentId={}", message.getAppointmentId());
            return;
        }

        // 消费端回查预约主库，过滤掉已删除
        Appointment appointment = appointmentMapper.selectById(message.getAppointmentId());
        if (appointment == null || appointment.getDeleted() == 1) {
            log.info("预约已删除，跳过提醒 appointmentId={}", message.getAppointmentId());
            return;
        }
        
        // 过滤改签留下的"幽灵消息"：判断数据库最新时间与消息体时间是否一致（容差1分钟）
        if (appointment.getDate() != null && message.getAppointmentTime() != null) {
            long diff = Math.abs(appointment.getDate().getTime() - message.getAppointmentTime().getTime());
            if (diff > 60_000) {
                log.info("预约已改签，跳过旧时间提醒 appointmentId={} dbTime={} msgTime={}", 
                        message.getAppointmentId(), appointment.getDate(), message.getAppointmentTime());
                return;
            }
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

        // 通过远程接口查询活跃用户
        List<Integer> receiverUserIds = crmUserClient.getActiveUserIdsByRoles(REMINDER_RECEIVER_ROLES);
        siteMessageService.createForUsers(
                "APPOINTMENT_REMINDER",
                String.valueOf(message.getAppointmentId()),
                receiverUserIds,
                title,
                content
        );
        log.info("已生成预约提醒站内信 appointmentId={} vipName={} time={}",
                message.getAppointmentId(), message.getVipName(), timeStr);
    }
}
