package com.example.workspace.unfolded.domain;

import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Getter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Embeddable
public class UnfoldedNoteId implements Serializable {

    @EqualsAndHashCode.Include
    @Enumerated(value = EnumType.STRING)
    private NoteType type;

    @EqualsAndHashCode.Include
    private Long noteId;

    public UnfoldedNoteId(NoteType type, Long noteId) {
        this.type = type;
        this.noteId = noteId;
    }
}
