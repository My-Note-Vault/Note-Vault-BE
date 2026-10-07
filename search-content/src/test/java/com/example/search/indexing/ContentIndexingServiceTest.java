package com.example.search.indexing;

import com.example.search.content.ContentSourceSnapshot;
import com.example.search.content.ContentSourceType;
import com.example.search.embedding.EmbeddingClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("검색 인덱싱 단위 테스트 - EmbeddingClient mock 사용")
class ContentIndexingServiceTest {
    @Mock ContentIndexingTransactions transactions;
    @Mock EmbeddingClient embeddings;
    private final ContentChunker chunker = new ContentChunker();
    private final ContentSourceSnapshot source = new ContentSourceSnapshot(ContentSourceType.DOCUMENT,
            1L, 2L, 3L, "NOTE", 1L, "제목", "본문", 4L, ContentChunker.sha256("본문"), LocalDateTime.of(2026, 10, 7, 0, 0));
    private ContentIndexingService service;

    @BeforeEach
    void setUp() {
        service = new ContentIndexingService(transactions, embeddings, chunker);
        when(embeddings.embeddingModel()).thenReturn("test-model");
    }

    private ContentIndexingTransactions.EmbeddingWork bodyWork() {
        var target = new ContentIndexingTransactions.EmbeddingTarget(10L, "본문", ContentChunker.sha256("본문"), 1);
        var work = new ContentIndexingTransactions.EmbeddingWork(source, "test-model", List.of(target));
        when(transactions.prepare(source, chunker.chunk(source.content()), "test-model")).thenReturn(work);
        return work;
    }

    @Test
    @DisplayName("제목과 본문을 임베딩하고 모두 저장된 뒤 완료 여부를 확인한다")
    void indexesTitleAndBody() {
        var title = new ContentIndexingTransactions.TitleWork(source, "test-model", 1);
        when(transactions.prepareTitle(source, "test-model")).thenReturn(title);
        var body = bodyWork();
        float[] titleVector = new float[EmbeddingClient.DIMENSIONS];
        float[] bodyVector = new float[EmbeddingClient.DIMENSIONS];
        when(embeddings.embed(List.of("제목"))).thenReturn(List.of(titleVector));
        when(embeddings.embed(List.of("본문"))).thenReturn(List.of(bodyVector));

        service.index(source);

        var order = inOrder(transactions, embeddings);
        order.verify(transactions).prepareTitle(source, "test-model");
        order.verify(embeddings).embed(List.of("제목"));
        order.verify(transactions).completeTitle(title, titleVector);
        order.verify(transactions).prepare(source, chunker.chunk("본문"), "test-model");
        order.verify(embeddings).embed(List.of("본문"));
        order.verify(transactions).complete(body, List.of(bodyVector));
        order.verify(transactions).verifyComplete(source, chunker.chunk("본문"), "test-model");
    }

    @Test
    @DisplayName("이미 처리된 제목과 본문은 embedding을 다시 요청하지 않는다")
    void skipsReadyContent() {
        when(transactions.prepare(source, chunker.chunk("본문"), "test-model"))
                .thenReturn(new ContentIndexingTransactions.EmbeddingWork(source, "test-model", List.of()));

        service.index(source);

        verify(embeddings, never()).embed(anyList());
        verify(transactions).verifyComplete(source, chunker.chunk("본문"), "test-model");
    }

    static Stream<List<float[]>> invalidVectors() {
        return Stream.of(null, List.<float[]>of(), Arrays.asList((float[]) null),
                List.of(new float[2]), List.of(new float[EmbeddingClient.DIMENSIONS], new float[EmbeddingClient.DIMENSIONS]));
    }

    @ParameterizedTest
    @MethodSource("invalidVectors")
    @DisplayName("본문 임베딩 응답의 개수·차원·null이 잘못되면 실패를 기록한다")
    void rejectsInvalidBodyVectors(List<float[]> vectors) {
        var work = bodyWork();
        when(embeddings.embed(List.of("본문"))).thenReturn(vectors);

        assertThatThrownBy(() -> service.index(source)).isInstanceOf(IllegalStateException.class);

        verify(transactions).fail(eq(work), anyString());
        verify(transactions, never()).complete(any(), any());
        verify(transactions, never()).verifyComplete(any(), any(), any());
    }

    @ParameterizedTest
    @MethodSource("invalidVectors")
    @DisplayName("제목 임베딩 응답이 올바른 단일 벡터가 아니면 저장하지 않는다")
    void rejectsInvalidTitleVectors(List<float[]> vectors) {
        var work = new ContentIndexingTransactions.TitleWork(source, "test-model", 1);
        when(transactions.prepareTitle(source, "test-model")).thenReturn(work);
        when(embeddings.embed(List.of("제목"))).thenReturn(vectors);

        assertThatThrownBy(() -> service.indexTitle(source)).isInstanceOf(IllegalStateException.class);

        verify(transactions).failTitle(eq(work), anyString());
        verify(transactions, never()).completeTitle(any(), any());
    }

    @Test
    @DisplayName("제목 임베딩이 실패해도 본문을 처리하고 원래 예외를 전달한다")
    void titleFailureDoesNotSkipBody() {
        var title = new ContentIndexingTransactions.TitleWork(source, "test-model", 1);
        when(transactions.prepareTitle(source, "test-model")).thenReturn(title);
        var body = bodyWork();
        var failure = new IllegalStateException("embedding unavailable");
        var vectors = List.of(new float[EmbeddingClient.DIMENSIONS]);
        when(embeddings.embed(List.of("제목"))).thenThrow(failure);
        when(embeddings.embed(List.of("본문"))).thenReturn(vectors);

        assertThatThrownBy(() -> service.index(source)).isSameAs(failure);

        verify(transactions).failTitle(title, "embedding unavailable");
        verify(transactions).complete(body, vectors);
        verify(transactions, never()).verifyComplete(any(), any(), any());
    }

    @Test
    @DisplayName("실패 상태 기록도 실패하면 최초 embedding 예외를 보존한다")
    void preservesOriginalFailure() {
        var body = bodyWork();
        var original = new IllegalStateException("embedding unavailable");
        var recording = new IllegalStateException("database unavailable");
        when(embeddings.embed(List.of("본문"))).thenThrow(original);
        doThrow(recording).when(transactions).fail(body, original.getMessage());

        assertThatThrownBy(() -> service.index(source)).isSameAs(original)
                .satisfies(error -> assertThat(error.getSuppressed()).containsExactly(recording));
    }
}
