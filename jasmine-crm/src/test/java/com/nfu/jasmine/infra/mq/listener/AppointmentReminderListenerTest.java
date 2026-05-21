package com.nfu.jasmine.infra.mq.listener;

import com.nfu.jasmine.appointment.model.entity.Appointment;
import com.nfu.jasmine.appointment.persistence.mapper.AppointmentMapper;
import com.nfu.jasmine.infra.client.CrmUserClient;
import com.nfu.jasmine.infra.mq.MqKeyNames;
import com.nfu.jasmine.infra.mq.message.AppointmentCreatedMessage;
import com.nfu.jasmine.infra.mq.support.MqIdempotencyService;
import com.nfu.jasmine.infra.mq.support.MqMessageSupport;
import com.nfu.jasmine.infra.notification.service.SiteMessageService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Date;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AppointmentReminderListenerTest {

    @Mock
    private SiteMessageService siteMessageService;
    @Mock
    private AppointmentMapper appointmentMapper;
    @Mock
    private MqIdempotencyService mqIdempotencyService;
    @Mock
    private CrmUserClient crmUserClient;

    @InjectMocks
    private AppointmentReminderListener listener;

    @Test
    void shouldCreateSiteMessageForValidAppointment() {
        AppointmentCreatedMessage message = createMessage(1, "张三", "13800001111");
        Appointment appointment = createAppointment(1, new Date());

        when(mqIdempotencyService.markIfFirstConsume(any())).thenReturn(true);
        when(appointmentMapper.selectById(1)).thenReturn(appointment);
        when(crmUserClient.getActiveUserIdsByRoles(List.of("admin", "Boss", "clerk")))
                .thenReturn(List.of(1, 2));

        listener.handle(message);

        verify(siteMessageService).createForUsers(
                eq("APPOINTMENT_REMINDER"),
                eq("1"),
                eq(List.of(1, 2)),
                any(),
                any()
        );
    }

    @Test
    void shouldSkipWhenIdempotencyCheckFails() {
        AppointmentCreatedMessage message = createMessage(1, "张三", "13800001111");

        when(mqIdempotencyService.markIfFirstConsume(any())).thenReturn(false);

        listener.handle(message);

        verify(siteMessageService, never()).createForUsers(any(), any(), any(), any(), any());
    }

    @Test
    void shouldSkipWhenAppointmentDeleted() {
        AppointmentCreatedMessage message = createMessage(1, "张三", "13800001111");
        Appointment appointment = createAppointment(1, new Date());
        appointment.setDeleted(1);

        when(mqIdempotencyService.markIfFirstConsume(any())).thenReturn(true);
        when(appointmentMapper.selectById(1)).thenReturn(appointment);

        listener.handle(message);

        verify(siteMessageService, never()).createForUsers(any(), any(), any(), any(), any());
    }

    @Test
    void shouldSkipWhenAppointmentNotFound() {
        AppointmentCreatedMessage message = createMessage(1, "张三", "13800001111");

        when(mqIdempotencyService.markIfFirstConsume(any())).thenReturn(true);
        when(appointmentMapper.selectById(1)).thenReturn(null);

        listener.handle(message);

        verify(siteMessageService, never()).createForUsers(any(), any(), any(), any(), any());
    }

    @Test
    void shouldSkipWhenAppointmentRescheduled() {
        Date originalTime = new Date(1700000000000L);
        Date rescheduledTime = new Date(1700000000000L + 120_000L); // 2 分钟后，超过 1 分钟容差

        AppointmentCreatedMessage message = createMessage(1, "张三", "13800001111");
        message.setAppointmentTime(originalTime);

        Appointment appointment = createAppointment(1, rescheduledTime);

        when(mqIdempotencyService.markIfFirstConsume(any())).thenReturn(true);
        when(appointmentMapper.selectById(1)).thenReturn(appointment);

        listener.handle(message);

        verify(siteMessageService, never()).createForUsers(any(), any(), any(), any(), any());
    }

    private AppointmentCreatedMessage createMessage(Integer appointmentId, String vipName, String vipPhone) {
        AppointmentCreatedMessage message = new AppointmentCreatedMessage();
        message.setAppointmentId(appointmentId);
        message.setVipId(1);
        message.setVipName(vipName);
        message.setVipPhone(vipPhone);
        message.setAppointmentTime(new Date());
        message.setContent("选花");
        message.setOccurredAt(new Date());
        return message;
    }

    private Appointment createAppointment(Integer id, Date date) {
        Appointment appointment = new Appointment();
        appointment.setId(id);
        appointment.setVipId(1);
        appointment.setDate(date);
        appointment.setContent("选花");
        appointment.setDeleted(0);
        return appointment;
    }
}
