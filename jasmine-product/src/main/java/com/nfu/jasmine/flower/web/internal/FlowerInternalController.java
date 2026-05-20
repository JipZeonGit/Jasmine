package com.nfu.jasmine.flower.web.internal;

import com.nfu.jasmine.common.dto.internal.FlowerDTO;
import com.nfu.jasmine.common.dto.internal.StockAdjustRequest;
import com.nfu.jasmine.common.dto.internal.StockAdjustResult;
import com.nfu.jasmine.common.enums.ResultCode;
import com.nfu.jasmine.common.exception.BusinessException;
import com.nfu.jasmine.flower.application.IFlowerService;
import com.nfu.jasmine.flower.application.support.ProductStockFacade;
import com.nfu.jasmine.flower.model.entity.Flower;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

/**
 * 花卉内部接口 —— 仅供服务间调用，网关层拦截外部访问。
 */
@Tag(name = "内部接口")
@RestController
@RequestMapping("/internal/flower")
public class FlowerInternalController {

    private final IFlowerService flowerService;
    private final ProductStockFacade productStockFacade;

    public FlowerInternalController(IFlowerService flowerService, ProductStockFacade productStockFacade) {
        this.flowerService = flowerService;
        this.productStockFacade = productStockFacade;
    }

    @Operation(summary = "根据ID查询花卉基本信息（内部）")
    @GetMapping("/{id}")
    public FlowerDTO getFlowerById(@PathVariable Integer id) {
        Flower flower = flowerService.getById(id);
        if (flower == null || Integer.valueOf(1).equals(flower.getDeleted())) {
            throw new BusinessException(ResultCode.NOT_FOUND, "花卉不存在！");
        }
        return toFlowerDTO(flower);
    }

    @Operation(summary = "库存调整（内部）")
    @PostMapping("/stock/adjust")
    public StockAdjustResult adjustStock(@RequestBody StockAdjustRequest request) {
        try {
            ProductStockFacade.StockChangeResult result = productStockFacade.adjustStock(
                    request.getFlowerId(),
                    request.getQuantity(),
                    request.getCostPrice(),
                    Boolean.TRUE.equals(request.getUpdateCostPrice()),
                    request.getReason()
            );
            return StockAdjustResult.ok(result.afterStock());
        } catch (BusinessException e) {
            return StockAdjustResult.fail(e.getMessage());
        }
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
