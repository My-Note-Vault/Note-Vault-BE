package com.notevault.workspace.api.search;

/**
 * sourceType/sourceId identify the indexed source; resourceType/resourceId identify its UI destination.
 * matchedContent is a short matching excerpt and can be null for title-only matches.
 * sourceRevision is the parent body revision, not a version of any matched linked plan.
 */
public record KeywordSearchHit(
        KeywordSourceType sourceType,
        Long sourceId,
        String resourceType,
        Long resourceId,
        String title,
        String matchedContent,
        Long sourceRevision,
        double score
) {
}
