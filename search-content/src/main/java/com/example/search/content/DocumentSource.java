package com.example.search.content;

import com.example.search.indexing.ContentChunker;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Objects;

/** Immutable indexing read model; CRDT worker writes use narrow, revision-checked SQL separately. */
@Entity(name = "SearchDocumentSource")
@Table(name = "document")
@Immutable
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DocumentSource {
    @Id private Long id;
    @Column(name = "workspace_id") private Long workspaceId;
    @Column(name = "author_id") private Long authorId;
    private String type;
    private String title;
    @Column(name = "search_content", columnDefinition = "TEXT") private String content;
    @Column(name = "search_revision") private Long revision;
    @Column(name = "updated_at") private LocalDateTime updatedAt;

    public ContentSourceSnapshot snapshot() {
        boolean home = "WORKSPACE_HOME".equals(type);
        String body = Objects.requireNonNullElse(content, "");
        return new ContentSourceSnapshot(ContentSourceType.DOCUMENT, id, workspaceId, authorId,
                home ? "space" : type.toLowerCase(Locale.ROOT), home ? workspaceId : id,
                Objects.requireNonNullElse(title, ""), body, revision, ContentChunker.sha256(body), updatedAt);
    }
}
