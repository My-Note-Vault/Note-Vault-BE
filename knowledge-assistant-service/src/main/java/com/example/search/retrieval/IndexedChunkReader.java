package com.example.search.retrieval;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.PreparedStatementCallback;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.function.Consumer;

@RequiredArgsConstructor
@Repository
public class IndexedChunkReader {
    private final NamedParameterJdbcTemplate jdbc;

    private static final String ACCESSIBLE_SOURCES = """
            WITH accessible AS (
                SELECT 'DOCUMENT' AS source_type, d.id AS source_id,
                       COALESCE(d.title, '') AS title,
                       CAST(d.search_revision AS VARCHAR) AS version,
                       CASE WHEN d.type = 'WORKSPACE_HOME' THEN d.workspace_id ELSE d.id END AS resource_id,
                       CASE WHEN d.type = 'WORKSPACE_HOME' THEN 'space' ELSE LOWER(d.type) END AS resource_type
                FROM document d
                WHERE EXISTS (SELECT 1 FROM workspace_member wm
                              WHERE wm.workspace_id = d.workspace_id AND wm.member_id = :memberId)
                UNION ALL
                SELECT 'DAILY_NOTE', n.id, CAST(n.logical_date AS VARCHAR),
                       encode(sha256(convert_to(
                           COALESCE(n.content, '') || COALESCE((
                               SELECT string_agg(E'\\n\\n' || p.content, '' ORDER BY p.id)
                               FROM daily_note_plan dnp JOIN plan p ON p.id=dnp.plan_id
                               WHERE dnp.daily_note_id=n.id AND p.content IS NOT NULL AND p.content <> ''
                           ), ''), 'UTF8')), 'hex'), n.id, 'daily'
                FROM daily_note n WHERE n.author_id = :memberId
            )
            """;

    private static final String CHUNKS = ACCESSIBLE_SOURCES + """
            SELECT c.id, c.source_type, c.source_id, a.resource_id, a.resource_type,
                   a.title AS source_title, c.content,
                   CASE WHEN c.embedding_status = 'READY' AND c.embedding_model = :model
                        THEN c.embedding ELSE NULL END AS embedding
            FROM content_chunk c JOIN accessible a
              ON a.source_type = c.source_type AND a.source_id = c.source_id
             AND a.version = c.source_version
            """;

    @Transactional(readOnly = true)
    public void scanAccessibleChunks(Long memberId, String model, Consumer<IndexedChunk> consumer) {
        jdbc.execute(CHUNKS, Map.of("memberId", memberId, "model", model),
                (PreparedStatementCallback<Void>) statement -> {
                    // PostgreSQL uses a cursor inside this read-only transaction.
                    statement.setFetchSize(256);
                    try (ResultSet rows = statement.executeQuery()) {
                        while (rows.next()) consumer.accept(readChunk(rows));
                    }
                    return null;
                });
    }

    @Transactional(readOnly = true)
    public void scanAccessibleTitles(Long memberId, String model, Consumer<IndexedTitle> consumer) {
        jdbc.execute(ACCESSIBLE_SOURCES + """
                SELECT t.source_type, t.source_id, t.embedding
                FROM content_title_embedding t JOIN accessible a
                  ON a.source_type = t.source_type AND a.source_id = t.source_id
                 AND a.title = t.source_title
                WHERE t.embedding_status = 'READY' AND t.embedding_model = :model
                """, Map.of("memberId", memberId, "model", model),
                (PreparedStatementCallback<Void>) statement -> {
                    statement.setFetchSize(256);
                    try (ResultSet rows = statement.executeQuery()) {
                        while (rows.next()) consumer.accept(new IndexedTitle(
                                rows.getString("source_type"), rows.getLong("source_id"),
                                rows.getString("embedding")));
                    }
                    return null;
                });
    }

    @Transactional(readOnly = true)
    public List<IndexedChunk> findAccessibleByIds(Long memberId, String model, List<Long> ids) {
        if (ids.isEmpty()) return List.of();
        return jdbc.query(CHUNKS + " WHERE c.id IN (:ids)",
                Map.of("memberId", memberId, "model", model, "ids", ids), (rs, row) -> readChunk(rs));
    }

    private IndexedChunk readChunk(ResultSet rs) throws SQLException {
        return new IndexedChunk(rs.getLong("id"), rs.getString("source_type"), rs.getLong("source_id"),
                rs.getLong("resource_id"), rs.getString("resource_type"), rs.getString("source_title"),
                rs.getString("content"), rs.getString("embedding"));
    }

    public record IndexedTitle(String sourceType, Long sourceId, String embedding) {
    }
}
