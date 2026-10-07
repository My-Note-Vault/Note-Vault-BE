package com.notevault.searchworker;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.ChangeMessageVisibilityRequest;
import software.amazon.awssdk.services.sqs.model.Message;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("worker 메시지 가시성 소유권 단위 테스트")
class MessageVisibilityTest {
    @Mock SqsClient sqs;
    @Mock ScheduledExecutorService scheduler;
    @Mock ScheduledFuture<?> heartbeat;
    private final ArgumentCaptor<Runnable> tick = ArgumentCaptor.forClass(Runnable.class);
    private MessageVisibility visibility;

    @BeforeEach
    void setUp() {
        doReturn(heartbeat).when(scheduler).scheduleWithFixedDelay(tick.capture(), eq(30L), eq(30L), eq(TimeUnit.SECONDS));
        visibility = new MessageVisibility(sqs, "https://sqs.invalid/test",
                Message.builder().messageId("id").receiptHandle("receipt").build(), scheduler);
    }

    @AfterEach
    void tearDown() {
        visibility.close();
        Thread.interrupted();
    }

    private ChangeMessageVisibilityRequest capturedRequest() {
        ArgumentCaptor<Consumer<ChangeMessageVisibilityRequest.Builder>> captor = ArgumentCaptor.captor();
        verify(sqs).changeMessageVisibility(captor.capture());
        var builder = ChangeMessageVisibilityRequest.builder();
        captor.getValue().accept(builder);
        return builder.build();
    }

    @Test
    @DisplayName("heartbeat는 처리 중 메시지의 가시성을 120초 연장한다")
    void heartbeatExtendsLease() {
        tick.getValue().run();
        var request = capturedRequest();
        assertThat(request.queueUrl()).isEqualTo("https://sqs.invalid/test");
        assertThat(request.receiptHandle()).isEqualTo("receipt");
        assertThat(request.visibilityTimeout()).isEqualTo(120);
    }

    @ParameterizedTest
    @CsvSource({"1,30,30", "2,30,60", "3,60,120", "4,120,240", "5,150,300", "100,150,300"})
    @DisplayName("재시도 지연은 수신 횟수별 백오프 범위 안에서 최대 300초로 제한된다")
    void retryUsesBoundedBackoff(int receiveCount, int minimum, int maximum) {
        visibility.retry(receiveCount);
        assertThat(capturedRequest().visibilityTimeout()).isBetween(minimum, maximum);
        verify(heartbeat).cancel(false);
    }

    @Test
    @DisplayName("가시성 연장 실패 시 작업 스레드를 중단하고 메시지 삭제를 금지한다")
    void lostLeasePreventsAcknowledgement() {
        when(sqs.changeMessageVisibility(ArgumentMatchers.<Consumer<ChangeMessageVisibilityRequest.Builder>>any()))
                .thenThrow(new IllegalStateException("lease lost"));
        tick.getValue().run();

        assertThat(Thread.currentThread().isInterrupted()).isTrue();
        assertThatThrownBy(visibility::beforeDelete).isInstanceOf(IllegalStateException.class);
        visibility.retry(1);
        verify(sqs, times(1)).changeMessageVisibility(ArgumentMatchers.<Consumer<ChangeMessageVisibilityRequest.Builder>>any());
    }

    @Test
    @DisplayName("삭제 준비 이후에는 heartbeat가 가시성을 연장하지 않는다")
    void stopsHeartbeatBeforeDelete() {
        visibility.beforeDelete();
        tick.getValue().run();
        verify(heartbeat).cancel(false);
        verifyNoInteractions(sqs);
    }
}
