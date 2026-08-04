package com.nfu.jasmine.gateway.filter;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * 网关入口请求追踪过滤器，优先级最高，先于 {@link JwtAuthGlobalFilter} 执行。
 * <p>
 * 职责：
 * 1. 从入站请求头 {@code X-Trace-Id} / {@code X-Request-Id} 读取，缺失则生成 UUID；
 * 2. 把同一份 traceId / requestId 写入转发给下游服务的请求头，使下游
 *    {@code RequestTraceFilter} 复用而非重新生成，跨服务访问日志与 MQ 审计可串联；
 * 3. 回写到网关响应头，便于客户端排查时凭 traceId 关联后端日志。
 * <p>
 * 注意：traceId 不属于安全敏感头，故不像 {@code X-User-Id} 那样强制剥离客户端伪造值，
 * 复用入站值以便与上游反代/Nginx 的链路保持一致。
 */
@Component
public class TraceIdGlobalFilter implements GlobalFilter, Ordered {

    // 与下游 RequestTraceFilter / InternalClientFactory 保持一致的头名
    public static final String TRACE_HEADER = "X-Trace-Id";
    public static final String REQUEST_HEADER = "X-Request-Id";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String traceId = resolveOrCreate(request.getHeaders().getFirst(TRACE_HEADER));
        String requestId = resolveOrCreate(request.getHeaders().getFirst(REQUEST_HEADER));

        // 回写到响应头，便于客户端凭 traceId 关联后端日志
        exchange.getResponse().getHeaders().add(TRACE_HEADER, traceId);
        exchange.getResponse().getHeaders().add(REQUEST_HEADER, requestId);

        // traceId 非安全敏感头：入站已有值时（如反代 Nginx 注入）原样透传给下游，
        // 复用与下游 RequestTraceFilter 一致；仅缺失时注入本次生成的值，避免多值头污染。
        ServerHttpRequest.Builder builder = request.mutate();
        if (request.getHeaders().getFirst(TRACE_HEADER) == null) {
            builder.header(TRACE_HEADER, traceId);
        }
        if (request.getHeaders().getFirst(REQUEST_HEADER) == null) {
            builder.header(REQUEST_HEADER, requestId);
        }

        return chain.filter(exchange.mutate().request(builder.build()).build());
    }

    @Override
    public int getOrder() {
        // 先于 JwtAuthGlobalFilter (-100) 执行，保证 JWT 鉴权日志也能带上 traceId
        return Ordered.HIGHEST_PRECEDENCE;
    }

    private String resolveOrCreate(String headerValue) {
        if (StringUtils.hasText(headerValue)) {
            return headerValue;
        }
        return UUID.randomUUID().toString().replace("-", "");
    }
}
