package com.example.search.indexing;

import com.example.search.content.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;

@RequiredArgsConstructor
@Service
@Transactional(propagation = Propagation.REQUIRES_NEW, timeout = 5)
public class ContentIndexingTransactions {
    private final DocumentSourceRepository documents;
    private final DailyNoteSourceRepository notes;
    private final ContentChunkRepository chunks;
    private final ContentTitleEmbeddingRepository titles;

    @Transactional(readOnly = true, propagation = Propagation.REQUIRES_NEW, timeout = 5)
    public Optional<ContentSourceSnapshot> readSource(ContentSourceType type, Long sourceId) {
        return switch (type) {
            case DOCUMENT -> documents.findById(sourceId).map(DocumentSource::snapshot);
            case DAILY_NOTE -> notes.findById(sourceId)
                    .map(note -> note.snapshot(notes.findPlanContents(sourceId)));
        };
    }

    @Transactional(readOnly = true, propagation = Propagation.REQUIRES_NEW, timeout = 5)
    public List<Long> sourceIds(ContentSourceType type, long after) {
        return type == ContentSourceType.DOCUMENT ? documents.findNextIds(after) : notes.findNextIds(after);
    }

    public void deleteIfMissing(ContentSourceType type, Long sourceId) {
        if (lockSource(type, sourceId).isPresent()) throw new ContentIndexingService.StaleContentException();
        // Source IDs are not reused. In-flight writers also require an existing source-row lock.
        chunks.deleteSource(type, sourceId);
        titles.deleteById(new ContentTitleEmbedding.Id(type, sourceId));
    }

    public TitleWork prepareTitle(ContentSourceSnapshot source, String model) {
        ContentSourceSnapshot current = lockAndReadSource(source);
        requireCurrentTitle(source, current);
        ContentTitleEmbedding.Id id = titleId(current);
        if (current.title().isBlank()) {
            titles.deleteById(id);
            return null;
        }
        ContentTitleEmbedding title = titles.findById(id).orElseGet(() -> new ContentTitleEmbedding(id));
        if (title.ready(current.title(), model)) return null;
        int attempt = title.claim(current.title(), model);
        titles.save(title);
        return new TitleWork(current, model, attempt);
    }

    public void completeTitle(TitleWork work, String vector) {
        ContentSourceSnapshot current = lockAndReadSource(work.source());
        requireCurrentTitle(work.source(), current);
        ContentTitleEmbedding title = titles.findById(titleId(current))
                .orElseThrow(ContentIndexingService.StaleContentException::new);
        if (title.owns(current.title(), work.model(), work.attempt())) {
            title.complete(vector);
        } else if (!title.ready(current.title(), work.model())) {
            throw new ContentIndexingService.StaleContentException();
        }
    }

    public void failTitle(TitleWork work, String error) {
        if (lockSource(work.source().type(), work.source().sourceId()).isEmpty()) return;
        titles.findById(titleId(work.source())).ifPresent(title -> {
            if (title.owns(work.source().title(), work.model(), work.attempt())) title.fail(error);
        });
    }

    /** Both title and body must still be complete before the consumer acknowledges the message. */
    public void verifyComplete(ContentSourceSnapshot source, List<ChunkDraft> drafts, String model) {
        ContentSourceSnapshot current = lockAndReadSource(source);
        requireCurrent(source, current);
        Optional<ContentTitleEmbedding> title = titles.findById(titleId(current));
        boolean titleReady = current.title().isBlank()
                ? title.isEmpty() : title.filter(value -> value.ready(current.title(), model)).isPresent();
        List<ContentChunk> rows = chunks.findAllBySourceTypeAndSourceIdOrderByChunkIndexAsc(
                source.type(), source.sourceId());
        if (!titleReady || rows.size() != drafts.size()) throw new ContentIndexingService.StaleContentException();
        for (int i = 0; i < drafts.size(); i++) {
            ContentChunk row = rows.get(i);
            ChunkDraft draft = drafts.get(i);
            if (row.getChunkIndex() != i || !source.version().equals(row.getSourceVersion())
                    || !draft.hash().equals(row.getContentHash()) || !draft.content().equals(row.getContent())
                    || row.getEmbeddingStatus() != EmbeddingStatus.READY || row.getEmbedding() == null
                    || !model.equals(row.getEmbeddingModel())
                    || !Objects.equals(source.title(), row.getSourceTitle())
                    || !Objects.equals(source.workspaceId(), row.getWorkspaceId())
                    || !Objects.equals(source.ownerId(), row.getOwnerId())
                    || !Objects.equals(source.resourceType(), row.getResourceType())
                    || !Objects.equals(source.resourceId(), row.getResourceId())) {
                throw new ContentIndexingService.StaleContentException();
            }
        }
    }

    public EmbeddingWork prepare(ContentSourceSnapshot source, List<ChunkDraft> drafts, String model) {
        ContentSourceSnapshot current = lockAndReadSource(source);
        requireCurrent(source, current);
        synchronize(current, drafts, model);
        List<ContentChunk> targets = chunks.findEmbeddingTargets(
                current.type(), current.sourceId(), EmbeddingStatus.READY, model);
        List<EmbeddingTarget> work = new ArrayList<>();
        for (ContentChunk chunk : targets) {
            chunk.startEmbedding();
            work.add(new EmbeddingTarget(chunk.getId(), chunk.getContent(),
                    chunk.getContentHash(), chunk.getEmbeddingAttempts()));
        }
        return new EmbeddingWork(current, model, List.copyOf(work));
    }

    public void complete(EmbeddingWork work, List<String> vectors) {
        ContentSourceSnapshot currentSource = lockAndReadSource(work.source());
        requireCurrent(work.source(), currentSource);
        Map<Long, ContentChunk> current = chunksById(work.source());
        // A newer attempt owns the result if the same event was processed concurrently.
        if (work.targets().stream().anyMatch(target -> !owns(current.get(target.id()), target, work))) {
            boolean alreadyCompleted = work.targets().stream().allMatch(target -> {
                ContentChunk chunk = current.get(target.id());
                return chunk != null
                        && chunk.getSourceVersion().equals(work.source().version())
                        && chunk.getContentHash().equals(target.hash())
                        && chunk.getEmbeddingStatus() == EmbeddingStatus.READY
                        && work.model().equals(chunk.getEmbeddingModel());
            });
            if (alreadyCompleted) {
                return;
            }
            // Do not report success while the newer attempt is unfinished or has failed.
            throw new ContentIndexingService.StaleContentException();
        }
        for (int index = 0; index < work.targets().size(); index++) {
            current.get(work.targets().get(index).id()).saveEmbedding(vectors.get(index), work.model());
        }
    }

    public void fail(EmbeddingWork work, String error) {
        try {
            lockAndReadSource(work.source());
        } catch (NoSuchElementException deleted) {
            return;
        }
        Map<Long, ContentChunk> current = chunksById(work.source());
        for (EmbeddingTarget target : work.targets()) {
            ContentChunk chunk = current.get(target.id());
            if (owns(chunk, target, work)) {
                chunk.failEmbedding(error);
            }
        }
    }

    private boolean owns(ContentChunk chunk, EmbeddingTarget target, EmbeddingWork work) {
        return chunk != null
                && chunk.getSourceVersion().equals(work.source().version())
                && chunk.getContentHash().equals(target.hash())
                && chunk.getEmbeddingAttempts() == target.attempt()
                && chunk.getEmbeddingStatus() == EmbeddingStatus.PROCESSING;
    }

    private Map<Long, ContentChunk> chunksById(ContentSourceSnapshot source) {
        Map<Long, ContentChunk> result = new HashMap<>();
        chunks.findAllBySourceTypeAndSourceIdOrderByChunkIndexAsc(source.type(), source.sourceId())
                .forEach(chunk -> result.put(chunk.getId(), chunk));
        return result;
    }

    private void synchronize(ContentSourceSnapshot source, List<ChunkDraft> drafts, String model) {
        List<ContentChunk> existing = chunks.findAllBySourceTypeAndSourceIdOrderByChunkIndexAsc(
                source.type(), source.sourceId());
        Map<Integer, ContentChunk> sameVersion = new HashMap<>();
        Map<String, ContentChunk> reusable = new HashMap<>();
        List<ContentChunk> obsolete = new ArrayList<>();
        for (ContentChunk chunk : existing) {
            if (chunk.getEmbeddingStatus() == EmbeddingStatus.READY
                    && model.equals(chunk.getEmbeddingModel())) {
                reusable.putIfAbsent(chunk.getContentHash(), chunk);
            }
            if (source.version().equals(chunk.getSourceVersion())) {
                if (sameVersion.putIfAbsent(chunk.getChunkIndex(), chunk) != null) {
                    throw new IllegalStateException("중복 청크를 정리하는 DB 마이그레이션이 필요합니다.");
                }
            } else {
                obsolete.add(chunk);
            }
        }
        if (sameVersion.keySet().stream().anyMatch(index -> index < 0 || index >= drafts.size())) {
            throw new IllegalStateException("같은 원문 버전의 청킹 결과가 변경되었습니다.");
        }
        List<ContentChunk> add = new ArrayList<>();
        for (int index = 0; index < drafts.size(); index++) {
            ChunkDraft draft = drafts.get(index);
            if (draft.index() != index) {
                throw new IllegalArgumentException("청크 순번은 0부터 연속이어야 합니다.");
            }
            ContentChunk chunk = sameVersion.get(index);
            if (chunk != null) {
                if (!chunk.getContentHash().equals(draft.hash()) || !chunk.getContent().equals(draft.content())) {
                    throw new IllegalStateException("같은 원문 버전의 청킹 결과가 변경되었습니다.");
                }
                chunk.retain(source, index);
                continue;
            }
            chunk = new ContentChunk(source, index, draft.content(), draft.hash());
            ContentChunk cached = reusable.get(draft.hash());
            if (cached != null && cached.getContent().equals(draft.content())) {
                chunk.saveEmbedding(cached.getEmbedding(), model);
            }
            add.add(chunk);
        }
        // A source-row lock serializes writers; the unique constraint is the final DB guard.
        // Same-version rows retain their IDs and positions, avoiding hash-based reordering conflicts.
        chunks.deleteAllInBatch(obsolete);
        chunks.saveAllAndFlush(add);
    }

    private Optional<ContentSourceSnapshot> lockSource(ContentSourceType type, Long sourceId) {
        return switch (type) {
            case DOCUMENT -> documents.lockById(sourceId).map(DocumentSource::snapshot);
            case DAILY_NOTE -> notes.lockById(sourceId)
                    .map(note -> {
                        notes.lockPlans(sourceId);
                        return note.snapshot(notes.lockPlanContents(sourceId));
                    });
        };
    }

    private ContentSourceSnapshot lockAndReadSource(ContentSourceSnapshot source) {
        return lockSource(source.type(), source.sourceId())
                .orElseThrow(() -> new NoSuchElementException("Search source was deleted"));
    }

    private void requireCurrent(ContentSourceSnapshot source, ContentSourceSnapshot current) {
        if (!source.sameSearchInput(current)) throw new ContentIndexingService.StaleContentException();
    }

    private void requireCurrentTitle(ContentSourceSnapshot source, ContentSourceSnapshot current) {
        if (!Objects.equals(source.title(), current.title())) throw new ContentIndexingService.StaleContentException();
    }

    private ContentTitleEmbedding.Id titleId(ContentSourceSnapshot source) {
        return new ContentTitleEmbedding.Id(source.type(), source.sourceId());
    }

    public record EmbeddingTarget(Long id, String content, String hash, int attempt) { }
    public record TitleWork(ContentSourceSnapshot source, String model, int attempt) { }
    public record EmbeddingWork(ContentSourceSnapshot source, String model, List<EmbeddingTarget> targets) { }
}
