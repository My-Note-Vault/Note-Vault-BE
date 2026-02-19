package com.example.noteservice.trivia.command.domain;

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
        indexes = @Index(columnList = "authorId")
)
@Entity
public class Trivia extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long parentTaskId;

    @Column(nullable = false, unique = true)
    private Long authorId;

    private String title;

    @Column(columnDefinition = "MEDIUMTEXT")
    private String content;

    private Boolean isHidden;


    public Trivia(
            final Long authorId,
            final Long parentTaskId,
            final String title,
            final String content,
            final Boolean isHidden
    ) {
        this.authorId = authorId;
        this.parentTaskId = parentTaskId;
        this.title = title;
        this.content = content;

        this.isHidden = Objects.requireNonNullElse(isHidden, false);

    }

    public void edit(
            final Long authorId,
            final String title,
            final String content,
            final Boolean isHidden
    ) {
        if (!this.authorId.equals(authorId)) {
            throw new NoSuchElementException("자신의 노트가 아닙니다!");
        }
        this.title = title == null ? this.title : title;
        this.content = content == null ? this.content : content;
        this.isHidden = isHidden == null ? this.isHidden : isHidden;
    }


}
