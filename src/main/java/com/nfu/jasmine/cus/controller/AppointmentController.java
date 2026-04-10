package com.nfu.jasmine.cus.controller;

import com.nfu.jasmine.common.vo.Result;
import com.nfu.jasmine.common.vo.TableData;
import com.nfu.jasmine.cus.dto.AppointmentCreateDTO;
import com.nfu.jasmine.cus.dto.AppointmentQueryDTO;
import com.nfu.jasmine.cus.dto.AppointmentUpdateDTO;
import com.nfu.jasmine.cus.service.IAppointmentService;
import com.nfu.jasmine.cus.vo.AppointmentVO;
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

    @Operation(summary = "获取全部预约")
    @GetMapping("/all")
    public Result<List<AppointmentVO>> getAllAppointment() {
        return Result.success(appointmentService.listAppointments(), "查询成功");
    }

    @Operation(summary = "新增预约")
    @PostMapping("")
    public Result<?> addAppointment(@Valid @RequestBody AppointmentCreateDTO appointmentDTO) {
        appointmentService.createAppointment(appointmentDTO);
        return Result.success("新增预约成功！");
    }

    @Operation(summary = "修改预约")
    @PutMapping("")
    public Result<?> updateAppointment(@Valid @RequestBody AppointmentUpdateDTO appointmentDTO) {
        appointmentService.updateAppointment(appointmentDTO);
        return Result.success("修改预约成功！");
    }

    @Operation(summary = "根据ID查询预约")
    @GetMapping("/{id}")
    public Result<AppointmentVO> getAppointmentById(@PathVariable("id") Integer id) {
        return Result.success(appointmentService.getAppointmentDetail(id));
    }

    @Operation(summary = "根据ID逻辑删除预约")
    @DeleteMapping("/{id}")
    public Result<?> deleteAppointmentById(@PathVariable("id") Integer id) {
        appointmentService.removeById(id);
        return Result.success("删除预约成功！");
    }

    @Operation(summary = "分页查询预约")
    @GetMapping("/list")
    public Result<TableData<AppointmentVO>> getAppointmentList(@Valid AppointmentQueryDTO queryDTO) {
        return Result.success(appointmentService.pageAppointments(queryDTO));
    }
}
