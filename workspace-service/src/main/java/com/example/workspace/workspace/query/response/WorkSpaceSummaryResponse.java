package com.example.workspace.workspace.query.response;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class WorkSpaceSummaryResponse {

    private final Long id;
    private final String name;
    private final Boolean isPublic;
}
