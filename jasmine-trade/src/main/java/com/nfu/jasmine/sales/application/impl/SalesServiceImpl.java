package com.nfu.jasmine.sales.application.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nfu.jasmine.common.dto.internal.FlowerDTO;
import com.nfu.jasmine.common.dto.internal.ProductStockFacade;
import com.nfu.jasmine.common.exception.BusinessException;
import com.nfu.jasmine.common.utils.BusinessNoUtil;
import com.nfu.jasmine.common.vo.TableData;
import com.nfu.jasmine.infra.client.FlowerClient;
import com.nfu.jasmine.infra.client.VipClient;
import com.nfu.jasmine.infra.mq.message.InventoryChangedMessage;
import com.nfu.jasmine.infra.mq.message.InventoryChangeSource;
import com.nfu.jasmine.infra.mq.message.SalesCreatedMessage;
import com.nfu.jasmine.infra.mq.publisher.MqMessagePublisher;
import com.nfu.jasmine.inventory.model.entity.Inventory;
import com.nfu.jasmine.inventory.model.enumtype.InventoryBizType;
import com.nfu.jasmine.inventory.persistence.mapper.InventoryMapper;
import com.nfu.jasmine.sales.application.ISalesService;
import com.nfu.jasmine.sales.model.entity.Sales;
import com.nfu.jasmine.sales.model.entity.SalesItem;
import com.nfu.jasmine.sales.persistence.mapper.SalesItemMapper;
import com.nfu.jasmine.sales.persistence.mapper.SalesMapper;
import com.nfu.jasmine.sales.web.dto.SalesItemSaveDTO;
import com.nfu.jasmine.sales.web.dto.SalesQueryDTO;
import com.nfu.jasmine.sales.web.dto.SalesSaveDTO;
import com.nfu.jasmine.sales.web.vo.SalesItemVO;
import com.nfu.jasmine.sales.web.vo.SalesVO;
import com.nfu.jasmine.sales.web.vo.TodayBusinessSummaryVO;
import com.nfu.jasmine.common.dto.internal.VipBasicDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class SalesServiceImpl extends ServiceImpl<SalesMapper, Sales> implements ISalesService {
    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Shanghai");

    @Autowired
    private SalesItemMapper salesItemMapper;

    @Autowired
    private InventoryMapper inventoryMapper;

    @Autowired
    private FlowerClient flowerClient;

    @Autowired
    private VipClient vipClient;

    @Autowired
    private MqMessagePublisher mqMessagePublisher;

    @Autowired
    private ProductStockFacade productStockFacade;

    @Override
    public List<SalesVO> listSales() {
        LambdaQueryWrapper<Sales> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(Sales::getDate).orderByDesc(Sales::getId);
        return buildSalesVOs(this.list(wrapper));
    }

    @Override
    public TableData<SalesVO> pageSales(SalesQueryDTO queryDTO) {
        LambdaQueryWrapper<Sales> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(Sales::getDate).orderByDesc(Sales::getId);

        if (StringUtils.hasLength(queryDTO.getOrderNo())) {
            wrapper.like(Sales::getOrderNo, queryDTO.getOrderNo());
        }
        if (queryDTO.getStartTime() != null) {
            wrapper.ge(Sales::getDate, queryDTO.getStartTime());
        }
        if (queryDTO.getEndTime() != null) {
            wrapper.le(Sales::getDate, queryDTO.getEndTime());
        }

        Page<Sales> page = new Page<>(queryDTO.getPageNo(), queryDTO.getPageSize());
        this.page(page, wrapper);

        TableData<SalesVO> data = new TableData<>();
        data.setTotal(page.getTotal());
        data.setRows(buildSalesVOs(page.getRecords()));
        return data;
    }

    @Override
    public SalesVO getSalesDetail(Integer id) {
        Sales sales = requireSales(id);
        return buildSalesVOs(List.of(sales)).stream().findFirst().orElse(null);
    }

    @Override
    public TodayBusinessSummaryVO getTodayBusinessSummary() {
        LocalDate today = LocalDate.now(BUSINESS_ZONE);
        Date startTime = Date.from(today.atStartOfDay(BUSINESS_ZONE).toInstant());
        Date endTime = Date.from(today.plusDays(1).atStartOfDay(BUSINESS_ZONE).toInstant());

        // 统计按业务日自然日口径汇总，避免页面把"今日"理解成最近 24 小时。
        List<Sales> todaySalesList = this.list(new LambdaQueryWrapper<Sales>()
                .ge(Sales::getDate, startTime)
                .lt(Sales::getDate, endTime));
        BigDecimal todaySalesAmount = sumBigDecimal(todaySalesList.stream()
                .map(Sales::getTotalAmount)
                .toList());

        List<SalesItem> todaySalesItems = Collections.emptyList();
        if (!todaySalesList.isEmpty()) {
            Set<Integer> salesIds = todaySalesList.stream().map(Sales::getId).collect(Collectors.toSet());
            todaySalesItems = salesItemMapper.selectList(new LambdaQueryWrapper<SalesItem>()
                    .in(SalesItem::getSalesId, salesIds));
        }
        BigDecimal todayGrossProfit = todaySalesAmount.subtract(sumBigDecimal(todaySalesItems.stream()
                .map(SalesItem::getCostAmount)
                .toList()));

        List<Inventory> todayPurchaseList = inventoryMapper.selectList(new LambdaQueryWrapper<Inventory>()
                .eq(Inventory::getBizType, InventoryBizType.PURCHASE_IN.name())
                .ge(Inventory::getDate, startTime)
                .lt(Inventory::getDate, endTime));
        BigDecimal todayPurchaseCost = sumBigDecimal(todayPurchaseList.stream()
                .map(Inventory::getTotalCost)
                .toList());

        TodayBusinessSummaryVO summary = new TodayBusinessSummaryVO();
        summary.setTodaySalesAmount(todaySalesAmount);
        summary.setTodayPurchaseCost(todayPurchaseCost);
        summary.setTodayGrossProfit(todayGrossProfit);
        summary.setTodayNetCashflow(todaySalesAmount.subtract(todayPurchaseCost));
        summary.setTodaySalesOrderCount(todaySalesList.size());
        summary.setTodayPurchaseCount(todayPurchaseList.size());
        return summary;
    }

    @Override
    @Transactional
    public void saveSales(SalesSaveDTO salesDTO, Integer operatorId) {
        validateVipIfPresent(salesDTO.getVipId());

        Sales sales = new Sales();
        sales.setOrderNo(BusinessNoUtil.generateSalesOrderNo());
        sales.setVipId(salesDTO.getVipId());
        sales.setDate(salesDTO.getDate());
        sales.setRemark(salesDTO.getRemark());
        sales.setOperatorId(operatorId);
        sales.setTotalAmount(BigDecimal.ZERO);
        sales.setDeleted(0);
        this.save(sales);

        BigDecimal totalAmount = rebuildSalesItems(sales, salesDTO.getItems(), operatorId);
        sales.setTotalAmount(totalAmount);
        this.updateById(sales);

        // 销售单创建成功后补一条业务事件，后面统计、审计或同步别的模块都从这里接。
        mqMessagePublisher.publishSalesCreatedAfterCommit(new SalesCreatedMessage(
                sales.getId(),
                sales.getOrderNo(),
                sales.getVipId(),
                operatorId,
                salesDTO.getItems() == null ? 0 : salesDTO.getItems().size(),
                totalAmount,
                sales.getDate(),
                new Date()
        ));
    }

    @Override
    @Transactional
    public void updateSales(SalesSaveDTO salesDTO, Integer operatorId) {
        Sales existing = requireSales(salesDTO.getId());
        validateVipIfPresent(salesDTO.getVipId());

        // 修改采用"先回滚再重建"策略：把旧明细库存全部恢复，然后按新明细重新扣减。
        restoreSales(existing);

        // 销售单修改后，需要为被回补的旧库存发送ROLLBACK事件
        List<Inventory> oldInventories = inventoryMapper.selectList(new LambdaQueryWrapper<Inventory>()
                .eq(Inventory::getBizNo, existing.getOrderNo())
                .eq(Inventory::getBizType, InventoryBizType.SALE_OUT.name()));
        for (Inventory inventory : oldInventories) {
            mqMessagePublisher.publishInventoryChangedAfterCommit(new InventoryChangedMessage(
                    inventory.getId(),
                    inventory.getBizNo(),
                    inventory.getFlowerId(),
                    inventory.getBizType(),
                    -inventory.getQuantity(),
                    inventory.getAfterStock(),
                    inventory.getBeforeStock(),
                    inventory.getOperatorId(),
                    inventory.getDate(),
                    new Date(),
                    InventoryChangeSource.SALES_ORDER,
                    InventoryChangeSource.ACTION_ROLLBACK
            ));
        }

        existing.setVipId(salesDTO.getVipId());
        existing.setDate(salesDTO.getDate());
        existing.setRemark(salesDTO.getRemark());
        existing.setOperatorId(operatorId);

        BigDecimal totalAmount = rebuildSalesItems(existing, salesDTO.getItems(), operatorId);
        existing.setTotalAmount(totalAmount);
        this.updateById(existing);
    }

    @Override
    @Transactional
    public void deleteSales(Integer id) {
        Sales existing = requireSales(id);
        restoreSales(existing);
        
        // 销售单删除后，需要为被回补的库存发送ROLLBACK事件
        List<Inventory> deletedInventories = inventoryMapper.selectList(new LambdaQueryWrapper<Inventory>()
                .eq(Inventory::getBizNo, existing.getOrderNo())
                .eq(Inventory::getBizType, InventoryBizType.SALE_OUT.name()));
        for (Inventory inventory : deletedInventories) {
            mqMessagePublisher.publishInventoryChangedAfterCommit(new InventoryChangedMessage(
                    inventory.getId(),
                    inventory.getBizNo(),
                    inventory.getFlowerId(),
                    inventory.getBizType(),
                    -inventory.getQuantity(),
                    inventory.getAfterStock(),
                    inventory.getBeforeStock(),
                    inventory.getOperatorId(),
                    inventory.getDate(),
                    new Date(),
                    InventoryChangeSource.SALES_ORDER,
                    InventoryChangeSource.ACTION_ROLLBACK
            ));
        }
        
        this.removeById(id);
    }

    // 回滚销售单对库存的影响：把已出库的数量加回花卉库存，然后清除明细和对应的库存流水。
    private void restoreSales(Sales sales) {
        List<SalesItem> items = listSalesItemsBySalesId(sales.getId());
        for (SalesItem item : items) {
            productStockFacade.adjustStock(item.getFlowerId(), item.getQuantity(), null, false, null);
        }

        salesItemMapper.delete(new LambdaQueryWrapper<SalesItem>().eq(SalesItem::getSalesId, sales.getId()));
        inventoryMapper.delete(new LambdaQueryWrapper<Inventory>()
                .eq(Inventory::getBizNo, sales.getOrderNo())
                .eq(Inventory::getBizType, InventoryBizType.SALE_OUT.name()));
    }

    private BigDecimal rebuildSalesItems(Sales sales, List<SalesItemSaveDTO> items, Integer operatorId) {
        if (items == null || items.isEmpty()) {
            throw new BusinessException("销售明细不能为空！");
        }

        BigDecimal totalAmount = BigDecimal.ZERO;
        for (SalesItemSaveDTO itemDTO : items) {
            int quantity = requirePositiveQuantity(itemDTO.getQuantity(), "销售数量必须大于 0！");
            ProductStockFacade.StockChangeResult stockChange = productStockFacade.adjustStock(
                    itemDTO.getFlowerId(),
                    -quantity,
                    null,
                    false,
                    "库存不足，请调整销售数量！"
            );
            FlowerDTO flower = stockChange.flower();
            BigDecimal unitPrice = requirePositivePrice(itemDTO.getUnitPrice(), "销售单价必须大于 0！");
            BigDecimal unitCost = requirePositivePrice(flower.getCost(), "请先维护花卉成本价，再创建销售单！");
            int beforeStock = stockChange.beforeStock();
            int afterStock = stockChange.afterStock();

            BigDecimal amount = unitPrice.multiply(BigDecimal.valueOf(quantity));
            BigDecimal costAmount = unitCost.multiply(BigDecimal.valueOf(quantity));
            totalAmount = totalAmount.add(amount);

            // 明细里冻结销售时刻的成本快照，避免花卉成本后续变化把历史毛利"改写"掉。
            SalesItem salesItem = new SalesItem();
            salesItem.setSalesId(sales.getId());
            salesItem.setFlowerId(flower.getId());
            salesItem.setQuantity(quantity);
            salesItem.setUnitPrice(unitPrice);
            salesItem.setUnitCost(unitCost);
            salesItem.setAmount(amount);
            salesItem.setCostAmount(costAmount);
            salesItem.setDeleted(0);
            salesItemMapper.insert(salesItem);

            // 销售出库也写入库存流水，保证"销售链路"和"库存链路"始终能对得上。
            Inventory inventory = new Inventory();
            inventory.setBizNo(sales.getOrderNo());
            inventory.setFlowerId(flower.getId());
            inventory.setBizType(InventoryBizType.SALE_OUT.name());
            inventory.setQuantity(quantity);
            inventory.setBeforeStock(beforeStock);
            inventory.setAfterStock(afterStock);
            inventory.setUnitCost(unitCost);
            inventory.setTotalCost(costAmount);
            inventory.setRemark(sales.getRemark());
            inventory.setOperatorId(operatorId);
            inventory.setDate(sales.getDate());
            inventory.setDeleted(0);
            inventoryMapper.insert(inventory);

            // 销售出库也统一发库存事件，后面做库存预警、异步统计时不需要再回头改销售事务。
            mqMessagePublisher.publishInventoryChangedAfterCommit(new InventoryChangedMessage(
                    inventory.getId(),
                    inventory.getBizNo(),
                    inventory.getFlowerId(),
                    inventory.getBizType(),
                    inventory.getQuantity(),
                    inventory.getBeforeStock(),
                    inventory.getAfterStock(),
                    inventory.getOperatorId(),
                    inventory.getDate(),
                    new Date(),
                    InventoryChangeSource.SALES_ORDER,
                    InventoryChangeSource.ACTION_CREATE
            ));
        }
        return totalAmount;
    }

    private List<SalesVO> buildSalesVOs(List<Sales> records) {
        if (records == null || records.isEmpty()) {
            return Collections.emptyList();
        }

        // 列表页需要同时展示会员和明细，先批量取回，避免 N+1 查询。
        Set<Integer> salesIds = records.stream().map(Sales::getId).collect(Collectors.toSet());
        Set<Integer> vipIds = records.stream()
                .map(Sales::getVipId)
                .filter(id -> id != null)
                .collect(Collectors.toSet());

        Map<Integer, VipBasicDTO> vipMap = vipIds.isEmpty()
                ? Collections.emptyMap()
                : vipClient.getVipsByIds(new ArrayList<>(vipIds)).stream()
                .collect(Collectors.toMap(VipBasicDTO::getId, v -> v, (left, right) -> left, LinkedHashMap::new));

        List<SalesItem> salesItems = salesItemMapper.selectList(new LambdaQueryWrapper<SalesItem>()
                .in(SalesItem::getSalesId, salesIds)
                .orderByAsc(SalesItem::getId));
        Map<Integer, List<SalesItem>> itemGroup = salesItems.stream().collect(Collectors.groupingBy(SalesItem::getSalesId));

        Set<Integer> flowerIds = salesItems.stream().map(SalesItem::getFlowerId).collect(Collectors.toSet());
        Map<Integer, FlowerDTO> flowerMap = flowerIds.isEmpty()
                ? Collections.emptyMap()
                : flowerClient.getFlowersByIds(new ArrayList<>(flowerIds)).stream()
                .collect(Collectors.toMap(FlowerDTO::getId, f -> f, (left, right) -> left, LinkedHashMap::new));

        List<SalesVO> result = new ArrayList<>(records.size());
        for (Sales sales : records) {
            VipBasicDTO vip = sales.getVipId() == null ? null : vipMap.get(sales.getVipId());
            List<SalesItemVO> itemVOList = itemGroup.getOrDefault(sales.getId(), Collections.emptyList()).stream()
                    .map(item -> toSalesItemVO(item, flowerMap.get(item.getFlowerId())))
                    .toList();

            SalesVO vo = new SalesVO();
            vo.setId(sales.getId());
            vo.setOrderNo(sales.getOrderNo());
            vo.setVipId(sales.getVipId());
            vo.setVipName(vip == null ? null : vip.getName());
            vo.setVipPhone(vip == null ? null : vip.getPhone());
            vo.setTotalAmount(sales.getTotalAmount());
            vo.setRemark(sales.getRemark());
            vo.setItemCount(itemVOList.size());
            vo.setDate(sales.getDate());
            vo.setItems(itemVOList);
            result.add(vo);
        }
        return result;
    }

    private SalesItemVO toSalesItemVO(SalesItem item, FlowerDTO flower) {
        SalesItemVO vo = new SalesItemVO();
        vo.setId(item.getId());
        vo.setFlowerId(item.getFlowerId());
        vo.setFlowerName(flower == null ? "未知花卉" : flower.getName());
        vo.setQuantity(item.getQuantity());
        vo.setUnitPrice(item.getUnitPrice());
        vo.setUnitCost(item.getUnitCost());
        vo.setAmount(item.getAmount());
        vo.setCostAmount(item.getCostAmount());
        return vo;
    }

    private List<SalesItem> listSalesItemsBySalesId(Integer salesId) {
        return salesItemMapper.selectList(new LambdaQueryWrapper<SalesItem>()
                .eq(SalesItem::getSalesId, salesId)
                .orderByAsc(SalesItem::getId));
    }

    private Sales requireSales(Integer id) {
        Sales sales = this.getById(id);
        if (sales == null) {
            throw new BusinessException("销售单不存在！");
        }
        return sales;
    }

    private void validateVipIfPresent(Integer vipId) {
        if (vipId != null) {
            try {
                VipBasicDTO vip = vipClient.getVipById(vipId);
                if (vip == null) {
                    throw new BusinessException("所选会员不存在，请刷新后重试！");
                }
            } catch (BusinessException e) {
                // 远程接口返回 404 时抛出的 BusinessException，直接透传
                throw new BusinessException("所选会员不存在，请刷新后重试！");
            }
        }
    }

    private int requirePositiveQuantity(Integer quantity, String message) {
        if (quantity == null || quantity <= 0) {
            throw new BusinessException(message);
        }
        return quantity;
    }

    private BigDecimal requirePositivePrice(BigDecimal price, String message) {
        if (price == null || price.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(message);
        }
        return price;
    }

    private BigDecimal sumBigDecimal(List<BigDecimal> values) {
        return values.stream()
                .filter(value -> value != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
