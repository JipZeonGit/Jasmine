package com.nfu.jasmine.common.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.UUID;

@Component
public class JwtUtil {
    private static final String TOKEN_TYPE_ACCESS = "access";
    private static final String TOKEN_TYPE_REFRESH = "refresh";

    // JWT 过期时间，默认半小时
    @Value("${app.security.jwt-expire-millis:1800000}")
    private long jwtExpire;

    // Refresh Token 过期时间，默认七天
    @Value("${app.security.jwt-refresh-expire-millis:604800000}")
    private long jwtRefreshExpire;

    // JWT 密钥，生产环境必须从环境变量注入
    @Value("${app.security.jwt-secret}")
    private String jwtKey;

    // 创建 Access Token
    public String createAccessToken(Integer userId, String username) {
        return createToken(userId, username, TOKEN_TYPE_ACCESS, UUID.randomUUID().toString(), jwtExpire);
    }

    // 创建 Refresh Token
    public String createRefreshToken(Integer userId, String username, String tokenId) {
        return createToken(userId, username, TOKEN_TYPE_REFRESH, tokenId, jwtRefreshExpire);
    }

    // 解析 JWT
    public JwtTokenClaims parseToken(String token) {
        Claims body = Jwts.parser()
                .verifyWith(encodeSecret(jwtKey))
                .build()
                .parseSignedClaims(token)
                .getPayload();

        Number userId = body.get("uid", Number.class);
        if (userId == null) {
            throw new JwtException("JWT缺少用户标识");
        }

        return new JwtTokenClaims(
                body.getId(),
                userId.intValue(),
                body.getSubject(),
                body.get("type", String.class),
                toLocalDateTime(body.getIssuedAt()),
                toLocalDateTime(body.getExpiration())
        );
    }

    // 解析 Access Token
    public JwtTokenClaims parseAccessToken(String token) {
        JwtTokenClaims claims = parseToken(token);
        validateTokenType(claims, TOKEN_TYPE_ACCESS);
        return claims;
    }

    // 解析 Refresh Token
    public JwtTokenClaims parseRefreshToken(String token) {
        JwtTokenClaims claims = parseToken(token);
        validateTokenType(claims, TOKEN_TYPE_REFRESH);
        return claims;
    }

    public long getJwtRefreshExpire() {
        return jwtRefreshExpire;
    }

    // 创建 JWT
    private String createToken(Integer userId, String username, String tokenType, String tokenId, long expireMillis) {
        // 当前时间
        long currentTime = System.currentTimeMillis();
        // 过期时间
        long expTime = currentTime + expireMillis;
        return Jwts.builder()
                .id(tokenId)
                .subject(username)
                .issuer("system")
                .claim("uid", userId)
                .claim("type", tokenType)
                .issuedAt(new Date(currentTime))
                .expiration(new Date(expTime))
                .signWith(encodeSecret(jwtKey))
                .compact();
    }

    private void validateTokenType(JwtTokenClaims claims, String expectedType) {
        if (!expectedType.equals(claims.getTokenType())) {
            throw new JwtException("JWT类型无效");
        }
    }

    private LocalDateTime toLocalDateTime(Date date) {
        return LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault());
    }

    // 密钥
    private SecretKey encodeSecret(String key) {
        return Keys.hmacShaKeyFor(key.getBytes(StandardCharsets.UTF_8));
    }
}