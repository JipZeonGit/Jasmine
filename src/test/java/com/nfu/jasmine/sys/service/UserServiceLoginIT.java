package com.nfu.jasmine.sys.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.nfu.jasmine.config.AbstractIntegrationTest;
import com.nfu.jasmine.sys.dto.LoginDTO;
import com.nfu.jasmine.sys.entity.User;
import com.nfu.jasmine.sys.mapper.UserMapper;
import com.nfu.jasmine.sys.vo.LoginVO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;

class UserServiceLoginIT extends AbstractIntegrationTest {

    @Autowired
    private IUserService userService;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @AfterEach
    void cleanup() {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, "login-smoke-user");
        userMapper.delete(wrapper);
    }

    @Test
    void loginReturnsTokenPairForKnownUser() {
        User seedUser = new User();
        seedUser.setUsername("login-smoke-user");
        seedUser.setPassword(passwordEncoder.encode("password123"));
        seedUser.setEmail("login-smoke-user@test.com");
        seedUser.setPhone("13800000000");
        seedUser.setStatus(1);
        seedUser.setAvatar("https://example.com/login-smoke-user.png");
        seedUser.setDeleted(0);
        userMapper.insert(seedUser);

        LoginDTO loginRequest = new LoginDTO();
        loginRequest.setUsername("login-smoke-user");
        loginRequest.setPassword("password123");

        LoginVO data = userService.login(loginRequest);

        assertThat(data).isNotNull();
        assertThat(data.getToken()).isNotBlank();
        assertThat(data.getRefreshToken()).isNotBlank();
    }
}