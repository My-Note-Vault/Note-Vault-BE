package com.example.common.ai;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class AiSummaryRateLimiter {

    private final int dailyLimit;
    private final ZoneId zoneId;

    private LocalDate currentDate;
    private AtomicInteger currentCount;

    public AiSummaryRateLimiter(
            @Value("${ai.summary.daily-limit:499}") final int dailyLimit,
            @Value("${ai.summary.zone-id:Asia/Seoul}") final String zoneId
    ) {
        this.dailyLimit = dailyLimit;
        this.zoneId = ZoneId.of(zoneId);
        this.currentDate = LocalDate.now(this.zoneId);
        this.currentCount = new AtomicInteger(0);
    }

    public synchronized int acquire() {
        LocalDate today = LocalDate.now(zoneId);
        if (!today.equals(currentDate)) {
            currentDate = today;
            currentCount = new AtomicInteger(0);
        }

        int next = currentCount.incrementAndGet();
        if (next > dailyLimit) {
            currentCount.decrementAndGet();
            throw new AiSummaryRateLimitExceededException("AI 요약은 하루 499회까지만 사용할 수 있습니다.");
        }

        return dailyLimit - next;
    }
}
