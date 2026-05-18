package com.example.workspace.calendar.service;

import com.example.workspace.calendar.infra.CalendarJdbcRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@RequiredArgsConstructor
@Service
public class CalendarService {

    private final CalendarJdbcRepository calendarJdbcRepository;

    @Transactional(readOnly = true)
    public Map<LocalDate, Map<DailyEventRow.EventType, Integer>> findMonthlySchedulesByMemberId(final Long memberId, final int year, final int month) {
        LocalDate from = LocalDate.of(year, month, 1);
        LocalDate to = LocalDate.of(year, month, 1).plusMonths(1);

        List<DailyEventRow> monthlyTaskSchedulesByMember = calendarJdbcRepository.findMonthlyTaskSchedulesByMember(memberId, from, to);
        List<DailyEventRow> monthlySubTaskSchedulesByMember = calendarJdbcRepository.findMonthlySubTaskSchedulesByMember(memberId, from, to);

        List<DailyEventRow> allSchedules = new ArrayList<>(monthlyTaskSchedulesByMember);
        allSchedules.addAll(monthlySubTaskSchedulesByMember);

        Map<LocalDate, Map<DailyEventRow.EventType, Integer>> schedulesByDate = new TreeMap<>();
        for (DailyEventRow schedule : allSchedules) {
            schedulesByDate.computeIfAbsent(schedule.getDate(), ignored -> new EnumMap<>(DailyEventRow.EventType.class))
                    .merge(schedule.getType(), schedule.getCount(), Integer::sum);
        }

        return new LinkedHashMap<>(schedulesByDate);
    }
}
