package com.notevault.searchworker;

import com.example.search.content.ContentSourceType;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** Version 1 JSON contract. The worker does not depend on the API's outbox module. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record SearchSyncMessage(
        UUID eventId, int schemaVersion, Instant occurredAt, ContentSourceType sourceType,
        Long sourceId, Operation operation, Long contentRevision
) {
    public SearchSyncMessage {
        Objects.requireNonNull(eventId, "eventId");
        Objects.requireNonNull(occurredAt, "occurredAt");
        Objects.requireNonNull(sourceType, "sourceType");
        Objects.requireNonNull(operation, "operation");
        if (schemaVersion != 1) throw new IllegalArgumentException("Unsupported search sync schema version");
        if (sourceId == null || sourceId <= 0) throw new IllegalArgumentException("sourceId must be positive");
        if (operation == Operation.REFRESH && (contentRevision == null || contentRevision < 0)) {
            throw new IllegalArgumentException("REFRESH requires a non-negative contentRevision");
        }
        if (operation == Operation.DELETE && contentRevision != null) {
            throw new IllegalArgumentException("DELETE must not include contentRevision");
        }
    }

    public enum Operation { REFRESH, DELETE }
}
