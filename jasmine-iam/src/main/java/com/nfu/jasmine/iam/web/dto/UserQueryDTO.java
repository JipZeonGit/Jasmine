package com.nfu.jasmine.iam.web.dto;

import com.nfu.jasmine.common.dto.PageQueryDTO;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class UserQueryDTO extends PageQueryDTO {
    private String username;
    private String phone;
}