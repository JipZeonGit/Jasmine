package com.nfu.jasmine.cus.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class SalesItemSaveDTO {
    private Integer id;

    @NotNull(message = "花卉不能为空！")
    private Integer flowerId;

    @NotNull(message = "销售数量不能为空！")
    private Integer quantity;

    @NotNull(message = "销售单价不能为空！")
    private BigDecimal unitPrice;
}
