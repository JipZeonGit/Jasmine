package com.nfu.jasmine.iam.application.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.nfu.jasmine.iam.model.entity.Menu;
import com.nfu.jasmine.iam.persistence.mapper.MenuMapper;
import com.nfu.jasmine.iam.application.IMenuService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nfu.jasmine.infra.cache.CacheNames;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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
        List<Menu> allMenus = this.list(new LambdaQueryWrapper<Menu>().orderByAsc(Menu::getParentId).orderByAsc(Menu::getMenuId));
        return buildMenuTree(allMenus);
    }

    @Override
    @Cacheable(value = CacheNames.MENU_LIST, key = "#userId", sync = true)
    public List<Menu> getMenuListByUserId(Integer userId) {
        List<Menu> grantedMenus = this.baseMapper.getAllMenusByUserId(userId);
        return buildMenuTree(grantedMenus);
    }

    private List<Menu> buildMenuTree(List<Menu> menus) {
        Map<Integer, List<Menu>> childrenMap = new LinkedHashMap<>();
        for (Menu menu : menus) {
            menu.setChildren(new ArrayList<>());
            int parentId = Optional.ofNullable(menu.getParentId()).orElse(0);
            childrenMap.computeIfAbsent(parentId, ignored -> new ArrayList<>()).add(menu);
        }

        List<Menu> rootMenus = childrenMap.getOrDefault(0, new ArrayList<>());
        attachChildren(rootMenus, childrenMap);
        return rootMenus;
    }

    private void attachChildren(List<Menu> menus, Map<Integer, List<Menu>> childrenMap) {
        for (Menu menu : menus) {
            List<Menu> children = childrenMap.getOrDefault(menu.getMenuId(), new ArrayList<>());
            menu.setChildren(children);
            attachChildren(children, childrenMap);
        }
    }

}
