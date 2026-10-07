package com.example.search.sync;

import com.example.search.sync.outbox.SearchSyncOutbox;
import com.example.search.sync.outbox.SearchSyncOutboxRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("문서 검색 갱신 배치 기록 단위 테스트")
class SearchSyncRecorderTest {
    private static final Instant NOW = Instant.parse("2026-10-07T00:00:00Z");
    @Mock SearchSyncOutboxRepository outbox;
    private final ObjectMapper mapper = JsonMapper.builder().findAndAddModules().build();
    private SearchSyncRecorder recorder;

    @BeforeEach
    void setUp() {
        recorder = new SearchSyncRecorder(outbox, mapper);
    }

    private SearchSyncOutbox pending() throws Exception {
        var event = new SearchSyncEvent(UUID.randomUUID(), 2, NOW, SearchSourceType.DOCUMENT,
                42L, WorkerMessageType.DOCUMENT_REFRESH, 1L);
        return SearchSyncOutbox.scheduled(event, mapper.writeValueAsString(event), NOW.plusSeconds(60));
    }

    @Test
    @DisplayName("첫 변경은 DB 시각을 기준으로 발행 기한과 기준 revision을 기록한다")
    void schedulesFirstBatch() {
        when(outbox.currentTimeMillis()).thenReturn(NOW.toEpochMilli());

        var batch = recorder.batchDocumentRefresh(42L, 5L, null, 0L, 100, Duration.ofMinutes(1));

        var saved = ArgumentCaptor.forClass(SearchSyncOutbox.class);
        verify(outbox).save(saved.capture());
        assertThat(batch.eventId()).isEqualTo(saved.getValue().getEventId());
        assertThat(batch.baseRevision()).isEqualTo(4L);
        assertThat(saved.getValue().getCreatedAt()).isEqualTo(NOW);
        assertThat(saved.getValue().getNextAttemptAt()).isEqualTo(NOW.plusSeconds(60));
    }

    @Test
    @DisplayName("배치 임계치에 도달하면 기존 이벤트를 최신 revision으로 갱신하고 즉시 발행한다")
    void thresholdMakesExistingBatchPublishable() throws Exception {
        var pending = pending();
        when(outbox.findForUpdate(pending.getId())).thenReturn(Optional.of(pending));
        when(outbox.currentTimeMillis()).thenReturn(NOW.plusSeconds(10).toEpochMilli());

        var batch = recorder.batchDocumentRefresh(42L, 100L, pending.getId(), 0L, 100, Duration.ofMinutes(1));

        assertThat(batch.eventId()).isEqualTo(pending.getId());
        assertThat(batch.baseRevision()).isZero();
        assertThat(pending.getNextAttemptAt()).isEqualTo(NOW.plusSeconds(10));
        assertThat(mapper.readTree(pending.getPayload()).path("contentRevision").asLong()).isEqualTo(100);
        verify(outbox, never()).save(any());
        var order = inOrder(outbox);
        order.verify(outbox).findForUpdate(pending.getId());
        order.verify(outbox).currentTimeMillis();
    }

    @Test
    @DisplayName("임계치 전 변경 병합은 이벤트 ID와 최초 발행 기한을 유지한다")
    void coalescesWithoutDelayingDeadline() throws Exception {
        var pending = pending();
        when(outbox.findForUpdate(pending.getId())).thenReturn(Optional.of(pending));
        when(outbox.currentTimeMillis()).thenReturn(NOW.plusSeconds(10).toEpochMilli());

        var batch = recorder.batchDocumentRefresh(42L, 2L, pending.getId(), 0L, 100, Duration.ofMinutes(1));

        assertThat(batch.eventId()).isEqualTo(pending.getId());
        assertThat(pending.getNextAttemptAt()).isEqualTo(NOW.plusSeconds(60));
        assertThat(mapper.readTree(pending.getPayload()).path("contentRevision").asLong()).isEqualTo(2);
        verify(outbox, never()).save(any());
    }

    @Test
    @DisplayName("이미 선점된 이벤트의 내용은 변경하지 않고 새 배치를 생성한다")
    void claimedBatchIsImmutable() throws Exception {
        var pending = pending();
        String original = pending.getPayload();
        pending.claim(NOW);
        when(outbox.findForUpdate(pending.getId())).thenReturn(Optional.of(pending));
        when(outbox.currentTimeMillis()).thenReturn(NOW.plusSeconds(10).toEpochMilli());

        var batch = recorder.batchDocumentRefresh(42L, 2L, pending.getId(), 0L, 100, Duration.ofMinutes(1));

        assertThat(batch.eventId()).isNotEqualTo(pending.getId());
        assertThat(batch.baseRevision()).isEqualTo(1L);
        assertThat(pending.getPayload()).isEqualTo(original);
        verify(outbox).save(any());
    }

    @Test
    @DisplayName("잘못된 배치 제한은 저장소를 호출하기 전에 거부한다")
    void invalidLimitsDoNotTouchDatabase() {
        assertThatThrownBy(() -> recorder.batchDocumentRefresh(42L, 0, null, 0, 100, Duration.ofMinutes(1)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> recorder.batchDocumentRefresh(42L, 1, null, 0, 0, Duration.ofMinutes(1)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> recorder.batchDocumentRefresh(42L, 1, null, 0, 100, Duration.ZERO))
                .isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(outbox);
    }
}
