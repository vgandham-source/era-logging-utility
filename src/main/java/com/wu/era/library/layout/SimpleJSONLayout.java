package com.wu.era.library.layout;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.layout.AbstractStringLayout;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * FR-01: Custom Log4j2 layout that serializes LogEvents as single-line JSON objects.
 * FR-13: Includes service metadata from environment variables.
 */
@Plugin(name = "SimpleJSONLayout", category = "Core", elementType = Layout.ELEMENT_TYPE, printObject = true)
public class SimpleJSONLayout extends AbstractStringLayout {

    private static final DateTimeFormatter ISO_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").withZone(ZoneOffset.UTC);

    private static final ObjectMapper MAPPER = new ObjectMapper();

    // FR-13: Service metadata keys
    private static final String ENV_SERVICE_ID = "SERVICEID";
    private static final String ENV_SERVICE_NAME = "SERVICENAME";
    private static final String ENV_SERVICE_BUILD_ID = "SERVICEBUILDID";

    private final String serviceId;
    private final String serviceName;
    private final String serviceBuildId;

    protected SimpleJSONLayout() {
        super(StandardCharsets.UTF_8);
        this.serviceId = getEnvOrProperty(ENV_SERVICE_ID, "");
        this.serviceName = getEnvOrProperty(ENV_SERVICE_NAME, "");
        this.serviceBuildId = getEnvOrProperty(ENV_SERVICE_BUILD_ID, "");
    }

    @PluginFactory
    public static SimpleJSONLayout createLayout() {
        return new SimpleJSONLayout();
    }

    @Override
    public String toSerializable(LogEvent event) {
        try {
            ObjectNode root = MAPPER.createObjectNode();

            // Timestamp
            String timestamp = ISO_FORMATTER.format(Instant.ofEpochMilli(event.getTimeMillis()));
            root.put("timestamp", timestamp);

            // Level and logger
            root.put("level", event.getLevel().name());
            root.put("logger", event.getLoggerName());
            root.put("thread", event.getThreadName());

            // Message
            String message = event.getMessage().getFormattedMessage();
            root.put("message", message);

            // Exception if present
            if (event.getThrown() != null) {
                Throwable thrown = event.getThrown();
                ObjectNode exNode = MAPPER.createObjectNode();
                exNode.put("class", thrown.getClass().getName());
                exNode.put("message", thrown.getMessage() != null ? thrown.getMessage() : "");
                root.set("exception", exNode);
            }

            // ThreadContext (MDC) values
            Map<String, String> contextData = event.getContextData().toMap();
            if (!contextData.isEmpty()) {
                ObjectNode ctxNode = MAPPER.createObjectNode();
                contextData.forEach(ctxNode::put);

                // FR-13: Add service metadata
                if (!serviceId.isEmpty()) ctxNode.put("svcId", serviceId);
                if (!serviceName.isEmpty()) ctxNode.put("svcNm", serviceName);
                if (!serviceBuildId.isEmpty()) ctxNode.put("svcBld", serviceBuildId);

                root.set("context", ctxNode);
            } else {
                // FR-13: Always include service metadata
                ObjectNode ctxNode = MAPPER.createObjectNode();
                if (!serviceId.isEmpty()) ctxNode.put("svcId", serviceId);
                if (!serviceName.isEmpty()) ctxNode.put("svcNm", serviceName);
                if (!serviceBuildId.isEmpty()) ctxNode.put("svcBld", serviceBuildId);
                if (ctxNode.size() > 0) {
                    root.set("context", ctxNode);
                }
            }

            return MAPPER.writeValueAsString(root) + System.lineSeparator();
        } catch (Exception e) {
            return "{\"level\":\"ERROR\",\"message\":\"Failed to serialize log event: " + e.getMessage() + "\"}" + System.lineSeparator();
        }
    }

    String getEnvOrProperty(String key, String defaultValue) {
        String value = System.getenv(key);
        if (value == null || value.isEmpty()) {
            value = System.getProperty(key, defaultValue);
        }
        return value;
    }
}
