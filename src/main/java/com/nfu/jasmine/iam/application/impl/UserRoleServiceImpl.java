package com.nfu.jasmine.iam.application.impl;

import com.nfu.jasmine.iam.model.entity.UserRole;
import com.nfu.jasmine.iam.persistence.mapper.UserRoleMapper;
import com.nfu.jasmine.iam.application.IUserRoleService;
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
public class UserRoleServiceImpl extends ServiceImpl<UserRoleMapper, UserRole> implements IUserRoleService {

}
