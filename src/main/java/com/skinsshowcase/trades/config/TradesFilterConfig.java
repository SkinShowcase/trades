package com.skinsshowcase.trades.config;

import com.skinsshowcase.trades.filter.TradesOwnerJwtFilter;
import com.skinsshowcase.trades.service.JwtSupportService;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TradesFilterConfig {

    @Bean
    public FilterRegistrationBean<TradesOwnerJwtFilter> tradesOwnerJwtFilterRegistration(JwtSupportService jwtSupportService) {
        var registration = new FilterRegistrationBean<TradesOwnerJwtFilter>();
        registration.setFilter(new TradesOwnerJwtFilter(jwtSupportService));
        registration.addUrlPatterns("/api/v1/trades/*");
        registration.setOrder(1);
        return registration;
    }
}
