package com.example.search.indexing;

import com.example.search.content.ContentSourceSnapshot;
import com.example.search.embedding.EmbeddingClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.NoSuchElementException;

@RequiredArgsConstructor
@Service
@Transactional(propagation = Propagation.NEVER)
public class ContentIndexingService {
    private final ContentIndexingTransactions transactions;
    private final EmbeddingClient openAi;
    private final ContentChunker chunker;

    public void indexDocument(Long memberId, String type, Long resourceId, Long requestedRevision) {
        ContentSourceSnapshot source = transactions.readDocument(memberId, type, resourceId);
        if (requestedRevision == null || !Objects.equals(source.revision(), requestedRevision)) {
            throw new StaleContentException();
        }
        index(source);
    }

    public void indexDailyNote(Long memberId, Long dailyNoteId) {
        index(transactions.readDailyNote(memberId, dailyNoteId));
    }

    /** Worker entry point for a Plan content change; each linked note keeps its own index. */
    public void indexDailyNotesForPlan(Long planId) {
        RuntimeException failure = null;
        for (ContentIndexingTransactions.DailyNoteReference note : transactions.findDailyNotesLinkedToPlan(planId)) {
            try {
                indexDailyNote(note.ownerId(), note.id());
            } catch (NoSuchElementException deleted) {
                // The note may have been deleted after its link was read.
            } catch (RuntimeException exception) {
                if (failure == null) failure = exception;
                else failure.addSuppressed(exception);
            }
        }
        if (failure != null) throw failure;
    }

    private void index(ContentSourceSnapshot source) {
        RuntimeException failure = null;
        try {
            indexTitle(source);
        } catch (RuntimeException exception) {
            failure = exception;
        }
        try {
            indexBody(source);
        } catch (RuntimeException exception) {
            if (failure == null) failure = exception;
            else failure.addSuppressed(exception);
        }
        if (failure != null) throw failure;
    }

    public void indexTitle(ContentSourceSnapshot source) {
        ContentIndexingTransactions.TitleWork work = transactions.prepareTitle(source, openAi.embeddingModel());
        if (work == null) return;
        try {
            // The transaction that claims the title has finished before this external call.
            List<String> vectors = openAi.embed(List.of(work.source().title()));
            if (vectors == null || vectors.size() != 1 || vectors.getFirst() == null || vectors.getFirst().isBlank()) {
                throw new IllegalStateException("제목 임베딩 응답이 비어 있거나 개수가 일치하지 않습니다.");
            }
            transactions.completeTitle(work, vectors.getFirst());
        } catch (RuntimeException exception) {
            try {
                transactions.failTitle(work, exception.getMessage() == null
                        ? exception.getClass().getSimpleName() : exception.getMessage());
            } catch (RuntimeException recordingFailure) {
                exception.addSuppressed(recordingFailure);
            }
            throw exception;
        }
    }

    private void indexBody(ContentSourceSnapshot source) {
        // Chunking and the external API call both run without a database transaction.
        List<ChunkDraft> drafts = chunker.chunk(source.content());
        ContentIndexingTransactions.EmbeddingWork work =
                transactions.prepare(source, drafts, openAi.embeddingModel());
        if (work.targets().isEmpty()) {
            return;
        }
        try {
            List<String> vectors = openAi.embed(work.targets().stream()
                    .map(ContentIndexingTransactions.EmbeddingTarget::content).toList());
            if (vectors == null || vectors.size() != work.targets().size()
                    || vectors.stream().anyMatch(value -> value == null || value.isBlank())) {
                throw new IllegalStateException("임베딩 응답 개수가 청크 개수와 일치하지 않거나 결과가 비어 있습니다.");
            }
            transactions.complete(work, vectors);
        } catch (RuntimeException exception) {
            try {
                transactions.fail(work, exception.getMessage() == null
                        ? exception.getClass().getSimpleName() : exception.getMessage());
            } catch (RuntimeException recordingFailure) {
                exception.addSuppressed(recordingFailure);
            }
            throw exception;
        }
    }

    public static class StaleContentException extends RuntimeException {
    }
}
