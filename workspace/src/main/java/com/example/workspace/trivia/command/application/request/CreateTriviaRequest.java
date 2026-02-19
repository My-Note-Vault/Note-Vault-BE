package com.example.workspace.trivia.command.application.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class CreateTriviaRequest {

    @NotBlank
    private final Long parentTaskId;
    @NotBlank
    private final String title;

    private final String content;
    private final Boolean isPublic;
}
