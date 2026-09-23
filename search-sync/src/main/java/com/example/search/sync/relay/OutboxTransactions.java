package com.example.search.sync.relay;

import com.example.search.sync.outbox.OutboxMessage;
import com.example.search.sync.outbox.SearchSyncOutbox;
import com.example.search.sync.outbox.SearchSyncOutboxRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Transactional(propagation = Propagation.REQUIRES_NEW, timeout = 5)
public class OutboxTransactions {
    private final SearchSyncOutboxRepository outbox;

    public List<OutboxMessage> claimBatch() {
        List<SearchSyncOutbox> rows = outbox.findPublishableBatch();
        if (rows.isEmpty()) return List.of();
        Instant now = databaseTime();
        return rows.stream().map(row -> row.claim(now)).toList();
    }

    public int markPublished(List<OutboxMessage> messages) {
        if (messages.isEmpty()) return 0;
        var claims = messages.stream().collect(Collectors.toMap(OutboxMessage::eventId, Function.identity()));
        List<SearchSyncOutbox> rows = outbox.findAllForUpdate(claims.keySet());
        Instant now = databaseTime();
        int updated = 0;
        for (SearchSyncOutbox row : rows) {
            if (row.markPublished(claims.get(row.getId()).leaseToken(), now)) updated++;
        }
        return updated;
    }

    public int markFailed(List<OutboxMessage.Failure> failures) {
        if (failures.isEmpty()) return 0;
        var failed = failures.stream().collect(Collectors.toMap(f -> f.message().eventId(), Function.identity()));
        List<SearchSyncOutbox> rows = outbox.findAllForUpdate(failed.keySet());
        Instant now = databaseTime();
        int updated = 0;
        for (SearchSyncOutbox row : rows) {
            OutboxMessage.Failure failure = failed.get(row.getId());
            if (row.markFailed(failure.message().leaseToken(), now, failure.reason())) updated++;
        }
        return updated;
    }

    public int recoverExpired() {
        List<SearchSyncOutbox> rows = outbox.findExpiredBatch();
        if (rows.isEmpty()) return 0;
        Instant now = databaseTime();
        rows.forEach(row -> row.recoverExpiredLease(now));
        return rows.size();
    }

    public int deletePublished() {
        return outbox.deletePublishedBatch();
    }

    public SearchSyncOutboxRepository.Backlog backlog() {
        return outbox.findBacklog();
    }

    private Instant databaseTime() {
        return Instant.ofEpochMilli(outbox.currentTimeMillis());
    }
}
