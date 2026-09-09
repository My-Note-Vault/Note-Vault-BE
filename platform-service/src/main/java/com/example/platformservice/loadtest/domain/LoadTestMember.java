package com.example.platformservice.loadtest.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "load_test_member",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_load_test_member_pool_sequence", columnNames = {"pool_id", "sequence_number"}),
                @UniqueConstraint(name = "uk_load_test_member_member", columnNames = "member_id")
        },
        indexes = @Index(name = "idx_load_test_member_pool", columnList = "pool_id")
)
@Entity
public class LoadTestMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "pool_id", nullable = false, length = 80)
    private String poolId;

    @Column(name = "sequence_number", nullable = false)
    private int sequenceNumber;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(name = "daily_note_id", nullable = false)
    private Long dailyNoteId;

    public LoadTestMember(final String poolId, final int sequenceNumber, final Long memberId, final Long dailyNoteId) {
        this.poolId = poolId;
        this.sequenceNumber = sequenceNumber;
        this.memberId = memberId;
        this.dailyNoteId = dailyNoteId;
    }
}
