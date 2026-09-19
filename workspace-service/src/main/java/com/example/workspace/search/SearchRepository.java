package com.example.workspace.search;

import com.notevault.workspace.api.search.FieldKeywordHit;
import com.notevault.workspace.api.search.KeywordSourceType;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.PreparedStatementCallback;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.function.Consumer;

@RequiredArgsConstructor
@Repository
public class SearchRepository {

    private static final String LIKE_ESCAPE = "\\";

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public void scanHybridMatches(Long memberId, String question, List<String> keywords,
                                  Consumer<FieldKeywordHit> consumer) {
        Map<String, Object> parameters = new HashMap<>(params(memberId, question));
        parameters.put("question", question);
        List<String> counts = new ArrayList<>();
        for (int index = 0; index < keywords.size(); index++) {
            String name = "term" + index;
            parameters.put(name, "%" + escapeLike(keywords.get(index)) + "%");
            counts.add("CASE WHEN text ILIKE :" + name + " ESCAPE :escape THEN 1 ELSE 0 END");
        }
        String count = counts.isEmpty() ? "0" : String.join(" + ", counts);
        String sql = """
                WITH accessible AS (
                    SELECT 'DOCUMENT' AS source_type, d.id AS source_id,
                           COALESCE(d.title, '') AS title,
                           CAST(d.search_revision AS VARCHAR) AS version
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
                               ), ''), 'UTF8')), 'hex')
                    FROM daily_note n WHERE n.author_id = :memberId
                ), fields AS (
                    SELECT source_type, source_id, CAST(NULL AS BIGINT) AS chunk_id, title AS text
                    FROM accessible
                    UNION ALL
                    SELECT c.source_type, c.source_id, c.id, c.content
                    FROM content_chunk c JOIN accessible a
                      ON a.source_type = c.source_type AND a.source_id = c.source_id
                     AND a.version = c.source_version
                ), scored AS (
                    SELECT source_type, source_id, chunk_id,
                           (CASE WHEN LOWER(text) = LOWER(:question) THEN 3.0
                                 WHEN text ILIKE :keyword ESCAPE :escape THEN 2.0
                                 ELSE 0.0 END)
                           + (%s) * 1.0 / :denominator AS score
                    FROM fields
                )
                SELECT source_type, source_id, chunk_id, score FROM scored WHERE score > 0
                """.formatted(count);
        parameters.put("denominator", Math.max(1, keywords.size()));
        jdbcTemplate.execute(sql, parameters, (PreparedStatementCallback<Void>) statement -> {
            statement.setFetchSize(256);
            try (var rows = statement.executeQuery()) {
                while (rows.next()) {
                    consumer.accept(new FieldKeywordHit(
                            KeywordSourceType.valueOf(rows.getString("source_type")),
                            rows.getLong("source_id"), rows.getObject("chunk_id", Long.class),
                            rows.getDouble("score")));
                }
            }
            return null;
        });
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
