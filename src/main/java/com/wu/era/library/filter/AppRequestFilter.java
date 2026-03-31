package com.wu.era.library.filter;

import com.wu.era.library.wrapper.WuContentCachingResponseWrapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.stream.Collectors;

/**
 * FR-02: Extracts x-wu-correlation header and places in ThreadContext as crId.
 * FR-03: Logs WARN when x-wu-correlationId is absent.
 * FR-04: Extracts standard WU headers into ThreadContext.
 * FR-05: Logs full request/response at DEBUG level.
 * FR-06: Logs request details at INFO for non-200 responses.
 * FR-10: Suppresses missing correlation ID warning for health check endpoints.
 * FR-12: Bypasses wrapping for async-dispatched requests.
 * FR-14: Clears ThreadContext after every request.
 */
@Component
@Order(1)
public class AppRequestFilter extends OncePerRequestFilter {

    private static final Logger log = LogManager.getLogger(AppRequestFilter.class);

    // Header names
    static final String HEADER_CORRELATION_ID = "x-wu-correlationId";
    static final String HEADER_API_KEY = "x-wu-apiKey";
    static final String HEADER_TENANT_ID = "x-wu-tenantId";
    static final String HEADER_EXTERNAL_REF_ID = "x-wu-externalRefId";
    static final String HEADER_AUTH_PRINCIPAL = "x-wu-authPrincipal";

    // ThreadContext keys
    static final String CTX_CORRELATION_ID = "crId";
    static final String CTX_API_KEY = "apiKey";
    static final String CTX_TENANT_ID = "tenantId";
    static final String CTX_EXTERNAL_REF_ID = "extRef";
    static final String CTX_AUTH_PRINCIPAL = "cIntId";
    static final String CTX_SERVER_IP = "svrIp";
    static final String CTX_SERVER_PORT = "svrPort";

    // FR-10: Health check URI patterns
    private static final List<String> HEALTH_CHECK_PATTERNS = Arrays.asList(
            "/actuator/health",
            "/health",
            "/ping",
            "/actuator"
    );

    // FR-12: Async dispatch env variable
    static final String ASYNC_REQUEST_DISPATCH_ENV = "ASYNC_REQUEST_DISPATCH";

    @Override
    protected boolean shouldNotFilterAsyncDispatch() {
        return false;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // FR-12: Bypass for async-dispatched requests or env variable override
        if (isAsyncDispatch(request) || isAsyncDispatchEnabled()) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            // FR-04: Extract headers into ThreadContext
            enrichThreadContext(request);

            // FR-03: Warn if correlation ID is missing (unless health check)
            String correlationId = request.getHeader(HEADER_CORRELATION_ID);
            if ((correlationId == null || correlationId.isEmpty()) && !isHealthCheck(request.getRequestURI())) {
                log.warn("No correlationId found in Header {}", request.getRequestURI());
            }

            WuContentCachingResponseWrapper wrappedResponse = new WuContentCachingResponseWrapper(response);

            // FR-05: Log request at DEBUG level
            if (log.isDebugEnabled()) {
                logRequest(request);
            }

            filterChain.doFilter(request, wrappedResponse);

            // FR-05: Log response at DEBUG level
            if (log.isDebugEnabled()) {
                logResponse(wrappedResponse);
            }

            // FR-06: Log request details at INFO for non-200 responses
            if (wrappedResponse.getStatus() != HttpServletResponse.SC_OK) {
                log.info("Non-200 response [{}] for {} {} headers={} body={}",
                        wrappedResponse.getStatus(),
                        request.getMethod(),
                        request.getRequestURI(),
                        getHeadersAsString(request),
                        getRequestBody(request));
            }

            // FR-15: Write cached response body back
            wrappedResponse.copyBodyToResponse();

        } finally {
            // FR-14: Always clear ThreadContext
            ThreadContext.clearAll();
        }
    }

    /**
     * FR-04: Extract standard WU headers and place into ThreadContext.
     */
    void enrichThreadContext(HttpServletRequest request) {
        // x-wu-correlationId → crId
        String correlationId = request.getHeader(HEADER_CORRELATION_ID);
        if (correlationId != null && !correlationId.isEmpty()) {
            ThreadContext.put(CTX_CORRELATION_ID, correlationId);
        }

        // x-wu-apiKey → apiKey (masked: all chars except last 4 replaced with *)
        String apiKey = request.getHeader(HEADER_API_KEY);
        if (apiKey != null && !apiKey.isEmpty()) {
            ThreadContext.put(CTX_API_KEY, maskApiKey(apiKey));
        }

        // x-wu-tenantId → tenantId
        String tenantId = request.getHeader(HEADER_TENANT_ID);
        if (tenantId != null && !tenantId.isEmpty()) {
            ThreadContext.put(CTX_TENANT_ID, tenantId);
        }

        // x-wu-externalRefId → extRef
        String externalRefId = request.getHeader(HEADER_EXTERNAL_REF_ID);
        if (externalRefId != null && !externalRefId.isEmpty()) {
            ThreadContext.put(CTX_EXTERNAL_REF_ID, externalRefId);
        }

        // x-wu-authPrincipal → cIntId
        String authPrincipal = request.getHeader(HEADER_AUTH_PRINCIPAL);
        if (authPrincipal != null && !authPrincipal.isEmpty()) {
            ThreadContext.put(CTX_AUTH_PRINCIPAL, authPrincipal);
        }

        // Auto-detect server IP
        try {
            ThreadContext.put(CTX_SERVER_IP, InetAddress.getLocalHost().getHostAddress());
        } catch (UnknownHostException e) {
            ThreadContext.put(CTX_SERVER_IP, "unknown");
        }

        // Auto-detect server port
        ThreadContext.put(CTX_SERVER_PORT, String.valueOf(request.getLocalPort()));
    }

    /**
     * FR-07: Mask API key - replace all characters except last 4 with *.
     */
    static String maskApiKey(String apiKey) {
        if (apiKey == null || apiKey.length() <= 4) {
            return apiKey;
        }
        int maskLength = apiKey.length() - 4;
        return "*".repeat(maskLength) + apiKey.substring(maskLength);
    }

    /**
     * FR-10: Check if URI matches health check patterns.
     */
    static boolean isHealthCheck(String uri) {
        if (uri == null) return false;
        return HEALTH_CHECK_PATTERNS.stream().anyMatch(uri::startsWith);
    }

    /**
     * FR-12: Check if async dispatch is enabled via env variable or system property.
     */
    boolean isAsyncDispatchEnabled() {
        String envValue = System.getenv(ASYNC_REQUEST_DISPATCH_ENV);
        if (envValue != null && envValue.equalsIgnoreCase("true")) {
            return true;
        }
        return System.getProperty(ASYNC_REQUEST_DISPATCH_ENV, "false").equalsIgnoreCase("true");
    }

    private void logRequest(HttpServletRequest request) {
        String headers = getHeadersAsString(request);
        String body = getRequestBody(request);
        log.debug("REQUEST: method={} uri={} headers={} queryString={} body={}",
                request.getMethod(),
                request.getRequestURI(),
                headers,
                request.getQueryString(),
                body);
    }

    private void logResponse(WuContentCachingResponseWrapper response) {
        byte[] body = response.getContentAsByteArray();
        String bodyStr = body.length > 0 ? new String(body) : "";
        log.debug("RESPONSE: status={} body={}", response.getStatus(), bodyStr);
    }

    private String getHeadersAsString(HttpServletRequest request) {
        StringBuilder sb = new StringBuilder("{");
        Enumeration<String> headerNames = request.getHeaderNames();
        if (headerNames != null) {
            while (headerNames.hasMoreElements()) {
                String name = headerNames.nextElement();
                String value = request.getHeader(name);
                // FR-07: Mask API key in logs
                if (HEADER_API_KEY.equalsIgnoreCase(name)) {
                    value = maskApiKey(value);
                }
                sb.append(name).append("=").append(value).append(", ");
            }
        }
        if (sb.length() > 1) {
            sb.setLength(sb.length() - 2);
        }
        sb.append("}");
        return sb.toString();
    }

    private String getRequestBody(HttpServletRequest request) {
        try {
            String body = request.getReader().lines().collect(Collectors.joining(System.lineSeparator()));
            return body.isEmpty() ? "" : body;
        } catch (IOException e) {
            return "";
        }
    }
}
