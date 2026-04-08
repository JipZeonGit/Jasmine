package com.nfu.jasmine.filter;

import com.alibaba.fastjson2.JSON;
import com.nfu.jasmine.common.utils.JwtTokenClaims;
import com.nfu.jasmine.common.utils.JwtUtil;
import com.nfu.jasmine.common.vo.Result;
import com.nfu.jasmine.sys.entity.User;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
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
    @Autowired
    private JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String token = resolveToken(request);
        if (!StringUtils.hasText(token)) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            JwtTokenClaims claims = jwtUtil.parseAccessToken(token);
            User loginUser = new User();
            loginUser.setId(claims.getUserId());
            loginUser.setUsername(claims.getUsername());

            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    loginUser,
                    null,
                    Collections.emptyList()
            );
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            request.setAttribute("loginUser", loginUser);
            SecurityContextHolder.getContext().setAuthentication(authentication);
            filterChain.doFilter(request, response);
        } catch (Exception e) {
            SecurityContextHolder.clearContext();
            response.setContentType("application/json;charset=utf-8");
            Result<Object> fail = Result.fail(20003, "JWT无效，请重新登录！");
            response.getWriter().write(JSON.toJSONString(fail));
        }
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String requestURI = request.getRequestURI();
        return "/user/login".equals(requestURI)
                || "/user/refresh".equals(requestURI)
                || "/error".equals(requestURI)
                || requestURI.startsWith("/swagger-ui/")
                || "/swagger-ui.html".equals(requestURI)
                || requestURI.startsWith("/v3/api-docs/")
                || requestURI.startsWith("/swagger-resources/");
    }

    private String resolveToken(HttpServletRequest request) {
        String authorization = request.getHeader("Authorization");
        if (StringUtils.hasText(authorization) && authorization.startsWith("Bearer ")) {
            return authorization.substring(7);
        }
        return null;
    }
}