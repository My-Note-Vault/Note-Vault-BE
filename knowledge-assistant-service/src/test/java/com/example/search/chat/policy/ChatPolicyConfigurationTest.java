package com.example.search.chat.policy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.core.env.StandardEnvironment;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class ChatPolicyConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withInitializer(context -> {
                // Developer machine environment variables must not change expected defaults.
                context.getEnvironment().getPropertySources()
                        .remove(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME);
                context.getEnvironment().getPropertySources()
                        .remove(StandardEnvironment.SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME);
            })
            .withInitializer(new ConfigDataApplicationContextInitializer())
            .withPropertyValues("spring.config.location=classpath:application-chat.yaml")
            .withUserConfiguration(ChatPolicyConfiguration.class);

    @Test
    @DisplayName("실제 application-chat.yaml에서 일반 환경의 정책을 읽는다")
    void loadsDefaultPolicy() {
        runner.run(context -> {
            assertThat(context).hasNotFailed().hasSingleBean(ChatPolicyProperties.class);
            ChatPolicyProperties policy = context.getBean(ChatPolicyProperties.class);
            assertThat(policy.dailyTokenLimit()).isEqualTo(100_000);
            assertThat(policy.maxRunTokens()).isEqualTo(20_000);
            assertThat(policy.maxToolCalls()).isEqualTo(5);
            assertThat(policy.maxModelCalls()).isEqualTo(6);
            assertThat(policy.runTimeout()).isEqualTo(Duration.ofSeconds(120));
        });
    }

    @ParameterizedTest
    @CsvSource({"prod, 100000", "local, 1000000", "dev, 1000000"})
    @DisplayName("프로필에 따라 일반·개발 한도 기본값을 적용한다")
    void selectsProfileDefaults(String profile, long expectedLimit) {
        runner.withPropertyValues("spring.profiles.active=" + profile).run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context.getBean(ChatPolicyProperties.class).dailyTokenLimit()).isEqualTo(expectedLimit);
        });
    }

    @ParameterizedTest
    @ValueSource(strings = {"prod", "local", "dev"})
    @DisplayName("명시한 환경변수 값이 모든 프로필의 기본 한도보다 우선한다")
    void explicitLimitOverridesProfileDefault(String profile) {
        runner.withPropertyValues("spring.profiles.active=" + profile, "CHAT_DAILY_TOKEN_LIMIT=1000")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    ChatPolicyProperties policy = context.getBean(ChatPolicyProperties.class);
                    assertThat(policy.dailyTokenLimit()).isEqualTo(1_000);
                    // A small development daily limit need not exceed the per-run limit.
                    assertThat(policy.maxRunTokens()).isEqualTo(20_000);
                });
    }

    @Test
    @DisplayName("도구 없는 실행과 최소 1초 제한을 포함한 실행 정책을 환경변수로 변경한다")
    void bindsExecutionOverrides() {
        runner.withPropertyValues("CHAT_MAX_RUN_TOKENS=250", "CHAT_MAX_TOOL_CALLS=0",
                        "CHAT_MAX_MODEL_CALLS=1", "CHAT_RUN_TIMEOUT=1s")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    ChatPolicyProperties policy = context.getBean(ChatPolicyProperties.class);
                    assertThat(policy.maxRunTokens()).isEqualTo(250);
                    assertThat(policy.maxToolCalls()).isZero();
                    assertThat(policy.maxModelCalls()).isEqualTo(1);
                    assertThat(policy.runTimeout()).isEqualTo(Duration.ofSeconds(1));
                });
    }

    @ParameterizedTest
    @CsvSource({
            "chat.policy.daily-token-limit=0, daily-token-limit",
            "chat.policy.daily-token-limit=-1, daily-token-limit",
            "chat.policy.max-run-tokens=0, max-run-tokens",
            "chat.policy.max-run-tokens=-1, max-run-tokens",
            "chat.policy.max-tool-calls=-1, max-tool-calls",
            "chat.policy.max-model-calls=0, max-model-calls",
            "chat.policy.max-model-calls=-1, max-model-calls",
            "chat.policy.run-timeout=0s, run-timeout",
            "chat.policy.run-timeout=-1s, run-timeout",
            "chat.policy.run-timeout=999ms, run-timeout"
    })
    @DisplayName("잘못된 한도나 시간 설정은 기동 시 거부한다")
    void rejectsInvalidConfigurationAtStartup(String setting, String property) {
        runner.withPropertyValues(setting).run(context -> {
            assertThat(context).hasFailed();
            assertThat(context.getStartupFailure())
                    .hasRootCauseInstanceOf(IllegalArgumentException.class)
                    .hasStackTraceContaining("chat.policy." + property);
        });
    }
}
