package com.example.noteservice.snapshot.command.domain;

import com.example.common.file.UploadStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
@Table(
        name = "snapshot",
        indexes = @Index(columnList = "noteId")
)
@SQLDelete(sql = "UPDATE snapshot " +
        "SET upload_preview_status = DELETED,  " +
        "upload_blurred_status = DELETED " +
        "WHERE id = ?")
@Entity
public class Snapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long noteId;

    @Column(nullable = false)
    private Long authorId;

    @Column(updatable = false)
    private String previewImageKey;
    @Column(updatable = false)
    private String blurredImageKey;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private UploadStatus uploadPreviewStatus;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private UploadStatus uploadBlurredStatus;

    @Column(updatable = false)
    private String title;

    @Column(columnDefinition = "MEDIUMTEXT", updatable = false)
    private String content;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;



    @Builder
    public Snapshot(
            final Long noteId,
            final Long authorId,
            final String previewImageKey,
            final String blurredImageKey,
            final String title, String content
    ) {
        this.noteId = noteId;
        this.authorId = authorId;
        this.previewImageKey = previewImageKey;
        this.blurredImageKey = blurredImageKey;
        this.title = title;
        this.content = content;

        this.uploadPreviewStatus = UploadStatus.PENDING;
        this.uploadBlurredStatus = UploadStatus.PENDING;
    }
}
