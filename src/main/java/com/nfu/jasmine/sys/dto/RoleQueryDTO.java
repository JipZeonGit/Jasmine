package com.nfu.jasmine.sys.dto;

import com.nfu.jasmine.common.dto.PageQueryDTO;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class RoleQueryDTO extends PageQueryDTO {
    private String roleName;
}