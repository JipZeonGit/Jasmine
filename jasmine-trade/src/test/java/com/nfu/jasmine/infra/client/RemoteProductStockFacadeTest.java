package com.nfu.jasmine.infra.client;

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
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.util.function.Function;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RemoteProductStockFacadeTest {

    @Mock
    private FlowerClient flowerClient;

    @Mock
    private CircuitBreakerFactory circuitBreakerFactory;

    @Mock
    private CircuitBreaker circuitBreaker;

    private RemoteProductStockFacade facade;

    @BeforeEach
    void setUp() {
        // 断路器直通：直接执行 supplier，不包装降级
        when(circuitBreakerFactory.create(any())).thenReturn(circuitBreaker);
        when(circuitBreaker.run(any(Supplier.class), any(Function.class)))
                .thenAnswer(invocation -> {
                    Supplier<?> supplier = invocation.getArgument(0);
                    return supplier.get();
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
        StockAdjustResult failResult = StockAdjustResult.fail("库存不足");
        when(flowerClient.adjustStock(any())).thenReturn(ResponseEntity.ok(failResult));

        assertThatThrownBy(() -> facade.adjustStock(1, -100, null, false, null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("库存不足");
    }

    @Test
    void adjustStockShouldThrowWhen422Response() {
        StockAdjustResult failResult = StockAdjustResult.fail("库存不足");
        when(flowerClient.adjustStock(any()))
                .thenReturn(ResponseEntity.status(422).body(failResult));

        assertThatThrownBy(() -> facade.adjustStock(1, -100, null, false, null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("库存不足");
    }

    @Test
    void adjustStockShouldThrowWhenNullResult() {
        when(flowerClient.adjustStock(any())).thenReturn(ResponseEntity.ok(null));

        assertThatThrownBy(() -> facade.adjustStock(1, -5, null, false, null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("库存调整失败");
    }
}
