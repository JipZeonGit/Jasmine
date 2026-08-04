package com.nfu.jasmine.common.utils;

import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtUtilTest {

    @Test
    void createAccessTokenShouldBeParsedBackToClaims() {
        JwtUtil jwtUtil = createJwtUtil();

        String token = jwtUtil.createAccessToken(1, "jwt-smoke-user");
        JwtTokenClaims claims = jwtUtil.parseAccessToken(token);

        assertThat(token).isNotBlank();
        assertThat(claims.getUserId()).isEqualTo(1);
        assertThat(claims.getUsername()).isEqualTo("jwt-smoke-user");
        assertThat(claims.getTokenType()).isEqualTo("access");
    }

    @Test
    void parseAccessTokenShouldRejectRefreshToken() {
        JwtUtil jwtUtil = createJwtUtil();

        String refreshToken = jwtUtil.createRefreshToken(1, "jwt-smoke-user", "refresh-token-id");

        assertThatThrownBy(() -> jwtUtil.parseAccessToken(refreshToken))
                .isInstanceOf(JwtException.class);
    }

    // 过期 token 应被拒绝
    @Test
    void parseTokenShouldRejectExpiredToken() {
        JwtUtil jwtUtil = createJwtUtilWithExpire(-1000L); // 负过期 = 已过期

        String token = jwtUtil.createAccessToken(1, "expired-user");

        assertThatThrownBy(() -> jwtUtil.parseAccessToken(token))
                .isInstanceOf(JwtException.class);
    }

    // 篡改签名应被拒绝
    @Test
    void parseTokenShouldRejectTamperedSignature() {
        JwtUtil jwtUtil = createJwtUtil();
        JwtUtil otherJwtUtil = createJwtUtilWithKey("another-different-jwt-secret-key-for-tamper-test-2026");

        String token = jwtUtil.createAccessToken(1, "tamper-user");
        // 用不同密钥的实例解析，模拟签名被篡改
        assertThatThrownBy(() -> otherJwtUtil.parseAccessToken(token))
                .isInstanceOf(JwtException.class);
    }

    // refresh token 应能正确识别类型并解析回 tokenId
    @Test
    void createRefreshTokenShouldBeParsedBackWithTokenId() {
        JwtUtil jwtUtil = createJwtUtil();
        String tokenId = "refresh-token-id-123";

        String refreshToken = jwtUtil.createRefreshToken(1, "refresh-user", tokenId);
        JwtTokenClaims claims = jwtUtil.parseRefreshToken(refreshToken);

        assertThat(claims.getTokenType()).isEqualTo("refresh");
        assertThat(claims.getTokenId()).isEqualTo(tokenId);
        assertThat(claims.getUserId()).isEqualTo(1);
        assertThat(claims.getUsername()).isEqualTo("refresh-user");
    }

    // claims 字段完整性：issuedAt / expiresAt 均应非空
    @Test
    void tokenClaimsShouldContainCompleteTimestamps() {
        JwtUtil jwtUtil = createJwtUtil();

        String token = jwtUtil.createAccessToken(1, "claims-user");
        JwtTokenClaims claims = jwtUtil.parseAccessToken(token);

        assertThat(claims.getIssuedAt()).isNotNull();
        assertThat(claims.getExpiresAt()).isNotNull();
        assertThat(claims.getExpiresAt()).isAfter(claims.getIssuedAt());
    }

    private JwtUtil createJwtUtil() {
        return createJwtUtilWithExpire(1800000L);
    }

    private JwtUtil createJwtUtilWithExpire(long expireMillis) {
        JwtUtil jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "jwtExpire", expireMillis);
        ReflectionTestUtils.setField(jwtUtil, "jwtRefreshExpire", 604800000L);
        ReflectionTestUtils.setField(jwtUtil, "jwtKey", "jasmine-unit-test-jwt-secret-for-fast-tests-2026");
        return jwtUtil;
    }

    private JwtUtil createJwtUtilWithKey(String key) {
        JwtUtil jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "jwtExpire", 1800000L);
        ReflectionTestUtils.setField(jwtUtil, "jwtRefreshExpire", 604800000L);
        ReflectionTestUtils.setField(jwtUtil, "jwtKey", key);
        return jwtUtil;
    }
}