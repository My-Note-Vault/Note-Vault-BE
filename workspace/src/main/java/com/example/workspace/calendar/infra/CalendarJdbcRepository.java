package com.example.workspace.calendar.infra;

import com.example.workspace.calendar.service.DailyEventRow;

import java.time.LocalDate;
import java.util.List;

public interface CalendarJdbcRepository {

    List<DailyEventRow> findMonthlyTaskSchedulesByMember(Long memberId, LocalDate from, LocalDate to);

    List<DailyEventRow> findMonthlySubTaskSchedulesByMember(Long memberId, LocalDate from, LocalDate to);
}
