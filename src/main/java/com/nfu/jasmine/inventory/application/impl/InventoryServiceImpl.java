package com.nfu.jasmine.inventory.application.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nfu.jasmine.common.exception.BusinessException;
import com.nfu.jasmine.common.utils.BusinessNoUtil;
import com.nfu.jasmine.common.vo.TableData;
import com.nfu.jasmine.infra.cache.CacheNames;
import com.nfu.jasmine.inventory.web.dto.InventoryQueryDTO;
import com.nfu.jasmine.inventory.web.dto.InventorySaveDTO;
import com.nfu.jasmine.flower.model.entity.Flower;
import com.nfu.jasmine.inventory.model.entity.Inventory;
import com.nfu.jasmine.inventory.model.enumtype.InventoryBizType;
import com.nfu.jasmine.flower.persistence.mapper.FlowerMapper;
import com.nfu.jasmine.inventory.persistence.mapper.InventoryMapper;
import com.nfu.jasmine.inventory.application.IInventoryService;
import com.nfu.jasmine.inventory.web.vo.InventoryVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class InventoryServiceImpl extends ServiceImpl<InventoryMapper, Inventory> implements IInventoryService {
    @Autowired
    private FlowerMapper flowerMapper;

    @Override
    public List<InventoryVO> listInventory() {
        LambdaQueryWrapper<Inventory> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(Inventory::getDate).orderByDesc(Inventory::getId);
        return buildInventoryVOs(this.list(wrapper));
    }

    @Override
    public TableData<InventoryVO> pageInventory(InventoryQueryDTO queryDTO) {
        LambdaQueryWrapper<Inventory> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(Inventory::getDate).orderByDesc(Inventory::getId);

        if (StringUtils.hasLength(queryDTO.getNum())) {
            wrapper.like(Inventory::getBizNo, queryDTO.getNum());
        }
        if (StringUtils.hasLength(queryDTO.getBizType())) {
            wrapper.eq(Inventory::getBizType, queryDTO.getBizType());
        }
        if (queryDTO.getStartTime() != null) {
            wrapper.ge(Inventory::getDate, queryDTO.getStartTime());
        }
        if (queryDTO.getEndTime() != null) {
            wrapper.le(Inventory::getDate, queryDTO.getEndTime());
        }
        if (StringUtils.hasLength(queryDTO.getName())) {
            List<Integer> flowerIds = flowerMapper.selectList(new LambdaQueryWrapper<Flower>()
                            .like(Flower::getName, queryDTO.getName()))
                    .stream()
                    .map(Flower::getId)
                    .toList();
            if (flowerIds.isEmpty()) {
                TableData<InventoryVO> empty = new TableData<>();
                empty.setTotal(0L);
                empty.setRows(Collections.emptyList());
                return empty;
            }
            wrapper.in(Inventory::getFlowerId, flowerIds);
        }

        Page<Inventory> page = new Page<>(queryDTO.getPageNo(), queryDTO.getPageSize());
        this.page(page, wrapper);

        TableData<InventoryVO> data = new TableData<>();
        data.setTotal(page.getTotal());
        data.setRows(buildInventoryVOs(page.getRecords()));
        return data;
    }

    @Override
    public InventoryVO getInventoryDetail(Integer id) {
        Inventory inventory = requireInventory(id);
        return buildInventoryVOs(List.of(inventory)).stream().findFirst().orElse(null);
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = CacheNames.FLOWER_LIST, allEntries = true),
            @CacheEvict(value = CacheNames.FLOWER_DETAIL, allEntries = true)
    })
    public void saveInventory(InventorySaveDTO inventoryDTO, Integer operatorId) {
        Flower flower = requireFlower(inventoryDTO.getFlowerId());
        InventoryBizType bizType = InventoryBizType.fromCode(inventoryDTO.getBizType());
        int quantity = requirePositiveQuantity(inventoryDTO.getQuantity());

        // 当前库存永远从花卉主数据读取，再由本次业务动作推导出变动后库存。
        int beforeStock = safeStock(flower);
        int afterStock = beforeStock + bizType.apply(quantity);
        ensureStockNotNegative(afterStock, flower.getName());

        BigDecimal unitCost = resolveUnitCost(bizType, inventoryDTO.getUnitCost());
        BigDecimal totalCost = buildTotalCost(unitCost, quantity);

        flower.setCurrentStock(afterStock);
        if (bizType == InventoryBizType.PURCHASE_IN && unitCost != null) {
          flower.setCostPrice(unitCost);
        }
        flowerMapper.updateById(flower);

        Inventory inventory = new Inventory();
        inventory.setBizNo(BusinessNoUtil.generateInventoryBizNo());
        inventory.setFlowerId(flower.getId());
        inventory.setBizType(bizType.name());
        inventory.setQuantity(quantity);
        inventory.setBeforeStock(beforeStock);
        inventory.setAfterStock(afterStock);
        inventory.setUnitCost(unitCost);
        inventory.setTotalCost(totalCost);
        inventory.setRemark(inventoryDTO.getRemark());
        inventory.setOperatorId(operatorId);
        inventory.setDate(inventoryDTO.getDate());
        inventory.setDeleted(0);
        this.save(inventory);
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = CacheNames.FLOWER_LIST, allEntries = true),
            @CacheEvict(value = CacheNames.FLOWER_DETAIL, allEntries = true)
    })
    public void updateInventory(InventorySaveDTO inventoryDTO, Integer operatorId) {
        Inventory existing = requireInventory(inventoryDTO.getId());
        InventoryBizType oldBizType = InventoryBizType.fromCode(existing.getBizType());
        int oldDelta = oldBizType.apply(existing.getQuantity());

        InventoryBizType newBizType = InventoryBizType.fromCode(inventoryDTO.getBizType());
        int newQuantity = requirePositiveQuantity(inventoryDTO.getQuantity());
        BigDecimal unitCost = resolveUnitCost(newBizType, inventoryDTO.getUnitCost());
        BigDecimal totalCost = buildTotalCost(unitCost, newQuantity);

        if (Objects.equals(existing.getFlowerId(), inventoryDTO.getFlowerId())) {
            Flower flower = requireFlower(existing.getFlowerId());
            int baseStock = safeStock(flower) - oldDelta;
            ensureStockNotNegative(baseStock, flower.getName());

            int afterStock = baseStock + newBizType.apply(newQuantity);
            ensureStockNotNegative(afterStock, flower.getName());

            flower.setCurrentStock(afterStock);
            if (newBizType == InventoryBizType.PURCHASE_IN && unitCost != null) {
                flower.setCostPrice(unitCost);
            }
            flowerMapper.updateById(flower);

            existing.setFlowerId(flower.getId());
            existing.setBizType(newBizType.name());
            existing.setQuantity(newQuantity);
            existing.setBeforeStock(baseStock);
            existing.setAfterStock(afterStock);
        } else {
            Flower oldFlower = requireFlower(existing.getFlowerId());
            int restoredOldStock = safeStock(oldFlower) - oldDelta;
            ensureStockNotNegative(restoredOldStock, oldFlower.getName());
            oldFlower.setCurrentStock(restoredOldStock);
            flowerMapper.updateById(oldFlower);

            Flower newFlower = requireFlower(inventoryDTO.getFlowerId());
            int beforeStock = safeStock(newFlower);
            int afterStock = beforeStock + newBizType.apply(newQuantity);
            ensureStockNotNegative(afterStock, newFlower.getName());
            newFlower.setCurrentStock(afterStock);
            if (newBizType == InventoryBizType.PURCHASE_IN && unitCost != null) {
                newFlower.setCostPrice(unitCost);
            }
            flowerMapper.updateById(newFlower);

            existing.setFlowerId(newFlower.getId());
            existing.setBizType(newBizType.name());
            existing.setQuantity(newQuantity);
            existing.setBeforeStock(beforeStock);
            existing.setAfterStock(afterStock);
        }

        existing.setUnitCost(unitCost);
        existing.setTotalCost(totalCost);
        existing.setRemark(inventoryDTO.getRemark());
        existing.setOperatorId(operatorId);
        existing.setDate(inventoryDTO.getDate());
        this.updateById(existing);
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = CacheNames.FLOWER_LIST, allEntries = true),
            @CacheEvict(value = CacheNames.FLOWER_DETAIL, allEntries = true)
    })
    public void deleteInventory(Integer id) {
        Inventory existing = requireInventory(id);
        Flower flower = requireFlower(existing.getFlowerId());
        int delta = InventoryBizType.fromCode(existing.getBizType()).apply(existing.getQuantity());
        int afterStock = safeStock(flower) - delta;
        ensureStockNotNegative(afterStock, flower.getName());

        flower.setCurrentStock(afterStock);
        flowerMapper.updateById(flower);
        this.removeById(id);
    }

    private List<InventoryVO> buildInventoryVOs(List<Inventory> records) {
        if (records == null || records.isEmpty()) {
            return Collections.emptyList();
        }

        // 列表展示需要花名，但流水表只存 flowerId，这里统一做一次批量回填。
        Set<Integer> flowerIds = records.stream().map(Inventory::getFlowerId).collect(Collectors.toSet());
        Map<Integer, Flower> flowerMap = flowerMapper.selectBatchIds(flowerIds).stream()
                .collect(Collectors.toMap(Flower::getId, flower -> flower, (left, right) -> left, LinkedHashMap::new));

        List<InventoryVO> result = new ArrayList<>(records.size());
        for (Inventory inventory : records) {
            Flower flower = flowerMap.get(inventory.getFlowerId());
            InventoryBizType bizType = InventoryBizType.fromCode(inventory.getBizType());
            InventoryVO vo = new InventoryVO();
            vo.setId(inventory.getId());
            vo.setBizNo(inventory.getBizNo());
            vo.setFlowerId(inventory.getFlowerId());
            vo.setFlowerName(flower == null ? "未知花卉" : flower.getName());
            vo.setBizType(inventory.getBizType());
            vo.setBizTypeLabel(bizType.getLabel());
            vo.setQuantity(inventory.getQuantity());
            vo.setBeforeStock(inventory.getBeforeStock());
            vo.setAfterStock(inventory.getAfterStock());
            vo.setUnitCost(inventory.getUnitCost());
            vo.setTotalCost(inventory.getTotalCost());
            vo.setRemark(inventory.getRemark());
            vo.setDate(inventory.getDate());
            result.add(vo);
        }
        return result;
    }

    private Inventory requireInventory(Integer id) {
        Inventory inventory = this.getById(id);
        if (inventory == null) {
            throw new BusinessException("库存流水不存在！");
        }
        return inventory;
    }

    private Flower requireFlower(Integer flowerId) {
        Flower flower = flowerMapper.selectById(flowerId);
        if (flower == null) {
            throw new BusinessException("花卉不存在或已下架！");
        }
        return flower;
    }

    private int requirePositiveQuantity(Integer quantity) {
        if (quantity == null || quantity <= 0) {
            throw new BusinessException("变动数量必须大于 0！");
        }
        return quantity;
    }

    private int safeStock(Flower flower) {
        return flower.getCurrentStock() == null ? 0 : flower.getCurrentStock();
    }

    private void ensureStockNotNegative(int stock, String flowerName) {
        if (stock < 0) {
            throw new BusinessException(flowerName + "库存不足，请先补货后再操作！");
        }
    }

    private BigDecimal resolveUnitCost(InventoryBizType bizType, BigDecimal unitCost) {
        if (bizType == InventoryBizType.PURCHASE_IN) {
            if (unitCost == null || unitCost.compareTo(BigDecimal.ZERO) <= 0) {
                throw new BusinessException("采购入库必须填写有效的进货单价！");
            }
            return unitCost;
        }
        return null;
    }

    private BigDecimal buildTotalCost(BigDecimal unitCost, Integer quantity) {
        if (unitCost == null || quantity == null) {
            return null;
        }
        return unitCost.multiply(BigDecimal.valueOf(quantity));
    }
}


