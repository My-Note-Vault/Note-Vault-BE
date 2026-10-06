package com.example.workspace.document.command.domain;

import com.example.common.Auditable;
import com.example.workspace.task.command.domain.value.Schedule;
import com.example.workspace.task.command.domain.value.Status;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.NoSuchElementException;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "document",
        indexes = {
                @Index(columnList = "workspace_id"),
                @Index(columnList = "parent_id"),
                @Index(columnList = "author_id"),
                @Index(columnList = "type")
        }
)
@Entity
@org.hibernate.annotations.DynamicUpdate
public class Document extends Auditable {

    private static final String DEFAULT_TASK_TITLE = "새 Task";
    private static final String DEFAULT_NOTE_TITLE = "새 Document";
    private static final String WORKSPACE_HOME_TITLE = "Workspace Home";
    private static final String DEFAULT_CONTENT = "";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DocumentType type;

    @Column(name = "workspace_id", nullable = false)
    private Long workSpaceId;

    @Column(name = "parent_id")
    private Long parentId;

    @Column(nullable = false)
    private Long authorId;

    @Embedded
    private Schedule schedule;

    @Column(nullable = false)
    private String title;

    @JsonIgnore
    @Column(columnDefinition = "TEXT")
    private String searchContent;

    @JsonIgnore
    @Column(name = "search_content_hash", length = 64)
    private String searchContentHash;

    @JsonIgnore
    @ColumnDefault("0")
    @Column(name = "search_revision")
    private Long searchRevision = 0L;

    @JsonIgnore
    @ColumnDefault("0")
    @Column(name = "snapshot_revision", nullable = false)
    private Long snapshotRevision = 0L;

    @JsonIgnore
    @Column(name = "refresh_batch_event_id")
    private java.util.UUID refreshBatchEventId;

    @JsonIgnore
    @ColumnDefault("0")
    @Column(name = "refresh_batch_base_revision", nullable = false)
    private Long refreshBatchBaseRevision = 0L;

    @JsonIgnore
    @ColumnDefault("0")
    @Column(name = "latest_revision", nullable = false)
    private Long latestRevision = 0L;

    @JsonIgnore
    @ColumnDefault("0")
    @Column(name = "compacted_revision", nullable = false)
    private Long compactedRevision = 0L;

    @JsonIgnore
    @Column(name = "last_compacted_at")
    private LocalDateTime lastCompactedAt;

    @JsonIgnore
    @JdbcTypeCode(SqlTypes.VARBINARY)
    @Column(name = "crdt_state", columnDefinition = "bytea")
    private byte[] crdtState;

    @Column(nullable = false)
    private Boolean isPublic;

    private Document(
            final DocumentType type,
            final Long workSpaceId,
            final Long parentId,
            final Long authorId,
            final Schedule schedule,
            final String title,
            final String searchContent,
            final Boolean isPublic
    ) {
        this.type = type;
        this.workSpaceId = workSpaceId;
        this.parentId = parentId;
        this.authorId = authorId;
        this.schedule = schedule;
        this.title = title;
        this.searchContent = searchContent;
        this.searchContentHash = hashSearchContent(searchContent);
        this.isPublic = isPublic;
    }

    public static Document task(final Long workSpaceId, final Long parentId, final Long authorId) {
        return new Document(
                DocumentType.TASK,
                workSpaceId,
                parentId,
                authorId,
                new Schedule(Status.NOT_STARTED, LocalDateTime.now(), LocalDateTime.now()),
                DEFAULT_TASK_TITLE,
                DEFAULT_CONTENT,
                false
        );
    }

    public static Document workspaceHome(
            final Long workSpaceId,
            final Long authorId,
            final String legacyContent
    ) {
        return new Document(
                DocumentType.WORKSPACE_HOME,
                workSpaceId,
                null,
                authorId,
                null,
                WORKSPACE_HOME_TITLE,
                legacyContent == null ? DEFAULT_CONTENT : legacyContent,
                false
        );
    }

    public static Document note(final Long workSpaceId, final Long parentId, final Long authorId) {
        return new Document(
                DocumentType.NOTE,
                workSpaceId,
                parentId,
                authorId,
                null,
                DEFAULT_NOTE_TITLE,
                DEFAULT_CONTENT,
                false
        );
    }

    public void edit(
            final String title,
            final String searchContent,
            final byte[] crdtState,
            final Status status,
            final LocalDateTime startDateTime,
            final LocalDateTime endDateTime,
            final Boolean isPublic
    ) {
        this.title = title == null ? this.title : title;
        if (searchContent != null) {
            this.searchContent = searchContent;
            this.searchContentHash = hashSearchContent(searchContent);
        }
        this.crdtState = crdtState == null ? this.crdtState : crdtState;
        this.isPublic = isPublic == null ? this.isPublic : isPublic;

        if (this.type == DocumentType.TASK) {
            if (this.schedule == null) {
                this.schedule = new Schedule(Status.NOT_STARTED, null, null);
            }
            this.schedule.edit(status, startDateTime, endDateTime);
        }
    }

    public void moveTo(final Document parent) {
        this.parentId = parent.getId();
        this.workSpaceId = parent.getWorkSpaceId();
    }

    public void reparentTo(final Long parentId) {
        this.parentId = parentId;
    }

    public long issueNextRevision() {
        long currentRevision = this.latestRevision == null ? 0L : this.latestRevision;
        this.latestRevision = currentRevision + 1;
        return this.latestRevision;
    }

    public void trackRefreshBatch(java.util.UUID eventId, long baseRevision) {
        if (eventId == null || baseRevision < 0 || baseRevision >= latestRevision) {
            throw new IllegalArgumentException("Invalid document refresh batch");
        }
        refreshBatchEventId = eventId;
        refreshBatchBaseRevision = baseRevision;
    }

    public void markCompacted(final long revision, final LocalDateTime compactedAt) {
        long currentCompactedRevision =
                this.compactedRevision == null ? 0L : this.compactedRevision;
        long currentSnapshotRevision = this.snapshotRevision == null ? 0L : this.snapshotRevision;
        if (revision <= currentCompactedRevision || revision > currentSnapshotRevision) {
            throw new IllegalArgumentException("Invalid CRDT compaction revision: " + revision);
        }
        this.compactedRevision = revision;
        this.lastCompactedAt = compactedAt;
    }

    private static String hashSearchContent(final String content) {
        if (content == null) {
            return null;
        }
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256")
                    .digest(content.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 알고리즘을 사용할 수 없습니다", exception);
        }
    }

    public void validateType(final DocumentType expected) {
        if (this.type != expected) {
            throw new NoSuchElementException(expected.notFoundMessage());
        }
    }
}
