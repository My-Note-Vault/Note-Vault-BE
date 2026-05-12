package com.example.workspace.common.websocket;

import com.example.workspace.common.redis.RedisPublisher;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.BinaryWebSocketHandler;

import java.net.InetAddress;
import java.nio.ByteBuffer;

@Slf4j
@RequiredArgsConstructor
@Component
public class WorkSpaceEditorWebSocketHandler extends BinaryWebSocketHandler {

    private final SessionRegistry sessionRegistry;
    private final RedisPublisher redisPublisher;

    @SneakyThrows
    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        log.info(
                "WS_CONNECTED sessionId={}, uri={}, server={}",
                session.getId(),
                session.getUri(),
                InetAddress.getLocalHost().getHostName()
        );
        sessionRegistry.add(sessionKey(session), session);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessionRegistry.remove(sessionKey(session), session);
    }

    @SneakyThrows
    @Override
    protected void handleBinaryMessage(WebSocketSession session, BinaryMessage message) {
        log.info(
                "WS_BINARY_RECEIVED sessionId={}, payloadSize={}, server={}",
                session.getId(),
                message.getPayloadLength(),
                InetAddress.getLocalHost().getHostName()
        );


        Long workSpaceId = (Long) session.getAttributes().get(CollaborationSessionAttributes.WORKSPACE_ID);
        String documentType = (String) session.getAttributes().get(CollaborationSessionAttributes.DOCUMENT_TYPE);
        Long documentId = (Long) session.getAttributes().get(CollaborationSessionAttributes.DOCUMENT_ID);

        ByteBuffer readOnlyBuffer = message.getPayload().asReadOnlyBuffer();
        byte[] payload = new byte[readOnlyBuffer.remaining()];
        readOnlyBuffer.get(payload);

        redisPublisher.publish(workSpaceId, documentType, documentId, session.getId(), payload);
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        sessionRegistry.remove(sessionKey(session), session);

        if (session.isOpen()) {
            session.close(CloseStatus.SERVER_ERROR);
        }
    }

    private SessionKey sessionKey(final WebSocketSession session) {
        String documentType = (String) session.getAttributes().get(CollaborationSessionAttributes.DOCUMENT_TYPE);
        Long documentId = (Long) session.getAttributes().get(CollaborationSessionAttributes.DOCUMENT_ID);
        return new SessionKey(documentType, documentId);
    }
}
