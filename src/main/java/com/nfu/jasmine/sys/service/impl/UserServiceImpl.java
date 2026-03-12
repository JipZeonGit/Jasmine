package com.nfu.jasmine.sys.service.impl;

import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.nfu.jasmine.common.utils.JwtUtil;
import com.nfu.jasmine.config.MyRedisConfig;
import com.nfu.jasmine.sys.entity.Menu;
import com.nfu.jasmine.sys.entity.User;
import com.nfu.jasmine.sys.entity.UserRole;
import com.nfu.jasmine.sys.mapper.UserMapper;
import com.nfu.jasmine.sys.mapper.UserRoleMapper;
import com.nfu.jasmine.sys.service.IMenuService;
import com.nfu.jasmine.sys.service.IUserService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import kotlin.jvm.internal.Lambda;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
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
    private RedisTemplate redisTemplate;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private JwtUtil jwtUtil;
    @Autowired
    private UserRoleMapper userRoleMapper;
    @Autowired
    private IMenuService menuService;

    // 用户登录
    @Override
    public Map<String, Object> login(User user) {
        // 根据用户名查询
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, user.getUsername());
        User loginUser = this.baseMapper.selectOne(wrapper);
        // 查询结果不为空，并且传入密码和数据库的密码进行匹配，则生成一个token，并将用户信息存入Redis
        if (loginUser != null && passwordEncoder.matches(user.getPassword(), loginUser.getPassword())) {
            // UUID生成key
            // String key = "user:" + UUID.randomUUID();

            // 去除密码，存入Redis，时效为30分钟
            loginUser.setPassword(null);
            // redisTemplate.opsForValue().set(key,loginUser,30, TimeUnit.MINUTES);

            // 创建JWT
            String token = jwtUtil.createToken(loginUser);

            // 返回数据
            Map<String, Object> data = new HashMap<>();
            data.put("token", token);
            return data;
        }
        return null;
    }

    // 获取用户信息
    @Override
    public Map<String, Object> getUserInfo(String token) {
        // 根据token获取用户信息
        // Object obj = redisTemplate.opsForValue().get(token);

        User loginUser = null;
        try {
            loginUser = jwtUtil.parseToken(token, User.class);
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (loginUser != null) {
            // fastjson2 反序列化
            // User loginUser = JSON.parseObject(JSON.toJSONString(obj),User.class);

            Map<String, Object> data = new HashMap<>();

            data.put("name", loginUser.getUsername());// 取用户名
            data.put("avatar", loginUser.getAvatar());// 取头像
            data.put("phone", loginUser.getPhone()); // 电话
            data.put("email", loginUser.getEmail()); // 邮箱
            data.put("status", loginUser.getStatus()); // 状态

            // 获取用户角色
            List<String> roleList = this.baseMapper.getRoleNameByUserId(loginUser.getId());
            data.put("roles", roleList);

            // 获取角色权限
            List<Menu> menuList = menuService.getMenuListByUserId(loginUser.getId());
            data.put("menuList", menuList);

            return data;
        }
        return null;
    }

    // 用户注销，退出登录
    @Override
    public void logout(String token) {
        // redisTemplate.delete(token);
    }

    @Override
    @Transactional
    public void addUser(User user) {
        // 新增用户
        // 写入用户表
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
        List<Integer> roleIdList = userRoleList.stream().map(userRole -> {
            return userRole.getRoleId();
        }).collect(Collectors.toList());

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
        // 清除原有的角色
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
    }

    // 修改用户密码
    @Override
    @CacheEvict(value = "user", allEntries = true)
    public boolean changePassword(String username, String oldPassword, String newPassword) {
        // 根据用户名查询用户
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, username);
        User user = this.baseMapper.selectOne(wrapper);

        if (user != null && passwordEncoder.matches(oldPassword, user.getPassword())) {
            // 旧密码匹配，可以修改密码
            String newPasswordHash = passwordEncoder.encode(newPassword);
            user.setPassword(newPasswordHash);
            this.baseMapper.updateById(user);
            return true;
        }
        return false; // 修改失败，用户名或旧密码不匹配
    }
}
