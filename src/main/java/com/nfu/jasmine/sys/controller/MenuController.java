package com.nfu.jasmine.sys.controller;

import com.nfu.jasmine.common.vo.Result;
import com.nfu.jasmine.sys.entity.Menu;
import com.nfu.jasmine.sys.service.IMenuService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

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

    @Operation(summary = "获取全部权限")
    @GetMapping
    public Result<?> getAllMenu() {
        List<Menu> menuList = menuService.getAllMenu();
        return Result.success(menuList);
    }
}
