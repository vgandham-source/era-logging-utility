package com.wu.era.library.config;

import com.wu.era.library.filter.AppRequestFilter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = {EraLoggingAutoConfiguration.class, LogLevelConfig.class})
class EraLoggingAutoConfigurationTest {

    @Autowired
    private ApplicationContext context;

    @Test
    void contextLoads() {
        assertThat(context).isNotNull();
    }

    @Test
    void appRequestFilterBeanCreated() {
        assertThat(context.containsBean("appRequestFilter")).isTrue();
    }
}
