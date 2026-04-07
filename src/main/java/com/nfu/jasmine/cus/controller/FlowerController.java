package com.nfu.jasmine.cus.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nfu.jasmine.common.vo.Result;
import com.nfu.jasmine.cus.entity.Flower;
import com.nfu.jasmine.cus.service.IFlowerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * 前端控制器
 * </p>
 *
 * @author jipzeongit
 * @since 2023-07-06
 */
@Tag(name = "花卉接口列表")
@RestController
@RequestMapping("/flower")
public class FlowerController {
    @Autowired
    private IFlowerService flowerService;

    @Operation(summary = "获取全部花卉")
    @GetMapping("/all")
    public Result<List<Flower>> getAllFlower() {
        List<Flower> list = flowerService.list();
        return Result.success(list, "查询成功");
    }

    @Operation(summary = "新增花卉")
    @PostMapping("")
    public Result<?> addFlower(@RequestBody Flower flower) {
        flowerService.save(flower);
        return Result.success("新增花卉成功！");
    }

    @Operation(summary = "修改花卉")
    @PutMapping("")
    public Result<?> updateFlower(@RequestBody Flower flower) {
        flowerService.updateById(flower);
        return Result.success("修改花卉成功！");
    }

    @Operation(summary = "根据ID查询单种花卉")
    @GetMapping("/{id}")
    public Result<Flower> getFlowerById(@PathVariable("id") Integer id) {
        Flower flower = flowerService.getById(id);
        return Result.success(flower);
    }

    @Operation(summary = "根据ID逻辑删除花卉数据")
    @DeleteMapping("/{id}")
    public Result<Flower> deleteFlowerById(@PathVariable("id") Integer id) {
        flowerService.removeById(id);
        return Result.success("删除花卉数据成功！");
    }

    @Operation(summary = "查询花卉")
    @GetMapping("/list")
    public Result<Map<String, Object>> getFlowerList(@RequestParam(value = "name", required = false) String name,
                                                     @RequestParam("pageNo") Long pageNo,
                                                     @RequestParam("pageSize") Long pageSize) {

        LambdaQueryWrapper<Flower> wrapper = new LambdaQueryWrapper<>();

        // 使用LambdaQueryWrapper的like方法实现模糊查询
        wrapper.like(StringUtils.hasLength(name), Flower::getName, name);

        // 按照ID进行排序
        wrapper.orderByAsc(Flower::getId);

        Page<Flower> page = new Page<>(pageNo, pageSize);
        flowerService.page(page, wrapper);

        Map<String, Object> data = new HashMap<>();
        data.put("total", page.getTotal());
        data.put("rows", page.getRecords());

        return Result.success(data);
    }
}
