package com.example.platformservice.review.command.application.request;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

@Getter
@RequiredArgsConstructor
public class ReviewerImageKeysRequest {

    private final List<String> reviewerImageKeys;
}
