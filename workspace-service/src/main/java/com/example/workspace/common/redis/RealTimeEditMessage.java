package com.example.workspace.common.redis;

public record RealTimeEditMessage(
        Long workSpaceId,
        String documentType,
        Long documentId,
        String senderSessionId,
        String base64Payload
) {
}
