package com.example.search.retrieval;

import com.notevault.workspace.api.search.SearchSourceContent;
import com.notevault.workspace.api.search.SearchSourceRef;

/** Answer context from an indexed chunk or an original-text excerpt. */
public record SearchEvidence(
        Long chunkId, SearchSourceRef source, Long resourceId, String resourceType,
        String content, Double similarity
) {
    public static SearchEvidence fromChunk(IndexedChunk chunk) {
        return new SearchEvidence(chunk.id(), chunk.source(), chunk.resourceId(),
                chunk.resourceType(), chunk.content(), chunk.similarity());
    }

    public static SearchEvidence fromSource(SearchSourceContent source, String excerpt) {
        return new SearchEvidence(null, source.source(), source.resourceId(), source.resourceType(), excerpt, null);
    }
}
