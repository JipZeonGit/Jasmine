package com.nfu.jasmine.iam.web;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nfu.jasmine.common.enums.ResultCode;
import com.nfu.jasmine.common.vo.Result;
import com.nfu.jasmine.common.vo.TableData;
import com.nfu.jasmine.iam.web.dto.ChangePasswordDTO;
import com.nfu.jasmine.iam.web.dto.LoginDTO;
import com.nfu.jasmine.iam.web.dto.RefreshTokenDTO;
import com.nfu.jasmine.iam.web.dto.UserCreateDTO;
import com.nfu.jasmine.iam.web.dto.UserQueryDTO;
import com.nfu.jasmine.iam.web.dto.UserUpdateDTO;
import com.nfu.jasmine.iam.model.entity.User;
import com.nfu.jasmine.iam.application.IUserService;
import com.nfu.jasmine.iam.web.vo.LoginVO;
import com.nfu.jasmine.iam.web.vo.UserInfoVO;
import com.nfu.jasmine.iam.web.vo.UserVO;
import com.nfu.jasmine.common.model.LoginUserInfo;
import com.nfu.jasmine.infra.security.CurrentUserProvider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
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
    @Autowired
    private CurrentUserProvider currentUserProvider;

    @Value("${app.security.refresh-cookie.name:jasmine_refresh_token}")
    private String refreshTokenCookieName;
    @Value("${app.security.refresh-cookie.secure:false}")
    private boolean refreshTokenCookieSecure;
    @Value("${app.security.refresh-cookie.same-site:Lax}")
    private String refreshTokenCookieSameSite;
    @Value("${app.security.jwt-refresh-expire-millis:604800000}")
    private long refreshTokenCookieTtlMillis;

    // 获取所有后台用户的列表数据
    @Operation(summary = "获取全部用户")
    @GetMapping("/all")
    public Result<List<UserVO>> getAllUser() {
        List<UserVO> list = userService.list().stream().map(this::toUserVO).collect(Collectors.toList());
        return Result.success(list, "查询成功");
    }

    // 账号密码登录接口，返回一对 Token
    @Operation(summary = "用户登录")
    @PostMapping("/login")
    public Result<LoginVO> login(@Valid @RequestBody LoginDTO loginDTO, HttpServletResponse response) {
        LoginVO data = userService.login(loginDTO);
        if (data != null) {
            writeRefreshTokenCookie(response, data.getRefreshToken());
            data.setRefreshToken(null);
            return Result.success(data);
        }
        return Result.fail(ResultCode.LOGIN_ERROR);
    }

    // 通过无感刷新用的 Token 来获取新的身份令牌
    @Operation(summary = "刷新登录状态")
    @PostMapping("/refresh")
    public Result<LoginVO> refreshToken(@RequestBody(required = false) RefreshTokenDTO refreshTokenDTO,
                                        HttpServletRequest request,
                                        HttpServletResponse response) {
        String refreshToken = resolveRefreshToken(request, refreshTokenDTO);
        if (!StringUtils.hasText(refreshToken)) {
            clearRefreshTokenCookie(response);
            return Result.fail(ResultCode.UNAUTHORIZED, "刷新令牌无效或已过期，请重新登录！");
        }
        LoginVO data = userService.refreshToken(refreshToken);
        if (data != null) {
            writeRefreshTokenCookie(response, data.getRefreshToken());
            data.setRefreshToken(null);
            return Result.success(data);
        }
        clearRefreshTokenCookie(response);
        return Result.fail(ResultCode.UNAUTHORIZED, "刷新令牌无效或已过期，请重新登录！");
    }

    // 获取当前登录用户的个人基本信息和对应的权限路由
    @Operation(summary = "获取用户信息")
    @GetMapping("/info")
    public Result<UserInfoVO> getUserInfo(HttpServletRequest request) {
        LoginUserInfo loginInfo = currentUserProvider.getCurrentUserOrNull(request);
        // UserController 在 iam 模块内，可安全转换为 User
        User loginUser = loginInfo instanceof User u ? u : null;
        UserInfoVO data = userService.getUserInfo(loginUser);
        if (data != null) {
            return Result.success(data);
        }
        return Result.fail(ResultCode.UNAUTHORIZED, "用户登录信息无效，请重新登录！");
    }

    // 注销清理后端的登录状态及 Token 设置失效
    @Operation(summary = "注销用户")
    @PostMapping("/logout")
    public Result<?> logout(HttpServletRequest request, HttpServletResponse response) {
        String token = resolveToken(request);
        userService.logout(token);
        clearRefreshTokenCookie(response);
        return Result.success();
    }

    // 分页查询和搜索用户信息列表
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

    // 由管理员在后台直接新建账号
    @Operation(summary = "新增用户")
    @PostMapping("")
    public Result<?> addUser(@Valid @RequestBody UserCreateDTO userDTO) {
        User user = new User();
        BeanUtils.copyProperties(userDTO, user);
        user.setPassword(passwordEncoder.encode(userDTO.getPassword())); // 用户密码加密
        userService.addUser(user);
        return Result.success("新增用户成功！");
    }

    // 修改用户信息，也会对应更新所属角色等信息，但不涉及密码修改
    @Operation(summary = "修改用户")
    @PutMapping("")
    public Result<?> updateUser(@Valid @RequestBody UserUpdateDTO userDTO) {
        User user = new User();
        BeanUtils.copyProperties(userDTO, user);
        user.setPassword(null);
        userService.updateUser(user);
        return Result.success("修改用户成功！");
    }

    // 通过传递ID定位和查询一个用户
    @Operation(summary = "根据ID查询单个用户")
    @GetMapping("/{id}")
    public Result<UserVO> getUserById(@PathVariable("id") Integer id) {
        User user = userService.getUserById(id);
        return Result.success(toUserVO(user));
    }

    // 根据用户的ID把它软删掉
    @Operation(summary = "根据ID逻辑删除用户数据")
    @DeleteMapping("/{id}")
    public Result<?> deleteUserById(@PathVariable("id") Integer id) {
        userService.deleteUserById(id);
        return Result.success("删除用户数据成功！");
    }

    // 用户自己提供的旧密码和新密码进行修改
    @Operation(summary = "修改用户密码")
    @PutMapping("/changePassword")
    public Result<String> changePassword(@Valid @RequestBody ChangePasswordDTO request, HttpServletRequest httpServletRequest) {
        Integer currentUserId = currentUserProvider.requireCurrentUserId(httpServletRequest);
        boolean success = userService.changePassword(currentUserId, request.getOldPassword(), request.getNewPassword());
        if (success) {
            return Result.success("密码修改成功！");
        }
        return Result.fail(ResultCode.BUSINESS_ERROR, "用户名或旧密码不匹配，密码修改失败！");
    }

    private String resolveToken(HttpServletRequest request) {
        String authorization = request.getHeader("Authorization");
        if (StringUtils.hasLength(authorization) && authorization.startsWith("Bearer ")) {
            return authorization.substring(7);
        }
        return null;
    }

    private String resolveRefreshToken(HttpServletRequest request, RefreshTokenDTO refreshTokenDTO) {
        if (refreshTokenDTO != null && StringUtils.hasText(refreshTokenDTO.getRefreshToken())) {
            return refreshTokenDTO.getRefreshToken().trim();
        }
        if (request.getCookies() == null) {
            return null;
        }
        for (Cookie cookie : request.getCookies()) {
            if (refreshTokenCookieName.equals(cookie.getName()) && StringUtils.hasText(cookie.getValue())) {
                return cookie.getValue().trim();
            }
        }
        return null;
    }

    private void writeRefreshTokenCookie(HttpServletResponse response, String refreshToken) {
        ResponseCookie cookie = ResponseCookie.from(refreshTokenCookieName, refreshToken)
                .httpOnly(true)
                .secure(refreshTokenCookieSecure)
                .sameSite(refreshTokenCookieSameSite)
                .path("/")
                .maxAge(Duration.ofMillis(refreshTokenCookieTtlMillis))
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private void clearRefreshTokenCookie(HttpServletResponse response) {
        ResponseCookie cookie = ResponseCookie.from(refreshTokenCookieName, "")
                .httpOnly(true)
                .secure(refreshTokenCookieSecure)
                .sameSite(refreshTokenCookieSameSite)
                .path("/")
                .maxAge(Duration.ZERO)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
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
