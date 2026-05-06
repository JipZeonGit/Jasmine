package com.nfu.jasmine.flower.web.dto;

import com.nfu.jasmine.common.dto.PageQueryDTO;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class FlowerQueryDTO extends PageQueryDTO {
    private String name;
}