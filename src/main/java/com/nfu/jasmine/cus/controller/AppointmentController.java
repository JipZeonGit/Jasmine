package com.nfu.jasmine.cus.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nfu.jasmine.common.vo.Result;
import com.nfu.jasmine.common.vo.TableData;
import com.nfu.jasmine.cus.dto.AppointmentCreateDTO;
import com.nfu.jasmine.cus.dto.AppointmentQueryDTO;
import com.nfu.jasmine.cus.dto.AppointmentUpdateDTO;
import com.nfu.jasmine.cus.entity.Appointment;
import com.nfu.jasmine.cus.service.IAppointmentService;
import com.nfu.jasmine.cus.vo.AppointmentVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * <p>
 * 前端控制器
 * </p>
 *
 * @author jipzeongit
 * @since 2023-07-06
 */
@Tag(name = "预约接口列表")
@Validated
@RestController
@RequestMapping("/appointment")
public class AppointmentController {
    @Autowired
    private IAppointmentService appointmentService;

    @Operation(summary = "获取全部预约")
    @GetMapping("/all")
    public Result<List<AppointmentVO>> getAllAppointment() {
        List<AppointmentVO> list = appointmentService.list().stream().map(this::toAppointmentVO).toList();
        return Result.success(list, "查询成功");
    }

    @Operation(summary = "新增预约")
    @PostMapping("")
    public Result<?> addAppointment(@Valid @RequestBody AppointmentCreateDTO appointmentDTO) {
        if (!StringUtils.hasLength(appointmentDTO.getVid()) && !StringUtils.hasLength(appointmentDTO.getPhone())) {
            return Result.fail("会员卡号或手机号至少填写一项！");
        }

        boolean success = appointmentService.addAppointment(
                appointmentDTO.getVid(),
                appointmentDTO.getPhone(),
                appointmentDTO.getDate(),
                appointmentDTO.getContent()
        );
        if (success) {
            return Result.success("新增预约成功!");
        }
        return Result.fail("用户信息不存在，请创建新会员！");
    }

    @Operation(summary = "修改预约")
    @PutMapping("")
    public Result<?> updateAppointment(@Valid @RequestBody AppointmentUpdateDTO appointmentDTO) {
        Appointment appointment = new Appointment();
        BeanUtils.copyProperties(appointmentDTO, appointment);
        appointmentService.updateById(appointment);
        return Result.success("修改预约成功！");
    }

    @Operation(summary = "根据ID查询预约")
    @GetMapping("/{id}")
    public Result<AppointmentVO> getAppointmentById(@PathVariable("id") Integer id) {
        Appointment appointment = appointmentService.getById(id);
        return Result.success(appointment == null ? null : toAppointmentVO(appointment));
    }

    @Operation(summary = "根据ID逻辑删除预约数据")
    @DeleteMapping("/{id}")
    public Result<?> deleteAppointmentById(@PathVariable("id") Integer id) {
        appointmentService.removeById(id);
        return Result.success("删除预约数据成功！");
    }

    @Operation(summary = "查询预约")
    @GetMapping("/list")
    public Result<TableData<AppointmentVO>> getAppointmentList(@Valid AppointmentQueryDTO queryDTO) {
        LambdaQueryWrapper<Appointment> wrapper = new LambdaQueryWrapper<>();

        wrapper.like(StringUtils.hasLength(queryDTO.getName()), Appointment::getName, queryDTO.getName());
        wrapper.like(StringUtils.hasLength(queryDTO.getPhone()), Appointment::getPhone, queryDTO.getPhone());
        if (queryDTO.getDate() != null) {
            wrapper.like(Appointment::getDate, queryDTO.getDate());
        }
        wrapper.orderByAsc(Appointment::getId);

        Page<Appointment> page = new Page<>(queryDTO.getPageNo(), queryDTO.getPageSize());
        appointmentService.page(page, wrapper);

        TableData<AppointmentVO> data = new TableData<>();
        data.setTotal(page.getTotal());
        data.setRows(page.getRecords().stream().map(this::toAppointmentVO).toList());

        return Result.success(data);
    }

    private AppointmentVO toAppointmentVO(Appointment appointment) {
        AppointmentVO appointmentVO = new AppointmentVO();
        BeanUtils.copyProperties(appointment, appointmentVO);
        return appointmentVO;
    }
}
