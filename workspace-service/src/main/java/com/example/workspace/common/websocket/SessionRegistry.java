package com.example.workspace.common.websocket;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class SessionRegistry {

    private static final int SEND_TIME_LIMIT_MILLIS = 10_000;
    private static final int BUFFER_SIZE_LIMIT_BYTES = 1_048_576;

    private final Map<SessionKey, Set<WebSocketSession>> rooms = new ConcurrentHashMap<>();

    public void add(SessionKey key, WebSocketSession session) {
        rooms.computeIfAbsent(key, k -> ConcurrentHashMap.newKeySet())
                .add(wrapIfNeeded(session));
    }

    public void remove(SessionKey key, WebSocketSession session) {
        Set<WebSocketSession> sessions = rooms.get(key);

        if (sessions == null) {
            return;
        }
        sessions.removeIf(registeredSession -> registeredSession.getId().equals(session.getId()));

        if (sessions.isEmpty()) {
            rooms.remove(key);
        }
    }


    public void broadcast(String documentType, Long documentId, String senderSessionId, byte[] payload) {
        SessionKey key = new SessionKey(documentType, documentId);

        for (WebSocketSession session : rooms.getOrDefault(key, Set.of())) {
            boolean isClosedOrSenderMySelf = !session.isOpen() || session.getId().equals(senderSessionId);

            if (isClosedOrSenderMySelf) {
                continue;
            }

            try {
                session.sendMessage(new BinaryMessage(payload));
            } catch (IOException | IllegalStateException e) {
                log.warn("Failed to send Yjs message to session {}", session.getId(), e);
                remove(key, session);
                closeQuietly(session);
            }
        }
    }

    private WebSocketSession wrapIfNeeded(WebSocketSession session) {
        if (session instanceof ConcurrentWebSocketSessionDecorator) {
            return session;
        }

        return new ConcurrentWebSocketSessionDecorator(
                session,
                SEND_TIME_LIMIT_MILLIS,
                BUFFER_SIZE_LIMIT_BYTES
        );
    }

    private void closeQuietly(WebSocketSession session) {
        if (!session.isOpen()) {
            return;
        }

        try {
            session.close(CloseStatus.SERVER_ERROR);
        } catch (IOException closeException) {
            log.debug("Failed to close Yjs session {}", session.getId(), closeException);
        }
    }
}
