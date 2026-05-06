package com.example.workspace.workspace.command.application.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class CreateWorkSpaceRequest {

    private final Long parentId;

    @NotBlank
    private final String name;
    private final String content;
    private final Boolean isPublic;
}
