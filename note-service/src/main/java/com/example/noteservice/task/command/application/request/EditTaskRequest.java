package com.example.noteservice.task.command.application.request;

import com.example.noteservice.task.command.domain.Status;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

@Getter
@RequiredArgsConstructor
public class EditTaskRequest {

    @NotBlank
    private final Long taskId;
    @NotBlank
    private final Long authorId;
    @NotBlank
    private final String title;
    @NotBlank
    private final String content;
    @NotBlank
    private final Status status;

}
