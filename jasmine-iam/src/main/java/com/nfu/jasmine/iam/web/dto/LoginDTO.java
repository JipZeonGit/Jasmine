package com.nfu.jasmine.iam.web.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * <p>
 * 登录请求参数
 * </p>
 *
 * @author jipzeongit
 * @since 2026-04-07
 */
@Data
public class LoginDTO {
    @NotBlank(message = "用户名不能为空！")
    private String username;

    @NotBlank(message = "密码不能为空！")
    private String password;
}