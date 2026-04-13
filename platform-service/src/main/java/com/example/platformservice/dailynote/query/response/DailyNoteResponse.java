package com.example.platformservice.dailynote.query.response;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.LocalDate;

@Getter
@RequiredArgsConstructor
public class DailyNoteResponse {

    private final Long dailyNoteId;

    private final String todayTodo;
    private final String tomorrowTodo;

    private final String memo;

    // LogicalDate..
    private final LocalDate todayDate;

}
