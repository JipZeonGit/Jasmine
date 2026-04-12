package com.nfu.jasmine.vip.web.dto;

import com.nfu.jasmine.common.dto.PageQueryDTO;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class VipQueryDTO extends PageQueryDTO {
    private String name;
    private String vid;
    private String phone;
}