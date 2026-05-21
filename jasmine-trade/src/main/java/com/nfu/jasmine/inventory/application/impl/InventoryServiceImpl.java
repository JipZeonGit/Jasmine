package com.nfu.jasmine.inventory.application.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nfu.jasmine.common.dto.internal.FlowerDTO;
import com.nfu.jasmine.common.dto.internal.ProductStockFacade;
import com.nfu.jasmine.common.exception.BusinessException;
import com.nfu.jasmine.common.utils.BusinessNoUtil;
import com.nfu.jasmine.common.vo.TableData;
import com.nfu.jasmine.infra.client.FlowerClient;
import com.nfu.jasmine.infra.mq.message.InventoryChangedMessage;
import com.nfu.jasmine.infra.mq.message.InventoryChangeSource;
import com.nfu.jasmine.infra.mq.publisher.MqMessagePublisher;
import com.nfu.jasmine.inventory.application.IInventoryService;
import com.nfu.jasmine.inventory.model.entity.Inventory;
import com.nfu.jasmine.inventory.model.enumtype.InventoryBizType;
import com.nfu.jasmine.inventory.persistence.mapper.InventoryMapper;
import com.nfu.jasmine.inventory.web.dto.InventoryQueryDTO;
import com.nfu.jasmine.inventory.web.dto.InventorySaveDTO;
import com.nfu.jasmine.inventory.web.vo.InventoryVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class InventoryServiceImpl extends ServiceImpl<InventoryMapper, Inventory> implements IInventoryService {
    @Autowired
    private FlowerClient flowerClient;

    @Autowired
    private MqMessagePublisher mqMessagePublisher;

    @Autowired
    private ProductStockFacade productStockFacade;

    @Autowired
    private com.nfu.jasmine.inventory.alert.service.InventoryAlertService inventoryAlertService;

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
            // 通过远程接口按花卉名称模糊查询花卉ID列表
            List<Integer> flowerIds = flowerClient.getFlowerIdsByName(queryDTO.getName());
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
    public void saveInventory(InventorySaveDTO inventoryDTO, Integer operatorId) {
        InventoryBizType bizType = InventoryBizType.fromCode(inventoryDTO.getBizType());
        int quantity = requirePositiveQuantity(inventoryDTO.getQuantity());

        BigDecimal unitCost = resolveUnitCost(bizType, inventoryDTO.getUnitCost());
        BigDecimal totalCost = buildTotalCost(unitCost, quantity);

        // 先查花卉名称用于错误提示
        FlowerDTO flowerInfo = flowerClient.getFlowerById(inventoryDTO.getFlowerId());

        ProductStockFacade.StockChangeResult stockChange = productStockFacade.adjustStock(
                inventoryDTO.getFlowerId(),
                bizType.apply(quantity),
                unitCost,
                bizType == InventoryBizType.PURCHASE_IN && unitCost != null,
                flowerInfo.getName() + "库存不足，请先补货后再操作！"
        );

        Inventory inventory = new Inventory();
        inventory.setBizNo(BusinessNoUtil.generateInventoryBizNo());
        inventory.setFlowerId(stockChange.flower().getId());
        inventory.setBizType(bizType.name());
        inventory.setQuantity(quantity);
        inventory.setBeforeStock(stockChange.beforeStock());
        inventory.setAfterStock(stockChange.afterStock());
        inventory.setUnitCost(unitCost);
        inventory.setTotalCost(totalCost);
        inventory.setRemark(inventoryDTO.getRemark());
        inventory.setOperatorId(operatorId);
        inventory.setDate(inventoryDTO.getDate());
        inventory.setDeleted(0);
        this.save(inventory);

        // 手工库存动作也统一补事件，这样采购、损耗、退货和盘点都能复用同一条消息主链。
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
                InventoryChangeSource.MANUAL_INVENTORY,
                InventoryChangeSource.ACTION_CREATE
        ));
    }

    @Override
    @Transactional
    public void updateInventory(InventorySaveDTO inventoryDTO, Integer operatorId) {
        Inventory existing = requireInventory(inventoryDTO.getId());
        // 先计算旧流水对库存的净影响（入库为正、出库为负），后面用来回滚再重算。
        InventoryBizType oldBizType = InventoryBizType.fromCode(existing.getBizType());
        int oldDelta = oldBizType.apply(existing.getQuantity());

        InventoryBizType newBizType = InventoryBizType.fromCode(inventoryDTO.getBizType());
        int newQuantity = requirePositiveQuantity(inventoryDTO.getQuantity());
        BigDecimal unitCost = resolveUnitCost(newBizType, inventoryDTO.getUnitCost());
        BigDecimal totalCost = buildTotalCost(unitCost, newQuantity);

        if (Objects.equals(existing.getFlowerId(), inventoryDTO.getFlowerId())) {
            // 同一花卉：先回滚旧影响 -> 得到基准库存 -> 再叠加新影响。
            FlowerDTO flowerInfo = flowerClient.getFlowerById(existing.getFlowerId());
            ProductStockFacade.StockChangeResult stockChange = productStockFacade.adjustStock(
                    existing.getFlowerId(),
                    newBizType.apply(newQuantity) - oldDelta,
                    unitCost,
                    newBizType == InventoryBizType.PURCHASE_IN && unitCost != null,
                    flowerInfo.getName() + "库存不足，请先补货后再操作！"
            );
            int baseStock = stockChange.beforeStock() - oldDelta;
            int afterStock = stockChange.afterStock();

            existing.setBizType(newBizType.name());
            existing.setQuantity(newQuantity);
            existing.setBeforeStock(baseStock);
            existing.setAfterStock(afterStock);
        } else {
            // 换花卉：旧花卉回滚库存，新花卉按新业务类型重新计算库存。
            productStockFacade.adjustStock(existing.getFlowerId(), -oldDelta, null, false, null);
            FlowerDTO newFlowerInfo = flowerClient.getFlowerById(inventoryDTO.getFlowerId());
            ProductStockFacade.StockChangeResult stockChange = productStockFacade.adjustStock(
                    inventoryDTO.getFlowerId(),
                    newBizType.apply(newQuantity),
                    unitCost,
                    newBizType == InventoryBizType.PURCHASE_IN && unitCost != null,
                    newFlowerInfo.getName() + "库存不足，请先补货后再操作！"
            );

            existing.setFlowerId(inventoryDTO.getFlowerId());
            existing.setBizType(newBizType.name());
            existing.setQuantity(newQuantity);
            existing.setBeforeStock(stockChange.beforeStock());
            existing.setAfterStock(stockChange.afterStock());
        }

        existing.setUnitCost(unitCost);
        existing.setTotalCost(totalCost);
        existing.setRemark(inventoryDTO.getRemark());
        existing.setOperatorId(operatorId);
        existing.setDate(inventoryDTO.getDate());
        this.updateById(existing);

        // 库存修改后也需要发事件，让下游能感知到库存变动。
        mqMessagePublisher.publishInventoryChangedAfterCommit(new InventoryChangedMessage(
                existing.getId(),
                existing.getBizNo(),
                existing.getFlowerId(),
                existing.getBizType(),
                existing.getQuantity(),
                existing.getBeforeStock(),
                existing.getAfterStock(),
                existing.getOperatorId(),
                existing.getDate(),
                new Date(),
                InventoryChangeSource.MANUAL_INVENTORY,
                InventoryChangeSource.ACTION_UPDATE
        ));
    }

    @Override
    @Transactional
    public void deleteInventory(Integer id) {
        Inventory existing = requireInventory(id);
        int delta = InventoryBizType.fromCode(existing.getBizType()).apply(existing.getQuantity());
        productStockFacade.adjustStock(existing.getFlowerId(), -delta, null, false, null);
        this.removeById(id);

        // 库存删除后也需要发事件，确保事件完整性。
        mqMessagePublisher.publishInventoryChangedAfterCommit(new InventoryChangedMessage(
                existing.getId(),
                existing.getBizNo(),
                existing.getFlowerId(),
                existing.getBizType(),
                -existing.getQuantity(),
                existing.getAfterStock(),
                existing.getBeforeStock(),
                existing.getOperatorId(),
                existing.getDate(),
                new Date(),
                InventoryChangeSource.MANUAL_INVENTORY,
                InventoryChangeSource.ACTION_DELETE
        ));
    }

    private List<InventoryVO> buildInventoryVOs(List<Inventory> records) {
        if (records == null || records.isEmpty()) {
            return Collections.emptyList();
        }

        // 列表展示需要花名，但流水表只存 flowerId，通过远程接口批量回填。
        Set<Integer> flowerIds = records.stream().map(Inventory::getFlowerId).collect(Collectors.toSet());
        Map<Integer, FlowerDTO> flowerMap = flowerClient.getFlowersByIds(new ArrayList<>(flowerIds)).stream()
                .collect(Collectors.toMap(FlowerDTO::getId, f -> f, (left, right) -> left, LinkedHashMap::new));

        List<InventoryVO> result = new ArrayList<>(records.size());
        for (Inventory inventory : records) {
            FlowerDTO flower = flowerMap.get(inventory.getFlowerId());
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

    private int requirePositiveQuantity(Integer quantity) {
        if (quantity == null || quantity <= 0) {
            throw new BusinessException("变动数量必须大于 0！");
        }
        return quantity;
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

    @Override
    public Long getLowStockCount() {
        return inventoryAlertService.getLowStockCount();
    }
}
