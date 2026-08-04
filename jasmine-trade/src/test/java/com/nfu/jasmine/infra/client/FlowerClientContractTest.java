package com.nfu.jasmine.infra.client;

import com.nfu.jasmine.common.dto.internal.FlowerDTO;
import com.nfu.jasmine.common.dto.internal.StockAdjustRequest;
import com.nfu.jasmine.common.dto.internal.StockAdjustResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

/**
 * FlowerClient 契约测试 —— 锁定 trade→product 的 HTTP 契约。
 * <p>
 * Provider: {@code jasmine-product/.../flower/web/internal/FlowerInternalController}，
 * 类注解 {@code @RequestMapping("/internal/flower")}。Provider 改接口时必须同步改本测试，
 * 否则 CI 报红即代表契约漂移。
 * <p>
 * 本测试手搓 RestClient 绑定到 MockRestServiceServer，绕过 InternalClientFactory 的
 * LoadBalancer 逻辑——契约测试只验"请求路径/方法/序列化/响应解析"，不验 LB/熔断/超时
 * （后者由 {@link RemoteProductStockFacadeTest} 覆盖）。
 */
class FlowerClientContractTest {

    private static final String BASE_URL = "http://product-service";

    private MockRestServiceServer server;
    private FlowerClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        // 手搓 RestClient-backed HttpInterface，与生产 InternalClientFactory 的创建方式一致
        // （都是 HttpServiceProxyFactory + RestClientAdapter），保证契约行为相同
        RestClient restClient = builder.baseUrl(BASE_URL).build();
        client = HttpServiceProxyFactory.builderFor(RestClientAdapter.create(restClient))
                .build().createClient(FlowerClient.class);
    }

    // getFlowerById：GET /internal/flower/{id}，path 变量正确，FlowerDTO 字段完整反序列化
    @Test
    void getFlowerByIdShouldSerializePathAndParseDto() {
        server.expect(requestTo(BASE_URL + "/internal/flower/1"))
                .andExpect(method(org.springframework.http.HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"id\":1,\"name\":\"红玫瑰\",\"price\":9.90,\"cost\":5.00,\"status\":1,\"safeStock\":10,\"currentStock\":120}"));

        FlowerDTO result = client.getFlowerById(1);

        assertThat(result.getId()).isEqualTo(1);
        assertThat(result.getName()).isEqualTo("红玫瑰");
        assertThat(result.getPrice()).isEqualByComparingTo("9.90");
        assertThat(result.getCurrentStock()).isEqualTo(120);
        server.verify();
    }

    // getFlowersByIds：POST /internal/flower/batch，请求体是 [1,2,3] JSON 数组
    @Test
    void getFlowersByIdsShouldPostJsonArrayBody() {
        server.expect(requestTo(BASE_URL + "/internal/flower/batch"))
                .andExpect(method(org.springframework.http.HttpMethod.POST))
                .andExpect(jsonPath("$[0]").value(1))
                .andExpect(jsonPath("$[1]").value(2))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("[{\"id\":1,\"name\":\"红玫瑰\"},{\"id\":2,\"name\":\"百合\"}]"));

        List<FlowerDTO> result = client.getFlowersByIds(List.of(1, 2));

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getId()).isEqualTo(1);
        assertThat(result.get(0).getName()).isEqualTo("红玫瑰");
        assertThat(result.get(1).getId()).isEqualTo(2);
        server.verify();
    }

    // getFlowerIdsByName：GET /internal/flower/ids-by-name?name=rose，query 参数正确
    @Test
    void getFlowerIdsByNameShouldPassQueryParam() {
        server.expect(requestTo(BASE_URL + "/internal/flower/ids-by-name?name=rose"))
                .andExpect(method(org.springframework.http.HttpMethod.GET))
                .andExpect(queryParam("name", "rose"))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("[1,3,5]"));

        List<Integer> result = client.getFlowerIdsByName("rose");

        assertThat(result).containsExactly(1, 3, 5);
        server.verify();
    }

    // adjustStock 成功：POST /internal/flower/stock/adjust，ResponseEntity 状态码 + body 解析
    @Test
    void adjustStockShouldParseResponseEntity() {
        StockAdjustRequest request = new StockAdjustRequest();
        request.setFlowerId(1);
        request.setQuantity(-100);
        request.setCostPrice(new BigDecimal("5.00"));
        request.setUpdateCostPrice(false);

        server.expect(requestTo(BASE_URL + "/internal/flower/stock/adjust"))
                .andExpect(method(org.springframework.http.HttpMethod.POST))
                .andExpect(jsonPath("$.flowerId").value(1))
                .andExpect(jsonPath("$.quantity").value(-100))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"success\":true,\"beforeStock\":120,\"currentStock\":20,\"flower\":{\"id\":1,\"name\":\"红玫瑰\"},\"message\":null}"));

        ResponseEntity<StockAdjustResult> response = client.adjustStock(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        StockAdjustResult body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.getSuccess()).isTrue();
        assertThat(body.getBeforeStock()).isEqualTo(120);
        assertThat(body.getCurrentStock()).isEqualTo(20);
        assertThat(body.getFlower().getName()).isEqualTo("红玫瑰");
        server.verify();
    }

    // adjustStock 业务失败：provider 返回 HTTP 422 + success=false body。
    //
    // 注意：RestClient 默认对 4xx 抛 HttpClientErrorException，不会进入 ResponseEntity。
    // 这与 FlowerClient.adjustStock() 的注释"422 不会抛异常"存在漂移——provider 端设计
    // 意图是 422 带 body 让 consumer 读 success=false，但 consumer 的 RestClient 未配
    // 自定义 status handler，实际会抛 HttpClientErrorException$UnprocessableEntity。
    // RemoteProductStockFacade 的断路器降级逻辑因匹配不到 BusinessException，会把
    // 库存不足误判为"商品服务暂时不可用"。这是已知遗留问题，本契约测试只锁定当前
    // 真实行为（422 抛异常），不修改行为。
    @Test
    void adjustStockShouldThrowOn422BusinessFailure() {
        StockAdjustRequest request = new StockAdjustRequest();
        request.setFlowerId(1);
        request.setQuantity(-9999);

        server.expect(requestTo(BASE_URL + "/internal/flower/stock/adjust"))
                .andRespond(withStatus(HttpStatus.UNPROCESSABLE_ENTITY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"success\":false,\"message\":\"红玫瑰库存不足\"}"));

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> client.adjustStock(request))
                .isInstanceOf(org.springframework.web.client.HttpClientErrorException.class)
                .hasMessageContaining("422")
                .hasMessageContaining("红玫瑰库存不足");
        server.verify();
    }
}
