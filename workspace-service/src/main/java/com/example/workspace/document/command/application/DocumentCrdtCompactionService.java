package com.example.workspace.document.command.application;

import com.example.workspace.document.command.domain.Document;
import com.example.workspace.document.command.domain.DocumentDelta;
import com.example.workspace.document.command.domain.DocumentDeltaRepository;
import com.example.workspace.document.command.domain.DocumentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;

@RequiredArgsConstructor
@Service
/** Legacy name: this service only deletes deltas already covered by a durable snapshot. */
public class DocumentCrdtCompactionService {

    private static final long MIN_UPDATE_COUNT = 1_000L;
    private static final long MIN_UPDATE_BYTES = 5L * 1024L * 1024L;

    private final DocumentRepository documentRepository;
    private final DocumentDeltaRepository documentDeltaRepository;

    @Transactional
    public CompactionBatchResult compactNextBatch(
            final Long documentId,
            final boolean requireThreshold
    ) {
        Document document = documentRepository.findWithWriteLockById(documentId)
                .orElseThrow(() -> new NoSuchElementException("Document를 찾을 수 없습니다"));

        long compactedRevision = valueOrZero(document.getCompactedRevision());
        long targetRevision = valueOrZero(document.getSnapshotRevision());
        if (
                document.getCrdtState() == null ||
                document.getCrdtState().length == 0 ||
                targetRevision <= compactedRevision
        ) {
            return CompactionBatchResult.notCompacted();
        }

        LocalDateTime now = LocalDateTime.now();
        if (requireThreshold && !shouldCompact(document, compactedRevision, targetRevision, now)) {
            return CompactionBatchResult.notCompacted();
        }

        List<DocumentDelta> batch =
                documentDeltaRepository
                        .findTop500ByDocumentIdAndRevisionGreaterThanAndRevisionLessThanEqualOrderByRevisionAsc(
                                documentId,
                                compactedRevision,
                                targetRevision
                        );
        if (batch.isEmpty()) {
            throw new IllegalStateException(
                    "CRDT compaction target has no source updates: documentId=" + documentId
            );
        }
        validateContinuousBatch(compactedRevision, batch);

        // Only deltas already represented in the durable snapshot can be deleted.
        documentDeltaRepository.deleteAllInBatch(batch);

        long batchEndRevision = batch.get(batch.size() - 1).getRevision();
        document.markCompacted(batchEndRevision, now);
        documentRepository.save(document);

        return new CompactionBatchResult(true, batchEndRevision < targetRevision);
    }

    private boolean shouldCompact(
            final Document document,
            final long compactedRevision,
            final long targetRevision,
            final LocalDateTime now
    ) {
        long pendingUpdateCount = targetRevision - compactedRevision;
        if (pendingUpdateCount >= MIN_UPDATE_COUNT) {
            return true;
        }

        Long pendingUpdateBytes = documentDeltaRepository.sumCrdtUpdateBytes(
                document.getId(),
                compactedRevision,
                targetRevision
        );
        if (pendingUpdateBytes != null && pendingUpdateBytes >= MIN_UPDATE_BYTES) {
            return true;
        }

        LocalDateTime lastCompactionReference = document.getLastCompactedAt() == null
                ? document.getCreatedAt()
                : document.getLastCompactedAt();
        return lastCompactionReference != null &&
                !lastCompactionReference.isAfter(now.minusDays(1));
    }

    private void validateContinuousBatch(
            final long compactedRevision,
            final List<DocumentDelta> batch
    ) {
        long expectedRevision = compactedRevision + 1;
        for (DocumentDelta delta : batch) {
            if (delta.getRevision() != expectedRevision) {
                throw new IllegalStateException(
                        "CRDT compaction revision gap: expected=" + expectedRevision
                                + ", actual=" + delta.getRevision()
                );
            }
            expectedRevision++;
        }
    }

    private long valueOrZero(final Long value) {
        return value == null ? 0L : value;
    }

    public record CompactionBatchResult(boolean compacted, boolean hasMore) {

        public static CompactionBatchResult notCompacted() {
            return new CompactionBatchResult(false, false);
        }
    }
}
