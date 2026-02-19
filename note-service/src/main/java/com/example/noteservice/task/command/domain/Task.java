package com.example.noteservice.task.command.domain;

import com.example.common.Auditable;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "task",
        indexes = @Index(columnList = "authorId")
)
@Entity
public class Task extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Long authorId;

    @Enumerated(EnumType.STRING)
    private Status status;

    private String title;

    @Column(columnDefinition = "MEDIUMTEXT")
    private String content;

    public Task(
            final Long authorId,
            final String title,
            final String content,
            final Status status
    ) {
        this.authorId = authorId;
        this.title = title;
        this.content = content;
        this.status = status;
    }

    public void edit(
            final Long authorId,
            final String title,
            final String content,
            final Status status
    ) {
        if (!this.authorId.equals(authorId)) {
            throw new IllegalArgumentException("자신의 노트가 아닙니다!");
        }
        this.title = title == null ? this.title : title;
        this.content = content == null ? this.content : content;
        this.status = status == null ? this.status : status;
    }

    public void updateStatus(final Long memberId, final Status status) {
        if (!this.authorId.equals(memberId)) {
            throw new IllegalArgumentException("자신의 노트가 아닙니다!");
        }
        this.status = status;
    }

}
