package com.nfu.jasmine.common.utils;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class JwtTokenClaims {
    private String tokenId;
    private Integer userId;
    private String username;
    private String tokenType;
    private LocalDateTime issuedAt;
    private LocalDateTime expiresAt;
}