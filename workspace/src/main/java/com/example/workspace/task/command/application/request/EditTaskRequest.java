package com.example.workspace.task.command.application.request;

import com.example.workspace.task.command.domain.Status;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class EditTaskRequest {

    @NotBlank
    private final Long taskId;
    @NotBlank
    private final String title;
    @NotBlank
    private final String content;
    @NotBlank
    private final Status status;

}
