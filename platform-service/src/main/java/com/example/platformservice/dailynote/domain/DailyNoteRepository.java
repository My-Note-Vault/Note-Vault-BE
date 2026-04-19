package com.example.platformservice.dailynote.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
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
    AND d.logicalDate >= :logicalDate
    ORDER BY d.logicalDate DESC
    LIMIT 1
""")
    Optional<DailyNote> findLatestDailyNoteAfter(Long authorId, LocalDate logicalDate);

    @Query("""
SELECT d 
FROM DailyNote d
WHERE d.createdAt >= :startDateTime
AND d.createdAt < :endDateTime
AND d.authorId = :authorId
""")
    Optional<DailyNote> findTodayDailyNoteByAuthorId(Long authorId, LocalDateTime startDateTime, LocalDateTime endDateTime);

    Optional<DailyNote> findByIdAndAuthorId(Long id, Long authorId);

    Optional<DailyNote> findByAuthorIdAndLogicalDate(Long authorId, LocalDate logicalDate);
}
