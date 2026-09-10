package com.example.platformservice.dailynote.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface DailyNoteRepository extends JpaRepository<DailyNote, Long> {

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
UPDATE DailyNote d
SET d.content = :content,
    d.contentRevision = d.contentRevision + 1,
    d.updatedAt = CURRENT_TIMESTAMP
WHERE d.id = :id
  AND d.authorId = :authorId
  AND d.contentRevision = :expectedRevision
""")
    int updateContentIfRevisionMatches(
            @Param("id") Long id,
            @Param("authorId") Long authorId,
            @Param("content") String content,
            @Param("expectedRevision") long expectedRevision
    );

    List<DailyNote> findAllByAuthorIdOrderByLogicalDateAsc(Long authorId);

    List<DailyNote> findAllByCreatedAt(LocalDateTime createdAt);

    Optional<DailyNote> findFirstByAuthorIdAndCreatedAtBetween(Long authorId, LocalDateTime createdAtAfter, LocalDateTime createdAtBefore);

    @Query("""
SELECT d
    FROM DailyNote d
    WHERE d.authorId = :authorId
    AND d.logicalDate < :logicalDate
    ORDER BY d.logicalDate DESC
    LIMIT 1
""")
    Optional<DailyNote> findLatestDailyNoteBefore(Long authorId, LocalDate logicalDate);

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

    List<DailyNote> findAllByAuthorIdAndFolderId(Long authorId, Long folderId);
}
