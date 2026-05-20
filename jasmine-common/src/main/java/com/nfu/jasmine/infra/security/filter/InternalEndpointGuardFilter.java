package com.nfu.jasmine.infra.security.filter;

import com.alibaba.fastjson2.JSON;
import com.nfu.jasmine.common.enums.ResultCode;
import com.nfu.jasmine.common.vo.Result;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * 内部接口保护过滤器。
 * <p>
 * 即使外部请求绕过 Gateway 直连业务服务端口，也必须在 X-Gateway-Token 请求头中
 * 携带网关共享密钥才能访问 /internal/** 接口。这是 Gateway /internal/** 拦截
 * 之外的纵深防御。
 * <p>
 * Gateway 侧通过 JwtAuthGlobalFilter 注入 X-Gateway-Token，
 * 下游服务通过 app.security.gateway-shared-token 校验匹配。
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class InternalEndpointGuardFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(InternalEndpointGuardFilter.class);

    public static final String HEADER_GATEWAY_TOKEN = "X-Gateway-Token";

    @Value("${app.security.gateway-shared-token:jasmine-dev-gateway-shared-token-change-me-2026}")
    private String expectedToken;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String token = request.getHeader(HEADER_GATEWAY_TOKEN);
        if (!StringUtils.hasText(token) || !expectedToken.equals(token)) {
            log.warn("microservice business endpoint accessed without valid gateway token, uri={}", request.getRequestURI());
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType("application/json;charset=utf-8");
            Result<Object> fail = Result.fail(ResultCode.FORBIDDEN, "禁止绕过网关直接访问微服务接口！");
            response.getWriter().write(JSON.toJSONString(fail));
            response.getWriter().flush();
            return;
        }
        filterChain.doFilter(request, response);
    }

    private static final java.util.List<String> EXCLUDE_PATH_PATTERNS = java.util.List.of(
            "/actuator/**",
            "/swagger-ui/**",
            "/v3/api-docs/**",
            "/swagger-resources/**",
            "/error"
    );

    private final org.springframework.util.AntPathMatcher pathMatcher = new org.springframework.util.AntPathMatcher();

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String uri = request.getRequestURI();
        // 放行系统级监控、API文档以及错误端点，其余所有业务请求均需进行 X-Gateway-Token 防守校验
        return EXCLUDE_PATH_PATTERNS.stream()
                .anyMatch(pattern -> pathMatcher.match(pattern, uri));
    }
}

