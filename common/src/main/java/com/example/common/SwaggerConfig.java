package com.example.common;

import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

@Configuration
@EnableWebMvc
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT"
)
public class SwaggerConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .components(new Components());
    }

    @Bean
    public GroupedOpenApi memberApi() {
        return GroupedOpenApi.builder()
                .group("member")
                .pathsToMatch("/api/v1/members/**")
                .build();
    }

    @Bean
    public GroupedOpenApi dailyNoteApi() {
        return GroupedOpenApi.builder()
                .group("daily-notes")
                .pathsToMatch("/api/v1/daily-notes/**")
                .build();
    }

    @Bean
    public GroupedOpenApi workSpaceApi() {
        return GroupedOpenApi.builder()
                .group("WorkSpace")
                .pathsToMatch("/api/v1/workspaces/**")
                .build();
    }

    @Bean
    public GroupedOpenApi unfoldedNotesApi() {
        return GroupedOpenApi.builder()
                .group("unflolded-notes")
                .pathsToMatch("/api/v1/unfolded-notes/**")
                .build();
    }

    @Bean
    public GroupedOpenApi documentApi() {
        return GroupedOpenApi.builder()
                .group("documents")
                .pathsToMatch("/api/v1/documents/**")
                .build();
    }

}
