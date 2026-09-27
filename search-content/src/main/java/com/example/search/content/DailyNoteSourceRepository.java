package com.example.search.content;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface DailyNoteSourceRepository extends Repository<DailyNoteSource, Long> {
    Optional<DailyNoteSource> findById(Long id);

    @Query(value = "SELECT * FROM daily_note WHERE id = :id FOR UPDATE", nativeQuery = true)
    Optional<DailyNoteSource> lockById(@Param("id") Long id);

    @Query(value = """
            SELECT p.content FROM daily_note_plan dnp JOIN plan p ON p.id = dnp.plan_id
            WHERE dnp.daily_note_id = :id ORDER BY p.id
            """, nativeQuery = true)
    List<String> findPlanContents(@Param("id") Long id);

    // Follow API deletion order: note (if applicable), plans, then links.
    // Acquire plans separately so the database cannot lock a link before its plan.
    @Query(value = """
            SELECT p.id FROM daily_note_plan dnp JOIN plan p ON p.id = dnp.plan_id
            WHERE dnp.daily_note_id = :id ORDER BY p.id FOR SHARE OF p
            """, nativeQuery = true)
    List<Long> lockPlans(@Param("id") Long id);

    // Lock empty plans too: an empty plan can acquire content during embedding.
    // Called after locking the note and plans; the note lock excludes new FK links.
    @Query(value = """
            SELECT p.content FROM daily_note_plan dnp JOIN plan p ON p.id = dnp.plan_id
            WHERE dnp.daily_note_id = :id ORDER BY p.id FOR SHARE OF dnp
            """, nativeQuery = true)
    List<String> lockPlanContents(@Param("id") Long id);

    @Query(value = "SELECT id FROM daily_note WHERE id > :after ORDER BY id LIMIT 256", nativeQuery = true)
    List<Long> findNextIds(@Param("after") long after);
}
