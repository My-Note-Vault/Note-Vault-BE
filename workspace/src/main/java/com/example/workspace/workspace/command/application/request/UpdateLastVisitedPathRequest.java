package com.example.workspace.workspace.command.application.request;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class UpdateLastVisitedPathRequest {
    private final String lastVisitedPath;
}
