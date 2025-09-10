package com.example.noteservice.note.domain;

import com.example.common.Auditable;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
public class Note extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "MEDIUMTEXT")
    private String content;

    public Note(final String title) {
        this.title = title;
    }

    public Note(
            final String title,
            final String content
    ) {
        this.title = title;
        this.content = content;
    }
}
