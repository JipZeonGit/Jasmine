package com.nfu.jasmine.infra.mq.message;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Date;

/**
 * 库存变动消息体，采购入库、销售出库、损耗、盘点等动作均复用此消息结构。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class InventoryChangedMessage implements Serializable {
    private Integer inventoryId;
    private String bizNo;
    private Integer flowerId;
    private String bizType;
    private Integer quantity;
    private Integer beforeStock;
    private Integer afterStock;
    private Integer operatorId;
    private Date bizTime;
    private Date occurredAt;
    // 来源：MANUAL_INVENTORY / SALES_ORDER，标识是谁触发的库存变更
    private String changeSource;
    // 动作：CREATE / UPDATE / DELETE / ROLLBACK，标识当前变更的操作类型
    private String changeAction;
}
