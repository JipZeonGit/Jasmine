package com.nfu.jasmine.cus.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.util.Date;

@Data
public class InventorySaveDTO {
    private Integer id;

    @NotNull(message = "花卉不能为空！")
    private Integer flowerId;

    @NotBlank(message = "业务类型不能为空！")
    private String bizType;

    @NotNull(message = "变动数量不能为空！")
    private Integer quantity;

    private BigDecimal unitCost;

    @NotNull(message = "业务时间不能为空！")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date date;

    private String remark;
}
