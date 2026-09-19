package com.example.search.retrieval;

/** Read-only search projection. Contains no indexing or persistence operations. */
public record IndexedChunk(
        Long id,
        String sourceType,
        Long sourceId,
        Long resourceId,
        String resourceType,
        String sourceTitle,
        String content,
        String embedding
) {
}
