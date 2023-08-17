package com.nfu.jasmine.cus.service;

import com.nfu.jasmine.cus.entity.Appointment;
import com.baomidou.mybatisplus.extension.service.IService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.RequestParam;

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
    void addAppointment(@RequestParam(value = "vid", required = false) String vid, @RequestParam(value = "phone", required = false) String phone, @RequestParam(value = "date", required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") Date date, @RequestParam(value = "content", required = false) String content);
}
