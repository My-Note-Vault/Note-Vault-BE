package com.notevault.searchworker;

import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.Message;

import java.time.Duration;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

/** One received message. Its heartbeat never runs on the thread doing embeddings. */
@Slf4j
final class MessageVisibility implements AutoCloseable {
    static final int TIMEOUT_SECONDS = 120;
    private static final long MAX_PROCESSING_NANOS = Duration.ofMinutes(10).toNanos();

    private final SqsClient client;
    private final String queueUrl;
    private final Message message;
    private final Thread processingThread = Thread.currentThread();
    private final long started = System.nanoTime();
    private final ScheduledFuture<?> heartbeat;
    private boolean closed;
    private boolean lost;

    MessageVisibility(SqsClient client, String queueUrl, Message message, ScheduledExecutorService scheduler) {
        this.client = client;
        this.queueUrl = queueUrl;
        this.message = message;
        heartbeat = scheduler.scheduleWithFixedDelay(this::extend, 30, 30, TimeUnit.SECONDS);
    }

    private synchronized void extend() {
        if (closed) return;
        try {
            if (expired()) throw new IllegalStateException("Message processing exceeded 10 minutes");
            change(TIMEOUT_SECONDS);
        } catch (RuntimeException failure) {
            lost = true;
            close();
            processingThread.interrupt();
            log.warn("SQS visibility extension failed: messageId={}", message.messageId(), failure);
        }
    }

    synchronized void beforeDelete() {
        close();
        if (lost || expired() || Thread.currentThread().isInterrupted()) {
            throw new IllegalStateException("Message visibility ownership was lost");
        }
    }

    synchronized void retry(int receiveCount) {
        close();
        if (lost || expired()) return;
        int ceiling = Math.min(300, 30 << Math.min(Math.max(receiveCount - 1, 0), 4));
        int delay = ThreadLocalRandom.current().nextInt(Math.max(30, ceiling / 2), ceiling + 1);
        change(delay);
    }

    private boolean expired() {
        return System.nanoTime() - started >= MAX_PROCESSING_NANOS;
    }

    private void change(int seconds) {
        client.changeMessageVisibility(request -> request.queueUrl(queueUrl)
                .receiptHandle(message.receiptHandle()).visibilityTimeout(seconds));
    }

    @Override
    public synchronized void close() {
        closed = true;
        heartbeat.cancel(false);
    }
}
