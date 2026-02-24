package com.example.platformservice.calendar.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "calendar",
        indexes = @Index(columnList = "memberId")
)
@Entity
public class Calendar {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private LocalDate date;

    @Column(nullable = false)
    private Boolean isHoliday;

    private String holidayName;

    public Calendar(
            String holidayName,
            String date,
            String isHoliday
    ) {
        this.holidayName = holidayName;
        this.date = LocalDate.parse(date, DateTimeFormatter.BASIC_ISO_DATE);
        this.isHoliday = "Y".equalsIgnoreCase(isHoliday);
    }
}
