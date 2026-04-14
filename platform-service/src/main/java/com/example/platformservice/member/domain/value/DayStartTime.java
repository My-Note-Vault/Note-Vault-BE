package com.example.platformservice.member.domain.value;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalTime;

@Getter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Embeddable
public class DayStartTime {

    public static final DayStartTime MIDNIGHT = new DayStartTime(0, 0);

    @Column(nullable = false)
    private int hour;
    @Column(nullable = false)
    private int minute;

    public DayStartTime(int hour, int minute) {
        this.hour = hour;
        this.minute = minute;
    }

    public static DayStartTime now() {
        LocalTime now = LocalTime.now();
        return new DayStartTime(now.getHour(), now.getMinute());
    }
}
