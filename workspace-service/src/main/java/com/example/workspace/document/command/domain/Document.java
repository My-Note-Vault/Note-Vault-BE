package com.example.workspace.document.command.domain;

import com.example.common.Auditable;
import com.example.workspace.task.command.domain.value.Schedule;
import com.example.workspace.task.command.domain.value.Status;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.NoSuchElementException;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "document",
        indexes = {
                @Index(columnList = "workspace_id"),
                @Index(columnList = "parent_id"),
                @Index(columnList = "author_id"),
                @Index(columnList = "type")
        }
)
@Entity
public class Document extends Auditable {

    private static final String DEFAULT_TASK_TITLE = "새 Task";
    private static final String DEFAULT_SUBTASK_TITLE = "새 SubTask";
    private static final String DEFAULT_NOTE_TITLE = "새 Document";
    private static final String DEFAULT_CONTENT = "";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DocumentType type;

    @Column(name = "workspace_id", nullable = false)
    private Long workSpaceId;

    @Column(name = "parent_id")
    private Long parentId;

    @Column(nullable = false)
    private Long authorId;

    @Embedded
    private Schedule schedule;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(nullable = false)
    private Boolean isPublic;

    private Document(
            final DocumentType type,
            final Long workSpaceId,
            final Long parentId,
            final Long authorId,
            final Schedule schedule,
            final String title,
            final String content,
            final Boolean isPublic
    ) {
        this.type = type;
        this.workSpaceId = workSpaceId;
        this.parentId = parentId;
        this.authorId = authorId;
        this.schedule = schedule;
        this.title = title;
        this.content = content;
        this.isPublic = isPublic;
    }

    public static Document task(final Long workSpaceId, final Long authorId) {
        return new Document(
                DocumentType.TASK,
                workSpaceId,
                null,
                authorId,
                new Schedule(Status.NOT_STARTED, LocalDateTime.now(), LocalDateTime.now()),
                DEFAULT_TASK_TITLE,
                DEFAULT_CONTENT,
                false
        );
    }

    public static Document subTask(final Document parentTask, final Long authorId) {
        parentTask.validateType(DocumentType.TASK);
        return new Document(
                DocumentType.SUBTASK,
                parentTask.getWorkSpaceId(),
                parentTask.getId(),
                authorId,
                new Schedule(Status.NOT_STARTED, null, null),
                DEFAULT_SUBTASK_TITLE,
                DEFAULT_CONTENT,
                false
        );
    }

    public static Document note(final Document parentSubTask, final Long authorId) {
        parentSubTask.validateType(DocumentType.SUBTASK);
        return new Document(
                DocumentType.NOTE,
                parentSubTask.getWorkSpaceId(),
                parentSubTask.getId(),
                authorId,
                null,
                DEFAULT_NOTE_TITLE,
                DEFAULT_CONTENT,
                false
        );
    }

    public void edit(
            final String title,
            final String content,
            final Status status,
            final LocalDateTime startDateTime,
            final LocalDateTime endDateTime,
            final Boolean isPublic
    ) {
        this.title = title == null ? this.title : title;
        this.content = content == null ? this.content : content;
        this.isPublic = isPublic == null ? this.isPublic : isPublic;

        if (this.type != DocumentType.NOTE) {
            if (this.schedule == null) {
                this.schedule = new Schedule(Status.NOT_STARTED, null, null);
            }
            this.schedule.edit(status, startDateTime, endDateTime);
        }
    }

    public void moveTo(final Document parent) {
        if (!this.type.requiresParent()) {
            throw new IllegalArgumentException(this.type + " 문서는 부모 문서가 필요하지 않습니다");
        }
        parent.validateType(this.type.parentType());
        this.parentId = parent.getId();
        this.workSpaceId = parent.getWorkSpaceId();
    }

    public void validateType(final DocumentType expected) {
        if (this.type != expected) {
            throw new NoSuchElementException(expected.notFoundMessage());
        }
    }
}
