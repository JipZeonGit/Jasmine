package com.nfu.jasmine.sys.service;

import com.nfu.jasmine.sys.entity.Menu;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author jipzeongit
 * @since 2023-05-29
 */
public interface IMenuService extends IService<Menu> {

    List<Menu> getAllMenu();
}
