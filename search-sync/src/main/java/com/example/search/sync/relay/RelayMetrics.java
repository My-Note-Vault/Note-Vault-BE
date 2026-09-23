package com.example.search.sync.relay;

import com.example.search.sync.outbox.SearchSyncOutboxRepository;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.springframework.beans.factory.ObjectProvider;

public class RelayMetrics {
    private final MeterRegistry registry;
    private volatile Snapshot snapshot = new Snapshot(Double.NaN, Double.NaN, Double.NaN);
    private volatile long sampledAt;

    public RelayMetrics(ObjectProvider<MeterRegistry> registries) {
        registry = registries.getIfAvailable(SimpleMeterRegistry::new);
        Gauge.builder("search.sync.relay.backlog", this, metrics -> metrics.snapshot.pending())
                .tag("status", "pending").register(registry);
        Gauge.builder("search.sync.relay.backlog", this, metrics -> metrics.snapshot.processing())
                .tag("status", "processing").register(registry);
        Gauge.builder("search.sync.relay.oldest.age", this, metrics -> metrics.snapshot.oldestSeconds())
                .baseUnit("seconds").register(registry);
        Gauge.builder("search.sync.relay.sample.age", this, RelayMetrics::sampleAgeSeconds)
                .baseUnit("seconds").register(registry);
    }

    public void record(String result, long count) {
        registry.counter("search.sync.relay.events", "result", result).increment(count);
    }

    public void error(String operation) {
        registry.counter("search.sync.relay.errors", "operation", operation).increment();
    }

    public void update(SearchSyncOutboxRepository.Backlog backlog) {
        snapshot = new Snapshot(backlog.getPending(), backlog.getProcessing(), backlog.getOldestSeconds());
        sampledAt = System.nanoTime();
    }

    private double sampleAgeSeconds() {
        return sampledAt == 0 ? Double.NaN : (System.nanoTime() - sampledAt) / 1_000_000_000.0;
    }

    private record Snapshot(double pending, double processing, double oldestSeconds) {
    }
}
