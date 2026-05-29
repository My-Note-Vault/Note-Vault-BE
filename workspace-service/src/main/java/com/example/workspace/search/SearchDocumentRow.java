package com.example.workspace.search;

import java.time.LocalDateTime;

record SearchDocumentRow(
        SearchDocumentType type,
        Long workSpaceId,
        String workSpaceName,
        String workSpaceContent,
        LocalDateTime workSpaceCreatedAt,
        Long taskId,
        String taskTitle,
        String taskContent,
        LocalDateTime taskCreatedAt,
        Long subTaskId,
        String subTaskTitle,
        String subTaskContent,
        LocalDateTime subTaskCreatedAt,
        Long noteId,
        String noteTitle,
        String noteContent,
        LocalDateTime noteCreatedAt
) {
}
