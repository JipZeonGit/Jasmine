package com.nfu.jasmine.inventory.alert.web.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nfu.jasmine.common.vo.Result;
import com.nfu.jasmine.common.vo.TableData;
import com.nfu.jasmine.inventory.alert.model.entity.InventoryAlert;
import com.nfu.jasmine.inventory.alert.model.enums.AlertStatus;
import com.nfu.jasmine.inventory.alert.persistence.mapper.InventoryAlertMapper;
import com.nfu.jasmine.inventory.alert.service.InventoryAlertService;
import com.nfu.jasmine.inventory.alert.web.dto.InventoryAlertQueryDTO;
import com.nfu.jasmine.inventory.alert.web.vo.InventoryAlertVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "库存预警接口列表")
@RestController
@RequestMapping("/inventory-alert")
@RequiredArgsConstructor
public class InventoryAlertController {

    private final InventoryAlertMapper inventoryAlertMapper;
    private final InventoryAlertService inventoryAlertService;

    @Operation(summary = "获取低库存预警数量")
    @GetMapping("/low-stock-count")
    public Result<Long> getLowStockCount() {
        return Result.success(inventoryAlertService.getLowStockCount());
    }

    @Operation(summary = "分页查询库存预警")
    @GetMapping("/list")
    public Result<TableData<InventoryAlertVO>> pageInventoryAlerts(InventoryAlertQueryDTO queryDTO) {
        Page<InventoryAlert> page = new Page<>(queryDTO.getPageNo(), queryDTO.getPageSize());
        
        LambdaQueryWrapper<InventoryAlert> wrapper = new LambdaQueryWrapper<>();
        if (queryDTO.getFlowerName() != null) {
            wrapper.like(InventoryAlert::getFlowerNameSnapshot, queryDTO.getFlowerName());
        }
        if (queryDTO.getAlertStatus() != null) {
            wrapper.eq(InventoryAlert::getAlertStatus, queryDTO.getAlertStatus());
        }
        wrapper.orderByDesc(InventoryAlert::getLastTriggerTime)
               .orderByDesc(InventoryAlert::getId);

        inventoryAlertMapper.selectPage(page, wrapper);
        
        TableData<InventoryAlertVO> data = new TableData<>();
        data.setTotal(page.getTotal());
        data.setRows(convertToVOList(page.getRecords()));
        
        return Result.success(data);
    }

    private List<InventoryAlertVO> convertToVOList(List<InventoryAlert> records) {
        return records.stream().map(record -> InventoryAlertVO.builder()
                .id(record.getId())
                .flowerId(record.getFlowerId())
                .flowerNameSnapshot(record.getFlowerNameSnapshot())
                .safeStock(record.getSafeStock())
                .currentStock(record.getCurrentStock())
                .alertStatus(record.getAlertStatus())
                .lastTriggerTime(record.getLastTriggerTime())
                .lastRecoverTime(record.getLastRecoverTime())
                .remark(record.getRemark())
                .build()
        ).toList();
    }
}