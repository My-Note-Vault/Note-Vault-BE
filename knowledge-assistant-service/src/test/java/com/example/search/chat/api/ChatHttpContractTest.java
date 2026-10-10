package com.example.search.chat.api;

import com.example.common.AuthArgumentResolver;
import com.example.common.CommonConstant;
import com.example.common.exception.GlobalControllerAdvice;
import com.example.search.chat.ChatController;
import com.example.search.chat.SemanticChatService;
import com.example.search.chat.policy.ChatPolicyProperties;
import com.example.search.chat.policy.ChatQuotaCalendar;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ChatHttpContractTest {
    private final ObjectMapper mapper = Jackson2ObjectMapperBuilder.json()
            .featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS).build();
    private SemanticChatService service;
    private LocalValidatorFactoryBean validator;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        service = mock(SemanticChatService.class);
        validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        ChatPolicyController policyController = new ChatPolicyController(
                new ChatPolicyProperties(4321, 900, 2, 3, Duration.ofSeconds(15)),
                new ChatQuotaCalendar(Clock.fixed(Instant.parse("2026-10-08T15:00:00Z"), ZoneOffset.UTC)),
                512);
        mvc = MockMvcBuilders.standaloneSetup(new ChatController(service), policyController)
                .setCustomArgumentResolvers(new AuthArgumentResolver())
                // The scoped chat advice must take precedence over the existing catch-all advice.
                .setControllerAdvice(new GlobalControllerAdvice(), new ChatControllerAdvice())
                .setMessageConverters(new MappingJackson2HttpMessageConverter(mapper))
                .setValidator(validator)
                .build();
    }

    @AfterEach
    void tearDown() {
        validator.close();
    }

    @Test
    @DisplayName("인증된 정책 조회는 설정값과 한국 시간 다음 자정을 반환하고 캐시를 금지한다")
    void returnsConfiguredPolicyWithKoreanResetTime() throws Exception {
        mvc.perform(authenticated(get("/api/v1/chat/policy")))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.quotaScope").value("USER"))
                .andExpect(jsonPath("$.timezone").value("Asia/Seoul"))
                .andExpect(jsonPath("$.resetTime").value("00:00:00"))
                .andExpect(jsonPath("$.quotaDate").value("2026-10-09"))
                .andExpect(jsonPath("$.resetAt").value("2026-10-09T15:00:00Z"))
                .andExpect(jsonPath("$.dailyTokenLimit").value(4321))
                .andExpect(jsonPath("$.maxQuestionLength").value(4000))
                .andExpect(jsonPath("$.maxConcurrentRunsPerSession").value(1))
                .andExpect(jsonPath("$.runLimits.maxTokens").value(900))
                .andExpect(jsonPath("$.runLimits.maxToolCalls").value(2))
                .andExpect(jsonPath("$.runLimits.maxModelCalls").value(3))
                .andExpect(jsonPath("$.runLimits.timeoutMillis").value(15000))
                .andExpect(jsonPath("$.runLimits.maxOutputTokensPerCall").value(512))
                .andExpect(jsonPath("$.chargedPurposes").value(
                        contains("MODEL_RESPONSE", "QUERY_EMBEDDING", "CONVERSATION_SUMMARY")))
                .andExpect(jsonPath("$.excludedPurposes").value(contains("DOCUMENT_INDEXING")))
                .andExpect(jsonPath("$.used").doesNotExist())
                .andExpect(jsonPath("$.remaining").doesNotExist());

        verifyNoInteractions(service);
    }

    @Test
    @DisplayName("정책 조회에 인증 정보가 없으면 기존 401 오류 규격을 유지한다")
    void policyRequiresAuthentication() throws Exception {
        mvc.perform(get("/api/v1/chat/policy"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED_ERROR"));
        verifyNoInteractions(service);
    }

    @ParameterizedTest
    @ValueSource(strings = {"/api/v1/chat", "/api/v1/chat/stream"})
    @DisplayName("일반·스트리밍 채팅은 인증된 사용자 없이 실행하지 않는다")
    void chatRequiresAuthentication(String path) throws Exception {
        mvc.perform(post(path).contentType(MediaType.APPLICATION_JSON).content("{\"question\":\"질문\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED_ERROR"))
                .andExpect(request().asyncNotStarted());
        verifyNoInteractions(service);
    }

    @ParameterizedTest
    @MethodSource("invalidRequests")
    @DisplayName("잘못된 질문·JSON은 SSE나 서비스 호출 전에 400과 채팅 오류 코드로 응답한다")
    void rejectsInvalidInputBeforeStartingWork(String path, String body) throws Exception {
        mvc.perform(authenticated(post(path)).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value("INVALID_CHAT_REQUEST"))
                .andExpect(jsonPath("$.message").isNotEmpty())
                .andExpect(jsonPath("$.runId").doesNotExist())
                .andExpect(jsonPath("$.resetAt").doesNotExist())
                .andExpect(request().asyncNotStarted());
        verifyNoInteractions(service);
    }

    static Stream<Arguments> invalidRequests() {
        List<String> bodies = List.of("{}", "{\"question\":null}", "{\"question\":\"\"}",
                "{\"question\":\"   \"}", "{\"question\":\"\u2003\"}",
                "{\"question\":\"" + "가".repeat(4001) + "\"}", "{\"question\":");
        return Stream.of("/api/v1/chat", "/api/v1/chat/stream")
                .flatMap(path -> bodies.stream().map(body -> Arguments.of(path, body)));
    }

    @Test
    @DisplayName("일반 채팅도 질문을 정규화하고 기존 성공 응답과 인증 사용자 전달을 유지한다")
    void preservesLegacySuccessResponse() throws Exception {
        when(service.chat(7L, "질문")).thenReturn(new SemanticChatService.ChatResult("ANSWERED", "답변", List.of()));

        mvc.perform(authenticated(post("/api/v1/chat"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"question\":\"  질문  \"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ANSWERED"))
                .andExpect(jsonPath("$.answer").value("답변"))
                .andExpect(jsonPath("$.sources").isEmpty());
        verify(service).chat(7L, "질문");
    }

    @ParameterizedTest
    @CsvSource({"SESSION_BUSY,409", "REQUEST_ID_CONFLICT,409", "SESSION_NOT_FOUND,404",
            "RUN_TOKEN_LIMIT_EXCEEDED,422", "RUN_TIMEOUT,504", "MODEL_REQUEST_FAILED,502"})
    @DisplayName("채팅 업무 오류가 공통 catch-all의 500으로 바뀌지 않는다")
    void preservesBusinessErrorStatus(ChatErrorCode code, int expectedStatus) throws Exception {
        when(service.chat(7L, "질문")).thenThrow(new ChatException(code));

        mvc.perform(authenticated(post("/api/v1/chat"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"question\":\"질문\"}"))
                .andExpect(status().is(expectedStatus))
                .andExpect(jsonPath("$.code").value(code.name()))
                .andExpect(jsonPath("$.runId").doesNotExist())
                .andExpect(jsonPath("$.resetAt").doesNotExist());
    }

    @Test
    @DisplayName("한도 초과 오류 규격은 429와 UTC 초기화 시각을 전달한다")
    void includesResetTimeForQuotaErrorContract() throws Exception {
        when(service.chat(7L, "질문")).thenThrow(new ChatException(ChatErrorCode.DAILY_QUOTA_EXCEEDED,
                Instant.parse("2026-10-09T15:00:00Z")));

        mvc.perform(authenticated(post("/api/v1/chat"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"question\":\"질문\"}"))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.code").value("DAILY_QUOTA_EXCEEDED"))
                .andExpect(jsonPath("$.resetAt").value("2026-10-09T15:00:00Z"));
    }

    @Test
    @DisplayName("스트리밍은 정규화한 질문으로 검색하고 기존 문맥 없음 이벤트를 유지한다")
    void preservesNoContextStream() throws Exception {
        when(service.prepare(7L, "질문")).thenReturn(new SemanticChatService.ChatPreparation("질문", "", List.of()));

        String stream = streamResponse("{\"question\":\"  질문  \"}");

        assertThat(stream).contains("event:start", "event:delta", "event:done", "NO_CONTEXT")
                .doesNotContain("event:error");
        verify(service).prepare(7L, "질문");
    }

    @ParameterizedTest
    @CsvSource({"PREPARING,SEARCH_FAILED", "GENERATING,MODEL_REQUEST_FAILED"})
    @DisplayName("스트리밍 실패는 오류 코드와 단계를 전달하고 내부 예외 원문을 노출하지 않는다")
    void emitsStructuredStreamFailures(String stage, String expectedCode) throws Exception {
        IllegalStateException failure = new IllegalStateException("private-provider-detail");
        if ("PREPARING".equals(stage)) {
            when(service.prepare(7L, "질문")).thenThrow(failure);
        } else {
            SemanticChatService.Source source = new SemanticChatService.Source(
                    1, 11L, "DOCUMENT", 21L, "note", "문서", 0.8, "내용");
            SemanticChatService.ChatPreparation preparation =
                    new SemanticChatService.ChatPreparation("질문", "내용", List.of(source));
            when(service.prepare(7L, "질문")).thenReturn(preparation);
            doThrow(failure).when(service).streamAnswer(eq(preparation), any());
        }

        String stream = streamResponse("{\"question\":\"질문\"}");

        assertThat(stream).contains("event:error").doesNotContain("private-provider-detail", "event:done");
        String errorData = stream.lines().filter(line -> line.startsWith("data:") && line.contains("\"code\""))
                .findFirst().orElseThrow();
        JsonNode error = mapper.readTree(errorData.substring("data:".length()));
        assertThat(error.path("code").asText()).isEqualTo(expectedCode);
        assertThat(error.path("stage").asText()).isEqualTo(stage);
        assertThat(error.path("message").asText()).isNotBlank();
    }

    private String streamResponse(String body) throws Exception {
        MvcResult result = mvc.perform(authenticated(post("/api/v1/chat/stream"))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(request().asyncStarted()).andReturn();
        result.getAsyncResult(5000);
        return mvc.perform(asyncDispatch(result))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_EVENT_STREAM))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
    }

    private MockHttpServletRequestBuilder authenticated(MockHttpServletRequestBuilder request) {
        return request.requestAttr(CommonConstant.AUTHORIZED_MEMBER_ID, 7L);
    }
}
