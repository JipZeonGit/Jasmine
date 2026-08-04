package com.nfu.jasmine.infra.client;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

/**
 * CrmUserClient 契约测试 —— 锁定 crm→iam 的 HTTP 契约。
 * <p>
 * Provider: {@code jasmine-iam/.../iam/web/internal/UserInternalController}，
 * 类注解 {@code @RequestMapping("/internal/user")}。Provider 改接口时必须同步改本测试。
 */
class CrmUserClientContractTest {

    private static final String BASE_URL = "http://iam-service";

    private MockRestServiceServer server;
    private CrmUserClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        RestClient restClient = builder.baseUrl(BASE_URL).build();
        client = HttpServiceProxyFactory.builderFor(RestClientAdapter.create(restClient))
                .build().createClient(CrmUserClient.class);
    }

    // getActiveUserIdsByRoles：GET /internal/user/active-ids-by-roles?roleNames=a&roleNames=b
    // 多值 query 参数：@RequestParam List<String> 会被 RestClient 序列化为
    // roleNames=admin&roleNames=boss（重复参数名），用 requestTo 精确匹配完整 URL
    @Test
    void getActiveUserIdsByRolesShouldPassMultiValueQueryParam() {
        server.expect(requestTo(BASE_URL + "/internal/user/active-ids-by-roles?roleNames=admin&roleNames=boss"))
                .andExpect(method(org.springframework.http.HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("[1,2]"));

        List<Integer> result = client.getActiveUserIdsByRoles(List.of("admin", "boss"));

        assertThat(result).containsExactly(1, 2);
        server.verify();
    }

    // getActiveUserIdsByRoles：无匹配角色时返回空数组
    @Test
    void getActiveUserIdsByRolesShouldReturnEmptyWhenNoMatch() {
        server.expect(requestTo(BASE_URL + "/internal/user/active-ids-by-roles?roleNames=ghost"))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("[]"));

        List<Integer> result = client.getActiveUserIdsByRoles(List.of("ghost"));

        assertThat(result).isEmpty();
        server.verify();
    }
}
