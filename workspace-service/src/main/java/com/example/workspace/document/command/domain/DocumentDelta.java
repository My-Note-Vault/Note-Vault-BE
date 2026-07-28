package com.example.workspace.document.command.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "document_delta",
        indexes = @Index(
                name = "idx_document_delta_document_id_revision",
                columnList = "document_id,revision"
        ),
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_document_delta_document_revision",
                        columnNames = {"document_id", "revision"}
                ),
                @UniqueConstraint(
                        name = "uk_document_delta_document_client_update",
                        columnNames = {"document_id", "client_update_id"}
                )
        }
)
@Entity
public class DocumentDelta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "document_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Document document;

    @Column(nullable = false)
    private Long revision;

    @Column(name = "client_update_id", nullable = false, length = 64)
    private String clientUpdateId;

    @JdbcTypeCode(SqlTypes.VARBINARY)
    @Column(name = "yjs_update", nullable = false, columnDefinition = "bytea")
    private byte[] crdtUpdate;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public DocumentDelta(
            final Document document,
            final Long revision,
            final String clientUpdateId,
            final byte[] crdtUpdate
    ) {
        this.document = document;
        this.revision = revision;
        this.clientUpdateId = clientUpdateId;
        this.crdtUpdate = crdtUpdate;
    }
}
