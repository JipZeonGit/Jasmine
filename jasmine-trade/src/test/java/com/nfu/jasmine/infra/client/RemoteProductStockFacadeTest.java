package com.nfu.jasmine.infra.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nfu.jasmine.common.dto.internal.FlowerDTO;
import com.nfu.jasmine.common.dto.internal.ProductStockFacade;
import com.nfu.jasmine.common.dto.internal.StockAdjustResult;
import com.nfu.jasmine.common.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.client.circuitbreaker.CircuitBreaker;
import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory;
import org.springframework.core.ResolvableType;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.function.Function;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RemoteProductStockFacadeTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Mock
    private FlowerClient flowerClient;

    @Mock
    private CircuitBreakerFactory circuitBreakerFactory;

    @Mock
    private CircuitBreaker circuitBreaker;

    private RemoteProductStockFacade facade;

    @BeforeEach
    void setUp() {
        when(circuitBreakerFactory.create(any())).thenReturn(circuitBreaker);
        // 模拟真实 Resilience4j 行为：supplier 正常返回结果；抛异常时走 fallback。
        // fallback 对 BusinessException 透传，对其他异常降级为"商品服务暂时不可用"。
        when(circuitBreaker.run(any(Supplier.class), any(Function.class)))
                .thenAnswer(invocation -> {
                    Supplier<?> supplier = invocation.getArgument(0);
                    Function<Throwable, ?> fallback = invocation.getArgument(1);
                    try {
                        return supplier.get();
                    } catch (Throwable t) {
                        return fallback.apply(t);
                    }
                });
        facade = new RemoteProductStockFacade(flowerClient, circuitBreakerFactory);
    }

    @Test
    void adjustStockShouldReturnSuccessResult() {
        FlowerDTO flowerDTO = new FlowerDTO();
        flowerDTO.setId(1);
        flowerDTO.setName("红玫瑰");
        flowerDTO.setPrice(new BigDecimal("13.00"));
        flowerDTO.setCost(new BigDecimal("10.00"));

        StockAdjustResult adjustResult = StockAdjustResult.ok(50, 45, flowerDTO);
        when(flowerClient.adjustStock(any())).thenReturn(ResponseEntity.ok(adjustResult));

        ProductStockFacade.StockChangeResult result = facade.adjustStock(1, -5, null, false, null);

        assertThat(result.beforeStock()).isEqualTo(50);
        assertThat(result.afterStock()).isEqualTo(45);
        assertThat(result.flower().getName()).isEqualTo("红玫瑰");
    }

    @Test
    void adjustStockShouldThrowWhenBusinessFailure() {
        // 防御性兜底：provider 返回 200 但 success=false（当前契约不会出现，保留以防回归）
        StockAdjustResult failResult = StockAdjustResult.fail("库存不足");
        when(flowerClient.adjustStock(any())).thenReturn(ResponseEntity.ok(failResult));

        assertThatThrownBy(() -> facade.adjustStock(1, -100, null, false, null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("库存不足");
    }

    /**
     * 422 + StockAdjustResult.fail body 是 provider 端对库存不足等业务失败的标准契约。
     * RestClient 默认对 4xx 抛 HttpClientErrorException，根本不会进入 ResponseEntity 分支。
     * 之前的错误测试 mock 成 ResponseEntity.status(422) 直接返回，掩盖了真实行为。
     * 此测试用真实的 HttpClientErrorException 验证 Facade 层的捕获与转换逻辑。
     * <p>
     * 注意：手搓 HttpClientErrorException 不会自动注入 bodyConvertFunction（生产环境
     * 由 RestClient 错误处理器注入），这里手动设置以模拟生产行为。
     */
    @Test
    void adjustStockShouldConvert422ToBusinessException() {
        HttpClientErrorException exception = create422Exception(
                "{\"success\":false,\"message\":\"红玫瑰库存不足\"}");

        when(flowerClient.adjustStock(any())).thenThrow(exception);

        assertThatThrownBy(() -> facade.adjustStock(1, -100, null, false, null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("红玫瑰库存不足");
    }

    /**
     * 关键回归点：422 必须透传业务消息，不能被断路器 fallback 降级为"商品服务暂时不可用"。
     * 否则用户卖超库存时收到的就是"服务暂时不可用"而非"红玫瑰库存不足"，
     * 且断路器会错误累计失败次数，频繁的库存不足可能误触发熔断。
     */
    @Test
    void adjustStock422ShouldNotTriggerCircuitBreakerFallback() {
        HttpClientErrorException exception = create422Exception(
                "{\"success\":false,\"message\":\"红玫瑰库存不足\"}");

        when(flowerClient.adjustStock(any())).thenThrow(exception);

        assertThatThrownBy(() -> facade.adjustStock(1, -100, null, false, null))
                .isInstanceOf(BusinessException.class)
                .hasMessageNotContaining("商品服务暂时不可用");
    }

    /**
     * 422 body 不可解析（如空 body 或非 JSON）时，仍应抛 BusinessException，
     * 使用兜底文案"库存调整失败！"，不暴露技术异常给前端。
     */
    @Test
    void adjustStock422ShouldFallbackMessageWhenBodyUnparseable() {
        // 空 body：触发 getResponseBodyAsByteArray 为空 → 兜底文案
        HttpClientErrorException exception = HttpClientErrorException.create(
                HttpStatus.UNPROCESSABLE_ENTITY, "库存不足",
                null, new byte[0], null);

        when(flowerClient.adjustStock(any())).thenThrow(exception);

        assertThatThrownBy(() -> facade.adjustStock(1, -100, null, false, null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("库存调整失败");
    }

    /**
     * 其他 4xx（如 404 花卉不存在）不属于业务失败，应原样抛出 HttpClientErrorException，
     * 让断路器 fallback 降级为"商品服务暂时不可用"。
     * 这与 provider 端的 422 语义区分一致。
     */
    @Test
    void adjustStockNon422ClientErrorShouldTriggerFallback() {
        HttpClientErrorException exception = HttpClientErrorException.create(
                HttpStatus.NOT_FOUND, "Not Found",
                null, new byte[0], null);

        when(flowerClient.adjustStock(any())).thenThrow(exception);

        assertThatThrownBy(() -> facade.adjustStock(1, -5, null, false, null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("商品服务暂时不可用");
    }

    @Test
    void adjustStockShouldThrowWhenNullResult() {
        when(flowerClient.adjustStock(any())).thenReturn(ResponseEntity.ok(null));

        assertThatThrownBy(() -> facade.adjustStock(1, -5, null, false, null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("库存调整失败");
    }

    /**
     * 构造一个 422 HttpClientErrorException，并手动注入 bodyConvertFunction。
     * 生产环境 RestClient 错误处理器会自动注入该函数，这里用 Jackson 复制该行为，
     * 让 {@link HttpClientErrorException#getResponseBodyAs} 在测试中也能正常工作。
     */
    private HttpClientErrorException create422Exception(String bodyJson) {
        byte[] body = bodyJson.getBytes(StandardCharsets.UTF_8);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpClientErrorException exception = HttpClientErrorException.create(
                HttpStatus.UNPROCESSABLE_ENTITY, "Unprocessable Entity", headers, body, StandardCharsets.UTF_8);
        exception.setBodyConvertFunction(resolvableType -> {
            try {
                return OBJECT_MAPPER.readValue(body,
                        OBJECT_MAPPER.constructType(resolvableType.getType()));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
        return exception;
    }
}

