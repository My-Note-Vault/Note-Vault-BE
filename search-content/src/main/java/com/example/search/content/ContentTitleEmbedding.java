package com.example.search.content;

import jakarta.persistence.*;
import com.example.search.embedding.EmbeddingClient;
import org.hibernate.annotations.Array;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "content_title_embedding")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ContentTitleEmbedding {
    @EmbeddedId private Id id;
    @Column(name = "source_title", nullable = false, columnDefinition = "TEXT") private String title;
    @JdbcTypeCode(SqlTypes.VECTOR)
    @Array(length = EmbeddingClient.DIMENSIONS)
    @Column(columnDefinition = "vector(1536)") private float[] embedding;
    @Column(name = "embedding_model", nullable = false) private String model;
    @Enumerated(EnumType.STRING)
    @Column(name = "embedding_status", nullable = false, length = 20) private EmbeddingStatus status;
    @Column(name = "embedding_attempts", nullable = false) private int attempts;
    @Column(name = "embedding_error", columnDefinition = "TEXT") private String error;
    @Column(name = "updated_at", nullable = false) private LocalDateTime updatedAt;

    public ContentTitleEmbedding(Id id) {
        this.id = id;
    }

    public int claim(String title, String model) {
        this.title = title;
        this.model = model;
        embedding = null;
        error = null;
        status = EmbeddingStatus.PROCESSING;
        return ++attempts;
    }

    public boolean ready(String title, String model) {
        return matches(title, model) && status == EmbeddingStatus.READY && embedding != null;
    }

    public boolean owns(String title, String model, int attempt) {
        return matches(title, model) && status == EmbeddingStatus.PROCESSING && attempts == attempt;
    }

    public void complete(float[] vector) {
        embedding = vector;
        status = EmbeddingStatus.READY;
        error = null;
    }

    public void fail(String reason) {
        status = EmbeddingStatus.FAILED;
        error = reason;
    }

    private boolean matches(String title, String model) {
        return Objects.equals(this.title, title) && Objects.equals(this.model, model);
    }

    @PrePersist @PreUpdate
    private void touch() {
        updatedAt = LocalDateTime.now();
    }

    @Embeddable
    @Getter
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    @AllArgsConstructor
    @EqualsAndHashCode
    public static class Id implements Serializable {
        @Enumerated(EnumType.STRING)
        @Column(name = "source_type", length = 30) private ContentSourceType sourceType;
        @Column(name = "source_id") private Long sourceId;
    }
}
