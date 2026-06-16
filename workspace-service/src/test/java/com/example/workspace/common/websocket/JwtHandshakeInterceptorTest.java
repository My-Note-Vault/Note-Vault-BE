package com.example.workspace.common.websocket;

import com.example.common.jwt.JwtService;
import com.example.workspace.workspace.command.domain.WorkSpace;
import com.example.workspace.workspace.query.WorkSpaceQueryService;
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
import java.util.NoSuchElementException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class JwtHandshakeInterceptorTest {

    @Mock
    private JwtService jwtService;

    @Mock
    private WorkSpaceQueryService workSpaceQueryService;

    @Mock
    private WebSocketHandler webSocketHandler;

    @Test
    @DisplayName("유효한 access token과 workspace 참여자면 세션 메타데이터를 저장한다")
    void beforeHandshake_withValidParticipant_storesAttributes() throws Exception {
        JwtHandshakeInterceptor interceptor = new JwtHandshakeInterceptor(jwtService, workSpaceQueryService);
        MockHttpServletRequest servletRequest = request("/ws/workspaces/10/task/101", "access-token");
        MockHttpServletResponse servletResponse = new MockHttpServletResponse();
        Map<String, Object> attributes = new HashMap<>();

        given(jwtService.isInvalidToken("access-token")).willReturn(false);
        given(jwtService.isAccessToken("access-token")).willReturn(true);
        given(jwtService.getMemberId("access-token")).willReturn(7L);
        given(workSpaceQueryService.findWorkSpaceById(7L, 10L)).willReturn(mock(WorkSpace.class));

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
    @DisplayName("유효하지 않은 토큰이면 401로 핸드셰이크를 거부한다")
    void beforeHandshake_withInvalidToken_rejectsRequest() throws Exception {
        JwtHandshakeInterceptor interceptor = new JwtHandshakeInterceptor(jwtService, workSpaceQueryService);
        MockHttpServletRequest servletRequest = request("/ws/workspaces/10/task/101", "invalid-token");
        MockHttpServletResponse servletResponse = new MockHttpServletResponse();

        given(jwtService.isInvalidToken("invalid-token")).willReturn(true);

        boolean result = interceptor.beforeHandshake(
                new ServletServerHttpRequest(servletRequest),
                new ServletServerHttpResponse(servletResponse),
                webSocketHandler,
                new HashMap<>()
        );

        assertThat(result).isFalse();
        assertThat(servletResponse.getStatus()).isEqualTo(401);
    }

    @Test
    @DisplayName("워크스페이스 참여자가 아니면 403으로 핸드셰이크를 거부한다")
    void beforeHandshake_withoutWorkspaceMembership_rejectsRequest() throws Exception {
        JwtHandshakeInterceptor interceptor = new JwtHandshakeInterceptor(jwtService, workSpaceQueryService);
        MockHttpServletRequest servletRequest = request("/ws/workspaces/10/task/101", "access-token");
        MockHttpServletResponse servletResponse = new MockHttpServletResponse();

        given(jwtService.isInvalidToken("access-token")).willReturn(false);
        given(jwtService.isAccessToken("access-token")).willReturn(true);
        given(jwtService.getMemberId("access-token")).willReturn(7L);
        given(workSpaceQueryService.findWorkSpaceById(7L, 10L))
                .willThrow(new NoSuchElementException("참여자가 아닙니다"));

        boolean result = interceptor.beforeHandshake(
                new ServletServerHttpRequest(servletRequest),
                new ServletServerHttpResponse(servletResponse),
                webSocketHandler,
                new HashMap<>()
        );

        assertThat(result).isFalse();
        assertThat(servletResponse.getStatus()).isEqualTo(403);
    }

    private MockHttpServletRequest request(final String requestUri, final String token) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", requestUri);
        request.setParameter("token", token);
        return request;
    }
}
