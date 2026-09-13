package com.example.workspace.common.websocket;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.util.UriTemplate;

import java.util.Map;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class JwtHandshakeInterceptor implements HandshakeInterceptor {

    private static final UriTemplate URI_TEMPLATE = new UriTemplate("/ws/workspaces/{workSpaceId}/{documentType}/{documentId}");

    private final WebSocketTicketStore ticketStore;

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler, Map<String, Object> attributes) throws Exception {
        ServletServerHttpRequest servletRequest = (ServletServerHttpRequest) request;
        ServletServerHttpResponse servletResponse = (ServletServerHttpResponse) response;

        try {
            String ticketValue = servletRequest.getServletRequest().getParameter("ticket");
            Optional<WebSocketTicket> consumedTicket = ticketStore.consume(ticketValue);
            if (consumedTicket.isEmpty()) {
                servletResponse.setStatusCode(HttpStatus.UNAUTHORIZED);
                return false;
            }

            String requestURI = servletRequest.getServletRequest().getRequestURI();
            Map<String, String> matchedVariables = URI_TEMPLATE.match(requestURI);

            Long workSpaceId = Long.parseLong(matchedVariables.get("workSpaceId"));
            String documentType = matchedVariables.get("documentType");
            Long documentId = Long.parseLong(matchedVariables.get("documentId"));

            WebSocketTicket ticket = consumedTicket.get();
            if (!ticket.workSpaceId().equals(workSpaceId)
                    || !ticket.documentType().equalsIgnoreCase(documentType)
                    || !ticket.documentId().equals(documentId)) {
                servletResponse.setStatusCode(HttpStatus.FORBIDDEN);
                return false;
            }

            attributes.put(CollaborationSessionAttributes.WORKSPACE_ID, workSpaceId);
            attributes.put(CollaborationSessionAttributes.DOCUMENT_TYPE, documentType);
            attributes.put(CollaborationSessionAttributes.DOCUMENT_ID, documentId);
            attributes.put(CollaborationSessionAttributes.MEMBER_ID, ticket.memberId());

            return true;
        } catch (IllegalArgumentException e) {
            servletResponse.setStatusCode(HttpStatus.BAD_REQUEST);
            return false;
        }
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler, Exception exception) {

    }
}
