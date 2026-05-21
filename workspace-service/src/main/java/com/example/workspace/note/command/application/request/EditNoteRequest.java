package com.example.workspace.note.command.application.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class EditNoteRequest {

    @NotBlank
    private final Long noteId;
    @NotBlank
    private final Long parentTaskId;
    @NotBlank
    private final String title;
    @NotBlank
    private final String content;
    @NotBlank
    private final Boolean isPublic;

}
