package com.notevault.workspace.api.search;

/** BODY with a null chunkId denotes original text that has no current indexed chunks. */
public record FieldKeywordHit(
        SearchSourceRef source, MatchedField matchedField, Long chunkId, double score
) {
    public enum MatchedField { TITLE, BODY }
}
