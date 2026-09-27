package com.notevault.workspace.api.search;

/** A source snapshot: body version and title must still match when loading its contents. */
public record SearchSourceRef(
        KeywordSourceType sourceType, Long sourceId, String version, String title
) {
    public boolean sameSource(SearchSourceRef other) {
        return sourceType == other.sourceType && sourceId.equals(other.sourceId);
    }
}
