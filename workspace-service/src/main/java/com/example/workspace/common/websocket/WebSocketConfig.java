package com.example.workspace.common.websocket;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@RequiredArgsConstructor
@EnableWebSocket
@Configuration
public class WebSocketConfig implements WebSocketConfigurer {

    private final WorkSpaceEditorWebSocketHandler workSpaceEditorWebSocketHandler;
    private final JwtHandshakeInterceptor jwtHandshakeInterceptor;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(workSpaceEditorWebSocketHandler, "/ws/workspaces/{workSpaceId}/{documentType}/{documentId}")
                .addInterceptors(jwtHandshakeInterceptor)
                .setAllowedOrigins("https://note-vault.cloud", "http://localhost:5173");
    }
}
