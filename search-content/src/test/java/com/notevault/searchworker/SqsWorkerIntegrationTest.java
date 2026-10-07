package com.notevault.searchworker;

import com.example.search.content.ContentSourceSnapshot;
import com.example.search.content.ContentSourceType;
import com.example.search.crdt.DocumentCrdtProcessor;
import com.example.search.crdt.DocumentCrdtTransactions;
import com.example.search.crdt.ProjectionProperties;
import com.example.search.crdt.YjsDocumentConverter;
import com.example.search.embedding.EmbeddingClient;
import com.example.search.indexing.ContentChunker;
import com.example.search.indexing.ContentIndexingService;
import com.example.search.indexing.ContentIndexingTransactions;
import com.example.search.indexing.SearchContentSync;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.notevault.searchworker.handlers.SearchDeleteHandler;
import com.notevault.searchworker.handlers.SearchRefreshHandler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentMatchers;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.*;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** 실제 수신 스레드 → JSON → dispatcher → CRDT → 인덱싱 → ACK 흐름을 연결한다.
 * SQS, embedding, Yjs 변환기와 DB 경계만 대체하므로 네트워크나 외부 서비스가 필요 없다. */
@Timeout(10)
@DisplayName("비동기 SQS worker 파이프라인 통합 테스트 - 외부 API 호출 없음")
class SqsWorkerIntegrationTest {
    private final SqsClient sqs = mock(SqsClient.class);
    private final EmbeddingClient embeddings = mock(EmbeddingClient.class);
    private final ContentIndexingTransactions indexingTransactions = mock(ContentIndexingTransactions.class);
    private final DocumentCrdtTransactions crdtTransactions = mock(DocumentCrdtTransactions.class);
    private final YjsDocumentConverter converter = mock(YjsDocumentConverter.class);
    private final ObjectMapper mapper = JsonMapper.builder().findAndAddModules().build();
    private final BlockingQueue<Message> deliveries = new LinkedBlockingQueue<>();
    private final BlockingQueue<DeleteMessageRequest> deleted = new LinkedBlockingQueue<>();
    private final BlockingQueue<ChangeMessageVisibilityRequest> retried = new LinkedBlockingQueue<>();
    private final ScheduledExecutorService heartbeat = Executors.newSingleThreadScheduledExecutor();
    private final ContentChunker chunker = new ContentChunker();
    private final ContentSourceSnapshot source = new ContentSourceSnapshot(ContentSourceType.DOCUMENT,
            42L, 10L, 1L, "NOTE", 42L, "제목", "본문", 7L, ContentChunker.sha256("본문"), LocalDateTime.of(2026, 10, 7, 0, 0));
    private SqsMessageConsumer worker;

    @BeforeEach
    void setUp() {
        var crdt = new DocumentCrdtProcessor(crdtTransactions, converter,
                new ProjectionProperties(1, Duration.ofSeconds(1), Duration.ofSeconds(1), 1024, 1024, 1024, 100));
        var indexing = new ContentIndexingService(indexingTransactions, embeddings, chunker);
        var sync = new SearchContentSync(indexingTransactions, indexing);
        var dispatcher = new WorkerMessageDispatcher(new SearchRefreshHandler(crdt, sync), new SearchDeleteHandler(sync));
        worker = new SqsMessageConsumer(sqs, new WorkerSqsProperties("https://sqs.invalid/test", "us-east-2", 1),
                mapper, dispatcher, heartbeat);
        when(sqs.receiveMessage(ArgumentMatchers.<Consumer<ReceiveMessageRequest.Builder>>any())).thenAnswer(invocation -> {
            Message message = deliveries.poll(50, TimeUnit.MILLISECONDS);
            return ReceiveMessageResponse.builder().messages(message == null ? List.of() : List.of(message)).build();
        });
        when(sqs.deleteMessage(ArgumentMatchers.<Consumer<DeleteMessageRequest.Builder>>any())).thenAnswer(invocation -> {
            Consumer<DeleteMessageRequest.Builder> configure = invocation.getArgument(0);
            var builder = DeleteMessageRequest.builder();
            configure.accept(builder);
            deleted.add(builder.build());
            return DeleteMessageResponse.builder().build();
        });
        when(sqs.changeMessageVisibility(ArgumentMatchers.<Consumer<ChangeMessageVisibilityRequest.Builder>>any())).thenAnswer(invocation -> {
            Consumer<ChangeMessageVisibilityRequest.Builder> configure = invocation.getArgument(0);
            var builder = ChangeMessageVisibilityRequest.builder();
            configure.accept(builder);
            retried.add(builder.build());
            return ChangeMessageVisibilityResponse.builder().build();
        });
    }

    @AfterEach
    void tearDown() throws InterruptedException {
        worker.stop();
        heartbeat.shutdownNow();
        assertThat(heartbeat.awaitTermination(2, TimeUnit.SECONDS)).isTrue();
    }

    private String refreshBody() throws Exception {
        return mapper.writeValueAsString(new SearchSyncMessage(UUID.randomUUID(), 2, Instant.parse("2026-10-07T00:00:00Z"),
                ContentSourceType.DOCUMENT, 42L, null, 7L, SearchSyncMessage.MessageType.DOCUMENT_REFRESH));
    }

    private void deliver(String body) {
        deliveries.add(Message.builder().messageId(UUID.randomUUID().toString()).receiptHandle("receipt-42")
                .body(body).attributesWithStrings(java.util.Map.of("ApproximateReceiveCount", "1")).build());
        worker.start();
    }

    private void prepareDocument() {
        var input = new DocumentCrdtTransactions.Input(42L, 6L, 7L, new byte[]{1}, List.of(new byte[]{2}));
        when(crdtTransactions.load(42L, 7L)).thenReturn(Optional.of(input));
        when(converter.project(input.state(), input.updates())).thenReturn(new YjsDocumentConverter.Projection("본문", new byte[]{3}));
        when(indexingTransactions.readSource(ContentSourceType.DOCUMENT, 42L)).thenReturn(Optional.of(source));
        when(embeddings.embeddingModel()).thenReturn("test-model");
        when(indexingTransactions.prepareTitle(source, "test-model"))
                .thenReturn(new ContentIndexingTransactions.TitleWork(source, "test-model", 1));
        when(indexingTransactions.prepare(source, chunker.chunk("본문"), "test-model"))
                .thenReturn(new ContentIndexingTransactions.EmbeddingWork(source, "test-model",
                        List.of(new ContentIndexingTransactions.EmbeddingTarget(5L, "본문", ContentChunker.sha256("본문"), 1))));
        when(embeddings.embed(anyList())).thenAnswer(invocation -> {
            List<String> texts = invocation.getArgument(0);
            return texts.stream().map(text -> new float[EmbeddingClient.DIMENSIONS]).toList();
        });
    }

    private void awaitAcknowledgement() throws InterruptedException {
        var request = deleted.poll(3, TimeUnit.SECONDS);
        assertThat(request).as("worker가 완료 메시지를 삭제해야 한다").isNotNull();
        worker.stop();
        assertThat(request.receiptHandle()).isEqualTo("receipt-42");
        assertThat(request.queueUrl()).isEqualTo("https://sqs.invalid/test");
        assertThat(retried).isEmpty();
        assertThat(worker.isRunning()).isFalse();
    }

    private void awaitRetry() throws InterruptedException {
        var request = retried.poll(3, TimeUnit.SECONDS);
        assertThat(request).as("worker가 실패 메시지를 재시도해야 한다").isNotNull();
        worker.stop();
        assertThat(request.receiptHandle()).isEqualTo("receipt-42");
        assertThat(request.visibilityTimeout()).isEqualTo(30);
        assertThat(deleted).isEmpty();
    }

    @Test
    @DisplayName("문서를 복원하고 제목·본문 인덱싱을 모두 마친 뒤 SQS 메시지를 삭제한다")
    void refreshCompletesBeforeAcknowledgement() throws Exception {
        prepareDocument();
        deliver(refreshBody());
        awaitAcknowledgement();

        var order = inOrder(crdtTransactions, converter, indexingTransactions, embeddings, sqs);
        order.verify(crdtTransactions).load(42L, 7L);
        order.verify(converter).project(any(), anyList());
        order.verify(crdtTransactions).saveProjection(any(), any());
        order.verify(indexingTransactions).readSource(ContentSourceType.DOCUMENT, 42L);
        order.verify(embeddings).embed(List.of("제목"));
        order.verify(indexingTransactions).completeTitle(any(), any());
        order.verify(embeddings).embed(List.of("본문"));
        order.verify(indexingTransactions).complete(any(), anyList());
        order.verify(indexingTransactions).verifyComplete(source, chunker.chunk("본문"), "test-model");
        order.verify(sqs).deleteMessage(ArgumentMatchers.<Consumer<DeleteMessageRequest.Builder>>any());
    }

    @Test
    @DisplayName("embedding 실패는 상태를 기록하고 메시지를 삭제하지 않은 채 재시도한다")
    void embeddingFailureRetriesWithoutAcknowledgement() throws Exception {
        prepareDocument();
        when(embeddings.embed(List.of("본문"))).thenThrow(new IllegalStateException("test embedding failure"));
        deliver(refreshBody());
        awaitRetry();

        verify(indexingTransactions).fail(any(), eq("test embedding failure"));
        verify(indexingTransactions, never()).complete(any(), any());
        verify(indexingTransactions, never()).verifyComplete(any(), any(), any());
    }

    @Test
    @DisplayName("문서 복원이 실패하면 embedding을 호출하지 않고 재시도한다")
    void projectionFailureStopsIndexing() throws Exception {
        when(crdtTransactions.load(42L, 7L)).thenThrow(new IllegalStateException("revision gap"));
        deliver(refreshBody());
        awaitRetry();

        verifyNoInteractions(embeddings, converter, indexingTransactions);
    }

    @Test
    @DisplayName("삭제 이벤트는 CRDT 복원과 embedding 없이 검색 데이터를 삭제한다")
    void deleteSkipsEmbedding() throws Exception {
        String body = mapper.writeValueAsString(new SearchSyncMessage(UUID.randomUUID(), 2, Instant.now(),
                ContentSourceType.DOCUMENT, 42L, null, null, SearchSyncMessage.MessageType.SEARCH_DELETE));
        deliver(body);
        awaitAcknowledgement();

        verify(indexingTransactions).deleteIfMissing(ContentSourceType.DOCUMENT, 42L);
        verifyNoInteractions(crdtTransactions, converter, embeddings);
    }

    @Test
    @DisplayName("이미 복원·인덱싱된 중복 이벤트는 embedding 재호출 없이 완료한다")
    void duplicateDeliverySkipsEmbedding() throws Exception {
        when(crdtTransactions.load(42L, 7L)).thenReturn(Optional.empty());
        when(indexingTransactions.readSource(ContentSourceType.DOCUMENT, 42L)).thenReturn(Optional.of(source));
        when(embeddings.embeddingModel()).thenReturn("test-model");
        when(indexingTransactions.prepare(source, chunker.chunk("본문"), "test-model"))
                .thenReturn(new ContentIndexingTransactions.EmbeddingWork(source, "test-model", List.of()));
        deliver(refreshBody());
        awaitAcknowledgement();

        verify(embeddings, never()).embed(anyList());
        verifyNoInteractions(converter);
        verify(indexingTransactions).verifyComplete(source, chunker.chunk("본문"), "test-model");
    }

    @ParameterizedTest
    @ValueSource(strings = {"null", "{}", "not-json", "[]"})
    @DisplayName("잘못된 JSON 메시지는 외부 작업 없이 재시도하고 삭제하지 않는다")
    void malformedDeliveryIsNotAcknowledged(String body) throws Exception {
        deliver(body);
        awaitRetry();
        verifyNoInteractions(embeddings, converter, crdtTransactions, indexingTransactions);
    }

    @ParameterizedTest
    @ValueSource(strings = {"numeric-enum", "fractional-revision", "trailing-json", "unsupported-version"})
    @DisplayName("숫자 enum·소수 revision·추가 JSON·미지원 버전은 엄격하게 거부한다")
    void invalidContractIsNotAcknowledged(String kind) throws Exception {
        String body = refreshBody();
        body = switch (kind) {
            case "numeric-enum" -> body.replace("\"DOCUMENT_REFRESH\"", "0");
            case "fractional-revision" -> body.replace("\"contentRevision\":7", "\"contentRevision\":7.5");
            case "trailing-json" -> body + " {}";
            default -> body.replace("\"schemaVersion\":2", "\"schemaVersion\":3");
        };
        deliver(body);
        awaitRetry();
        verifyNoInteractions(embeddings, converter, crdtTransactions, indexingTransactions);
    }

    @Test
    @DisplayName("SQS 삭제 요청이 실패하면 처리된 메시지도 재시도 대상으로 남긴다")
    void failedAcknowledgementRetries() throws Exception {
        prepareDocument();
        doThrow(new IllegalStateException("delete unavailable")).when(sqs)
                .deleteMessage(ArgumentMatchers.<Consumer<DeleteMessageRequest.Builder>>any());
        deliver(refreshBody());
        awaitRetry();
        verify(indexingTransactions).verifyComplete(source, chunker.chunk("본문"), "test-model");
    }

    @Test
    @DisplayName("잘못된 메시지 다음에 도착한 정상 메시지도 같은 worker가 계속 처리한다")
    void failureDoesNotStopReceiveLoop() throws Exception {
        prepareDocument();
        deliveries.add(Message.builder().messageId("bad").receiptHandle("bad-receipt").body("null").build());
        deliver(refreshBody());

        assertThat(retried.poll(3, TimeUnit.SECONDS)).isNotNull();
        assertThat(deleted.poll(3, TimeUnit.SECONDS)).isNotNull();
        worker.stop();
        verify(sqs, times(1)).deleteMessage(ArgumentMatchers.<Consumer<DeleteMessageRequest.Builder>>any());
        verify(indexingTransactions).verifyComplete(source, chunker.chunk("본문"), "test-model");
    }
}
