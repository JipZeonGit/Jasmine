package com.nfu.jasmine.flower.application.support;

import com.nfu.jasmine.common.dto.internal.FlowerDTO;
import com.nfu.jasmine.common.dto.internal.ProductStockFacade;
import com.nfu.jasmine.common.enums.ResultCode;
import com.nfu.jasmine.common.exception.BusinessException;
import com.nfu.jasmine.flower.model.entity.Flower;
import com.nfu.jasmine.flower.persistence.mapper.FlowerMapper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * 花卉库存原子变更服务。
 * <p>
 * 所有库存增减最终都收口到 compare-and-set SQL：先读取当前库存，再按预期库存做原子更新，
 * 更新失败说明并发期间库存已被别的事务改动，此时重试读取并重新计算，避免丢失更新。
 */
@Service
public class FlowerStockService implements ProductStockFacade {
    private static final int MAX_RETRY_TIMES = 8;

    private final FlowerMapper flowerMapper;

    public FlowerStockService(FlowerMapper flowerMapper) {
        this.flowerMapper = flowerMapper;
    }

    @Override
    public ProductStockFacade.StockChangeResult adjustStock(Integer flowerId, int delta, BigDecimal costPrice, boolean updateCostPrice, String insufficientMessage) {
        for (int attempt = 0; attempt < MAX_RETRY_TIMES; attempt++) {
            Flower flower = requireFlower(flowerId);
            int beforeStock = safeStock(flower);
            int afterStock = beforeStock + delta;
            if (afterStock < 0) {
                throw new BusinessException(ResultCode.CONFLICT, insufficientMessage == null ? flower.getName() + "库存不足，请稍后重试！" : insufficientMessage);
            }

            int updated = flowerMapper.compareAndSetStock(
                    flowerId,
                    beforeStock,
                    afterStock,
                    costPrice,
                    updateCostPrice
            );
            if (updated == 1) {
                flower.setCurrentStock(afterStock);
                if (updateCostPrice) {
                    flower.setCostPrice(costPrice);
                }
                return new ProductStockFacade.StockChangeResult(toFlowerDTO(flower), beforeStock, afterStock);
            }
        }
        throw new BusinessException(ResultCode.CONFLICT, "库存正在被其他请求更新，请稍后重试！");
    }

    private Flower requireFlower(Integer flowerId) {
        Flower flower = flowerMapper.selectById(flowerId);
        if (flower == null || Integer.valueOf(1).equals(flower.getDeleted())) {
            throw new BusinessException(ResultCode.NOT_FOUND, "花卉不存在！");
        }
        return flower;
    }

    private int safeStock(Flower flower) {
        return flower.getCurrentStock() == null ? 0 : flower.getCurrentStock();
    }

    private FlowerDTO toFlowerDTO(Flower flower) {
        FlowerDTO dto = new FlowerDTO();
        dto.setId(flower.getId());
        dto.setName(flower.getName());
        dto.setPrice(flower.getSalePrice());
        dto.setCost(flower.getCostPrice());
        dto.setStatus(flower.getStatus());
        return dto;
    }
}
