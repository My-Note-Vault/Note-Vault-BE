package com.example.noteservice.note.query.response;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;

@Getter
@RequiredArgsConstructor
public class EditNoteResponse {

    private final Long noteId;
    private final Long version;
    private final LocalDateTime updatedAt;
}
