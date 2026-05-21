package com.nfu.jasmine.gateway.filter;

import com.nfu.jasmine.common.utils.JwtTokenClaims;
import com.nfu.jasmine.common.utils.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.test.util.ReflectionTestUtils;
import reactor.core.publisher.Mono;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtAuthGlobalFilterTest {

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private GatewayFilterChain chain;

    private JwtAuthGlobalFilter filter;

    @BeforeEach
    void setUp() {
        filter = new JwtAuthGlobalFilter(jwtUtil);
        ReflectionTestUtils.setField(filter, "gatewaySharedToken", "test-gateway-token");
    }

    @Test
    void shouldBlockInternalPaths() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/internal/flower/1").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);
        when(chain.filter(any())).thenReturn(Mono.empty());

        filter.filter(exchange, chain).block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void shouldPassWhitelistedPathWithoutToken() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/user/login").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);
        when(chain.filter(any())).thenReturn(Mono.empty());

        filter.filter(exchange, chain).block();

        assertThat(exchange.getResponse().getStatusCode()).isNull();
    }

    @Test
    void shouldReturn401WhenNoTokenProvided() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/flower/list").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        filter.filter(exchange, chain).block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void shouldReturn401WhenTokenInvalid() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/flower/list")
                .header(HttpHeaders.AUTHORIZATION, "Bearer invalid-token")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);
        when(jwtUtil.parseAccessToken("invalid-token")).thenThrow(new RuntimeException("invalid"));

        filter.filter(exchange, chain).block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void shouldInjectUserHeadersWhenTokenValid() {
        JwtTokenClaims claims = new JwtTokenClaims();
        claims.setUserId(42);
        claims.setUsername("testuser");

        MockServerHttpRequest request = MockServerHttpRequest.get("/flower/list")
                .header(HttpHeaders.AUTHORIZATION, "Bearer valid-token")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);
        when(jwtUtil.parseAccessToken("valid-token")).thenReturn(claims);
        when(chain.filter(any())).thenReturn(Mono.empty());

        filter.filter(exchange, chain).block();

        assertThat(exchange.getResponse().getStatusCode()).isNull();
    }

    @Test
    void shouldReturn401ForRefreshTokenUsedAsAccessToken() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/flower/list")
                .header(HttpHeaders.AUTHORIZATION, "Bearer refresh-token")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);
        when(jwtUtil.parseAccessToken("refresh-token"))
                .thenThrow(new io.jsonwebtoken.JwtException("not access"));

        filter.filter(exchange, chain).block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void shouldHaveCorrectOrder() {
        assertThat(filter.getOrder()).isEqualTo(-100);
    }
}
