package com.example.workspace.calendar.ui;

import com.example.common.AuthMemberId;
import com.example.workspace.calendar.service.CalendarService;
import com.example.workspace.calendar.service.DailyEventRow;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

@RequiredArgsConstructor
@RequestMapping("/api/v1/calendar")
@RestController
public class CalendarController {

    private final CalendarService calendarService;


    //         api/v1/calendar/schedules?year=2026&month=4
    @GetMapping("/schedules")
    public ResponseEntity<Map<LocalDate, Map<DailyEventRow.EventType, Integer>>> getAllCalendars(
            @RequestParam("year") int year,
            @RequestParam("month") int month,
            @AuthMemberId Long memberId
    ) {
        Map<LocalDate, Map<DailyEventRow.EventType, Integer>> responses = calendarService.findMonthlySchedulesByMemberId(memberId, year, month);
        return ResponseEntity.ok(responses);
    }
}
