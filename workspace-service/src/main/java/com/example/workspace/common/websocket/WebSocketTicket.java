package com.example.workspace.common.websocket;

public record WebSocketTicket(
        Long memberId,
        Long workSpaceId,
        String documentType,
        Long documentId
) {
}
