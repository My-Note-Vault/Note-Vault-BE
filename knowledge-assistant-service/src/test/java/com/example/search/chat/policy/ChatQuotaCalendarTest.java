package com.example.search.chat.policy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class ChatQuotaCalendarTest {

    @ParameterizedTest
    @DisplayName("한국 시간 자정과 월말·연말·윤일 경계에서 한도 날짜와 초기화 시각을 계산한다")
    @CsvSource({
            "2026-10-08T14:59:59.999999999Z, 2026-10-08, 2026-10-07T15:00:00Z, 2026-10-08T15:00:00Z",
            "2026-10-08T15:00:00Z,           2026-10-09, 2026-10-08T15:00:00Z, 2026-10-09T15:00:00Z",
            "2026-10-08T15:00:00.000000001Z, 2026-10-09, 2026-10-08T15:00:00Z, 2026-10-09T15:00:00Z",
            "2026-10-31T15:00:00Z,           2026-11-01, 2026-10-31T15:00:00Z, 2026-11-01T15:00:00Z",
            "2026-12-31T15:00:00Z,           2027-01-01, 2026-12-31T15:00:00Z, 2027-01-01T15:00:00Z",
            "2028-02-28T15:00:00Z,           2028-02-29, 2028-02-28T15:00:00Z, 2028-02-29T15:00:00Z",
            "2028-02-29T15:00:00Z,           2028-03-01, 2028-02-29T15:00:00Z, 2028-03-01T15:00:00Z"
    })
    void calculatesKoreanCalendarBoundaries(String now, String date, String startsAt, String resetsAt) {
        ChatQuotaCalendar calendar = new ChatQuotaCalendar(Clock.fixed(Instant.parse(now), ZoneOffset.UTC));

        ChatQuotaCalendar.QuotaDay day = calendar.currentDay();

        assertThat(day.date()).isEqualTo(LocalDate.parse(date));
        assertThat(day.startsAt()).isEqualTo(Instant.parse(startsAt));
        assertThat(day.resetsAt()).isEqualTo(Instant.parse(resetsAt));
    }

    @ParameterizedTest
    @ValueSource(strings = {"UTC", "America/Los_Angeles", "Pacific/Kiritimati"})
    @DisplayName("주입한 Clock의 시간대와 무관하게 한국 시간으로 날짜를 계산한다")
    void ignoresClockTimeZone(String zone) {
        Clock clock = Clock.fixed(Instant.parse("2026-10-08T15:00:00Z"), ZoneId.of(zone));

        ChatQuotaCalendar.QuotaDay day = new ChatQuotaCalendar(clock).currentDay();

        assertThat(day.date()).isEqualTo(LocalDate.of(2026, 10, 9));
        assertThat(day.resetsAt()).isEqualTo(Instant.parse("2026-10-09T15:00:00Z"));
    }

    @Test
    @DisplayName("자정 이후에도 과거 예약 시각의 날짜를 현재 날짜로 바꾸지 않는다")
    void resolvesReservationDateIndependentlyOfCurrentTime() {
        ChatQuotaCalendar calendar = new ChatQuotaCalendar(
                Clock.fixed(Instant.parse("2026-10-09T01:00:00Z"), ZoneOffset.UTC));

        ChatQuotaCalendar.QuotaDay reservationDay = calendar.dayAt(Instant.parse("2026-10-08T14:59:59Z"));

        assertThat(calendar.currentDay().date()).isEqualTo(LocalDate.of(2026, 10, 9));
        assertThat(reservationDay.date()).isEqualTo(LocalDate.of(2026, 10, 8));
        assertThat(reservationDay.resetsAt()).isEqualTo(Instant.parse("2026-10-08T15:00:00Z"));
    }
}
