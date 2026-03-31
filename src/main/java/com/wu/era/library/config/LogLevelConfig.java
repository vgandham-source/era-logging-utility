package com.wu.era.library.config;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.PropertySource;

import java.util.Arrays;

/**
 * FR-11: Scans Spring environment for loglevel.* properties and applies them as Log4j2 level overrides.
 */
@org.springframework.context.annotation.Configuration
public class LogLevelConfig {

    private static final Logger log = LogManager.getLogger(LogLevelConfig.class);
    private static final String LOG_LEVEL_PREFIX = "loglevel.";

    @Bean
    public LogLevelConfigurer logLevelConfigurer(ConfigurableEnvironment environment) {
        return new LogLevelConfigurer(environment);
    }

    public static class LogLevelConfigurer {

        public LogLevelConfigurer(ConfigurableEnvironment environment) {
            applyLogLevels(environment);
        }

        void applyLogLevels(ConfigurableEnvironment environment) {
            LoggerContext context = (LoggerContext) LogManager.getContext(false);
            Configuration config = context.getConfiguration();

            for (PropertySource<?> propertySource : environment.getPropertySources()) {
                if (propertySource instanceof EnumerablePropertySource<?> enumerable) {
                    Arrays.stream(enumerable.getPropertyNames())
                            .filter(name -> name.startsWith(LOG_LEVEL_PREFIX))
                            .forEach(name -> {
                                String loggerName = name.substring(LOG_LEVEL_PREFIX.length());
                                String levelStr = environment.getProperty(name);
                                applyLevel(config, loggerName, levelStr);
                            });
                }
            }

            context.updateLoggers();
        }

        private void applyLevel(Configuration config, String loggerName, String levelStr) {
            if (levelStr == null || levelStr.isEmpty()) {
                return;
            }
            Level level = Level.toLevel(levelStr, null);
            if (level == null) {
                log.warn("Invalid log level '{}' for logger '{}'", levelStr, loggerName);
                return;
            }

            if ("root".equalsIgnoreCase(loggerName)) {
                config.getRootLogger().setLevel(level);
                log.info("Set root logger level to {}", level);
            } else {
                LoggerConfig loggerConfig = config.getLoggerConfig(loggerName);
                if (loggerConfig.getName().equals(loggerName)) {
                    // Exact match - update existing config
                    loggerConfig.setLevel(level);
                } else {
                    // No exact match - create new logger config
                    LoggerConfig newConfig = new LoggerConfig(loggerName, level, true);
                    config.addLogger(loggerName, newConfig);
                }
                log.info("Set logger '{}' level to {}", loggerName, level);
            }
        }
    }
}
