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
        if ("space".equalsIgnoreCase(type)) {
            return readWorkspaceHome(memberId, resourceId);
        }
        String resourceType = type.toLowerCase(Locale.ROOT);
        List<ContentSourceSnapshot> documents = jdbc.query("""
                SELECT d.id, d.workspace_id, d.author_id, d.title, d.search_content,
                       d.search_revision, d.search_content_hash, d.updated_at
                FROM document d
                JOIN workspace_member wm ON wm.workspace_id = d.workspace_id AND wm.member_id = ?
                WHERE d.id = ? AND LOWER(d.type) = ?
                """, (rs, row) -> snapshot(rs, ContentSourceType.DOCUMENT, resourceType, resourceId),
                memberId, resourceId, resourceType);
        return requireDocument(documents);
    }

    @Transactional(readOnly = true)
    public ContentSourceSnapshot readDailyNote(Long memberId, Long dailyNoteId) {
        return findDailyNoteSnapshot(memberId, dailyNoteId);
    }

    private ContentSourceSnapshot findDailyNoteSnapshot(Long memberId, Long dailyNoteId) {
        List<ContentSourceSnapshot> notes = jdbc.query("""
                SELECT n.id, NULL AS workspace_id, n.author_id,
                       CAST(n.logical_date AS VARCHAR) AS title,
                       COALESCE(n.content, '') || COALESCE((
                           SELECT string_agg(E'\\n\\n' || p.content, '' ORDER BY p.id)
                           FROM daily_note_plan dnp JOIN plan p ON p.id = dnp.plan_id
                           WHERE dnp.daily_note_id = n.id AND p.content IS NOT NULL AND p.content <> ''
                       ), '') AS content,
                       n.content_revision AS search_revision, NULL AS search_content_hash, n.updated_at
                FROM daily_note n WHERE n.id = ? AND n.author_id = ?
                """, (rs, row) -> snapshot(rs, ContentSourceType.DAILY_NOTE, "daily", dailyNoteId),
                dailyNoteId, memberId);
        if (notes.isEmpty()) {
            throw new NoSuchElementException("DailyNote를 찾을 수 없습니다");
        }
        return notes.getFirst();
    }

    @Transactional(readOnly = true)
    public List<DailyNoteReference> findDailyNotesLinkedToPlan(Long planId) {
        return jdbc.query("""
                SELECT n.id, n.author_id FROM daily_note n
                JOIN daily_note_plan dnp ON dnp.daily_note_id=n.id
                WHERE dnp.plan_id=? ORDER BY n.id
                """, (rs, row) -> new DailyNoteReference(rs.getLong("id"), rs.getLong("author_id")), planId);
    }

    /** Internal worker backfill; never exposed through the API module. */
    @Transactional(readOnly = true)
    public ContentSourceSnapshot readDocumentForIndexing(Long documentId) {
        List<ContentSourceSnapshot> result = jdbc.query("""
                SELECT d.id, d.workspace_id, d.author_id, d.title, d.search_content,
                       d.search_revision, d.search_content_hash, d.updated_at,
                       CASE WHEN d.type='WORKSPACE_HOME' THEN 'space' ELSE LOWER(d.type) END AS resource_type,
                       CASE WHEN d.type='WORKSPACE_HOME' THEN d.workspace_id ELSE d.id END AS resource_id
                FROM document d WHERE d.id=?
                """, (rs, row) -> snapshot(rs, ContentSourceType.DOCUMENT,
                rs.getString("resource_type"), rs.getLong("resource_id")), documentId);
        return requireDocument(result);
    }

    @Transactional
    public TitleWork prepareTitle(ContentSourceSnapshot source, String model) {
        ContentSourceSnapshot current = lockAndReadSource(source);
        if (!Objects.equals(source.title(), current.title())) {
            throw new ContentIndexingService.StaleContentException();
        }
        if (current.title() == null || current.title().isBlank()) {
            jdbc.update("DELETE FROM content_title_embedding WHERE source_type=? AND source_id=?",
                    current.type().name(), current.sourceId());
            return null;
        }
        Integer ready = jdbc.queryForObject("""
                SELECT COUNT(*) FROM content_title_embedding
                WHERE source_type=? AND source_id=? AND source_title=?
                  AND embedding_model=? AND embedding_status='READY'
                """, Integer.class, current.type().name(), current.sourceId(), current.title(), model);
        if (ready != null && ready > 0) return null;
        Integer attempt = jdbc.queryForObject("""
                INSERT INTO content_title_embedding
                    (source_type, source_id, source_title, embedding_model, embedding_status, embedding_attempts)
                VALUES (?, ?, ?, ?, 'PROCESSING', 1)
                ON CONFLICT (source_type, source_id) DO UPDATE
                SET source_title=EXCLUDED.source_title, embedding_model=EXCLUDED.embedding_model,
                    embedding=NULL, embedding_status='PROCESSING', embedding_error=NULL,
                    embedding_attempts=content_title_embedding.embedding_attempts+1,
                    updated_at=CURRENT_TIMESTAMP
                RETURNING embedding_attempts
                """, Integer.class, current.type().name(), current.sourceId(), current.title(), model);
        return new TitleWork(current, model, Objects.requireNonNull(attempt));
    }

    @Transactional
    public void completeTitle(TitleWork work, String vector) {
        ContentSourceSnapshot current = lockAndReadSource(work.source());
        if (!Objects.equals(work.source().title(), current.title())) {
            throw new ContentIndexingService.StaleContentException();
        }
        int changed = jdbc.update("""
                UPDATE content_title_embedding SET embedding=?, embedding_status='READY',
                    embedding_error=NULL, updated_at=CURRENT_TIMESTAMP
                WHERE source_type=? AND source_id=? AND source_title=? AND embedding_model=?
                  AND embedding_attempts=? AND embedding_status='PROCESSING'
                """, vector, current.type().name(), current.sourceId(), current.title(), work.model(), work.attempt());
        if (changed == 0) {
            Integer ready = jdbc.queryForObject("""
                    SELECT COUNT(*) FROM content_title_embedding
                    WHERE source_type=? AND source_id=? AND source_title=?
                      AND embedding_model=? AND embedding_status='READY'
                    """, Integer.class, current.type().name(), current.sourceId(), current.title(), work.model());
            if (ready == null || ready == 0) throw new ContentIndexingService.StaleContentException();
        }
    }

    @Transactional
    public void failTitle(TitleWork work, String error) {
        try {
            lockAndReadSource(work.source());
        } catch (NoSuchElementException deleted) {
            return;
        }
        jdbc.update("""
                UPDATE content_title_embedding SET embedding_status='FAILED', embedding_error=?,
                    updated_at=CURRENT_TIMESTAMP
                WHERE source_type=? AND source_id=? AND source_title=? AND embedding_model=?
                  AND embedding_attempts=? AND embedding_status='PROCESSING'
                """, error, work.source().type().name(), work.source().sourceId(), work.source().title(),
                work.model(), work.attempt());
    }

    @Transactional
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

    @Transactional
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

    @Transactional
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

    private ContentSourceSnapshot lockAndReadSource(ContentSourceSnapshot source) {
        return switch (source.type()) {
            case DOCUMENT -> lockAndReadDocument(source);
            case DAILY_NOTE -> lockAndReadDailyNote(source.ownerId(), source.sourceId());
        };
    }

    private void requireCurrent(ContentSourceSnapshot source, ContentSourceSnapshot current) {
        if (!Objects.equals(source.contentHash(), current.contentHash())
                || (source.type() == ContentSourceType.DOCUMENT
                    && (!Objects.equals(source.revision(), current.revision())
                        || !Objects.equals(source.sourceUpdatedAt(), current.sourceUpdatedAt())))) {
            throw new ContentIndexingService.StaleContentException();
        }
    }

    private ContentSourceSnapshot readWorkspaceHome(Long memberId, Long workspaceId) {
        List<ContentSourceSnapshot> documents = jdbc.query("""
                SELECT d.id, d.workspace_id, d.author_id, d.title, d.search_content,
                       d.search_revision, d.search_content_hash, d.updated_at
                FROM document d
                JOIN workspace_member wm ON wm.workspace_id = d.workspace_id AND wm.member_id = ?
                WHERE d.workspace_id = ? AND d.type = 'WORKSPACE_HOME'
                """, (rs, row) -> snapshot(rs, ContentSourceType.DOCUMENT, "space", workspaceId),
                memberId, workspaceId);
        return requireDocument(documents);
    }

    private ContentSourceSnapshot lockAndReadDocument(ContentSourceSnapshot source) {
        List<ContentSourceSnapshot> documents = jdbc.query("""
                SELECT d.id, d.workspace_id, d.author_id, d.title, d.search_content,
                       d.search_revision, d.search_content_hash, d.updated_at
                FROM document d WHERE d.id = ? FOR UPDATE
                """, (rs, row) -> snapshot(rs, ContentSourceType.DOCUMENT, source.resourceType(), source.resourceId()),
                source.sourceId());
        return requireDocument(documents);
    }

    private ContentSourceSnapshot lockAndReadDailyNote(Long memberId, Long dailyNoteId) {
        lockDailyNote(memberId, dailyNoteId);
        lockLinkedPlans(dailyNoteId);
        // Read after all locks are acquired, using the surrounding write transaction.
        return findDailyNoteSnapshot(memberId, dailyNoteId);
    }

    private void lockDailyNote(Long memberId, Long dailyNoteId) {
        List<Long> notes = jdbc.query("""
                SELECT id FROM daily_note WHERE id = ? AND author_id = ? FOR UPDATE
                """, (rs, row) -> rs.getLong("id"), dailyNoteId, memberId);
        if (notes.isEmpty()) {
            throw new NoSuchElementException("DailyNote를 찾을 수 없습니다");
        }
    }

    private void lockLinkedPlans(Long dailyNoteId) {
        jdbc.query("""
                SELECT p.id
                FROM daily_note_plan dnp JOIN plan p ON p.id = dnp.plan_id
                WHERE dnp.daily_note_id = ?
                ORDER BY p.id
                FOR SHARE OF p, dnp
                """, (rs, row) -> rs.getLong("id"), dailyNoteId);
    }

    private ContentSourceSnapshot requireDocument(List<ContentSourceSnapshot> documents) {
        if (documents.isEmpty()) {
            throw new NoSuchElementException("Document를 찾을 수 없습니다");
        }
        return documents.getFirst();
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

    public record DailyNoteReference(Long id, Long ownerId) {
    }

    public record TitleWork(ContentSourceSnapshot source, String model, int attempt) {
    }

    public record EmbeddingWork(ContentSourceSnapshot source, String model, List<EmbeddingTarget> targets) {
    }
}
