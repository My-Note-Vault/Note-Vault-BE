package com.example.workspace.common.websocket;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class WebSocketTicketStore {

    private static final String KEY_PREFIX = "websocket-ticket:";
    private static final Duration TICKET_TTL = Duration.ofSeconds(30);
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final RedisTemplate<String, String> redisTemplate;

    public String issue(final WebSocketTicket ticket) {
        byte[] randomBytes = new byte[32];
        SECURE_RANDOM.nextBytes(randomBytes);
        String ticketValue = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);

        redisTemplate.opsForValue().set(key(ticketValue), serialize(ticket), TICKET_TTL);
        return ticketValue;
    }

    public Optional<WebSocketTicket> consume(final String ticketValue) {
        if (ticketValue == null || ticketValue.isBlank()) {
            return Optional.empty();
        }

        // Redis GETDEL을 사용하므로 조회와 삭제 사이에 다른 handshake가 끼어들 수 없다.
        String serialized = redisTemplate.opsForValue().getAndDelete(key(ticketValue));
        if (serialized == null) {
            return Optional.empty();
        }

        try {
            return Optional.of(deserialize(serialized));
        } catch (RuntimeException exception) {
            return Optional.empty();
        }
    }

    private String key(final String ticketValue) {
        return KEY_PREFIX + ticketValue;
    }

    private String serialize(final WebSocketTicket ticket) {
        return String.join(":",
                ticket.memberId().toString(),
                ticket.workSpaceId().toString(),
                ticket.documentType(),
                ticket.documentId().toString()
        );
    }

    private WebSocketTicket deserialize(final String value) {
        String[] parts = value.split(":", -1);
        if (parts.length != 4) {
            throw new IllegalArgumentException("Invalid WebSocket ticket payload");
        }
        return new WebSocketTicket(
                Long.parseLong(parts[0]),
                Long.parseLong(parts[1]),
                parts[2],
                Long.parseLong(parts[3])
        );
    }
}
