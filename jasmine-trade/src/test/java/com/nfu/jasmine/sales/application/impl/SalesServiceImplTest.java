package com.nfu.jasmine.sales.application.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.nfu.jasmine.common.dto.internal.FlowerDTO;
import com.nfu.jasmine.common.dto.internal.ProductStockFacade;
import com.nfu.jasmine.common.dto.internal.VipBasicDTO;
import com.nfu.jasmine.common.exception.BusinessException;
import com.nfu.jasmine.infra.client.FlowerClient;
import com.nfu.jasmine.infra.client.VipClient;
import com.nfu.jasmine.infra.mq.publisher.MqMessagePublisher;
import com.nfu.jasmine.inventory.model.entity.Inventory;
import com.nfu.jasmine.inventory.persistence.mapper.InventoryMapper;
import com.nfu.jasmine.sales.model.entity.Sales;
import com.nfu.jasmine.sales.model.entity.SalesItem;
import com.nfu.jasmine.sales.persistence.mapper.SalesItemMapper;
import com.nfu.jasmine.sales.persistence.mapper.SalesMapper;
import com.nfu.jasmine.sales.web.dto.SalesItemSaveDTO;
import com.nfu.jasmine.sales.web.dto.SalesSaveDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
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

    @BeforeEach
    void setUp() {
        // ServiceImpl 需要通过反射注入 baseMapper
        ReflectionTestUtils.setField(salesService, "baseMapper", salesMapper);
    }

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
        when(vipClient.getVipById(1)).thenReturn(new VipBasicDTO());
        when(salesMapper.insert(any(Sales.class))).thenReturn(1);
        when(salesItemMapper.insert(any(SalesItem.class))).thenReturn(1);
        when(inventoryMapper.insert(any(Inventory.class))).thenReturn(1);

        SalesItemSaveDTO item = new SalesItemSaveDTO();
        item.setFlowerId(1);
        item.setQuantity(5);
        item.setUnitPrice(new BigDecimal("13.00"));

        SalesSaveDTO dto = new SalesSaveDTO();
        dto.setVipId(1);
        dto.setDate(new Date());
        dto.setRemark("测试");
        dto.setItems(List.of(item));

        salesService.saveSales(dto, 1);

        verify(productStockFacade).adjustStock(1, -5, null, false, "库存不足，请调整销售数量！");
        verify(mqMessagePublisher).publishSalesCreatedAfterCommit(any());
        verify(mqMessagePublisher).publishInventoryChangedAfterCommit(any());
    }

    @Test
    void saveSalesShouldThrowWhenItemsEmpty() {
        SalesSaveDTO dto = new SalesSaveDTO();
        dto.setVipId(null);
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

    // ==================== updateSales ====================

    /**
     * updateSales 采用"先回滚再重建"策略：
     * 1. restoreSales 把旧明细的库存加回去（adjustStock +quantity）
     * 2. 删除旧明细和旧库存流水
     * 3. rebuildSalesItems 按新明细重新扣减库存
     * 4. 为旧库存流水发 ROLLBACK 事件
     */
    @Test
    void updateSalesShouldRollbackOldItemsAndRebuildNew() {
        Sales existing = new Sales();
        existing.setId(10);
        existing.setOrderNo("ORD10");
        existing.setVipId(null);
        existing.setDate(new Date());
        when(salesMapper.selectById(10)).thenReturn(existing);

        // 旧明细：1 朵红玫瑰
        SalesItem oldItem = new SalesItem();
        oldItem.setId(100);
        oldItem.setSalesId(10);
        oldItem.setFlowerId(1);
        oldItem.setQuantity(5);
        when(salesItemMapper.selectList(any())).thenReturn(List.of(oldItem));

        // 旧库存流水（用于发 ROLLBACK 事件）
        Inventory oldInventory = new Inventory();
        oldInventory.setId(200);
        oldInventory.setBizNo("ORD10");
        oldInventory.setFlowerId(1);
        oldInventory.setBizType("SALE_OUT");
        oldInventory.setQuantity(5);
        oldInventory.setBeforeStock(50);
        oldInventory.setAfterStock(45);
        // inventoryMapper.selectList 被调两次：一次在 restoreSales 删除前（实际不查），
        // 一次在 updateSales 中查旧流水发 ROLLBACK 事件。这里统一返回。
        when(inventoryMapper.selectList(any())).thenReturn(List.of(oldInventory));

        // 新明细：2 朵红玫瑰
        FlowerDTO flower = new FlowerDTO();
        flower.setId(1);
        flower.setName("红玫瑰");
        flower.setCost(new BigDecimal("10.00"));

        // restoreSales 调用：adjustStock(1, +5, ...) 回滚旧库存
        // rebuildSalesItems 调用：adjustStock(1, -2, ...) 扣减新库存
        when(productStockFacade.adjustStock(eq(1), eq(5), any(), eq(false), any()))
                .thenReturn(new ProductStockFacade.StockChangeResult(flower, 45, 50));
        when(productStockFacade.adjustStock(eq(1), eq(-2), any(), eq(false), any()))
                .thenReturn(new ProductStockFacade.StockChangeResult(flower, 50, 48));

        when(salesItemMapper.insert(any(SalesItem.class))).thenReturn(1);
        when(inventoryMapper.insert(any(Inventory.class))).thenReturn(1);

        SalesItemSaveDTO newItem = new SalesItemSaveDTO();
        newItem.setFlowerId(1);
        newItem.setQuantity(2);
        newItem.setUnitPrice(new BigDecimal("13.00"));

        SalesSaveDTO dto = new SalesSaveDTO();
        dto.setId(10);
        dto.setVipId(null);
        dto.setDate(new Date());
        dto.setItems(List.of(newItem));

        salesService.updateSales(dto, 99);

        // 验证回滚旧库存：adjustStock(1, +5, ...)
        verify(productStockFacade).adjustStock(eq(1), eq(5), any(), eq(false), any());
        // 验证扣减新库存：adjustStock(1, -2, ...)
        verify(productStockFacade).adjustStock(eq(1), eq(-2), any(), eq(false), any());
        // 验证删除旧明细和旧库存流水
        verify(salesItemMapper).delete(any());
        verify(inventoryMapper).delete(any());
        // 验证更新销售单
        ArgumentCaptor<Sales> salesCaptor = ArgumentCaptor.forClass(Sales.class);
        verify(salesMapper).updateById(salesCaptor.capture());
        assertThat(salesCaptor.getValue().getTotalAmount())
                .isEqualByComparingTo("26.00"); // 13.00 * 2
        assertThat(salesCaptor.getValue().getOperatorId()).isEqualTo(99);
        // 验证 MQ 事件：1 次 ROLLBACK（旧流水回滚）+ 1 次 CREATE（新明细出库）= 2 次
        verify(mqMessagePublisher, org.mockito.Mockito.times(2))
                .publishInventoryChangedAfterCommit(any());
    }

    @Test
    void updateSalesShouldThrowWhenSalesNotFound() {
        SalesSaveDTO dto = new SalesSaveDTO();
        dto.setId(999);
        dto.setDate(new Date());
        dto.setItems(List.of());
        when(salesMapper.selectById(999)).thenReturn(null);

        assertThatThrownBy(() -> salesService.updateSales(dto, 1))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("销售单不存在");
    }

    @Test
    void updateSalesShouldThrowWhenItemsEmpty() {
        Sales existing = new Sales();
        existing.setId(10);
        existing.setOrderNo("ORD10");
        existing.setDate(new Date());
        when(salesMapper.selectById(10)).thenReturn(existing);
        when(salesItemMapper.selectList(any())).thenReturn(List.of());

        SalesSaveDTO dto = new SalesSaveDTO();
        dto.setId(10);
        dto.setVipId(null);
        dto.setDate(new Date());
        dto.setItems(Collections.emptyList());

        assertThatThrownBy(() -> salesService.updateSales(dto, 1))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("销售明细不能为空");
    }

    // ==================== deleteSales ====================

    @Test
    void deleteSalesShouldRestoreStockAndRemoveRecords() {
        Sales existing = new Sales();
        existing.setId(10);
        existing.setOrderNo("ORD10");
        existing.setDate(new Date());
        when(salesMapper.selectById(10)).thenReturn(existing);

        // 旧明细：5 朵红玫瑰
        SalesItem oldItem = new SalesItem();
        oldItem.setId(100);
        oldItem.setSalesId(10);
        oldItem.setFlowerId(1);
        oldItem.setQuantity(5);
        when(salesItemMapper.selectList(any())).thenReturn(List.of(oldItem));

        // 旧库存流水（用于发 ROLLBACK 事件）
        Inventory oldInventory = new Inventory();
        oldInventory.setId(200);
        oldInventory.setBizNo("ORD10");
        oldInventory.setFlowerId(1);
        oldInventory.setBizType("SALE_OUT");
        oldInventory.setQuantity(5);
        oldInventory.setBeforeStock(50);
        oldInventory.setAfterStock(45);
        when(inventoryMapper.selectList(any())).thenReturn(List.of(oldInventory));

        // restoreSales 调用：adjustStock(1, +5, ...) 回滚库存
        FlowerDTO flower = new FlowerDTO();
        flower.setId(1);
        flower.setName("红玫瑰");
        when(productStockFacade.adjustStock(eq(1), eq(5), any(), eq(false), any()))
                .thenReturn(new ProductStockFacade.StockChangeResult(flower, 45, 50));

        salesService.deleteSales(10);

        // 验证回滚库存
        verify(productStockFacade).adjustStock(eq(1), eq(5), any(), eq(false), any());
        // 验证删除旧明细和旧库存流水
        verify(salesItemMapper).delete(any());
        verify(inventoryMapper).delete(any());
        // 验证删除销售单
        verify(salesMapper).deleteById(10);
        // 验证为旧库存流水发 ROLLBACK 事件
        verify(mqMessagePublisher).publishInventoryChangedAfterCommit(any());
    }

    @Test
    void deleteSalesShouldThrowWhenSalesNotFound() {
        when(salesMapper.selectById(999)).thenReturn(null);

        assertThatThrownBy(() -> salesService.deleteSales(999))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("销售单不存在");

        verify(productStockFacade, never()).adjustStock(any(), org.mockito.ArgumentMatchers.anyInt(),
                any(), org.mockito.ArgumentMatchers.anyBoolean(), any());
        verify(salesMapper, never()).deleteById(org.mockito.ArgumentMatchers.anyInt());
    }
}
