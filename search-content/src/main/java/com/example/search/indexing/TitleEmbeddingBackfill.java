package com.example.search.indexing;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.NoSuchElementException;

/** Explicit, restartable operation. Normal worker startup never invokes the embedding API for a backfill. */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "worker.title-backfill", havingValue = "true")
public class TitleEmbeddingBackfill implements ApplicationRunner {
    private final JdbcTemplate jdbc;
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
        String sql = documents
                ? "SELECT id, author_id FROM document WHERE id > ? ORDER BY id LIMIT 256"
                : "SELECT id, author_id FROM daily_note WHERE id > ? ORDER BY id LIMIT 256";
        while (true) {
            List<Source> page = jdbc.query(sql,
                    (rs, row) -> new Source(rs.getLong("id"), rs.getLong("author_id")), after);
            if (page.isEmpty()) break;
            for (Source source : page) {
                try {
                    indexing.indexTitle(documents
                            ? transactions.readDocumentForIndexing(source.id())
                            : transactions.readDailyNote(source.ownerId(), source.id()));
                } catch (NoSuchElementException deleted) {
                    // A source deleted after the page was read needs no title vector.
                } catch (RuntimeException failure) {
                    failed++;
                    log.warn("Title backfill failed: type={}, id={}",
                            documents ? "DOCUMENT" : "DAILY_NOTE", source.id(), failure);
                }
                after = source.id();
                visited++;
            }
            log.info("Title backfill: type={}, visited={}, failed={}",
                    documents ? "DOCUMENT" : "DAILY_NOTE", visited, failed);
        }
        return failed;
    }

    private record Source(Long id, Long ownerId) { }
}
