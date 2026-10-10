package com.example.search.chat.session;

import com.example.search.chat.api.ChatErrorCode;
import com.example.search.chat.api.ChatException;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

/** Cursors are pagination positions, never authorization credentials. */
final class ChatCursor {
    private ChatCursor() { }

    record SessionPosition(Instant updatedAt, UUID sessionId) { }

    static String session(long memberId, Instant updatedAt, UUID sessionId) {
        return encode("v1|sessions|" + memberId + "|" + updatedAt + "|" + sessionId);
    }

    static SessionPosition session(String cursor, long memberId) {
        if (cursor == null) return null;
        try {
            String[] parts = decode(cursor, "sessions", memberId);
            Instant updatedAt = Instant.parse(parts[3]);
            if (updatedAt.isBefore(Instant.EPOCH) || updatedAt.isAfter(Instant.parse("9999-12-31T23:59:59Z"))) {
                throw new IllegalArgumentException("Invalid cursor timestamp");
            }
            return new SessionPosition(updatedAt, UUID.fromString(parts[4]));
        } catch (RuntimeException invalid) {
            throw new ChatException(ChatErrorCode.INVALID_CHAT_REQUEST);
        }
    }

    static String message(long memberId, UUID sessionId, long sequence) {
        return encode("v1|messages|" + memberId + "|" + sessionId + "|" + sequence);
    }

    static long message(String cursor, long memberId, UUID sessionId) {
        if (cursor == null) return 0;
        try {
            String[] parts = decode(cursor, "messages", memberId);
            long sequence = Long.parseLong(parts[4]);
            if (!sessionId.equals(UUID.fromString(parts[3])) || sequence < 1) {
                throw new IllegalArgumentException("Invalid message position");
            }
            return sequence;
        } catch (RuntimeException invalid) {
            throw new ChatException(ChatErrorCode.INVALID_CHAT_REQUEST);
        }
    }

    private static String encode(String value) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    private static String[] decode(String cursor, String kind, long memberId) {
        if (cursor.isBlank() || cursor.length() > 256) throw new IllegalArgumentException("Invalid cursor length");
        String[] parts = new String(Base64.getUrlDecoder().decode(cursor), StandardCharsets.UTF_8).split("\\|", -1);
        if (parts.length != 5 || !"v1".equals(parts[0]) || !kind.equals(parts[1])
                || !Long.toString(memberId).equals(parts[2])) {
            throw new IllegalArgumentException("Invalid cursor scope");
        }
        return parts;
    }
}
