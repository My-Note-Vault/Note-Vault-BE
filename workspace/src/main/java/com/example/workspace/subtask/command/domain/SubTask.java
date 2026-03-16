package com.example.workspace.subtask.command.domain;

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
        name = "subtask",
        indexes = @Index(columnList = "author_id")
)
@Entity
public class SubTask extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long taskId;

    @Column(nullable = false, unique = true)
    private Long authorId;

    @Embedded
    private Schedule schedule;

    private String title;

    @Column(columnDefinition = "MEDIUMTEXT")
    private String content;


    public SubTask(
            final Long authorId,
            final Long taskId,
            final LocalDateTime startDateTime,
            final LocalDateTime endDateTime,
            final Status status,
            final String title,
            final String content
    ) {
        this.authorId = authorId;
        this.taskId = taskId;
        this.schedule = new Schedule(status, startDateTime, endDateTime);
        this.title = title;
        this.content = content;

    }

    public void edit(
            final Long authorId,
            final String title,
            final String content,
            final Status status,
            final LocalDateTime startDateTime,
            final LocalDateTime endDateTime
    ) {
        if (!this.authorId.equals(authorId)) {
            throw new NoSuchElementException("자신의 노트가 아닙니다!");
        }
        this.title = title == null ? this.title : title;
        this.content = content == null ? this.content : content;
        this.schedule.edit(status, startDateTime, endDateTime);
    }


}
