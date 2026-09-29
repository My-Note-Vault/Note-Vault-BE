package com.example.workspace.search;

import com.notevault.workspace.api.search.FieldKeywordHit;
import com.notevault.workspace.api.search.KeywordSourceType;
import com.notevault.workspace.api.search.SearchSourceContent;
import com.notevault.workspace.api.search.SearchSourceRef;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;

@RequiredArgsConstructor
@Repository
public class SearchRepository {

    private static final String LIKE_ESCAPE = "\\";

    private static final String ACCESSIBLE_SOURCES = """
            WITH source_text AS (
                SELECT 'DOCUMENT' AS source_type, d.id AS source_id,
                       COALESCE(d.title, '') AS title, CAST(d.search_revision AS VARCHAR) AS revision,
                       CASE WHEN d.type = 'WORKSPACE_HOME' THEN d.workspace_id ELSE d.id END AS resource_id,
                       CASE WHEN d.type = 'WORKSPACE_HOME' THEN 'space' ELSE LOWER(d.type) END AS resource_type,
                       COALESCE(d.search_content, '') AS content
                FROM document d
                WHERE EXISTS (SELECT 1 FROM workspace_member wm
                              WHERE wm.workspace_id = d.workspace_id AND wm.member_id = :memberId)
                UNION ALL
                SELECT 'DAILY_NOTE', n.id, CAST(n.logical_date AS VARCHAR), CAST(NULL AS VARCHAR),
                       n.id, 'daily', COALESCE(n.content, '') || COALESCE((
                           SELECT string_agg(E'\\n\\n' || p.content, '' ORDER BY p.id)
                           FROM daily_note_plan dnp JOIN plan p ON p.id = dnp.plan_id
                           WHERE dnp.daily_note_id = n.id AND p.content IS NOT NULL AND p.content <> ''
                       ), '')
                FROM daily_note n WHERE n.author_id = :memberId
            ), accessible AS (
                SELECT source_type, source_id, title, resource_id, resource_type, content,
                       CASE WHEN source_type = 'DAILY_NOTE'
                            THEN encode(sha256(convert_to(content, 'UTF8')), 'hex')
                            ELSE revision END AS version
                FROM source_text
            )
            """;

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public List<FieldKeywordHit> findHybridMatches(Long memberId, String question, List<String> keywords, int limit) {
        Map<String, Object> parameters = new HashMap<>(params(memberId, question));
        parameters.put("question", question);
        parameters.put("limit", limit);
        List<String> counts = new ArrayList<>();
        for (int index = 0; index < keywords.size(); index++) {
            String name = "term" + index;
            parameters.put(name, "%" + escapeLike(keywords.get(index)) + "%");
            counts.add("CASE WHEN text ILIKE :" + name + " ESCAPE :escape THEN 1 ELSE 0 END");
        }
        String count = counts.isEmpty() ? "0" : String.join(" + ", counts);
        String sql = ACCESSIBLE_SOURCES + """
                , fields AS (
                    SELECT source_type, source_id, version, title, 'TITLE' AS matched_field,
                           CAST(NULL AS BIGINT) AS chunk_id, title AS text
                    FROM accessible
                    UNION ALL
                    SELECT c.source_type, c.source_id, a.version, a.title, 'BODY', c.id, c.content
                    FROM content_chunk c JOIN accessible a
                      ON a.source_type = c.source_type AND a.source_id = c.source_id
                     AND a.version = c.source_version
                    UNION ALL
                    SELECT a.source_type, a.source_id, a.version, a.title, 'BODY',
                           CAST(NULL AS BIGINT), a.content
                    FROM accessible a
                    WHERE NOT EXISTS (
                        SELECT 1 FROM content_chunk c
                        WHERE c.source_type = a.source_type AND c.source_id = a.source_id
                          AND c.source_version = a.version
                    )
                ), scored AS (
                    SELECT source_type, source_id, version, title, matched_field, chunk_id,
                           (CASE WHEN LOWER(text) = LOWER(:question) THEN 3.0
                                 WHEN text ILIKE :keyword ESCAPE :escape THEN 2.0
                                 ELSE 0.0 END)
                           + (%s) * 1.0 / :denominator AS score
                    FROM fields
                ), document_matches AS (
                    SELECT *, ROW_NUMBER() OVER (
                        PARTITION BY source_type, source_id, matched_field
                        ORDER BY score DESC, chunk_id NULLS LAST
                    ) AS document_position
                    FROM scored WHERE score > 0
                ), field_matches AS (
                    SELECT *, ROW_NUMBER() OVER (
                        PARTITION BY matched_field ORDER BY score DESC, source_type, source_id
                    ) AS field_position
                    FROM document_matches WHERE document_position = 1
                )
                SELECT source_type, source_id, version, title, matched_field, chunk_id, score
                FROM field_matches WHERE field_position <= :limit
                ORDER BY matched_field, field_position
                """.formatted(count);
        parameters.put("denominator", Math.max(1, keywords.size()));
        return jdbcTemplate.query(sql, parameters, (rows, row) -> new FieldKeywordHit(
                new SearchSourceRef(KeywordSourceType.valueOf(rows.getString("source_type")),
                        rows.getLong("source_id"), rows.getString("version"), rows.getString("title")),
                FieldKeywordHit.MatchedField.valueOf(rows.getString("matched_field")),
                rows.getObject("chunk_id", Long.class), rows.getDouble("score")));
    }

    public List<SearchSourceContent> findAccessibleSources(Long memberId, List<SearchSourceRef> sources) {
        if (sources.isEmpty()) return List.of();
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("memberId", memberId);
        List<String> predicates = new ArrayList<>();
        for (int index = 0; index < sources.size(); index++) {
            SearchSourceRef source = sources.get(index);
            parameters.put("type" + index, source.sourceType().name());
            parameters.put("id" + index, source.sourceId());
            parameters.put("version" + index, source.version());
            parameters.put("title" + index, source.title());
            predicates.add("(source_type = :type%d AND source_id = :id%d AND version = :version%d AND title = :title%d)"
                    .formatted(index, index, index, index));
        }
        return jdbcTemplate.query(ACCESSIBLE_SOURCES + "SELECT * FROM accessible WHERE "
                        + String.join(" OR ", predicates), parameters,
                (rows, row) -> new SearchSourceContent(
                        new SearchSourceRef(KeywordSourceType.valueOf(rows.getString("source_type")),
                                rows.getLong("source_id"), rows.getString("version"), rows.getString("title")),
                        rows.getLong("resource_id"), rows.getString("resource_type"), rows.getString("content")));
    }

    public List<SearchDocumentRow> searchWorkspaceNotes(final Long memberId, final String targetWord) {
        String sql = """
        SELECT *
        FROM (
            SELECT
                'WORKSPACE' AS type,
                w.id,
                w.name AS title,
                COALESCE(home.search_content, w.content) AS content,
                w.created_at,
                home.id AS source_id,
                home.search_revision AS source_revision
            FROM workspace w
            INNER JOIN workspace_member wm ON wm.workspace_id = w.id
            LEFT JOIN document home
              ON home.workspace_id = w.id
             AND home.type = 'WORKSPACE_HOME'
            WHERE wm.member_id = :memberId
              AND (
                  w.name ILIKE :keyword ESCAPE :escape
                  OR COALESCE(home.search_content, w.content, '') ILIKE :keyword ESCAPE :escape
              )

            UNION ALL

            SELECT
                d.type,
                d.id,
                d.title,
                d.search_content AS content,
                d.created_at,
                d.id AS source_id,
                d.search_revision AS source_revision
            FROM document d
            INNER JOIN workspace_member wm ON wm.workspace_id = d.workspace_id
            WHERE wm.member_id = :memberId
              AND d.type <> 'WORKSPACE_HOME'
              AND (
                  d.title ILIKE :keyword ESCAPE :escape
                  OR COALESCE(d.search_content, '') ILIKE :keyword ESCAPE :escape
              )
        ) search_result
        ORDER BY created_at DESC
        """;

        return jdbcTemplate.query(sql, params(memberId, targetWord), (rs, rowNum) -> new SearchDocumentRow(
                SearchDocumentType.valueOf(rs.getString("type")),
                rs.getLong("id"),
                rs.getObject("source_id", Long.class),
                rs.getString("title"),
                rs.getString("content"),
                getLocalDateTime(rs.getTimestamp("created_at")),
                null,
                rs.getObject("source_revision", Long.class)
        ));
    }

    public List<SearchDocumentRow> searchDailyNotes(final Long memberId, final String targetWord) {
        String sql = """
        SELECT
            dn.id,
            dn.logical_date,
            CASE
                WHEN COALESCE(dn.content, '') ILIKE :keyword ESCAPE :escape THEN dn.content
                ELSE matched_plan.content
            END AS content,
            dn.created_at,
            dn.content_revision
        FROM daily_note dn
        LEFT JOIN LATERAL (
            SELECT p.content
            FROM daily_note_plan dnp
            INNER JOIN plan p ON p.id = dnp.plan_id
            WHERE dnp.daily_note_id = dn.id
              AND COALESCE(p.content, '') ILIKE :keyword ESCAPE :escape
            ORDER BY p.id
            LIMIT 1
        ) matched_plan ON TRUE
        WHERE dn.author_id = :memberId
          AND (
              COALESCE(dn.content, '') ILIKE :keyword ESCAPE :escape
              OR matched_plan.content IS NOT NULL
          )
        ORDER BY dn.created_at ASC
        """;

        return jdbcTemplate.query(sql, params(memberId, targetWord), (rs, rowNum) -> new SearchDocumentRow(
                SearchDocumentType.DAILY_NOTE,
                rs.getLong("id"),
                rs.getLong("id"),
                getLocalDate(rs.getObject("logical_date")).toString(),
                rs.getString("content"),
                getLocalDateTime(rs.getTimestamp("created_at")),
                getLocalDate(rs.getObject("logical_date")),
                rs.getObject("content_revision", Long.class)
        ));
    }

    private Map<String, Object> params(final Long memberId, final String targetWord) {
        return Map.of(
                "memberId", memberId,
                "keyword", "%" + escapeLike(targetWord.strip()) + "%",
                "escape", LIKE_ESCAPE
        );
    }

    private String escapeLike(final String value) {
        return value
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
    }

    private LocalDateTime getLocalDateTime(final Timestamp value) {
        if (value == null) {
            return null;
        }
        return value.toLocalDateTime();
    }

    private LocalDate getLocalDate(final Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof LocalDate localDate) {
            return localDate;
        }
        return java.sql.Date.valueOf(value.toString()).toLocalDate();
    }
}
