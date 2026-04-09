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

    @NotNull(message = "花卉单价不能为空！")
    private BigDecimal unitprice;

    @NotNull(message = "花卉成本不能为空！")
    private BigDecimal costs;
}