package com.nfu.jasmine.common.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PageQueryDTO {
    @NotNull(message = "页码不能为空！")
    @Min(value = 1, message = "页码不能小于 1！")
    private Long pageNo;

    @NotNull(message = "每页条数不能为空！")
    @Min(value = 1, message = "每页条数不能小于 1！")
    @Max(value = 100, message = "每页条数不能超过 100！")
    private Long pageSize;
}