package com.nfu.jasmine.cus.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.util.Date;

@Data
public class InventorySaveDTO {
    private Integer id;

    @NotBlank(message = "花卉名称不能为空！")
    private String name;

    private String num;

    @NotNull(message = "库存数量不能为空！")
    private Integer quantity;

    @NotNull(message = "入库时间不能为空！")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date date;

    @NotNull(message = "剩余数量不能为空！")
    private Integer residue;
}