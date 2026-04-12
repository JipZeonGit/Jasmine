package com.nfu.jasmine.flower.application.impl;

import com.nfu.jasmine.flower.model.entity.Flower;
import com.nfu.jasmine.flower.persistence.mapper.FlowerMapper;
import com.nfu.jasmine.flower.application.IFlowerService;
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
