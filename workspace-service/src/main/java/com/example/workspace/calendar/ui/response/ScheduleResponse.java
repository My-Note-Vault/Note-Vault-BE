package com.example.workspace.calendar.ui.response;

import com.example.workspace.calendar.service.DailyEventRow;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.LocalDate;

@Getter
@RequiredArgsConstructor
public class ScheduleResponse {

    private final DailyEventRow.EventType type;
    private final LocalDate date;
    private final int count;
}
