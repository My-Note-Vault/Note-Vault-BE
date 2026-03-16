package com.example.workspace.search;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@RequiredArgsConstructor
@Repository
public class SearchRepository {

    private final JdbcTemplate jdbcTemplate;

    public NoteGroup searchAllDocuments(final Long memberId) {
        String searchWorkSpaceQuery = """
        SELECT id, title
        FROM workspace
        WHERE author_id = ?
        ORDER BY status DESC, id DESC
    """;

        String searchTaskQuery = """
        SELECT id, title
        FROM task
        WHERE author_id = ?
        ORDER BY status DESC, id DESC
    """;

        String searchSubTaskQuery = """
        SELECT id, title
        FROM subtask
        WHERE author_id = ?
        ORDER BY status DESC, id DESC
    """;

        String searchTriviaQuery = """
        SELECT id, title
        FROM trivia
        WHERE author_id = ?
        ORDER BY status DESC, id DESC
    """;

        var workSpaces = jdbcTemplate.query(
                searchWorkSpaceQuery,
                (rs, rowNum) -> new NoteGroup.Note(
                        rs.getLong("id"),
                        rs.getString("title"),
                        rs.getString("status")
                ),
                memberId
        );

        var tasks = jdbcTemplate.query(
                searchTaskQuery,
                (rs, rowNum) -> new NoteGroup.Note(
                        rs.getLong("id"),
                        rs.getString("title"),
                        rs.getString("status")
                ),
                memberId
        );

        var subTasks = jdbcTemplate.query(
                searchSubTaskQuery,
                (rs, rowNum) -> new NoteGroup.Note(
                        rs.getLong("id"),
                        rs.getString("title"),
                        rs.getString("status")
                ),
                memberId
        );

        var trivia = jdbcTemplate.query(
                searchTriviaQuery,
                (rs, rowNum) -> new NoteGroup.Note(
                        rs.getLong("id"),
                        rs.getString("title"),
                        rs.getString("status")
                ),
                memberId
        );

        return new NoteGroup(workSpaces, tasks, subTasks, trivia);
    }
}
