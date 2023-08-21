package com.nfu.jasmine.cus.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nfu.jasmine.common.vo.Result;
import com.nfu.jasmine.cus.entity.Appointment;
import com.nfu.jasmine.cus.entity.Vip;
import com.nfu.jasmine.cus.mapper.VipMapper;
import com.nfu.jasmine.cus.service.IAppointmentService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.stereotype.Controller;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    @Autowired
    private VipMapper vipMapper;

    @ApiOperation("获取全部预约")
    @GetMapping("/all")
    public Result<List<Appointment>> getAllAppointment(){
        List<Appointment> list = appointmentService.list();
        return Result.success(list,"查询成功");
    }

    @ApiOperation("新增预约")
    @PostMapping("")
    public Result<?> addAppointment(@RequestParam(value = "vid", required = false) String vid, @RequestParam(value = "phone", required = false) String phone, @RequestParam(value = "date", required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") Date date, @RequestParam(value = "content", required = false) String content) {
        boolean vipExists = false;
        if (vid != null) {
            Vip vip = vipMapper.selectOne(new QueryWrapper<Vip>().eq("vid", vid));
            if (vip != null) {
                vipExists = true;
            }
        } else if (phone != null) {
            Vip vip = vipMapper.selectOne(new QueryWrapper<Vip>().eq("phone", phone));
            if (vip != null) {
                vipExists = true;
            }
        }

        if (vipExists) {
            appointmentService.addAppointment(vid, phone, date, content);
            return Result.success("新增预约成功!");
        } else {
            return Result.fail("用户信息不存在，请创建新会员！");
        }
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

    @ApiOperation("查询预约")
    @GetMapping("/list")
    public Result<Map<String,Object>> getAppointmentList(@RequestParam(value = "name",required = false) String name , @RequestParam(value = "phone",required = false) String phone , @RequestParam(value = "date",required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") Date date, @RequestParam("pageNo") Long pageNo, @RequestParam("pageSize") Long pageSize){
        LambdaQueryWrapper<Appointment> wrapper = new LambdaQueryWrapper<>();

        // 使用LambdaQueryWrapper的like方法来实现模糊查询
        wrapper.like(StringUtils.hasLength(name), Appointment::getName, name);
        wrapper.like(StringUtils.hasLength(phone), Appointment::getPhone, phone);

        if (date != null && !date.equals("")) {
            // 将日期字段转换为字符串进行模糊查询
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            String dateString = sdf.format(date);
            wrapper.like(Appointment::getDate, dateString);
        } else {
            date = null;
        }

        // 按照ID进行排序
        wrapper.orderByAsc(Appointment::getId);

        Page<Appointment> page = new Page<>(pageNo,pageSize);
        appointmentService.page(page,wrapper);

        Map<String,Object> data = new HashMap<>();
        data.put("total",page.getTotal());
        data.put("rows",page.getRecords());

        return Result.success(data);
    }
}
