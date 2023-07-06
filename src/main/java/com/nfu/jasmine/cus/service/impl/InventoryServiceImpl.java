package com.nfu.jasmine.cus.service.impl;

import com.nfu.jasmine.cus.entity.Inventory;
import com.nfu.jasmine.cus.mapper.InventoryMapper;
import com.nfu.jasmine.cus.service.IInventoryService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author jipzeongit
 * @since 2023-07-06
 */
@Service
public class InventoryServiceImpl extends ServiceImpl<InventoryMapper, Inventory> implements IInventoryService {

}
