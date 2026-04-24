package com.nfu.jasmine.infra.mq;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.nfu.jasmine.appointment.application.IAppointmentService;
import com.nfu.jasmine.appointment.web.dto.AppointmentCreateDTO;
import com.nfu.jasmine.config.AbstractIntegrationTest;
import com.nfu.jasmine.infra.notification.model.entity.SiteMessage;
import com.nfu.jasmine.infra.notification.persistence.mapper.SiteMessageMapper;
import com.nfu.jasmine.infra.outbox.model.entity.EventOutbox;
import com.nfu.jasmine.infra.outbox.persistence.mapper.EventOutboxMapper;
import com.nfu.jasmine.infra.outbox.relay.OutboxRelay;
import com.nfu.jasmine.vip.model.entity.Vip;
import com.nfu.jasmine.vip.persistence.mapper.VipMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class AppointmentReminderIntegrationIT extends AbstractIntegrationTest {

    @Autowired
    private VipMapper vipMapper;

    @Autowired
    private IAppointmentService appointmentService;

    @Autowired
    private EventOutboxMapper eventOutboxMapper;

    @Autowired
    private OutboxRelay outboxRelay;

    @Autowired
    private SiteMessageMapper siteMessageMapper;

    @Test
    void shouldRelayDelayedMessageAndCreateSiteMessage() throws Exception {
        // 1. 创建前置会员数据
        Vip vip = new Vip();
        vip.setVid("V9999");
        vip.setName("延时张总");
        vip.setPhone("13800009999");
        vipMapper.insert(vip);

        // 2. 模拟创建一个 1小时 + 3秒 后到店的预约单
        // 根据业务规则：delayMs = 预约时间 - 当前时间 - 1小时
        // 期望：delayMs 约等于 3000 ms
        Date targetDate = new Date(System.currentTimeMillis() + 3600_000L + 3000L);
        
        AppointmentCreateDTO createDTO = new AppointmentCreateDTO();
        createDTO.setVipId(vip.getId());
        createDTO.setDate(targetDate);
        createDTO.setContent("请准备好99朵卡罗拉红玫瑰，测试延时死信队列专用！");
        
        appointmentService.createAppointment(createDTO);

        // 3. 验证 Outbox 已产生，并且 delay_ms 包含合理值 (2000ms ~ 4000ms之间)
        List<EventOutbox> outboxes = eventOutboxMapper.selectList(new LambdaQueryWrapper<EventOutbox>()
                .eq(EventOutbox::getRoutingKey, JasmineMqConstants.APPOINTMENT_DELAY_ROUTING_KEY));
        assertThat(outboxes).hasSize(1);
        EventOutbox delayOutbox = outboxes.get(0);
        assertThat(delayOutbox.getDelayMs()).isBetween(2000L, 4000L);

        // 修改创建时间和 nextRetryTime 确保能立即被 Relay 扫到并投递
        delayOutbox.setCreatedAt(new Date());
        delayOutbox.setNextRetryTime(new Date());
        eventOutboxMapper.updateById(delayOutbox);

        // 4. 触发 Relay 将其投入死信驻留队列 (Delay Queue)
        outboxRelay.relayPendingMessages();

        // 5. 轮询等待（最长等待 10 秒，因为前面设置了约 3 秒的 TTL 延时）
        // 在这段时间里：消息在 delay queue 存活 3 秒 -> 过期弹射到 reminder queue -> 消费者拉取写入 site_message
        boolean isAlerted = false;
        for (int i = 0; i < 20; i++) {
            Thread.sleep(500); // 每次等半秒
            
            SiteMessage siteMessage = siteMessageMapper.selectOne(
                    new LambdaQueryWrapper<SiteMessage>().eq(SiteMessage::getBizType, "APPOINTMENT_REMINDER")
            );
            
            if (siteMessage != null) {
                isAlerted = true;
                assertThat(siteMessage.getTitle()).contains("延时张总");
                assertThat(siteMessage.getContent()).contains("卡罗拉红玫瑰");
                assertThat(siteMessage.getIsRead()).isEqualTo(0);
                break;
            }
        }

        assertThat(isAlerted).as("消费者应当在 TTL 过期后成功从死信唤醒队列拉取到消息并写入站内信").isTrue();
    }
}
