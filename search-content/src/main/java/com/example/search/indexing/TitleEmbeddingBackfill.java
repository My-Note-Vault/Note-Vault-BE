package com.example.search.indexing;

import com.example.search.content.ContentSourceType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.NoSuchElementException;

/** Explicit, restartable operation. Normal worker startup never invokes the embedding API for a backfill. */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "worker.title-backfill", havingValue = "true")
public class TitleEmbeddingBackfill implements ApplicationRunner {
    private final ContentIndexingTransactions transactions;
    private final ContentIndexingService indexing;

    @Override
    public void run(ApplicationArguments args) {
        long failed = backfill(true) + backfill(false);
        if (failed > 0) throw new IllegalStateException("Title backfill failed for " + failed + " sources; rerun to retry");
        log.info("Title embedding backfill completed");
    }

    private long backfill(boolean documents) {
        long after = 0, failed = 0, visited = 0;
        // A page is a read batch, not a total candidate limit. Continue until every source is visited.
        ContentSourceType type = documents ? ContentSourceType.DOCUMENT : ContentSourceType.DAILY_NOTE;
        while (true) {
            List<Long> page = transactions.sourceIds(type, after);
            if (page.isEmpty()) break;
            for (Long sourceId : page) {
                try {
                    indexing.indexTitle(transactions.readSource(type, sourceId).orElseThrow(NoSuchElementException::new));
                } catch (NoSuchElementException deleted) {
                    // A source deleted after the page was read needs no title vector.
                } catch (RuntimeException failure) {
                    failed++;
                    log.warn("Title backfill failed: type={}, id={}",
                            documents ? "DOCUMENT" : "DAILY_NOTE", sourceId, failure);
                }
                after = sourceId;
                visited++;
            }
            log.info("Title backfill: type={}, visited={}, failed={}",
                    documents ? "DOCUMENT" : "DAILY_NOTE", visited, failed);
        }
        return failed;
    }

}
