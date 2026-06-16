package com.example.workspace.common.redis;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RedisPublisherTest {

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    private RedisPublisher redisPublisher;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        redisPublisher = new RedisPublisher(redisTemplate, objectMapper);
    }

    @Test
    @DisplayName("Yjs 바이너리 업데이트를 문서 채널에 발행한다")
    void publish_sendsSerializedMessageToDocumentChannel() throws Exception {
        byte[] payload = new byte[]{1, 2, 3, 4};

        redisPublisher.publish(10L, "task", 101L, "session-1", payload);

        ArgumentCaptor<String> channelCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> bodyCaptor = ArgumentCaptor.forClass(String.class);
        verify(redisTemplate).convertAndSend(channelCaptor.capture(), bodyCaptor.capture());

        assertThat(channelCaptor.getValue()).isEqualTo("collab:task:101");

        RealTimeEditMessage message =
                objectMapper.readValue(bodyCaptor.getValue(), RealTimeEditMessage.class);

        assertThat(message.workSpaceId()).isEqualTo(10L);
        assertThat(message.documentType()).isEqualTo("task");
        assertThat(message.documentId()).isEqualTo(101L);
        assertThat(message.senderSessionId()).isEqualTo("session-1");
        assertThat(message.base64Payload()).isEqualTo("AQIDBA==");
    }
}
