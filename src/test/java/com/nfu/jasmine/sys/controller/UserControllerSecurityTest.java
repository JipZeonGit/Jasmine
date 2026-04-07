package com.nfu.jasmine.sys.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nfu.jasmine.sys.entity.User;
import com.nfu.jasmine.sys.mapper.UserMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.HashMap;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UserControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @AfterEach
    void cleanup() {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, "security-smoke-user");
        userMapper.delete(wrapper);
    }

    @Test
    void getUserListWithoutLoginShouldBeRejected() throws Exception {
        mockMvc.perform(get("/user/list")
                        .param("pageNo", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(20003));
    }

    @Test
    void loginWithoutUsernameShouldFailValidation() throws Exception {
        Map<String, Object> loginRequest = new HashMap<>();
        loginRequest.put("password", "password123");

        mockMvc.perform(post("/user/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(20005))
                .andExpect(jsonPath("$.message").value("用户名不能为空！"));
    }

    @Test
    void getUserInfoWithAuthorizationHeaderShouldSucceed() throws Exception {
        String token = createLoginToken();

        mockMvc.perform(get("/user/info")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(20000))
                .andExpect(jsonPath("$.data.name").value("security-smoke-user"));
    }

    @Test
    void getUserInfoWithLegacyXTokenShouldBeRejected() throws Exception {
        String token = createLoginToken();

        mockMvc.perform(get("/user/info")
                        .header("X-Token", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(20003));
    }

    @Test
    void userInfoPreflightRequestShouldAllowFrontendOrigin() throws Exception {
        mockMvc.perform(options("/user/info")
                        .header("Origin", "http://localhost:8888")
                        .header("Access-Control-Request-Method", "GET")
                        .header("Access-Control-Request-Headers", "Authorization"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:8888"));
    }

    private String createLoginToken() throws Exception {
        User seedUser = new User();
        seedUser.setUsername("security-smoke-user");
        seedUser.setPassword(passwordEncoder.encode("password123"));
        seedUser.setEmail("security-smoke-user@test.com");
        seedUser.setPhone("13800000001");
        seedUser.setStatus(1);
        seedUser.setAvatar("https://example.com/security-smoke-user.png");
        seedUser.setDeleted(0);
        userMapper.insert(seedUser);

        Map<String, Object> loginRequest = new HashMap<>();
        loginRequest.put("username", "security-smoke-user");
        loginRequest.put("password", "password123");

        String loginResponse = mockMvc.perform(post("/user/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(20000))
                .andReturn()
                .getResponse()
                .getContentAsString();

        return objectMapper.readTree(loginResponse).path("data").path("token").asText();
    }
}
