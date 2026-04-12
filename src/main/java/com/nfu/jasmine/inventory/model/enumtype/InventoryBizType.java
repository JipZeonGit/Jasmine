package com.nfu.jasmine.inventory.model.enumtype;

import com.nfu.jasmine.common.exception.BusinessException;

public enum InventoryBizType {
    PURCHASE_IN("采购入库", 1),
    SALE_OUT("销售出库", -1),
    LOSS_OUT("损耗出库", -1),
    RETURN_IN("退货入库", 1),
    CHECK_IN("盘点调增", 1),
    CHECK_OUT("盘点调减", -1);

    private final String label;
    private final int direction;

    InventoryBizType(String label, int direction) {
        this.label = label;
        this.direction = direction;
    }

    public String getLabel() {
        return label;
    }

    // 前端始终录入正数，库存方向由业务类型统一转换，避免再靠“正负号”表达业务语义。
    public int apply(int quantity) {
        return quantity * direction;
    }

    public static InventoryBizType fromCode(String code) {
        for (InventoryBizType type : values()) {
            if (type.name().equals(code)) {
                return type;
            }
        }
        throw new BusinessException("不支持的库存业务类型：" + code);
    }
}
