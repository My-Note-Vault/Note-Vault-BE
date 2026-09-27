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

/** Rebuilds DailyNote body + linked Plans through the normal idempotent worker pipeline. */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "worker.daily-note-backfill", havingValue = "true")
public class DailyNoteIndexingBackfill implements ApplicationRunner {
    private final ContentIndexingTransactions transactions;
    private final SearchContentSync sync;

    @Override
    public void run(ApplicationArguments args) {
        long after = 0, visited = 0, failed = 0;
        while (true) {
            List<Long> page = transactions.sourceIds(ContentSourceType.DAILY_NOTE, after);
            if (page.isEmpty()) break;
            for (Long sourceId : page) {
                try {
                    sync.synchronize(ContentSourceType.DAILY_NOTE, sourceId, null);
                } catch (NoSuchElementException deleted) {
                    // A note deleted after the page was read needs no index.
                } catch (RuntimeException failure) {
                    failed++;
                    log.warn("DailyNote backfill failed: id={}", sourceId, failure);
                }
                after = sourceId;
                visited++;
            }
            log.info("DailyNote backfill: visited={}, failed={}", visited, failed);
        }
        if (failed > 0) {
            throw new IllegalStateException("DailyNote backfill failed for " + failed + " sources; rerun to retry");
        }
        log.info("DailyNote body and Plan backfill completed");
    }

}
