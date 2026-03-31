package com.wu.era.library.logger;

import org.apache.logging.log4j.Level;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class WUCustomLoggerTest {

    @Test
    void auditLevel_hasCorrectPriority() {
        assertThat(WUCustomLogger.AUDIT).isNotNull();
        assertThat(WUCustomLogger.AUDIT.intLevel()).isEqualTo(250);
        assertThat(WUCustomLogger.AUDIT.name()).isEqualTo("AUDIT");
    }

    @Test
    void auditLevel_sitsBetweenFatalAndInfo() {
        // FATAL = 100, AUDIT = 250, INFO = 400
        // Lower intLevel = higher severity
        assertThat(WUCustomLogger.AUDIT.intLevel()).isGreaterThan(Level.FATAL.intLevel());
        assertThat(WUCustomLogger.AUDIT.intLevel()).isLessThan(Level.INFO.intLevel());
    }

    @Test
    void getLogger_byClass_returnsInstance() {
        WUCustomLogger logger = WUCustomLogger.getLogger(WUCustomLoggerTest.class);
        assertThat(logger).isNotNull();
    }

    @Test
    void getLogger_byName_returnsInstance() {
        WUCustomLogger logger = WUCustomLogger.getLogger("test.logger");
        assertThat(logger).isNotNull();
    }

    @Test
    void getLogger_withExistingWULogger_returnsSame() {
        WUCustomLogger logger1 = WUCustomLogger.getLogger("test.wrap.logger");
        WUCustomLogger logger2 = WUCustomLogger.getLogger(logger1);
        assertThat(logger2).isSameAs(logger1);
    }

    @Test
    void audit_withMessage_doesNotThrow() {
        WUCustomLogger logger = WUCustomLogger.getLogger(WUCustomLoggerTest.class);
        logger.audit("audit message");
    }

    @Test
    void audit_withMessageAndParams_doesNotThrow() {
        WUCustomLogger logger = WUCustomLogger.getLogger(WUCustomLoggerTest.class);
        logger.audit("audit message {}", "param1");
    }

    @Test
    void audit_withMessageAndThrowable_doesNotThrow() {
        WUCustomLogger logger = WUCustomLogger.getLogger(WUCustomLoggerTest.class);
        logger.audit("audit error", new RuntimeException("test"));
    }

    @Test
    void audit_withMarker_doesNotThrow() {
        WUCustomLogger logger = WUCustomLogger.getLogger(WUCustomLoggerTest.class);
        logger.audit(null, "audit with marker");
    }

    @Test
    void audit_withMessage_object_doesNotThrow() {
        WUCustomLogger logger = WUCustomLogger.getLogger(WUCustomLoggerTest.class);
        org.apache.logging.log4j.message.SimpleMessage msg = new org.apache.logging.log4j.message.SimpleMessage("test");
        logger.audit(msg);
    }

    @Test
    void isAuditEnabled_returnsBoolean() {
        WUCustomLogger logger = WUCustomLogger.getLogger(WUCustomLoggerTest.class);
        // Just check it doesn't throw - result depends on log configuration
        boolean result = logger.isAuditEnabled();
        assertThat(result).isIn(true, false);
    }

    @Test
    void getLogger_byLogger_returnsWULogger() {
        org.apache.logging.log4j.Logger log4jLogger = org.apache.logging.log4j.LogManager.getLogger("test.plain.logger");
        WUCustomLogger wuLogger = WUCustomLogger.getLogger(log4jLogger);
        assertThat(wuLogger).isNotNull();
    }
}
