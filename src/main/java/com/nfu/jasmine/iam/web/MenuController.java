package com.nfu.jasmine.iam.web;

import com.nfu.jasmine.common.vo.Result;
import com.nfu.jasmine.iam.model.entity.Menu;
import com.nfu.jasmine.iam.application.IMenuService;
import com.nfu.jasmine.iam.web.vo.MenuVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

/**
 * <p>
 * 前端控制器
 * </p>
 *
 * @author jipzeongit
 * @since 2023-05-29
 */
@Tag(name = "导航栏接口列表")
@RestController
@RequestMapping("/menu")
public class MenuController {
    @Autowired
    private IMenuService menuService;

    // 一次性获取系统中所有的菜单和权限树
    @Operation(summary = "获取全部权限")
    @GetMapping
    public Result<List<MenuVO>> getAllMenu() {
        List<MenuVO> menuList = menuService.getAllMenu().stream().map(this::toMenuVO).collect(Collectors.toList());
        return Result.success(menuList);
    }

    private MenuVO toMenuVO(Menu menu) {
        if (menu == null) {
            return null;
        }
        MenuVO vo = new MenuVO();
        BeanUtils.copyProperties(menu, vo);
        if (menu.getChildren() != null) {
            vo.setChildren(menu.getChildren().stream().map(this::toMenuVO).collect(Collectors.toList()));
        }
        return vo;
    }
}