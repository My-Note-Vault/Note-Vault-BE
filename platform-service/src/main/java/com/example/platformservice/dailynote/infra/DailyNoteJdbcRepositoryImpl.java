package com.example.platformservice.dailynote.infra;

import com.example.platformservice.dailynote.command.domain.DailyNote;
import com.example.platformservice.dailynote.command.domain.DailyNoteJdbcRepository;
import com.example.platformservice.dailynote.query.TomorrowTodo;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@RequiredArgsConstructor
@Repository
public class DailyNoteJdbcRepositoryImpl implements DailyNoteJdbcRepository {

    private JdbcTemplate jdbcTemplate;

    @Override
    public Optional<DailyNote> findByMemberIdAndBetweenDates(
            final Long memberId,
            final LocalDateTime startDateTime,
            final LocalDateTime endDateTime
    ) {
        String sql = """
                SELECT * FROM daily_note
                WHERE member_id = ?
                AND created_time BETWEEN ? AND ?
                """;

        RowMapper<DailyNote> rowMapper = (rs, rowNum) -> new DailyNote(
                rs.getLong("author_id"),
                rs.getString("today_todo"),
                rs.getString("tomorrow_todo"),
                rs.getString("memo"),
                rs.getBoolean("isCollapsed")
        );

        DailyNote dailyNote = jdbcTemplate.queryForObject(sql, rowMapper, memberId, startDateTime, endDateTime);
        if (dailyNote == null)
            return Optional.empty();

        return Optional.of(dailyNote);
    }
}
