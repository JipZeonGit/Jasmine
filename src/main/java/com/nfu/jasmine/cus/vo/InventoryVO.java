package com.nfu.jasmine.cus.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
public class InventoryVO {
    private Integer id;
    private String bizNo;
    private Integer flowerId;
    private String flowerName;
    private String bizType;
    private String bizTypeLabel;
    private Integer quantity;
    private Integer beforeStock;
    private Integer afterStock;
    private BigDecimal unitCost;
    private BigDecimal totalCost;
    private String remark;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date date;
}
