package com.wu.era.library.config;

import com.wu.era.library.filter.AppRequestFilter;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

/**
 * Spring Boot Auto-configuration for ERA Logging Utility.
 */
@AutoConfiguration
@ConditionalOnWebApplication
@Import(LogLevelConfig.class)
public class EraLoggingAutoConfiguration {

    @Bean
    public AppRequestFilter appRequestFilter() {
        return new AppRequestFilter();
    }
}
