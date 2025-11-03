package com.example.platformservice.review.command.application.request;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class DeleteReviewRequest {

    private final Long reviewId;
}
