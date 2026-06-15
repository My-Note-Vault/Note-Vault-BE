package com.example.common.ai;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class AiSummaryResponse {

    private final String summary;
    private final int remainingToday;
}
