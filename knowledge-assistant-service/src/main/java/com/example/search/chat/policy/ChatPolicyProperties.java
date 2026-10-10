package com.example.search.chat.policy;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;

/** Declares limits. Durable reservation/settlement and run enforcement are added in steps 4 and 6. */
@ConfigurationProperties(prefix = "chat.policy")
public record ChatPolicyProperties(
        @DefaultValue("100000") long dailyTokenLimit,
        @DefaultValue("20000") long maxRunTokens,
        @DefaultValue("5") int maxToolCalls,
        @DefaultValue("6") int maxModelCalls,
        @DefaultValue("120s") Duration runTimeout) {

    public ChatPolicyProperties {
        if (dailyTokenLimit <= 0) {
            throw new IllegalArgumentException("chat.policy.daily-token-limit must be positive");
        }
        if (maxRunTokens <= 0) {
            throw new IllegalArgumentException("chat.policy.max-run-tokens must be positive");
        }
        if (maxToolCalls < 0) {
            throw new IllegalArgumentException("chat.policy.max-tool-calls must be zero or positive");
        }
        if (maxModelCalls <= 0) {
            throw new IllegalArgumentException("chat.policy.max-model-calls must be positive");
        }
        if (runTimeout == null || runTimeout.compareTo(Duration.ofSeconds(1)) < 0) {
            throw new IllegalArgumentException("chat.policy.run-timeout must be at least one second");
        }
    }
}
