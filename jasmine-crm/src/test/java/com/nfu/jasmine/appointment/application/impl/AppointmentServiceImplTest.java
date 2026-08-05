package com.nfu.jasmine.appointment.application.impl;

import com.nfu.jasmine.appointment.model.entity.Appointment;
import com.nfu.jasmine.appointment.persistence.mapper.AppointmentMapper;
import com.nfu.jasmine.appointment.web.dto.AppointmentCreateDTO;
import com.nfu.jasmine.appointment.web.dto.AppointmentQueryDTO;
import com.nfu.jasmine.appointment.web.dto.AppointmentUpdateDTO;
import com.nfu.jasmine.appointment.web.vo.AppointmentVO;
import com.nfu.jasmine.common.exception.BusinessException;
import com.nfu.jasmine.common.vo.TableData;
import com.nfu.jasmine.infra.mq.message.AppointmentCreatedMessage;
import com.nfu.jasmine.infra.mq.publisher.MqMessagePublisher;
import com.nfu.jasmine.vip.application.support.VipReadFacade;
import com.nfu.jasmine.vip.model.entity.Vip;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Date;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * AppointmentServiceImpl 单元测试。
 * <p>
 * 覆盖 createAppointment 的事务与延时提醒事件、updateAppointment 的更新与重发提醒、
 * 以及查询方法的分支（如按会员名/手机号查询无命中时返回空）。
 */
@ExtendWith(MockitoExtension.class)
class AppointmentServiceImplTest {

    @Mock
    private AppointmentMapper appointmentMapper;
    @Mock
    private VipReadFacade vipReadFacade;
    @Mock
    private MqMessagePublisher mqMessagePublisher;

    @InjectMocks
    private AppointmentServiceImpl appointmentService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(appointmentService, "baseMapper", appointmentMapper);
    }

    // ==================== createAppointment ====================

    @Test
    void createAppointmentShouldSaveAndPublishCreatedAndReminderEvents() {
        AppointmentCreateDTO dto = new AppointmentCreateDTO();
        dto.setVipId(1);
        dto.setDate(new Date(System.currentTimeMillis() + 7200_000L)); // 2 小时后
        dto.setContent("预约修剪花束");

        Vip vip = buildVip(1, "V001", "张三", "13800138000");
        when(vipReadFacade.resolve(eq(1), any(), any())).thenReturn(vip);

        appointmentService.createAppointment(dto);

        ArgumentCaptor<Appointment> appointmentCaptor = ArgumentCaptor.forClass(Appointment.class);
        verify(appointmentMapper).insert(appointmentCaptor.capture());
        Appointment saved = appointmentCaptor.getValue();
        assertThat(saved.getVipId()).isEqualTo(1);
        assertThat(saved.getContent()).isEqualTo("预约修剪花束");
        assertThat(saved.getDeleted()).isEqualTo(0);

        // 验证发了"创建"事件 + "延时提醒"事件
        verify(mqMessagePublisher).publishAppointmentCreatedAfterCommit(any(AppointmentCreatedMessage.class));
        // delayMs = 预约时间 - now - 1小时 = 2小时 - 1小时 = 1小时（正数即可）
        verify(mqMessagePublisher).publishAppointmentReminderDelayed(
                any(AppointmentCreatedMessage.class), anyLong());
    }

    @Test
    void createAppointmentShouldThrowWhenVipNotFound() {
        AppointmentCreateDTO dto = new AppointmentCreateDTO();
        dto.setVipId(999);
        dto.setDate(new Date());
        dto.setContent("内容");
        when(vipReadFacade.resolve(eq(999), any(), any())).thenReturn(null);

        assertThatThrownBy(() -> appointmentService.createAppointment(dto))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("会员不存在");

        verify(appointmentMapper, never()).insert(any(Appointment.class));
        verify(mqMessagePublisher, never()).publishAppointmentCreatedAfterCommit(any());
    }

    // ==================== updateAppointment ====================

    @Test
    void updateAppointmentShouldUpdateAndRepublishReminder() {
        Appointment existing = new Appointment();
        existing.setId(10);
        existing.setVipId(1);
        existing.setDate(new Date(System.currentTimeMillis() - 3600_000L)); // 旧时间
        existing.setContent("旧内容");
        when(appointmentMapper.selectById(10)).thenReturn(existing);

        Vip vip = buildVip(1, "V001", "张三", "13800138000");
        when(vipReadFacade.findById(1)).thenReturn(vip);

        AppointmentUpdateDTO dto = new AppointmentUpdateDTO();
        dto.setId(10);
        dto.setDate(new Date(System.currentTimeMillis() + 7200_000L)); // 新时间：2 小时后
        dto.setContent("新内容");

        appointmentService.updateAppointment(dto);

        ArgumentCaptor<Appointment> appointmentCaptor = ArgumentCaptor.forClass(Appointment.class);
        verify(appointmentMapper).updateById(appointmentCaptor.capture());
        Appointment updated = appointmentCaptor.getValue();
        assertThat(updated.getContent()).isEqualTo("新内容");

        // 验证重发了延时提醒
        verify(mqMessagePublisher).publishAppointmentReminderDelayed(
                any(AppointmentCreatedMessage.class), anyLong());
    }

    @Test
    void updateAppointmentShouldThrowWhenNotFound() {
        AppointmentUpdateDTO dto = new AppointmentUpdateDTO();
        dto.setId(999);
        dto.setDate(new Date());
        dto.setContent("内容");
        when(appointmentMapper.selectById(999)).thenReturn(null);

        assertThatThrownBy(() -> appointmentService.updateAppointment(dto))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("预约记录不存在");

        verify(appointmentMapper, never()).updateById(any(Appointment.class));
        verify(mqMessagePublisher, never()).publishAppointmentReminderDelayed(any(), anyLong());
    }

    @Test
    void updateAppointmentShouldNotPublishReminderWhenVipDeleted() {
        // 会员已删除时，不应发提醒事件
        Appointment existing = new Appointment();
        existing.setId(10);
        existing.setVipId(1);
        existing.setDate(new Date());
        existing.setContent("旧内容");
        when(appointmentMapper.selectById(10)).thenReturn(existing);
        when(vipReadFacade.findById(1)).thenReturn(null);

        AppointmentUpdateDTO dto = new AppointmentUpdateDTO();
        dto.setId(10);
        dto.setDate(new Date(System.currentTimeMillis() + 7200_000L));
        dto.setContent("新内容");

        appointmentService.updateAppointment(dto);

        verify(appointmentMapper).updateById(any(Appointment.class));
        verify(mqMessagePublisher, never()).publishAppointmentReminderDelayed(any(), anyLong());
    }

    // ==================== 查询方法 ====================

    @Test
    void getAppointmentDetailShouldReturnVo() {
        Appointment appointment = new Appointment();
        appointment.setId(10);
        appointment.setVipId(1);
        appointment.setDate(new Date());
        appointment.setContent("内容");
        when(appointmentMapper.selectById(10)).thenReturn(appointment);

        Vip vip = buildVip(1, "V001", "张三", "13800138000");
        when(vipReadFacade.findByIds(any(Set.class)))
                .thenReturn(java.util.Map.of(1, vip));

        AppointmentVO vo = appointmentService.getAppointmentDetail(10);

        assertThat(vo).isNotNull();
        assertThat(vo.getId()).isEqualTo(10);
        assertThat(vo.getName()).isEqualTo("张三");
        assertThat(vo.getVid()).isEqualTo("V001");
    }

    @Test
    void getAppointmentDetailShouldThrowWhenNotFound() {
        when(appointmentMapper.selectById(999)).thenReturn(null);

        assertThatThrownBy(() -> appointmentService.getAppointmentDetail(999))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("预约记录不存在");
    }

    @Test
    void pageAppointmentsByNameOrPhoneWithNoMatchShouldReturnEmpty() {
        AppointmentQueryDTO queryDTO = new AppointmentQueryDTO();
        queryDTO.setName("不存在的会员");
        queryDTO.setPageNo(1L);
        queryDTO.setPageSize(10L);
        when(vipReadFacade.findIdsByNameOrPhone(eq("不存在的会员"), any()))
                .thenReturn(List.of());

        TableData<AppointmentVO> result = appointmentService.pageAppointments(queryDTO);

        assertThat(result.getTotal()).isEqualTo(0L);
        assertThat(result.getRows()).isEmpty();
        verify(appointmentMapper, never()).selectPage(any(), any());
    }

    // ==================== helper ====================

    private Vip buildVip(Integer id, String vid, String name, String phone) {
        Vip vip = new Vip();
        vip.setId(id);
        vip.setVid(vid);
        vip.setName(name);
        vip.setPhone(phone);
        vip.setSex("男");
        return vip;
    }
}
