package com.nfu.jasmine.cus.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nfu.jasmine.common.enums.ResultCode;
import com.nfu.jasmine.common.vo.Result;
import com.nfu.jasmine.common.vo.TableData;
import com.nfu.jasmine.cus.dto.FlowerQueryDTO;
import com.nfu.jasmine.cus.dto.FlowerSaveDTO;
import com.nfu.jasmine.cus.entity.Flower;
import com.nfu.jasmine.cus.service.IFlowerService;
import com.nfu.jasmine.cus.vo.FlowerVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "花卉接口列表")
@Validated
@RestController
@RequestMapping("/flower")
public class FlowerController {
    @Autowired
    private IFlowerService flowerService;

    @Operation(summary = "获取全部花卉")
    @GetMapping("/all")
    public Result<List<FlowerVO>> getAllFlower() {
        return Result.success(flowerService.list().stream().map(this::toFlowerVO).toList(), "查询成功");
    }

    @Operation(summary = "新增花卉")
    @PostMapping("")
    public Result<?> addFlower(@Valid @RequestBody FlowerSaveDTO flowerDTO) {
        Flower flower = new Flower();
        BeanUtils.copyProperties(flowerDTO, flower);
        flower.setCurrentStock(0);
        flower.setDeleted(0);
        flowerService.save(flower);
        return Result.success("新增花卉成功！");
    }

    @Operation(summary = "修改花卉")
    @PutMapping("")
    public Result<?> updateFlower(@Valid @RequestBody FlowerSaveDTO flowerDTO) {
        if (flowerDTO.getId() == null) {
            return Result.fail(ResultCode.VALIDATE_FAILED, "花卉ID不能为空！");
        }

        Flower flower = flowerService.getById(flowerDTO.getId());
        if (flower == null) {
            return Result.fail(ResultCode.NOT_FOUND, "花卉不存在！");
        }

        Integer currentStock = flower.getCurrentStock();
        BeanUtils.copyProperties(flowerDTO, flower);
        flower.setCurrentStock(currentStock);
        flowerService.updateById(flower);
        return Result.success("修改花卉成功！");
    }

    @Operation(summary = "根据ID查询花卉")
    @GetMapping("/{id}")
    public Result<FlowerVO> getFlowerById(@PathVariable("id") Integer id) {
        return Result.success(toFlowerVO(flowerService.getById(id)));
    }

    @Operation(summary = "根据ID逻辑删除花卉")
    @DeleteMapping("/{id}")
    public Result<?> deleteFlowerById(@PathVariable("id") Integer id) {
        Flower flower = flowerService.getById(id);
        if (flower == null) {
            return Result.fail(ResultCode.NOT_FOUND, "花卉不存在！");
        }
        if (flower.getCurrentStock() != null && flower.getCurrentStock() > 0) {
            return Result.fail(ResultCode.CONFLICT, "当前库存不为 0 的花卉不能直接删除！");
        }
        flowerService.removeById(id);
        return Result.success("删除花卉成功！");
    }

    @Operation(summary = "分页查询花卉")
    @GetMapping("/list")
    public Result<TableData<FlowerVO>> getFlowerList(@Valid FlowerQueryDTO queryDTO) {
        LambdaQueryWrapper<Flower> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StringUtils.hasLength(queryDTO.getName()), Flower::getName, queryDTO.getName());
        wrapper.orderByAsc(Flower::getId);

        Page<Flower> page = new Page<>(queryDTO.getPageNo(), queryDTO.getPageSize());
        flowerService.page(page, wrapper);

        TableData<FlowerVO> data = new TableData<>();
        data.setTotal(page.getTotal());
        data.setRows(page.getRecords().stream().map(this::toFlowerVO).toList());
        return Result.success(data);
    }

    private FlowerVO toFlowerVO(Flower flower) {
        if (flower == null) {
            return null;
        }
        FlowerVO vo = new FlowerVO();
        BeanUtils.copyProperties(flower, vo);
        return vo;
    }
}
