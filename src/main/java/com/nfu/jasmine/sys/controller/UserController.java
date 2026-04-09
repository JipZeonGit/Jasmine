package com.nfu.jasmine.sys.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nfu.jasmine.common.vo.Result;
import com.nfu.jasmine.common.vo.TableData;
import com.nfu.jasmine.sys.dto.ChangePasswordDTO;
import com.nfu.jasmine.sys.dto.LoginDTO;
import com.nfu.jasmine.sys.dto.RefreshTokenDTO;
import com.nfu.jasmine.sys.dto.UserCreateDTO;
import com.nfu.jasmine.sys.dto.UserQueryDTO;
import com.nfu.jasmine.sys.dto.UserUpdateDTO;
import com.nfu.jasmine.sys.entity.User;
import com.nfu.jasmine.sys.service.IUserService;
import com.nfu.jasmine.sys.vo.LoginVO;
import com.nfu.jasmine.sys.vo.UserInfoVO;
import com.nfu.jasmine.sys.vo.UserVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

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
@Tag(name = "用户接口列表")
@Validated
@RestController
@RequestMapping("/user")
public class UserController {
    @Autowired
    private IUserService userService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Operation(summary = "获取全部用户")
    @GetMapping("/all")
    public Result<List<UserVO>> getAllUser() {
        List<UserVO> list = userService.list().stream().map(this::toUserVO).collect(Collectors.toList());
        return Result.success(list, "查询成功");
    }

    @Operation(summary = "用户登录")
    @PostMapping("/login")
    public Result<LoginVO> login(@Valid @RequestBody LoginDTO loginDTO) {
        LoginVO data = userService.login(loginDTO);
        if (data != null) {
            return Result.success(data);
        }
        return Result.fail(20002, "用户名或密码错误！");
    }

    @Operation(summary = "刷新登录状态")
    @PostMapping("/refresh")
    public Result<LoginVO> refreshToken(@Valid @RequestBody RefreshTokenDTO refreshTokenDTO) {
        LoginVO data = userService.refreshToken(refreshTokenDTO);
        if (data != null) {
            return Result.success(data);
        }
        return Result.fail(20003, "刷新令牌无效或已过期，请重新登录！");
    }

    @Operation(summary = "获取用户信息")
    @GetMapping("/info")
    public Result<UserInfoVO> getUserInfo(HttpServletRequest request) {
        User loginUser = getLoginUser(request);
        UserInfoVO data = userService.getUserInfo(loginUser);
        if (data != null) {
            return Result.success(data);
        }
        return Result.fail(20003, "用户登录信息无效，请重新登录！");
    }

    @Operation(summary = "注销用户")
    @PostMapping("/logout")
    public Result<?> logout(HttpServletRequest request) {
        String token = resolveToken(request);
        userService.logout(token);
        return Result.success();
    }

    @Operation(summary = "查询用户")
    @GetMapping("/list")
    public Result<TableData<UserVO>> getUserList(@Valid UserQueryDTO queryDTO) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StringUtils.hasLength(queryDTO.getUsername()), User::getUsername, queryDTO.getUsername());
        wrapper.like(StringUtils.hasLength(queryDTO.getPhone()), User::getPhone, queryDTO.getPhone());
        wrapper.orderByAsc(User::getId); // 按照用户ID进行排序

        Page<User> page = new Page<>(queryDTO.getPageNo(), queryDTO.getPageSize());
        userService.page(page, wrapper);

        TableData<UserVO> data = new TableData<>();
        data.setTotal(page.getTotal());
        data.setRows(page.getRecords().stream().map(this::toUserVO).collect(Collectors.toList()));

        return Result.success(data);
    }

    @Operation(summary = "新增用户")
    @PostMapping("")
    public Result<?> addUser(@Valid @RequestBody UserCreateDTO userDTO) {
        User user = new User();
        BeanUtils.copyProperties(userDTO, user);
        user.setPassword(passwordEncoder.encode(userDTO.getPassword())); // 用户密码加密
        userService.addUser(user);
        return Result.success("新增用户成功！");
    }

    @Operation(summary = "修改用户")
    @PutMapping("")
    public Result<?> updateUser(@Valid @RequestBody UserUpdateDTO userDTO) {
        User user = new User();
        BeanUtils.copyProperties(userDTO, user);
        user.setPassword(null);
        userService.updateUser(user);
        return Result.success("修改用户成功！");
    }

    @Operation(summary = "根据ID查询单个用户")
    @GetMapping("/{id}")
    public Result<UserVO> getUserById(@PathVariable("id") Integer id) {
        User user = userService.getUserById(id);
        return Result.success(toUserVO(user));
    }

    @Operation(summary = "根据ID逻辑删除用户数据")
    @DeleteMapping("/{id}")
    public Result<?> deleteUserById(@PathVariable("id") Integer id) {
        userService.deleteUserById(id);
        return Result.success("删除用户数据成功！");
    }

    @Operation(summary = "修改用户密码")
    @PutMapping("/changePassword")
    public Result<String> changePassword(@Valid @RequestBody ChangePasswordDTO request) {
        boolean success = userService.changePassword(request.getUsername(), request.getOldPassword(), request.getNewPassword());
        if (success) {
            return Result.success("密码修改成功！");
        }
        return Result.fail(20005, "用户名或旧密码不匹配，密码修改失败！");
    }

    private User getLoginUser(HttpServletRequest request) {
        Object loginUser = request.getAttribute("loginUser");
        if (loginUser instanceof User) {
            return (User) loginUser;
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof User) {
            return (User) authentication.getPrincipal();
        }
        return null;
    }

    private String resolveToken(HttpServletRequest request) {
        String authorization = request.getHeader("Authorization");
        if (StringUtils.hasLength(authorization) && authorization.startsWith("Bearer ")) {
            return authorization.substring(7);
        }
        return null;
    }

    private UserVO toUserVO(User user) {
        if (user == null) {
            return null;
        }
        UserVO vo = new UserVO();
        BeanUtils.copyProperties(user, vo);
        return vo;
    }
}