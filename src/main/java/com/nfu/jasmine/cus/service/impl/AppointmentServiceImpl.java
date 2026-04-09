package com.nfu.jasmine.cus.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nfu.jasmine.cus.entity.Appointment;
import com.nfu.jasmine.cus.entity.Vip;
import com.nfu.jasmine.cus.mapper.AppointmentMapper;
import com.nfu.jasmine.cus.mapper.VipMapper;
import com.nfu.jasmine.cus.service.IAppointmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

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
    public boolean addAppointment(String vid, String phone, Date date, String content) {
        // 当会员卡号或手机号任一有效时，先查出会员，再回填预约记录中的会员信息。
        Vip vip = null;
        if (StringUtils.hasLength(vid)) {
            vip = vipMapper.selectOne(new QueryWrapper<Vip>().eq("vid", vid));
        } else if (StringUtils.hasLength(phone)) {
            vip = vipMapper.selectOne(new QueryWrapper<Vip>().eq("phone", phone));
        }

        if (vip == null) {
            return false;
        }

        Appointment appointment = new Appointment();
        appointment.setVid(vip.getVid());
        appointment.setPhone(vip.getPhone());
        appointment.setName(vip.getName());
        appointment.setSex(vip.getSex());
        appointment.setDate(date);
        appointment.setContent(content);
        this.baseMapper.insert(appointment);
        return true;
    }
}
