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
) {
    public String version() {
        // DailyNote's indexing source also includes linked Plans, which do not change its body revision.
        if (type == ContentSourceType.DAILY_NOTE) return contentHash;
        return revision == null ? contentHash : String.valueOf(revision);
    }
}
