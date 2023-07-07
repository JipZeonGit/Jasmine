package com.nfu.jasmine.cus.controller;

import com.nfu.jasmine.common.vo.Result;
import com.nfu.jasmine.cus.entity.Flower;
import com.nfu.jasmine.cus.entity.Sales;
import com.nfu.jasmine.cus.service.IFlowerService;
import com.nfu.jasmine.cus.service.ISalesService;
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
}
