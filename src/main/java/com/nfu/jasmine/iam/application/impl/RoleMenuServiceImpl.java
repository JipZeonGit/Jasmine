package com.nfu.jasmine.iam.application.impl;

import com.nfu.jasmine.iam.model.entity.RoleMenu;
import com.nfu.jasmine.iam.persistence.mapper.RoleMenuMapper;
import com.nfu.jasmine.iam.application.IRoleMenuService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author jipzeongit
 * @since 2023-05-29
 */
@Service
public class RoleMenuServiceImpl extends ServiceImpl<RoleMenuMapper, RoleMenu> implements IRoleMenuService {

}
