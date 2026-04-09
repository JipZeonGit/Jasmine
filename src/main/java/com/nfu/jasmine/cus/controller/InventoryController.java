package com.nfu.jasmine.cus.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nfu.jasmine.common.enums.ResultCode;
import com.nfu.jasmine.common.utils.SerialNumberUtil;
import com.nfu.jasmine.common.vo.Result;
import com.nfu.jasmine.common.vo.TableData;
import com.nfu.jasmine.cus.dto.InventoryQueryDTO;
import com.nfu.jasmine.cus.dto.InventorySaveDTO;
import com.nfu.jasmine.cus.entity.Inventory;
import com.nfu.jasmine.cus.service.IInventoryService;
import com.nfu.jasmine.cus.vo.InventoryVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * <p>
 * 前端控制器
 * </p>
 *
 * @author jipzeongit
 * @since 2023-07-06
 */
@Tag(name = "库存接口列表")
@Validated
@RestController
@RequestMapping("/inventory")
public class InventoryController {
    @Autowired
    private IInventoryService inventoryService;

    @Operation(summary = "获取全部库存")
    @GetMapping("/all")
    public Result<List<InventoryVO>> getAllInventory() {
        List<InventoryVO> list = inventoryService.list().stream().map(this::toInventoryVO).collect(Collectors.toList());
        return Result.success(list, "查询成功");
    }

    @Operation(summary = "新增库存")
    @PostMapping("")
    public Result<?> addInventory(@Valid @RequestBody InventorySaveDTO inventoryDTO) {
        Inventory inventory = new Inventory();
        BeanUtils.copyProperties(inventoryDTO, inventory);
        inventory.setNum(SerialNumberUtil.generateSerialNumber());
        inventoryService.save(inventory);
        return Result.success("新增库存成功！");
    }

    @Operation(summary = "修改库存")
    @PutMapping("")
    public Result<?> updateFlower(@Valid @RequestBody InventorySaveDTO inventoryDTO) {
        if (inventoryDTO.getId() == null) {
            return Result.fail(ResultCode.VALIDATE_FAILED, "库存ID不能为空！");
        }
        Inventory inventory = new Inventory();
        BeanUtils.copyProperties(inventoryDTO, inventory);
        inventoryService.updateById(inventory);
        return Result.success("修改库存成功！");
    }

    @Operation(summary = "根据ID查询库存")
    @GetMapping("/{id}")
    public Result<InventoryVO> getInventoryById(@PathVariable("id") Integer id) {
        Inventory inventory = inventoryService.getById(id);
        return Result.success(toInventoryVO(inventory));
    }

    @Operation(summary = "根据ID逻辑删除库存数据")
    @DeleteMapping("/{id}")
    public Result<?> deleteInventoryById(@PathVariable("id") Integer id) {
        inventoryService.removeById(id);
        return Result.success("删除库存数据成功！");
    }

    @Operation(summary = "查询仓库")
    @GetMapping("/list")
    public Result<TableData<InventoryVO>> getInventoryList(@Valid InventoryQueryDTO queryDTO) {
        LambdaQueryWrapper<Inventory> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StringUtils.hasLength(queryDTO.getName()), Inventory::getName, queryDTO.getName());
        wrapper.like(StringUtils.hasLength(queryDTO.getNum()), Inventory::getNum, queryDTO.getNum());
        wrapper.orderByAsc(Inventory::getId);

        Page<Inventory> page = new Page<>(queryDTO.getPageNo(), queryDTO.getPageSize());
        inventoryService.page(page, wrapper);

        TableData<InventoryVO> data = new TableData<>();
        data.setTotal(page.getTotal());
        data.setRows(page.getRecords().stream().map(this::toInventoryVO).collect(Collectors.toList()));
        return Result.success(data);
    }

    private InventoryVO toInventoryVO(Inventory inventory) {
        if (inventory == null) {
            return null;
        }
        InventoryVO vo = new InventoryVO();
        BeanUtils.copyProperties(inventory, vo);
        return vo;
    }
}