package com.example.platformservice.dailynote.infra;

import com.example.platformservice.dailynote.command.domain.DailyNote;

import java.time.LocalDateTime;
import java.util.Optional;

public interface DailyNoteJdbcRepository {


    Optional<DailyNote> findByAuthorIdAndBetweenDates(
            Long authorId,
            LocalDateTime startDateTime,
            LocalDateTime endDateTime
    );
}
