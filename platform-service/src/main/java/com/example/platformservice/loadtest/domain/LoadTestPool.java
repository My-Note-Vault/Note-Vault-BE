package com.example.platformservice.loadtest.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "load_test_pool")
@Entity
public class LoadTestPool {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "pool_id", nullable = false, unique = true, length = 80)
    private String poolId;

    @Column(name = "user_count", nullable = false)
    private int userCount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private LoadTestPoolStatus status;

    public LoadTestPool(final String poolId) {
        this.poolId = poolId;
        this.status = LoadTestPoolStatus.CREATING;
    }

    public void markReady(final int userCount) {
        this.userCount = userCount;
        this.status = LoadTestPoolStatus.READY;
    }
}
