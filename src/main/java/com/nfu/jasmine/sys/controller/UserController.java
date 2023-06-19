package com.nfu.jasmine.sys.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nfu.jasmine.common.vo.Result;
import com.nfu.jasmine.sys.entity.User;
import com.nfu.jasmine.sys.service.IUserService;
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
 *  前端控制器
 * </p>
 *
 * @author jipzeongit
 * @since 2023-05-29
 */
@RestController
@RequestMapping("/user")
public class UserController {
    @Autowired
    private IUserService userService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    //获取全部用户
    @GetMapping("/all")
    public Result<List<User>> getAllUser(){
        List<User> list = userService.list();
        return Result.success(list,"查询成功");
    }

    //用户登录
    @PostMapping("/login")
    public Result<Map<String,Object>> login(@RequestBody User user){
        Map<String,Object> data = userService.login(user);
        if(data != null) {
            return Result.success(data);
        }
        return Result.fail(20002,"用户名或密码错误！");
    }

    //获取用户信息
    @GetMapping("/info")
    public Result<Map<String,Object>> getUserInfo(@RequestParam("token") String token){
        //根据token获取用户信息，从Redis获取
        Map<String,Object> data = userService.getUserInfo(token);
        if(data != null) {
            return Result.success(data);
        }
        return Result.fail(20003,"用户登录信息无效，请重新登录！");
    }

    //注销用户
    @PostMapping("/logout")
    public Result<?> logout(@RequestHeader("X-Token") String token){
        userService.logout(token);
        return Result.success();
    }

    //查询用户
    @GetMapping("/list")
    public Result<Map<String,Object>> getUserList(@RequestParam(value = "username",required = false) String username,@RequestParam(value = "phone",required = false) String phone,@RequestParam("pageNo") Long pageNo,@RequestParam("pageSize") Long pageSize){

        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();

        wrapper.eq(StringUtils.hasLength(username),User::getUsername,username);
        wrapper.eq(StringUtils.hasLength(phone),User::getPhone,phone);
        wrapper.orderByDesc(User::getId); //按照用户ID进行排序

        Page<User> page = new Page<>(pageNo,pageSize);
        userService.page(page,wrapper);

        Map<String,Object> data = new HashMap<>();
        data.put("total",page.getTotal());
        data.put("rows",page.getRecords());

        return Result.success(data);
    }

    //新增用户
    @PostMapping("")
    public Result<?> addUser(@RequestBody User user){
        user.setPassword(passwordEncoder.encode(user.getPassword())); //用户密码加密
        userService.save(user);
        return Result.success("新增用户成功！");
    }

    //修改用户
    @PutMapping("")
    public Result<?> updateUser(@RequestBody User user){
        user.setPassword(null);
        userService.updateById(user);
        return Result.success("修改用户成功！");
    }

    //根据ID查询单个用户
    @GetMapping("/{id}")
    public Result<User> getUserById(@PathVariable("id") Integer id){
        User user = userService.getById(id);
        return Result.success(user);
    }

    //根据ID逻辑删除用户数据
    @DeleteMapping("/{id}")
    public Result<User> deleteUserById(@PathVariable("id") Integer id){
        userService.removeById(id);
        return Result.success("删除用户数据成功！");
    }
}
