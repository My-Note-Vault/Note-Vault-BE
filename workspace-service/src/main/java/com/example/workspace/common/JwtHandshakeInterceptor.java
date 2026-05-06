package com.example.workspace.common;

import com.example.common.jwt.JwtService;
import com.example.workspace.workspace.command.domain.ParticipantRepository;
import com.example.workspace.workspace.command.domain.WorkSpace;
import com.example.workspace.workspace.query.WorkSpaceQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class JwtHandshakeInterceptor implements HandshakeInterceptor {

    private final JwtService jwtService;
    private final WorkSpaceQueryService workSpaceQueryService;

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler, Map<String, Object> attributes) throws Exception {
        ServletServerHttpRequest servletRequest = (ServletServerHttpRequest) request;

        String token = servletRequest.getServletRequest().getParameter("token");
        Long workSpaceId = extractWorkSpaceId(servletRequest.getServletRequest().getRequestURI());

        Long memberId = jwtService.getMemberId(token);
        WorkSpace workSpace = workSpaceQueryService.findWorkSpaceById(memberId, workSpaceId);

        attributes.put("workSpaceId", workSpace.getId());
        attributes.put("memberId", memberId);

        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler, Exception exception) {

    }

    private Long extractWorkSpaceId(String uri) {
        String[] parts = uri.split("/");
        return Long.valueOf(parts[parts.length - 1]);
    }

}
