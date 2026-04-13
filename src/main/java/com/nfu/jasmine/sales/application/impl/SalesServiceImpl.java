package com.nfu.jasmine.sales.application.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nfu.jasmine.common.exception.BusinessException;
import com.nfu.jasmine.common.utils.BusinessNoUtil;
import com.nfu.jasmine.common.vo.TableData;
import com.nfu.jasmine.infra.cache.CacheNames;
import com.nfu.jasmine.sales.web.dto.SalesItemSaveDTO;
import com.nfu.jasmine.sales.web.dto.SalesQueryDTO;
import com.nfu.jasmine.sales.web.dto.SalesSaveDTO;
import com.nfu.jasmine.flower.model.entity.Flower;
import com.nfu.jasmine.inventory.model.entity.Inventory;
import com.nfu.jasmine.sales.model.entity.Sales;
import com.nfu.jasmine.sales.model.entity.SalesItem;
import com.nfu.jasmine.vip.model.entity.Vip;
import com.nfu.jasmine.inventory.model.enumtype.InventoryBizType;
import com.nfu.jasmine.flower.persistence.mapper.FlowerMapper;
import com.nfu.jasmine.inventory.persistence.mapper.InventoryMapper;
import com.nfu.jasmine.sales.persistence.mapper.SalesItemMapper;
import com.nfu.jasmine.sales.persistence.mapper.SalesMapper;
import com.nfu.jasmine.vip.persistence.mapper.VipMapper;
import com.nfu.jasmine.sales.application.ISalesService;
import com.nfu.jasmine.sales.web.vo.SalesItemVO;
import com.nfu.jasmine.sales.web.vo.SalesVO;
import com.nfu.jasmine.sales.web.vo.TodayBusinessSummaryVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
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
    private FlowerMapper flowerMapper;

    @Autowired
    private InventoryMapper inventoryMapper;

    @Autowired
    private VipMapper vipMapper;

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

        // 统计按业务日自然日口径汇总，避免页面把“今日”理解成最近 24 小时。
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
    @Caching(evict = {
            @CacheEvict(value = CacheNames.FLOWER_LIST, allEntries = true),
            @CacheEvict(value = CacheNames.FLOWER_DETAIL, allEntries = true)
    })
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
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = CacheNames.FLOWER_LIST, allEntries = true),
            @CacheEvict(value = CacheNames.FLOWER_DETAIL, allEntries = true)
    })
    public void updateSales(SalesSaveDTO salesDTO, Integer operatorId) {
        Sales existing = requireSales(salesDTO.getId());
        validateVipIfPresent(salesDTO.getVipId());

        restoreSales(existing);

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
    @Caching(evict = {
            @CacheEvict(value = CacheNames.FLOWER_LIST, allEntries = true),
            @CacheEvict(value = CacheNames.FLOWER_DETAIL, allEntries = true)
    })
    public void deleteSales(Integer id) {
        Sales existing = requireSales(id);
        restoreSales(existing);
        this.removeById(id);
    }

    private void restoreSales(Sales sales) {
        List<SalesItem> items = listSalesItemsBySalesId(sales.getId());
        for (SalesItem item : items) {
            Flower flower = requireFlower(item.getFlowerId());
            int restoredStock = safeStock(flower) + item.getQuantity();
            flower.setCurrentStock(restoredStock);
            flowerMapper.updateById(flower);
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
            Flower flower = requireFlower(itemDTO.getFlowerId());
            int quantity = requirePositiveQuantity(itemDTO.getQuantity(), "销售数量必须大于 0！");
            BigDecimal unitPrice = requirePositivePrice(itemDTO.getUnitPrice(), "销售单价必须大于 0！");
            BigDecimal unitCost = requirePositivePrice(flower.getCostPrice(), "请先维护花卉成本价，再创建销售单！");

            int beforeStock = safeStock(flower);
            int afterStock = beforeStock - quantity;
            if (afterStock < 0) {
                throw new BusinessException(flower.getName() + "库存不足，请调整销售数量！");
            }

            BigDecimal amount = unitPrice.multiply(BigDecimal.valueOf(quantity));
            BigDecimal costAmount = unitCost.multiply(BigDecimal.valueOf(quantity));
            totalAmount = totalAmount.add(amount);

            // 明细里冻结销售时刻的成本快照，避免花卉成本后续变化把历史毛利“改写”掉。
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

            // 销售出库也写入库存流水，保证“销售链路”和“库存链路”始终能对得上。
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

            flower.setCurrentStock(afterStock);
            flowerMapper.updateById(flower);
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

        Map<Integer, Vip> vipMap = vipIds.isEmpty()
                ? Collections.emptyMap()
                : vipMapper.selectBatchIds(vipIds).stream()
                .collect(Collectors.toMap(Vip::getId, vip -> vip, (left, right) -> left, LinkedHashMap::new));

        List<SalesItem> salesItems = salesItemMapper.selectList(new LambdaQueryWrapper<SalesItem>()
                .in(SalesItem::getSalesId, salesIds)
                .orderByAsc(SalesItem::getId));
        Map<Integer, List<SalesItem>> itemGroup = salesItems.stream().collect(Collectors.groupingBy(SalesItem::getSalesId));

        Set<Integer> flowerIds = salesItems.stream().map(SalesItem::getFlowerId).collect(Collectors.toSet());
        Map<Integer, Flower> flowerMap = flowerIds.isEmpty()
                ? Collections.emptyMap()
                : flowerMapper.selectBatchIds(flowerIds).stream()
                .collect(Collectors.toMap(Flower::getId, flower -> flower, (left, right) -> left, LinkedHashMap::new));

        List<SalesVO> result = new ArrayList<>(records.size());
        for (Sales sales : records) {
            Vip vip = sales.getVipId() == null ? null : vipMap.get(sales.getVipId());
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

    private SalesItemVO toSalesItemVO(SalesItem item, Flower flower) {
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
        if (vipId != null && vipMapper.selectById(vipId) == null) {
            throw new BusinessException("所选会员不存在，请刷新后重试！");
        }
    }

    private Flower requireFlower(Integer flowerId) {
        Flower flower = flowerMapper.selectById(flowerId);
        if (flower == null) {
            throw new BusinessException("花卉不存在或已下架！");
        }
        return flower;
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

    private int safeStock(Flower flower) {
        return flower.getCurrentStock() == null ? 0 : flower.getCurrentStock();
    }

    private BigDecimal sumBigDecimal(List<BigDecimal> values) {
        return values.stream()
                .filter(value -> value != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}


