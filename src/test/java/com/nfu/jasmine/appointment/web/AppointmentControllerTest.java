package com.nfu.jasmine.appointment.web;

import com.nfu.jasmine.common.enums.ResultCode;
import com.nfu.jasmine.common.vo.Result;
import com.nfu.jasmine.appointment.web.dto.AppointmentCreateDTO;
import com.nfu.jasmine.appointment.application.IAppointmentService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AppointmentControllerTest {
    @Mock
    private IAppointmentService appointmentService;

    @InjectMocks
    private AppointmentController appointmentController;

    @Test
    void addAppointmentShouldDelegateToService() {
        AppointmentCreateDTO appointmentDTO = new AppointmentCreateDTO();
        appointmentDTO.setPhone("13677778888");
        appointmentDTO.setDate(new Date());
        appointmentDTO.setContent("到店选花");

        Result<?> result = appointmentController.addAppointment(appointmentDTO);

        assertEquals(ResultCode.SUCCESS.getCode(), result.getCode());
        assertEquals("新增预约成功！", result.getMessage());
        assertNull(result.getData());
        verify(appointmentService).createAppointment(appointmentDTO);
    }
}
