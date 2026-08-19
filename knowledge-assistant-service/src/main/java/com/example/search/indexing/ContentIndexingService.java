package com.example.search.indexing;

import com.example.search.content.ContentChunk;
import com.example.search.content.ContentChunkRepository;
import com.example.search.content.ContentSourceSnapshot;
import com.example.search.content.ContentSourceType;
import com.example.search.content.EmbeddingStatus;
import com.example.search.infrastructure.OpenAiSearchClient;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;

@RequiredArgsConstructor
@Service
public class ContentIndexingService {
    private final JdbcTemplate jdbc;
    private final ContentChunkRepository chunks;
    private final OpenAiSearchClient openAi;

    @Transactional
    public void indexDocument(Long memberId, String type, Long resourceId, Long requestedRevision) {
        ContentSourceSnapshot source = readDocument(memberId, type, resourceId, true);
        if (requestedRevision == null || !Objects.equals(source.revision(), requestedRevision)) {
            throw new StaleContentException();
        }
        index(source);
    }

    @Transactional
    public void indexDailyNote(Long memberId, Long dailyNoteId) {
        index(readDailyNote(memberId, dailyNoteId, true));
    }

    private void index(ContentSourceSnapshot source) {
        List<Draft> drafts = chunk(source.content());
        ContentSourceSnapshot current = source.type() == ContentSourceType.DOCUMENT
                ? readDocumentById(source.sourceId(), source.resourceType(), source.resourceId(), true)
                : readDailyNote(source.ownerId(), source.sourceId(), true);
        if (!Objects.equals(source.contentHash(), current.contentHash())
                || !Objects.equals(source.revision(), current.revision())
                || !Objects.equals(source.sourceUpdatedAt(), current.sourceUpdatedAt())) {
            throw new StaleContentException();
        }

        synchronize(current, drafts);
        List<ContentChunk> targets = chunks.findEmbeddingTargets(
                current.type(), current.sourceId(), EmbeddingStatus.READY, openAi.embeddingModel());
        if (targets.isEmpty()) {
            return;
        }
        targets.forEach(ContentChunk::startEmbedding);
        try {
            List<String> embeddings = openAi.embed(targets.stream().map(ContentChunk::getContent).toList());
            for (int index = 0; index < targets.size(); index++) {
                targets.get(index).saveEmbedding(embeddings.get(index), openAi.embeddingModel());
            }
        } catch (RuntimeException exception) {
            targets.forEach(chunk -> chunk.failEmbedding(message(exception)));
            throw exception;
        }
    }

    private void synchronize(ContentSourceSnapshot source, List<Draft> drafts) {
        List<ContentChunk> existing = chunks.findAllBySourceTypeAndSourceIdOrderByChunkIndexAsc(
                source.type(), source.sourceId());
        Map<String, Deque<ContentChunk>> byHash = new HashMap<>();
        existing.forEach(chunk -> byHash.computeIfAbsent(chunk.getContentHash(), ignored -> new ArrayDeque<>()).add(chunk));
        Set<Long> keep = new HashSet<>();
        List<ContentChunk> add = new ArrayList<>();

        for (Draft draft : drafts) {
            Deque<ContentChunk> matching = byHash.get(draft.hash());
            ContentChunk chunk = matching == null ? null : matching.pollFirst();
            if (chunk == null) {
                add.add(new ContentChunk(source, draft.index(), draft.content(), draft.hash()));
            } else {
                chunk.retain(source, draft.index());
                keep.add(chunk.getId());
            }
        }
        chunks.deleteAllInBatch(existing.stream().filter(chunk -> !keep.contains(chunk.getId())).toList());
        chunks.saveAll(add);
    }

    private ContentSourceSnapshot readDocument(Long memberId, String type, Long resourceId, boolean lock) {
        if ("space".equalsIgnoreCase(type)) {
            return one("SELECT d.id,d.workspace_id,d.author_id,d.title,d.search_content,d.search_revision,d.search_content_hash,d.updated_at "
                            + "FROM document d JOIN workspace_member wm ON wm.workspace_id=d.workspace_id AND wm.member_id=? "
                            + "WHERE d.workspace_id=? AND d.type='WORKSPACE_HOME'" + (lock ? " FOR UPDATE" : ""),
                    memberId, resourceId, "space", resourceId, ContentSourceType.DOCUMENT);
        }
        return one("SELECT d.id,d.workspace_id,d.author_id,d.title,d.search_content,d.search_revision,d.search_content_hash,d.updated_at "
                        + "FROM document d JOIN workspace_member wm ON wm.workspace_id=d.workspace_id AND wm.member_id=? "
                        + "WHERE d.id=? AND LOWER(d.type)=?" + (lock ? " FOR UPDATE" : ""),
                memberId, resourceId, type.toLowerCase(Locale.ROOT), resourceId, ContentSourceType.DOCUMENT);
    }

    private ContentSourceSnapshot readDocumentById(Long id, String resourceType, Long resourceId, boolean lock) {
        return one("SELECT d.id,d.workspace_id,d.author_id,d.title,d.search_content,d.search_revision,d.search_content_hash,d.updated_at "
                        + "FROM document d WHERE d.id=?" + (lock ? " FOR UPDATE" : ""),
                null, id, resourceType, resourceId, ContentSourceType.DOCUMENT);
    }

    private ContentSourceSnapshot readDailyNote(Long memberId, Long id, boolean lock) {
        String sql = "SELECT n.id,NULL AS workspace_id,n.author_id,CAST(n.logical_date AS VARCHAR) AS title,n.content,"
                + "NULL AS search_revision,NULL AS search_content_hash,n.updated_at FROM daily_note n "
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
                revision, sha256(content), updatedAt);
    }

    private List<Draft> chunk(String raw) {
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        List<String> sections = new ArrayList<>();
        StringBuilder section = new StringBuilder();
        boolean fencedCode = false;
        for (String line : raw.replace("\r\n", "\n").split("\n", -1)) {
            if (line.stripLeading().startsWith("```") || line.stripLeading().startsWith("~~~")) {
                fencedCode = !fencedCode;
            }
            if (!fencedCode && line.matches("^#{1,6}\\s+.+$") && !section.isEmpty()) {
                sections.add(section.toString().strip());
                section.setLength(0);
            }
            if (!section.isEmpty()) {
                section.append('\n');
            }
            section.append(line);
        }
        if (!section.isEmpty()) {
            sections.add(section.toString().strip());
        }

        List<Draft> result = new ArrayList<>();
        StringBuilder accumulated = new StringBuilder();
        for (String value : sections) {
            if (!accumulated.isEmpty()) {
                accumulated.append('\n');
            }
            accumulated.append(value);
            if (accumulated.codePointCount(0, accumulated.length()) >= 700) {
                addDrafts(result, accumulated.toString());
                accumulated.setLength(0);
            }
        }
        if (!accumulated.isEmpty()) {
            addDrafts(result, accumulated.toString());
        }
        return result;
    }

    private void addDrafts(List<Draft> result, String content) {
        int start = 0;
        while (start < content.length()) {
            int end = content.offsetByCodePoints(
                    start, Math.min(1800, content.codePointCount(start, content.length())));
            String part = content.substring(start, end).strip();
            if (!part.isEmpty()) {
                result.add(new Draft(result.size(), part, sha256(part)));
            }
            start = end;
        }
    }

    private String sha256(String value) {
        try {
            return HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private String message(Exception exception) {
        return exception.getMessage() == null ? exception.getClass().getSimpleName() : exception.getMessage();
    }

    private record Draft(int index, String content, String hash) {
    }

    public static class StaleContentException extends RuntimeException {
    }
}
