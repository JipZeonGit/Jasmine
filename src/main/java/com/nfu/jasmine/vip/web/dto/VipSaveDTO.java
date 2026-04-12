package com.nfu.jasmine.vip.web.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class VipSaveDTO {
    private Integer id;
    private String vid;

    @NotBlank(message = "会员姓名不能为空！")
    private String name;

    @NotBlank(message = "会员性别不能为空！")
    private String sex;

    @NotBlank(message = "手机号不能为空！")
    private String phone;
}