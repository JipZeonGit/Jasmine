package com.nfu.jasmine.inventory.alert.web.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * 库存预警查询 VO。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryAlertVO {
    private Integer id;
    private Integer flowerId;
    private String flowerNameSnapshot;
    private Integer safeStock;
    private Integer currentStock;
    private String alertStatus;
    private Date lastTriggerTime;
    private Date lastRecoverTime;
    private String remark;
}