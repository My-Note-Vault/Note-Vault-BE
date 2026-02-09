package com.example.platformservice.auth.component;

import jakarta.servlet.Filter;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;


@RequiredArgsConstructor
@Component
public class JwtSecurityAutoConfig {

    @Bean
    public FilterRegistrationBean<Filter> jwtFilterRegistration(JwtFilter jwtFilter) {
        FilterRegistrationBean<Filter> registration = new FilterRegistrationBean<>();
        registration.setFilter(jwtFilter);
        registration.addUrlPatterns("/*");// 모든 요청 필터링
        registration.setOrder(1); // 필터 순서
        return registration;
    }

}
