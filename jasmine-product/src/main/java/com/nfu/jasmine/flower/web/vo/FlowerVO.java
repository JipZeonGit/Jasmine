package com.nfu.jasmine.flower.web.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class FlowerVO {
    private Integer id;
    private String name;
    private String unit;
    private BigDecimal salePrice;
    private BigDecimal costPrice;
    private Integer safeStock;
    private Integer currentStock;
    private Integer status;
}
