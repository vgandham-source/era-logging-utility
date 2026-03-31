package com.wu.era.library.layout;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.message.SimpleMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SimpleJSONLayoutTest {

    private SimpleJSONLayout layout;

    @BeforeEach
    void setUp() {
        layout = SimpleJSONLayout.createLayout();
    }

    @Test
    void createLayout_returnsInstance() {
        assertThat(layout).isNotNull();
    }

    @Test
    void toSerializable_producesValidJson() {
        LogEvent event = Log4jLogEvent.newBuilder()
                .setLevel(Level.INFO)
                .setLoggerName("TestLogger")
                .setMessage(new SimpleMessage("Hello World"))
                .build();

        String result = layout.toSerializable(event);

        assertThat(result).contains("\"level\":\"INFO\"");
        assertThat(result).contains("\"message\":\"Hello World\"");
        assertThat(result).contains("\"logger\":\"TestLogger\"");
        assertThat(result).contains("\"timestamp\"");
    }

    @Test
    void toSerializable_isSingleLine() {
        LogEvent event = Log4jLogEvent.newBuilder()
                .setLevel(Level.DEBUG)
                .setLoggerName("TestLogger")
                .setMessage(new SimpleMessage("single line test"))
                .build();

        String result = layout.toSerializable(event);
        // Should end with a single newline and not have newlines in the JSON
        String jsonPart = result.trim();
        assertThat(jsonPart).doesNotContain("\n");
    }

    @Test
    void toSerializable_withException_includesExceptionInfo() {
        LogEvent event = Log4jLogEvent.newBuilder()
                .setLevel(Level.ERROR)
                .setLoggerName("TestLogger")
                .setMessage(new SimpleMessage("Error occurred"))
                .setThrown(new RuntimeException("test exception"))
                .build();

        String result = layout.toSerializable(event);

        assertThat(result).contains("\"exception\"");
        assertThat(result).contains("\"class\":\"java.lang.RuntimeException\"");
        assertThat(result).contains("test exception");
    }

    @Test
    void toSerializable_includesThreadInfo() {
        LogEvent event = Log4jLogEvent.newBuilder()
                .setLevel(Level.INFO)
                .setLoggerName("TestLogger")
                .setMessage(new SimpleMessage("thread test"))
                .build();

        String result = layout.toSerializable(event);
        assertThat(result).contains("\"thread\"");
    }

    @Test
    void getEnvOrProperty_returnsDefaultWhenNotSet() {
        String result = layout.getEnvOrProperty("NON_EXISTENT_KEY_XYZ", "defaultVal");
        assertThat(result).isEqualTo("defaultVal");
    }

    @Test
    void getEnvOrProperty_returnsSystemProperty() {
        System.setProperty("TEST_LAYOUT_PROP", "testValue");
        try {
            String result = layout.getEnvOrProperty("TEST_LAYOUT_PROP", "default");
            assertThat(result).isEqualTo("testValue");
        } finally {
            System.clearProperty("TEST_LAYOUT_PROP");
        }
    }

    @Test
    void toSerializable_withContextData_includesContext() {
        org.apache.logging.log4j.ThreadContext.put("crId", "test-correlation-id");
        try {
            LogEvent event = Log4jLogEvent.newBuilder()
                    .setLevel(Level.INFO)
                    .setLoggerName("TestLogger")
                    .setMessage(new SimpleMessage("context test"))
                    .build();

            String result = layout.toSerializable(event);
            assertThat(result).contains("\"context\"");
        } finally {
            org.apache.logging.log4j.ThreadContext.clearAll();
        }
    }

    @Test
    void toSerializable_serviceMetadataIncluded_whenEnvSet() {
        System.setProperty("SERVICEID", "svc-001");
        System.setProperty("SERVICENAME", "test-service");
        System.setProperty("SERVICEBUILDID", "build-1.0");
        try {
            SimpleJSONLayout layoutWithMeta = SimpleJSONLayout.createLayout();
            LogEvent event = Log4jLogEvent.newBuilder()
                    .setLevel(Level.INFO)
                    .setLoggerName("TestLogger")
                    .setMessage(new SimpleMessage("meta test"))
                    .build();

            String result = layoutWithMeta.toSerializable(event);
            assertThat(result).contains("svcId");
            assertThat(result).contains("svcNm");
            assertThat(result).contains("svcBld");
        } finally {
            System.clearProperty("SERVICEID");
            System.clearProperty("SERVICENAME");
            System.clearProperty("SERVICEBUILDID");
        }
    }
}
