package com.nfu.jasmine.cus.controller;

import com.nfu.jasmine.common.utils.SerialNumberUtil;
import com.nfu.jasmine.common.vo.Result;
import com.nfu.jasmine.cus.entity.Inventory;
import com.nfu.jasmine.cus.service.IInventoryService;
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
@Api(tags = {"库存接口列表"})
@RestController
@RequestMapping("/inventory")
public class InventoryController {
    @Autowired
    private IInventoryService inventoryService;

    @Autowired
    private SerialNumberUtil serialNumberUtil;

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

}
