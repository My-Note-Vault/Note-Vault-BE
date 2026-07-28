package com.example.workspace.document.command.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "document_delta_archive",
        indexes = {
                @Index(
                        name = "idx_document_delta_archive_delete_after",
                        columnList = "delete_after"
                )
        },
        uniqueConstraints = @UniqueConstraint(
                name = "uk_document_delta_archive_document_revision",
                columnNames = {"document_id", "revision"}
        )
)
@Entity
public class DocumentDeltaArchive {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "document_id", nullable = false)
    private Long documentId;

    @Column(nullable = false)
    private Long revision;

    @Column(name = "client_update_id", nullable = false, length = 64)
    private String clientUpdateId;

    @JdbcTypeCode(SqlTypes.VARBINARY)
    @Column(name = "crdt_update", nullable = false, columnDefinition = "bytea")
    private byte[] crdtUpdate;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "archived_at", nullable = false)
    private LocalDateTime archivedAt;

    @Column(name = "delete_after", nullable = false)
    private LocalDateTime deleteAfter;

    public DocumentDeltaArchive(
            final DocumentDelta delta,
            final LocalDateTime archivedAt,
            final LocalDateTime deleteAfter
    ) {
        this.documentId = delta.getDocument().getId();
        this.revision = delta.getRevision();
        this.clientUpdateId = delta.getClientUpdateId();
        this.crdtUpdate = delta.getCrdtUpdate();
        this.createdAt = delta.getCreatedAt();
        this.archivedAt = archivedAt;
        this.deleteAfter = deleteAfter;
    }
}
