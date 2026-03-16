package com.example.platformservice.dailynote.command.domain;

import com.example.platformservice.dailynote.query.TomorrowTodo;

import java.time.LocalDateTime;
import java.util.Optional;

public interface DailyNoteJdbcRepository {


    Optional<DailyNote> findByMemberIdAndBetweenDates(
            Long memberId,
            LocalDateTime startDateTime,
            LocalDateTime endDateTime
    );
}
