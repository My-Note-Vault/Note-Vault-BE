package com.example.platformservice.noteinfo.command.application.request;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class InfoSummaryKeyRequest {

    private final String thumbnailKey;
    private final String authorProfileKey;
}
