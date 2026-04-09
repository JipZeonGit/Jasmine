package com.nfu.jasmine.sys.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class UserUpdateDTO {
    @NotNull(message = "用户ID不能为空！")
    private Integer id;

    @NotBlank(message = "用户名不能为空！")
    private String username;

    private String email;

    private String phone;

    @NotNull(message = "用户状态不能为空！")
    private Integer status;

    private String avatar;

    private List<Integer> roleIdList;
}