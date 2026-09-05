package com.example.platformservice.draw.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "daily_draw_result",
        uniqueConstraints = @UniqueConstraint(name = "uk_draw_date_category", columnNames = {"draw_date", "category"}),
        indexes = @Index(name = "idx_draw_date", columnList = "draw_date"))
public class DailyDrawResult {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "draw_date", nullable = false)
    private LocalDate drawDate;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30)
    private DrawCategory category;
    @Column(name = "winner_member_id")
    private Long winnerMemberId;
    @Column(name = "winner_name_snapshot")
    private String winnerNameSnapshot;
    @Column(name = "eligible_count", nullable = false)
    private int eligibleCount;
    @Column(name = "total_member_count", nullable = false)
    private long totalMemberCount;
    @Column(name = "drawn_at", nullable = false)
    private LocalDateTime drawnAt;

    public DailyDrawResult(LocalDate drawDate, DrawCategory category, Long winnerMemberId,
                           String winnerNameSnapshot, int eligibleCount, long totalMemberCount,
                           LocalDateTime drawnAt) {
        this.drawDate = drawDate;
        this.category = category;
        this.winnerMemberId = winnerMemberId;
        this.winnerNameSnapshot = winnerNameSnapshot;
        this.eligibleCount = eligibleCount;
        this.totalMemberCount = totalMemberCount;
        this.drawnAt = drawnAt;
    }
}
