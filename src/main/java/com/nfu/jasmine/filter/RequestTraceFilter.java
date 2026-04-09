package com.nfu.jasmine.filter;

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
import java.util.UUID;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestTraceFilter extends OncePerRequestFilter {
    private static final Logger accessLogger = LoggerFactory.getLogger("ACCESS_LOG");
    private static final String TRACE_ID = "traceId";
    private static final String REQUEST_ID = "requestId";
    private static final String TRACE_HEADER = "X-Trace-Id";
    private static final String REQUEST_HEADER = "X-Request-Id";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String traceId = resolveOrCreateId(request.getHeader(TRACE_HEADER));
        String requestId = resolveOrCreateId(request.getHeader(REQUEST_HEADER));
        long startTime = System.currentTimeMillis();

        MDC.put(TRACE_ID, traceId);
        MDC.put(REQUEST_ID, requestId);
        response.setHeader(TRACE_HEADER, traceId);
        response.setHeader(REQUEST_HEADER, requestId);

        try {
            filterChain.doFilter(request, response);
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            accessLogger.info("method={} uri={} status={} durationMs={} clientIp={}",
                    request.getMethod(),
                    buildRequestUri(request),
                    response.getStatus(),
                    duration,
                    resolveClientIp(request));
            MDC.remove(TRACE_ID);
            MDC.remove(REQUEST_ID);
        }
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String uri = request.getRequestURI();
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
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}