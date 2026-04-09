package com.nfu.jasmine.cus.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.nfu.jasmine.cus.entity.Appointment;

import java.util.Date;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author jipzeongit
 * @since 2023-07-06
 */
public interface IAppointmentService extends IService<Appointment> {
    boolean addAppointment(String vid, String phone, Date date, String content);
}
