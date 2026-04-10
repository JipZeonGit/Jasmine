package com.nfu.jasmine.cus.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class FlowerSaveDTO {
    private Integer id;

    @NotBlank(message = "花卉名称不能为空！")
    private String name;

    @NotBlank(message = "计量单位不能为空！")
    private String unit;

    @NotNull(message = "花卉售价不能为空！")
    private BigDecimal salePrice;

    @NotNull(message = "花卉成本不能为空！")
    private BigDecimal costPrice;

    @NotNull(message = "安全库存不能为空！")
    private Integer safeStock;

    @NotNull(message = "在售状态不能为空！")
    private Integer status;
}
