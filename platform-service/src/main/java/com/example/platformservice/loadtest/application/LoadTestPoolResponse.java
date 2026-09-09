package com.example.platformservice.loadtest.application;

import com.example.platformservice.loadtest.domain.LoadTestPool;
import com.example.platformservice.loadtest.domain.LoadTestPoolStatus;

public record LoadTestPoolResponse(String poolId, int userCount, LoadTestPoolStatus status, boolean created) {
    public static LoadTestPoolResponse created(final LoadTestPool pool) {
        return new LoadTestPoolResponse(pool.getPoolId(), pool.getUserCount(), pool.getStatus(), true);
    }

    public static LoadTestPoolResponse existing(final LoadTestPool pool) {
        return new LoadTestPoolResponse(pool.getPoolId(), pool.getUserCount(), pool.getStatus(), false);
    }
}
