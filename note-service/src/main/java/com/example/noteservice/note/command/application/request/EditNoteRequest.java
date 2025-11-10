package com.example.noteservice.note.command.application.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

@Getter
@RequiredArgsConstructor
public class EditNoteRequest {

    @NotBlank
    private final Long noteId;
    @NotBlank
    private final Long authorId;
    @NotBlank
    private final Long baseVersion;

    @NotBlank
    private final List<NoteDelta> deltas;
}
