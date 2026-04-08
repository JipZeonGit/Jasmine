package com.nfu.jasmine.sys.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * <p>
 * 修改密码请求参数
 * </p>
 *
 * @author jipzeongit
 * @since 2026-04-07
 */
@Data
public class ChangePasswordDTO {
    @NotBlank(message = "用户名不能为空！")
    private String username;

    @NotBlank(message = "旧密码不能为空！")
    private String oldPassword;

    @NotBlank(message = "新密码不能为空！")
    private String newPassword;
}