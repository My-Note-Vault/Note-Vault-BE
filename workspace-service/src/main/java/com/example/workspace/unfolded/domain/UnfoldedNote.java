package com.example.workspace.unfolded.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "unfolded_notes",
        indexes = @Index(columnList = "author_id")
)
@Entity
public class UnfoldedNote {

    @EmbeddedId
    private UnfoldedNoteId id;

    private Long authorId;

    public UnfoldedNote(final UnfoldedNoteId id, final Long authorId) {
        this.id = id;
        this.authorId = authorId;
    }
}
