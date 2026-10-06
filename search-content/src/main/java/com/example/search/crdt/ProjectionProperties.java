package com.example.search.crdt;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import java.time.Duration;

@ConfigurationProperties("worker.crdt")
public record ProjectionProperties(
        @DefaultValue("1") int concurrency,
        @DefaultValue("10s") Duration executionTimeout,
        @DefaultValue("30s") Duration startupTimeout,
        @DefaultValue("33554432") long maxInputBytes,
        @DefaultValue("33554432") int maxStateBytes,
        @DefaultValue("4194304") int maxContentCharacters,
        @DefaultValue("100000") int maxUpdates
) {
    public ProjectionProperties {
        if (concurrency < 1 || concurrency > 4 || maxInputBytes < 1 || maxStateBytes < 1
                || maxContentCharacters < 1 || maxUpdates < 1) {
            throw new IllegalArgumentException("Invalid worker CRDT limits");
        }
        for (Duration duration : new Duration[]{executionTimeout, startupTimeout}) {
            if (duration == null || duration.toMillis() < 1) {
                throw new IllegalArgumentException("CRDT durations must be positive");
            }
        }
    }
}
