package com.notevault.workspace.api.search;

import java.util.List;

public interface KeywordSearchReader {

    /** Returns at most limit documents per field, including original text without current chunks. */
    List<FieldKeywordHit> findHybridMatches(Long memberId, String question, List<String> keywords, int limit);

    /** Loads a batch of original texts, checking access, body version and title in the same query. */
    List<SearchSourceContent> findAccessibleSources(Long memberId, List<SearchSourceRef> sources);

    /**
     * Searches only sources accessible to memberId. Keeps at most limitPerKeyword hits per keyword,
     * merges duplicate sources using their highest score, and returns hits in descending score order.
     * Blank keywords are ignored; an empty keyword list or zero limit returns no hits.
     */
    List<KeywordSearchHit> search(Long memberId, List<String> keywords, int limitPerKeyword);
}
