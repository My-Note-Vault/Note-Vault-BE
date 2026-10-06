package com.example.search.sync;

import com.example.search.sync.outbox.SearchSyncOutbox;
import com.example.search.sync.outbox.SearchSyncOutboxRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.Duration;
import java.util.UUID;

/** Records work in the caller's transaction; does not call SQS or the worker. */
@Service
@Transactional(propagation = Propagation.MANDATORY)
public class SearchSyncRecorder {
    private final SearchSyncOutboxRepository outbox;
    private final ObjectMapper eventMapper;

    public SearchSyncRecorder(SearchSyncOutboxRepository outbox, ObjectMapper mapper) {
        this.outbox = outbox;
        this.eventMapper = mapper.copy()
                .disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    /** documentId is the document PK, including for workspace home documents. */
    public UUID refreshDocument(Long documentId, Long revision) {
        return record(SearchSourceType.DOCUMENT, documentId, WorkerMessageType.DOCUMENT_REFRESH, revision);
    }

    public UUID deleteDocument(Long documentId) {
        return record(SearchSourceType.DOCUMENT, documentId, WorkerMessageType.SEARCH_DELETE, null);
    }

    public UUID refreshDailyNote(Long dailyNoteId, Long revision) {
        return record(SearchSourceType.DAILY_NOTE, dailyNoteId, WorkerMessageType.SEARCH_REFRESH, revision);
    }

    public UUID deleteDailyNote(Long dailyNoteId) {
        return record(SearchSourceType.DAILY_NOTE, dailyNoteId, WorkerMessageType.SEARCH_DELETE, null);
    }

    /** Caller must serialize document edits with the document row lock. */
    public DocumentBatch batchDocumentRefresh(Long documentId, long revision, UUID pendingId,
            long baseRevision, int minUpdates, Duration maxWait) {
        if (revision < 1 || minUpdates < 1 || maxWait == null || maxWait.toMillis() < 1) {
            throw new IllegalArgumentException("Invalid document refresh batch limits");
        }
        SearchSyncOutbox pending = pendingId == null ? null : outbox.findForUpdate(pendingId).orElse(null);
        // Read the DB clock after obtaining the lock, including any time spent waiting for it.
        Instant now = Instant.ofEpochMilli(outbox.currentTimeMillis());
        if (pending != null && pending.getSourceType() == SearchSourceType.DOCUMENT
                && documentId.equals(pending.getSourceId()) && baseRevision >= 0 && baseRevision < revision) {
            SearchSyncEvent event = new SearchSyncEvent(pending.getEventId(), 2, pending.getCreatedAt(),
                    SearchSourceType.DOCUMENT, documentId, WorkerMessageType.DOCUMENT_REFRESH, revision);
            if (pending.coalesce(serialize(event), revision - baseRevision >= minUpdates, now)) {
                return new DocumentBatch(pending.getEventId(), baseRevision);
            }
        }
        SearchSyncEvent event = new SearchSyncEvent(UUID.randomUUID(), 2, now,
                SearchSourceType.DOCUMENT, documentId, WorkerMessageType.DOCUMENT_REFRESH, revision);
        outbox.save(SearchSyncOutbox.scheduled(event, serialize(event),
                minUpdates == 1 ? now : now.plus(maxWait)));
        return new DocumentBatch(event.eventId(), revision - 1);
    }

    private UUID record(SearchSourceType type, Long id, WorkerMessageType messageType, Long revision) {
        SearchSyncEvent event = new SearchSyncEvent(
                UUID.randomUUID(), 2, Instant.now(), type, id, messageType, revision);
        outbox.save(SearchSyncOutbox.pending(event, serialize(event)));
        return event.eventId();
    }

    private String serialize(SearchSyncEvent event) {
        try {
            return eventMapper.writeValueAsString(event);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to serialize worker event", exception);
        }
    }

    public record DocumentBatch(UUID eventId, long baseRevision) { }
}
