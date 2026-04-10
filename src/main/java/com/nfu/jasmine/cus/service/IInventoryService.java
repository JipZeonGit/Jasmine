package com.nfu.jasmine.cus.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.nfu.jasmine.common.vo.TableData;
import com.nfu.jasmine.cus.dto.InventoryQueryDTO;
import com.nfu.jasmine.cus.dto.InventorySaveDTO;
import com.nfu.jasmine.cus.entity.Inventory;
import com.nfu.jasmine.cus.vo.InventoryVO;

import java.util.List;

public interface IInventoryService extends IService<Inventory> {
    List<InventoryVO> listInventory();

    TableData<InventoryVO> pageInventory(InventoryQueryDTO queryDTO);

    InventoryVO getInventoryDetail(Integer id);

    void saveInventory(InventorySaveDTO inventoryDTO, Integer operatorId);

    void updateInventory(InventorySaveDTO inventoryDTO, Integer operatorId);

    void deleteInventory(Integer id);
}
