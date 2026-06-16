package com.example.workspace.common.websocket;

import com.example.workspace.common.redis.RedisPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class WorkSpaceEditorWebSocketHandlerTest {

    @Mock
    private SessionRegistry sessionRegistry;

    @Mock
    private RedisPublisher redisPublisher;

    @Mock
    private WebSocketSession session;

    private WorkSpaceEditorWebSocketHandler handler;

    @BeforeEach
    void setUp() {
        handler = new WorkSpaceEditorWebSocketHandler(sessionRegistry, redisPublisher);
        given(session.getAttributes()).willReturn(Map.of(
                CollaborationSessionAttributes.WORKSPACE_ID, 10L,
                CollaborationSessionAttributes.DOCUMENT_TYPE, "task",
                CollaborationSessionAttributes.DOCUMENT_ID, 101L
        ));
    }

    @Test
    @DisplayName("연결이 열리면 같은 문서 세션 그룹에 등록한다")
    void afterConnectionEstablished_addsSessionToRegistry() {
        handler.afterConnectionEstablished(session);

        verify(sessionRegistry).add(new SessionKey("task", 101L), session);
    }

    @Test
    @DisplayName("Yjs 바이너리 메시지를 Redis Pub/Sub으로 발행한다")
    void handleBinaryMessage_publishesPayload() {
        given(session.getId()).willReturn("session-1");

        handler.handleBinaryMessage(session, new BinaryMessage(new byte[]{1, 2, 3}));

        ArgumentCaptor<byte[]> payloadCaptor = ArgumentCaptor.forClass(byte[].class);
        verify(redisPublisher).publish(
                org.mockito.ArgumentMatchers.eq(10L),
                org.mockito.ArgumentMatchers.eq("task"),
                org.mockito.ArgumentMatchers.eq(101L),
                org.mockito.ArgumentMatchers.eq("session-1"),
                payloadCaptor.capture()
        );

        assertThat(payloadCaptor.getValue()).containsExactly(1, 2, 3);
    }

    @Test
    @DisplayName("전송 오류가 나면 세션을 제거하고 연결을 닫는다")
    void handleTransportError_removesSessionAndClosesConnection() throws Exception {
        given(session.isOpen()).willReturn(true);

        handler.handleTransportError(session, new RuntimeException("boom"));

        verify(sessionRegistry).remove(new SessionKey("task", 101L), session);
        verify(session).close(CloseStatus.SERVER_ERROR);
    }
}
