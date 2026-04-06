package com.example.platformservice.dailynote.command.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface DailyNoteRepository extends JpaRepository<DailyNote, Long> {

    List<DailyNote> findAllByAuthorId(Long authorId);

    List<DailyNote> findAllByCreatedAt(LocalDateTime createdAt);

    Optional<DailyNote> findFirstByAuthorIdAndCreatedAtBetween(Long authorId, LocalDateTime createdAtAfter, LocalDateTime createdAtBefore);

    @Query("""
SELECT d
    FROM DailyNote d
    WHERE d.authorId = :authorId
    AND d.createdAt >= :criteriaDateTime
    ORDER BY d.createdAt DESC
    LIMIT 1
""")
    Optional<String> findLatestTomorrowTodoWithin3days(Long authorId, LocalDateTime criteriaDateTime);
}
