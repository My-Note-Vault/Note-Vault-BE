package com.example.common.ai;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class AiSummaryService {

    private final OpenAiSummaryClient openAiSummaryClient;
    private final AiSummaryRateLimiter rateLimiter;

    @Value("${ai.summary.max-content-length:24000}")
    private int maxContentLength;

    public AiSummaryResponse summarize(final AiSummaryRequest request) {
        String content = request.getContent();
        if (content == null || content.trim().isEmpty()) {
            throw new IllegalArgumentException("요약할 내용이 없습니다.");
        }

        if (content.length() > maxContentLength) {
            throw new IllegalArgumentException("요약 요청 본문이 너무 깁니다.");
        }

        int remainingToday = rateLimiter.acquire();
        String summary = openAiSummaryClient.summarize(
                request.getTitle(),
                request.getSectionTitle(),
                content
        );
        return new AiSummaryResponse(summary, remainingToday);
    }
}
