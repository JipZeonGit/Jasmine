package com.nfu.jasmine.iam.application.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nfu.jasmine.iam.model.entity.Role;
import com.nfu.jasmine.iam.model.entity.RoleMenu;
import com.nfu.jasmine.iam.persistence.mapper.RoleMapper;
import com.nfu.jasmine.iam.persistence.mapper.RoleMenuMapper;
import com.nfu.jasmine.iam.application.IRoleService;
import com.nfu.jasmine.infra.cache.CacheNames;
import jakarta.annotation.Resource;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author jipzeongit
 * @since 2023-05-29
 */
@Service
public class RoleServiceImpl extends ServiceImpl<RoleMapper, Role> implements IRoleService {

    @Resource
    private RoleMenuMapper roleMenuMapper;

    @Override
    @Cacheable(value = CacheNames.ROLE_LIST, key = "'all'")
    public List<Role> listAllRoles() {
        return this.list(new LambdaQueryWrapper<Role>().orderByAsc(Role::getRoleId));
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = CacheNames.ROLE_LIST, allEntries = true),
            @CacheEvict(value = CacheNames.MENU_LIST, allEntries = true)
    })
    public void addRole(Role role) {
        // 写入角色表
        this.baseMapper.insert(role);
        // 写入权限对照表
        if (null != role.getMenuIdList()) {
            for (Integer menuId : role.getMenuIdList()) {
                roleMenuMapper.insert(new RoleMenu(null, role.getRoleId(), menuId));
            }
        }
    }

    @Override
    public Role getRoleById(Integer id) {
        Role role = this.baseMapper.selectById(id);
        List<Integer> menuIdList = roleMenuMapper.getMenuIdListByRoleId(id);
        role.setMenuIdList(menuIdList);
        return role;
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = CacheNames.ROLE_LIST, allEntries = true),
            @CacheEvict(value = CacheNames.MENU_LIST, allEntries = true)
    })
    public void updateRole(Role role) {
        // 修改角色表
        this.baseMapper.updateById(role);
        // 删除原有权限
        LambdaQueryWrapper<RoleMenu> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RoleMenu::getRoleId, role.getRoleId());
        roleMenuMapper.delete(wrapper);
        // 新增角色权限
        if (null != role.getMenuIdList()) {
            for (Integer menuId : role.getMenuIdList()) {
                roleMenuMapper.insert(new RoleMenu(null, role.getRoleId(), menuId));
            }
        }
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = CacheNames.ROLE_LIST, allEntries = true),
            @CacheEvict(value = CacheNames.MENU_LIST, allEntries = true)
    })
    public void deleteRoleById(Integer id) {
        this.baseMapper.deleteById(id);
        // 删除原有权限
        LambdaQueryWrapper<RoleMenu> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RoleMenu::getRoleId, id);
        roleMenuMapper.delete(wrapper);
    }
}
