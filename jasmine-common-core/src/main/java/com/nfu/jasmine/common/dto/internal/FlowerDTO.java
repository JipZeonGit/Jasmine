package com.nfu.jasmine.common.dto.internal;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 花卉基本信息 DTO，用于服务间内部通信。
 */
@Data
public class FlowerDTO {
    private Integer id;
    private String name;
    private BigDecimal price;
    private BigDecimal cost;
    private Integer status;
    private Integer safeStock;
    private Integer currentStock;
}
