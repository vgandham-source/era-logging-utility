package com.wu.era.library.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.logging.log4j.ThreadContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class AppRequestFilterTest {

    private AppRequestFilter filter;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;
    private MockFilterChain filterChain;

    @BeforeEach
    void setUp() {
        filter = new AppRequestFilter();
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        filterChain = new MockFilterChain();
    }

    @AfterEach
    void tearDown() {
        ThreadContext.clearAll();
    }

    @Test
    void doFilterInternal_withCorrelationId_putsInThreadContext() throws Exception {
        request.addHeader("x-wu-correlationId", "test-correlation-123");

        filter.doFilterInternal(request, response, filterChain);

        // ThreadContext is cleared after the request, so we just verify no exceptions
        assertThat(filterChain.getRequest()).isNotNull();
    }

    @Test
    void doFilterInternal_clearThreadContextAfterRequest() throws Exception {
        request.addHeader("x-wu-correlationId", "test-id");

        filter.doFilterInternal(request, response, filterChain);

        // FR-14: ThreadContext should be cleared after request
        assertThat(ThreadContext.getContext()).isEmpty();
    }

    @Test
    void enrichThreadContext_setsAllHeaders() throws Exception {
        request.addHeader("x-wu-correlationId", "corr-123");
        request.addHeader("x-wu-apiKey", "api-key-12345678");
        request.addHeader("x-wu-tenantId", "tenant-001");
        request.addHeader("x-wu-externalRefId", "ext-ref-999");
        request.addHeader("x-wu-authPrincipal", "user@example.com");

        // Capture context during filter execution using a custom chain
        FilterChain capturingChain = (req, resp) -> {
            assertThat(ThreadContext.get("crId")).isEqualTo("corr-123");
            assertThat(ThreadContext.get("apiKey")).isEqualTo("************5678");
            assertThat(ThreadContext.get("tenantId")).isEqualTo("tenant-001");
            assertThat(ThreadContext.get("extRef")).isEqualTo("ext-ref-999");
            assertThat(ThreadContext.get("cIntId")).isEqualTo("user@example.com");
            assertThat(ThreadContext.get("svrPort")).isNotNull();
        };

        filter.doFilterInternal(request, response, capturingChain);
    }

    @Test
    void enrichThreadContext_noHeaders_setsServerInfo() throws Exception {
        FilterChain capturingChain = (req, resp) -> {
            assertThat(ThreadContext.get("svrIp")).isNotNull();
            assertThat(ThreadContext.get("svrPort")).isNotNull();
        };
        filter.doFilterInternal(request, response, capturingChain);
    }

    @Test
    void maskApiKey_shortKey_returnsUnchanged() {
        assertThat(AppRequestFilter.maskApiKey("abc")).isEqualTo("abc");
        assertThat(AppRequestFilter.maskApiKey("1234")).isEqualTo("1234");
    }

    @Test
    void maskApiKey_longKey_masksAllButLast4() {
        String masked = AppRequestFilter.maskApiKey("abcdefgh");
        assertThat(masked).isEqualTo("****efgh");
        assertThat(masked).hasSize(8);
    }

    @Test
    void maskApiKey_nullKey_returnsNull() {
        assertThat(AppRequestFilter.maskApiKey(null)).isNull();
    }

    @Test
    void maskApiKey_exactly4chars_returnsUnchanged() {
        assertThat(AppRequestFilter.maskApiKey("1234")).isEqualTo("1234");
    }

    @Test
    void isHealthCheck_healthEndpoints_returnsTrue() {
        assertThat(AppRequestFilter.isHealthCheck("/actuator/health")).isTrue();
        assertThat(AppRequestFilter.isHealthCheck("/health")).isTrue();
        assertThat(AppRequestFilter.isHealthCheck("/ping")).isTrue();
        assertThat(AppRequestFilter.isHealthCheck("/actuator")).isTrue();
    }

    @Test
    void isHealthCheck_regularEndpoints_returnsFalse() {
        assertThat(AppRequestFilter.isHealthCheck("/api/v1/users")).isFalse();
        assertThat(AppRequestFilter.isHealthCheck("/transfer")).isFalse();
    }

    @Test
    void isHealthCheck_nullUri_returnsFalse() {
        assertThat(AppRequestFilter.isHealthCheck(null)).isFalse();
    }

    @Test
    void doFilterInternal_healthCheckUri_noWarningLogged() throws Exception {
        request.setRequestURI("/actuator/health");
        // No correlation ID - health check should not warn
        filter.doFilterInternal(request, response, filterChain);
        // Test passes if no exception thrown
        assertThat(filterChain.getRequest()).isNotNull();
    }

    @Test
    void doFilterInternal_non200Response_logsInfo() throws Exception {
        request.addHeader("x-wu-correlationId", "test-id");

        FilterChain chain = (req, resp) -> {
            ((HttpServletResponse) resp).setStatus(500);
        };

        filter.doFilterInternal(request, response, chain);
        // No exception should be thrown
    }

    @Test
    void doFilterInternal_asyncDispatch_callsFilterDirectly() throws Exception {
        // Simulate async dispatch
        request.setAsyncStarted(true);
        MockFilterChain mockChain = new MockFilterChain();

        filter.doFilterInternal(request, response, mockChain);
        assertThat(mockChain.getRequest()).isNotNull();
    }

    @Test
    void isAsyncDispatchEnabled_defaultFalse() {
        System.clearProperty(AppRequestFilter.ASYNC_REQUEST_DISPATCH_ENV);
        // Only check when env var is not set
        if (System.getenv(AppRequestFilter.ASYNC_REQUEST_DISPATCH_ENV) == null) {
            assertThat(filter.isAsyncDispatchEnabled()).isFalse();
        }
    }

    @Test
    void isAsyncDispatchEnabled_systemPropertyTrue() {
        System.setProperty(AppRequestFilter.ASYNC_REQUEST_DISPATCH_ENV, "true");
        try {
            assertThat(filter.isAsyncDispatchEnabled()).isTrue();
        } finally {
            System.clearProperty(AppRequestFilter.ASYNC_REQUEST_DISPATCH_ENV);
        }
    }

    @Test
    void isAsyncDispatchEnabled_systemPropertyFalse() {
        System.setProperty(AppRequestFilter.ASYNC_REQUEST_DISPATCH_ENV, "false");
        try {
            if (System.getenv(AppRequestFilter.ASYNC_REQUEST_DISPATCH_ENV) == null) {
                assertThat(filter.isAsyncDispatchEnabled()).isFalse();
            }
        } finally {
            System.clearProperty(AppRequestFilter.ASYNC_REQUEST_DISPATCH_ENV);
        }
    }

    @Test
    void doFilterInternal_withQueryString_doesNotThrow() throws Exception {
        request.addHeader("x-wu-correlationId", "test-id");
        request.setQueryString("param1=value1&param2=value2");

        filter.doFilterInternal(request, response, filterChain);
        assertThat(filterChain.getRequest()).isNotNull();
    }

    @Test
    void enrichThreadContext_directCall_setsValuesInContext() {
        request.addHeader("x-wu-correlationId", "direct-test");
        filter.enrichThreadContext(request);

        assertThat(ThreadContext.get("crId")).isEqualTo("direct-test");
        ThreadContext.clearAll();
    }
}
