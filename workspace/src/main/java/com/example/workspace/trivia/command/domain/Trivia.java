package com.example.workspace.trivia.command.domain;

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
        name = "trivia",
        indexes = @Index(columnList = "author_id")
)
@Entity
public class Trivia extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long parentSubTaskId;

    @Column(nullable = false, unique = true)
    private Long authorId;

    private String title;

    @Column(columnDefinition = "MEDIUMTEXT")
    private String content;

    private Boolean isPublic;


    public Trivia(
            final Long authorId,
            final Long parentSubTaskId,
            final String title,
            final String content,
            final Boolean isPublic
    ) {
        this.authorId = authorId;
        this.parentSubTaskId = parentSubTaskId;
        this.title = title;
        this.content = content;

        this.isPublic = Objects.requireNonNullElse(isPublic, false);

    }

    public void edit(
            final Long authorId,
            final Long parentTaskId,
            final String title,
            final String content,
            final Boolean isPublic
    ) {
        if (!this.authorId.equals(authorId)) {
            throw new NoSuchElementException("자신의 노트가 아닙니다!");
        }
        this.parentSubTaskId = parentTaskId == null ? this.parentSubTaskId : parentTaskId;
        this.title = title == null ? this.title : title;
        this.content = content == null ? this.content : content;
        this.isPublic = isPublic == null ? this.isPublic : isPublic;
    }


}
