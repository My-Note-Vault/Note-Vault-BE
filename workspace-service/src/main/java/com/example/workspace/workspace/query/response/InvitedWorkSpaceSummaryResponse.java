package com.example.workspace.workspace.query.response;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;

@Getter
@RequiredArgsConstructor
public class InvitedWorkSpaceSummaryResponse {

    private final String workspaceName;
    private final String inviterName;
    private final LocalDateTime expiresAt;

}
