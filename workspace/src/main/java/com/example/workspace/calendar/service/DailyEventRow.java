package com.example.workspace.calendar.service;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.LocalDate;

@Getter
@RequiredArgsConstructor
public class DailyEventRow {

    private final LocalDate date;
    private final EventStatus status;
    private final int count;

    public enum EventStatus {
        OPEN, CLOSED
    }
}
