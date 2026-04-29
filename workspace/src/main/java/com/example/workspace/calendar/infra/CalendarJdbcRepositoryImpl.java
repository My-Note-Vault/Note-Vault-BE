package com.example.workspace.calendar.infra;

import com.example.workspace.calendar.service.DailyEventRow;
import com.example.workspace.task.command.domain.value.Status;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
@Repository
public class CalendarJdbcRepositoryImpl implements CalendarJdbcRepository {

    private final NamedParameterJdbcTemplate namedJdbcTemplate;

    @Override
    public List<DailyEventRow> findMonthlyTaskSchedulesByMember(Long memberId, LocalDate from, LocalDate to) {

        String sql = """
                SELECT t.start_date_time, t.status, COUNT(*) AS count FROM task t
                WHERE t.author_id = :memberId
                AND t.start_date_time >= :from
                AND t.start_date_time < :to
                GROUP BY t.start_date_time,  t.status
                """;

        RowMapper<DailyEventRow> mapper = (rs, rowNum) -> {
            Status status = rs.getObject("t.status", Status.class);

            DailyEventRow.EventStatus eventStatus = status.equals(Status.COMPLETED)
                    ? DailyEventRow.EventStatus.CLOSED
                    : DailyEventRow.EventStatus.OPEN;

            return new DailyEventRow(
                    rs.getObject("t.start_date_time", LocalDate.class),
                    eventStatus,
                    rs.getInt("count")
            );
        };
        Map<String, Object> params = Map.of(
                "memberId", memberId,
                "from", from,
                "to", to
        );
        return namedJdbcTemplate.query(sql, params, mapper);
    }

    @Override
    public List<DailyEventRow> findMonthlySubTaskSchedulesByMember(Long memberId, LocalDate from, LocalDate to) {

        String sql = """
                SELECT st.start_date_time , st.status, COUNT(*) AS count FROM subtask st
                WHERE st.author_id = :memberId
                AND st.start_date_time >= :from
                AND st.start_date_time < :to
                GROUP BY st.start_date_time, st.status
                """;

        RowMapper<DailyEventRow> mapper = (rs, rowNum) -> {
            Status status = rs.getObject("st.status", Status.class);

            DailyEventRow.EventStatus eventStatus = status.equals(Status.COMPLETED)
                    ? DailyEventRow.EventStatus.CLOSED
                    : DailyEventRow.EventStatus.OPEN;

            return new DailyEventRow(
                    rs.getObject("st.start_date_time", LocalDate.class),
                    eventStatus,
                    rs.getInt("count")
            );
        };

        Map<String, Object> params = Map.of(
                "memberId", memberId,
                "from", from,
                "to", to
        );
        return namedJdbcTemplate.query(sql, params, mapper);
    }

}
