package com.nfu.jasmine.workflow;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.nfu.jasmine.config.AbstractIntegrationTest;
import com.nfu.jasmine.appointment.application.IAppointmentService;
import com.nfu.jasmine.appointment.web.dto.AppointmentCreateDTO;
import com.nfu.jasmine.inventory.web.dto.InventorySaveDTO;
import com.nfu.jasmine.sales.web.dto.SalesItemSaveDTO;
import com.nfu.jasmine.sales.web.dto.SalesSaveDTO;
import com.nfu.jasmine.flower.model.entity.Flower;
import com.nfu.jasmine.inventory.model.entity.Inventory;
import com.nfu.jasmine.inventory.model.enumtype.InventoryBizType;
import com.nfu.jasmine.flower.persistence.mapper.FlowerMapper;
import com.nfu.jasmine.inventory.persistence.mapper.InventoryMapper;
import com.nfu.jasmine.inventory.application.IInventoryService;
import com.nfu.jasmine.appointment.web.vo.AppointmentVO;
import com.nfu.jasmine.sales.application.ISalesService;
import com.nfu.jasmine.sales.web.vo.SalesVO;
import com.nfu.jasmine.sales.web.vo.TodayBusinessSummaryVO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class BusinessModelWorkflowIT extends AbstractIntegrationTest {

    @Autowired
    private ISalesService salesService;

    @Autowired
    private IAppointmentService appointmentService;

    @Autowired
    private IInventoryService inventoryService;

    @Autowired
    private FlowerMapper flowerMapper;

    @Autowired
    private InventoryMapper inventoryMapper;

    @Test
    void createSalesShouldDeductFlowerStockWriteInventoryFlowAndSnapshotCost() {
        Flower flower = flowerMapper.selectById(1);
        assertThat(flower).isNotNull();
        int beforeStock = flower.getCurrentStock();

        SalesItemSaveDTO item = new SalesItemSaveDTO();
        item.setFlowerId(flower.getId());
        item.setQuantity(5);
        item.setUnitPrice(new BigDecimal("13.00"));

        SalesSaveDTO salesDTO = new SalesSaveDTO();
        salesDTO.setVipId(1);
        salesDTO.setDate(new Date());
        salesDTO.setRemark("集成测试销售");
        salesDTO.setItems(List.of(item));

        salesService.saveSales(salesDTO, 1);

        Flower updatedFlower = flowerMapper.selectById(flower.getId());
        assertThat(updatedFlower.getCurrentStock()).isEqualTo(beforeStock - 5);

        SalesVO latestSales = salesService.listSales().stream().findFirst().orElse(null);
        assertThat(latestSales).isNotNull();
        assertThat(latestSales.getItems()).hasSize(1);
        assertThat(latestSales.getTotalAmount()).isEqualByComparingTo("65.00");
        assertThat(latestSales.getItems().get(0).getUnitCost()).isEqualByComparingTo("10.00");
        assertThat(latestSales.getItems().get(0).getCostAmount()).isEqualByComparingTo("50.00");

        List<Inventory> inventoryList = inventoryMapper.selectList(new LambdaQueryWrapper<Inventory>()
                .eq(Inventory::getBizNo, latestSales.getOrderNo())
                .eq(Inventory::getBizType, InventoryBizType.SALE_OUT.name()));
        assertThat(inventoryList).hasSize(1);
        assertThat(inventoryList.get(0).getBeforeStock()).isEqualTo(beforeStock);
        assertThat(inventoryList.get(0).getAfterStock()).isEqualTo(beforeStock - 5);
        assertThat(inventoryList.get(0).getUnitCost()).isEqualByComparingTo("10.00");
        assertThat(inventoryList.get(0).getTotalCost()).isEqualByComparingTo("50.00");
    }

    @Test
    void createPurchaseInventoryShouldRecordActualCostAndSyncFlowerCostPrice() {
        Flower flower = flowerMapper.selectById(2);
        assertThat(flower).isNotNull();
        int beforeStock = flower.getCurrentStock();

        InventorySaveDTO inventoryDTO = new InventorySaveDTO();
        inventoryDTO.setFlowerId(flower.getId());
        inventoryDTO.setBizType(InventoryBizType.PURCHASE_IN.name());
        inventoryDTO.setQuantity(20);
        inventoryDTO.setUnitCost(new BigDecimal("6.50"));
        inventoryDTO.setDate(new Date());
        inventoryDTO.setRemark("集成测试采购");

        inventoryService.saveInventory(inventoryDTO, 1);

        Flower updatedFlower = flowerMapper.selectById(flower.getId());
        assertThat(updatedFlower.getCurrentStock()).isEqualTo(beforeStock + 20);
        assertThat(updatedFlower.getCostPrice()).isEqualByComparingTo("6.50");

        Inventory latestInventory = inventoryMapper.selectList(new LambdaQueryWrapper<Inventory>()
                .eq(Inventory::getFlowerId, flower.getId())
                .eq(Inventory::getBizType, InventoryBizType.PURCHASE_IN.name())
                .orderByDesc(Inventory::getId)).get(0);
        assertThat(latestInventory.getUnitCost()).isEqualByComparingTo("6.50");
        assertThat(latestInventory.getTotalCost()).isEqualByComparingTo("130.00");
    }

    @Test
    void getTodayBusinessSummaryShouldAggregateSalesPurchaseAndProfit() {
        TodayBusinessSummaryVO summary = salesService.getTodayBusinessSummary();

        assertThat(summary.getTodaySalesAmount()).isNotNull();
        assertThat(summary.getTodayPurchaseCost()).isNotNull();
        assertThat(summary.getTodayGrossProfit()).isNotNull();
        assertThat(summary.getTodayNetCashflow()).isEqualByComparingTo(
                summary.getTodaySalesAmount().subtract(summary.getTodayPurchaseCost())
        );
    }

    @Test
    void createAppointmentShouldBindRealVipRelation() {
        String content = "集成测试预约-" + System.currentTimeMillis();

        AppointmentCreateDTO appointmentDTO = new AppointmentCreateDTO();
        appointmentDTO.setPhone("13677778888");
        appointmentDTO.setDate(new Date());
        appointmentDTO.setContent(content);

        appointmentService.createAppointment(appointmentDTO);

        AppointmentVO latestAppointment = appointmentService.listAppointments().stream()
                .filter(item -> content.equals(item.getContent()))
                .findFirst()
                .orElse(null);
        assertThat(latestAppointment).isNotNull();
        assertThat(latestAppointment.getVipId()).isEqualTo(1);
        assertThat(latestAppointment.getName()).isEqualTo("管先生");
        assertThat(latestAppointment.getPhone()).isEqualTo("13677778888");
    }
}
