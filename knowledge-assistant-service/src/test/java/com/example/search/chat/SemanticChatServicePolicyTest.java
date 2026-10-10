package com.example.search.chat;

import com.example.search.chat.api.ChatErrorCode;
import com.example.search.chat.api.ChatException;
import com.example.search.infrastructure.OpenAiSearchClient;
import com.example.search.retrieval.HybridSearch;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class SemanticChatServicePolicyTest {
    private final HybridSearch search = mock(HybridSearch.class);
    private final OpenAiSearchClient openAi = mock(OpenAiSearchClient.class);
    private final KeywordExtractor keywords = mock(KeywordExtractor.class);
    private final SemanticChatService service = new SemanticChatService(search, openAi, keywords);

    @ParameterizedTest
    @MethodSource("invalidQuestions")
    @DisplayName("잘못된 질문은 검색·키워드 추출·유료 API 호출 전에 거부한다")
    void rejectsInvalidQuestionsBeforeCallingDependencies(String question) {
        assertThatThrownBy(() -> service.prepare(7L, question))
                .isInstanceOfSatisfying(ChatException.class,
                        exception -> assertThat(exception.code()).isEqualTo(ChatErrorCode.INVALID_CHAT_REQUEST));

        verifyNoInteractions(search, openAi, keywords);
    }

    static Stream<String> invalidQuestions() {
        return Stream.of(null, "", " \t\n", "\u2003", "가".repeat(4001), "😀".repeat(2001),
                " " + "가".repeat(4000));
    }

    @ParameterizedTest
    @MethodSource("boundaryQuestions")
    @DisplayName("4,000 UTF-16 단위 질문은 한글과 이모지 모두 허용한다")
    void acceptsQuestionsAtTheLengthLimit(String question) {
        stubEmptySearch(question);

        SemanticChatService.ChatPreparation preparation = service.prepare(7L, question);

        assertThat(preparation.question()).isEqualTo(question);
        assertThat(preparation.hasContext()).isFalse();
        verify(openAi).embedQuestion(question);
    }

    static Stream<String> boundaryQuestions() {
        return Stream.of("가".repeat(4000), "😀".repeat(2000));
    }

    @Test
    @DisplayName("앞뒤 공백을 제거한 같은 질문을 임베딩·검색·준비 결과에 사용한다")
    void usesNormalizedQuestionThroughoutPreparation() {
        stubEmptySearch("회의 내용");

        SemanticChatService.ChatPreparation preparation = service.prepare(7L, "\u2003 회의 내용 \n");

        assertThat(preparation.question()).isEqualTo("회의 내용");
        assertThat(preparation.sources()).isEmpty();
        verify(keywords).extract("회의 내용");
        verify(openAi).embeddingModel();
        verify(openAi).embedQuestion("회의 내용");
        verify(search).search(7L, "회의 내용", List.of("키워드"), "embedding-model", "[1,2]", 6);
        verifyNoMoreInteractions(search, openAi, keywords);
    }

    private void stubEmptySearch(String question) {
        ReflectionTestUtils.setField(service, "topK", 6);
        when(keywords.extract(question)).thenReturn(List.of("키워드"));
        when(openAi.embeddingModel()).thenReturn("embedding-model");
        when(openAi.embedQuestion(question)).thenReturn("[1,2]");
        when(search.search(7L, question, List.of("키워드"), "embedding-model", "[1,2]", 6))
                .thenReturn(List.of());
    }
}
