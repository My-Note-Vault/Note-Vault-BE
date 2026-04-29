package com.example.workspace.calendar.ui;

import com.example.common.AuthMemberId;
import com.example.workspace.calendar.service.CalendarService;
import com.example.workspace.calendar.ui.response.ScheduleResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequiredArgsConstructor
@RequestMapping("/api/v1/calendar")
@RestController
public class CalendarController {

    private final CalendarService calendarService;


    //         api/v1/calendar/schedules?year=2026&month=4
    @GetMapping("/schedules")
    public ResponseEntity<List<ScheduleResponse>> getAllCalendars(
            @RequestParam("year") int year,
            @RequestParam("month") int month,
            @AuthMemberId Long memberId
    ) {
        List<ScheduleResponse> responses = calendarService.findMonthlySchedulesByMemberId(memberId, year, month);
        return ResponseEntity.ok(responses);
    }
}
