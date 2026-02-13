package com.example.noteservice.task.command.application.request;

import com.example.noteservice.task.command.domain.Status;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class CreateTaskRequest {

    private final Status status;
    private final String title;
    private final String content;
}
