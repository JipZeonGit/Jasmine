package com.nfu.jasmine.common.utils;

import com.alibaba.fastjson2.JSON;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

@Component
public class JwtUtil {
    // 有效期
    private static final long JWT_EXPIRE = 30 * 60 * 1000L;  //半小时
    // 令牌密钥
    private static final String JWT_KEY = "jasmine-jwt-secret-key-for-boot3-upgrade";

    // 创建JWT
    public String createToken(Object data) {
        // 当前时间
        long currentTime = System.currentTimeMillis();
        // 过期时间
        long expTime = currentTime + JWT_EXPIRE;
        return Jwts.builder()
                .id(UUID.randomUUID() + "")
                .subject(JSON.toJSONString(data))
                .issuer("system")
                .issuedAt(new Date(currentTime))
                .expiration(new Date(expTime))
                .signWith(encodeSecret(JWT_KEY))
                .compact();
    }

    // 私钥
    private SecretKey encodeSecret(String key) {
        return Keys.hmacShaKeyFor(key.getBytes(StandardCharsets.UTF_8));
    }

    // 解析JWT
    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(encodeSecret(JWT_KEY))
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public <T> T parseToken(String token, Class<T> clazz) {
        Claims body = Jwts.parser()
                .verifyWith(encodeSecret(JWT_KEY))
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return JSON.parseObject(body.getSubject(), clazz);
    }
}
