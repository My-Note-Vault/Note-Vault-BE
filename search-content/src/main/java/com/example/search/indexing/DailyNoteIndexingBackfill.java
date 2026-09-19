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

/** Rebuilds DailyNote body + linked Plans through the normal idempotent worker pipeline. */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "worker.daily-note-backfill", havingValue = "true")
public class DailyNoteIndexingBackfill implements ApplicationRunner {
    private final JdbcTemplate jdbc;
    private final ContentIndexingService indexing;

    @Override
    public void run(ApplicationArguments args) {
        long after = 0, visited = 0, failed = 0;
        while (true) {
            List<Source> page = jdbc.query(
                    "SELECT id, author_id FROM daily_note WHERE id > ? ORDER BY id LIMIT 256",
                    (rs, row) -> new Source(rs.getLong("id"), rs.getLong("author_id")), after);
            if (page.isEmpty()) break;
            for (Source source : page) {
                try {
                    indexing.indexDailyNote(source.ownerId(), source.id());
                } catch (NoSuchElementException deleted) {
                    // A note deleted after the page was read needs no index.
                } catch (RuntimeException failure) {
                    failed++;
                    log.warn("DailyNote backfill failed: id={}", source.id(), failure);
                }
                after = source.id();
                visited++;
            }
            log.info("DailyNote backfill: visited={}, failed={}", visited, failed);
        }
        if (failed > 0) {
            throw new IllegalStateException("DailyNote backfill failed for " + failed + " sources; rerun to retry");
        }
        log.info("DailyNote body and Plan backfill completed");
    }

    private record Source(Long id, Long ownerId) { }
}
