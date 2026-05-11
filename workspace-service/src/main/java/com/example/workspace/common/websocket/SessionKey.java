package com.example.workspace.common.websocket;

public record SessionKey(
        String documentType,
        Long documentId
) {
}
