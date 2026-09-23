package com.example.search.sync.relay;

import com.example.search.sync.outbox.OutboxMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;

import java.util.List;

@Slf4j
@RequiredArgsConstructor
public class OutboxRelay {
    private static final int MAX_BATCHES_PER_POLL = 5;

    private final OutboxTransactions transactions;
    private final SqsEventPublisher publisher;
    private final RelayMetrics metrics;
    private volatile boolean stopping;

    @Scheduled(fixedDelayString = "${search.sync.relay.poll-delay:PT1S}", scheduler = "searchSyncRelayScheduler")
    public void poll() {
        runSafely("poll", this::publishPending);
    }

    private void publishPending() {
        metrics.record("recovered", transactions.recoverExpired());
        for (int batch = 0; batch < MAX_BATCHES_PER_POLL && !stopping; batch++) {
            List<OutboxMessage> messages = transactions.claimBatch();
            if (messages.isEmpty() || stopping) return;
            metrics.record("claimed", messages.size());

            // The claim has committed. No DB transaction spans this network request.
            SqsEventPublisher.Publication result = publisher.publish(messages);
            int published = transactions.markPublished(result.successful());
            metrics.record("published", published);
            int retried = transactions.markFailed(result.failed());
            metrics.record("retried", retried);
            metrics.record("lost_lease", messages.size() - published - retried);
            if (!result.failed().isEmpty()) {
                log.warn("SQS publication failed for {} events; first eventId={}; reason={}",
                        result.failed().size(), result.failed().getFirst().message().eventId(),
                        result.failed().getFirst().reason());
            }
        }
    }

    @Scheduled(fixedDelay = 30_000, scheduler = "searchSyncRelayScheduler")
    public void sampleBacklog() {
        runSafely("metrics", () -> metrics.update(transactions.backlog()));
    }

    @Scheduled(initialDelay = 3_600_000, fixedDelay = 3_600_000, scheduler = "searchSyncRelayScheduler")
    public void cleanup() {
        runSafely("cleanup", () -> metrics.record("cleaned", transactions.deletePublished()));
    }

    @EventListener(ContextClosedEvent.class)
    public void stopClaiming() {
        stopping = true;
    }

    private void runSafely(String operation, Runnable action) {
        if (stopping) return;
        try {
            action.run();
        } catch (RuntimeException exception) {
            // Unrecorded deliveries stay leased and are retried after lease expiry.
            metrics.error(operation);
            log.error("Outbox relay {} failed", operation, exception);
        }
    }
}
