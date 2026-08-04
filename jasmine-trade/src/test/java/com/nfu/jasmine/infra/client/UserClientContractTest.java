package com.nfu.jasmine.infra.client;

import com.nfu.jasmine.common.dto.internal.UserBasicDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

/**
 * UserClient 契约测试 —— 锁定 trade→iam 的 HTTP 契约。
 * <p>
 * Provider: {@code jasmine-iam/.../iam/web/internal/UserInternalController}，
 * 类注解 {@code @RequestMapping("/internal/user")}。Provider 改接口时必须同步改本测试。
 */
class UserClientContractTest {

    private static final String BASE_URL = "http://iam-service";

    private MockRestServiceServer server;
    private UserClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        RestClient restClient = builder.baseUrl(BASE_URL).build();
        client = HttpServiceProxyFactory.builderFor(RestClientAdapter.create(restClient))
                .build().createClient(UserClient.class);
    }

    // getUserById：GET /internal/user/{id}，path 变量正确，UserBasicDTO 字段完整反序列化
    @Test
    void getUserByIdShouldSerializePathAndParseDto() {
        server.expect(requestTo(BASE_URL + "/internal/user/1"))
                .andExpect(method(org.springframework.http.HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"id\":1,\"username\":\"admin\",\"realName\":\"admin\"}"));

        UserBasicDTO result = client.getUserById(1);

        assertThat(result.getId()).isEqualTo(1);
        assertThat(result.getUsername()).isEqualTo("admin");
        assertThat(result.getRealName()).isEqualTo("admin");
        server.verify();
    }

    // getUserById：provider 返回 404（用户不存在），RestClient 默认抛 HttpClientErrorException
    // 这与 FlowerInternalController/VipInternalController 的 404 行为一致
    @Test
    void getUserByIdShouldThrowOn404NotFound() {
        server.expect(requestTo(BASE_URL + "/internal/user/999"))
                .andRespond(withStatus(HttpStatus.NOT_FOUND)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"code\":404,\"message\":\"用户不存在！\"}"));

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> client.getUserById(999))
                .isInstanceOf(org.springframework.web.client.HttpClientErrorException.class)
                .hasMessageContaining("404");
        server.verify();
    }
}
