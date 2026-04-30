package com.example.workspace.unfolded.infra;

import com.example.workspace.unfolded.TaskOverviewResponse;
import com.example.workspace.unfolded.domain.UnfoldedNoteJdbcRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.*;

@RequiredArgsConstructor
@Repository
public class UnfoldedNoteJdbcRepositoryImpl implements UnfoldedNoteJdbcRepository {

    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    @Override
    public void findAllUnfoldedNotes(final Long authorId) {

    }

    @Override
    public List<TaskOverviewResponse> findAllNotesInfoByWorkSpaceId(final Long authorId, final Long workspaceId) {

        String sql = """
        SELECT
            t.id AS task_id,
            t.title AS task_title,
            s.id AS subtask_id,
            s.title AS subtask_title,
            tr.id AS trivia_id,
            tr.title AS trivia_title
        FROM task t
        LEFT JOIN subtask s ON s.task_id = t.id
        LEFT JOIN trivia tr ON tr.subtask_id = s.id
        WHERE t.workspace_id = :workspaceId
        ORDER BY t.created_at, s.id, tr.id
        """;

        Map<String, Object> params = Map.of("workspaceId", workspaceId);

        return namedParameterJdbcTemplate.query(sql, params, rs -> {

            Map<Long, TaskOverviewResponse> taskMap = new LinkedHashMap<>();
            Map<Long, TaskOverviewResponse.SubTaskSummary> subTaskMap = new HashMap<>();

            while (rs.next()) {
                long taskId = rs.getLong("task_id");
                String taskTitle = rs.getString("task_title");

                TaskOverviewResponse task = taskMap.computeIfAbsent(taskId, id ->
                        new TaskOverviewResponse(id, taskTitle, new ArrayList<>())
                );

                Long subTaskId = (Long) rs.getObject("subtask_id");
                if (subTaskId == null) continue;

                String subTaskTitle = rs.getString("subtask_title");

                TaskOverviewResponse.SubTaskSummary subTask =
                        subTaskMap.computeIfAbsent(subTaskId, id -> {
                            var created = new TaskOverviewResponse.SubTaskSummary(
                                    id,
                                    subTaskTitle,
                                    new ArrayList<>()
                            );
                            task.subTaskSummaries().add(created);
                            return created;
                        });

                Long triviaId = (Long) rs.getObject("trivia_id");
                if (triviaId == null) continue;

                String triviaTitle = rs.getString("trivia_title");

                subTask.triviaSummaries().add(
                        new TaskOverviewResponse.SubTaskSummary.TriviaSummary(
                                triviaId,
                                triviaTitle
                        )
                );
            }
            return new ArrayList<>(taskMap.values());
        });
    }
}
