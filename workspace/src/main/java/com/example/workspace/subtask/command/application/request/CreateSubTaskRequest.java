package com.example.workspace.subtask.command.application.request;

import com.example.workspace.subtask.command.domain.Status;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class CreateSubTaskRequest {

    @NotBlank
    private final Long taskId;
    @NotBlank
    private final String title;

    private final String content;
    private final Status status;
}
