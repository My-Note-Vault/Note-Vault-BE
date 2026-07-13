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
                w.id AS workspace_id,
                w.name AS workspace_name,
                w.content AS workspace_content,
                w.created_at AS workspace_created_at,
                NULL AS task_id,
                NULL AS task_title,
                NULL AS task_content,
                NULL AS task_created_at,
                NULL AS subtask_id,
                NULL AS subtask_title,
                NULL AS subtask_content,
                NULL AS subtask_created_at,
                NULL AS note_id,
                NULL AS note_title,
                NULL AS note_content,
                NULL AS note_created_at,
                w.created_at AS matched_created_at
            FROM workspace w
            INNER JOIN workspace_member wm ON wm.workspace_id = w.id
            WHERE wm.member_id = :memberId
              AND (w.name LIKE :keyword ESCAPE :escape OR COALESCE(w.content, '') LIKE :keyword ESCAPE :escape)

            UNION ALL

            SELECT
                'TASK' AS type,
                w.id AS workspace_id,
                w.name AS workspace_name,
                w.content AS workspace_content,
                w.created_at AS workspace_created_at,
                d.id AS task_id,
                d.title AS task_title,
                d.content AS task_content,
                d.created_at AS task_created_at,
                NULL AS subtask_id,
                NULL AS subtask_title,
                NULL AS subtask_content,
                NULL AS subtask_created_at,
                NULL AS note_id,
                NULL AS note_title,
                NULL AS note_content,
                NULL AS note_created_at,
                d.created_at AS matched_created_at
            FROM document d
            INNER JOIN workspace w ON w.id = d.workspace_id
            INNER JOIN workspace_member wm ON wm.workspace_id = w.id
            WHERE wm.member_id = :memberId
              AND d.type = 'TASK'
              AND (d.title LIKE :keyword ESCAPE :escape OR COALESCE(d.content, '') LIKE :keyword ESCAPE :escape)

            UNION ALL

            SELECT
                'SUBTASK' AS type,
                w.id AS workspace_id,
                w.name AS workspace_name,
                w.content AS workspace_content,
                w.created_at AS workspace_created_at,
                t.id AS task_id,
                t.title AS task_title,
                t.content AS task_content,
                t.created_at AS task_created_at,
                d.id AS subtask_id,
                d.title AS subtask_title,
                d.content AS subtask_content,
                d.created_at AS subtask_created_at,
                NULL AS note_id,
                NULL AS note_title,
                NULL AS note_content,
                NULL AS note_created_at,
                d.created_at AS matched_created_at
            FROM document d
            INNER JOIN document t ON t.id = d.parent_id AND t.type = 'TASK'
            INNER JOIN workspace w ON w.id = d.workspace_id
            INNER JOIN workspace_member wm ON wm.workspace_id = w.id
            WHERE wm.member_id = :memberId
              AND d.type = 'SUBTASK'
              AND (d.title LIKE :keyword ESCAPE :escape OR COALESCE(d.content, '') LIKE :keyword ESCAPE :escape)

            UNION ALL

            SELECT
                'NOTE' AS type,
                w.id AS workspace_id,
                w.name AS workspace_name,
                w.content AS workspace_content,
                w.created_at AS workspace_created_at,
                t.id AS task_id,
                t.title AS task_title,
                t.content AS task_content,
                t.created_at AS task_created_at,
                s.id AS subtask_id,
                s.title AS subtask_title,
                s.content AS subtask_content,
                s.created_at AS subtask_created_at,
                d.id AS note_id,
                d.title AS note_title,
                d.content AS note_content,
                d.created_at AS note_created_at,
                d.created_at AS matched_created_at
            FROM document d
            INNER JOIN document s ON s.id = d.parent_id AND s.type = 'SUBTASK'
            INNER JOIN document t ON t.id = s.parent_id AND t.type = 'TASK'
            INNER JOIN workspace w ON w.id = d.workspace_id
            INNER JOIN workspace_member wm ON wm.workspace_id = w.id
            WHERE wm.member_id = :memberId
              AND d.type = 'NOTE'
              AND (d.title LIKE :keyword ESCAPE :escape OR COALESCE(d.content, '') LIKE :keyword ESCAPE :escape)
        ) search_result
        ORDER BY matched_created_at ASC
        """;

        return jdbcTemplate.query(sql, params(memberId, targetWord), (rs, rowNum) -> new SearchDocumentRow(
                SearchDocumentType.valueOf(rs.getString("type")),
                rs.getLong("workspace_id"),
                rs.getString("workspace_name"),
                rs.getString("workspace_content"),
                getLocalDateTime(rs.getTimestamp("workspace_created_at")),
                getLong(rs.getObject("task_id")),
                rs.getString("task_title"),
                rs.getString("task_content"),
                getLocalDateTime(rs.getTimestamp("task_created_at")),
                getLong(rs.getObject("subtask_id")),
                rs.getString("subtask_title"),
                rs.getString("subtask_content"),
                getLocalDateTime(rs.getTimestamp("subtask_created_at")),
                getLong(rs.getObject("note_id")),
                rs.getString("note_title"),
                rs.getString("note_content"),
                getLocalDateTime(rs.getTimestamp("note_created_at"))
        ));
    }

    public List<SearchResponse.DailyNoteResult> searchDailyNotes(final Long memberId, final String targetWord) {
        String sql = """
        SELECT id, logical_date, content, created_at
        FROM daily_note
        WHERE author_id = :memberId
          AND COALESCE(content, '') LIKE :keyword ESCAPE :escape
        ORDER BY created_at ASC
        """;

        return jdbcTemplate.query(sql, params(memberId, targetWord), (rs, rowNum) -> new SearchResponse.DailyNoteResult(
                rs.getLong("id"),
                getLocalDate(rs.getObject("logical_date")),
                rs.getString("content"),
                getLocalDateTime(rs.getTimestamp("created_at"))
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

    private Long getLong(final Object value) {
        if (value == null) {
            return null;
        }
        return ((Number) value).longValue();
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
