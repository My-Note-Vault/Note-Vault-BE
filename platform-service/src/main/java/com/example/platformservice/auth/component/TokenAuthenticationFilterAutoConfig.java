package com.example.platformservice.auth.component;

import com.example.common.jwt.JwtService;
import jakarta.servlet.Filter;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;


@RequiredArgsConstructor
@Component
public class TokenAuthenticationFilterAutoConfig {

    @Bean
    public TokenAuthenticationFilter tokenAuthenticationFilter(JwtService jwtService) {
        return new TokenAuthenticationFilter(jwtService);
    }

    @Bean
    public FilterRegistrationBean<Filter> tokenAuthenticationFilterRegistration(final TokenAuthenticationFilter tokenAuthenticationFilter) {
        FilterRegistrationBean<Filter> registration = new FilterRegistrationBean<>();
        registration.setFilter(tokenAuthenticationFilter);
        registration.addUrlPatterns("/*");// 모든 요청 필터링
        registration.setOrder(1); // 필터 순서
        return registration;
    }

}
