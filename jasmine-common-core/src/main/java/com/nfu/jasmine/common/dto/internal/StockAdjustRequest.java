package com.nfu.jasmine.common.dto.internal;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 库存调整请求 DTO，用于服务间内部通信。
 */
@Data
public class StockAdjustRequest {
    private Integer flowerId;
    private Integer quantity;
    private BigDecimal costPrice;
    private Boolean updateCostPrice;
    private Integer operatorId;
    private String reason;
}
