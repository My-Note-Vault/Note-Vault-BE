package com.example.search.retrieval;

import com.notevault.workspace.api.search.SearchSourceRef;

/** Read-only search projection. Contains no indexing or persistence operations. */
public record IndexedChunk(
        Long id,
        SearchSourceRef source,
        Long resourceId,
        String resourceType,
        String content,
        Double similarity
) {
}
