package com.example.noteservice.note.command.application.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class NoteDelta {

    @NotBlank
    private final String operation;

    private final Integer pos;

    private final Integer start;
    private final Integer end;

    private final String text;
}
