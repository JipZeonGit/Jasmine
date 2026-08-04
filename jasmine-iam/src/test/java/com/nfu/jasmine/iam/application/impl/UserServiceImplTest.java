package com.nfu.jasmine.iam.application.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.nfu.jasmine.common.utils.JwtTokenClaims;
import com.nfu.jasmine.common.utils.JwtUtil;
import com.nfu.jasmine.iam.model.entity.AuthRefreshToken;
import com.nfu.jasmine.iam.model.entity.User;
import com.nfu.jasmine.iam.model.entity.UserRole;
import com.nfu.jasmine.iam.persistence.mapper.AuthRefreshTokenMapper;
import com.nfu.jasmine.iam.persistence.mapper.UserMapper;
import com.nfu.jasmine.iam.persistence.mapper.UserRoleMapper;
import com.nfu.jasmine.iam.web.dto.LoginDTO;
import com.nfu.jasmine.iam.web.vo.LoginVO;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;

import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * UserServiceImpl 单测 —— 覆盖 login/refreshToken/changePassword/updateUser/deleteUser
 * 的成功与失败分支。缓存注解在纯单测不触发（无 Spring 上下文），单测只验证业务逻辑。
 */
@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserMapper userMapper;
    @Mock
    private UserRoleMapper userRoleMapper;
    @Mock
    private AuthRefreshTokenMapper authRefreshTokenMapper;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtUtil jwtUtil;
    @Mock
    private com.nfu.jasmine.iam.application.IMenuService menuService;

    @InjectMocks
    private UserServiceImpl userService;

    @BeforeEach
    void setUp() {
        // ServiceImpl 需要通过反射注入 baseMapper
        ReflectionTestUtils.setField(userService, "baseMapper", userMapper);
        // 纯单测无 MyBatis-Plus 上下文，LambdaUpdateWrapper 引用实体方法引用时
        // 需要预先初始化 TableInfo lambda cache，否则报 "can not find lambda cache"
        Configuration cfg = new Configuration();
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(cfg, "");
        TableInfoHelper.initTableInfo(assistant, AuthRefreshToken.class);
        TableInfoHelper.initTableInfo(assistant, User.class);
    }

    // ===== login =====

    // login 成功：密码匹配 → 签发 token + 吊销既有 refresh
    @Test
    void loginShouldReturnTokenPairWhenPasswordMatches() {
        User user = createActiveUser(1, "admin");
        when(userMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(user);
        when(passwordEncoder.matches("password123", "encoded")).thenReturn(true);
        // issueTokenPair 内部会调 jwtUtil
        when(jwtUtil.createAccessToken(1, "admin")).thenReturn("access-token");
        when(jwtUtil.createRefreshToken(eq(1), eq("admin"), anyString())).thenReturn("refresh-token");
        JwtTokenClaims refreshClaims = new JwtTokenClaims("tid", 1, "admin", "refresh",
                LocalDateTime.now(), LocalDateTime.now().plusDays(7));
        when(jwtUtil.parseRefreshToken("refresh-token")).thenReturn(refreshClaims);

        LoginDTO dto = new LoginDTO();
        dto.setUsername("admin");
        dto.setPassword("password123");

        LoginVO result = userService.login(dto);

        assertThat(result).isNotNull();
        assertThat(result.getToken()).isEqualTo("access-token");
        assertThat(result.getRefreshToken()).isEqualTo("refresh-token");
        // 验证吊销了既有 refresh token，且 wrapper 条件锁定 user_id + 未吊销
        assertRevokeWrapperTargetsUser(1);
        // 验证插入了新的 refresh token 记录
        verify(authRefreshTokenMapper).insert(any(AuthRefreshToken.class));
    }

    // login 失败：用户不存在
    @Test
    void loginShouldReturnNullWhenUserNotFound() {
        when(userMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

        LoginDTO dto = new LoginDTO();
        dto.setUsername("ghost");
        dto.setPassword("password123");

        LoginVO result = userService.login(dto);

        assertThat(result).isNull();
        verify(jwtUtil, never()).createAccessToken(anyInt(), anyString());
    }

    // login 失败：密码错误
    @Test
    void loginShouldReturnNullWhenPasswordWrong() {
        User user = createActiveUser(1, "admin");
        when(userMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(user);
        when(passwordEncoder.matches("wrong", "encoded")).thenReturn(false);

        LoginDTO dto = new LoginDTO();
        dto.setUsername("admin");
        dto.setPassword("wrong");

        LoginVO result = userService.login(dto);

        assertThat(result).isNull();
        verify(jwtUtil, never()).createAccessToken(anyInt(), anyString());
    }

    // login 失败：用户已禁用（status != 1）
    @Test
    void loginShouldReturnNullWhenUserDisabled() {
        User user = createActiveUser(1, "admin");
        user.setStatus(0);
        when(userMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(user);

        LoginDTO dto = new LoginDTO();
        dto.setUsername("admin");
        dto.setPassword("password123");

        LoginVO result = userService.login(dto);

        assertThat(result).isNull();
        verify(passwordEncoder, never()).matches(anyString(), anyString());
    }

    // ===== refreshToken =====

    // refreshToken 成功：旧 token 吊销 + 重签新对
    @Test
    void refreshTokenShouldRotateWhenValid() {
        JwtTokenClaims claims = new JwtTokenClaims("tid-1", 1, "admin", "refresh",
                LocalDateTime.now(), LocalDateTime.now().plusDays(7));
        when(jwtUtil.parseRefreshToken("old-refresh")).thenReturn(claims);

        AuthRefreshToken storedToken = new AuthRefreshToken();
        storedToken.setId(10);
        storedToken.setUserId(1);
        storedToken.setTokenId("tid-1");
        storedToken.setRevoked(0);
        storedToken.setExpiresAt(LocalDateTime.now().plusDays(7));
        when(authRefreshTokenMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(storedToken);

        User user = createActiveUser(1, "admin");
        when(userMapper.selectById(1)).thenReturn(user);
        when(jwtUtil.createAccessToken(1, "admin")).thenReturn("new-access");
        when(jwtUtil.createRefreshToken(eq(1), eq("admin"), anyString())).thenReturn("new-refresh");
        when(jwtUtil.parseRefreshToken("new-refresh")).thenReturn(claims);

        LoginVO result = userService.refreshToken("old-refresh");

        assertThat(result).isNotNull();
        assertThat(result.getToken()).isEqualTo("new-access");
        // 验证旧 token 被吊销（revoked 置 1）
        assertThat(storedToken.getRevoked()).isEqualTo(1);
        verify(authRefreshTokenMapper).updateById(storedToken);
    }

    // refreshToken 失败：token 解析失败
    @Test
    void refreshTokenShouldReturnNullWhenTokenInvalid() {
        when(jwtUtil.parseRefreshToken("bad-token")).thenThrow(new RuntimeException("parse error"));

        LoginVO result = userService.refreshToken("bad-token");

        assertThat(result).isNull();
        verify(authRefreshTokenMapper, never()).selectOne(any());
    }

    // refreshToken 失败：token 已吊销（DB 查不到）
    @Test
    void refreshTokenShouldReturnNullWhenTokenRevoked() {
        JwtTokenClaims claims = new JwtTokenClaims("tid-1", 1, "admin", "refresh",
                LocalDateTime.now(), LocalDateTime.now().plusDays(7));
        when(jwtUtil.parseRefreshToken("revoked-refresh")).thenReturn(claims);
        when(authRefreshTokenMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

        LoginVO result = userService.refreshToken("revoked-refresh");

        assertThat(result).isNull();
        // 验证查询条件锁定 user_id + token_id + 未吊销 + 未过期
        assertRefreshTokenLookupWrapper(1, "tid-1");
        verify(userMapper, never()).selectById(anyInt());
    }

    // refreshToken 失败：用户已禁用
    @Test
    void refreshTokenShouldReturnNullWhenUserDisabled() {
        JwtTokenClaims claims = new JwtTokenClaims("tid-1", 1, "admin", "refresh",
                LocalDateTime.now(), LocalDateTime.now().plusDays(7));
        when(jwtUtil.parseRefreshToken("valid-refresh")).thenReturn(claims);

        AuthRefreshToken storedToken = new AuthRefreshToken();
        storedToken.setRevoked(0);
        storedToken.setExpiresAt(LocalDateTime.now().plusDays(7));
        when(authRefreshTokenMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(storedToken);

        User disabledUser = createActiveUser(1, "admin");
        disabledUser.setStatus(0);
        when(userMapper.selectById(1)).thenReturn(disabledUser);

        LoginVO result = userService.refreshToken("valid-refresh");

        assertThat(result).isNull();
        verify(jwtUtil, never()).createAccessToken(anyInt(), anyString());
    }

    // ===== changePassword =====

    // changePassword 失败：旧密码错误
    @Test
    void changePasswordShouldReturnFalseWhenOldPasswordWrong() {
        User user = createActiveUser(1, "admin");
        when(userMapper.selectById(1)).thenReturn(user);
        when(passwordEncoder.matches("wrong-old", "encoded")).thenReturn(false);

        boolean result = userService.changePassword(1, "wrong-old", "new-pass");

        assertThat(result).isFalse();
        verify(userMapper, never()).updateById(any(User.class));
    }

    // changePassword 成功：改密 + 吊销 refresh token
    @Test
    void changePasswordShouldRevokeRefreshTokensWhenChanged() {
        User user = createActiveUser(1, "admin");
        when(userMapper.selectById(1)).thenReturn(user);
        when(passwordEncoder.matches("old-pass", "encoded")).thenReturn(true);
        when(passwordEncoder.encode("new-pass")).thenReturn("new-encoded");

        boolean result = userService.changePassword(1, "old-pass", "new-pass");

        assertThat(result).isTrue();
        verify(userMapper).updateById((User) any());
        // 验证吊销了 refresh token，wrapper 锁定当前 user
        assertRevokeWrapperTargetsUser(1);
    }

    // ===== updateUser =====

    // updateUser：禁用用户（status=0）时应吊销 refresh token
    @Test
    void updateUserShouldRevokeRefreshTokensWhenUserDisabled() {
        User user = createActiveUser(1, "admin");
        user.setStatus(0); // 禁用
        user.setRoleIdList(List.of(2));

        userService.updateUser(user);

        verify(userMapper).updateById(user);
        // 验证清旧角色 + 插新角色
        verify(userRoleMapper).delete(any(LambdaQueryWrapper.class));
        verify(userRoleMapper).insert(any(UserRole.class));
        // 验证吊销了 refresh token，wrapper 锁定被禁用的 user
        assertRevokeWrapperTargetsUser(1);
    }

    // updateUser：启用用户（status=1）时不吊销 refresh token
    @Test
    void updateUserShouldNotRevokeRefreshTokensWhenUserActive() {
        User user = createActiveUser(1, "admin");
        user.setStatus(1);
        user.setRoleIdList(null);

        userService.updateUser(user);

        verify(userMapper).updateById(user);
        verify(authRefreshTokenMapper, never()).update(any(), any());
    }

    // ===== deleteUserById =====

    // deleteUserById：删用户 + 清角色关联 + 吊销 refresh token
    @Test
    void deleteUserByIdShouldDeleteUserAndClearRolesAndRevokeTokens() {
        userService.deleteUserById(1);

        verify(userMapper).deleteById(1);
        verify(userRoleMapper).delete(any(LambdaQueryWrapper.class));
        assertRevokeWrapperTargetsUser(1);
    }

    // ===== 辅助方法 =====

    /**
     * 抓取传给 authRefreshTokenMapper.update 的 LambdaUpdateWrapper，断言其
     * WHERE 条件同时包含 user_id 与 revoked=0 两个约束（缺一即回归，如漏掉
     * revoked=0 会变成全表吊销），且 SET 子句将 revoked 置为 1。
     * <p>
     * 这比 {@code verify(mapper).update(isNull(), any())} 更严格——后者只校验
     * "调了一次 update"，无法发现 wrapper 条件写错。getTargetSql() 返回的 SQL
     * 用占位符（如 "userId = ?"），因此只校验列名出现与否，不校验具体值——
     * 值由调用方变量传入，不属于 wrapper 构造正确性范畴。
     */
    private void assertRevokeWrapperTargetsUser(int expectedUserId) {
        ArgumentCaptor<LambdaUpdateWrapper<AuthRefreshToken>> captor =
                ArgumentCaptor.forClass(LambdaUpdateWrapper.class);
        verify(authRefreshTokenMapper).update(isNull(), captor.capture());
        LambdaUpdateWrapper<AuthRefreshToken> wrapper = captor.getValue();
        String sql = wrapper.getTargetSql();
        // 两个条件必须同时出现：锁定当前 user + 仅未吊销记录
        assertThat(sql).contains("userId");
        assertThat(sql).contains("revoked");
        // 两个条件用 AND 连接（缺一即回归，如全表吊销）
        assertThat(sql).contains("AND");
        // SET 子句必须更新 revoked 字段
        assertThat(wrapper.getSqlSet()).contains("revoked");
    }

    /**
     * 抓取传给 authRefreshTokenMapper.selectOne 的 LambdaQueryWrapper，断言其
     * 查询条件包含 user_id + token_id + revoked=0 + expires_at 未过期四个约束。
     * 同样只校验列名出现与否，不校验占位符的具体值。
     */
    private void assertRefreshTokenLookupWrapper(int expectedUserId, String expectedTokenId) {
        ArgumentCaptor<LambdaQueryWrapper<AuthRefreshToken>> captor =
                ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(authRefreshTokenMapper).selectOne(captor.capture());
        String sql = captor.getValue().getTargetSql();
        // 四个条件必须同时出现：user_id + token_id + 未吊销 + 未过期
        assertThat(sql).contains("userId");
        assertThat(sql).contains("tokenId");
        assertThat(sql).contains("revoked");
        assertThat(sql).contains("expiresAt >");
    }

    private User createActiveUser(int id, String username) {
        User user = new User();
        user.setId(id);
        user.setUsername(username);
        user.setPassword("encoded");
        user.setStatus(1);
        user.setDeleted(0);
        return user;
    }
}
