package com.nfu.jasmine.appointment.application;

import com.baomidou.mybatisplus.extension.service.IService;
import com.nfu.jasmine.common.vo.TableData;
import com.nfu.jasmine.appointment.web.dto.AppointmentCreateDTO;
import com.nfu.jasmine.appointment.web.dto.AppointmentQueryDTO;
import com.nfu.jasmine.appointment.web.dto.AppointmentUpdateDTO;
import com.nfu.jasmine.appointment.model.entity.Appointment;
import com.nfu.jasmine.appointment.web.vo.AppointmentVO;

import java.util.List;

public interface IAppointmentService extends IService<Appointment> {
    List<AppointmentVO> listAppointments();

    TableData<AppointmentVO> pageAppointments(AppointmentQueryDTO queryDTO);

    AppointmentVO getAppointmentDetail(Integer id);

    void createAppointment(AppointmentCreateDTO appointmentDTO);

    void updateAppointment(AppointmentUpdateDTO appointmentDTO);
}
