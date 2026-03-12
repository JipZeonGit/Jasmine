package com.nfu.jasmine.sys.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nfu.jasmine.common.vo.Result;
import com.nfu.jasmine.sys.entity.User;
import com.nfu.jasmine.sys.service.IUserService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import kotlin.jvm.internal.Lambda;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.swing.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * 前端控制器
 * </p>
 *
 * @author jipzeongit
 * @since 2023-05-29
 */
@Api(tags = { "用户接口列表" })
@RestController
@RequestMapping("/user")
public class UserController {
    @Autowired
    private IUserService userService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @ApiOperation("获取全部用户")
    @GetMapping("/all")
    public Result<List<User>> getAllUser() {
        List<User> list = userService.list();
        return Result.success(list, "查询成功");
    }

    @ApiOperation("用户登录")
    @PostMapping("/login")
    public Result<Map<String, Object>> login(@RequestBody User user) {
        Map<String, Object> data = userService.login(user);
        if (data != null) {
            return Result.success(data);
        }
        return Result.fail(20002, "用户名或密码错误！");
    }

    @ApiOperation("获取用户信息")
    @GetMapping("/info")
    public Result<Map<String, Object>> getUserInfo(@RequestParam("token") String token) {
        // 根据token获取用户信息，从Redis获取
        Map<String, Object> data = userService.getUserInfo(token);
        if (data != null) {
            return Result.success(data);
        }
        return Result.fail(20003, "用户登录信息无效，请重新登录！");
    }

    @ApiOperation("注销用户")
    @PostMapping("/logout")
    public Result<?> logout(@RequestHeader("X-Token") String token) {
        userService.logout(token);
        return Result.success();
    }

    @ApiOperation("查询用户")
    @GetMapping("/list")
    public Result<Map<String, Object>> getUserList(@RequestParam(value = "username", required = false) String username,
            @RequestParam(value = "phone", required = false) String phone, @RequestParam("pageNo") Long pageNo,
            @RequestParam("pageSize") Long pageSize) {

        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();

        wrapper.like(StringUtils.hasLength(username), User::getUsername, username);
        wrapper.like(StringUtils.hasLength(phone), User::getPhone, phone);
        wrapper.orderByAsc(User::getId); // 按照用户ID进行排序

        Page<User> page = new Page<>(pageNo, pageSize);
        userService.page(page, wrapper);

        Map<String, Object> data = new HashMap<>();
        data.put("total", page.getTotal());
        data.put("rows", page.getRecords());

        return Result.success(data);
    }

    @ApiOperation("新增用户")
    @PostMapping("")
    public Result<?> addUser(@RequestBody User user) {
        user.setPassword(passwordEncoder.encode(user.getPassword())); // 用户密码加密
        userService.addUser(user);
        return Result.success("新增用户成功！");
    }

    @ApiOperation("修改用户")
    @PutMapping("")
    public Result<?> updateUser(@RequestBody User user) {
        user.setPassword(null);
        userService.updateUser(user);
        return Result.success("修改用户成功！");
    }

    @ApiOperation("根据ID查询单个用户")
    @GetMapping("/{id}")
    public Result<User> getUserById(@PathVariable("id") Integer id) {
        User user = userService.getUserById(id);
        return Result.success(user);
    }

    @ApiOperation("根据ID逻辑删除用户数据")
    @DeleteMapping("/{id}")
    public Result<User> deleteUserById(@PathVariable("id") Integer id) {
        userService.deleteUserById(id);
        return Result.success("删除用户数据成功！");
    }

    @ApiOperation("修改用户密码")
    @PutMapping("/changePassword")
    public Result<String> changePassword(@RequestBody Map<String, String> request) {
        String username = request.get("username");
        String oldPassword = request.get("oldPassword");
        String newPassword = request.get("newPassword");

        if (username != null && oldPassword != null && newPassword != null) {
            boolean success = userService.changePassword(username, oldPassword, newPassword);
            if (success) {
                return Result.success("密码修改成功！");
            } else {
                return Result.fail(20005, "用户名或旧密码不匹配，密码修改失败！");
            }
        } else {
            return Result.fail(20005, "请求参数不完整，密码修改失败！");
        }
    }

}
