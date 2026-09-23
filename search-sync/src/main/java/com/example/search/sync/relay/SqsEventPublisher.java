package com.example.search.sync.relay;

import com.example.search.sync.outbox.OutboxMessage;
import lombok.RequiredArgsConstructor;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageBatchRequestEntry;
import software.amazon.awssdk.services.sqs.model.SendMessageBatchResponse;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class SqsEventPublisher {
    private final SqsClient client;
    private final RelayProperties properties;

    public Publication publish(List<OutboxMessage> messages) {
        var entries = messages.stream().map(message -> SendMessageBatchRequestEntry.builder()
                .id(message.eventId().toString())
                .messageBody(message.payload())
                .build()).toList();
        try {
            SendMessageBatchResponse response = client.sendMessageBatch(request -> request
                    .queueUrl(properties.queueUrl()).entries(entries));
            return classify(messages, response);
        } catch (SdkException exception) {
            String reason = exception.getClass().getSimpleName() + ": " + exception.getMessage();
            return new Publication(List.of(), messages.stream()
                    .map(message -> new OutboxMessage.Failure(message, reason)).toList());
        }
    }

    private Publication classify(List<OutboxMessage> messages, SendMessageBatchResponse response) {
        var successIds = response.successful().stream().map(result -> result.id()).collect(Collectors.toSet());
        var errors = response.failed().stream().collect(Collectors.toMap(
                result -> result.id(), result -> result.code() + ": " + result.message(), (first, second) -> first));
        List<OutboxMessage> successful = new ArrayList<>();
        List<OutboxMessage.Failure> failed = new ArrayList<>();
        for (OutboxMessage message : messages) {
            String id = message.eventId().toString();
            if (successIds.contains(id) && !errors.containsKey(id)) {
                successful.add(message);
            } else {
                failed.add(new OutboxMessage.Failure(message,
                        errors.getOrDefault(id, "SQS response omitted this event")));
            }
        }
        return new Publication(List.copyOf(successful), List.copyOf(failed));
    }

    public record Publication(List<OutboxMessage> successful, List<OutboxMessage.Failure> failed) {
    }
}
