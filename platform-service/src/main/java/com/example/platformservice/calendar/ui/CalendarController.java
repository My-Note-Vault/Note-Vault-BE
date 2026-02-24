package com.example.platformservice.calendar.ui;

import com.example.platformservice.calendar.service.CalendarService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RequestMapping("/api/v1/calendar")
@RestController
public class CalendarController {

    private final CalendarService calendarService;

    @PostMapping("/{year}")
    public ResponseEntity<Void> addHolidayInfo(@PathVariable int year) {
        calendarService.insertAllHolidayInfo(year);
        return ResponseEntity.noContent().build();
    }
}
