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
            n.id AS note_id,
            n.title AS note_title
        FROM document t
        LEFT JOIN document s ON s.parent_id = t.id AND s.type = 'SUBTASK'
        LEFT JOIN document n ON n.parent_id = s.id AND n.type = 'NOTE'
        WHERE t.workspace_id = :workspaceId
          AND t.type = 'TASK'
        ORDER BY t.created_at, s.id, n.id
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

                Long noteId = (Long) rs.getObject("note_id");
                if (noteId == null) continue;

                String noteTitle = rs.getString("note_title");

                subTask.noteSummaries().add(
                        new TaskOverviewResponse.SubTaskSummary.NoteSummary(
                                noteId,
                                noteTitle
                        )
                );
            }
            return new ArrayList<>(taskMap.values());
        });
    }
}
