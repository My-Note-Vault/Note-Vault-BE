package com.example.search.chat.api;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.Instant;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class ChatApiContractTest {
    private static final UUID REQUEST_ID = UUID.fromString("b832252b-7804-42a8-bb79-f00c5c0f2442");
    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void createValidator() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void closeValidator() {
        factory.close();
    }

    @ParameterizedTest
    @MethodSource("invalidRunRequests")
    @DisplayName("실행 요청은 중복 방지 ID와 유효한 질문을 요구한다")
    void validatesRunRequest(UUID requestId, String question, String invalidField) {
        var violations = validator.validate(new ChatApiContract.CreateRunRequest(requestId, question));

        assertThat(violations).extracting(violation -> violation.getPropertyPath().toString())
                .containsExactly(invalidField);
    }

    static Stream<Arguments> invalidRunRequests() {
        return Stream.of(Arguments.of(null, "질문", "requestId"),
                Arguments.of(REQUEST_ID, null, "question"), Arguments.of(REQUEST_ID, "", "question"),
                Arguments.of(REQUEST_ID, " \n\t", "question"),
                Arguments.of(REQUEST_ID, "가".repeat(4001), "question"));
    }

    @Test
    @DisplayName("실행 요청의 질문 길이 상한 4,000자를 허용한다")
    void acceptsRunRequestAtLengthLimit() {
        assertThat(validator.validate(new ChatApiContract.CreateRunRequest(REQUEST_ID, "가".repeat(4000))))
                .isEmpty();
    }

    @Test
    @DisplayName("세션 제목은 생략하거나 100자까지 입력할 수 있고 101자는 거부한다")
    void validatesOptionalSessionTitleBoundary() {
        assertThat(validator.validate(new ChatApiContract.CreateSessionRequest(null))).isEmpty();
        assertThat(validator.validate(new ChatApiContract.CreateSessionRequest("가".repeat(100)))).isEmpty();
        assertThat(validator.validate(new ChatApiContract.CreateSessionRequest("가".repeat(101))))
                .extracting(violation -> violation.getPropertyPath().toString()).containsExactly("title");
    }

    @Test
    @DisplayName("실행 이벤트는 약속된 소문자 이름·순번·UTC 시각·payload로 직렬화하고 복원한다")
    void roundTripsRunEventWireFormat() throws Exception {
        ObjectMapper mapper = JsonMapper.builder().addModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS).build();
        var event = new ChatApiContract.RunEvent<>(REQUEST_ID, 3, ChatApiContract.EventType.TOOL_STARTED,
                Instant.parse("2026-10-08T01:00:00Z"),
                new ChatApiContract.ToolStartedPayload("call_1", "search_documents"));

        String json = mapper.writeValueAsString(event);
        JsonNode payload = mapper.readTree(json);

        assertThat(payload.path("runId").asText()).isEqualTo(REQUEST_ID.toString());
        assertThat(payload.path("sequence").asLong()).isEqualTo(3);
        assertThat(payload.path("type").asText()).isEqualTo("tool_started");
        assertThat(payload.path("occurredAt").asText()).isEqualTo("2026-10-08T01:00:00Z");
        assertThat(payload.path("data").path("callId").asText()).isEqualTo("call_1");
        assertThat(payload.path("data").path("toolName").asText()).isEqualTo("search_documents");
        ChatApiContract.RunEvent<ChatApiContract.ToolStartedPayload> restored = mapper.readValue(json,
                new TypeReference<>() {});
        assertThat(restored).isEqualTo(event);
    }
}
