package com.nfu.jasmine.cus.controller;

import com.nfu.jasmine.common.vo.Result;
import com.nfu.jasmine.cus.entity.Appointment;
import com.nfu.jasmine.cus.service.IAppointmentService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.stereotype.Controller;

import java.util.List;

/**
 * <p>
 *  前端控制器
 * </p>
 *
 * @author jipzeongit
 * @since 2023-07-06
 */
@Api(tags = {"预约接口列表"})
@RestController
@RequestMapping("/appointment")
public class AppointmentController {
    @Autowired
    private IAppointmentService appointmentService;

    @ApiOperation("获取全部预约")
    @GetMapping("/all")
    public Result<List<Appointment>> getAllAppointment(){
        List<Appointment> list = appointmentService.list();
        return Result.success(list,"查询成功");
    }

    @ApiOperation("新增预约")
    @PostMapping("")
    public Result<?> addAppointment(@RequestBody Appointment appointment){
        appointmentService.save(appointment);
        return Result.success("新增预约成功！");
    }

    @ApiOperation("修改预约")
    @PutMapping("")
    public Result<?> updateAppointment(@RequestBody Appointment appointment){
        appointmentService.updateById(appointment);
        return Result.success("修改预约成功！");
    }

    @ApiOperation("根据ID查询预约")
    @GetMapping("/{id}")
    public Result<Appointment> getAppointmentById(@PathVariable("id") Integer id){
        Appointment appointment = appointmentService.getById(id);
        return Result.success(appointment);
    }

    @ApiOperation("根据ID逻辑删除预约数据")
    @DeleteMapping("/{id}")
    public Result<Appointment> deleteAppointmentById(@PathVariable("id") Integer id){
        appointmentService.removeById(id);
        return Result.success("删除预约数据成功！");
    }
}
