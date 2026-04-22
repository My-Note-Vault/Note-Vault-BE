package com.example.platformservice.dailynote.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface DailyNotePlanRepository extends JpaRepository<DailyNotePlan, Long> {
    void deleteAllByPlan(Plan plan);

    void deleteAllByDailyNote(DailyNote dailyNote);

    // dailyNoteId = :dailyNoteId && isDone = false
    @Query("""
SELECT p FROM DailyNotePlan dnp
JOIN Plan p ON p.id = dnp.plan.id
WHERE dnp.dailyNote.id = :dailyNoteId
""")
    List<Plan> findAllPlansByDailyNoteId(Long dailyNoteId);
}
