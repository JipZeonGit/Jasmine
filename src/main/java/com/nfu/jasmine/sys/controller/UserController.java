package com.nfu.jasmine.sys.controller;

import com.nfu.jasmine.common.vo.Result;
import com.nfu.jasmine.sys.entity.User;
import com.nfu.jasmine.sys.service.IUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.stereotype.Controller;

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
}
