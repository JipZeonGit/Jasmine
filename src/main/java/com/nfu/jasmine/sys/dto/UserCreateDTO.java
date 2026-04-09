package com.nfu.jasmine.sys.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class UserCreateDTO {
    @NotBlank(message = "用户名不能为空！")
    private String username;

    @NotBlank(message = "密码不能为空！")
    private String password;

    private String email;

    private String phone;

    @NotNull(message = "用户状态不能为空！")
    private Integer status;

    private String avatar;

    private List<Integer> roleIdList;
}