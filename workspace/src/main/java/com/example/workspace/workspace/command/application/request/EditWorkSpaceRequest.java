package com.example.workspace.workspace.command.application.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class EditWorkSpaceRequest {

    @NotBlank
    private final Long workSpaceId;
    @NotBlank
    private final Long parentId;
    @NotBlank
    private final String name;
    @NotBlank
    private final String content;
    @NotBlank
    private final Boolean isPublic;

}
