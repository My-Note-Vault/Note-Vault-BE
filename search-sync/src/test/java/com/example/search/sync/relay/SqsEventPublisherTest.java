package com.example.search.sync.relay;

import com.example.search.sync.outbox.OutboxMessage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.ArgumentMatchers;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.*;

import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SQS 배치 발행 단위 테스트 - 실제 AWS 호출 없음")
class SqsEventPublisherTest {
    @Mock SqsClient client;

    private SqsEventPublisher publisher() {
        return new SqsEventPublisher(client, new RelayProperties("https://sqs.invalid/test", "us-east-2", Duration.ofSeconds(10)));
    }

    private OutboxMessage message() {
        return new OutboxMessage(UUID.randomUUID(), "{\"schemaVersion\":2}", UUID.randomUUID());
    }

    @Test
    @DisplayName("배치 응답의 성공·실패·누락 이벤트를 각각 분류한다")
    void classifiesPartialResponse() {
        var success = message();
        var failure = message();
        var omitted = message();
        when(client.sendMessageBatch(ArgumentMatchers.<Consumer<SendMessageBatchRequest.Builder>>any())).thenAnswer(invocation -> {
            Consumer<SendMessageBatchRequest.Builder> configure = invocation.getArgument(0);
            var builder = SendMessageBatchRequest.builder();
            configure.accept(builder);
            var request = builder.build();
            assertThat(request.queueUrl()).isEqualTo("https://sqs.invalid/test");
            assertThat(request.entries()).extracting(SendMessageBatchRequestEntry::id)
                    .containsExactly(success.eventId().toString(), failure.eventId().toString(), omitted.eventId().toString());
            assertThat(request.entries()).extracting(SendMessageBatchRequestEntry::messageBody)
                    .containsOnly(success.payload());
            return SendMessageBatchResponse.builder()
                    .successful(SendMessageBatchResultEntry.builder().id(success.eventId().toString()).build())
                    .failed(BatchResultErrorEntry.builder().id(failure.eventId().toString())
                            .code("Throttled").message("retry").build()).build();
        });

        var result = publisher().publish(List.of(success, failure, omitted));

        assertThat(result.successful()).containsExactly(success);
        assertThat(result.failed()).extracting(OutboxMessage.Failure::message).containsExactly(failure, omitted);
        assertThat(result.failed()).extracting(OutboxMessage.Failure::reason)
                .containsExactly("Throttled: retry", "SQS response omitted this event");
    }

    @Test
    @DisplayName("SDK 전송 예외가 발생하면 배치 전체를 재시도 대상으로 반환한다")
    void sdkFailureRetriesAll() {
        var messages = List.of(message(), message());
        when(client.sendMessageBatch(ArgumentMatchers.<Consumer<SendMessageBatchRequest.Builder>>any()))
                .thenThrow(SdkClientException.create("offline"));

        var result = publisher().publish(messages);

        assertThat(result.successful()).isEmpty();
        assertThat(result.failed()).extracting(OutboxMessage.Failure::message).containsExactlyElementsOf(messages);
        assertThat(result.failed()).allSatisfy(failure -> assertThat(failure.reason()).contains("offline"));
    }

    @Test
    @DisplayName("동일 이벤트가 성공과 실패 양쪽에 있으면 실패로 처리한다")
    void contradictoryResponseIsFailure() {
        var message = message();
        when(client.sendMessageBatch(ArgumentMatchers.<Consumer<SendMessageBatchRequest.Builder>>any())).thenReturn(SendMessageBatchResponse.builder()
                .successful(SendMessageBatchResultEntry.builder().id(message.eventId().toString()).build())
                .failed(BatchResultErrorEntry.builder().id(message.eventId().toString()).code("Error").message("retry").build())
                .build());

        var result = publisher().publish(List.of(message));

        assertThat(result.successful()).isEmpty();
        assertThat(result.failed()).extracting(OutboxMessage.Failure::message).containsExactly(message);
    }
}
