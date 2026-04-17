package com.nfu.jasmine.appointment.web;

import com.nfu.jasmine.common.enums.ResultCode;
import com.nfu.jasmine.common.vo.Result;
import com.nfu.jasmine.appointment.web.dto.AppointmentCreateDTO;
import com.nfu.jasmine.appointment.application.IAppointmentService;
import com.nfu.jasmine.infra.idempotency.RequestIdempotencyService;
import jakarta.servlet.http.HttpServletRequest;
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
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AppointmentControllerTest {
    @Mock
    private IAppointmentService appointmentService;

    @Mock
    private RequestIdempotencyService requestIdempotencyService;

    @Mock
    private HttpServletRequest request;

    @InjectMocks
    private AppointmentController appointmentController;

    @Test
    void addAppointmentShouldDelegateToService() {
        AppointmentCreateDTO appointmentDTO = new AppointmentCreateDTO();
        appointmentDTO.setPhone("13677778888");
        appointmentDTO.setDate(new Date());
        appointmentDTO.setContent("到店选花");

        doAnswer(invocation -> {
            Runnable action = invocation.getArgument(4);
            action.run();
            return null;
        }).when(requestIdempotencyService).executeCreate(eq("appointment:create"), eq(null), eq(null), eq(appointmentDTO), any(Runnable.class));

        Result<?> result = appointmentController.addAppointment(appointmentDTO, null, request);

        assertEquals(ResultCode.SUCCESS.getCode(), result.getCode());
        assertEquals("新增预约成功！", result.getMessage());
        assertNull(result.getData());
        verify(requestIdempotencyService).executeCreate(eq("appointment:create"), eq(null), eq(null), eq(appointmentDTO), any(Runnable.class));
        verify(appointmentService).createAppointment(appointmentDTO);
    }
}
