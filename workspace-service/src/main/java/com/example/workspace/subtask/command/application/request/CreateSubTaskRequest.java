package com.example.workspace.subtask.command.application.request;

import com.example.workspace.task.command.domain.value.Status;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;

@Getter
@RequiredArgsConstructor
public class CreateSubTaskRequest {

    @NotBlank
    private final Long taskId;

}
