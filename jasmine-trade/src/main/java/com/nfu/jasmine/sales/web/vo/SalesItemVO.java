package com.nfu.jasmine.sales.web.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class SalesItemVO {
    private Integer id;
    private Integer flowerId;
    private String flowerName;
    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal unitCost;
    private BigDecimal amount;
    private BigDecimal costAmount;
}
