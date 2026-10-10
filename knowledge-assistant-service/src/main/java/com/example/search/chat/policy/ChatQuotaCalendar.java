package com.example.search.chat.policy;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;

@Component
public class ChatQuotaCalendar {
    private final Clock clock;

    public ChatQuotaCalendar(@Qualifier("chatQuotaClock") Clock clock) {
        this.clock = Objects.requireNonNull(clock);
    }

    public QuotaDay currentDay() {
        return dayAt(clock.instant());
    }

    /** Persist this date when reserving tokens and reuse it when settling, even after midnight. */
    public QuotaDay dayAt(Instant reservationTime) {
        LocalDate date = Objects.requireNonNull(reservationTime).atZone(ChatPolicy.QUOTA_ZONE).toLocalDate();
        return new QuotaDay(date, date.atStartOfDay(ChatPolicy.QUOTA_ZONE).toInstant(),
                date.plusDays(1).atStartOfDay(ChatPolicy.QUOTA_ZONE).toInstant());
    }

    public record QuotaDay(LocalDate date, Instant startsAt, Instant resetsAt) {
    }
}
