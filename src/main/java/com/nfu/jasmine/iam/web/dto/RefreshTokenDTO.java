package com.nfu.jasmine.iam.web.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RefreshTokenDTO {
    @NotBlank(message = "刷新令牌不能为空！")
    private String refreshToken;
}