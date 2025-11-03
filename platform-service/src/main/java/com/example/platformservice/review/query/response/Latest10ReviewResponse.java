package com.example.platformservice.review.query.response;

import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

@Getter
@Builder
@RequiredArgsConstructor
public class Latest10ReviewResponse {

    private final long totalReviewCount;
    private final double averageRating;
    private final List<ReviewResponse> reviewResponses;
}
