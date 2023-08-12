package com.nfu.jasmine.cus.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nfu.jasmine.common.utils.SerialNumberUtil;
import com.nfu.jasmine.common.vo.Result;
import com.nfu.jasmine.cus.entity.Flower;
import com.nfu.jasmine.cus.entity.Inventory;
import com.nfu.jasmine.cus.service.IInventoryService;
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
@Api(tags = {"库存接口列表"})
@RestController
@RequestMapping("/inventory")
public class InventoryController {
    @Autowired
    private IInventoryService inventoryService;

    @ApiOperation("获取全部库存")
    @GetMapping("/all")
    public Result<List<Inventory>> getAllInventory(){
        List<Inventory> list = inventoryService.list();
        return Result.success(list,"查询成功");
    }

    @ApiOperation("新增库存")
    @PostMapping("")
    public Result<?> addInventory(@RequestBody Inventory inventory){
        inventory.setNum(SerialNumberUtil.generateSerialNumber());
        inventoryService.save(inventory);
        return Result.success("新增库存成功！");
    }

    @ApiOperation("修改库存")
    @PutMapping("")
    public Result<?> updateFlower(@RequestBody Inventory inventory){
        inventoryService.updateById(inventory);
        return Result.success("修改库存成功！");
    }

    @ApiOperation("根据ID查询库存")
    @GetMapping("/{id}")
    public Result<Inventory> getInventoryById(@PathVariable("id") Integer id){
        Inventory inventory = inventoryService.getById(id);
        return Result.success(inventory);
    }

    @ApiOperation("根据ID逻辑删除库存数据")
    @DeleteMapping("/{id}")
    public Result<Inventory> deleteInventoryById(@PathVariable("id") Integer id){
        inventoryService.removeById(id);
        return Result.success("删除库存数据成功！");
    }

    @ApiOperation("查询仓库")
    @GetMapping("/list")
    public Result<Map<String,Object>> getInventoryList(@RequestParam(value = "name",required = false) String name,@RequestParam(value = "num",required = false) String num, @RequestParam("pageNo") Long pageNo, @RequestParam("pageSize") Long pageSize){

        LambdaQueryWrapper<Inventory> wrapper = new LambdaQueryWrapper<>();

        // 使用LambdaQueryWrapper的like方法来实现模糊查询
        wrapper.like(StringUtils.hasLength(name), Inventory::getName, name);
        wrapper.like(StringUtils.hasLength(num),Inventory::getNum,num);

        // 按照ID进行排序
        wrapper.orderByAsc(Inventory::getId);

        Page<Inventory> page = new Page<>(pageNo,pageSize);
        inventoryService.page(page,wrapper);

        Map<String,Object> data = new HashMap<>();
        data.put("total",page.getTotal());
        data.put("rows",page.getRecords());

        return Result.success(data);
    }

}
