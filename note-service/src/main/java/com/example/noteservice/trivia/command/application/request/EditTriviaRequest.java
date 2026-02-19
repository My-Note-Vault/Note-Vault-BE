package com.example.noteservice.trivia.command.application.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class EditTriviaRequest {

    @NotBlank
    private final Long parentTaskId;
    @NotBlank
    private final String title;
    @NotBlank
    private final String content;
    @NotBlank
    private final Boolean isHidden;

}
