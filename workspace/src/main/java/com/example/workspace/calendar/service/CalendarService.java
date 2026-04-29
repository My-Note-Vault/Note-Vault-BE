package com.example.workspace.calendar.service;

import com.example.workspace.calendar.infra.CalendarJdbcRepository;
import com.example.workspace.calendar.ui.response.ScheduleResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
public class CalendarService {

    private final CalendarJdbcRepository calendarJdbcRepository;

    @Transactional(readOnly = true)
    public List<ScheduleResponse> findMonthlySchedulesByMemberId(final Long memberId, final int year, final int month) {
        LocalDate from = LocalDate.of(year, month, 1);
        LocalDate to = LocalDate.of(year, month, 1).plusMonths(1);

        List<DailyEventRow> monthlyTaskSchedulesByMember = calendarJdbcRepository.findMonthlyTaskSchedulesByMember(memberId, from, to);
        List<DailyEventRow> monthlySubTaskSchedulesByMember = calendarJdbcRepository.findMonthlySubTaskSchedulesByMember(memberId, from, to);

        List<DailyEventRow> allSchedules = new ArrayList<>(monthlyTaskSchedulesByMember);
        allSchedules.addAll(monthlySubTaskSchedulesByMember);

        Map<LocalDate, ScheduleResponse> map =
                allSchedules.stream().collect(Collectors.toMap(
                        DailyEventRow::getDate,

                        r -> new ScheduleResponse(
                                r.getDate(),
                                r.getStatus() == DailyEventRow.EventStatus.OPEN ? r.getCount() : 0,
                                r.getStatus() == DailyEventRow.EventStatus.CLOSED   ? r.getCount() : 0
                        ),

                        (a, b) -> new ScheduleResponse(
                                a.getDate(),
                                a.getStartCount() + b.getStartCount(),
                                a.getEndCount() + b.getEndCount()
                        )
                ));

        return map.values().stream()
                .sorted(Comparator.comparing(ScheduleResponse::getDate))
                .toList();
    }
}
