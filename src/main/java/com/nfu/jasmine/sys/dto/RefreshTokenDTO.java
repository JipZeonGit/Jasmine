package com.nfu.jasmine.sys.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RefreshTokenDTO {
    @NotBlank(message = "刷新令牌不能为空！")
    private String refreshToken;
}