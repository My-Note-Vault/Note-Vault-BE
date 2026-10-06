package com.example.search.crdt;

import com.example.search.indexing.ContentChunker;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Short transactions only; all JavaScript runs after the read lock has been released. */
@Service
@RequiredArgsConstructor
@Transactional(propagation = Propagation.REQUIRES_NEW, timeout = 5)
public class DocumentCrdtTransactions {
    private final JdbcTemplate jdbc;
    private final ProjectionProperties limits;

    public Optional<Input> load(long documentId, long requestedRevision) {
        // The API's delta writer and cleanup both lock this same document row for update.
        List<Head> heads = jdbc.query("""
                SELECT snapshot_revision, coalesce(search_revision, 0) AS search_revision,
                       latest_revision, coalesce(octet_length(crdt_state), 0) AS state_bytes
                FROM document WHERE id = ? FOR SHARE
                """, (rs, row) -> new Head(rs.getLong("snapshot_revision"), rs.getLong("search_revision"),
                rs.getLong("latest_revision"), rs.getLong("state_bytes")), documentId);
        if (heads.isEmpty()) return Optional.empty();
        Head head = heads.getFirst();
        if (head.latest() < requestedRevision) throw new IllegalStateException("Document behind requested revision");
        if (head.snapshot() > head.latest() || head.search() > head.latest()
                || (head.snapshot() > 0 && head.stateBytes() == 0)) {
            throw new IllegalStateException("Invalid document revision state");
        }
        if (head.snapshot() >= head.latest() && head.search() >= head.latest()) return Optional.empty();

        if (head.latest() - head.snapshot() > limits.maxUpdates() || head.stateBytes() > limits.maxInputBytes()) {
            throw new YjsProjectionException(YjsProjectionException.Reason.INPUT_LIMIT);
        }
        // Check bytes before materializing the bytea columns into Java heap.
        Long deltaBytes = jdbc.queryForObject("""
                SELECT coalesce(sum(octet_length(yjs_update)), 0) FROM document_delta
                WHERE document_id = ? AND revision > ? AND revision <= ?
                """, Long.class, documentId, head.snapshot(), head.latest());
        if (deltaBytes == null || deltaBytes > limits.maxInputBytes() - head.stateBytes()) {
            throw new YjsProjectionException(YjsProjectionException.Reason.INPUT_LIMIT);
        }
        byte[] state = jdbc.queryForObject("SELECT crdt_state FROM document WHERE id = ?", byte[].class, documentId);
        List<byte[]> updates = new ArrayList<>();
        jdbc.query("""
                SELECT revision, yjs_update FROM document_delta
                WHERE document_id = ? AND revision > ? AND revision <= ? ORDER BY revision
                """, rs -> {
            long expected = head.snapshot() + updates.size() + 1;
            if (rs.getLong("revision") != expected) throw new IllegalStateException("CRDT revision gap");
            updates.add(rs.getBytes("yjs_update"));
        }, documentId, head.snapshot(), head.latest());
        if (head.snapshot() + updates.size() != head.latest()) {
            throw new IllegalStateException("CRDT history does not reach latest revision");
        }
        return Optional.of(new Input(documentId, head.snapshot(), head.latest(), state, List.copyOf(updates)));
    }

    /** Snapshot and body always represent the same captured revision; indexing follows after commit. */
    public void saveProjection(Input input, YjsDocumentConverter.Projection projection) {
        checkInterrupted();
        int changed = jdbc.update("""
                UPDATE document SET crdt_state = ?, snapshot_revision = ?,
                       search_content = ?, search_content_hash = ?, search_revision = ?
                WHERE id = ? AND snapshot_revision = ?
                  AND coalesce(search_revision, 0) <= ? AND latest_revision >= ?
                """, projection.crdtState(), input.targetRevision(), projection.content(),
                ContentChunker.sha256(projection.content()), input.targetRevision(), input.documentId(),
                input.snapshotRevision(), input.targetRevision(), input.targetRevision());
        if (changed == 0) {
            List<Long> covered = jdbc.query("""
                    SELECT least(snapshot_revision, coalesce(search_revision, 0))
                    FROM document WHERE id = ?
                    """, (rs, row) -> rs.getLong(1), input.documentId());
            if (!covered.isEmpty() && covered.getFirst() < input.targetRevision()) {
                throw new IllegalStateException("Concurrent document refresh requires retry");
            }
        }
    }

    private void checkInterrupted() {
        if (Thread.currentThread().isInterrupted()) throw new IllegalStateException("CRDT delivery interrupted");
    }

    private record Head(long snapshot, long search, long latest, long stateBytes) { }
    public record Input(long documentId, long snapshotRevision, long targetRevision,
                        byte[] state, List<byte[]> updates) { }
}
