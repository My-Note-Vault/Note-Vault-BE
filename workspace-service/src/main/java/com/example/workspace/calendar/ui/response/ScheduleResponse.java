package com.example.workspace.calendar.ui.response;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.LocalDate;

@Getter
@RequiredArgsConstructor
public class ScheduleResponse {

    private final LocalDate date;
    private final int startCount;
    private final int endCount;
}
