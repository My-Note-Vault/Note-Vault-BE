package com.example.workspace.search;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
@Service
public class SearchService {

    private final SearchRepository searchRepository;

    @Transactional(readOnly = true)
    public SearchResponse searchAllNotes(final Long memberId, final String targetWord) {
        if (targetWord == null || targetWord.isBlank()) {
            return new SearchResponse(List.of(), List.of());
        }

        List<SearchDocumentRow> documentRows = searchRepository.searchWorkspaceDocuments(memberId, targetWord);
        List<SearchResponse.DailyNoteResult> dailyNotes = searchRepository.searchDailyNotes(memberId, targetWord);

        Map<Long, WorkSpaceResultBuilder> workspaceBuilders = new LinkedHashMap<>();

        for (SearchDocumentRow row : documentRows) {
            WorkSpaceResultBuilder workSpace = workspaceBuilders.computeIfAbsent(
                    row.workSpaceId(),
                    id -> new WorkSpaceResultBuilder(
                            row.workSpaceId(),
                            row.workSpaceName(),
                            row.workSpaceContent(),
                            row.workSpaceCreatedAt()
                    )
            );

            if (row.type() == SearchDocumentType.WORKSPACE) {
                workSpace.matched = true;
                continue;
            }

            TaskResultBuilder task = workSpace.tasks.computeIfAbsent(
                    row.taskId(),
                    id -> new TaskResultBuilder(row.taskId(), row.taskTitle(), row.taskContent(), row.taskCreatedAt())
            );

            if (row.type() == SearchDocumentType.TASK) {
                task.matched = true;
                continue;
            }

            SubTaskResultBuilder subTask = task.subTasks.computeIfAbsent(
                    row.subTaskId(),
                    id -> new SubTaskResultBuilder(row.subTaskId(), row.subTaskTitle(), row.subTaskContent(), row.subTaskCreatedAt())
            );

            if (row.type() == SearchDocumentType.SUBTASK) {
                subTask.matched = true;
                continue;
            }

            subTask.notes.putIfAbsent(
                    row.noteId(),
                    new NoteResultBuilder(row.noteId(), row.noteTitle(), row.noteContent(), row.noteCreatedAt(), true)
            );
        }

        List<SearchResponse.WorkSpaceResult> workSpaces = workspaceBuilders.values().stream()
                .sorted(Comparator.comparing(
                        WorkSpaceResultBuilder::createdAt,
                        Comparator.nullsLast(LocalDateTime::compareTo)
                ))
                .map(WorkSpaceResultBuilder::build)
                .toList();

        return new SearchResponse(workSpaces, dailyNotes);
    }

    private static class WorkSpaceResultBuilder {
        private final Long id;
        private final String name;
        private final String content;
        private final LocalDateTime createdAt;
        private final Map<Long, TaskResultBuilder> tasks = new LinkedHashMap<>();
        private boolean matched;

        private WorkSpaceResultBuilder(final Long id, final String name, final String content, final LocalDateTime createdAt) {
            this.id = id;
            this.name = name;
            this.content = content;
            this.createdAt = createdAt;
        }

        private LocalDateTime createdAt() {
            return createdAt;
        }

        private SearchResponse.WorkSpaceResult build() {
            return new SearchResponse.WorkSpaceResult(
                    id,
                    name,
                    content,
                    createdAt,
                    matched,
                    tasks.values().stream()
                            .sorted(Comparator.comparing(
                                    TaskResultBuilder::createdAt,
                                    Comparator.nullsLast(LocalDateTime::compareTo)
                            ))
                            .map(TaskResultBuilder::build)
                            .toList()
            );
        }
    }

    private static class TaskResultBuilder {
        private final Long id;
        private final String title;
        private final String content;
        private final LocalDateTime createdAt;
        private final Map<Long, SubTaskResultBuilder> subTasks = new LinkedHashMap<>();
        private boolean matched;

        private TaskResultBuilder(final Long id, final String title, final String content, final LocalDateTime createdAt) {
            this.id = id;
            this.title = title;
            this.content = content;
            this.createdAt = createdAt;
        }

        private LocalDateTime createdAt() {
            return createdAt;
        }

        private SearchResponse.TaskResult build() {
            return new SearchResponse.TaskResult(
                    id,
                    title,
                    content,
                    createdAt,
                    matched,
                    subTasks.values().stream()
                            .sorted(Comparator.comparing(
                                    SubTaskResultBuilder::createdAt,
                                    Comparator.nullsLast(LocalDateTime::compareTo)
                            ))
                            .map(SubTaskResultBuilder::build)
                            .toList()
            );
        }
    }

    private static class SubTaskResultBuilder {
        private final Long id;
        private final String title;
        private final String content;
        private final LocalDateTime createdAt;
        private final Map<Long, NoteResultBuilder> notes = new LinkedHashMap<>();
        private boolean matched;

        private SubTaskResultBuilder(final Long id, final String title, final String content, final LocalDateTime createdAt) {
            this.id = id;
            this.title = title;
            this.content = content;
            this.createdAt = createdAt;
        }

        private LocalDateTime createdAt() {
            return createdAt;
        }

        private SearchResponse.SubTaskResult build() {
            return new SearchResponse.SubTaskResult(
                    id,
                    title,
                    content,
                    createdAt,
                    matched,
                    notes.values().stream()
                            .sorted(Comparator.comparing(
                                    NoteResultBuilder::createdAt,
                                    Comparator.nullsLast(LocalDateTime::compareTo)
                            ))
                            .map(NoteResultBuilder::build)
                            .toList()
            );
        }
    }

    private record NoteResultBuilder(
            Long id,
            String title,
            String content,
            LocalDateTime createdAt,
            boolean matched
    ) {

        private SearchResponse.NoteResult build() {
            return new SearchResponse.NoteResult(id, title, content, createdAt, matched);
        }
    }
}
