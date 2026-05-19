package com.nfu.jasmine.flower.application.support;

import com.nfu.jasmine.flower.model.entity.Flower;

import java.math.BigDecimal;

/**
 * 商品库存内部门面。
 * <p>
 * Phase3 远程化时，交易服务只需要替换该门面的实现，不直接耦合库存服务实现类。
 */
public interface ProductStockFacade {

	StockChangeResult adjustStock(Integer flowerId, int delta, BigDecimal costPrice, boolean updateCostPrice, String insufficientMessage);

	record StockChangeResult(Flower flower, int beforeStock, int afterStock) {
	}
}
