package com.wu.era.library.logger;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.message.MessageFactory;
import org.apache.logging.log4j.spi.AbstractLogger;
import org.apache.logging.log4j.spi.ExtendedLoggerWrapper;

/**
 * FR-09: Custom logger wrapper that provides AUDIT log level (priority 250).
 * AUDIT level sits between FATAL (100) and INFO (400).
 */
public class WUCustomLogger extends ExtendedLoggerWrapper {

    private static final long serialVersionUID = 1L;

    /**
     * FR-09: Custom AUDIT level with numeric priority 250 (between FATAL=100 and INFO=400).
     */
    public static final Level AUDIT = Level.forName("AUDIT", 250);

    private static final String FQCN = WUCustomLogger.class.getName();

    protected WUCustomLogger(AbstractLogger logger, String name, MessageFactory messageFactory) {
        super(logger, name, messageFactory);
    }

    /**
     * Factory method - creates a WUCustomLogger for the given class.
     */
    public static WUCustomLogger getLogger(Class<?> clazz) {
        Logger logger = LogManager.getLogger(clazz);
        return getLogger(logger);
    }

    /**
     * Factory method - creates a WUCustomLogger for the given name.
     */
    public static WUCustomLogger getLogger(String name) {
        Logger logger = LogManager.getLogger(name);
        return getLogger(logger);
    }

    /**
     * Factory method - wraps an existing Logger.
     */
    public static WUCustomLogger getLogger(Logger logger) {
        if (logger instanceof WUCustomLogger) {
            return (WUCustomLogger) logger;
        }
        return new WUCustomLogger((AbstractLogger) logger, logger.getName(), logger.getMessageFactory());
    }

    /**
     * FR-09: Emit an AUDIT log event with the given message.
     */
    public void audit(String message) {
        logIfEnabled(FQCN, AUDIT, null, message, (Throwable) null);
    }

    /**
     * FR-09: Emit an AUDIT log event with the given message and parameters.
     */
    public void audit(String message, Object... params) {
        logIfEnabled(FQCN, AUDIT, null, message, params);
    }

    /**
     * FR-09: Emit an AUDIT log event with a message and throwable.
     */
    public void audit(String message, Throwable throwable) {
        logIfEnabled(FQCN, AUDIT, null, message, throwable);
    }

    /**
     * FR-09: Emit an AUDIT log event with a Marker.
     */
    public void audit(Marker marker, String message) {
        logIfEnabled(FQCN, AUDIT, marker, message, (Throwable) null);
    }

    /**
     * FR-09: Emit an AUDIT log event with a Message object.
     */
    public void audit(Message message) {
        logIfEnabled(FQCN, AUDIT, null, message, (Throwable) null);
    }

    /**
     * Check if AUDIT level is enabled.
     */
    public boolean isAuditEnabled() {
        return isEnabled(AUDIT);
    }
}
