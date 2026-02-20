package com.example.workspace.workspace.command.application.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

@Getter
@RequiredArgsConstructor
public class UpdateParticipantsRequest {

    @NotBlank
    private final Long workSpaceId;
    @NotNull
    private final List<Long> memberIdsToAdd;
    @NotNull
    private final List<Long> memberIdsToRemove;
}
