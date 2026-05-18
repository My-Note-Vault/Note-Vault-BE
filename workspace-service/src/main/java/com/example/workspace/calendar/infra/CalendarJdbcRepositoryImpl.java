package com.example.workspace.calendar.infra;

import com.example.workspace.calendar.service.DailyEventRow;
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
                SELECT 'START' AS type, DATE(t.start_date_time) AS schedule_date, COUNT(*) AS count FROM task t
                WHERE t.author_id = :memberId
                AND t.start_date_time >= :from
                AND t.start_date_time < :to
                AND t.status != 'COMPLETED'
                GROUP BY schedule_date
                
                UNION ALL
                
                SELECT 'END' AS type, DATE(t.end_date_time) AS schedule_date, COUNT(*) AS count FROM task t
                WHERE t.author_id = :memberId
                AND t.end_date_time >= :from
                AND t.end_date_time < :to
                AND t.status != 'COMPLETED'
                GROUP BY schedule_date
                """;

        RowMapper<DailyEventRow> mapper = (rs, rowNum) -> {
            return new DailyEventRow(
                    DailyEventRow.EventType.valueOf(rs.getString("type")),
                    rs.getObject("schedule_date", LocalDate.class),
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
                SELECT 'START' AS type, DATE(st.start_date_time) AS schedule_date, COUNT(*) AS count FROM subtask st
                WHERE st.author_id = :memberId
                AND st.start_date_time >= :from
                AND st.start_date_time < :to
                AND st.status != 'COMPLETED'
                GROUP BY schedule_date
                
                UNION ALL
                
                SELECT 'END' AS type, DATE(st.end_date_time) AS schedule_date, COUNT(*) AS count FROM subtask st
                WHERE st.author_id = :memberId
                AND st.end_date_time >= :from
                AND st.end_date_time < :to
                AND st.status != 'COMPLETED'
                GROUP BY schedule_date
                """;

//        String sql = """
//                SELECT st.start_date_time , st.status, COUNT(*) AS count FROM subtask st
//                WHERE st.author_id = :memberId
//                AND st.start_date_time >= :from
//                AND st.start_date_time < :to
//                GROUP BY st.start_date_time, st.status
//                """;

        RowMapper<DailyEventRow> mapper = (rs, rowNum) -> {
            return new DailyEventRow(
                    DailyEventRow.EventType.valueOf(rs.getString("type")),
                    rs.getObject("schedule_date", LocalDate.class),
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
