package com.example.search.sync;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** Message value; a scheduled document batch may replace it until the relay claims the row. */
public record SearchSyncEvent(
        UUID eventId,
        int schemaVersion,
        Instant occurredAt,
        SearchSourceType sourceType,
        Long sourceId,
        WorkerMessageType messageType,
        Long contentRevision
) {
    public SearchSyncEvent {
        Objects.requireNonNull(eventId, "eventId");
        Objects.requireNonNull(occurredAt, "occurredAt");
        Objects.requireNonNull(sourceType, "sourceType");
        Objects.requireNonNull(messageType, "messageType");
        if (schemaVersion != 2) {
            throw new IllegalArgumentException("Unsupported search sync schema version");
        }
        if (sourceId == null || sourceId <= 0) {
            throw new IllegalArgumentException("sourceId must be positive");
        }
        if (messageType != WorkerMessageType.SEARCH_DELETE
                && (contentRevision == null || contentRevision < 0)) {
            throw new IllegalArgumentException("Work requires a non-negative contentRevision");
        }
        if (messageType == WorkerMessageType.SEARCH_DELETE && contentRevision != null) {
            throw new IllegalArgumentException("DELETE must not include contentRevision");
        }
        if (messageType == WorkerMessageType.DOCUMENT_REFRESH && sourceType != SearchSourceType.DOCUMENT) {
            throw new IllegalArgumentException("DOCUMENT_REFRESH requires DOCUMENT");
        }
    }
}
