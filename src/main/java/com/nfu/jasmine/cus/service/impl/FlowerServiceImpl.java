package com.nfu.jasmine.cus.service.impl;

import com.nfu.jasmine.cus.entity.Flower;
import com.nfu.jasmine.cus.mapper.FlowerMapper;
import com.nfu.jasmine.cus.service.IFlowerService;
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
public class FlowerServiceImpl extends ServiceImpl<FlowerMapper, Flower> implements IFlowerService {

}
