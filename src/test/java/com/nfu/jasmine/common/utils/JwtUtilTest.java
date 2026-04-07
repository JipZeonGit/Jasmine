package com.nfu.jasmine.common.utils;

import com.nfu.jasmine.sys.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class JwtUtilTest {

    @Autowired
    private JwtUtil jwtUtil;

    @Test
    void createTokenShouldBeParsedBackToUser() {
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