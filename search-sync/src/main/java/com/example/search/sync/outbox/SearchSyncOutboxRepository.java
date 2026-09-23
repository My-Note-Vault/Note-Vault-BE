package com.example.search.sync.outbox;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface SearchSyncOutboxRepository extends JpaRepository<SearchSyncOutbox, UUID> {
    @Query(value = """
            SELECT * FROM search_sync_outbox
            WHERE status = 'PENDING' AND next_attempt_at <= CURRENT_TIMESTAMP
            ORDER BY next_attempt_at, created_at, event_id
            LIMIT 10 FOR UPDATE SKIP LOCKED
            """, nativeQuery = true)
    List<SearchSyncOutbox> findPublishableBatch();

    @Query(value = """
            SELECT * FROM search_sync_outbox
            WHERE status = 'PROCESSING' AND lease_expires_at <= CURRENT_TIMESTAMP
            ORDER BY lease_expires_at, event_id
            LIMIT 50 FOR UPDATE SKIP LOCKED
            """, nativeQuery = true)
    List<SearchSyncOutbox> findExpiredBatch();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from SearchSyncOutbox o where o.eventId in :ids order by o.eventId")
    List<SearchSyncOutbox> findAllForUpdate(@Param("ids") Collection<UUID> ids);

    // Use the database clock for leases and retries on every API instance.
    @Query(value = "SELECT CAST(EXTRACT(EPOCH FROM clock_timestamp()) * 1000 AS BIGINT)", nativeQuery = true)
    long currentTimeMillis();

    @Modifying
    @Query(value = """
            WITH expired AS (
                SELECT event_id FROM search_sync_outbox
                WHERE status = 'PUBLISHED' AND published_at < CURRENT_TIMESTAMP - INTERVAL '7 days'
                ORDER BY published_at, event_id
                LIMIT 1000 FOR UPDATE SKIP LOCKED
            )
            DELETE FROM search_sync_outbox o USING expired e WHERE o.event_id = e.event_id
            """, nativeQuery = true)
    int deletePublishedBatch();

    @Query(value = """
            SELECT COUNT(*) FILTER (WHERE status = 'PENDING') AS pending,
                   COUNT(*) FILTER (WHERE status = 'PROCESSING') AS processing,
                   GREATEST(0, COALESCE(EXTRACT(EPOCH FROM
                       (CURRENT_TIMESTAMP - MIN(created_at))), 0)) AS "oldestSeconds"
            FROM search_sync_outbox WHERE status IN ('PENDING', 'PROCESSING')
            """, nativeQuery = true)
    Backlog findBacklog();

    interface Backlog {
        long getPending();
        long getProcessing();
        double getOldestSeconds();
    }
}
