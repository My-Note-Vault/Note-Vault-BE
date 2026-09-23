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

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "search_sync_outbox")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SearchSyncOutbox implements Persistable<UUID> {
    @Id
    @Column(name = "event_id", nullable = false, updatable = false)
    private UUID eventId;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false, updatable = false, length = 30)
    private SearchSourceType sourceType;

    @Column(name = "source_id", nullable = false, updatable = false)
    private Long sourceId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, updatable = false, columnDefinition = "jsonb")
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
        Objects.requireNonNull(event, "event");
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
        row.nextAttemptAt = event.occurredAt();
        return row;
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
