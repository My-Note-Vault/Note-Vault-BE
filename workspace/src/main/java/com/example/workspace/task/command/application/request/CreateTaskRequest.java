package com.example.workspace.task.command.application.request;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class CreateTaskRequest {

    private final Long workSpaceId;

}
