package com.example.workspace.common.websocket;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.socket.WebSocketHandler;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class JwtHandshakeInterceptorTest {

    @Mock
    private WebSocketTicketStore ticketStore;

    @Mock
    private WebSocketMetrics metrics;

    @Mock
    private WebSocketHandler webSocketHandler;

    @Test
    @DisplayName("유효한 일회용 ticket의 문서 경로가 일치하면 세션 메타데이터를 저장한다")
    void beforeHandshake_withValidParticipant_storesAttributes() throws Exception {
        JwtHandshakeInterceptor interceptor = new JwtHandshakeInterceptor(ticketStore, metrics);
        MockHttpServletRequest servletRequest = request("/ws/workspaces/10/task/101", "access-token");
        MockHttpServletResponse servletResponse = new MockHttpServletResponse();
        Map<String, Object> attributes = new HashMap<>();

        given(ticketStore.consume("access-token")).willReturn(Optional.of(new WebSocketTicket(7L, 10L, "task", 101L)));

        boolean result = interceptor.beforeHandshake(
                new ServletServerHttpRequest(servletRequest),
                new ServletServerHttpResponse(servletResponse),
                webSocketHandler,
                attributes
        );

        assertThat(result).isTrue();
        assertThat(attributes.get(CollaborationSessionAttributes.WORKSPACE_ID)).isEqualTo(10L);
        assertThat(attributes.get(CollaborationSessionAttributes.DOCUMENT_TYPE)).isEqualTo("task");
        assertThat(attributes.get(CollaborationSessionAttributes.DOCUMENT_ID)).isEqualTo(101L);
        assertThat(attributes.get(CollaborationSessionAttributes.MEMBER_ID)).isEqualTo(7L);
    }

    @Test
    @DisplayName("만료되거나 이미 소비된 ticket이면 401로 핸드셰이크를 거부한다")
    void beforeHandshake_withInvalidToken_rejectsRequest() throws Exception {
        JwtHandshakeInterceptor interceptor = new JwtHandshakeInterceptor(ticketStore, metrics);
        MockHttpServletRequest servletRequest = request("/ws/workspaces/10/task/101", "invalid-token");
        MockHttpServletResponse servletResponse = new MockHttpServletResponse();

        given(ticketStore.consume("invalid-token")).willReturn(Optional.empty());

        boolean result = interceptor.beforeHandshake(
                new ServletServerHttpRequest(servletRequest),
                new ServletServerHttpResponse(servletResponse),
                webSocketHandler,
                new HashMap<>()
        );

        assertThat(result).isFalse();
        assertThat(servletResponse.getStatus()).isEqualTo(401);
        verify(metrics).connectionFailure("handshake", "invalid_ticket");
    }

    @Test
    @DisplayName("ticket과 다른 문서 경로로 연결하면 403으로 핸드셰이크를 거부한다")
    void beforeHandshake_withoutWorkspaceMembership_rejectsRequest() throws Exception {
        JwtHandshakeInterceptor interceptor = new JwtHandshakeInterceptor(ticketStore, metrics);
        MockHttpServletRequest servletRequest = request("/ws/workspaces/10/task/101", "access-token");
        MockHttpServletResponse servletResponse = new MockHttpServletResponse();

        given(ticketStore.consume("access-token")).willReturn(Optional.of(new WebSocketTicket(7L, 10L, "task", 999L)));

        boolean result = interceptor.beforeHandshake(
                new ServletServerHttpRequest(servletRequest),
                new ServletServerHttpResponse(servletResponse),
                webSocketHandler,
                new HashMap<>()
        );

        assertThat(result).isFalse();
        assertThat(servletResponse.getStatus()).isEqualTo(403);
        verify(metrics).connectionFailure("handshake", "ticket_mismatch");
    }

    private MockHttpServletRequest request(final String requestUri, final String token) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", requestUri);
        request.setParameter("ticket", token);
        return request;
    }
}
