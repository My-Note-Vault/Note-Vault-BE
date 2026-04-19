package com.example.platformservice.dailynote.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "daily_note_plan",
        uniqueConstraints = @UniqueConstraint(columnNames = {"daily_note_id", "plan_id"}),
        indexes = {
                @Index(name = "idx_daily_note_id",columnList = "daily_note_id"),
                @Index(name = "idx_plan_id",columnList = "plan_id")
        }
)
@Entity
public class DailyNotePlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    private DailyNote dailyNote;

    @ManyToOne(fetch = FetchType.LAZY)
    private Plan plan;

    public DailyNotePlan(final DailyNote dailyNote, final Plan plan) {
        this.dailyNote = dailyNote;
        this.plan = plan;
    }
}
