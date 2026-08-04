package com.nfu.jasmine.iam.application.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.nfu.jasmine.iam.model.entity.Menu;
import com.nfu.jasmine.iam.persistence.mapper.MenuMapper;
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
import static org.mockito.Mockito.when;

/**
 * MenuServiceImpl 单测 —— 覆盖菜单树构建逻辑。
 * 缓存注解（@Cacheable）在纯单测不触发（无 Spring 上下文），
 * 单测只验证 buildMenuTree 的树组装逻辑。
 */
@ExtendWith(MockitoExtension.class)
class MenuServiceImplTest {

    @Mock
    private MenuMapper menuMapper;

    @InjectMocks
    private MenuServiceImpl menuService;

    @BeforeEach
    void setUp() {
        // ServiceImpl 需要通过反射注入 baseMapper
        ReflectionTestUtils.setField(menuService, "baseMapper", menuMapper);
    }

    // getAllMenu：构建完整菜单树
    @Test
    void getAllMenuShouldBuildTree() {
        // parentId=0 为根，parentId=1 是 menuId=1 的子节点
        Menu root = createMenu(1, 0, "系统管理");
        Menu child = createMenu(2, 1, "用户管理");
        when(menuMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(root, child));

        List<Menu> tree = menuService.getAllMenu();

        assertThat(tree).hasSize(1);
        assertThat(tree.get(0).getMenuId()).isEqualTo(1);
        assertThat(tree.get(0).getChildren()).hasSize(1);
        assertThat(tree.get(0).getChildren().get(0).getMenuId()).isEqualTo(2);
    }

    // getMenuListByUserId：构建用户授权菜单树
    @Test
    void getMenuListByUserIdShouldBuildGrantedTree() {
        Menu root = createMenu(1, 0, "系统管理");
        when(menuMapper.getAllMenusByUserId(1)).thenReturn(List.of(root));

        List<Menu> tree = menuService.getMenuListByUserId(1);

        assertThat(tree).hasSize(1);
        assertThat(tree.get(0).getMenuId()).isEqualTo(1);
        assertThat(tree.get(0).getChildren()).isEmpty();
    }

    // buildMenuTree：空列表返回空树
    @Test
    void getAllMenuShouldReturnEmptyWhenNoMenus() {
        when(menuMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());

        List<Menu> tree = menuService.getAllMenu();

        assertThat(tree).isEmpty();
    }

    // buildMenuTree：孤儿节点（parentId 指向不存在的菜单）应被丢弃
    @Test
    void getAllMenuShouldDropOrphanNode() {
        // menuId=2 的 parentId=99，但不存在 menuId=99 的菜单 → 孤儿
        Menu orphan = createMenu(2, 99, "孤儿菜单");
        when(menuMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(orphan));

        List<Menu> tree = menuService.getAllMenu();

        // 孤儿节点不属于 parentId=0 的根，不会出现在树中
        assertThat(tree).isEmpty();
    }

    // buildMenuTree：三层嵌套
    @Test
    void getAllMenuShouldBuildThreeLevelTree() {
        Menu level1 = createMenu(1, 0, "根菜单");
        Menu level2 = createMenu(2, 1, "二级菜单");
        Menu level3 = createMenu(3, 2, "三级菜单");
        when(menuMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(List.of(level1, level2, level3));

        List<Menu> tree = menuService.getAllMenu();

        assertThat(tree).hasSize(1);
        Menu l1 = tree.get(0);
        assertThat(l1.getMenuId()).isEqualTo(1);
        assertThat(l1.getChildren()).hasSize(1);
        Menu l2 = l1.getChildren().get(0);
        assertThat(l2.getMenuId()).isEqualTo(2);
        assertThat(l2.getChildren()).hasSize(1);
        Menu l3 = l2.getChildren().get(0);
        assertThat(l3.getMenuId()).isEqualTo(3);
        assertThat(l3.getChildren()).isEmpty();
    }

    private Menu createMenu(int menuId, int parentId, String title) {
        Menu menu = new Menu();
        menu.setMenuId(menuId);
        menu.setParentId(parentId);
        menu.setTitle(title);
        return menu;
    }
}
