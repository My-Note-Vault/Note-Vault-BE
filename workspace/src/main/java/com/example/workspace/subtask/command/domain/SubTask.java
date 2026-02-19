package com.example.workspace.subtask.command.domain;

import com.example.common.Auditable;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.NoSuchElementException;
import java.util.Objects;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "subtask",
        indexes = @Index(columnList = "authorId")
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

    @Enumerated(EnumType.STRING)
    private Status status;

    private String title;

    @Column(columnDefinition = "MEDIUMTEXT")
    private String content;


    public SubTask(
            final Long authorId,
            final Long taskId,
            final String title,
            final String content,
            final Status status
    ) {
        this.authorId = authorId;
        this.taskId = taskId;
        this.title = title;
        this.content = content;

        this.status = Objects.requireNonNullElse(status, Status.NOT_STARTED);
    }

    public void edit(
            final Long authorId,
            final String title,
            final String content,
            final Status status
    ) {
        if (!this.authorId.equals(authorId)) {
            throw new NoSuchElementException("자신의 노트가 아닙니다!");
        }
        this.title = title == null ? this.title : title;
        this.content = content == null ? this.content : content;
        this.status = status == null ? this.status : status;
    }


}
