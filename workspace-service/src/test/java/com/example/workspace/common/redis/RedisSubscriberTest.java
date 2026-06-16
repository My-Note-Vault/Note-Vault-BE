package com.example.workspace.common.redis;

import com.example.workspace.common.websocket.SessionRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.connection.Message;

import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RedisSubscriberTest {

    @Mock
    private SessionRegistry sessionRegistry;

    private RedisSubscriber redisSubscriber;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        redisSubscriber = new RedisSubscriber(objectMapper, sessionRegistry);
    }

    @Test
    @DisplayName("Redis 메시지를 디코딩해 같은 문서 세션에 브로드캐스트한다")
    void onMessage_broadcastsDecodedPayload() throws Exception {
        byte[] payload = new byte[]{9, 8, 7};
        RealTimeEditMessage realtimeMessage = new RealTimeEditMessage(
                10L,
                "task",
                101L,
                "session-1",
                Base64.getEncoder().encodeToString(payload)
        );

        Message message = mock(Message.class);
        given(message.getBody()).willReturn(objectMapper.writeValueAsBytes(realtimeMessage));

        redisSubscriber.onMessage(message, new byte[0]);

        ArgumentCaptor<byte[]> payloadCaptor = ArgumentCaptor.forClass(byte[].class);
        verify(sessionRegistry).broadcast(eq("task"), eq(101L), eq("session-1"), payloadCaptor.capture());
        assertThat(payloadCaptor.getValue()).containsExactly(payload);
    }
}
