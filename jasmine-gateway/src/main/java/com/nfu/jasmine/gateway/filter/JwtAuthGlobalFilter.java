package com.nfu.jasmine.gateway.filter;

import com.nfu.jasmine.common.utils.JwtTokenClaims;
import com.nfu.jasmine.common.utils.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * 网关全局 JWT 鉴权过滤器。
 * <p>
 * 职责：
 * 1. 对白名单路径直接放行
 * 2. 对其他路径校验 Bearer Token 签名与有效期
 * 3. 将 userId / username 写入请求头传递给下游服务
 * <p>
 * 注意：网关只做"签名验证 + claims 提取"，不查数据库、不做 RBAC。
 * RBAC 由各下游服务自行根据 X-User-Roles 判断。
 */
@Component
public class JwtAuthGlobalFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthGlobalFilter.class);

    // 网关透传给下游的标准请求头
    public static final String HEADER_USER_ID = "X-User-Id";
    public static final String HEADER_USER_NAME = "X-User-Name";

    // 白名单路径，不需要 JWT 即可访问
    private static final List<String> WHITE_LIST = List.of(
            "/user/login",
            "/user/refresh",
            "/actuator/health",
            "/actuator/health/**",
            "/actuator/info",
            "/actuator/prometheus",
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/v3/api-docs/**",
            "/swagger-resources/**"
    );

    private final JwtUtil jwtUtil;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    public JwtAuthGlobalFilter(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();

        // 内部接口禁止外部访问
        if (path.startsWith("/internal/")) {
            log.warn("Gateway: blocked external access to internal path={}", path);
            exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
            return exchange.getResponse().setComplete();
        }

        // 白名单放行
        if (isWhiteListed(path)) {
            return chain.filter(exchange);
        }

        // 提取 Bearer Token
        String token = resolveToken(request);
        if (!StringUtils.hasText(token)) {
            log.debug("Gateway JWT: no token for path={}", path);
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        // 验签 + 解析
        JwtTokenClaims claims;
        try {
            claims = jwtUtil.parseAccessToken(token);
        } catch (Exception e) {
            log.warn("Gateway JWT: invalid token path={} reason={}", path, e.getMessage());
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        // 将用户信息写入请求头，传递给下游服务
        ServerHttpRequest mutatedRequest = request.mutate()
                .header(HEADER_USER_ID, String.valueOf(claims.getUserId()))
                .header(HEADER_USER_NAME, claims.getUsername())
                .build();

        return chain.filter(exchange.mutate().request(mutatedRequest).build());
    }

    @Override
    public int getOrder() {
        // 在路由过滤器之前执行
        return -100;
    }

    private boolean isWhiteListed(String path) {
        return WHITE_LIST.stream().anyMatch(pattern -> pathMatcher.match(pattern, path));
    }

    private String resolveToken(ServerHttpRequest request) {
        String authorization = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (StringUtils.hasText(authorization) && authorization.startsWith("Bearer ")) {
            return authorization.substring(7);
        }
        return null;
    }
}
