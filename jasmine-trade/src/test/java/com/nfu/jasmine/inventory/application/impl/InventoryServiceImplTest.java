package com.nfu.jasmine.inventory.application.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nfu.jasmine.common.dto.internal.FlowerDTO;
import com.nfu.jasmine.common.dto.internal.ProductStockFacade;
import com.nfu.jasmine.common.exception.BusinessException;
import com.nfu.jasmine.common.vo.TableData;
import com.nfu.jasmine.infra.client.FlowerClient;
import com.nfu.jasmine.infra.mq.message.InventoryChangedMessage;
import com.nfu.jasmine.infra.mq.publisher.MqMessagePublisher;
import com.nfu.jasmine.inventory.alert.service.InventoryAlertService;
import com.nfu.jasmine.inventory.model.entity.Inventory;
import com.nfu.jasmine.inventory.model.enumtype.InventoryBizType;
import com.nfu.jasmine.inventory.persistence.mapper.InventoryMapper;
import com.nfu.jasmine.inventory.web.dto.InventoryQueryDTO;
import com.nfu.jasmine.inventory.web.dto.InventorySaveDTO;
import com.nfu.jasmine.inventory.web.vo.InventoryVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * InventoryServiceImpl 单元测试。
 * <p>
 * 覆盖 saveInventory / updateInventory / deleteInventory 三大写操作的事务、库存调整与 MQ 事件，
 * 以及查询方法的分支逻辑（如按花卉名称模糊查询命中/未命中）。
 */
@ExtendWith(MockitoExtension.class)
class InventoryServiceImplTest {

    @Mock
    private InventoryMapper inventoryMapper;
    @Mock
    private FlowerClient flowerClient;
    @Mock
    private MqMessagePublisher mqMessagePublisher;
    @Mock
    private ProductStockFacade productStockFacade;
    @Mock
    private InventoryAlertService inventoryAlertService;

    @InjectMocks
    private InventoryServiceImpl inventoryService;

    @BeforeEach
    void setUp() {
        // ServiceImpl 需要通过反射注入 baseMapper
        ReflectionTestUtils.setField(inventoryService, "baseMapper", inventoryMapper);
    }

    // ==================== saveInventory ====================

    @Test
    void saveInventoryShouldDeductStockAndPublishEvent() {
        InventorySaveDTO dto = buildSaveDTO(1, "PURCHASE_IN", 20, new BigDecimal("8.50"));

        FlowerDTO flowerInfo = buildFlowerDTO(1, "红玫瑰");
        when(flowerClient.getFlowerById(1)).thenReturn(flowerInfo);

        ProductStockFacade.StockChangeResult stockChange =
                new ProductStockFacade.StockChangeResult(flowerInfo, 30, 50);
        when(productStockFacade.adjustStock(
                eq(1), eq(20), eq(new BigDecimal("8.50")), eq(true), any()))
                .thenReturn(stockChange);

        inventoryService.saveInventory(dto, 99);

        ArgumentCaptor<Inventory> inventoryCaptor = ArgumentCaptor.forClass(Inventory.class);
        verify(inventoryMapper).insert(inventoryCaptor.capture());
        Inventory saved = inventoryCaptor.getValue();
        assertThat(saved.getFlowerId()).isEqualTo(1);
        assertThat(saved.getBizType()).isEqualTo("PURCHASE_IN");
        assertThat(saved.getQuantity()).isEqualTo(20);
        assertThat(saved.getBeforeStock()).isEqualTo(30);
        assertThat(saved.getAfterStock()).isEqualTo(50);
        assertThat(saved.getUnitCost()).isEqualByComparingTo("8.50");
        assertThat(saved.getTotalCost()).isEqualByComparingTo("170.00"); // 8.50 * 20
        assertThat(saved.getOperatorId()).isEqualTo(99);
        assertThat(saved.getDeleted()).isEqualTo(0);

        verify(mqMessagePublisher).publishInventoryChangedAfterCommit(any(InventoryChangedMessage.class));
    }

    @Test
    void saveInventoryShouldApplyBizTypeDirection() {
        // SALE_OUT 方向为 -1，库存应扣减
        InventorySaveDTO dto = buildSaveDTO(1, "SALE_OUT", 5, null);

        FlowerDTO flowerInfo = buildFlowerDTO(1, "红玫瑰");
        when(flowerClient.getFlowerById(1)).thenReturn(flowerInfo);

        ProductStockFacade.StockChangeResult stockChange =
                new ProductStockFacade.StockChangeResult(flowerInfo, 50, 45);
        when(productStockFacade.adjustStock(
                eq(1), eq(-5), eq(null), eq(false), any()))
                .thenReturn(stockChange);

        inventoryService.saveInventory(dto, 1);

        // 验证传入 adjustStock 的 delta 已经被 apply() 转换为 -5
        verify(productStockFacade).adjustStock(eq(1), eq(-5), eq(null), eq(false), any());
    }

    @Test
    void saveInventoryShouldRejectInvalidBizType() {
        InventorySaveDTO dto = buildSaveDTO(1, "INVALID_TYPE", 5, null);
        assertThatThrownBy(() -> inventoryService.saveInventory(dto, 1))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("不支持的库存业务类型");
    }

    @Test
    void saveInventoryShouldRejectNonPositiveQuantity() {
        InventorySaveDTO dto = buildSaveDTO(1, "PURCHASE_IN", 0, new BigDecimal("8.50"));
        assertThatThrownBy(() -> inventoryService.saveInventory(dto, 1))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("变动数量必须大于 0");
    }

    @Test
    void saveInventoryShouldRejectPurchaseInWithoutUnitCost() {
        InventorySaveDTO dto = buildSaveDTO(1, "PURCHASE_IN", 20, null);
        assertThatThrownBy(() -> inventoryService.saveInventory(dto, 1))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("采购入库必须填写有效的进货单价");
    }

    // ==================== updateInventory ====================

    @Test
    void updateInventorySameFlowerShouldRollbackAndReapply() {
        // 场景：旧流水是 SALE_OUT 5 朵（delta=-5），改成 SALE_OUT 3 朵（delta=-3）
        // 库存基准 = beforeStock - oldDelta，afterStock = baseStock + newDelta
        Inventory existing = buildInventory(10, 1, "SALE_OUT", 5, 45, 40);

        InventorySaveDTO dto = buildSaveDTO(1, "SALE_OUT", 3, null);
        dto.setId(10);

        when(inventoryMapper.selectById(10)).thenReturn(existing);
        FlowerDTO flowerInfo = buildFlowerDTO(1, "红玫瑰");
        when(flowerClient.getFlowerById(1)).thenReturn(flowerInfo);

        // 同花卉：adjustStock delta = newDelta - oldDelta = -3 - (-5) = 2（回滚 2 朵）
        ProductStockFacade.StockChangeResult stockChange =
                new ProductStockFacade.StockChangeResult(flowerInfo, 40, 42);
        when(productStockFacade.adjustStock(
                eq(1), eq(2), eq(null), eq(false), any()))
                .thenReturn(stockChange);

        inventoryService.updateInventory(dto, 99);

        ArgumentCaptor<Inventory> captor = ArgumentCaptor.forClass(Inventory.class);
        verify(inventoryMapper).updateById(captor.capture());
        Inventory updated = captor.getValue();
        assertThat(updated.getBizType()).isEqualTo("SALE_OUT");
        assertThat(updated.getQuantity()).isEqualTo(3);
        // baseStock = beforeStock - oldDelta = 40 - (-5) = 45
        assertThat(updated.getBeforeStock()).isEqualTo(45);
        assertThat(updated.getAfterStock()).isEqualTo(42);
        assertThat(updated.getOperatorId()).isEqualTo(99);

        verify(mqMessagePublisher).publishInventoryChangedAfterCommit(any(InventoryChangedMessage.class));
    }

    @Test
    void updateInventoryChangeFlowerShouldRollbackOldAndApplyNew() {
        // 场景：旧流水 flowerId=1 SALE_OUT 5（delta=-5），改成 flowerId=2 PURCHASE_IN 10（delta=+10）
        Inventory existing = buildInventory(10, 1, "SALE_OUT", 5, 50, 45);

        InventorySaveDTO dto = buildSaveDTO(2, "PURCHASE_IN", 10, new BigDecimal("12.00"));
        dto.setId(10);

        when(inventoryMapper.selectById(10)).thenReturn(existing);

        // 旧花卉回滚：adjustStock(1, +5, null, false, null)
        when(productStockFacade.adjustStock(eq(1), eq(5), eq(null), eq(false), eq(null)))
                .thenReturn(new ProductStockFacade.StockChangeResult(
                        buildFlowerDTO(1, "红玫瑰"), 45, 50));

        // 新花卉新增：adjustStock(2, +10, 12.00, true, "...")
        FlowerDTO newFlower = buildFlowerDTO(2, "百合");
        when(flowerClient.getFlowerById(2)).thenReturn(newFlower);
        when(productStockFacade.adjustStock(
                eq(2), eq(10), eq(new BigDecimal("12.00")), eq(true), any()))
                .thenReturn(new ProductStockFacade.StockChangeResult(newFlower, 20, 30));

        inventoryService.updateInventory(dto, 99);

        ArgumentCaptor<Inventory> captor = ArgumentCaptor.forClass(Inventory.class);
        verify(inventoryMapper).updateById(captor.capture());
        Inventory updated = captor.getValue();
        assertThat(updated.getFlowerId()).isEqualTo(2);
        assertThat(updated.getBizType()).isEqualTo("PURCHASE_IN");
        assertThat(updated.getQuantity()).isEqualTo(10);
        assertThat(updated.getBeforeStock()).isEqualTo(20);
        assertThat(updated.getAfterStock()).isEqualTo(30);
        assertThat(updated.getUnitCost()).isEqualByComparingTo("12.00");
        assertThat(updated.getTotalCost()).isEqualByComparingTo("120.00");

        // 应该调用 adjustStock 两次（旧花卉回滚 + 新花卉新增）
        verify(productStockFacade).adjustStock(eq(1), eq(5), eq(null), eq(false), eq(null));
        verify(mqMessagePublisher).publishInventoryChangedAfterCommit(any(InventoryChangedMessage.class));
    }

    @Test
    void updateInventoryShouldThrowWhenNotFound() {
        InventorySaveDTO dto = buildSaveDTO(1, "PURCHASE_IN", 5, new BigDecimal("8.50"));
        dto.setId(999);
        when(inventoryMapper.selectById(999)).thenReturn(null);

        assertThatThrownBy(() -> inventoryService.updateInventory(dto, 1))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("库存流水不存在");
    }

    // ==================== deleteInventory ====================

    @Test
    void deleteInventoryShouldRollbackStockAndRemove() {
        // 旧流水是 PURCHASE_IN 20（delta=+20），删除时应回滚：adjustStock(flowerId, -20, null, false, null)
        Inventory existing = buildInventory(10, 1, "PURCHASE_IN", 20, 30, 50);

        when(inventoryMapper.selectById(10)).thenReturn(existing);

        inventoryService.deleteInventory(10);

        verify(productStockFacade).adjustStock(eq(1), eq(-20), eq(null), eq(false), eq(null));
        verify(inventoryMapper).deleteById(10);
        verify(mqMessagePublisher).publishInventoryChangedAfterCommit(any(InventoryChangedMessage.class));
    }

    @Test
    void deleteInventoryShouldThrowWhenNotFound() {
        when(inventoryMapper.selectById(999)).thenReturn(null);

        assertThatThrownBy(() -> inventoryService.deleteInventory(999))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("库存流水不存在");

        verify(productStockFacade, never()).adjustStock(any(), anyInt(),
                any(), anyBoolean(), any());
        verify(mqMessagePublisher, never()).publishInventoryChangedAfterCommit(any());
    }

    // ==================== 查询方法 ====================

    @Test
    void getInventoryDetailShouldReturnVo() {
        Inventory inventory = buildInventory(10, 1, "PURCHASE_IN", 20, 30, 50);
        when(inventoryMapper.selectById(10)).thenReturn(inventory);
        when(flowerClient.getFlowersByIds(anyList()))
                .thenReturn(List.of(buildFlowerDTO(1, "红玫瑰")));

        InventoryVO vo = inventoryService.getInventoryDetail(10);

        assertThat(vo).isNotNull();
        assertThat(vo.getId()).isEqualTo(10);
        assertThat(vo.getFlowerName()).isEqualTo("红玫瑰");
        assertThat(vo.getBizTypeLabel()).isEqualTo("采购入库");
    }

    @Test
    void getInventoryDetailShouldThrowWhenNotFound() {
        when(inventoryMapper.selectById(999)).thenReturn(null);

        assertThatThrownBy(() -> inventoryService.getInventoryDetail(999))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("库存流水不存在");
    }

    @Test
    void pageInventoryByNameWithNoMatchShouldReturnEmpty() {
        InventoryQueryDTO queryDTO = new InventoryQueryDTO();
        queryDTO.setName("不存在的花");
        queryDTO.setPageNo(1L);
        queryDTO.setPageSize(10L);
        when(flowerClient.getFlowerIdsByName("不存在的花")).thenReturn(List.of());

        TableData<InventoryVO> result = inventoryService.pageInventory(queryDTO);

        assertThat(result.getTotal()).isEqualTo(0L);
        assertThat(result.getRows()).isEmpty();
        // 不应继续查库
        verify(inventoryMapper, never()).selectPage(any(), any());
    }

    @Test
    void getLowStockCountShouldDelegateToAlertService() {
        when(inventoryAlertService.getLowStockCount()).thenReturn(5L);

        Long count = inventoryService.getLowStockCount();

        assertThat(count).isEqualTo(5L);
        verify(inventoryAlertService).getLowStockCount();
    }

    // ==================== helper ====================

    private InventorySaveDTO buildSaveDTO(Integer flowerId, String bizType, int quantity, BigDecimal unitCost) {
        InventorySaveDTO dto = new InventorySaveDTO();
        dto.setFlowerId(flowerId);
        dto.setBizType(bizType);
        dto.setQuantity(quantity);
        dto.setUnitCost(unitCost);
        dto.setDate(new Date());
        return dto;
    }

    private Inventory buildInventory(Integer id, Integer flowerId, String bizType,
                                     int quantity, int beforeStock, int afterStock) {
        Inventory inventory = new Inventory();
        inventory.setId(id);
        inventory.setBizNo("INV" + id);
        inventory.setFlowerId(flowerId);
        inventory.setBizType(bizType);
        inventory.setQuantity(quantity);
        inventory.setBeforeStock(beforeStock);
        inventory.setAfterStock(afterStock);
        inventory.setDate(new Date());
        inventory.setDeleted(0);
        return inventory;
    }

    private FlowerDTO buildFlowerDTO(Integer id, String name) {
        FlowerDTO flower = new FlowerDTO();
        flower.setId(id);
        flower.setName(name);
        return flower;
    }
}
