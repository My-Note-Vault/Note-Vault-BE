package com.example.search.content;

import java.time.LocalDateTime;

public record ContentSourceSnapshot(
        ContentSourceType type,
        Long sourceId,
        Long workspaceId,
        Long ownerId,
        String resourceType,
        Long resourceId,
        String title,
        String content,
        Long revision,
        String contentHash,
        LocalDateTime sourceUpdatedAt
) { }
