package com.example.workspace.unfolded.domain;

import com.example.workspace.unfolded.TaskOverviewResponse;

import java.util.List;

public interface UnfoldedNoteJdbcRepository {
    void findAllUnfoldedNotes(Long authorId);

    List<TaskOverviewResponse> findAllNotesInfoByWorkSpaceId(Long authorId, Long workspaceId);
}
