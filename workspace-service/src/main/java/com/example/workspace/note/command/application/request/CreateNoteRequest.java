package com.example.workspace.note.command.application.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class CreateNoteRequest {
    @NotBlank
    private final Long subTaskId;

}
