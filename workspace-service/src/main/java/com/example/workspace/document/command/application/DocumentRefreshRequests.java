package com.example.workspace.document.command.application;

import com.example.search.sync.SearchSyncRecorder;
import com.example.workspace.document.command.domain.Document;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;

/** Batches accepted updates in the same transaction as the delta and document revision. */
@Component
public class DocumentRefreshRequests {
    private final SearchSyncRecorder recorder;
    private final int minUpdates;
    private final Duration maxWait;

    public DocumentRefreshRequests(SearchSyncRecorder recorder,
            @Value("${workspace.crdt.refresh.min-updates:100}") int minUpdates,
            @Value("${workspace.crdt.refresh.max-wait:PT1M}") Duration maxWait) {
        if (minUpdates < 1 || maxWait == null || maxWait.toMillis() < 1) {
            throw new IllegalArgumentException("Invalid document refresh batching limits");
        }
        this.recorder = recorder;
        this.minUpdates = minUpdates;
        this.maxWait = maxWait;
    }

    public void recordDelta(Document document) {
        var batch = recorder.batchDocumentRefresh(document.getId(), document.getLatestRevision(),
                document.getRefreshBatchEventId(), document.getRefreshBatchBaseRevision(), minUpdates, maxWait);
        document.trackRefreshBatch(batch.eventId(), batch.baseRevision());
    }
}
