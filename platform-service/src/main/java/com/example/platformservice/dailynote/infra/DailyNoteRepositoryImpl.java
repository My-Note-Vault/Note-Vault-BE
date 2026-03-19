package com.example.platformservice.dailynote.infra;

import com.example.platformservice.dailynote.command.domain.DailyNote;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@RequiredArgsConstructor
@Repository
public class DailyNoteRepositoryImpl implements DailyNoteJdbcRepository {

    private JdbcTemplate jdbcTemplate;

    @Override
    public Optional<DailyNote> findByAuthorIdAndBetweenDates(
            final Long authorId,
            final LocalDateTime startDateTime,
            final LocalDateTime endDateTime
    ) {
        String sql = """
                SELECT * FROM daily_note
                WHERE author_id = ?
                AND created_time BETWEEN ? AND ?
                """;

        RowMapper<DailyNote> rowMapper = (rs, rowNum) -> new DailyNote(
                rs.getLong("author_id"),
                rs.getString("today_todo"),
                rs.getString("tomorrow_todo"),
                rs.getString("memo"),
                rs.getBoolean("isCollapsed")
        );

        DailyNote dailyNote = jdbcTemplate.queryForObject(sql, rowMapper, authorId, startDateTime, endDateTime);
        if (dailyNote == null)
            return Optional.empty();

        return Optional.of(dailyNote);
    }
}
