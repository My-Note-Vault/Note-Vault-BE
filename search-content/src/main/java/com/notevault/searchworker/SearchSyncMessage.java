package com.notevault.searchworker;

import com.example.search.content.ContentSourceType;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** Document refresh is a complete pipeline. Older queued contracts remain readable. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record SearchSyncMessage(
        UUID eventId, int schemaVersion, Instant occurredAt, ContentSourceType sourceType,
        Long sourceId, Operation operation, Long contentRevision, MessageType messageType
) {
    public SearchSyncMessage {
        Objects.requireNonNull(eventId, "eventId");
        Objects.requireNonNull(occurredAt, "occurredAt");
        Objects.requireNonNull(sourceType, "sourceType");
        if (sourceId == null || sourceId <= 0) throw new IllegalArgumentException("sourceId must be positive");
        if (schemaVersion == 1) {
            if (messageType != null) throw new IllegalArgumentException("Version 1 uses operation");
            Objects.requireNonNull(operation, "operation");
            messageType = operation == Operation.REFRESH ? MessageType.SEARCH_REFRESH : MessageType.SEARCH_DELETE;
        } else if (schemaVersion == 2) {
            if (operation != null) throw new IllegalArgumentException("Version 2 uses messageType");
            Objects.requireNonNull(messageType, "messageType");
        } else {
            throw new IllegalArgumentException("Unsupported worker schema version");
        }
        if (messageType == MessageType.SEARCH_DELETE) {
            if (contentRevision != null) throw new IllegalArgumentException("DELETE must not include revision");
        } else if (contentRevision == null || contentRevision < 0) {
            throw new IllegalArgumentException("Work requires a non-negative revision");
        }
        if ((messageType == MessageType.CRDT_COMPACT || messageType == MessageType.DOCUMENT_REFRESH)
                && sourceType != ContentSourceType.DOCUMENT) {
            throw new IllegalArgumentException("Document work requires DOCUMENT");
        }
    }

    public enum Operation { REFRESH, DELETE }
    public enum MessageType { DOCUMENT_REFRESH, SEARCH_REFRESH, SEARCH_DELETE, CRDT_COMPACT }
}
