package com.nfu.jasmine.cus.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.nfu.jasmine.common.vo.TableData;
import com.nfu.jasmine.cus.dto.AppointmentCreateDTO;
import com.nfu.jasmine.cus.dto.AppointmentQueryDTO;
import com.nfu.jasmine.cus.dto.AppointmentUpdateDTO;
import com.nfu.jasmine.cus.entity.Appointment;
import com.nfu.jasmine.cus.vo.AppointmentVO;

import java.util.List;

public interface IAppointmentService extends IService<Appointment> {
    List<AppointmentVO> listAppointments();

    TableData<AppointmentVO> pageAppointments(AppointmentQueryDTO queryDTO);

    AppointmentVO getAppointmentDetail(Integer id);

    void createAppointment(AppointmentCreateDTO appointmentDTO);

    void updateAppointment(AppointmentUpdateDTO appointmentDTO);
}
