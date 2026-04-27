package com.nfu.jasmine.infra.security;

import com.nfu.jasmine.common.enums.ResultCode;
import com.nfu.jasmine.common.exception.BusinessException;
import com.nfu.jasmine.iam.model.entity.User;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * 统一获取当前登录用户，避免控制器各自重复解析 request 和 SecurityContext。
 */
@Component
public class CurrentUserProvider {

    public User requireCurrentUser(HttpServletRequest request) {
        User currentUser = getCurrentUserOrNull(request);
        if (currentUser == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "用户登录信息无效，请重新登录！");
        }
        return currentUser;
    }

    public Integer requireCurrentUserId(HttpServletRequest request) {
        return requireCurrentUser(request).getId();
    }

    public User getCurrentUserOrNull(HttpServletRequest request) {
        Object loginUser = request.getAttribute("loginUser");
        if (loginUser instanceof User user) {
            return user;
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof User user) {
            return user;
        }
        return null;
    }
}
