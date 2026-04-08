package com.nfu.jasmine.common.utils;

import com.nfu.jasmine.sys.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class JwtUtilTest {

    @Test
    void createTokenShouldBeParsedBackToUser() {
        JwtUtil jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "jwtExpire", 1800000L);
        ReflectionTestUtils.setField(jwtUtil, "jwtKey", "jasmine-unit-test-jwt-secret-for-fast-tests-2026");

        User user = new User();
        user.setId(1);
        user.setUsername("jwt-smoke-user");
        user.setEmail("jwt-smoke-user@test.com");
        user.setPhone("13800000002");
        user.setStatus(1);
        user.setAvatar("https://example.com/jwt-smoke-user.png");
        user.setDeleted(0);

        String token = jwtUtil.createToken(user);
        User loginUser = jwtUtil.parseToken(token, User.class);

        assertThat(token).isNotBlank();
        assertThat(loginUser).isNotNull();
        assertThat(loginUser.getUsername()).isEqualTo("jwt-smoke-user");
    }
}