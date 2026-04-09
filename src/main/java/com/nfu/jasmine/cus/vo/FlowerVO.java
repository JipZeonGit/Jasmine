package com.nfu.jasmine.cus.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class FlowerVO {
    private Integer id;
    private String name;
    private BigDecimal unitprice;
    private BigDecimal costs;
}