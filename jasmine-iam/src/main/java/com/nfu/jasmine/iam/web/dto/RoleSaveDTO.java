package com.nfu.jasmine.iam.web.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

@Data
public class RoleSaveDTO {
    private Integer roleId;

    @NotBlank(message = "角色名称不能为空！")
    private String roleName;

    private String roleDesc;

    private List<Integer> menuIdList;
}