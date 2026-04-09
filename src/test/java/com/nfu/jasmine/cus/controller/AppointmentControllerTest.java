package com.nfu.jasmine.cus.controller;

import com.nfu.jasmine.common.enums.ResultCode;
import com.nfu.jasmine.common.vo.Result;
import com.nfu.jasmine.cus.dto.AppointmentCreateDTO;
import com.nfu.jasmine.cus.service.IAppointmentService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AppointmentControllerTest {
    @Mock
    private IAppointmentService appointmentService;

    @InjectMocks
    private AppointmentController appointmentController;

    @Test
    void addAppointmentShouldRejectWhenVidAndPhoneAreBothMissing() {
        AppointmentCreateDTO appointmentDTO = new AppointmentCreateDTO();
        appointmentDTO.setDate(new Date());
        appointmentDTO.setContent("到店选花");

        Result<?> result = appointmentController.addAppointment(appointmentDTO);

        assertEquals(ResultCode.VALIDATE_FAILED.getCode(), result.getCode());
        assertEquals("会员卡号或手机号至少填写一项！", result.getMessage());
        assertNull(result.getData());
        verifyNoInteractions(appointmentService);
    }

    @Test
    void addAppointmentShouldReturnNotFoundMessageWhenVipDoesNotExist() {
        AppointmentCreateDTO appointmentDTO = new AppointmentCreateDTO();
        appointmentDTO.setPhone("13677778888");
        appointmentDTO.setDate(new Date());
        appointmentDTO.setContent("预订花束");
        when(appointmentService.addAppointment(eq(null), eq("13677778888"), any(Date.class), eq("预订花束"))).thenReturn(false);

        Result<?> result = appointmentController.addAppointment(appointmentDTO);

        assertEquals(ResultCode.NOT_FOUND.getCode(), result.getCode());
        assertEquals("用户信息不存在，请创建新会员！", result.getMessage());
        verify(appointmentService).addAppointment(eq(null), eq("13677778888"), any(Date.class), eq("预订花束"));
    }
}