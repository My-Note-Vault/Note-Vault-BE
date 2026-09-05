package com.example.workspace.document.command.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "document_edit_activity",
        uniqueConstraints = @UniqueConstraint(name = "uk_edit_activity_update", columnNames = "client_update_id"),
        indexes = @Index(name = "idx_edit_activity_date_member", columnList = "activity_date,member_id"))
public class DocumentEditActivity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "document_id", nullable = false)
    private Long documentId;
    @Column(name = "member_id", nullable = false)
    private Long memberId;
    @Column(name = "client_update_id", nullable = false, length = 64)
    private String clientUpdateId;
    @Column(name = "inserted_character_count", nullable = false)
    private int insertedCharacterCount;
    @Column(name = "activity_date", nullable = false)
    private LocalDate activityDate;
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public DocumentEditActivity(Long documentId, Long memberId, String clientUpdateId,
                                int insertedCharacterCount, LocalDate activityDate) {
        this.documentId = documentId;
        this.memberId = memberId;
        this.clientUpdateId = clientUpdateId;
        this.insertedCharacterCount = insertedCharacterCount;
        this.activityDate = activityDate;
    }
}
