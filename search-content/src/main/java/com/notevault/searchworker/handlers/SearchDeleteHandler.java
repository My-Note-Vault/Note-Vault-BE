package com.notevault.searchworker.handlers;

import com.example.search.indexing.SearchContentSync;
import com.notevault.searchworker.SearchSyncMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SearchDeleteHandler {
    private final SearchContentSync search;

    public String handle(SearchSyncMessage message) {
        return search.delete(message.sourceType(), message.sourceId()).name();
    }
}
