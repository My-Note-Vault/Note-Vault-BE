package com.example.search.sync.relay;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Validated
@ConfigurationProperties("search.sync.relay")
public record RelayProperties(
        @NotBlank String queueUrl,
        @NotBlank String region,
        @NotNull @DefaultValue("1s") Duration pollDelay
) {
    public RelayProperties {
        if (pollDelay != null && pollDelay.toMillis() < 1) {
            throw new IllegalArgumentException("search.sync.relay.poll-delay must be at least 1ms");
        }
    }
}
