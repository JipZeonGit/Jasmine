package com.nfu.jasmine.infra.client;

import com.nfu.jasmine.common.dto.internal.FlowerDTO;
import com.nfu.jasmine.common.dto.internal.ProductStockFacade;
import com.nfu.jasmine.common.dto.internal.StockAdjustRequest;
import com.nfu.jasmine.common.dto.internal.StockAdjustResult;
import com.nfu.jasmine.common.exception.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.client.circuitbreaker.CircuitBreaker;
import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * 远程商品库存门面实现 —— 通过 HTTP 调用 product-service 内部接口。
 * <p>
 * 使用 Resilience4j 断路器保护远程调用，当 product-service 持续不可用时快速失败，
 * 避免级联故障拖垮 trade-service。业务异常（库存不足等）直接透传，不触发降级。
 */
@Component
public class RemoteProductStockFacade implements ProductStockFacade {

    private static final Logger log = LoggerFactory.getLogger(RemoteProductStockFacade.class);

    private final FlowerClient flowerClient;
    private final CircuitBreakerFactory circuitBreakerFactory;

    public RemoteProductStockFacade(FlowerClient flowerClient, CircuitBreakerFactory circuitBreakerFactory) {
        this.flowerClient = flowerClient;
        this.circuitBreakerFactory = circuitBreakerFactory;
    }

    @Override
    public StockChangeResult adjustStock(Integer flowerId, int delta, BigDecimal costPrice,
                                         boolean updateCostPrice, String insufficientMessage) {
        CircuitBreaker cb = circuitBreakerFactory.create("productStock");
        return cb.run(
                () -> doAdjustStock(flowerId, delta, costPrice, updateCostPrice, insufficientMessage),
                throwable -> {
                    // 业务异常透传，断路器只为真正的"服务不可用"降级
                    if (throwable instanceof BusinessException be) {
                        throw be;
                    }
                    log.error("商品服务调用失败，断路器降级 flowerId={} delta={}", flowerId, delta, throwable);
                    throw new BusinessException("商品服务暂时不可用，请稍后重试");
                }
        );
    }

    private StockChangeResult doAdjustStock(Integer flowerId, int delta, BigDecimal costPrice,
                                            boolean updateCostPrice, String insufficientMessage) {
        StockAdjustRequest request = new StockAdjustRequest();
        request.setFlowerId(flowerId);
        request.setQuantity(delta);
        request.setCostPrice(costPrice);
        request.setUpdateCostPrice(updateCostPrice);
        request.setReason(insufficientMessage);

        StockAdjustResult result = flowerClient.adjustStock(request).getBody();

        if (result == null || !Boolean.TRUE.equals(result.getSuccess())) {
            // 4xx 业务失败：库存不足、参数非法等，直接抛出业务异常
            String msg = result != null && result.getMessage() != null ? result.getMessage() : "库存调整失败！";
            throw new BusinessException(msg);
        }

        // 服务端在响应中直接带回完整花卉信息与 beforeStock，避免再发一次请求
        FlowerDTO flowerDTO = result.getFlower();
        return new StockChangeResult(flowerDTO, result.getBeforeStock(), result.getCurrentStock());
    }
}
