package com.example.search.indexing;

import com.example.search.content.ContentChunk;
import com.example.search.content.ContentChunkRepository;
import com.example.search.content.ContentSourceSnapshot;
import com.example.search.content.ContentSourceType;
import com.example.search.content.EmbeddingStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;

@RequiredArgsConstructor
@Service
public class ContentIndexingTransactions {
    private final JdbcTemplate jdbc;
    private final ContentChunkRepository chunks;

    @Transactional(readOnly = true)
    public ContentSourceSnapshot readDocument(Long memberId, String type, Long resourceId) {
        return readDocument(memberId, type, resourceId, false);
    }

    @Transactional(readOnly = true)
    public ContentSourceSnapshot readDailyNote(Long memberId, Long dailyNoteId) {
        return readDailyNote(memberId, dailyNoteId, false);
    }

    @Transactional
    public EmbeddingWork prepare(ContentSourceSnapshot source, List<ChunkDraft> drafts, String model) {
        ContentSourceSnapshot current = lockSource(source);
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

    @Transactional
    public void complete(EmbeddingWork work, List<String> vectors) {
        requireCurrent(work.source(), lockSource(work.source()));
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

    @Transactional
    public void fail(EmbeddingWork work, String error) {
        try {
            lockSource(work.source());
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

    private ContentSourceSnapshot lockSource(ContentSourceSnapshot source) {
        return source.type() == ContentSourceType.DOCUMENT
                ? readDocumentById(source.sourceId(), source.resourceType(), source.resourceId(), true)
                : readDailyNote(source.ownerId(), source.sourceId(), true);
    }

    private void requireCurrent(ContentSourceSnapshot source, ContentSourceSnapshot current) {
        if (!Objects.equals(source.contentHash(), current.contentHash())
                || !Objects.equals(source.revision(), current.revision())
                || !Objects.equals(source.sourceUpdatedAt(), current.sourceUpdatedAt())) {
            throw new ContentIndexingService.StaleContentException();
        }
    }

    private ContentSourceSnapshot readDocument(Long memberId, String type, Long resourceId, boolean lock) {
        if ("space".equalsIgnoreCase(type)) {
            return one("SELECT d.id,d.workspace_id,d.author_id,d.title,d.search_content,d.search_revision,d.search_content_hash,d.updated_at "
                            + "FROM document d JOIN workspace_member wm ON wm.workspace_id=d.workspace_id AND wm.member_id=? "
                            + "WHERE d.workspace_id=? AND d.type='WORKSPACE_HOME'" + (lock ? " FOR UPDATE OF d" : ""),
                    memberId, resourceId, "space", resourceId, ContentSourceType.DOCUMENT);
        }
        return one("SELECT d.id,d.workspace_id,d.author_id,d.title,d.search_content,d.search_revision,d.search_content_hash,d.updated_at "
                        + "FROM document d JOIN workspace_member wm ON wm.workspace_id=d.workspace_id AND wm.member_id=? "
                        + "WHERE d.id=? AND LOWER(d.type)=?" + (lock ? " FOR UPDATE OF d" : ""),
                memberId, resourceId, type.toLowerCase(Locale.ROOT), resourceId, ContentSourceType.DOCUMENT);
    }

    private ContentSourceSnapshot readDocumentById(Long id, String resourceType, Long resourceId, boolean lock) {
        return one("SELECT d.id,d.workspace_id,d.author_id,d.title,d.search_content,d.search_revision,d.search_content_hash,d.updated_at "
                        + "FROM document d WHERE d.id=?" + (lock ? " FOR UPDATE" : ""),
                null, id, resourceType, resourceId, ContentSourceType.DOCUMENT);
    }

    private ContentSourceSnapshot readDailyNote(Long memberId, Long id, boolean lock) {
        String sql = "SELECT n.id,NULL AS workspace_id,n.author_id,CAST(n.logical_date AS VARCHAR) AS title,n.content,"
                + "n.content_revision AS search_revision,NULL AS search_content_hash,n.updated_at FROM daily_note n "
                + "WHERE n.id=? AND n.author_id=?" + (lock ? " FOR UPDATE" : "");
        List<ContentSourceSnapshot> result = jdbc.query(
                sql, (rs, row) -> snapshot(rs, ContentSourceType.DAILY_NOTE, "daily", id), id, memberId);
        if (result.isEmpty()) {
            throw new NoSuchElementException("DailyNote를 찾을 수 없습니다");
        }
        return result.getFirst();
    }

    private ContentSourceSnapshot one(String sql, Long memberId, Long id, String type,
                                      Long resourceId, ContentSourceType sourceType) {
        Object[] args = memberId == null
                ? new Object[]{id}
                : "space".equals(type) ? new Object[]{memberId, id} : new Object[]{memberId, id, type};
        List<ContentSourceSnapshot> result = jdbc.query(
                sql, (rs, row) -> snapshot(rs, sourceType, type, resourceId), args);
        if (result.isEmpty()) {
            throw new NoSuchElementException("Document를 찾을 수 없습니다");
        }
        return result.getFirst();
    }

    private ContentSourceSnapshot snapshot(java.sql.ResultSet rs, ContentSourceType type,
                                           String resourceType, Long resourceId) throws java.sql.SQLException {
        String content = type == ContentSourceType.DAILY_NOTE
                ? Objects.requireNonNullElse(rs.getString("content"), "")
                : Objects.requireNonNullElse(rs.getString("search_content"), "");
        Long revision = rs.getObject("search_revision", Long.class);
        LocalDateTime updatedAt = rs.getObject("updated_at", LocalDateTime.class);
        return new ContentSourceSnapshot(type, rs.getLong("id"), rs.getObject("workspace_id", Long.class),
                rs.getLong("author_id"), resourceType, resourceId, rs.getString("title"), content,
                revision, ContentChunker.sha256(content), updatedAt);
    }

    public record EmbeddingTarget(Long id, String content, String hash, int attempt) {
    }

    public record EmbeddingWork(ContentSourceSnapshot source, String model, List<EmbeddingTarget> targets) {
    }
}
