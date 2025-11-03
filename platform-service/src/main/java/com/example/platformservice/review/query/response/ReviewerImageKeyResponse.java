package com.example.platformservice.review.query.response;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

@Getter
@RequiredArgsConstructor
public class ReviewerImageKeyResponse {

    private final List<String> reviewerImageUrls;

}
