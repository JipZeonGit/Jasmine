package com.nfu.jasmine.cus.dto;

import com.nfu.jasmine.common.dto.PageQueryDTO;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.format.annotation.DateTimeFormat;

import java.util.Date;

@Data
@EqualsAndHashCode(callSuper = true)
public class SalesQueryDTO extends PageQueryDTO {
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date date;
}