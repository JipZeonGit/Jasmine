package com.nfu.jasmine.sys.service;

import com.nfu.jasmine.config.AbstractIntegrationTest;
import com.nfu.jasmine.sys.dto.LoginDTO;
import com.nfu.jasmine.sys.entity.User;
import com.nfu.jasmine.sys.mapper.UserMapper;
import com.nfu.jasmine.sys.vo.LoginVO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class UserServiceLoginIT extends AbstractIntegrationTest {

    @Autowired
    private IUserService userService;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void loginReturnsTokenPairForKnownUser() {
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        String username = "login-smoke-" + suffix;
        User seedUser = new User();
        seedUser.setUsername(username);
        seedUser.setPassword(passwordEncoder.encode("password123"));
        seedUser.setEmail("login-" + suffix + "@test.com");
        seedUser.setPhone("13800000000");
        seedUser.setStatus(1);
        seedUser.setAvatar("https://example.com/" + username + ".png");
        seedUser.setDeleted(0);
        userMapper.insert(seedUser);

        LoginDTO loginRequest = new LoginDTO();
        loginRequest.setUsername(username);
        loginRequest.setPassword("password123");

        LoginVO data = userService.login(loginRequest);

        assertThat(data).isNotNull();
        assertThat(data.getToken()).isNotBlank();
        assertThat(data.getRefreshToken()).isNotBlank();
    }
}
