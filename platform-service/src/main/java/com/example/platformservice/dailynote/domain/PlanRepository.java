package com.example.platformservice.dailynote.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;

public interface PlanRepository extends JpaRepository<Plan, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Plan p where p.id = :id")
    Optional<Plan> findWithWriteLockById(@Param("id") Long id);

    // Keep plans stable until the new note links and its REFRESH event commit.
    // Lock only plans, in ID order, to coordinate with editPlan/deletePlan.
    @Query(value = """
            SELECT p.* FROM plan p
            JOIN daily_note_plan link ON link.plan_id = p.id
            WHERE link.daily_note_id = :dailyNoteId AND p.is_done = false
            ORDER BY p.id
            FOR SHARE OF p
            """, nativeQuery = true)
    List<Plan> findIncompleteForCarryOver(@Param("dailyNoteId") Long dailyNoteId);
}
