package com.nfu.jasmine.infra.security;

import com.nfu.jasmine.common.enums.ResultCode;
import com.nfu.jasmine.common.exception.BusinessException;
import com.nfu.jasmine.common.model.LoginUserInfo;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 统一获取当前登录用户。
 * <p>
 * 微服务架构下，Gateway 已完成 JWT 验签并将用户信息写入请求头：
 * - X-User-Id：用户 ID
 * - X-User-Name：用户名
 * <p>
 * 本类优先从请求头读取（适用于经 Gateway 转发的请求），
 * 回退到 request attribute（适用于 IAM 服务自身的 JwtAuthenticationFilter 直接设置的场景）。
 */
@Component
public class CurrentUserProvider {

    public static final String HEADER_USER_ID = "X-User-Id";
    public static final String HEADER_USER_NAME = "X-User-Name";

    public LoginUserInfo requireCurrentUser(HttpServletRequest request) {
        LoginUserInfo currentUser = getCurrentUserOrNull(request);
        if (currentUser == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "用户登录信息无效，请重新登录！");
        }
        return currentUser;
    }

    public Integer requireCurrentUserId(HttpServletRequest request) {
        return requireCurrentUser(request).getId();
    }

    public LoginUserInfo getCurrentUserOrNull(HttpServletRequest request) {
        // 优先从 request attribute 读取（IAM 服务自身 JwtAuthenticationFilter 设置）
        Object loginUser = request.getAttribute("loginUser");
        if (loginUser instanceof LoginUserInfo info) {
            return info;
        }

        // 回退：从 Gateway 透传的请求头读取
        String userIdHeader = request.getHeader(HEADER_USER_ID);
        String usernameHeader = request.getHeader(HEADER_USER_NAME);
        if (StringUtils.hasText(userIdHeader)) {
            try {
                int userId = Integer.parseInt(userIdHeader);
                String username = StringUtils.hasText(usernameHeader) ? usernameHeader : "unknown";
                return new GatewayUserInfo(userId, username);
            } catch (NumberFormatException ignored) {
                // 请求头格式异常，视为未登录
            }
        }

        return null;
    }

    /**
     * 从 Gateway 请求头构造的轻量用户信息，不含完整 User 实体。
     */
    private record GatewayUserInfo(Integer id, String username) implements LoginUserInfo {
        @Override
        public Integer getId() {
            return id;
        }

        @Override
        public String getUsername() {
            return username;
        }
    }
}
