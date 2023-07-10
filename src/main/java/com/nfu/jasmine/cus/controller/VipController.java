package com.nfu.jasmine.cus.controller;

import com.nfu.jasmine.common.vo.Result;
import com.nfu.jasmine.cus.entity.Appointment;
import com.nfu.jasmine.cus.entity.Vip;
import com.nfu.jasmine.cus.service.IVipService;
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
@Api(tags = {"会员接口列表"})
@RestController
@RequestMapping("/vip")
public class VipController {
    @Autowired
    private IVipService vipService;

    @ApiOperation("获取全部会员")
    @GetMapping("/all")
    public Result<List<Vip>> getAllVip(){
        List<Vip> list = vipService.list();
        return Result.success(list,"查询成功");
    }

    @ApiOperation("新增会员")
    @PostMapping("")
    public Result<?> addVip(@RequestBody Vip vip){
        vipService.save(vip);
        return Result.success("新增预约成功！");
    }

    @ApiOperation("修改会员")
    @PutMapping("")
    public Result<?> updateVip(@RequestBody Vip vip){
        vipService.updateById(vip);
        return Result.success("修改会员成功！");
    }

    @ApiOperation("根据ID查询会员")
    @GetMapping("/{id}")
    public Result<Vip> getVipById(@PathVariable("id") Integer id){
        Vip vip = vipService.getById(id);
        return Result.success(vip);
    }

    @ApiOperation("根据ID逻辑删除会员数据")
    @DeleteMapping("/{id}")
    public Result<Appointment> deleteVipById(@PathVariable("id") Integer id){
        vipService.removeById(id);
        return Result.success("删除会员数据成功！");
    }
}
