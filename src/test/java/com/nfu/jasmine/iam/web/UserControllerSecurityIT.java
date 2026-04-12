package com.nfu.jasmine.iam.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nfu.jasmine.config.AbstractIntegrationTest;
import com.nfu.jasmine.iam.model.entity.User;
import com.nfu.jasmine.iam.persistence.mapper.UserMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class UserControllerSecurityIT extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void getUserListWithoutLoginShouldBeRejected() throws Exception {
        mockMvc.perform(get("/user/list")
                        .param("pageNo", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(20003));
    }

    @Test
    void actuatorHealthShouldBePublic() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.components.db.status").value("UP"));
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
        JsonNode loginData = loginAndReturnData();

        mockMvc.perform(get("/user/info")
                        .header("Authorization", "Bearer " + loginData.path("token").asText()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(20000))
                .andExpect(jsonPath("$.data.name").value(loginData.path("username").asText()));
    }

    @Test
    void getUserInfoWithLegacyXTokenShouldBeRejected() throws Exception {
        JsonNode loginData = loginAndReturnData();

        mockMvc.perform(get("/user/info")
                        .header("X-Token", loginData.path("token").asText()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(20003));
    }

    @Test
    void refreshTokenShouldReturnNewTokenPair() throws Exception {
        JsonNode loginData = loginAndReturnData();
        Map<String, Object> refreshRequest = new HashMap<>();
        refreshRequest.put("refreshToken", loginData.path("refreshToken").asText());

        String response = mockMvc.perform(post("/user/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(refreshRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(20000))
                .andExpect(jsonPath("$.data.token").isString())
                .andExpect(jsonPath("$.data.refreshToken").isString())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode refreshedData = objectMapper.readTree(response).path("data");
        assertThat(refreshedData.path("token").asText()).isNotEqualTo(loginData.path("token").asText());
        assertThat(refreshedData.path("refreshToken").asText()).isNotEqualTo(loginData.path("refreshToken").asText());
    }

    @Test
    void logoutShouldInvalidateRefreshToken() throws Exception {
        JsonNode loginData = loginAndReturnData();
        String accessToken = loginData.path("token").asText();
        String refreshToken = loginData.path("refreshToken").asText();

        mockMvc.perform(post("/user/logout")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(20000));

        Map<String, Object> refreshRequest = new HashMap<>();
        refreshRequest.put("refreshToken", refreshToken);

        mockMvc.perform(post("/user/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(refreshRequest)))
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

    private JsonNode loginAndReturnData() throws Exception {
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        String username = "security-smoke-" + suffix;
        User seedUser = new User();
        seedUser.setUsername(username);
        seedUser.setPassword(passwordEncoder.encode("password123"));
        seedUser.setEmail("sec-" + suffix + "@test.com");
        seedUser.setPhone("13800000001");
        seedUser.setStatus(1);
        seedUser.setAvatar("https://example.com/" + username + ".png");
        seedUser.setDeleted(0);
        userMapper.insert(seedUser);

        Map<String, Object> loginRequest = new HashMap<>();
        loginRequest.put("username", username);
        loginRequest.put("password", "password123");

        String loginResponse = mockMvc.perform(post("/user/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(20000))
                .andExpect(jsonPath("$.data.token").isString())
                .andExpect(jsonPath("$.data.refreshToken").isString())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode data = objectMapper.readTree(loginResponse).path("data");
        ((com.fasterxml.jackson.databind.node.ObjectNode) data).put("username", username);
        return data;
    }
}
