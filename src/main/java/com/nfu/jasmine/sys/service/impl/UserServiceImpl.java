package com.nfu.jasmine.sys.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nfu.jasmine.common.utils.JwtTokenClaims;
import com.nfu.jasmine.common.utils.JwtUtil;
import com.nfu.jasmine.sys.dto.LoginDTO;
import com.nfu.jasmine.sys.dto.RefreshTokenDTO;
import com.nfu.jasmine.sys.entity.AuthRefreshToken;
import com.nfu.jasmine.sys.entity.Menu;
import com.nfu.jasmine.sys.entity.User;
import com.nfu.jasmine.sys.entity.UserRole;
import com.nfu.jasmine.sys.mapper.AuthRefreshTokenMapper;
import com.nfu.jasmine.sys.mapper.UserMapper;
import com.nfu.jasmine.sys.mapper.UserRoleMapper;
import com.nfu.jasmine.sys.service.IMenuService;
import com.nfu.jasmine.sys.service.IUserService;
import com.nfu.jasmine.sys.vo.LoginVO;
import com.nfu.jasmine.sys.vo.UserInfoVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author jipzeongit
 * @since 2023-05-29
 */
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private JwtUtil jwtUtil;
    @Autowired
    private UserRoleMapper userRoleMapper;
    @Autowired
    private IMenuService menuService;
    @Autowired
    private AuthRefreshTokenMapper authRefreshTokenMapper;

    // 用户登录
    @Override
    @Transactional
    public LoginVO login(LoginDTO loginDTO) {
        // 根据用户名查询
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, loginDTO.getUsername());
        User loginUser = this.baseMapper.selectOne(wrapper);
        if (loginUser == null || !passwordEncoder.matches(loginDTO.getPassword(), loginUser.getPassword())) {
            return null;
        }

        revokeActiveRefreshTokens(loginUser.getId());
        return issueTokenPair(loginUser);
    }

    // 刷新登录状态
    @Override
    @Transactional
    public LoginVO refreshToken(RefreshTokenDTO refreshTokenDTO) {
        JwtTokenClaims claims;
        try {
            claims = jwtUtil.parseRefreshToken(refreshTokenDTO.getRefreshToken());
        } catch (Exception e) {
            return null;
        }

        LambdaQueryWrapper<AuthRefreshToken> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AuthRefreshToken::getUserId, claims.getUserId())
                .eq(AuthRefreshToken::getTokenId, claims.getTokenId())
                .eq(AuthRefreshToken::getRevoked, 0)
                .gt(AuthRefreshToken::getExpiresAt, LocalDateTime.now())
                .last("limit 1");
        AuthRefreshToken refreshToken = authRefreshTokenMapper.selectOne(wrapper);
        if (refreshToken == null) {
            return null;
        }

        refreshToken.setRevoked(1);
        authRefreshTokenMapper.updateById(refreshToken);

        User user = this.baseMapper.selectById(claims.getUserId());
        if (user == null || Integer.valueOf(1).equals(user.getDeleted())) {
            return null;
        }

        return issueTokenPair(user);
    }

    // 获取用户信息
    @Override
    public UserInfoVO getUserInfo(User loginUser) {
        if (loginUser == null || loginUser.getId() == null) {
            return null;
        }

        User currentUser = this.baseMapper.selectById(loginUser.getId());
        if (currentUser == null) {
            return null;
        }

        UserInfoVO data = new UserInfoVO();
        data.setName(currentUser.getUsername());
        data.setAvatar(currentUser.getAvatar());
        data.setPhone(currentUser.getPhone());
        data.setEmail(currentUser.getEmail());
        data.setStatus(currentUser.getStatus());

        // 获取用户角色
        List<String> roleList = this.baseMapper.getRoleNameByUserId(currentUser.getId());
        data.setRoles(roleList);

        // 获取角色权限
        List<Menu> menuList = menuService.getMenuListByUserId(currentUser.getId());
        data.setMenuList(menuList);
        return data;
    }

    // 用户注销，退出登录
    @Override
    @Transactional
    public void logout(String token) {
        if (token == null) {
            return;
        }

        try {
            JwtTokenClaims claims = jwtUtil.parseAccessToken(token);
            revokeActiveRefreshTokens(claims.getUserId());
        } catch (Exception ignored) {
        }
    }

    @Override
    @Transactional
    public void addUser(User user) {
        // 新增用户
        this.baseMapper.insert(user);
        // 写入角色表
        List<Integer> roleIdList = user.getRoleIdList();
        if (roleIdList != null) {
            for (Integer roleId : roleIdList) {
                userRoleMapper.insert(new UserRole(null, user.getId(), roleId));
            }
        }
    }

    @Override
    @Cacheable(value = "user", key = "#id")
    public User getUserById(Integer id) {
        // 根据ID查询用户信息
        User user = this.baseMapper.selectById(id);

        // 构建查询条件，查询用户角色列表
        LambdaQueryWrapper<UserRole> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserRole::getUserId, id);
        List<UserRole> userRoleList = userRoleMapper.selectList(wrapper);

        // 提取角色ID列表
        List<Integer> roleIdList = userRoleList.stream().map(UserRole::getRoleId).collect(Collectors.toList());

        // 设置角色ID列表到用户信息中
        user.setRoleIdList(roleIdList);
        return user;
    }

    @Override
    @Transactional
    @CacheEvict(value = "user", key = "#user.id")
    public void updateUser(User user) {
        // 更新用户表
        this.baseMapper.updateById(user);
        // 清除原有角色
        LambdaQueryWrapper<UserRole> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserRole::getUserId, user.getId());
        userRoleMapper.delete(wrapper);
        // 设置新的角色
        List<Integer> roleIdList = user.getRoleIdList();
        if (roleIdList != null) {
            for (Integer roleId : roleIdList) {
                userRoleMapper.insert(new UserRole(null, user.getId(), roleId));
            }
        }
    }

    @Override
    @CacheEvict(value = "user", key = "#id")
    public void deleteUserById(Integer id) {
        // 删除用户
        this.baseMapper.deleteById(id);
        // 清除原有角色
        LambdaQueryWrapper<UserRole> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserRole::getUserId, id);
        userRoleMapper.delete(wrapper);
        revokeActiveRefreshTokens(id);
    }

    // 修改用户密码
    @Override
    @CacheEvict(value = "user", allEntries = true)
    @Transactional
    public boolean changePassword(String username, String oldPassword, String newPassword) {
        // 根据用户名查询用户
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, username);
        User user = this.baseMapper.selectOne(wrapper);

        if (user != null && passwordEncoder.matches(oldPassword, user.getPassword())) {
            // 旧密码匹配，可以修改密码
            user.setPassword(passwordEncoder.encode(newPassword));
            this.baseMapper.updateById(user);
            revokeActiveRefreshTokens(user.getId());
            return true;
        }
        return false; // 修改失败，用户名或旧密码不匹配
    }

    private LoginVO issueTokenPair(User user) {
        String accessToken = jwtUtil.createAccessToken(user.getId(), user.getUsername());
        String refreshTokenId = UUID.randomUUID().toString();
        String refreshToken = jwtUtil.createRefreshToken(user.getId(), user.getUsername(), refreshTokenId);
        JwtTokenClaims refreshClaims = jwtUtil.parseRefreshToken(refreshToken);

        AuthRefreshToken authRefreshToken = new AuthRefreshToken();
        authRefreshToken.setUserId(user.getId());
        authRefreshToken.setTokenId(refreshTokenId);
        authRefreshToken.setExpiresAt(refreshClaims.getExpiresAt());
        authRefreshToken.setRevoked(0);
        authRefreshTokenMapper.insert(authRefreshToken);

        return new LoginVO(accessToken, refreshToken);
    }

    private void revokeActiveRefreshTokens(Integer userId) {
        LambdaUpdateWrapper<AuthRefreshToken> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(AuthRefreshToken::getUserId, userId)
                .eq(AuthRefreshToken::getRevoked, 0)
                .set(AuthRefreshToken::getRevoked, 1);
        authRefreshTokenMapper.update(null, wrapper);
    }
}