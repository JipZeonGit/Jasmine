package com.nfu.jasmine.common.dto.internal;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 库存调整结果 DTO，用于服务间内部通信。
 * <p>
 * 调整成功时附带调整前/后库存与花卉完整信息，调用方无需再发起第二次查询。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StockAdjustResult {
    private Boolean success;
    private Integer beforeStock;
    private Integer currentStock;
    private FlowerDTO flower;
    private String message;

    public static StockAdjustResult ok(Integer beforeStock, Integer currentStock, FlowerDTO flower) {
        return new StockAdjustResult(true, beforeStock, currentStock, flower, null);
    }

    public static StockAdjustResult fail(String message) {
        return new StockAdjustResult(false, null, null, null, message);
    }
}
