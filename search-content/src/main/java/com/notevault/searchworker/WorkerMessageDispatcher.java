package com.notevault.searchworker;

import com.notevault.searchworker.handlers.SearchRefreshHandler;
import com.notevault.searchworker.handlers.SearchDeleteHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class WorkerMessageDispatcher {
    private final SearchRefreshHandler refresh;
    private final SearchDeleteHandler delete;

    public String dispatch(SearchSyncMessage message) {
        return switch (message.messageType()) {
            // CRDT_COMPACT is accepted only for deliveries created by older API versions.
            case DOCUMENT_REFRESH, CRDT_COMPACT, SEARCH_REFRESH -> refresh.handle(message);
            case SEARCH_DELETE -> delete.handle(message);
        };
    }
}
