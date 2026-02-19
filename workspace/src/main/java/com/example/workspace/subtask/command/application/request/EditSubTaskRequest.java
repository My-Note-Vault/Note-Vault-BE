package com.example.workspace.subtask.command.application.request;

import com.example.workspace.subtask.command.domain.Status;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class EditSubTaskRequest {

    @NotBlank
    private final Long subTaskId;
    @NotBlank
    private final String title;
    @NotBlank
    private final String content;
    @NotBlank
    private final Status status;

}
