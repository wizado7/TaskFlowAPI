package com.tasktracker.auth.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

class AuthRateLimitFilterTest {

    @Test
    void blocksAuthRequestsAfterConfiguredAttempts() throws Exception {
        AuthRateLimitFilter filter = new AuthRateLimitFilter(true, 2, 60, 300);

        MockHttpServletResponse first = doLoginRequest(filter);
        MockHttpServletResponse second = doLoginRequest(filter);
        MockHttpServletResponse third = doLoginRequest(filter);

        assertThat(first.getStatus()).isEqualTo(200);
        assertThat(second.getStatus()).isEqualTo(200);
        assertThat(third.getStatus()).isEqualTo(429);
        assertThat(third.getContentAsString()).contains("Too many authentication attempts");
    }

    @Test
    void ignoresNonAuthPaths() throws Exception {
        AuthRateLimitFilter filter = new AuthRateLimitFilter(true, 0, 60, 300);
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/projects");
        request.setRemoteAddr("127.0.0.1");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getStatus()).isEqualTo(200);
    }

    private MockHttpServletResponse doLoginRequest(AuthRateLimitFilter filter) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/auth/login");
        request.setRemoteAddr("127.0.0.1");
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, new MockFilterChain());
        return response;
    }
}
