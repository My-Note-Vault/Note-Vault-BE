package com.example.search.retrieval;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
@Repository
public class IndexedChunkReader {
    private final NamedParameterJdbcTemplate jdbc;

    @Transactional(readOnly = true)
    public List<IndexedChunk> findAccessibleCandidates(Long memberId, String model, int limit) {
        if (limit < 1) {
            throw new IllegalArgumentException("limit must be positive");
        }
        return jdbc.query("""
                SELECT c.id, c.source_type, c.source_id, c.resource_id, c.resource_type,
                       c.source_title, c.content, c.embedding
                FROM content_chunk c
                WHERE c.embedding_status = 'READY' AND c.embedding_model = :model
                  AND ((c.source_type = 'DOCUMENT'
                        AND EXISTS (SELECT 1 FROM workspace_member wm
                                    WHERE wm.workspace_id = c.workspace_id AND wm.member_id = :memberId)
                        AND EXISTS (SELECT 1 FROM document d
                                    WHERE d.id = c.source_id
                                      AND CAST(d.search_revision AS VARCHAR) = c.source_version))
                    OR (c.source_type = 'DAILY_NOTE' AND c.owner_id = :memberId
                        AND EXISTS (SELECT 1 FROM daily_note n
                                    WHERE n.id = c.source_id AND n.updated_at = c.source_updated_at
                                      AND CAST(n.content_revision AS VARCHAR) = c.source_version)))
                ORDER BY c.id DESC
                LIMIT :limit
                """, Map.of("memberId", memberId, "model", model, "limit", limit),
                (rs, row) -> new IndexedChunk(
                        rs.getLong("id"), rs.getString("source_type"), rs.getLong("source_id"),
                        rs.getLong("resource_id"), rs.getString("resource_type"),
                        rs.getString("source_title"), rs.getString("content"), rs.getString("embedding")));
    }
}
