package com.example.platformservice.member.domain.value;

import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Embeddable
public class Note implements Serializable {

    @EqualsAndHashCode.Include
    @Enumerated(EnumType.STRING)
    private NoteType type;
    @EqualsAndHashCode.Include
    private Long noteId;

    public enum NoteType {
        BOARD, CALENDAR, DAILY_NOTE, SPACE, TASK, SUBTASK, TRIVIA
    }
}
