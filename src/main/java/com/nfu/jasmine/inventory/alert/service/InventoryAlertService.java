package com.nfu.jasmine.inventory.alert.service;

import com.nfu.jasmine.inventory.alert.model.entity.InventoryAlert;
import com.nfu.jasmine.inventory.alert.model.enums.AlertStatus;
import com.nfu.jasmine.inventory.alert.persistence.mapper.InventoryAlertMapper;
import org.springframework.stereotype.Service;

import java.util.Date;

/**
 * 库存预警读模型服务。
 * <p>
 * 提供库存预警状态更新和查询能力。
 */
@Service
public class InventoryAlertService {
    private final InventoryAlertMapper inventoryAlertMapper;

    public InventoryAlertService(InventoryAlertMapper inventoryAlertMapper) {
        this.inventoryAlertMapper = inventoryAlertMapper;
    }

    public void upsertAlert(Integer flowerId, String flowerName, Integer safeStock, Integer currentStock) {
        AlertStatus newStatus = currentStock < safeStock ? AlertStatus.LOW_STOCK : AlertStatus.NORMAL;

        InventoryAlert existing = inventoryAlertMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<InventoryAlert>()
                        .eq(InventoryAlert::getFlowerId, flowerId)
        );
        if (existing == null) {
            InventoryAlert alert = InventoryAlert.builder()
                    .flowerId(flowerId)
                    .flowerNameSnapshot(flowerName)
                    .safeStock(safeStock)
                    .currentStock(currentStock)
                    .alertStatus(newStatus.name())
                    .lastTriggerTime(newStatus == AlertStatus.LOW_STOCK ? new Date() : null)
                    .lastRecoverTime(newStatus == AlertStatus.NORMAL ? new Date() : null)
                    .build();
            inventoryAlertMapper.insert(alert);
        } else {
            existing.setFlowerNameSnapshot(flowerName);
            existing.setSafeStock(safeStock);
            existing.setCurrentStock(currentStock);
            boolean statusChanged = !existing.getAlertStatus().equals(newStatus.name());
            existing.setAlertStatus(newStatus.name());
            if (statusChanged) {
                if (newStatus == AlertStatus.LOW_STOCK) {
                    existing.setLastTriggerTime(new Date());
                } else {
                    existing.setLastRecoverTime(new Date());
                }
            }
            inventoryAlertMapper.updateById(existing);
        }
    }

    public Long getLowStockCount() {
        return inventoryAlertMapper.selectCount(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<InventoryAlert>()
                        .eq(InventoryAlert::getAlertStatus, AlertStatus.LOW_STOCK.name())
        );
    }
}
