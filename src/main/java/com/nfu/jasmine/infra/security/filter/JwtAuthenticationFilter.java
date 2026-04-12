package com.nfu.jasmine.infra.security.filter;

import com.alibaba.fastjson2.JSON;
import com.nfu.jasmine.common.enums.ResultCode;
import com.nfu.jasmine.common.utils.JwtTokenClaims;
import com.nfu.jasmine.common.utils.JwtUtil;
import com.nfu.jasmine.common.vo.Result;
import com.nfu.jasmine.iam.model.entity.User;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    @Autowired
    private JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        // 尝试从请求头中获取 Token
        String token = resolveToken(request);
        
        // 如果没有 Token，直接放行，可能有不需要登录的接口，也可能直接被 Spring Security 拦截
        if (!StringUtils.hasText(token)) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            // 解析 Token 获取用户信息
            JwtTokenClaims claims = jwtUtil.parseAccessToken(token);
            User loginUser = new User();
            loginUser.setId(claims.getUserId());
            loginUser.setUsername(claims.getUsername());

            // 将用户信息封装进 Authentication 对象，标记为已认证
            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    loginUser,
                    null,
                    Collections.emptyList()
            );
            // 记录当前请求的部分详细信息
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            // 将登录用户存入 request 属性中，方便后续 Controller 直接获取
            request.setAttribute("loginUser", loginUser);
            // 将认证信息存入 Spring Security 上下文
            SecurityContextHolder.getContext().setAuthentication(authentication);
            
            // 验证成功，继续执行后续的过滤器链
            filterChain.doFilter(request, response);
        } catch (Exception e) {
            // Token 无效或过期，清空 SecurityContext
            SecurityContextHolder.clearContext();
            log.warn("JWT 校验失败 uri={} message={}", request.getRequestURI(), e.getMessage());
            
            // 直接往前台返回 401 和 JSON 格式的错误提示
            response.setContentType("application/json;charset=utf-8");
            Result<Object> fail = Result.fail(ResultCode.UNAUTHORIZED, "JWT无效，请重新登录！");
            response.getWriter().write(JSON.toJSONString(fail));
        }
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // 放行特定的请求（如登录、刷新Token、Swagger文档、健康检查等），对这些路径不执行上述 JWT 校验
        String requestURI = request.getRequestURI();
        return "/user/login".equals(requestURI)
                || "/user/refresh".equals(requestURI)
                || "/error".equals(requestURI)
                || requestURI.startsWith("/swagger-ui/")
                || "/swagger-ui.html".equals(requestURI)
                || requestURI.startsWith("/v3/api-docs/")
                || requestURI.startsWith("/swagger-resources/")
                || "/actuator/health".equals(requestURI)
                || requestURI.startsWith("/actuator/health/")
                || "/actuator/info".equals(requestURI)
                || "/actuator/prometheus".equals(requestURI);
    }

    private String resolveToken(HttpServletRequest request) {
        // 从 Authorization 请求头中获取 Token
        String authorization = request.getHeader("Authorization");
        // Token 必须是以 "Bearer " 开头的格式
        if (StringUtils.hasText(authorization) && authorization.startsWith("Bearer ")) {
            return authorization.substring(7); // 截取掉前缀，返回真实的 JWT
        }
        return null; // 没有的话就返回空
    }
}