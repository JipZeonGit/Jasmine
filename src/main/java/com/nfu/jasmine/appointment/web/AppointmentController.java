package com.nfu.jasmine.appointment.web;

import com.nfu.jasmine.common.vo.Result;
import com.nfu.jasmine.common.vo.TableData;
import com.nfu.jasmine.appointment.web.dto.AppointmentCreateDTO;
import com.nfu.jasmine.appointment.web.dto.AppointmentQueryDTO;
import com.nfu.jasmine.appointment.web.dto.AppointmentUpdateDTO;
import com.nfu.jasmine.appointment.application.IAppointmentService;
import com.nfu.jasmine.appointment.web.vo.AppointmentVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "预约接口列表")
@Validated
@RestController
@RequestMapping("/appointment")
public class AppointmentController {
    @Autowired
    private IAppointmentService appointmentService;

// 查出目前所有的预约单子
    @Operation(summary = "获取全部预约")
    @GetMapping("/all")
    public Result<List<AppointmentVO>> getAllAppointment() {
        return Result.success(appointmentService.listAppointments(), "查询成功");
    }

// 发起一个新的预约，填卡号或者手机号来绑定会员
    @Operation(summary = "新增预约")
    @PostMapping("")
    public Result<?> addAppointment(@Valid @RequestBody AppointmentCreateDTO appointmentDTO) {
        appointmentService.createAppointment(appointmentDTO);
        return Result.success("新增预约成功！");
    }

// 修改更新现有的预约信息内容
    @Operation(summary = "修改预约")
    @PutMapping("")
    public Result<?> updateAppointment(@Valid @RequestBody AppointmentUpdateDTO appointmentDTO) {
        appointmentService.updateAppointment(appointmentDTO);
        return Result.success("修改预约成功！");
    }

// 拿到指定ID的某个预约详单
    @Operation(summary = "根据ID查询预约")
    @GetMapping("/{id}")
    public Result<AppointmentVO> getAppointmentById(@PathVariable("id") Integer id) {
        return Result.success(appointmentService.getAppointmentDetail(id));
    }

// 把某个特定记录作逻辑删除
    @Operation(summary = "根据ID逻辑删除预约")
    @DeleteMapping("/{id}")
    public Result<?> deleteAppointmentById(@PathVariable("id") Integer id) {
        appointmentService.removeById(id);
        return Result.success("删除预约成功！");
    }

// 列表页的分页和多条件搜索接口，能根据名字、手机、日期筛选
    @Operation(summary = "分页查询预约")
    @GetMapping("/list")
    public Result<TableData<AppointmentVO>> getAppointmentList(@Valid AppointmentQueryDTO queryDTO) {
        return Result.success(appointmentService.pageAppointments(queryDTO));
    }
}
