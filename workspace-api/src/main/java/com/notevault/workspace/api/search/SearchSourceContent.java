package com.notevault.workspace.api.search;

/** Accessible original text used for answer excerpts when indexed chunks are unavailable. */
public record SearchSourceContent(
        SearchSourceRef source, Long resourceId, String resourceType, String content
) {
}
