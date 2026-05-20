package com.nfu.jasmine.infra.client;

import com.nfu.jasmine.common.dto.internal.StockAdjustRequest;
import com.nfu.jasmine.common.dto.internal.StockAdjustResult;
import com.nfu.jasmine.common.exception.BusinessException;
import com.nfu.jasmine.flower.application.support.ProductStockFacade;
import com.nfu.jasmine.flower.model.entity.Flower;
import org.springframework.cloud.client.circuitbreaker.CircuitBreaker;
import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * 远程商品库存门面实现 —— 通过 HTTP 调用 product-service 内部接口。
 * <p>
 * 标记 @Primary 使其在 trade-service 中优先于 jasmine-product 模块内的本地实现。
 * 后续移除 jasmine-product 依赖后可去掉 @Primary。
 * <p>
 * 使用 Resilience4j 断路器保护远程调用，当 product-service 持续不可用时快速失败，
 * 避免级联故障拖垮 trade-service。
 */
@Primary
@Component
public class RemoteProductStockFacade implements ProductStockFacade {

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

        StockAdjustResult result = flowerClient.adjustStock(request);

        if (!Boolean.TRUE.equals(result.getSuccess())) {
            throw new BusinessException(result.getMessage() != null ? result.getMessage() : "库存调整失败！");
        }

        // 远程调用后需要获取完整花卉信息来构造 StockChangeResult
        var flowerDTO = flowerClient.getFlowerById(flowerId);
        Flower flower = new Flower();
        flower.setId(flowerDTO.getId());
        flower.setName(flowerDTO.getName());
        flower.setSalePrice(flowerDTO.getPrice());
        flower.setCostPrice(flowerDTO.getCost());
        flower.setStatus(flowerDTO.getStatus());
        flower.setCurrentStock(result.getCurrentStock());

        // 远程调用无法精确获取 beforeStock，通过 delta 反推
        int afterStock = result.getCurrentStock();
        int beforeStock = afterStock - delta;

        return new StockChangeResult(flower, beforeStock, afterStock);
    }
}
