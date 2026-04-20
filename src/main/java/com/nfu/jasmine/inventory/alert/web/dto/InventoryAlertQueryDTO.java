package com.nfu.jasmine.inventory.alert.web.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 库存预警查询 DTO。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryAlertQueryDTO {
    private Integer pageNo = 1;
    private Integer pageSize = 10;
    private String flowerName;
    private String alertStatus;
}