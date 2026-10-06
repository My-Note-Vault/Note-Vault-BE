package com.example.search.sync.outbox;

import com.example.search.sync.SearchSourceType;
import com.example.search.sync.SearchSyncEvent;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.domain.Persistable;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Entity
@Table(name = "search_sync_outbox")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SearchSyncOutbox implements Persistable<UUID> {
    private static final Duration LEASE_DURATION = Duration.ofMinutes(1);
    private static final long INITIAL_RETRY_MILLIS = 1000;
    private static final long MAX_RETRY_MILLIS = Duration.ofMinutes(5).toMillis();

    @Id
    @Column(name = "event_id", nullable = false, updatable = false)
    private UUID eventId;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false, updatable = false, length = 30)
    private SearchSourceType sourceType;

    @Column(name = "source_id", nullable = false, updatable = false)
    private Long sourceId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SearchSyncOutboxStatus status;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;

    @Column(name = "next_attempt_at", nullable = false, columnDefinition = "timestamptz")
    private Instant nextAttemptAt;

    @Column(name = "lease_token")
    private UUID leaseToken;

    @Column(name = "lease_expires_at", columnDefinition = "timestamptz")
    private Instant leaseExpiresAt;

    @Column(name = "last_error", columnDefinition = "text")
    private String lastError;

    @Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "timestamptz")
    private Instant createdAt;

    @Column(name = "published_at", columnDefinition = "timestamptz")
    private Instant publishedAt;

    @Transient
    private boolean newEntity = true;

    public static SearchSyncOutbox pending(SearchSyncEvent event, String payload) {
        return scheduled(event, payload, event.occurredAt());
    }

    public static SearchSyncOutbox scheduled(SearchSyncEvent event, String payload, Instant publishAt) {
        Objects.requireNonNull(event, "event");
        Objects.requireNonNull(publishAt, "publishAt");
        if (payload == null || payload.isBlank()) {
            throw new IllegalArgumentException("payload must not be blank");
        }
        SearchSyncOutbox row = new SearchSyncOutbox();
        row.eventId = event.eventId();
        row.sourceType = event.sourceType();
        row.sourceId = event.sourceId();
        row.payload = payload;
        row.status = SearchSyncOutboxStatus.PENDING;
        row.createdAt = event.occurredAt();
        row.nextAttemptAt = publishAt;
        return row;
    }

    /** Only unclaimed, not-yet-due batches may change; retries retain their original payload. */
    public boolean coalesce(String payload, boolean publishNow, Instant now) {
        if (status != SearchSyncOutboxStatus.PENDING || attemptCount != 0 || !nextAttemptAt.isAfter(now)) {
            return false;
        }
        if (payload == null || payload.isBlank()) throw new IllegalArgumentException("payload must not be blank");
        this.payload = payload;
        if (publishNow) nextAttemptAt = now;
        // Appending updates never postpones the original one-minute deadline.
        return true;
    }

    public OutboxMessage claim(Instant now) {
        if (status != SearchSyncOutboxStatus.PENDING) {
            throw new IllegalStateException("Only pending events can be claimed");
        }
        status = SearchSyncOutboxStatus.PROCESSING;
        leaseToken = UUID.randomUUID();
        leaseExpiresAt = now.plus(LEASE_DURATION);
        attemptCount++;
        return new OutboxMessage(eventId, payload, leaseToken);
    }

    public boolean markPublished(UUID token, Instant now) {
        if (!ownsLease(token, now)) return false;
        status = SearchSyncOutboxStatus.PUBLISHED;
        publishedAt = now;
        lastError = null;
        clearLease();
        return true;
    }

    public boolean markFailed(UUID token, Instant now, String error) {
        if (!ownsLease(token, now)) return false;
        retryLater(now, error);
        return true;
    }

    public void recoverExpiredLease(Instant now) {
        if (status != SearchSyncOutboxStatus.PROCESSING || leaseExpiresAt.isAfter(now)) {
            throw new IllegalStateException("Only expired processing events can be recovered");
        }
        retryLater(now, "Relay lease expired before publication was recorded");
    }

    private boolean ownsLease(UUID token, Instant now) {
        return status == SearchSyncOutboxStatus.PROCESSING
                && Objects.equals(leaseToken, token) && leaseExpiresAt.isAfter(now);
    }

    private void retryLater(Instant now, String error) {
        status = SearchSyncOutboxStatus.PENDING;
        nextAttemptAt = now.plusMillis(retryDelayMillis());
        String reason = Objects.requireNonNullElse(error, "SQS publication failed");
        lastError = reason.substring(0, Math.min(reason.length(), 2048));
        clearLease();
    }

    private long retryDelayMillis() {
        // Exponential backoff with jitter: starts at 1s, capped at 5 minutes.
        long ceiling = INITIAL_RETRY_MILLIS;
        for (int attempt = 1; attempt < attemptCount && ceiling < MAX_RETRY_MILLIS; attempt++) {
            ceiling = Math.min(MAX_RETRY_MILLIS, ceiling * 2);
        }
        return ThreadLocalRandom.current().nextLong(Math.max(INITIAL_RETRY_MILLIS, ceiling / 2), ceiling + 1);
    }

    private void clearLease() {
        leaseToken = null;
        leaseExpiresAt = null;
    }

    @Override
    public UUID getId() {
        return eventId;
    }

    @Override
    public boolean isNew() {
        return newEntity;
    }

    @PostPersist
    @PostLoad
    private void markPersisted() {
        newEntity = false;
    }
}
