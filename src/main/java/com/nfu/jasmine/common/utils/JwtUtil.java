package com.nfu.jasmine.common.utils;

import com.alibaba.fastjson2.JSON;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

@Component
public class JwtUtil {
    // JWT 过期时间，默认半小时
    @Value("${app.security.jwt-expire-millis:1800000}")
    private long jwtExpire;

    // JWT 密钥，生产环境必须从环境变量注入
    @Value("${app.security.jwt-secret}")
    private String jwtKey;

    // 创建JWT
    public String createToken(Object data) {
        // 当前时间
        long currentTime = System.currentTimeMillis();
        // 过期时间
        long expTime = currentTime + jwtExpire;
        return Jwts.builder()
                .id(UUID.randomUUID() + "")
                .subject(JSON.toJSONString(data))
                .issuer("system")
                .issuedAt(new Date(currentTime))
                .expiration(new Date(expTime))
                .signWith(encodeSecret(jwtKey))
                .compact();
    }

    // 密钥
    private SecretKey encodeSecret(String key) {
        return Keys.hmacShaKeyFor(key.getBytes(StandardCharsets.UTF_8));
    }

    // 解析JWT
    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(encodeSecret(jwtKey))
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public <T> T parseToken(String token, Class<T> clazz) {
        Claims body = Jwts.parser()
                .verifyWith(encodeSecret(jwtKey))
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return JSON.parseObject(body.getSubject(), clazz);
    }
}