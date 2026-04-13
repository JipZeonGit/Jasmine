package com.nfu.jasmine.infra.mq.message;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Date;

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
}
