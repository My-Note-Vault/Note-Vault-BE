package com.example.platformservice.noteinfo.query.response;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class InfoSummaryResponse {

    private final String thumbnailKey;
    private final String title;

    private final long reviewCount;
    private final double averageRating;

    private final long likeCount;

    private final String authorProfileKey;
    private final String authorName;
}
