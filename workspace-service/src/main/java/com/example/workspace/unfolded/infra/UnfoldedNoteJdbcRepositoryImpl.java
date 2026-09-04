package com.example.workspace.unfolded.infra;

import com.example.workspace.unfolded.TaskOverviewResponse;
import com.example.workspace.document.command.domain.DocumentType;
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
                SELECT d.id, d.type, d.title, d.parent_id
                FROM document d
                INNER JOIN workspace_member wm ON wm.workspace_id = d.workspace_id
                WHERE d.workspace_id = :workspaceId
                  AND wm.member_id = :authorId
                  AND d.type IN ('TASK', 'NOTE')
                ORDER BY d.created_at, d.id
                """;

        Map<String, Object> params = Map.of(
                "workspaceId", workspaceId,
                "authorId", authorId
        );

        return namedParameterJdbcTemplate.query(sql, params, rs -> {

            Map<Long, TaskOverviewResponse> documents = new LinkedHashMap<>();

            while (rs.next()) {
                long id = rs.getLong("id");
                documents.put(id, new TaskOverviewResponse(
                        id,
                        DocumentType.valueOf(rs.getString("type")),
                        rs.getString("title"),
                        (Long) rs.getObject("parent_id"),
                        new ArrayList<>()
                ));
            }

            List<TaskOverviewResponse> roots = new ArrayList<>();
            for (TaskOverviewResponse document : documents.values()) {
                TaskOverviewResponse parent = document.parentId() == null
                        ? null
                        : documents.get(document.parentId());
                if (parent == null) {
                    roots.add(document);
                } else {
                    parent.children().add(document);
                }
            }
            return roots;
        });
    }
}
