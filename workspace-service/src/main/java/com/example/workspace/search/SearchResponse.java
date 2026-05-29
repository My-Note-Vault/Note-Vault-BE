package com.example.workspace.search;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record SearchResponse(
        List<WorkSpaceResult> workSpaces,
        List<DailyNoteResult> dailyNotes
) {

    public record WorkSpaceResult(
            Long id,
            String name,
            String content,
            LocalDateTime createdAt,
            boolean matched,
            List<TaskResult> tasks
    ) {
    }

    public record TaskResult(
            Long id,
            String title,
            String content,
            LocalDateTime createdAt,
            boolean matched,
            List<SubTaskResult> subTasks
    ) {
    }

    public record SubTaskResult(
            Long id,
            String title,
            String content,
            LocalDateTime createdAt,
            boolean matched,
            List<NoteResult> notes
    ) {
    }

    public record NoteResult(
            Long id,
            String title,
            String content,
            LocalDateTime createdAt,
            boolean matched
    ) {
    }

    public record DailyNoteResult(
            Long id,
            LocalDate logicalDate,
            String content,
            LocalDateTime createdAt
    ) {
    }
}
