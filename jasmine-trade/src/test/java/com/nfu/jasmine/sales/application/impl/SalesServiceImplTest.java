package com.nfu.jasmine.sales.application.impl;

import com.nfu.jasmine.common.dto.internal.FlowerDTO;
import com.nfu.jasmine.common.dto.internal.ProductStockFacade;
import com.nfu.jasmine.common.dto.internal.VipBasicDTO;
import com.nfu.jasmine.common.exception.BusinessException;
import com.nfu.jasmine.infra.client.FlowerClient;
import com.nfu.jasmine.infra.client.VipClient;
import com.nfu.jasmine.infra.mq.publisher.MqMessagePublisher;
import com.nfu.jasmine.inventory.model.entity.Inventory;
import com.nfu.jasmine.inventory.persistence.mapper.InventoryMapper;
import com.nfu.jasmine.inventory.persistence.mapper.SalesItemMapper;
import com.nfu.jasmine.inventory.persistence.mapper.SalesMapper;
import com.nfu.jasmine.sales.model.entity.Sales;
import com.nfu.jasmine.sales.model.entity.SalesItem;
import com.nfu.jasmine.sales.web.dto.SalesItemSaveDTO;
import com.nfu.jasmine.sales.web.dto.SalesSaveDTO;
import com.nfu.jasmine.sales.web.vo.SalesVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SalesServiceImplTest {

    @Mock
    private SalesMapper salesMapper;
    @Mock
    private SalesItemMapper salesItemMapper;
    @Mock
    private InventoryMapper inventoryMapper;
    @Mock
    private FlowerClient flowerClient;
    @Mock
    private VipClient vipClient;
    @Mock
    private MqMessagePublisher mqMessagePublisher;
    @Mock
    private ProductStockFacade productStockFacade;

    @InjectMocks
    private SalesServiceImpl salesService;

    @Test
    void saveSalesShouldDeductStockAndCreateRecords() {
        FlowerDTO flower = new FlowerDTO();
        flower.setId(1);
        flower.setName("红玫瑰");
        flower.setCost(new BigDecimal("10.00"));

        ProductStockFacade.StockChangeResult stockChange =
                new ProductStockFacade.StockChangeResult(flower, 50, 45);

        when(productStockFacade.adjustStock(1, -5, null, false, "库存不足，请调整销售数量！"))
                .thenReturn(stockChange);

        SalesItemSaveDTO item = new SalesItemSaveDTO();
        item.setFlowerId(1);
        item.setQuantity(5);
        item.setUnitPrice(new BigDecimal("13.00"));

        SalesSaveDTO dto = new SalesSaveDTO();
        dto.setVipId(1);
        dto.setDate(new Date());
        dto.setRemark("测试");
        dto.setItems(List.of(item));

        when(vipClient.getVipById(1)).thenReturn(new VipBasicDTO());
        when(salesMapper.insert(any(Sales.class))).thenReturn(1);
        when(salesItemMapper.insert(any(SalesItem.class))).thenReturn(1);
        when(inventoryMapper.insert(any(Inventory.class))).thenReturn(1);

        salesService.saveSales(dto, 1);

        verify(productStockFacade).adjustStock(1, -5, null, false, "库存不足，请调整销售数量！");
        verify(mqMessagePublisher).publishSalesCreatedAfterCommit(any());
        verify(mqMessagePublisher).publishInventoryChangedAfterCommit(any());
    }

    @Test
    void saveSalesShouldThrowWhenItemsEmpty() {
        SalesSaveDTO dto = new SalesSaveDTO();
        dto.setVipId(1);
        dto.setDate(new Date());
        dto.setItems(Collections.emptyList());

        assertThatThrownBy(() -> salesService.saveSales(dto, 1))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("销售明细不能为空");
    }

    @Test
    void saveSalesShouldThrowWhenVipNotFound() {
        when(vipClient.getVipById(999)).thenThrow(new BusinessException("会员不存在"));

        SalesItemSaveDTO item = new SalesItemSaveDTO();
        item.setFlowerId(1);
        item.setQuantity(5);
        item.setUnitPrice(new BigDecimal("13.00"));

        SalesSaveDTO dto = new SalesSaveDTO();
        dto.setVipId(999);
        dto.setDate(new Date());
        dto.setItems(List.of(item));

        assertThatThrownBy(() -> salesService.saveSales(dto, 1))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("会员不存在");
    }

    @Test
    void saveSalesShouldWorkWithoutVip() {
        FlowerDTO flower = new FlowerDTO();
        flower.setId(1);
        flower.setName("红玫瑰");
        flower.setCost(new BigDecimal("10.00"));

        ProductStockFacade.StockChangeResult stockChange =
                new ProductStockFacade.StockChangeResult(flower, 50, 45);

        when(productStockFacade.adjustStock(1, -5, null, false, "库存不足，请调整销售数量！"))
                .thenReturn(stockChange);
        when(salesMapper.insert(any(Sales.class))).thenReturn(1);
        when(salesItemMapper.insert(any(SalesItem.class))).thenReturn(1);
        when(inventoryMapper.insert(any(Inventory.class))).thenReturn(1);

        SalesItemSaveDTO item = new SalesItemSaveDTO();
        item.setFlowerId(1);
        item.setQuantity(5);
        item.setUnitPrice(new BigDecimal("13.00"));

        SalesSaveDTO dto = new SalesSaveDTO();
        dto.setVipId(null);
        dto.setDate(new Date());
        dto.setItems(List.of(item));

        salesService.saveSales(dto, 1);

        verify(productStockFacade).adjustStock(1, -5, null, false, "库存不足，请调整销售数量！");
    }

    @Test
    void getTodayBusinessSummaryShouldReturnSummary() {
        when(salesMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(Collections.emptyList());
        when(salesItemMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(Collections.emptyList());
        when(inventoryMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(Collections.emptyList());

        var summary = salesService.getTodayBusinessSummary();

        assertThat(summary).isNotNull();
        assertThat(summary.getTodaySalesAmount()).isEqualByComparingTo(BigDecimal.ZERO);
    }
}
