package com.nfu.jasmine.iam.application.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.nfu.jasmine.iam.model.entity.Role;
import com.nfu.jasmine.iam.model.entity.RoleMenu;
import com.nfu.jasmine.iam.persistence.mapper.RoleMapper;
import com.nfu.jasmine.iam.persistence.mapper.RoleMenuMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * RoleServiceImpl 单测 —— 覆盖角色增删改查 + role_menu 关联表维护逻辑。
 * 缓存注解（@Cacheable/@CacheEvict）在纯单测不触发（无 Spring 上下文），
 * 单测只验证 Service 方法体内的业务逻辑。
 */
@ExtendWith(MockitoExtension.class)
class RoleServiceImplTest {

    @Mock
    private RoleMapper roleMapper;
    @Mock
    private RoleMenuMapper roleMenuMapper;

    @InjectMocks
    private RoleServiceImpl roleService;

    @BeforeEach
    void setUp() {
        // ServiceImpl 需要通过反射注入 baseMapper
        ReflectionTestUtils.setField(roleService, "baseMapper", roleMapper);
    }

    // listAllRoles：按 roleId 升序返回
    @Test
    void listAllRolesShouldReturnOrderedRoles() {
        Role admin = new Role(1, "admin", "管理员", null);
        Role boss = new Role(2, "Boss", "老板", null);
        when(roleMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(admin, boss));

        List<Role> roles = roleService.listAllRoles();

        assertThat(roles).hasSize(2);
        assertThat(roles.get(0).getRoleId()).isEqualTo(1);
        assertThat(roles.get(1).getRoleId()).isEqualTo(2);
    }

    // addRole：写入角色表 + 批量写入 role_menu
    @Test
    void addRoleShouldInsertRoleAndRoleMenus() {
        Role role = new Role();
        role.setRoleId(1);
        role.setRoleName("clerk");
        role.setMenuIdList(List.of(10, 20, 30));

        roleService.addRole(role);

        verify(roleMapper).insert((Role) any());
        // 验证批量插入了 3 条 role_menu
        verify(roleMenuMapper, times(3)).insert(any(RoleMenu.class));
    }

    // addRole：menuIdList 为 null 时不插 role_menu
    @Test
    void addRoleShouldSkipRoleMenuWhenMenuIdListNull() {
        Role role = new Role();
        role.setRoleId(1);
        role.setMenuIdList(null);

        roleService.addRole(role);

        verify(roleMapper).insert((Role) any());
        verify(roleMenuMapper, never()).insert(any(RoleMenu.class));
    }

    // getRoleById：查询角色 + 查询菜单 ID 列表
    @Test
    void getRoleByIdShouldReturnRoleWithMenuIds() {
        Role role = new Role(1, "admin", "管理员", null);
        when(roleMapper.selectById(1)).thenReturn(role);
        when(roleMenuMapper.getMenuIdListByRoleId(1)).thenReturn(List.of(10, 20));

        Role result = roleService.getRoleById(1);

        assertThat(result.getRoleId()).isEqualTo(1);
        assertThat(result.getMenuIdList()).containsExactly(10, 20);
    }

    // updateRole：先删旧 role_menu 再插新的
    @Test
    void updateRoleShouldDeleteOldMenusThenInsertNew() {
        Role role = new Role();
        role.setRoleId(1);
        role.setMenuIdList(List.of(10, 20));

        roleService.updateRole(role);

        verify(roleMapper).updateById(role);
        // 先删后插
        verify(roleMenuMapper).delete(any(LambdaQueryWrapper.class));
        verify(roleMenuMapper, times(2)).insert(any(RoleMenu.class));
    }

    // deleteRoleById：删角色 + 级联删 role_menu
    @Test
    void deleteRoleByIdShouldDeleteRoleAndCascadeRoleMenus() {
        roleService.deleteRoleById(1);

        verify(roleMapper).deleteById(1);
        verify(roleMenuMapper).delete(any(LambdaQueryWrapper.class));
    }
}
