package com.example.platformservice.draw.application;

import com.example.platformservice.draw.domain.DrawCategory;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record DrawOverviewResponse(List<EligibleCount> eligibleCounts, List<DrawDay> days) {
    public record EligibleCount(DrawCategory category, int count, boolean participating) {}
    public record DrawDay(LocalDate drawDate, LocalDateTime drawnAt, List<DrawResult> results) {}
    public record DrawResult(DrawCategory category, Long winnerMemberId, String winnerName, int eligibleCount) {}
}
