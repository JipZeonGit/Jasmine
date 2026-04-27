package com.nfu.jasmine.iam.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nfu.jasmine.config.AbstractIntegrationTest;
import com.nfu.jasmine.iam.model.entity.User;
import com.nfu.jasmine.iam.model.entity.UserRole;
import com.nfu.jasmine.iam.persistence.mapper.UserMapper;
import com.nfu.jasmine.iam.persistence.mapper.UserRoleMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.nullValue;
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
    private UserRoleMapper userRoleMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void getUserListWithoutLoginShouldBeRejected() throws Exception {
        mockMvc.perform(get("/user/list")
                        .param("pageNo", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isUnauthorized())
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
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(20003));
    }

    @Test
    void nonAdminUserShouldNotAccessUserList() throws Exception {
        JsonNode loginData = loginAndReturnData();

        mockMvc.perform(get("/user/list")
                        .header("Authorization", "Bearer " + loginData.path("token").asText())
                        .param("pageNo", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(20004));
    }

    @Test
    void refreshTokenShouldReturnNewTokenPair() throws Exception {
        JsonNode loginData = loginAndReturnData();
        String refreshCookie = loginData.path("refreshCookie").asText();

        String response = mockMvc.perform(post("/user/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}")
                        .header("Cookie", refreshCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(20000))
                .andExpect(jsonPath("$.data.token").isString())
                .andExpect(jsonPath("$.data.refreshToken").value(nullValue()))
                .andExpect(header().string("Set-Cookie", containsString("jasmine_refresh_token=")))
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode refreshedData = objectMapper.readTree(response).path("data");
        assertThat(refreshedData.path("token").asText()).isNotEqualTo(loginData.path("token").asText());
    }

    @Test
    void logoutShouldInvalidateRefreshToken() throws Exception {
        JsonNode loginData = loginAndReturnData();
        String accessToken = loginData.path("token").asText();
        String refreshCookie = loginData.path("refreshCookie").asText();

        mockMvc.perform(post("/user/logout")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(20000));

        mockMvc.perform(post("/user/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}")
                        .header("Cookie", refreshCookie))
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
        userRoleMapper.insert(new UserRole(null, seedUser.getId(), 4));

        Map<String, Object> loginRequest = new HashMap<>();
        loginRequest.put("username", username);
        loginRequest.put("password", "password123");

        MvcResult loginResult = mockMvc.perform(post("/user/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(20000))
                .andExpect(jsonPath("$.data.token").isString())
                .andExpect(jsonPath("$.data.refreshToken").value(nullValue()))
                .andExpect(header().string("Set-Cookie", containsString("jasmine_refresh_token=")))
                .andReturn();

        String loginResponse = loginResult.getResponse().getContentAsString();
        JsonNode data = objectMapper.readTree(loginResponse).path("data");
        ((com.fasterxml.jackson.databind.node.ObjectNode) data).put("username", username);
        ((com.fasterxml.jackson.databind.node.ObjectNode) data).put("refreshCookie",
                extractCookiePair(loginResult.getResponse().getHeader("Set-Cookie")));
        return data;
    }

    private String extractCookiePair(String setCookieHeader) {
        assertThat(setCookieHeader).isNotBlank();
        return setCookieHeader.split(";", 2)[0];
    }
}
