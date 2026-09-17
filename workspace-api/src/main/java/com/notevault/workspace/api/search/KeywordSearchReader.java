package com.notevault.workspace.api.search;

import java.util.List;

public interface KeywordSearchReader {

    /**
     * Searches only sources accessible to memberId. Keeps at most limitPerKeyword hits per keyword,
     * merges duplicate sources using their highest score, and returns hits in descending score order.
     * Blank keywords are ignored; an empty keyword list or zero limit returns no hits.
     */
    List<KeywordSearchHit> search(Long memberId, List<String> keywords, int limitPerKeyword);
}
