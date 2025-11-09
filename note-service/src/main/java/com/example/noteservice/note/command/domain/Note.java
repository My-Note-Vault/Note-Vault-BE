package com.example.noteservice.note.command.domain;

import com.example.common.Auditable;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.NoSuchElementException;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "note",
        indexes = @Index(columnList = "authorId")
)
@Entity
public class Note extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Long authorId;

//    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "MEDIUMTEXT")
    private String content;

    @Version
    @Column(nullable = false)
    private Long version;


    public Note(final Long authorId) {
        this.authorId = authorId;
        this.version = 0L;
    }

    public void edit(
            final Long authorId,
            final String content
    ) {
        if (!this.authorId.equals(authorId)) {
            throw new NoSuchElementException("자신의 노트가 아닙니다!");
        }
        this.content = content;
    }

}
