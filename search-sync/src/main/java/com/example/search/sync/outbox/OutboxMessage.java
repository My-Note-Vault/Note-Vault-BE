package com.example.search.sync.outbox;

import java.util.UUID;

/** Immutable work handed to the publisher after the claim transaction commits. */
public record OutboxMessage(UUID eventId, String payload, UUID leaseToken) {
    public record Failure(OutboxMessage message, String reason) {
    }
}
