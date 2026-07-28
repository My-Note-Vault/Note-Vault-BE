package com.example.workspace.search;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
@Repository
public class SearchRepository {

    private static final String LIKE_ESCAPE = "\\";

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public List<SearchDocumentRow> searchWorkspaceNotes(final Long memberId, final String targetWord) {
        String sql = """
        SELECT *
        FROM (
            SELECT
                'WORKSPACE' AS type,
                w.id,
                w.name AS title,
                COALESCE(home.search_content, w.content) AS content,
                w.created_at
            FROM workspace w
            INNER JOIN workspace_member wm ON wm.workspace_id = w.id
            LEFT JOIN document home
              ON home.workspace_id = w.id
             AND home.type = 'WORKSPACE_HOME'
            WHERE wm.member_id = :memberId
              AND (
                  w.name LIKE :keyword ESCAPE :escape
                  OR COALESCE(home.search_content, w.content, '') LIKE :keyword ESCAPE :escape
              )

            UNION ALL

            SELECT
                d.type,
                d.id,
                d.title,
                d.search_content AS content,
                d.created_at
            FROM document d
            INNER JOIN workspace_member wm ON wm.workspace_id = d.workspace_id
            WHERE wm.member_id = :memberId
              AND d.type <> 'WORKSPACE_HOME'
              AND (
                  d.title LIKE :keyword ESCAPE :escape
                  OR COALESCE(d.search_content, '') LIKE :keyword ESCAPE :escape
              )
        ) search_result
        ORDER BY created_at DESC
        """;

        return jdbcTemplate.query(sql, params(memberId, targetWord), (rs, rowNum) -> new SearchDocumentRow(
                SearchDocumentType.valueOf(rs.getString("type")),
                rs.getLong("id"),
                rs.getString("title"),
                rs.getString("content"),
                getLocalDateTime(rs.getTimestamp("created_at"))
        ));
    }

    public List<SearchResponse.SearchResult> searchDailyNotes(final Long memberId, final String targetWord) {
        String sql = """
        SELECT id, logical_date, content, created_at
        FROM daily_note
        WHERE author_id = :memberId
          AND COALESCE(content, '') LIKE :keyword ESCAPE :escape
        ORDER BY created_at ASC
        """;

        return jdbcTemplate.query(sql, params(memberId, targetWord), (rs, rowNum) -> new SearchResponse.SearchResult(
                rs.getLong("id"),
                SearchDocumentType.DAILY_NOTE,
                getLocalDate(rs.getObject("logical_date")).toString(),
                rs.getString("content"),
                getLocalDateTime(rs.getTimestamp("created_at")),
                getLocalDate(rs.getObject("logical_date"))
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
