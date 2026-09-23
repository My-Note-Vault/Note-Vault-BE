package com.example.search.sync;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** Immutable message persisted in the outbox and later forwarded unchanged to SQS. */
public record SearchSyncEvent(
        UUID eventId,
        int schemaVersion,
        Instant occurredAt,
        SearchSourceType sourceType,
        Long sourceId,
        SearchSyncOperation operation,
        Long contentRevision
) {
    public SearchSyncEvent {
        Objects.requireNonNull(eventId, "eventId");
        Objects.requireNonNull(occurredAt, "occurredAt");
        Objects.requireNonNull(sourceType, "sourceType");
        Objects.requireNonNull(operation, "operation");
        if (schemaVersion != 1) {
            throw new IllegalArgumentException("Unsupported search sync schema version");
        }
        if (sourceId == null || sourceId <= 0) {
            throw new IllegalArgumentException("sourceId must be positive");
        }
        if (operation == SearchSyncOperation.REFRESH
                && (contentRevision == null || contentRevision < 0)) {
            throw new IllegalArgumentException("REFRESH requires a non-negative contentRevision");
        }
        if (operation == SearchSyncOperation.DELETE && contentRevision != null) {
            throw new IllegalArgumentException("DELETE must not include contentRevision");
        }
    }
}
