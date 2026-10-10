package com.example.search.chat.policy;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(ChatPolicyProperties.class)
public class ChatPolicyConfiguration {

    /** Only quota calendar calculations use this clock; auth and execution deadlines do not. */
    @Bean("chatQuotaClock")
    public Clock chatQuotaClock() {
        return Clock.systemUTC();
    }
}
