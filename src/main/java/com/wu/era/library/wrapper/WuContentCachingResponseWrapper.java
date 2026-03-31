package com.wu.era.library.wrapper;

import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.WriteListener;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * FR-15: Response wrapper that caches the response body for logging without consuming the output stream.
 */
public class WuContentCachingResponseWrapper extends HttpServletResponseWrapper {

    private final ByteArrayOutputStream cachedBody = new ByteArrayOutputStream();
    private ServletOutputStream outputStream;
    private PrintWriter writer;

    public WuContentCachingResponseWrapper(HttpServletResponse response) {
        super(response);
    }

    @Override
    public ServletOutputStream getOutputStream() throws IOException {
        if (outputStream == null) {
            outputStream = new CachingServletOutputStream(cachedBody, getResponse());
        }
        return outputStream;
    }

    @Override
    public PrintWriter getWriter() throws IOException {
        if (writer == null) {
            writer = new PrintWriter(getOutputStream());
        }
        return writer;
    }

    public byte[] getContentAsByteArray() {
        if (writer != null) {
            writer.flush();
        }
        return cachedBody.toByteArray();
    }

    /**
     * FR-15: Write the cached body to the actual response output stream.
     * Respects Transfer-Encoding header (does not set content-length when Transfer-Encoding is present).
     */
    public void copyBodyToResponse() throws IOException {
        byte[] body = getContentAsByteArray();
        if (body.length > 0) {
            HttpServletResponse response = (HttpServletResponse) getResponse();
            String transferEncoding = response.getHeader("Transfer-Encoding");
            if (transferEncoding == null || transferEncoding.isEmpty()) {
                response.setContentLength(body.length);
            }
            response.getOutputStream().write(body);
            response.getOutputStream().flush();
        }
    }

    private static class CachingServletOutputStream extends ServletOutputStream {
        private final ByteArrayOutputStream cache;
        private final HttpServletResponse originalResponse;

        CachingServletOutputStream(ByteArrayOutputStream cache, jakarta.servlet.ServletResponse originalResponse) {
            this.cache = cache;
            this.originalResponse = (HttpServletResponse) originalResponse;
        }

        @Override
        public boolean isReady() {
            return true;
        }

        @Override
        public void setWriteListener(WriteListener writeListener) {
            // No-op for synchronous usage
        }

        @Override
        public void write(int b) throws IOException {
            cache.write(b);
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            cache.write(b, off, len);
        }
    }
}
