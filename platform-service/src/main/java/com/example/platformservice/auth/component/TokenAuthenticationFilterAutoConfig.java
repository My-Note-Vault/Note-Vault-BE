package com.example.platformservice.auth.component;

import com.example.common.jwt.JwtService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.Filter;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;


@RequiredArgsConstructor
@Component
public class TokenAuthenticationFilterAutoConfig {

    @Bean
    public JwtExceptionFilter jwtExceptionFilter(ObjectMapper objectMapper) {
        return new JwtExceptionFilter(objectMapper);
    }

    @Bean
    public TokenAuthenticationFilter tokenAuthenticationFilter(JwtService jwtService) {
        return new TokenAuthenticationFilter(jwtService);
    }

    @Bean
    public FilterRegistrationBean<Filter> jwtExceptionFilterRegistration(final JwtExceptionFilter jwtExceptionFilter) {
        FilterRegistrationBean<Filter> registration = new FilterRegistrationBean<>();
        registration.setFilter(jwtExceptionFilter);
        registration.addUrlPatterns("/*");
        registration.setOrder(1);
        return registration;
    }

    @Bean
    public FilterRegistrationBean<Filter> tokenAuthenticationFilterRegistration(final TokenAuthenticationFilter tokenAuthenticationFilter) {
        FilterRegistrationBean<Filter> registration = new FilterRegistrationBean<>();
        registration.setFilter(tokenAuthenticationFilter);
        registration.addUrlPatterns("/*");
        registration.setOrder(2);
        return registration;
    }

}
