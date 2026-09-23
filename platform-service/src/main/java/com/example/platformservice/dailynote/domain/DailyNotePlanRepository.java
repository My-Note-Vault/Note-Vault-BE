package com.example.platformservice.dailynote.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface DailyNotePlanRepository extends JpaRepository<DailyNotePlan, Long> {
    boolean existsByDailyNote_IdAndPlan_Id(Long dailyNoteId, Long planId);

    void deleteAllByPlan(Plan plan);

    void deleteAllByDailyNote(DailyNote dailyNote);

    @Query("""
            SELECT DISTINCT n.id AS dailyNoteId, n.contentRevision AS contentRevision
            FROM DailyNotePlan link JOIN link.dailyNote n
            WHERE link.plan.id = :planId
            ORDER BY n.id
            """)
    List<SearchTarget> findSearchTargetsByPlanId(@Param("planId") Long planId);

    interface SearchTarget {
        Long getDailyNoteId();
        long getContentRevision();
    }

    @Query("""
SELECT p FROM DailyNotePlan dnp
JOIN Plan p ON p.id = dnp.plan.id
WHERE dnp.dailyNote.id = :dailyNoteId
ORDER BY dnp.id ASC
""")
    List<Plan> findAllPlansByDailyNoteId(Long dailyNoteId);

    @Query("""
SELECT p FROM DailyNotePlan dnp
JOIN Plan p ON p.id = dnp.plan.id
WHERE dnp.dailyNote.id = :dailyNoteId
AND p.isDone = false
ORDER BY dnp.id ASC
""")
    List<Plan> findAllIncompletePlansByDailyNoteId(Long dailyNoteId);
}
