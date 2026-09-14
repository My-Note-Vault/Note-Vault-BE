package com.example.workspace.common.websocket;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class WebSocketMetrics {

    private final MeterRegistry meterRegistry;

    public WebSocketMetrics(final MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    public void connectionOpened(final String documentType) {
        meterRegistry.counter(
                "websocket.connections",
                "document_type", normalizeDocumentType(documentType)
        ).increment();
    }

    public void connectionClosed(final String documentType, final String outcome) {
        meterRegistry.counter(
                "websocket.connections.closed",
                "document_type", normalizeDocumentType(documentType),
                "outcome", outcome
        ).increment();
    }

    public void connectionFailure(final String stage, final String reason) {
        meterRegistry.counter(
                "websocket.connection.failures",
                "stage", stage,
                "reason", reason
        ).increment();
    }

    public void messageReceived(
            final String documentType,
            final String messageType,
            final int payloadBytes
    ) {
        String normalizedDocumentType = normalizeDocumentType(documentType);
        meterRegistry.counter(
                "websocket.messages.received",
                "document_type", normalizedDocumentType,
                "message_type", messageType
        ).increment();
        meterRegistry.counter(
                "websocket.message.bytes.received",
                "document_type", normalizedDocumentType
        ).increment(payloadBytes);
    }

    public void messageSent(
            final String documentType,
            final String delivery,
            final int payloadBytes
    ) {
        String normalizedDocumentType = normalizeDocumentType(documentType);
        meterRegistry.counter(
                "websocket.messages.sent",
                "document_type", normalizedDocumentType,
                "delivery", delivery
        ).increment();
        meterRegistry.counter(
                "websocket.message.bytes.sent",
                "document_type", normalizedDocumentType,
                "delivery", delivery
        ).increment(payloadBytes);
    }

    public void sendFailure(final String documentType, final String delivery) {
        meterRegistry.counter(
                "websocket.send.failures",
                "document_type", normalizeDocumentType(documentType),
                "delivery", delivery
        ).increment();
    }

    public void recordMessageLatency(
            final String documentType,
            final String messageType,
            final String outcome,
            final long elapsedNanos
    ) {
        Timer.builder("websocket.message.latency")
                .description("Time spent processing an inbound WebSocket message")
                .tag("document_type", normalizeDocumentType(documentType))
                .tag("message_type", messageType)
                .tag("outcome", outcome)
                .publishPercentileHistogram()
                .register(meterRegistry)
                .record(Duration.ofNanos(elapsedNanos));
    }

    private String normalizeDocumentType(final String documentType) {
        if (documentType == null) {
            return "unknown";
        }

        return switch (documentType.toLowerCase()) {
            case "space", "note", "task" -> documentType.toLowerCase();
            default -> "other";
        };
    }
}
