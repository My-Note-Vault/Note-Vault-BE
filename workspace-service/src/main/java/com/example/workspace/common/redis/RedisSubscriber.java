package com.example.workspace.common.redis;

import com.example.workspace.common.websocket.SessionRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Slf4j
@RequiredArgsConstructor
@Component
public class RedisSubscriber implements MessageListener {

    private final ObjectMapper objectMapper;
    private final SessionRegistry sessionRegistry;


    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            String json = new String(message.getBody(), StandardCharsets.UTF_8);
            RealTimeEditMessage redisMessage = objectMapper.readValue(json, RealTimeEditMessage.class);

            byte[] payload = Base64.getDecoder()
                    .decode(redisMessage.base64Payload());

            sessionRegistry.broadcast(
                    redisMessage.documentType(),
                    redisMessage.documentId(),
                    redisMessage.senderSessionId(),
                    payload
            );

            String channel = new String(
                    message.getChannel(),
                    StandardCharsets.UTF_8
            );
            log.info(
                    "REDIS_SUBSCRIBE channel={}, bodySize={}, server={}",
                    channel,
                    message.getBody().length,
                    InetAddress.getLocalHost().getHostName()
            );
        } catch (Exception e) {
            log.error("Failed to consume Yjs collaboration message from Redis", e);
        }
    }
}
