package com.example.workspace.task.command.domain;

import com.example.common.Auditable;
import com.example.workspace.task.command.domain.value.Schedule;
import com.example.workspace.task.command.domain.value.Status;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "task",
        indexes = @Index(columnList = "author_id")
)
@Entity
public class Task extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "workspace_id", nullable = false)
    private Long workSpaceId;

    @Column(nullable = false, unique = true)
    private Long authorId;

    @Embedded
    private Schedule schedule;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "MEDIUMTEXT")
    private String content;

    public Task(
            final Long workSpaceId,
            final Long authorId,
            final String title,
            final String content,
            final Status status
    ) {
        this(workSpaceId, authorId, null, null, status, title, content);
    }

    public Task(
            final Long workSpaceId,
            final Long authorId,
            final LocalDateTime startDateTime,
            final Status status,
            final String title,
            final String content
    ) {
        this(workSpaceId, authorId, startDateTime, null, status, title, content);
    }

    public Task(
            final Long workSpaceId,
            final Long authorId,
            final LocalDateTime startDateTime,
            final LocalDateTime endDateTime,
            final Status status,
            final String title,
            final String content
    ) {
        this.workSpaceId = workSpaceId;
        this.authorId = authorId;
        this.schedule = new Schedule(status, startDateTime, endDateTime);
        this.title = title;
        this.content = content;
    }

    public void edit(
            final Long authorId,
            final String title,
            final String content,

            final Status status,
            final LocalDateTime startDate,
            final LocalDateTime endDate
    ) {
        if (!this.authorId.equals(authorId)) {
            throw new IllegalArgumentException("자신의 노트가 아닙니다!");
        }
        this.title = title == null ? this.title : title;
        this.content = content == null ? this.content : content;

        this.schedule.edit(status, startDate, endDate);
    }

    public void updateStatus(final Long authorId, final Status status) {
        this.edit(authorId, null, null, status, null, null);
    }
}
