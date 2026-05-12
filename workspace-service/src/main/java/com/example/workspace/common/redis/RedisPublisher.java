package com.example.workspace.common.redis;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.util.Base64;

@Slf4j
@RequiredArgsConstructor
@Component
public class RedisPublisher {

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    @SneakyThrows
    public void publish(
            final Long workSpaceId,
            final String documentType,
            final Long documentId,
            final String senderSessionId,
            final byte[] payload
    ) {
        try {
            RealTimeEditMessage message = new RealTimeEditMessage(
                    workSpaceId,
                    documentType,
                    documentId,
                    senderSessionId,
                    Base64.getEncoder().encodeToString(payload)
            );

            String json = objectMapper.writeValueAsString(message);
            redisTemplate.convertAndSend(CollaborationChannel.documentChannel(documentType, documentId), json);

            log.info(
                    "REDIS_PUBLISH channel={}, payloadSize={}, server={}",
                    CollaborationChannel.documentChannel(documentType, documentId),
                    payload.length,
                    InetAddress.getLocalHost().getHostName()
            );
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to publish Yjs collaboration message", e);
        }
    }
}
