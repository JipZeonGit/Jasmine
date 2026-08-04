package com.nfu.jasmine.infra.client;

import org.slf4j.MDC;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.util.StringUtils;

import java.io.IOException;

/**
 * 出站请求拦截器：把当前线程 MDC 里的 traceId / requestId 透传到下游服务，
 * 使下游 {@code RequestTraceFilter} 复用同一个 traceId，跨服务访问日志与 MQ 审计可串联。
 * <p>
 * 设计要点：
 * <ul>
 *   <li>若 MDC 无值（如定时任务上下文），不注入头，由下游自行生成，避免引入无意义的孤立 trace；</li>
 *   <li>已存在的同名头不覆盖，保留上层显式设置的值。</li>
 * </ul>
 * 头名与 {@code RequestTraceFilter}、网关 {@code TraceIdGlobalFilter} 保持一致。
 */
class TraceContextPropagatingInterceptor implements ClientHttpRequestInterceptor {

    private static final String TRACE_HEADER = "X-Trace-Id";
    private static final String REQUEST_HEADER = "X-Request-Id";
    private static final String MDC_TRACE_ID = "traceId";
    private static final String MDC_REQUEST_ID = "requestId";

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution)
            throws IOException {
        String traceId = MDC.get(MDC_TRACE_ID);
        String requestId = MDC.get(MDC_REQUEST_ID);
        if (StringUtils.hasText(traceId) && request.getHeaders().getFirst(TRACE_HEADER) == null) {
            request.getHeaders().set(TRACE_HEADER, traceId);
        }
        if (StringUtils.hasText(requestId) && request.getHeaders().getFirst(REQUEST_HEADER) == null) {
            request.getHeaders().set(REQUEST_HEADER, requestId);
        }
        return execution.execute(request, body);
    }
}
