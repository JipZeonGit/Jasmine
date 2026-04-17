package com.nfu.jasmine.iam.application.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.nfu.jasmine.iam.model.entity.Menu;
import com.nfu.jasmine.iam.persistence.mapper.MenuMapper;
import com.nfu.jasmine.iam.application.IMenuService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nfu.jasmine.infra.cache.CacheNames;
import org.springframework.stereotype.Service;

import java.util.List;

import org.springframework.cache.annotation.Cacheable;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author jipzeongit
 * @since 2023-05-29
 */
@Service
public class MenuServiceImpl extends ServiceImpl<MenuMapper, Menu> implements IMenuService {

    @Override
    public List<Menu> getAllMenu() {
        // 一级菜单
        LambdaQueryWrapper<Menu> wrapper = new LambdaQueryWrapper();
        wrapper.eq(Menu::getParentId,0);
        List<Menu> menuList = this.list(wrapper);
        // 子菜单
        setMenuChildren(menuList);
        return menuList;
    }

    private void setMenuChildren(List<Menu> menuList) {
        if(menuList != null) {
            for (Menu menu:menuList) {
                LambdaQueryWrapper<Menu> subWrapper = new LambdaQueryWrapper();
                subWrapper.eq(Menu::getParentId, menu.getMenuId());
                List<Menu> subMenuList = this.list(subWrapper);
                menu.setChildren(subMenuList);
                // 递归
                setMenuChildren(subMenuList);
            }
        }
    }

    @Override
    @Cacheable(value = CacheNames.MENU_LIST, key = "#userId", sync = true)
    public List<Menu> getMenuListByUserId(Integer userId) {
        // 一级菜单
        List<Menu> menuList = this.baseMapper.getMenuListByUserId(userId,0);
        // 二级菜单
        setMenuChildrenByUserId(userId, menuList);
        return menuList;
    }

    private void setMenuChildrenByUserId(Integer userId, List<Menu> menuList) {
        if(menuList != null){
            for(Menu menu : menuList){
                List<Menu> subMenuList = this.baseMapper.getMenuListByUserId(userId,menu.getMenuId());
                menu.setChildren(subMenuList);
                //递归
                setMenuChildrenByUserId(userId,subMenuList);
            }
        }
    }


}
