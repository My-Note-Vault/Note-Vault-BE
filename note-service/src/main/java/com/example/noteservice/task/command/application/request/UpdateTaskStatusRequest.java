package com.example.noteservice.task.command.application.request;

import com.example.noteservice.task.command.domain.Status;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class UpdateTaskStatusRequest {

    private final Long taskId;
    private final Long authorId;
    private final Status status;
}
