package com.nfu.jasmine.cus.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nfu.jasmine.common.vo.Result;
import com.nfu.jasmine.cus.entity.Flower;
import com.nfu.jasmine.cus.entity.Inventory;
import com.nfu.jasmine.cus.entity.Sales;
import com.nfu.jasmine.cus.service.IFlowerService;
import com.nfu.jasmine.cus.service.ISalesService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
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
@Api(tags = {"销售接口列表"})
@RestController
@RequestMapping("/sales")
public class SalesController {
    @Autowired
    private ISalesService salesService;

    @ApiOperation("获取全部销售订单")
    @GetMapping("/all")
    public Result<List<Sales>> getAllSales(){
        List<Sales> list = salesService.list();
        return Result.success(list,"查询成功");
    }

    @ApiOperation("新增销售订单")
    @PostMapping("")
    public Result<?> addSales(@RequestBody Sales sales){
        salesService.save(sales);
        return Result.success("新增销售订单成功！");
    }

    @ApiOperation("修改销售订单")
    @PutMapping("")
    public Result<?> updateSales(@RequestBody Sales sales){
        salesService.updateById(sales);
        return Result.success("修改销售订单成功！");
    }

    @ApiOperation("根据ID查询单份销售订单")
    @GetMapping("/{id}")
    public Result<Sales> getSalesById(@PathVariable("id") Integer id){
        Sales sales = salesService.getById(id);
        return Result.success(sales);
    }

    @ApiOperation("根据ID逻辑删除销售订单数据")
    @DeleteMapping("/{id}")
    public Result<Sales> deleteSalesById(@PathVariable("id") Integer id){
        salesService.removeById(id);
        return Result.success("删除销售订单数据成功！");
    }

    @ApiOperation("查询销售订单")
    @GetMapping("/list")
    public Result<Map<String,Object>> getSalesList(@RequestParam(value = "date",required = false) Date date, @RequestParam("pageNo") Long pageNo, @RequestParam("pageSize") Long pageSize){

        LambdaQueryWrapper<Sales> wrapper = new LambdaQueryWrapper<>();

        // 使用LambdaQueryWrapper的like方法来实现模糊查询
        if (date != null) {
            // 将日期字段转换为字符串进行模糊查询
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            String dateString = sdf.format(date);
            wrapper.like(Sales::getDate, dateString);
        }

        // 按照ID进行排序
        wrapper.orderByAsc(Sales::getId);

        Page<Sales> page = new Page<>(pageNo,pageSize);
        salesService.page(page,wrapper);

        Map<String,Object> data = new HashMap<>();
        data.put("total",page.getTotal());
        data.put("rows",page.getRecords());

        return Result.success(data);
    }
}
