package com.notevault.workspace.api.search;

/** A null chunkId denotes a title match; otherwise only that chunk matched. */
public record FieldKeywordHit(
        KeywordSourceType sourceType, Long sourceId, Long chunkId, double score
) {
}
