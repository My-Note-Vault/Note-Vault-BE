package com.example.workspace.calendar.infra;

import com.example.workspace.calendar.service.DailyEventRow;
import com.example.workspace.calendar.ui.response.DateEventResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
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
                SELECT 'START' AS type, DATE(t.start_date_time) AS schedule_date, COUNT(*) AS count FROM document t
                WHERE t.author_id = :memberId
                AND t.type = 'TASK'
                AND t.start_date_time >= :from
                AND t.start_date_time < :to
                AND t.status != 'COMPLETED'
                GROUP BY schedule_date
                
                UNION ALL
                
                SELECT 'END' AS type, DATE(t.end_date_time) AS schedule_date, COUNT(*) AS count FROM document t
                WHERE t.author_id = :memberId
                AND t.type = 'TASK'
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
                SELECT 'START' AS type, DATE(st.start_date_time) AS schedule_date, COUNT(*) AS count FROM document st
                WHERE st.author_id = :memberId
                AND st.type = 'SUBTASK'
                AND st.start_date_time >= :from
                AND st.start_date_time < :to
                AND st.status != 'COMPLETED'
                GROUP BY schedule_date
                
                UNION ALL
                
                SELECT 'END' AS type, DATE(st.end_date_time) AS schedule_date, COUNT(*) AS count FROM document st
                WHERE st.author_id = :memberId
                AND st.type = 'SUBTASK'
                AND st.end_date_time >= :from
                AND st.end_date_time < :to
                AND st.status != 'COMPLETED'
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
    public List<DateEventResponse> findTasksByDate(Long memberId, LocalDate targetDate) {
        String sql = """
        SELECT
            t.id,
            t.title,
            t.status,
            DATE(t.start_date_time) AS start_date,
            DATE(t.end_date_time) AS end_date
        FROM document t
        WHERE (
            DATE(t.start_date_time) = :targetDate OR
            DATE(t.end_date_time) = :targetDate
        )
        AND t.author_id = :memberId
        AND t.type = 'TASK'
        """;

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("targetDate", targetDate)
                .addValue("memberId", memberId);

        return namedJdbcTemplate.query(
                sql,
                params,
                (rs, rowNum) -> new DateEventResponse(
                        "TASK",
                        rs.getLong("id"),
                        rs.getString("title"),
                        rs.getString("status"),
                        rs.getObject("start_date", LocalDate.class),
                        rs.getObject("end_date", LocalDate.class),
                        null
                )
        );
    }

    @Override
    public List<DateEventResponse> findSubTasksByDate(Long memberId, LocalDate targetDate) {
        String sql = """
        SELECT
            st.id,
            st.title,
            st.status,
            DATE(st.start_date_time) AS start_date,
            DATE(st.end_date_time) AS end_date,
            t.id AS parent_task_id,
            t.title AS parent_task_title,
            t.status AS parent_task_status,
            DATE(t.start_date_time) AS parent_task_start_date,
            DATE(t.end_date_time) AS parent_task_end_date
        FROM document st
        JOIN document t ON t.id = st.parent_id AND t.type = 'TASK'
        WHERE (
            DATE(st.start_date_time) = :targetDate OR
            DATE(st.end_date_time) = :targetDate
        )
        AND st.author_id = :memberId
        AND st.type = 'SUBTASK'
        """;

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("targetDate", targetDate)
                .addValue("memberId", memberId);

        return namedJdbcTemplate.query(
                sql,
                params,
                (rs, rowNum) -> new DateEventResponse(
                        "SUBTASK",
                        rs.getLong("id"),
                        rs.getString("title"),
                        rs.getString("status"),
                        rs.getObject("start_date", LocalDate.class),
                        rs.getObject("end_date", LocalDate.class),
                        new DateEventResponse.Parent(
                                rs.getLong("parent_task_id"),
                                rs.getString("parent_task_title"),
                                rs.getString("parent_task_status"),
                                rs.getObject("parent_task_start_date", LocalDate.class),
                                rs.getObject("parent_task_end_date", LocalDate.class)
                        )
                )
        );
    }

}
