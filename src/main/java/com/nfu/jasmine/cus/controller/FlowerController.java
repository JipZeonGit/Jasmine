package com.nfu.jasmine.cus.controller;

import com.nfu.jasmine.common.vo.Result;
import com.nfu.jasmine.cus.entity.Flower;
import com.nfu.jasmine.cus.service.IFlowerService;
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
@Api(tags = {"花卉接口列表"})
@RestController
@RequestMapping("/flower")
public class FlowerController {
    @Autowired
    private IFlowerService flowerService;

    @ApiOperation("获取全部花卉")
    @GetMapping("/all")
    public Result<List<Flower>> getAllFlower(){
        List<Flower> list = flowerService.list();
        return Result.success(list,"查询成功");
    }

    @ApiOperation("新增花卉")
    @PostMapping("")
    public Result<?> addFlower(@RequestBody Flower flower){
        flowerService.save(flower);
        return Result.success("新增花卉成功！");
    }

    @ApiOperation("修改花卉")
    @PutMapping("")
    public Result<?> updateFlower(@RequestBody Flower flower){
        flowerService.updateById(flower);
        return Result.success("修改花卉成功！");
    }

    @ApiOperation("根据ID查询单种花卉")
    @GetMapping("/{id}")
    public Result<Flower> getFlowerById(@PathVariable("id") Integer id){
        Flower flower = flowerService.getById(id);
        return Result.success(flower);
    }

    @ApiOperation("根据ID逻辑删除花卉数据")
    @DeleteMapping("/{id}")
    public Result<Flower> deleteFlowerById(@PathVariable("id") Integer id){
        flowerService.removeById(id);
        return Result.success("删除花卉数据成功！");
    }
}
