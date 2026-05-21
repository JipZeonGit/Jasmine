package com.nfu.jasmine.infra.security.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class InternalEndpointGuardFilterTest {

    private InternalEndpointGuardFilter filter;
    private static final String VALID_TOKEN = "test-gateway-token";

    @BeforeEach
    void setUp() {
        filter = new InternalEndpointGuardFilter();
        ReflectionTestUtils.setField(filter, "expectedToken", VALID_TOKEN);
    }

    @Test
    void shouldAllowRequestWithValidToken() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/flower/list");
        request.addHeader("X-Gateway-Token", VALID_TOKEN);
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilterInternal(request, response, chain);

        verify(chain).doFilter(request, response);
    }

    @Test
    void shouldBlockRequestWithoutToken() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/flower/list");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilterInternal(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(403);
        verify(chain, never()).doFilter(request, response);
    }

    @Test
    void shouldBlockRequestWithWrongToken() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/flower/list");
        request.addHeader("X-Gateway-Token", "wrong-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilterInternal(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(403);
        verify(chain, never()).doFilter(request, response);
    }

    @Test
    void shouldSkipFilterForActuatorEndpoints() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/actuator/health");

        boolean shouldNotFilter = filter.shouldNotFilter(request);

        assertThat(shouldNotFilter).isTrue();
    }

    @Test
    void shouldSkipFilterForSwaggerEndpoints() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/swagger-ui/index.html");

        boolean shouldNotFilter = filter.shouldNotFilter(request);

        assertThat(shouldNotFilter).isTrue();
    }

    @Test
    void shouldNotSkipFilterForBusinessEndpoints() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/flower/list");

        boolean shouldNotFilter = filter.shouldNotFilter(request);

        assertThat(shouldNotFilter).isFalse();
    }

    @Test
    void shouldReturnJsonResponseOnForbidden() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/flower/list");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilterInternal(request, response, chain);

        assertThat(response.getContentType()).isEqualTo("application/json;charset=utf-8");
        assertThat(response.getContentAsString()).contains("禁止绕过网关");
    }
}
