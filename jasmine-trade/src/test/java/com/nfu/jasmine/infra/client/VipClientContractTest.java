package com.nfu.jasmine.infra.client;

import com.nfu.jasmine.common.dto.internal.VipBasicDTO;
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
 * VipClient 契约测试 —— 锁定 trade→crm 的 HTTP 契约。
 * <p>
 * Provider: {@code jasmine-crm/.../vip/web/internal/VipInternalController}，
 * 类注解 {@code @RequestMapping("/internal/vip")}。Provider 改接口时必须同步改本测试。
 */
class VipClientContractTest {

    private static final String BASE_URL = "http://crm-service";

    private MockRestServiceServer server;
    private VipClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        RestClient restClient = builder.baseUrl(BASE_URL).build();
        client = HttpServiceProxyFactory.builderFor(RestClientAdapter.create(restClient))
                .build().createClient(VipClient.class);
    }

    // getVipById：GET /internal/vip/{id}，path 变量正确，VipBasicDTO 字段完整反序列化
    @Test
    void getVipByIdShouldSerializePathAndParseDto() {
        server.expect(requestTo(BASE_URL + "/internal/vip/1"))
                .andExpect(method(org.springframework.http.HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"id\":1,\"vid\":\"V001\",\"name\":\"张三\",\"sex\":\"男\",\"phone\":\"13800138000\"}"));

        VipBasicDTO result = client.getVipById(1);

        assertThat(result.getId()).isEqualTo(1);
        assertThat(result.getVid()).isEqualTo("V001");
        assertThat(result.getName()).isEqualTo("张三");
        assertThat(result.getPhone()).isEqualTo("13800138000");
        server.verify();
    }

    // getVipsByIds：POST /internal/vip/batch，请求体是 [1,2] JSON 数组
    // 注意：参数类型是 Collection<Integer>，序列化为 JSON 数组与 List 无差别
    @Test
    void getVipsByIdsShouldPostJsonArrayBody() {
        server.expect(requestTo(BASE_URL + "/internal/vip/batch"))
                .andExpect(method(org.springframework.http.HttpMethod.POST))
                .andExpect(jsonPath("$[0]").value(1))
                .andExpect(jsonPath("$[1]").value(2))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("[{\"id\":1,\"vid\":\"V001\",\"name\":\"张三\"},{\"id\":2,\"vid\":\"V002\",\"name\":\"李四\"}]"));

        List<VipBasicDTO> result = client.getVipsByIds(List.of(1, 2));

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getVid()).isEqualTo("V001");
        assertThat(result.get(1).getName()).isEqualTo("李四");
        server.verify();
    }

    // getVipsByIds：空列表请求，provider 返回空数组，consumer 解析为空 List
    @Test
    void getVipsByIdsShouldReturnEmptyListWhenProviderReturnsEmptyArray() {
        server.expect(requestTo(BASE_URL + "/internal/vip/batch"))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("[]"));

        List<VipBasicDTO> result = client.getVipsByIds(List.of());

        assertThat(result).isEmpty();
        server.verify();
    }

    // existsById：GET /internal/vip/exists/{id}，返回 boolean
    @Test
    void existsByIdShouldReturnBoolean() {
        server.expect(requestTo(BASE_URL + "/internal/vip/exists/1"))
                .andExpect(method(org.springframework.http.HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("true"));

        boolean result = client.existsById(1);

        assertThat(result).isTrue();
        server.verify();
    }
}
