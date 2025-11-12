package com.example.common.api;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class NoteSummaryResponse {

    private final String title;
    private final String content;

}
