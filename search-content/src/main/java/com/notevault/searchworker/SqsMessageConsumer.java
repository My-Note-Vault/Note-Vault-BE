package com.notevault.searchworker;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.DeserializationFeature;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.SmartLifecycle;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.MessageSystemAttributeName;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
public class SqsMessageConsumer implements SmartLifecycle {
    private final SqsClient client;
    private final WorkerSqsProperties properties;
    private final ObjectReader reader;
    private final WorkerMessageDispatcher dispatcher;
    private final ScheduledExecutorService heartbeat;
    private volatile boolean running;
    private ExecutorService workers;

    public SqsMessageConsumer(SqsClient client, WorkerSqsProperties properties, ObjectMapper mapper,
                             WorkerMessageDispatcher dispatcher, ScheduledExecutorService heartbeat) {
        this.client = client;
        this.properties = properties;
        this.reader = mapper.readerFor(SearchSyncMessage.class)
                .with(DeserializationFeature.FAIL_ON_NUMBERS_FOR_ENUMS, DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
                .without(DeserializationFeature.ACCEPT_FLOAT_AS_INT);
        this.dispatcher = dispatcher;
        this.heartbeat = heartbeat;
    }

    @Override
    public synchronized void start() {
        if (running) return;
        running = true;
        workers = Executors.newFixedThreadPool(properties.concurrency(),
                Thread.ofPlatform().name("sqs-consumer-", 0).factory());
        for (int i = 0; i < properties.concurrency(); i++) workers.execute(this::consume);
        log.info("SQS worker started: region={}, concurrency={}", properties.region(), properties.concurrency());
    }

    private void consume() {
        while (running && !Thread.currentThread().isInterrupted()) {
            try {
                var response = client.receiveMessage(request -> request.queueUrl(properties.queueUrl())
                        .waitTimeSeconds(20).maxNumberOfMessages(1)
                        .visibilityTimeout(MessageVisibility.TIMEOUT_SECONDS)
                        .messageSystemAttributeNames(MessageSystemAttributeName.APPROXIMATE_RECEIVE_COUNT));
                for (Message message : response.messages()) {
                    if (!running) return; // Unstarted deliveries become visible again without being deleted.
                    process(message);
                }
            } catch (RuntimeException failure) {
                if (!running) return;
                log.error("SQS receive failed; retrying in 5 seconds", failure);
                try {
                    Thread.sleep(5_000);
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }
    }

    private void process(Message message) {
        SearchSyncMessage event = null;
        int receiveCount = receiveCount(message);
        long started = System.nanoTime();
        try (MessageVisibility visibility = new MessageVisibility(client, properties.queueUrl(), message, heartbeat)) {
            try {
                event = reader.readValue(message.body());
                if (event == null) throw new IllegalArgumentException("Search sync message must be a JSON object");
                String result = dispatcher.dispatch(event);
                visibility.beforeDelete();
                client.deleteMessage(request -> request.queueUrl(properties.queueUrl())
                        .receiptHandle(message.receiptHandle()));
                log.info("Worker completed: eventId={}, messageType={}, sourceType={}, sourceId={}, result={}, receiveCount={}, durationMs={}",
                        event.eventId(), event.messageType(), event.sourceType(), event.sourceId(), result, receiveCount,
                        TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started));
            } catch (Exception failure) {
                try {
                    visibility.retry(receiveCount);
                } catch (RuntimeException retryFailure) {
                    failure.addSuppressed(retryFailure);
                }
                // Never acknowledge failures, including malformed contracts and failed DeleteMessage calls.
                log.warn("Worker failed: messageId={}, eventId={}, messageType={}, sourceType={}, sourceId={}, receiveCount={}, error={}",
                        message.messageId(), event == null ? null : event.eventId(),
                        event == null ? null : event.messageType(),
                        event == null ? null : event.sourceType(), event == null ? null : event.sourceId(),
                        receiveCount, safeReason(failure));
            }
        } finally {
            // A lost visibility lease cancels this delivery, not the worker's next receive loop.
            if (running) Thread.interrupted();
        }
    }

    // Parser/guest exceptions can contain document content or the original message body.
    private String safeReason(Exception failure) {
        for (Throwable cause = failure; cause != null; cause = cause.getCause()) {
            if (cause instanceof com.example.search.crdt.YjsProjectionException yjs) {
                return "YJS_" + yjs.reason();
            }
        }
        return failure.getClass().getSimpleName();
    }

    private int receiveCount(Message message) {
        try {
            return Math.max(1, Integer.parseInt(message.attributes()
                    .getOrDefault(MessageSystemAttributeName.APPROXIMATE_RECEIVE_COUNT, "1")));
        } catch (NumberFormatException invalid) {
            return 1;
        }
    }

    @Override
    public void stop(Runnable callback) {
        running = false;
        Thread.ofPlatform().name("sqs-consumer-shutdown").start(() -> {
            try {
                stop();
            } finally {
                callback.run();
            }
        });
    }

    @Override
    public void stop() {
        running = false;
        if (workers == null) return;
        workers.shutdown();
        try {
            if (!workers.awaitTermination(90, TimeUnit.SECONDS)) workers.shutdownNow();
        } catch (InterruptedException interrupted) {
            workers.shutdownNow();
            Thread.currentThread().interrupt();
        }
        log.info("SQS worker stopped; unfinished messages remain in SQS");
    }

    @Override
    public boolean isRunning() { return running; }

    @Override
    public int getPhase() { return Integer.MAX_VALUE; }
}
