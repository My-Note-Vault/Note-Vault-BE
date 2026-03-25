package com.example.workspace.unfolded.infra;

import com.example.workspace.unfolded.NoteInfoResponse;
import com.example.workspace.unfolded.domain.NoteType;
import com.example.workspace.unfolded.domain.UnfoldedNoteJdbcRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
@Repository
public class UnfoldedNotesJdbcRepository implements UnfoldedNoteJdbcRepository {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void findAllUnfoldedNotes(final Long authorId) {

    }

    @Override
    public List<NoteInfoResponse> findAllNotesInfo(final Long authorId) {
        String workSpaceSql = """
                SELECT w.id, w.parent_id, w.name
                FROM workspace w
                WHERE creator_id = ?
                """;
        RowMapper<NoteInfoResponse> workSpaceMapper = (rs, rowNum) -> new NoteInfoResponse(
                rs.getLong("w.id"),
                NoteType.WORKSPACE,
                rs.getLong("w.parent_id"),
                rs.getString("w.name")
        );
        List<NoteInfoResponse> workSpaces = jdbcTemplate.query(workSpaceSql, workSpaceMapper, authorId);

        String taskSql = """
                SELECT t.id, t.workspace_id, t.title
                FROM task t
                WHERE author_id = ?
                """;
        RowMapper<NoteInfoResponse> taskeMapper = (rs, rowNum) -> new NoteInfoResponse(
                rs.getLong("t.id"),
                NoteType.TASK,
                rs.getLong("t.workspace_id"),
                rs.getString("t.title")
        );
        List<NoteInfoResponse> tasks = jdbcTemplate.query(taskSql, taskeMapper, authorId);

        String subTaskSql = """
                SELECT s.id, s.task_id, s.title
                FROM subtask s
                WHERE author_id = ?
                """;
        RowMapper<NoteInfoResponse> subTaskMapper = (rs, rowNum) -> new NoteInfoResponse(
                rs.getLong("s.id"),
                NoteType.SUBTASK,
                rs.getLong("s.task_id"),
                rs.getString("s.title")
        );
        List<NoteInfoResponse> subTasks = jdbcTemplate.query(subTaskSql, subTaskMapper, authorId);

        String triviaSql = """
                SELECT t.id, t.subtask_id, t.title
                FROM trivia t
                WHERE author_id = ?
                """;
        RowMapper<NoteInfoResponse> triviaMapper = (rs, rowNum) -> new NoteInfoResponse(
                rs.getLong("t.id"),
                NoteType.TRIVIA,
                rs.getLong("t.subtask_id"),
                rs.getString("t.title")
        );
        List<NoteInfoResponse> trivia = jdbcTemplate.query(triviaSql, triviaMapper, authorId);

        List<NoteInfoResponse> result = new ArrayList<>();
        result.addAll(workSpaces);
        result.addAll(tasks);
        result.addAll(subTasks);
        result.addAll(trivia);
        return result;
    }
}
