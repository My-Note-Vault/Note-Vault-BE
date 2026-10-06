package com.example.search.indexing;

import com.example.search.content.ContentSourceSnapshot;
import com.example.search.content.ContentSourceType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.NoSuchElementException;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(propagation = Propagation.NEVER)
public class SearchContentSync {
    private final ContentIndexingTransactions transactions;
    private final ContentIndexingService indexing;

    public Result synchronize(ContentSourceType type, Long sourceId, Long requestedRevision) {
        Optional<ContentSourceSnapshot> snapshot = transactions.readSource(type, sourceId);
        if (snapshot.isEmpty()) return deleteMissing(type, sourceId);
        ContentSourceSnapshot source = snapshot.get();
        // Older events request the current state; only a database behind the event must wait.
        if (requestedRevision != null
                && (source.revision() == null || source.revision() < requestedRevision)) {
            throw new ContentIndexingService.StaleContentException();
        }
        try {
            indexing.index(source);
            return Result.READY;
        } catch (NoSuchElementException deletedDuringEmbedding) {
            return deleteMissing(type, sourceId);
        }
    }

    private Result deleteMissing(ContentSourceType type, Long sourceId) {
        transactions.deleteIfMissing(type, sourceId);
        return Result.DELETED;
    }

    public enum Result { READY, DELETED }

    public Result delete(ContentSourceType type, Long sourceId) {
        return deleteMissing(type, sourceId);
    }
}
