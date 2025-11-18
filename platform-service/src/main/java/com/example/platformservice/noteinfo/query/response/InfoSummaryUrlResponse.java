package com.example.platformservice.noteinfo.query.response;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class InfoSummaryUrlResponse {

    private final String thumbnailUrl;
    private final String authorProfileUrl;

}
