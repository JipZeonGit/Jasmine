package com.nfu.jasmine.sys.service.impl;

import com.nfu.jasmine.sys.entity.User;
import com.nfu.jasmine.sys.mapper.UserMapper;
import com.nfu.jasmine.sys.service.IUserService;
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
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {

}
