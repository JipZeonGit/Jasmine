package com.nfu.jasmine.common.dto.internal;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 库存调整结果 DTO，用于服务间内部通信。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StockAdjustResult {
    private Boolean success;
    private Integer currentStock;
    private String message;

    public static StockAdjustResult ok(Integer currentStock) {
        return new StockAdjustResult(true, currentStock, null);
    }

    public static StockAdjustResult fail(String message) {
        return new StockAdjustResult(false, null, message);
    }
}
