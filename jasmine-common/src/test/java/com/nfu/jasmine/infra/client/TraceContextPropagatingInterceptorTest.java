package com.nfu.jasmine.infra.client;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpResponse;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 验证出站拦截器把 MDC 中的 traceId / requestId 正确透传到下游请求头。
 */
@ExtendWith(MockitoExtension.class)
class TraceContextPropagatingInterceptorTest {

    private static final String TRACE_HEADER = "X-Trace-Id";
    private static final String REQUEST_HEADER = "X-Request-Id";

    private final TraceContextPropagatingInterceptor interceptor = new TraceContextPropagatingInterceptor();

    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    @Test
    void shouldPropagateTraceIdAndRequestIdFromMdc() throws IOException {
        MDC.put("traceId", "trace-abc");
        MDC.put("requestId", "req-xyz");
        HttpRequest request = mockRequest();
        ClientHttpRequestExecution execution = mockExecution();

        interceptor.intercept(request, new byte[0], execution);

        assertThat(request.getHeaders().getFirst(TRACE_HEADER)).isEqualTo("trace-abc");
        assertThat(request.getHeaders().getFirst(REQUEST_HEADER)).isEqualTo("req-xyz");
        verify(execution).execute(request, new byte[0]);
    }

    @Test
    void shouldNotInjectHeadersWhenMdcEmpty() throws IOException {
        // 模拟定时任务等无请求上下文场景：不应注入孤立 trace，留给下游自行生成
        HttpRequest request = mockRequest();
        ClientHttpRequestExecution execution = mockExecution();

        interceptor.intercept(request, new byte[0], execution);

        assertThat(request.getHeaders().getFirst(TRACE_HEADER)).isNull();
        assertThat(request.getHeaders().getFirst(REQUEST_HEADER)).isNull();
    }

    @Test
    void shouldNotOverwriteExistingTraceHeader() throws IOException {
        // 上层已显式设置的头应保留，不被 MDC 值覆盖
        MDC.put("traceId", "mdc-trace");
        HttpRequest request = mockRequest();
        request.getHeaders().set(TRACE_HEADER, "upstream-trace");
        ClientHttpRequestExecution execution = mockExecution();

        interceptor.intercept(request, new byte[0], execution);

        assertThat(request.getHeaders().getFirst(TRACE_HEADER)).isEqualTo("upstream-trace");
    }

    @Test
    void shouldOnlyInjectHeadersPresentInMdc() throws IOException {
        // MDC 只有 traceId、没有 requestId 时，只注入 trace 头
        MDC.put("traceId", "trace-only");
        HttpRequest request = mockRequest();
        ClientHttpRequestExecution execution = mockExecution();

        interceptor.intercept(request, new byte[0], execution);

        assertThat(request.getHeaders().getFirst(TRACE_HEADER)).isEqualTo("trace-only");
        assertThat(request.getHeaders().getFirst(REQUEST_HEADER)).isNull();
    }

    private HttpRequest mockRequest() {
        HttpRequest request = mock(HttpRequest.class);
        when(request.getHeaders()).thenReturn(new HttpHeaders());
        return request;
    }

    private ClientHttpRequestExecution mockExecution() throws IOException {
        ClientHttpRequestExecution execution = mock(ClientHttpRequestExecution.class);
        when(execution.execute(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
                .thenReturn(mock(ClientHttpResponse.class));
        return execution;
    }
}
