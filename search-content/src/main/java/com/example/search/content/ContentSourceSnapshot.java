package com.example.search.content;

import java.time.LocalDateTime;
import java.util.Objects;

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
    public boolean sameSearchInput(ContentSourceSnapshot other) {
        return type == other.type && Objects.equals(sourceId, other.sourceId)
                && Objects.equals(version(), other.version())
                && Objects.equals(contentHash, other.contentHash)
                && Objects.equals(title, other.title)
                && Objects.equals(workspaceId, other.workspaceId)
                && Objects.equals(ownerId, other.ownerId)
                && Objects.equals(resourceType, other.resourceType)
                && Objects.equals(resourceId, other.resourceId);
    }

    public String version() {
        // DailyNote's indexing source also includes linked Plans, which do not change its body revision.
        if (type == ContentSourceType.DAILY_NOTE) return contentHash;
        return revision == null ? contentHash : String.valueOf(revision);
    }
}
