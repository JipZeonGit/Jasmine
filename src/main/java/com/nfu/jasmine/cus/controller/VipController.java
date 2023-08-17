package com.nfu.jasmine.cus.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nfu.jasmine.common.utils.MembershipIdUtil;
import com.nfu.jasmine.common.utils.SerialNumberUtil;
import com.nfu.jasmine.common.vo.Result;
import com.nfu.jasmine.cus.entity.Appointment;
import com.nfu.jasmine.cus.entity.Vip;
import com.nfu.jasmine.cus.mapper.VipMapper;
import com.nfu.jasmine.cus.service.IVipService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.stereotype.Controller;

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
@Api(tags = {"会员接口列表"})
@RestController
@RequestMapping("/vip")
public class VipController {
    @Autowired
    private IVipService vipService;
    @Autowired
    private VipMapper vipMapper;

    @ApiOperation("获取全部会员")
    @GetMapping("/all")
    public Result<List<Vip>> getAllVip(){
        List<Vip> list = vipService.list();
        return Result.success(list,"查询成功");
    }

    @ApiOperation("新增会员")
    @PostMapping("")
    public Result<?> addVip(@RequestBody Vip vip){
        vip.setVid(MembershipIdUtil.generateMembershipCardNumber());
        // 判断手机号是否存在
        boolean vipExists = false;
        String phone = vip.getPhone();
        if (phone != null) {
            Vip vipByPhone = vipMapper.selectOne(new QueryWrapper<Vip>().eq("phone",phone));
            if (vipByPhone != null) {
                vipExists = true;
            }
        }
        if (vipExists) {
            return Result.fail("该手机号已注册,请重新输入没有注册的手机号!");
        } else {
            vipService.save(vip);
            return Result.success("新增会员成功!");
        }
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

    @ApiOperation("查询会员")
    @GetMapping("/list")
    public Result<Map<String,Object>> getVipList(@RequestParam(value = "name",required = false) String name,@RequestParam(value = "vid",required = false) String vid,@RequestParam(value = "phone",required = false) String phone, @RequestParam("pageNo") Long pageNo, @RequestParam("pageSize") Long pageSize){

        LambdaQueryWrapper<Vip> wrapper = new LambdaQueryWrapper<>();

        // 使用LambdaQueryWrapper的like方法来实现模糊查询
        wrapper.like(StringUtils.hasLength(name), Vip::getName, name);
        wrapper.like(StringUtils.hasLength(vid), Vip::getVid, vid);
        wrapper.like(StringUtils.hasLength(phone), Vip::getPhone, phone);

        // 按照ID进行排序
        wrapper.orderByAsc(Vip::getId);

        Page<Vip> page = new Page<>(pageNo,pageSize);
        vipService.page(page,wrapper);

        Map<String,Object> data = new HashMap<>();
        data.put("total",page.getTotal());
        data.put("rows",page.getRecords());

        return Result.success(data);
    }
}
