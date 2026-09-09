package com.example.platformservice.loadtest.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LoadTestPoolRepository extends JpaRepository<LoadTestPool, Long> {
    Optional<LoadTestPool> findByPoolId(String poolId);
}
