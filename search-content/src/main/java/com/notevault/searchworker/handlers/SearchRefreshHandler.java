package com.notevault.searchworker.handlers;

import com.example.search.content.ContentSourceType;
import com.example.search.crdt.DocumentCrdtProcessor;
import com.example.search.indexing.SearchContentSync;
import com.notevault.searchworker.SearchSyncMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SearchRefreshHandler {
    private final DocumentCrdtProcessor crdt;
    private final SearchContentSync search;

    public String handle(SearchSyncMessage message) {
        if (message.sourceType() == ContentSourceType.DOCUMENT) {
            crdt.refreshDocument(message.sourceId(), message.contentRevision());
        }
        // Always finish indexing on retries, even if the snapshot/body already cover the request.
        return search.synchronize(message.sourceType(), message.sourceId(), message.contentRevision()).name();
    }
}
