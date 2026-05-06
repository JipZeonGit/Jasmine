package com.nfu.jasmine.vip.application.impl;

import com.nfu.jasmine.vip.model.entity.Vip;
import com.nfu.jasmine.vip.persistence.mapper.VipMapper;
import com.nfu.jasmine.vip.application.IVipService;
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
public class VipServiceImpl extends ServiceImpl<VipMapper, Vip> implements IVipService {

}
