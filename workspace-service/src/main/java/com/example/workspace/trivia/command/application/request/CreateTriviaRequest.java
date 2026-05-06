package com.example.workspace.trivia.command.application.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class CreateTriviaRequest {
    @NotBlank
    private final Long subTaskId;

}
