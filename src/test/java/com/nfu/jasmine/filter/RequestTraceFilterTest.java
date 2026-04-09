package com.nfu.jasmine.filter;

import jakarta.servlet.ServletException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RequestTraceFilterTest {
    private final RequestTraceFilter filter = new RequestTraceFilter();

    @Test
    void shouldGenerateTraceIdAndRequestIdWhenHeadersAreMissing() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/user/info");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        String traceId = response.getHeader("X-Trace-Id");
        String requestId = response.getHeader("X-Request-Id");
        assertTrue(traceId != null && !traceId.isBlank());
        assertTrue(requestId != null && !requestId.isBlank());
    }

    @Test
    void shouldReuseIncomingTraceHeaders() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/user/info");
        request.addHeader("X-Trace-Id", "trace-123");
        request.addHeader("X-Request-Id", "request-456");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertEquals("trace-123", response.getHeader("X-Trace-Id"));
        assertEquals("request-456", response.getHeader("X-Request-Id"));
    }

    @Test
    void shouldSkipActuatorHealthRequests() throws ServletException {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/actuator/health");
        assertTrue(filter.shouldNotFilter(request));
    }

    @Test
    void shouldNotSkipBusinessRequests() throws ServletException {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/user/info");
        assertFalse(filter.shouldNotFilter(request));
    }
}