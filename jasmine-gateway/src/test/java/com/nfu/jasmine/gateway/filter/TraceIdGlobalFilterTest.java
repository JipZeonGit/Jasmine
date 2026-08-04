package com.nfu.jasmine.gateway.filter;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 验证网关入口过滤器：生成/复用 traceId，并写入下游请求头与响应头。
 */
@ExtendWith(MockitoExtension.class)
class TraceIdGlobalFilterTest {

    private static final String TRACE_HEADER = "X-Trace-Id";
    private static final String REQUEST_HEADER = "X-Request-Id";

    private final TraceIdGlobalFilter filter = new TraceIdGlobalFilter();

    @Test
    void shouldGenerateTraceIdWhenAbsent() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/flower/list").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);
        GatewayFilterChain chain = mock(GatewayFilterChain.class);
        when(chain.filter(any())).thenReturn(Mono.empty());

        filter.filter(exchange, chain).block();

        // 响应头应回写 traceId，便于客户端关联
        String traceId = exchange.getResponse().getHeaders().getFirst(TRACE_HEADER);
        assertThat(traceId).isNotBlank();
        assertThat(traceId).doesNotContain("-");
        // 下游请求也应携带同一 traceId
        ArgumentCaptor<org.springframework.web.server.ServerWebExchange> captor =
                ArgumentCaptor.forClass(org.springframework.web.server.ServerWebExchange.class);
        verify(chain).filter(captor.capture());
        assertThat(captor.getValue().getRequest().getHeaders().getFirst(TRACE_HEADER)).isEqualTo(traceId);
        assertThat(captor.getValue().getRequest().getHeaders().getFirst(REQUEST_HEADER)).isNotBlank();
    }

    @Test
    void shouldReuseIncomingTraceId() {
        String incoming = "upstream-trace-123";
        MockServerHttpRequest request = MockServerHttpRequest.get("/flower/list")
                .header(TRACE_HEADER, incoming)
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);
        GatewayFilterChain chain = mock(GatewayFilterChain.class);
        when(chain.filter(any())).thenReturn(Mono.empty());

        filter.filter(exchange, chain).block();

        // 入站已有 traceId 时应复用，不重新生成
        assertThat(exchange.getResponse().getHeaders().getFirst(TRACE_HEADER)).isEqualTo(incoming);
        ArgumentCaptor<org.springframework.web.server.ServerWebExchange> captor =
                ArgumentCaptor.forClass(org.springframework.web.server.ServerWebExchange.class);
        verify(chain).filter(captor.capture());
        assertThat(captor.getValue().getRequest().getHeaders().getFirst(TRACE_HEADER)).isEqualTo(incoming);
    }

    @Test
    void shouldNotInjectWhenIncomingHeaderPresent() {
        // 入站已有 traceId 时原样透传，不再注入新值，避免多值头污染
        // 与下游 RequestTraceFilter 复用入站头的设计一致
        MockServerHttpRequest request = MockServerHttpRequest.get("/flower/list")
                .header(TRACE_HEADER, "upstream-trace")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);
        GatewayFilterChain chain = mock(GatewayFilterChain.class);
        when(chain.filter(any())).thenReturn(Mono.empty());

        filter.filter(exchange, chain).block();

        ArgumentCaptor<org.springframework.web.server.ServerWebExchange> captor =
                ArgumentCaptor.forClass(org.springframework.web.server.ServerWebExchange.class);
        verify(chain).filter(captor.capture());
        HttpHeaders downstreamHeaders = captor.getValue().getRequest().getHeaders();
        // 仅保留入站值，未追加网关生成的重复头
        assertThat(downstreamHeaders.get(TRACE_HEADER)).containsExactly("upstream-trace");
    }

    @Test
    void shouldHaveHighestPrecedence() {
        // 必须先于 JwtAuthGlobalFilter (-100) 执行，保证鉴权日志也能带上 traceId
        assertThat(filter.getOrder()).isEqualTo(Ordered.HIGHEST_PRECEDENCE);
    }
}
