package com.example.workspace.task.command.application.request;

import com.example.workspace.task.command.domain.value.Status;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;

@Getter
@RequiredArgsConstructor
public class CreateTaskRequest {

    private final LocalDateTime startDateTime;
    private final LocalDateTime endDateTime;
    private final Status status;

    @NotBlank
    private final String title;
    private final String content;
}
