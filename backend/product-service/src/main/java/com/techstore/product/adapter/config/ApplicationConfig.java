package com.techstore.product.adapter.config;

import java.time.Clock;
import com.techstore.product.adapter.http.CorrelationIdFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

@Configuration
public class ApplicationConfig {
    @Bean
    Clock clock() { return Clock.systemUTC(); }

    @Bean
    CorrelationIdFilter correlationIdFilter() { return new CorrelationIdFilter(); }

    @Bean
    FilterRegistrationBean<CorrelationIdFilter> correlationIdFilterRegistration(CorrelationIdFilter filter) {
        FilterRegistrationBean<CorrelationIdFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return registration;
    }
}