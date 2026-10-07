package com.example.search.sync.outbox;

import com.example.search.sync.SearchSourceType;
import com.example.search.sync.SearchSyncEvent;
import com.example.search.sync.WorkerMessageType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

@DisplayName("검색 Outbox 상태와 발행 소유권 단위 테스트")
class SearchSyncOutboxTest {
    private static final Instant NOW = Instant.parse("2026-10-07T00:00:00Z");

    private SearchSyncOutbox scheduled() {
        var event = new SearchSyncEvent(UUID.randomUUID(), 2, NOW,
                SearchSourceType.DOCUMENT, 1L, WorkerMessageType.DOCUMENT_REFRESH, 1L);
        return SearchSyncOutbox.scheduled(event, "{\"contentRevision\":1}", NOW.plusSeconds(60));
    }

    @Test
    @DisplayName("대기 이벤트를 선점하면 처리 횟수와 1분짜리 소유권을 발급한다")
    void claimCreatesLease() {
        var row = scheduled();
        var delivery = row.claim(NOW);

        assertThat(row.getStatus()).isEqualTo(SearchSyncOutboxStatus.PROCESSING);
        assertThat(row.getAttemptCount()).isEqualTo(1);
        assertThat(row.getLeaseExpiresAt()).isEqualTo(NOW.plusSeconds(60));
        assertThat(delivery.eventId()).isEqualTo(row.getId());
        assertThat(delivery.payload()).isEqualTo(row.getPayload());
        assertThat(delivery.leaseToken()).isNotNull().isEqualTo(row.getLeaseToken());
        assertThatThrownBy(() -> row.claim(NOW)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("유효한 소유자만 발행 완료로 변경하고 소유권을 해제한다")
    void onlyOwnerCanPublish() {
        var row = scheduled();
        var delivery = row.claim(NOW);

        assertThat(row.markPublished(UUID.randomUUID(), NOW.plusSeconds(1))).isFalse();
        assertThat(row.getStatus()).isEqualTo(SearchSyncOutboxStatus.PROCESSING);
        assertThat(row.markPublished(delivery.leaseToken(), NOW.plusSeconds(1))).isTrue();
        assertThat(row.getStatus()).isEqualTo(SearchSyncOutboxStatus.PUBLISHED);
        assertThat(row.getPublishedAt()).isEqualTo(NOW.plusSeconds(1));
        assertThat(row.getLeaseToken()).isNull();
        assertThat(row.getLeaseExpiresAt()).isNull();
        assertThat(row.markFailed(delivery.leaseToken(), NOW.plusSeconds(2), "late")).isFalse();
    }

    @Test
    @DisplayName("소유권 만료 시각부터 이전 작업자는 성공과 실패를 기록할 수 없다")
    void expiredOwnerCannotChangeState() {
        var row = scheduled();
        var delivery = row.claim(NOW);

        assertThat(row.markPublished(delivery.leaseToken(), NOW.plusSeconds(60))).isFalse();
        assertThat(row.markFailed(delivery.leaseToken(), NOW.plusSeconds(60), "late")).isFalse();
        row.recoverExpiredLease(NOW.plusSeconds(60));
        assertThat(row.getStatus()).isEqualTo(SearchSyncOutboxStatus.PENDING);
        var next = row.claim(NOW.plusSeconds(61));
        assertThat(next.leaseToken()).isNotEqualTo(delivery.leaseToken());
        assertThat(row.markPublished(delivery.leaseToken(), NOW.plusSeconds(62))).isFalse();
        assertThat(row.getAttemptCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("실패는 재시도 시각을 예약하고 오류를 2048자로 제한한다")
    void failureSchedulesRetry() {
        var row = scheduled();
        var delivery = row.claim(NOW);

        assertThat(row.markFailed(delivery.leaseToken(), NOW, "x".repeat(3000))).isTrue();
        assertThat(row.getStatus()).isEqualTo(SearchSyncOutboxStatus.PENDING);
        assertThat(row.getNextAttemptAt()).isEqualTo(NOW.plusSeconds(1));
        assertThat(row.getLastError()).hasSize(2048);
        assertThat(row.getLeaseToken()).isNull();
        assertThat(row.coalesce("{}", true, NOW)).isFalse();
    }

    @Test
    @DisplayName("반복 실패의 지수 백오프는 최대 5분을 넘지 않는다")
    void retryBackoffIsBounded() {
        var row = scheduled();
        Instant now = NOW;
        for (int attempt = 1; attempt <= 15; attempt++) {
            var delivery = row.claim(now);
            row.markFailed(delivery.leaseToken(), now, null);
            long ceiling = Math.min(300_000L, 1000L << Math.min(attempt - 1, 20));
            assertThat(row.getNextAttemptAt()).isBetween(
                    now.plusMillis(Math.max(1000, ceiling / 2)), now.plusMillis(ceiling));
            now = row.getNextAttemptAt();
        }
        assertThat(row.getLastError()).isEqualTo("SQS publication failed");
    }

    @Test
    @DisplayName("예약된 문서 변경을 병합해도 최초 발행 기한은 늦추지 않는다")
    void coalescePreservesDeadline() {
        var row = scheduled();
        assertThat(row.coalesce("{\"contentRevision\":2}", false, NOW.plusSeconds(10))).isTrue();
        assertThat(row.getPayload()).isEqualTo("{\"contentRevision\":2}");
        assertThat(row.getNextAttemptAt()).isEqualTo(NOW.plusSeconds(60));
        assertThat(row.coalesce("{\"contentRevision\":100}", true, NOW.plusSeconds(20))).isTrue();
        assertThat(row.getNextAttemptAt()).isEqualTo(NOW.plusSeconds(20));
        assertThat(row.coalesce("{}", false, NOW.plusSeconds(20))).isFalse();
    }

    @Test
    @DisplayName("만료되지 않은 처리 소유권은 복구하지 않는다")
    void cannotRecoverActiveLease() {
        var row = scheduled();
        row.claim(NOW);
        assertThatThrownBy(() -> row.recoverExpiredLease(NOW.plusSeconds(59)))
                .isInstanceOf(IllegalStateException.class);
        assertThat(row.getStatus()).isEqualTo(SearchSyncOutboxStatus.PROCESSING);
    }
}
