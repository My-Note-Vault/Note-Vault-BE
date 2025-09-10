package com.example.platformservice.auth.component;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

@RequiredArgsConstructor
@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final JwtInterceptor jwtInterceptor;
    private final LoginMemberArgumentResolver loginMemberArgumentResolver;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(jwtInterceptor)
                .addPathPatterns("/api/v1/**", "/admin/**") // 보호할 엔드포인트
                .excludePathPatterns("/api/v1/oauth/**"); // 로그인은 제외
    }

    @Override
    public void addArgumentResolvers(
            final List<HandlerMethodArgumentResolver> resolvers
    ) {
        resolvers.add(loginMemberArgumentResolver);
    }
}
