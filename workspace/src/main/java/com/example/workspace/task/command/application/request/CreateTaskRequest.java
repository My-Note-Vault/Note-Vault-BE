package com.example.workspace.task.command.application.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class CreateTaskRequest {

    @NotBlank
    private final Long workSpaceId;

}
