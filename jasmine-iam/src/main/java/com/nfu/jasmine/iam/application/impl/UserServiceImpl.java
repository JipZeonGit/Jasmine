package com.nfu.jasmine.iam.application.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nfu.jasmine.common.utils.JwtTokenClaims;
import com.nfu.jasmine.common.utils.JwtUtil;
import com.nfu.jasmine.iam.web.dto.LoginDTO;
import com.nfu.jasmine.iam.model.entity.AuthRefreshToken;
import com.nfu.jasmine.iam.model.entity.Menu;
import com.nfu.jasmine.iam.model.entity.User;
import com.nfu.jasmine.iam.model.entity.UserRole;
import com.nfu.jasmine.iam.persistence.mapper.AuthRefreshTokenMapper;
import com.nfu.jasmine.iam.persistence.mapper.UserMapper;
import com.nfu.jasmine.iam.persistence.mapper.UserRoleMapper;
import com.nfu.jasmine.iam.application.IMenuService;
import com.nfu.jasmine.iam.application.IUserService;
import com.nfu.jasmine.iam.web.vo.LoginVO;
import com.nfu.jasmine.iam.web.vo.UserInfoVO;
import com.nfu.jasmine.infra.cache.CacheNames;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Caching;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
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
@Slf4j
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
        if (!isUserActive(loginUser) || !passwordEncoder.matches(loginDTO.getPassword(), loginUser.getPassword())) {
            log.warn("用户登录失败 username={}", loginDTO.getUsername());
            return null;
        }

        revokeActiveRefreshTokens(loginUser.getId());
        log.info("用户登录成功 userId={} username={}", loginUser.getId(), loginUser.getUsername());
        return issueTokenPair(loginUser);
    }

    // 刷新登录状态
    @Override
    @Transactional
    public LoginVO refreshToken(String refreshTokenValue) {
        JwtTokenClaims claims;
        try {
            claims = jwtUtil.parseRefreshToken(refreshTokenValue);
        } catch (Exception e) {
            log.warn("刷新令牌解析失败 message={}", e.getMessage());
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
            log.warn("刷新令牌已失效 userId={}", claims.getUserId());
            return null;
        }

        refreshToken.setRevoked(1);
        authRefreshTokenMapper.updateById(refreshToken);

        User user = this.baseMapper.selectById(claims.getUserId());
        if (!isUserActive(user)) {
            log.warn("刷新令牌对应用户不存在、已删除或已禁用 userId={}", claims.getUserId());
            return null;
        }

        log.info("刷新访问令牌成功 userId={} username={}", user.getId(), user.getUsername());
        return issueTokenPair(user);
    }

    // 获取用户信息
    @Override
    public UserInfoVO getUserInfo(User loginUser) {
        if (loginUser == null || loginUser.getId() == null) {
            return null;
        }

        User currentUser = this.baseMapper.selectById(loginUser.getId());
        if (!isUserActive(currentUser)) {
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
            log.info("用户退出登录 userId={}", claims.getUserId());
        } catch (Exception e) {
            log.warn("注销时访问令牌无效 message={}", e.getMessage());
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
    @Cacheable(value = CacheNames.USER, key = "#id", sync = true)
    public User getUserById(Integer id) {
        // 根据ID查询用户信息
        User user = this.baseMapper.selectById(id);
        if (user == null) {
            return null;
        }

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
    @Caching(evict = {
            @CacheEvict(value = CacheNames.USER, key = "#user.id"),
            @CacheEvict(value = CacheNames.MENU_LIST, key = "#user.id")
    })
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
        if (!Integer.valueOf(1).equals(user.getStatus())) {
            revokeActiveRefreshTokens(user.getId());
        }
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = CacheNames.USER, key = "#id"),
            @CacheEvict(value = CacheNames.MENU_LIST, key = "#id")
    })
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
    @CacheEvict(value = CacheNames.USER, allEntries = true)
    @Transactional
    public boolean changePassword(Integer userId, String oldPassword, String newPassword) {
        User user = this.baseMapper.selectById(userId);

        if (isUserActive(user) && passwordEncoder.matches(oldPassword, user.getPassword())) {
            // 旧密码匹配，可以修改密码
            user.setPassword(passwordEncoder.encode(newPassword));
            this.baseMapper.updateById(user);
            revokeActiveRefreshTokens(user.getId());
            log.info("用户修改密码成功 userId={} username={}", user.getId(), user.getUsername());
            return true;
        }
        log.warn("用户修改密码失败 userId={}", userId);
        return false; // 修改失败，用户不存在、已禁用或旧密码不匹配
    }

    @Override
    public User getActiveUserById(Integer id) {
        User user = this.baseMapper.selectById(id);
        return isUserActive(user) ? user : null;
    }

    @Override
    public List<String> getRoleNamesByUserId(Integer userId) {
        List<String> roleNames = this.baseMapper.getRoleNameByUserId(userId);
        return roleNames == null ? Collections.emptyList() : roleNames;
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

    @Override
    public List<Integer> getActiveUserIdsByRoleNames(List<String> roleNames) {
        return getBaseMapper().getActiveUserIdsByRoleNames(roleNames);
    }

    private boolean isUserActive(User user) {
        return user != null
                && !Integer.valueOf(1).equals(user.getDeleted())
                && Integer.valueOf(1).equals(user.getStatus());
    }
}
