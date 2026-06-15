package com.example.common.ai;

public class AiSummaryRateLimitExceededException extends RuntimeException {

    public AiSummaryRateLimitExceededException(final String message) {
        super(message);
    }
}
