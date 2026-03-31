package com.wu.era.library.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class LogLevelConfigTest {

    @Test
    void logLevelConfigurer_appliesRootLogLevel() {
        MockEnvironment env = new MockEnvironment();
        env.setProperty("loglevel.root", "WARN");

        assertThatCode(() -> new LogLevelConfig.LogLevelConfigurer(env))
                .doesNotThrowAnyException();
    }

    @Test
    void logLevelConfigurer_appliesPackageLogLevel() {
        MockEnvironment env = new MockEnvironment();
        env.setProperty("loglevel.com.wu.era", "DEBUG");

        assertThatCode(() -> new LogLevelConfig.LogLevelConfigurer(env))
                .doesNotThrowAnyException();
    }

    @Test
    void logLevelConfigurer_ignoresInvalidLogLevel() {
        MockEnvironment env = new MockEnvironment();
        env.setProperty("loglevel.com.wu.era", "INVALID_LEVEL");

        assertThatCode(() -> new LogLevelConfig.LogLevelConfigurer(env))
                .doesNotThrowAnyException();
    }

    @Test
    void logLevelConfigurer_ignoresNonLogLevelProperties() {
        MockEnvironment env = new MockEnvironment();
        env.setProperty("server.port", "8080");
        env.setProperty("spring.application.name", "test-app");

        assertThatCode(() -> new LogLevelConfig.LogLevelConfigurer(env))
                .doesNotThrowAnyException();
    }

    @Test
    void logLevelConfigurer_withMultipleLevels() {
        MockEnvironment env = new MockEnvironment();
        env.setProperty("loglevel.root", "INFO");
        env.setProperty("loglevel.com.wu.era", "DEBUG");
        env.setProperty("loglevel.org.springframework", "WARN");

        assertThatCode(() -> new LogLevelConfig.LogLevelConfigurer(env))
                .doesNotThrowAnyException();
    }

    @Test
    void logLevelConfigurer_withEmptyEnvironment() {
        MockEnvironment env = new MockEnvironment();

        assertThatCode(() -> new LogLevelConfig.LogLevelConfigurer(env))
                .doesNotThrowAnyException();
    }

    @Test
    void logLevelConfig_createsBeanSuccessfully() {
        LogLevelConfig config = new LogLevelConfig();
        MockEnvironment env = new MockEnvironment();

        LogLevelConfig.LogLevelConfigurer configurer = config.logLevelConfigurer(env);
        assertThat(configurer).isNotNull();
    }
}
