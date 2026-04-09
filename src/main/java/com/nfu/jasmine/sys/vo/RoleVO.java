package com.nfu.jasmine.sys.vo;

import lombok.Data;

import java.util.List;

@Data
public class RoleVO {
    private Integer roleId;
    private String roleName;
    private String roleDesc;
    private List<Integer> menuIdList;
}