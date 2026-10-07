package com.example.search.sync.relay;

import com.example.search.sync.outbox.OutboxMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Outbox 비동기 relay 단위 테스트")
class OutboxRelayTest {
    @Mock OutboxTransactions transactions;
    @Mock SqsEventPublisher publisher;
    @Mock RelayMetrics metrics;
    private OutboxRelay relay;

    @BeforeEach
    void setUp() { relay = new OutboxRelay(transactions, publisher, metrics); }

    @Test
    @DisplayName("만료 소유권 복구 후 발행 결과에 따라 성공·재시도를 기록한다")
    void recordsPartialPublication() {
        var success = new OutboxMessage(UUID.randomUUID(), "{}", UUID.randomUUID());
        var failure = new OutboxMessage(UUID.randomUUID(), "{}", UUID.randomUUID());
        var failures = List.of(new OutboxMessage.Failure(failure, "throttled"));
        when(transactions.recoverExpired()).thenReturn(2);
        when(transactions.claimBatch()).thenReturn(List.of(success, failure)).thenReturn(List.of());
        when(publisher.publish(List.of(success, failure)))
                .thenReturn(new SqsEventPublisher.Publication(List.of(success), failures));
        when(transactions.markPublished(List.of(success))).thenReturn(1);
        when(transactions.markFailed(failures)).thenReturn(1);

        relay.poll();

        var order = inOrder(transactions, publisher);
        order.verify(transactions).recoverExpired();
        order.verify(transactions).claimBatch();
        order.verify(publisher).publish(List.of(success, failure));
        order.verify(transactions).markPublished(List.of(success));
        order.verify(transactions).markFailed(failures);
        verify(metrics).record("recovered", 2);
        verify(metrics).record("published", 1);
        verify(metrics).record("retried", 1);
        verify(metrics).record("lost_lease", 0);
    }

    @Test
    @DisplayName("한 번의 poll에서는 최대 다섯 배치만 처리한다")
    void capsBatchesPerPoll() {
        var message = new OutboxMessage(UUID.randomUUID(), "{}", UUID.randomUUID());
        when(transactions.claimBatch()).thenReturn(List.of(message));
        when(publisher.publish(anyList())).thenReturn(new SqsEventPublisher.Publication(List.of(message), List.of()));
        when(transactions.markPublished(anyList())).thenReturn(1);

        relay.poll();

        verify(transactions, times(5)).claimBatch();
        verify(publisher, times(5)).publish(List.of(message));
    }

    @Test
    @DisplayName("발행 상태 기록 예외는 전파하지 않고 다음 poll에서 다시 처리할 수 있다")
    void failureDoesNotStopFuturePolls() {
        when(transactions.recoverExpired()).thenThrow(new IllegalStateException("database unavailable")).thenReturn(0);

        assertThatCode(relay::poll).doesNotThrowAnyException();
        relay.poll();

        verify(metrics).error("poll");
        verify(transactions).claimBatch();
        verifyNoInteractions(publisher);
    }

    @Test
    @DisplayName("종료 신호 이후에는 새 배치를 선점하거나 정리 작업을 수행하지 않는다")
    void shutdownStopsScheduledWork() {
        relay.stopClaiming();
        relay.poll();
        relay.cleanup();
        relay.sampleBacklog();

        verifyNoInteractions(transactions, publisher, metrics);
    }
}
