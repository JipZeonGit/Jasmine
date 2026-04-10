package com.nfu.jasmine.cus.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.util.Date;
import java.util.List;

@Data
public class SalesSaveDTO {
    private Integer id;
    private Integer vipId;

    @NotNull(message = "销售时间不能为空！")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date date;

    private String remark;

    @Valid
    @NotEmpty(message = "销售明细不能为空！")
    private List<SalesItemSaveDTO> items;
}
