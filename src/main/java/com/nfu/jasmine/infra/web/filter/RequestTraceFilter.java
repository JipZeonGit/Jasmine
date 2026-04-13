package com.nfu.jasmine.infra.web.filter;

import com.nfu.jasmine.iam.model.entity.User;
import com.nfu.jasmine.infra.mq.message.AccessLogMessage;
import com.nfu.jasmine.infra.mq.publisher.MqMessagePublisher;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Date;
import java.util.UUID;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestTraceFilter extends OncePerRequestFilter {
    private static final Logger accessLogger = LoggerFactory.getLogger("ACCESS_LOG");
    private static final String TRACE_ID = "traceId";
    private static final String REQUEST_ID = "requestId";
    private static final String TRACE_HEADER = "X-Trace-Id";
    private static final String REQUEST_HEADER = "X-Request-Id";

    private final MqMessagePublisher mqMessagePublisher;

    public RequestTraceFilter(MqMessagePublisher mqMessagePublisher) {
        this.mqMessagePublisher = mqMessagePublisher;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String traceId = resolveOrCreateId(request.getHeader(TRACE_HEADER));
        String requestId = resolveOrCreateId(request.getHeader(REQUEST_HEADER));
        long startTime = System.currentTimeMillis();

        // 统一把请求标识放进 MDC，后续业务日志和访问日志都能自动带上这两个字段。
        MDC.put(TRACE_ID, traceId);
        MDC.put(REQUEST_ID, requestId);
        response.setHeader(TRACE_HEADER, traceId);
        response.setHeader(REQUEST_HEADER, requestId);

        try {
            filterChain.doFilter(request, response);
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            User loginUser = resolveLoginUser(request);
            // 访问日志现在优先异步发 MQ，只有 MQ 没开或发送失败时才回退到本地同步日志。
            AccessLogMessage message = new AccessLogMessage(
                    traceId,
                    requestId,
                    loginUser == null ? null : loginUser.getId(),
                    loginUser == null ? null : loginUser.getUsername(),
                    request.getMethod(),
                    buildRequestUri(request),
                    response.getStatus(),
                    duration,
                    resolveClientIp(request),
                    new Date()
            );
            if (!mqMessagePublisher.publishAccessLog(message)) {
                accessLogger.info("method={} uri={} status={} durationMs={} clientIp={} userId={} username={}",
                        message.getMethod(),
                        message.getUri(),
                        message.getStatus(),
                        message.getDurationMs(),
                        message.getClientIp(),
                        message.getUserId(),
                        message.getUsername());
            }
            MDC.remove(TRACE_ID);
            MDC.remove(REQUEST_ID);
        }
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String uri = request.getRequestURI();
        // 健康检查、文档等高频公共端点不单独记录访问日志，避免污染主要业务日志。
        return "/actuator/health".equals(uri)
                || "/actuator/info".equals(uri)
                || "/actuator/prometheus".equals(uri)
                || "/error".equals(uri)
                || uri.startsWith("/swagger-ui/")
                || "/swagger-ui.html".equals(uri)
                || uri.startsWith("/v3/api-docs/")
                || uri.startsWith("/swagger-resources/");
    }

    private String resolveOrCreateId(String headerValue) {
        if (StringUtils.hasText(headerValue)) {
            return headerValue;
        }
        return UUID.randomUUID().toString().replace("-", "");
    }

    private String buildRequestUri(HttpServletRequest request) {
        if (!StringUtils.hasText(request.getQueryString())) {
            return request.getRequestURI();
        }
        return request.getRequestURI() + "?" + request.getQueryString();
    }

    private String resolveClientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(forwardedFor)) {
            // 经过代理时优先记录真实来源 IP，便于后续排查访问链路。
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private User resolveLoginUser(HttpServletRequest request) {
        Object loginUser = request.getAttribute("loginUser");
        if (loginUser instanceof User user) {
            return user;
        }
        return null;
    }
}
