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

    private JwtUtil createJwtUtil() {
        JwtUtil jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "jwtExpire", 1800000L);
        ReflectionTestUtils.setField(jwtUtil, "jwtRefreshExpire", 604800000L);
        ReflectionTestUtils.setField(jwtUtil, "jwtKey", "jasmine-unit-test-jwt-secret-for-fast-tests-2026");
        return jwtUtil;
    }
}