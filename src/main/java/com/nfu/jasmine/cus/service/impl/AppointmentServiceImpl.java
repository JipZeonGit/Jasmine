package com.nfu.jasmine.cus.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.nfu.jasmine.common.vo.Result;
import com.nfu.jasmine.cus.entity.Appointment;
import com.nfu.jasmine.cus.entity.Vip;
import com.nfu.jasmine.cus.mapper.AppointmentMapper;
import com.nfu.jasmine.cus.mapper.VipMapper;
import com.nfu.jasmine.cus.service.IAppointmentService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Date;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author jipzeongit
 * @since 2023-07-06
 */
@Service
public class AppointmentServiceImpl extends ServiceImpl<AppointmentMapper, Appointment> implements IAppointmentService {
    @Autowired
    private VipMapper vipMapper;
    @Override
    public void addAppointment(@RequestParam(value = "vid", required = false) String vid,
                               @RequestParam(value = "phone", required = false) String phone,
                               @RequestParam(value = "date", required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") Date date,
                               @RequestParam(value = "content", required = false) String content){
        // 当vid不为空或者phone不为空时，从vipMapper里根据vid和phone进行查询，如果查询到有拥有该vid或者phone的Vip对象，则从该对象读取出它的vid,phone,name,sex字段信息,并将这些字段信息和传入的date一起封装回Appointment对象，并在this.baseMapper里新增该条Appointment数据
        Vip vip = null;
        if (vid != null) {
            vip = vipMapper.selectOne(new QueryWrapper<Vip>().eq("vid",vid));
        } else if (phone != null) {
            vip = vipMapper.selectOne(new QueryWrapper<Vip>().eq("phone",phone));
        }

        if (vip != null) {
            Appointment appointment = new Appointment();
            appointment.setVid(vip.getVid());
            appointment.setPhone(vip.getPhone());
            appointment.setName(vip.getName());
            appointment.setSex(vip.getSex());
            appointment.setDate(date);
            appointment.setContent(content);
            this.baseMapper.insert(appointment);
        }
    }
}
