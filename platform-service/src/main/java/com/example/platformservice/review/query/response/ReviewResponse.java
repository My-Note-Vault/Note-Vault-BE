package com.example.platformservice.review.query.response;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;

@Getter
@RequiredArgsConstructor
public class ReviewResponse {

    private final Long reviewId;

    private final Long reviewerId;
    private final String reviewerNickname;
    private final String reviewerImageKey;

    private final String reviewContent;
    private final Integer rating;

    private final LocalDateTime updatedAt;
}
