package com.example.workspace.task.command.application.request;

import com.example.workspace.task.command.domain.value.Status;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;

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

    private final LocalDateTime startDateTime;
    private final LocalDateTime endDateTime;

}
