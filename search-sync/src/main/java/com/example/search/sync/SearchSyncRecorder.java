package com.example.search.sync;

import com.example.search.sync.outbox.SearchSyncOutbox;
import com.example.search.sync.outbox.SearchSyncOutboxRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
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
        return record(SearchSourceType.DOCUMENT, documentId, SearchSyncOperation.REFRESH, revision);
    }

    public UUID deleteDocument(Long documentId) {
        return record(SearchSourceType.DOCUMENT, documentId, SearchSyncOperation.DELETE, null);
    }

    public UUID refreshDailyNote(Long dailyNoteId, Long revision) {
        return record(SearchSourceType.DAILY_NOTE, dailyNoteId, SearchSyncOperation.REFRESH, revision);
    }

    public UUID deleteDailyNote(Long dailyNoteId) {
        return record(SearchSourceType.DAILY_NOTE, dailyNoteId, SearchSyncOperation.DELETE, null);
    }

    private UUID record(SearchSourceType type, Long id, SearchSyncOperation operation, Long revision) {
        SearchSyncEvent event = new SearchSyncEvent(
                UUID.randomUUID(), 1, Instant.now(), type, id, operation, revision);
        try {
            outbox.save(SearchSyncOutbox.pending(event, eventMapper.writeValueAsString(event)));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to serialize search sync event", exception);
        }
        return event.eventId();
    }
}
