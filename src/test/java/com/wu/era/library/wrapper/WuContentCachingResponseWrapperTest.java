package com.wu.era.library.wrapper;

import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;
import java.io.PrintWriter;

import static org.assertj.core.api.Assertions.assertThat;

class WuContentCachingResponseWrapperTest {

    private MockHttpServletResponse mockResponse;
    private WuContentCachingResponseWrapper wrapper;

    @BeforeEach
    void setUp() {
        mockResponse = new MockHttpServletResponse();
        wrapper = new WuContentCachingResponseWrapper(mockResponse);
    }

    @Test
    void getOutputStream_returnsCachingStream() throws IOException {
        ServletOutputStream os = wrapper.getOutputStream();
        assertThat(os).isNotNull();
    }

    @Test
    void getOutputStream_returnsSameInstance() throws IOException {
        ServletOutputStream os1 = wrapper.getOutputStream();
        ServletOutputStream os2 = wrapper.getOutputStream();
        assertThat(os1).isSameAs(os2);
    }

    @Test
    void getWriter_returnsPrintWriter() throws IOException {
        PrintWriter writer = wrapper.getWriter();
        assertThat(writer).isNotNull();
    }

    @Test
    void getWriter_returnsSameInstance() throws IOException {
        PrintWriter w1 = wrapper.getWriter();
        PrintWriter w2 = wrapper.getWriter();
        assertThat(w1).isSameAs(w2);
    }

    @Test
    void writeAndGetContent_returnsWrittenBytes() throws IOException {
        ServletOutputStream os = wrapper.getOutputStream();
        os.write("Hello".getBytes());

        byte[] content = wrapper.getContentAsByteArray();
        assertThat(new String(content)).isEqualTo("Hello");
    }

    @Test
    void getContentAsByteArray_emptyWhenNothingWritten() {
        byte[] content = wrapper.getContentAsByteArray();
        assertThat(content).isEmpty();
    }

    @Test
    void copyBodyToResponse_writesBodyToOriginalResponse() throws IOException {
        wrapper.getOutputStream().write("response body".getBytes());
        wrapper.copyBodyToResponse();

        assertThat(mockResponse.getContentAsString()).isEqualTo("response body");
        assertThat(mockResponse.getContentLength()).isEqualTo("response body".length());
    }

    @Test
    void copyBodyToResponse_emptyBody_doesNothing() throws IOException {
        wrapper.copyBodyToResponse();
        assertThat(mockResponse.getContentAsString()).isEmpty();
    }

    @Test
    void copyBodyToResponse_withTransferEncoding_doesNotSetContentLength() throws IOException {
        mockResponse.addHeader("Transfer-Encoding", "chunked");
        wrapper = new WuContentCachingResponseWrapper(mockResponse);
        wrapper.getOutputStream().write("chunked body".getBytes());
        wrapper.copyBodyToResponse();

        // Content-Length should not be set when Transfer-Encoding is present
        assertThat(mockResponse.getHeader("Content-Length")).isNull();
    }

    @Test
    void writeListener_isNoOp() throws IOException {
        ServletOutputStream os = wrapper.getOutputStream();
        os.setWriteListener(null); // Should not throw
        assertThat(os.isReady()).isTrue();
    }

    @Test
    void writeBytes_withOffsetAndLength() throws IOException {
        byte[] data = "Hello World".getBytes();
        wrapper.getOutputStream().write(data, 0, 5);

        byte[] content = wrapper.getContentAsByteArray();
        assertThat(new String(content)).isEqualTo("Hello");
    }
}
