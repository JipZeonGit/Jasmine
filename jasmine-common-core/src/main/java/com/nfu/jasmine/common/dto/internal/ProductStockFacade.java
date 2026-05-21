package com.nfu.jasmine.common.dto.internal;

import java.math.BigDecimal;

/**
 * 商品库存内部门面。
 * <p>
 * 定义在 common-core，trade-service 和 product-service 共享此接口。
 * trade-service 通过远程实现调用 product-service，product-service 通过本地实现操作数据库。
 */
public interface ProductStockFacade {

	StockChangeResult adjustStock(Integer flowerId, int delta, BigDecimal costPrice, boolean updateCostPrice, String insufficientMessage);

	/**
	 * 库存变更结果，使用 FlowerDTO 而非 Flower 实体，解耦 trade 对 product 的编译期依赖。
	 */
	record StockChangeResult(FlowerDTO flower, int beforeStock, int afterStock) {
	}
}
