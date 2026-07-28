package com.example.workspace.document.command.application;

import com.example.workspace.document.command.domain.DocumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Component
public class DocumentCrdtCompactionScheduler {

    private static final int MAX_BATCHES_PER_DOCUMENT = 20;

    private final DocumentRepository documentRepository;
    private final DocumentCrdtCompactionService compactionService;

    @Scheduled(
            cron = "${workspace.crdt.compaction.cron:0 0 3 * * *}",
            zone = "${workspace.crdt.compaction.zone:Asia/Seoul}"
    )
    public void compactDaily() {
        List<Long> candidateIds = documentRepository.findCrdtCompactionCandidateIds();
        for (Long documentId : candidateIds) {
            compactDocument(documentId);
        }
        int purged = compactionService.purgeExpiredArchives(LocalDateTime.now());
        if (purged > 0) {
            log.info("Purged {} expired CRDT delta archives", purged);
        }
    }

    private void compactDocument(final Long documentId) {
        try {
            boolean requireThreshold = true;
            for (int batch = 0; batch < MAX_BATCHES_PER_DOCUMENT; batch++) {
                DocumentCrdtCompactionService.CompactionBatchResult result =
                        compactionService.compactNextBatch(documentId, requireThreshold);
                if (!result.compacted() || !result.hasMore()) {
                    return;
                }
                requireThreshold = false;
            }
            log.warn(
                    "CRDT compaction reached batch limit for document {}",
                    documentId
            );
        } catch (Exception exception) {
            log.error("Failed to compact CRDT updates for document {}", documentId, exception);
        }
    }
}
