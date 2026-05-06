package com.nfu.jasmine.inventory.web.dto;

import com.nfu.jasmine.common.dto.PageQueryDTO;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.format.annotation.DateTimeFormat;

import java.util.Date;

@Data
@EqualsAndHashCode(callSuper = true)
public class InventoryQueryDTO extends PageQueryDTO {
    private String name;
    private String num;
    private String bizType;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date startTime;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date endTime;
}
