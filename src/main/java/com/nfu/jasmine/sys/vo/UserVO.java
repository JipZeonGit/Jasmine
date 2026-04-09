package com.nfu.jasmine.sys.vo;

import lombok.Data;

import java.util.List;

@Data
public class UserVO {
    private Integer id;
    private String username;
    private String email;
    private String phone;
    private Integer status;
    private String avatar;
    private List<Integer> roleIdList;
}