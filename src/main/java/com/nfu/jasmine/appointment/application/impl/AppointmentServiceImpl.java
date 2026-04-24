package com.nfu.jasmine.appointment.application.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nfu.jasmine.appointment.application.IAppointmentService;
import com.nfu.jasmine.appointment.model.entity.Appointment;
import com.nfu.jasmine.appointment.persistence.mapper.AppointmentMapper;
import com.nfu.jasmine.appointment.web.dto.AppointmentCreateDTO;
import com.nfu.jasmine.appointment.web.dto.AppointmentQueryDTO;
import com.nfu.jasmine.appointment.web.dto.AppointmentUpdateDTO;
import com.nfu.jasmine.appointment.web.vo.AppointmentVO;
import com.nfu.jasmine.common.enums.ResultCode;
import com.nfu.jasmine.common.exception.BusinessException;
import com.nfu.jasmine.common.vo.TableData;
import com.nfu.jasmine.infra.mq.message.AppointmentCreatedMessage;
import com.nfu.jasmine.infra.mq.publisher.MqMessagePublisher;
import com.nfu.jasmine.vip.model.entity.Vip;
import com.nfu.jasmine.vip.persistence.mapper.VipMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AppointmentServiceImpl extends ServiceImpl<AppointmentMapper, Appointment> implements IAppointmentService {
    @Autowired
    private VipMapper vipMapper;

    @Autowired
    private MqMessagePublisher mqMessagePublisher;

    @Override
    public List<AppointmentVO> listAppointments() {
        LambdaQueryWrapper<Appointment> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(Appointment::getDate).orderByDesc(Appointment::getId);
        return buildAppointmentVOs(this.list(wrapper));
    }

    @Override
    public TableData<AppointmentVO> pageAppointments(AppointmentQueryDTO queryDTO) {
        LambdaQueryWrapper<Appointment> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(Appointment::getDate).orderByDesc(Appointment::getId);

        if (queryDTO.getStartTime() != null) {
            wrapper.ge(Appointment::getDate, queryDTO.getStartTime());
        }
        if (queryDTO.getEndTime() != null) {
            wrapper.le(Appointment::getDate, queryDTO.getEndTime());
        }

        // 预约表只保留 vipId，姓名和手机号查询要先映射到会员，再回查预约。
        if (StringUtils.hasLength(queryDTO.getName()) || StringUtils.hasLength(queryDTO.getPhone())) {
            LambdaQueryWrapper<Vip> vipWrapper = new LambdaQueryWrapper<>();
            vipWrapper.like(StringUtils.hasLength(queryDTO.getName()), Vip::getName, queryDTO.getName());
            vipWrapper.like(StringUtils.hasLength(queryDTO.getPhone()), Vip::getPhone, queryDTO.getPhone());
            List<Integer> vipIds = vipMapper.selectList(vipWrapper).stream().map(Vip::getId).toList();
            if (vipIds.isEmpty()) {
                TableData<AppointmentVO> empty = new TableData<>();
                empty.setTotal(0L);
                empty.setRows(Collections.emptyList());
                return empty;
            }
            wrapper.in(Appointment::getVipId, vipIds);
        }

        Page<Appointment> page = new Page<>(queryDTO.getPageNo(), queryDTO.getPageSize());
        this.page(page, wrapper);

        TableData<AppointmentVO> data = new TableData<>();
        data.setTotal(page.getTotal());
        data.setRows(buildAppointmentVOs(page.getRecords()));
        return data;
    }

    @Override
    public AppointmentVO getAppointmentDetail(Integer id) {
        Appointment appointment = this.getById(id);
        if (appointment == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "预约记录不存在！");
        }
        return buildAppointmentVOs(List.of(appointment)).stream().findFirst().orElse(null);
    }

    @Override
    @Transactional
    public void createAppointment(AppointmentCreateDTO appointmentDTO) {
        // 新模型下预约必须绑定真实会员，禁止再落成一条松散文本预约。
        Vip vip = resolveVip(appointmentDTO.getVipId(), appointmentDTO.getVid(), appointmentDTO.getPhone());
        Appointment appointment = new Appointment();
        appointment.setVipId(vip.getId());
        appointment.setDate(appointmentDTO.getDate());
        appointment.setContent(appointmentDTO.getContent());
        appointment.setDeleted(0);
        this.save(appointment);

        // 预约创建成功事件
        AppointmentCreatedMessage createdMessage = new AppointmentCreatedMessage(
                appointment.getId(),
                vip.getId(),
                vip.getName(),
                vip.getPhone(),
                appointment.getDate(),
                appointment.getContent(),
                new Date()
        );
        mqMessagePublisher.publishAppointmentCreatedAfterCommit(createdMessage);

        // 延时提醒：预约时间 - 当前时间 - 提前 1 小时通知；不足 1 小时则退化为即时提醒
        long delayMs = appointment.getDate().getTime() - System.currentTimeMillis() - 3600_000L;
        mqMessagePublisher.publishAppointmentReminderDelayed(createdMessage, delayMs);
    }

    @Override
    @Transactional
    public void updateAppointment(AppointmentUpdateDTO appointmentDTO) {
        Appointment appointment = this.getById(appointmentDTO.getId());
        if (appointment == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "预约记录不存在！");
        }
        appointment.setDate(appointmentDTO.getDate());
        appointment.setContent(appointmentDTO.getContent());
        this.updateById(appointment);

        // 如果修改了时间或内容，重新发布一份延时提醒（旧时间的延时消息会在消费端被拦截丢弃）
        Vip vip = vipMapper.selectById(appointment.getVipId());
        if (vip != null) {
            AppointmentCreatedMessage message = new AppointmentCreatedMessage(
                    appointment.getId(),
                    vip.getId(),
                    vip.getName(),
                    vip.getPhone(),
                    appointment.getDate(),
                    appointment.getContent(),
                    new Date()
            );
            long delayMs = appointment.getDate().getTime() - System.currentTimeMillis() - 3600_000L;
            mqMessagePublisher.publishAppointmentReminderDelayed(message, delayMs);
        }
    }

    private Vip resolveVip(Integer vipId, String vid, String phone) {
        Vip vip = null;
        if (vipId != null) {
            vip = vipMapper.selectById(vipId);
        } else if (StringUtils.hasLength(vid)) {
            vip = vipMapper.selectOne(new LambdaQueryWrapper<Vip>().eq(Vip::getVid, vid));
        } else if (StringUtils.hasLength(phone)) {
            vip = vipMapper.selectOne(new LambdaQueryWrapper<Vip>().eq(Vip::getPhone, phone));
        }

        // 预约消息依赖明确的会员接收对象，因此这里不允许退回到纯文本预约的旧模式。
        if (vip == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "会员不存在，请先创建会员！");
        }
        return vip;
    }

    private List<AppointmentVO> buildAppointmentVOs(List<Appointment> appointments) {
        if (appointments == null || appointments.isEmpty()) {
            return Collections.emptyList();
        }

        Set<Integer> vipIds = appointments.stream().map(Appointment::getVipId).collect(Collectors.toSet());
        Map<Integer, Vip> vipMap = vipMapper.selectBatchIds(vipIds).stream()
                .collect(Collectors.toMap(Vip::getId, vip -> vip, (left, right) -> left, LinkedHashMap::new));

        List<AppointmentVO> result = new ArrayList<>(appointments.size());
        for (Appointment appointment : appointments) {
            Vip vip = vipMap.get(appointment.getVipId());
            AppointmentVO vo = new AppointmentVO();
            vo.setId(appointment.getId());
            vo.setVipId(appointment.getVipId());
            vo.setVid(vip == null ? null : vip.getVid());
            vo.setName(vip == null ? "已删除会员" : vip.getName());
            vo.setSex(vip == null ? null : vip.getSex());
            vo.setPhone(vip == null ? null : vip.getPhone());
            vo.setContent(appointment.getContent());
            vo.setDate(appointment.getDate());
            result.add(vo);
        }
        return result;
    }
}
